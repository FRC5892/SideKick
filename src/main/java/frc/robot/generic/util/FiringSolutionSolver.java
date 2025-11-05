package frc.robot.generic.util;

import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.wpilibj.Timer;
import org.littletonrobotics.junction.Logger;

/**
 * Computes turret firing solutions with tunable drag for real-world FRC shots. Fully
 * AdvantageKit-logged and designed for Python post-processing for tuning.
 */
public final class FiringSolutionSolver {

  // ----------------------
  // Tunable physical constants (dashboard-adjustable)
  // ----------------------
  private static final LoggedTunableNumber kDragCoefficient =
      new LoggedTunableNumber("FiringSolver/DragCoefficient", 0.003);
  private static final LoggedTunableNumber kProjectileArea =
      new LoggedTunableNumber("FiringSolver/ProjectileArea", 0.0015);
  private static final LoggedTunableNumber kProjectileMass =
      new LoggedTunableNumber("FiringSolver/ProjectileMass", 0.18);
  private static final LoggedTunableNumber kLaunchHeight =
      new LoggedTunableNumber("FiringSolver/LaunchHeight", 0.8);
  private static final LoggedTunableNumber kTargetHeight =
      new LoggedTunableNumber("FiringSolver/TargetHeight", 2.3);
  private static final LoggedTunableNumber kMaxExitVelocity =
      new LoggedTunableNumber("FiringSolver/MaxExitVelocity", 30.0);

  // ----------------------
  // Iteration parameters for the solver
  // ----------------------
  private static final LoggedTunableNumber kVelocityIterations =
      new LoggedTunableNumber("FiringSolver/VelocityIterations", 25);
  private static final LoggedTunableNumber kAngleIterations =
      new LoggedTunableNumber("FiringSolver/AngleIterations", 25);
  private static final LoggedTunableNumber kVelocityTolerance =
      new LoggedTunableNumber("FiringSolver/VelocityTolerance", 0.01);
  private static final LoggedTunableNumber kAngleTolerance =
      new LoggedTunableNumber("FiringSolver/AngleTolerance", 1e-4);

  // ----------------------
  // Short-range threshold for drag logging
  // ----------------------
  private static final LoggedTunableNumber kShortRangeDistance =
      new LoggedTunableNumber("FiringSolver/ShortRangeDistance", 5.0); // meters

  // ----------------------
  // Physical constants
  // ----------------------
  private static final double GRAVITY = 9.80665;
  private static final double AIR_DENSITY = 1.225;
  private static final String LOG_PREFIX = "FiringSolver/";

  private FiringSolutionSolver() {}

  /**
   * Computes the yaw, pitch, and exit velocity to hit a target. Only called when shooting; no
   * background loops.
   */
  public static FiringSolution computeFiringSolution(
      Translation3d targetPosition,
      Translation3d robotPose,
      double robotYaw,
      boolean isFieldRelative) {

    if (targetPosition == null || robotPose == null) {
      throw new IllegalArgumentException("Target position and robot pose cannot be null");
    }

    Translation3d relTarget =
        isFieldRelative
            ? fieldToRobotRelative(targetPosition, robotPose, robotYaw)
            : targetPosition;

    double dx = relTarget.getX();
    double dy = relTarget.getY();
    double dz = kTargetHeight.get() - kLaunchHeight.get();

    double flatYaw = Math.atan2(dy, dx);
    double horizontalDistance = Math.hypot(dx, dy);

    // Compute velocity and pitch using iterative solver
    double velocity = estimateExitVelocity(horizontalDistance, dz);
    double pitch = estimateLaunchAngle(horizontalDistance, dz, velocity);

    // Clamp physically achievable outputs
    pitch = Math.max(0.0, Math.min(Math.PI / 2, pitch));
    flatYaw = Math.max(-Math.PI, Math.min(Math.PI, flatYaw));

    // Log solution including short-range drag adjustments
    logSolution(flatYaw, pitch, velocity, horizontalDistance);

    return new FiringSolution(flatYaw, pitch, velocity);
  }

  private static Translation3d fieldToRobotRelative(
      Translation3d fieldTarget, Translation3d robotPose, double robotYaw) {

    Translation3d offset = fieldTarget.minus(robotPose);
    double cosYaw = Math.cos(-robotYaw);
    double sinYaw = Math.sin(-robotYaw);
    double x = offset.getX() * cosYaw - offset.getY() * sinYaw;
    double y = offset.getX() * sinYaw + offset.getY() * cosYaw;

    return new Translation3d(x, y, offset.getZ());
  }

  // ----------------------
  // Iterative solver helpers
  // ----------------------
  private static double estimateExitVelocity(double range, double heightDiff) {
    double v0 = 10.0;
    int iterations = (int) kVelocityIterations.get();
    double dragFactor =
        0.5 * AIR_DENSITY * kDragCoefficient.get() * kProjectileArea.get() / kProjectileMass.get();

    for (int i = 0; i < iterations; i++) {
      double t = range / v0; // rough flight time approximation
      double estDrop = 0.5 * (GRAVITY + dragFactor * v0 * v0) * t * t;
      double error = estDrop - heightDiff;

      v0 -= error * 0.5; // corrective iteration
      v0 = Math.max(2.0, Math.min(kMaxExitVelocity.get(), v0));

      if (Math.abs(error) < kVelocityTolerance.get()) break;
    }
    return v0;
  }

  private static double estimateLaunchAngle(double range, double heightDiff, double velocity) {
    double angle = 0.4; // initial guess
    int iterations = (int) kAngleIterations.get();

    for (int i = 0; i < iterations; i++) {
      double sin = Math.sin(angle);
      double cos = Math.cos(angle);
      double t = range / (velocity * cos);
      double y = velocity * sin * t - 0.5 * GRAVITY * t * t - heightDiff;
      double dyda = velocity * cos * t + 1e-6;

      angle -= y / dyda;

      if (Math.abs(y) < kAngleTolerance.get()) break;
    }
    return angle;
  }

  // ----------------------
  // Logging
  // ----------------------
  private static void logSolution(
      double yaw, double pitch, double velocity, double horizontalDistance) {

    // Short-range shots may not experience significant drag
    double loggedDrag =
        horizontalDistance < kShortRangeDistance.get() ? 0.0 : kDragCoefficient.get();

    Logger.recordOutput(LOG_PREFIX + "YawRad", yaw);
    Logger.recordOutput(LOG_PREFIX + "PitchRad", pitch);
    Logger.recordOutput(LOG_PREFIX + "VelocityMPS", velocity);
    Logger.recordOutput(LOG_PREFIX + "LoggedDragCoefficient", loggedDrag);
    Logger.recordOutput(LOG_PREFIX + "Timestamp", Timer.getFPGATimestamp());

    System.out.printf(
        "Yaw: %.3f rad, Pitch: %.3f rad, Velocity: %.2f m/s, LoggedDrag: %.6f%n",
        yaw, pitch, velocity, loggedDrag);
  }

  /**
   * Manual dashboard logging for hit/miss. // TODO: actually make work with dashboard. thats later
   * me's problem
   *
   * @param hit true if the shot hit, false if missed
   */
  public static void logShotResult(boolean hit) {
    Logger.recordOutput(LOG_PREFIX + "ShotResult", hit ? "Hit" : "Miss");
    System.out.println("Shot recorded as " + (hit ? "HIT" : "MISS"));
  }

  /** Immutable data container for firing solution */
  public record FiringSolution(double yawRadians, double pitchRadians, double exitVelocity) {}
}
