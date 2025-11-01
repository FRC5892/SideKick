package frc.robot.outReach.utils;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation3d;
import java.util.Optional;

/*
 * Utility class for computing firing solutions for a complete shooter system.
 *
 * Given a translation to a target and the robot's current velocity, this class computes the
 * projectile initial velocity, turret yaw, and hood pitch required to hit the target accounting for
 * gravity and robot motion. Does NOT interface with any motors.
 *
 * Currently, this is designed to use the solution with the lowest shooter speed. If there is a
 * tie, it will then select between the two based on which solution has a shorter flight time.
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

  /** Turret yaw limits (degrees) */
  private final double maxTurretYawDeg = 167.0;

  /** Hood pitch limits (degrees) - update when known */
  private double minHoodPitchDeg = -90.0; // placeholder

  private double maxHoodPitchDeg = 90.0; // placeholder

  /** Stores the reason a shot is impossible */
  private String lastImpossibleReason = "";

  /** Represents a computed firing solution. */
  public static class FiringSolution {
    public final double time;
    public final double shooterSpeed;
    public final Rotation2d turretYaw;
    public final Rotation2d hoodPitch;
    public final Translation3d initialVelocity;

    public FiringSolution(
        double time,
        double shooterSpeed,
        Rotation2d turretYaw,
        Rotation2d hoodPitch,
        Translation3d initialVelocity) {
      this.time = time;
      this.shooterSpeed = shooterSpeed;
      this.turretYaw = turretYaw;
      this.hoodPitch = hoodPitch;
      this.initialVelocity = initialVelocity;
    }

    @Override
    public String toString() {
      return String.format(
          "t=%.3fs, speed=%.2fm/s, yaw=%.2f°, pitch=%.2f°",
          time, shooterSpeed, turretYaw.getDegrees(), hoodPitch.getDegrees());
    }
  }

  /** Constructor with shooter speed limits. */
  public FiringSolutionSolver(double minShooterSpeed, double maxShooterSpeed) {
    this.minShooterSpeed = minShooterSpeed;
    this.maxShooterSpeed = maxShooterSpeed;
  }

  /** Set hood pitch limits once known. */
  public void setHoodPitchLimits(double minDeg, double maxDeg) {
    this.minHoodPitchDeg = minDeg;
    this.maxHoodPitchDeg = maxDeg;
  }

  /** Get reason why last shot was impossible. */
  public String getLastImpossibleReason() {
    return lastImpossibleReason;
  }

  /**
   * Computes the optimal firing solution for a moving robot and stationary target.
   *
   * @param translationToTarget Translation from shooter to target (robot frame, meters)
   * @param robotVelocity Current robot velocity (robot frame, m/s)
   * @return Optional containing firing solution, or empty if physically impossible
   */
  public Optional<FiringSolution> solve(
      Translation3d translationToTarget, Translation3d robotVelocity) {

    lastImpossibleReason = "No solution found"; // default reason
    FiringSolution best = null;

    for (double t = minTime; t <= maxTime; t += dt) {
      double vx = (translationToTarget.getX() - robotVelocity.getX() * t) / t;
      double vy = (translationToTarget.getY() - robotVelocity.getY() * t) / t;
      double vz =
          (translationToTarget.getZ() - robotVelocity.getZ() * t + 0.5 * GRAVITY * t * t) / t;

      double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);

      if (speed < minShooterSpeed || speed > maxShooterSpeed) {
        lastImpossibleReason = "Shooter speed out of range";
        continue;
      }

      double yawDeg = Math.toDegrees(Math.atan2(vy, vx));
      if (yawDeg < -maxTurretYawDeg || yawDeg > maxTurretYawDeg) {
        lastImpossibleReason = "Turret yaw out of range";
        continue;
      }
      double yaw = Math.toRadians(yawDeg);

      double pitchDeg = Math.toDegrees(Math.atan2(vz, Math.hypot(vx, vy)));
      if (pitchDeg < minHoodPitchDeg || pitchDeg > maxHoodPitchDeg) {
        lastImpossibleReason = "Hood pitch out of range";
        continue;
      }
      double pitch = Math.toRadians(pitchDeg);

      FiringSolution sol =
          new FiringSolution(
              t, speed, new Rotation2d(yaw), new Rotation2d(pitch), new Translation3d(vx, vy, vz));

      if (best == null
          || sol.shooterSpeed < best.shooterSpeed - 1e-6
          || (Math.abs(sol.shooterSpeed - best.shooterSpeed) < 1e-6 && sol.time < best.time)) {
        best = sol;
      }
    }

    return Optional.ofNullable(best);
  }

  /** Converts linear shooter speed (m/s) to wheel RPM. */
  public static double shooterSpeedToRPM(double linearSpeedMps, double wheelRadiusMeters) {
    double omega = linearSpeedMps / wheelRadiusMeters; // rad/s
    return omega * 60.0 / (2.0 * Math.PI); // rpm
  }
}
