package frc.robot.outReach.utils;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.wpilibj.DriverStation;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/*
 * Utility class for computing firing solutions for a complete shooter system.
 *
 * Given a target pose and the robot's current velocity, this class computes the
 * projectile initial velocity, turret yaw, and hood pitch required to hit the target accounting
 * for gravity and robot motion. Does NOT interface with any motors.
 *
 * Designed to choose the solution with the lowest shooter speed. In case of a tie, it selects
 * the solution with the shortest flight time.
 */
public class FiringSolutionSolver {

  /** Gravity acceleration (m/s^2) */
  private static final double GRAVITY = 9.81;

  /** Shooter speed limits (m/s) */
  private final double minShooterSpeed;

  private final double maxShooterSpeed;

  /** Flight time parameters */
  private final double minTime = 0.05;

  private final double maxTime = 5.0;
  private final double dt = 0.01;

  /** Turret yaw limits (radians) */
  private final double maxTurretYawRad = Math.toRadians(167.0);

  /** Hood pitch limits (radians) - update when known */
  private double minHoodPitchRad = Math.toRadians(-90.0); // TODO: replace with actual limits

  private double maxHoodPitchRad = Math.toRadians(90.0); // TODO: replace with actual limits

  /** Flywheel radius for internal angular velocity calculation (meters) */
  private static final double FLYWHEEL_RADIUS_METERS = 0.0762; // TODO: replace with actual

  /** Stores the reason a shot is impossible */
  private String lastImpossibleReason = "";

  /** Represents a computed firing solution. */
  public static class FiringSolution {
    public final double time;
    public final double shooterSpeedMps;
    public final double shooterAngularVelocityRadPerSec;
    public final Rotation2d turretYaw;
    public final Rotation2d hoodPitch;
    public final Transform3d initialVelocityPose;

    public FiringSolution(
        double time,
        double shooterSpeedMps,
        double shooterAngularVelocityRadPerSec,
        Rotation2d turretYaw,
        Rotation2d hoodPitch,
        Transform3d initialVelocityPose) {
      this.time = time;
      this.shooterSpeedMps = shooterSpeedMps;
      this.shooterAngularVelocityRadPerSec = shooterAngularVelocityRadPerSec;
      this.turretYaw = turretYaw;
      this.hoodPitch = hoodPitch;
      this.initialVelocityPose = initialVelocityPose;
    }

    @Override
    public String toString() {
      Translation3d t = initialVelocityPose.getTranslation();
      Rotation3d r = initialVelocityPose.getRotation();
      return String.format(
          "t=%.3fs, speed=%.2fm/s (%.2f rad/s), yaw=%.2f° (%.2f rad), pitch=%.2f° (%.2f rad), "
              + "vel=[%.2f, %.2f, %.2f] m, rot=[%.2f°, %.2f°, %.2f°]",
          time,
          shooterSpeedMps,
          shooterAngularVelocityRadPerSec,
          turretYaw.getDegrees(),
          turretYaw.getRadians(),
          hoodPitch.getDegrees(),
          hoodPitch.getRadians(),
          t.getX(),
          t.getY(),
          t.getZ(),
          Math.toDegrees(r.getX()),
          Math.toDegrees(r.getY()),
          Math.toDegrees(r.getZ()));
    }
  }

  /** Constructor with shooter speed limits. */
  public FiringSolutionSolver(double minShooterSpeed, double maxShooterSpeed) {
    this.minShooterSpeed = minShooterSpeed;
    this.maxShooterSpeed = maxShooterSpeed;
  }

  /** Set hood pitch limits once known. */
  public void setHoodPitchLimits(double minDeg, double maxDeg) {
    this.minHoodPitchRad = Math.toRadians(minDeg);
    this.maxHoodPitchRad = Math.toRadians(maxDeg);
  }

  /** Get reason why last shot was impossible. */
  public String getLastImpossibleReason() {
    return lastImpossibleReason;
  }

  /**
   * Computes the optimal firing solution for a moving robot and stationary target.
   *
   * @param targetPose Target pose (robot frame)
   * @param robotVelocity Current robot velocity (robot frame, m/s)
   * @return Optional containing firing solution, or empty if physically impossible
   */
  public Optional<FiringSolution> solve(Transform3d targetPose, Translation3d robotVelocity) {

    lastImpossibleReason = "No solution found"; // default reason
    FiringSolution best = null;

    // Track which warnings we already reported to avoid spamming logs
    Set<String> reportedWarnings = new HashSet<>();

    Translation3d translationToTarget = targetPose.getTranslation();

    for (double t = minTime; t <= maxTime; t += dt) {

      double vx = (translationToTarget.getX() - robotVelocity.getX() * t) / t;
      double vy = (translationToTarget.getY() - robotVelocity.getY() * t) / t;
      double vz =
          (translationToTarget.getZ() - robotVelocity.getZ() * t + 0.5 * GRAVITY * t * t) / t;

      double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);

      if (speed < minShooterSpeed || speed > maxShooterSpeed) {
        lastImpossibleReason = "Shooter speed out of range";
        if (!reportedWarnings.contains("Shooter speed")) {
          DriverStation.reportWarning(lastImpossibleReason, false);
          reportedWarnings.add("Shooter speed");
        }
        continue;
      }

      double yaw = Math.atan2(vy, vx);
      if (yaw < -maxTurretYawRad || yaw > maxTurretYawRad) {
        lastImpossibleReason = "Turret yaw out of range";
        if (!reportedWarnings.contains("Turret yaw")) {
          DriverStation.reportWarning(lastImpossibleReason, false);
          reportedWarnings.add("Turret yaw");
        }
        continue;
      }

      double pitch = Math.atan2(vz, Math.hypot(vx, vy));
      if (pitch < minHoodPitchRad || pitch > maxHoodPitchRad) {
        lastImpossibleReason = "Hood pitch out of range";
        if (!reportedWarnings.contains("Hood pitch")) {
          DriverStation.reportWarning(lastImpossibleReason, false);
          reportedWarnings.add("Hood pitch");
        }
        continue;
      }

      double shooterAngVelRadPerSec = speed / FLYWHEEL_RADIUS_METERS;

      Transform3d velocityPose =
          new Transform3d(
              new Translation3d(vx, vy, vz),
              new Rotation3d(0.0, pitch, yaw) // roll=0, pitch=pitch, yaw=yaw
              );

      FiringSolution sol =
          new FiringSolution(
              t,
              speed,
              shooterAngVelRadPerSec,
              new Rotation2d(yaw),
              new Rotation2d(pitch),
              velocityPose);

      if (best == null
          || sol.shooterSpeedMps < best.shooterSpeedMps - 1e-6
          || (Math.abs(sol.shooterSpeedMps - best.shooterSpeedMps) < 1e-6
              && sol.time < best.time)) {
        best = sol;
      }
    }

    return Optional.ofNullable(best);
  }
}
