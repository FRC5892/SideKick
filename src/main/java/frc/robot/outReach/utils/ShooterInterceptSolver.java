package frc.robot.outReach.utils;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation3d;
import java.util.Optional;

/**
 * Utility class for calculating firing solutions for a shooter system.
 *
 * <p>Given a translation to a target and the robot's current velocity, this class computes the
 * projectile initial velocity, turret yaw, and hood pitch required to hit the target accounting for
 * gravity and robot motion.
 *
 * <p>This is designed as a pure math utility: it does NOT interface with any motors.
 */
public class ShooterInterceptSolver {

  /** Gravity acceleration (m/s^2) */
  private static final double GRAVITY = 9.81;

  /** Minimum and maximum shooter linear speeds (m/s) */
  private final double minShooterSpeed;

  private final double maxShooterSpeed;

  /** Solver parameters */
  private final double minTime = 0.05; // Minimum flight time to consider (s)

  private final double maxTime = 5.0; // Maximum flight time to consider (s)
  private final double dt = 0.01; // Iteration step (s)

  /** Represents a computed firing solution. */
  public static class FiringSolution {
    /** Time for projectile to reach target (s) */
    public final double time;

    /** Linear shooter speed required (m/s) */
    public final double shooterSpeed;

    /** Horizontal turret rotation relative to robot forward (+X) */
    public final Rotation2d turretYaw;

    /** Vertical hood rotation (pitch) */
    public final Rotation2d hoodPitch;

    /** Initial velocity vector in robot frame (m/s) */
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

  /**
   * Creates a new solver instance with specified shooter speed limits.
   *
   * @param minShooterSpeed Minimum shooter linear speed (m/s)
   * @param maxShooterSpeed Maximum shooter linear speed (m/s)
   */
  public ShooterInterceptSolver(double minShooterSpeed, double maxShooterSpeed) {
    this.minShooterSpeed = minShooterSpeed;
    this.maxShooterSpeed = maxShooterSpeed;
  }

  /**
   * Computes the firing solution for a moving robot and stationary target.
   *
   * @param translationToTag Translation from shooter origin to target (robot frame, meters)
   * @param robotVelocity Current robot velocity (robot frame, m/s)
   * @return Optional<FiringSolution> containing turret yaw, hood pitch, and shooter speed, or empty
   *     if no feasible solution
   */
  public Optional<FiringSolution> solve(
      Translation3d translationToTag, Translation3d robotVelocity) {
    FiringSolution best = null;

    for (double t = minTime; t <= maxTime; t += dt) {
      // Compute projectile initial velocity to intercept target while robot moves
      double vx = (translationToTag.getX() - robotVelocity.getX() * t) / t;
      double vy = (translationToTag.getY() - robotVelocity.getY() * t) / t;
      double vz = (translationToTag.getZ() - robotVelocity.getZ() * t + 0.5 * GRAVITY * t * t) / t;

      double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);

      // Skip if speed is outside shooter capabilities
      if (speed < minShooterSpeed || speed > maxShooterSpeed) continue;

      double yaw = Math.atan2(vy, vx);
      double pitch = Math.atan2(vz, Math.hypot(vx, vy));

      FiringSolution sol =
          new FiringSolution(
              t, speed, new Rotation2d(yaw), new Rotation2d(pitch), new Translation3d(vx, vy, vz));

      // Choose solution with lowest shooter speed, then shortest flight time
      if (best == null
          || sol.shooterSpeed < best.shooterSpeed - 1e-6
          || (Math.abs(sol.shooterSpeed - best.shooterSpeed) < 1e-6 && sol.time < best.time)) {
        best = sol;
      }
    }

    return Optional.ofNullable(best);
  }

  /**
   * Converts linear shooter speed (m/s) to wheel RPM.
   *
   * @param linearSpeedMps Linear speed of ball leaving flywheel (m/s)
   * @param wheelRadiusMeters Flywheel radius (meters)
   * @return Wheel rotational speed in RPM
   */
  public static double shooterSpeedToRPM(double linearSpeedMps, double wheelRadiusMeters) {
    double omega = linearSpeedMps / wheelRadiusMeters; // rad/s
    return omega * 60.0 / (2.0 * Math.PI); // rpm
  }
}
