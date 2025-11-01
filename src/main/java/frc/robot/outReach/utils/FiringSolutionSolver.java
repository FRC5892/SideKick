package frc.robot.outReach.utils;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.DriverStation;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Utility class for computing firing solutions for a shooter system. Given a target pose relative
 * to robot and the robot’s current velocity, computes projectile initial velocity, turret yaw, hood
 * pitch, accounting for gravity and robot motion. Chooses solution with lowest shooter speed; if
 * tied, shortest flight time.
 */
public class FiringSolutionSolver {

  /** Gravity acceleration (m/s²) */
  private static final double GRAVITY = 9.81;

  /** Shooter speed limits (m/s) */
  private final double minShooterSpeed;

  private final double maxShooterSpeed;

  /** Flight time search parameters */
  private final double minTime = 0.05;

  private final double maxTime = 5.0;
  private final double dt = 0.01;

  /** Turret yaw limits (radians) */
  private final double maxTurretYawRad = Units.degreesToRadians(167.0);

  /** Hood pitch limits (radians) — must be configured for your robot */
  private double minHoodPitchRad = Units.degreesToRadians(0.0);

  private double maxHoodPitchRad = Units.degreesToRadians(90.0);

  /** Flywheel radius (m) — replace with your robot’s value */
  private static final double FLYWHEEL_RADIUS_METERS = 0.0762;

  /** Reason for last impossible shot attempt */
  private String lastImpossibleReason = "";

  /** Holds a valid firing solution. */
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
          "t=%.3fs, speed=%.2fm/s, angularVel=%.2frad/s, yaw=%.2f° (%.2frad), pitch=%.2f° (%.2frad), "
              + "vel=[%.2f, %.2f, %.2f] m/s, rot=[%.2f°, %.2f°, %.2f°]",
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
          Units.radiansToDegrees(r.getX()),
          Units.radiansToDegrees(r.getY()),
          Units.radiansToDegrees(r.getZ()));
    }
  }

  /** Constructor — supply min and max shooter speed in m/s */
  public FiringSolutionSolver(double minShooterSpeed, double maxShooterSpeed) {
    this.minShooterSpeed = minShooterSpeed;
    this.maxShooterSpeed = maxShooterSpeed;
  }

  /** Call this once you know actual hood pitch limits (in degrees) */
  public void setHoodPitchLimits(double minDeg, double maxDeg) {
    this.minHoodPitchRad = Units.degreesToRadians(minDeg);
    this.maxHoodPitchRad = Units.degreesToRadians(maxDeg);
  }

  /** Get reason for last “no solution” */
  public String getLastImpossibleReason() {
    return lastImpossibleReason;
  }

  /**
   * Solve for firing solution given a stationary target and moving robot.
   *
   * @param targetPose The pose of the target relative to robot (in robot coordinate frame)
   * @param robotVelocity Robot velocity vector in robot frame (m/s)
   * @return Optional containing best solution, or empty if no valid solution found
   */
  public Optional<FiringSolution> solve(Transform3d targetPose, Translation3d robotVelocity) {
    lastImpossibleReason = "No solution found";
    FiringSolution best = null;

    Set<String> reportedWarnings = new HashSet<>();
    Translation3d translationToTarget = targetPose.getTranslation();

    for (double t = minTime; t <= maxTime; t += dt) {
      // Compute needed velocity components in robot frame
      double vx = (translationToTarget.getX() - robotVelocity.getX() * t) / t;
      double vy = (translationToTarget.getY() - robotVelocity.getY() * t) / t;
      double vz =
          (translationToTarget.getZ() - robotVelocity.getZ() * t + 0.5 * GRAVITY * t * t) / t;

      double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);

      if (speed < minShooterSpeed || speed > maxShooterSpeed) {
        lastImpossibleReason = "Shooter speed out of range: " + speed;
        if (!reportedWarnings.contains("Shooter speed")) {
          DriverStation.reportWarning(lastImpossibleReason, false);
          reportedWarnings.add("Shooter speed");
        }
        continue;
      }

      double yaw = Math.atan2(vy, vx);
      if (yaw < -maxTurretYawRad || yaw > maxTurretYawRad) {
        lastImpossibleReason = "Turret yaw out of range: " + Units.radiansToDegrees(yaw) + "°";
        if (!reportedWarnings.contains("Turret yaw")) {
          DriverStation.reportWarning(lastImpossibleReason, false);
          reportedWarnings.add("Turret yaw");
        }
        continue;
      }

      double pitch = Math.atan2(vz, Math.hypot(vx, vy));
      if (pitch < minHoodPitchRad || pitch > maxHoodPitchRad) {
        lastImpossibleReason = "Hood pitch out of range: " + Units.radiansToDegrees(pitch) + "°";
        if (!reportedWarnings.contains("Hood pitch")) {
          DriverStation.reportWarning(lastImpossibleReason, false);
          reportedWarnings.add("Hood pitch");
        }
        continue;
      }

      double shooterAngularVelocityRadPerSec = speed / FLYWHEEL_RADIUS_METERS;

      // Create a Transform3d representing the initial velocity vector and orientation
      Transform3d initialVelocityPose =
          new Transform3d(new Translation3d(vx, vy, vz), new Rotation3d(0.0, pitch, yaw));

      FiringSolution sol =
          new FiringSolution(
              t,
              speed,
              shooterAngularVelocityRadPerSec,
              new Rotation2d(yaw),
              new Rotation2d(pitch),
              initialVelocityPose);

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
