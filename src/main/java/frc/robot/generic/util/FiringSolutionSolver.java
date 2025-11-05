package frc.robot.generic.util;

import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.wpilibj.Timer;
import org.littletonrobotics.junction.Logger;

/**
 * Computes projectile firing solutions for turreted shooters. Fully WPILib-compliant and
 * AdvantageKit-logged. Supports tunable drag, mass, area, and iterative solver parameters.
 */
public final class FiringSolutionSolver {

  // --- Tunable constants (via AdvantageKit dashboard) ---
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

  // --- Tunable iteration parameters ---
  private static final LoggedTunableNumber kVelocityIterationCount =
      new LoggedTunableNumber("FiringSolver/VelocityIterations", 20);
  private static final LoggedTunableNumber kAngleIterationCount =
      new LoggedTunableNumber("FiringSolver/AngleIterations", 20);
  private static final LoggedTunableNumber kVelocityTolerance =
      new LoggedTunableNumber("FiringSolver/VelocityTolerance", 0.01);
  private static final LoggedTunableNumber kAngleTolerance =
      new LoggedTunableNumber("FiringSolver/AngleTolerance", 1e-4);

  private static final double GRAVITY = 9.80665;
  private static final double AIR_DENSITY = 1.225;
  private static final String LOG_PREFIX = "FiringSolver/";

  private FiringSolutionSolver() {}

  public static FiringSolution computeFiringSolution(
      Translation3d targetPosition,
      Translation3d robotPose,
      double robotYaw,
      boolean isFieldRelative) {

    Translation3d relTarget =
        isFieldRelative
            ? fieldToRobotRelative(targetPosition, robotPose, robotYaw)
            : targetPosition;

    double dx = relTarget.getX();
    double dy = relTarget.getY();

    // use the real z difference then apply tunable offsets 
    // TODO: Consider removing height offsets once launcher/target calibration is finalized.
    double dz = relTarget.getZ() + (kTargetHeight.get() - kLaunchHeight.get());

    double horizontalDistance = Math.hypot(dx, dy);
    double flatYaw = Math.atan2(dy, dx);

    double velocity = estimateExitVelocity(horizontalDistance, dz);
    double pitch = estimateLaunchAngle(horizontalDistance, dz, velocity);

    // Clamp pitch/yaw to safe turret limits
    pitch = MathUtil.clamp(pitch, 0.0, Math.PI / 2);
    flatYaw = MathUtil.clamp(flatYaw, -Math.PI, Math.PI);

    logSolution(flatYaw, pitch, velocity);
    return new FiringSolution(flatYaw, pitch, velocity);
  }

  private static Translation3d fieldToRobotRelative(
      Translation3d fieldTarget, Translation3d robotPose, double robotYaw) {

    double dx = fieldTarget.getX() - robotPose.getX();
    double dy = fieldTarget.getY() - robotPose.getY();
    double dz = fieldTarget.getZ() - robotPose.getZ();

    double cosA = Math.cos(-robotYaw);
    double sinA = Math.sin(-robotYaw);

    double robotX = dx * cosA - dy * sinA;
    double robotY = dx * sinA + dy * cosA;

    return new Translation3d(robotX, robotY, dz);
  }

  private static double estimateExitVelocity(double range, double heightDiff) {
    double v0 = 10.0;
    int iterations = (int) kVelocityIterationCount.get();
    for (int i = 0; i < iterations; i++) {
      double dragAccel =
          0.5
              * AIR_DENSITY
              * kDragCoefficient.get()
              * kProjectileArea.get()
              * v0
              * v0
              / kProjectileMass.get();
      double t = range / v0; // rough time estimate
      double estDrop = 0.5 * GRAVITY * t * t + 0.5 * dragAccel * t * t;
      double error = estDrop - heightDiff;
      v0 -= error * 0.5;
      v0 = Math.max(2.0, Math.min(kMaxExitVelocity.get(), v0));
      if (Math.abs(error) < kVelocityTolerance.get()) break;
    }
    return v0;
  }

  private static double estimateLaunchAngle(double range, double heightDiff, double velocity) {
    double angle = 0.4;
    int iterations = (int) kAngleIterationCount.get();
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

  private static void logSolution(double yaw, double pitch, double velocity) {
    Logger.recordOutput(LOG_PREFIX + "YawRad", yaw);
    Logger.recordOutput(LOG_PREFIX + "PitchRad", pitch);
    Logger.recordOutput(LOG_PREFIX + "VelocityMPS", velocity);
    Logger.recordOutput(LOG_PREFIX + "Timestamp", Timer.getFPGATimestamp());
  }

  public static void logHit(boolean hit) {
    Logger.recordOutput(LOG_PREFIX + "ShotResult", hit ? "Hit" : "Miss");
  }

  public record FiringSolution(double yawRadians, double pitchRadians, double exitVelocity) {}
}
