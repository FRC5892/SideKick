package frc.robot.generic.util;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Translation3d;
import org.littletonrobotics.junction.Logger;

/**
 * Computes projectile firing solutions for turreted shooters. Fully WPILib-compliant and
 * AdvantageKit-logged. Supports tunable drag, mass, area, and iterative solver parameters.
 */
public final class FiringSolutionSolver {

  // --- Tunable constants (via AdvantageKit dashboard) ---
  // Drag coefficient (dimensionless)
  private static final LoggedTunableNumber kDragCoefficient =
      new LoggedTunableNumber("FiringSolver/DragCoefficient", 0.003);

  // Projectile cross-sectional area in square meters (m^2)
  private static final LoggedTunableNumber kProjectileArea =
      new LoggedTunableNumber("FiringSolver/ProjectileArea", 0.0015);

  // Projectile mass in kilograms (kg)
  private static final LoggedTunableNumber kProjectileMass =
      new LoggedTunableNumber("FiringSolver/ProjectileMass", 0.18);

  // Launcher height in meters (m)
  private static final LoggedTunableNumber kLaunchHeight =
      new LoggedTunableNumber("FiringSolver/LaunchHeight", 0.8);

  // Target height in meters (m)
  private static final LoggedTunableNumber kTargetHeight =
      new LoggedTunableNumber("FiringSolver/TargetHeight", 2.3);

  // Maximum exit velocity in meters per second (m/s)
  private static final LoggedTunableNumber kMaxExitVelocity =
      new LoggedTunableNumber("FiringSolver/MaxExitVelocity", 30.0);

  // --- Tunable iteration parameters ---
  // Number of velocity iterations (unitless)
  private static final LoggedTunableNumber kVelocityIterationCount =
      new LoggedTunableNumber("FiringSolver/VelocityIterations", 20);

  // Number of angle iterations (unitless)
  private static final LoggedTunableNumber kAngleIterationCount =
      new LoggedTunableNumber("FiringSolver/AngleIterations", 20);

  // Velocity convergence tolerance in meters per second (m/s)
  private static final LoggedTunableNumber kVelocityTolerance =
      new LoggedTunableNumber("FiringSolver/VelocityTolerance", 0.01);

  // Angle convergence tolerance in radians (rad)
  private static final LoggedTunableNumber kAngleTolerance =
      new LoggedTunableNumber("FiringSolver/AngleTolerance", 1e-4);

  private static final double GRAVITY = 9.80665;
  // off by ~0.075 due to humidity, not important enough to fix.
  private static final double AIR_DENSITY = 1.225;

  private FiringSolutionSolver() {}

  public static FiringSolution computeFiringSolution(
      Translation3d targetPosition, boolean isFieldRelative) {

    Translation3d relTarget = targetPosition;

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

    // Create solution once
    FiringSolution solution = new FiringSolution(flatYaw, pitch, velocity);

    // Record EVERYTHING for Bayesian tuner - captures full robot state at shot time
    Logger.recordOutput("FiringSolver/Solution", solution);
    Logger.recordOutput("FiringSolver/Distance", horizontalDistance);
    Logger.recordOutput("FiringSolver/TargetHeight", kTargetHeight.get());
    Logger.recordOutput("FiringSolver/LaunchHeight", kLaunchHeight.get());
    Logger.recordOutput("FiringSolver/DragCoefficient", kDragCoefficient.get());
    Logger.recordOutput("FiringSolver/AirDensity", AIR_DENSITY);
    Logger.recordOutput("FiringSolver/ProjectileMass", kProjectileMass.get());
    Logger.recordOutput("FiringSolver/ProjectileArea", kProjectileArea.get());

    // Return the same solution
    return solution;
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

  /** Logs whether a shot hit or missed. */
  public static void logShotResult(boolean hit) {
    Logger.recordOutput("FiringSolver/Hit", hit);
    
    // Also log timestamp to help tuner detect new shot events
    Logger.recordOutput("FiringSolver/ShotTimestamp", System.currentTimeMillis() / 1000.0);
  }

  public record FiringSolution(double yawRadians, double pitchRadians, double exitVelocity) {}
}
