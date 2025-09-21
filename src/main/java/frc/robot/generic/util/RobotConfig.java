package frc.robot.generic.util;

import edu.wpi.first.wpilibj2.command.CommandScheduler;
import frc.robot.Constants;
import java.util.function.Supplier;

public abstract class RobotConfig {
  /** Entry Point for a robot, can be this class or another. * */
  public abstract Supplier<AbstractRobotContainer> getContainer();

  public static RobotConfig defaultCommandBased(Supplier<AbstractRobotContainer> container) {
    return new RobotConfig() {
      @Override
      public Supplier<AbstractRobotContainer> getContainer() {
        return container;
      }
    };
  }

  /**
   * This function is called once before logging is started. Shouldn't be used for much more than
   * adding metadata. Do not initialize motors or other robot code with this. If used in when {@link
   * #useDefaultLogging()} is {@code false}, adding metadata, logging destinations/sources, and
   * starting logging must happen in here. Make sure to take {@link Constants#currentMode} into
   * account
   */
  public void preLoggingInit() {}

  /**
   * This function is called once after metadata and replay sources/destinations are added, but
   * before logging is started. This could be used to add additional sources/destinations. If so,
   * make sure to check the current robot mode with {@link Constants#currentMode}. Not called
   */
  public void loggerPreStart() {}

  /**
   * Overriding this and returning false will disable all logging initialization, including starting
   * logging. This in conjunction with {@link #preLoggingInit()} allows logging to be completely
   * configured, or disabled
   */
  public boolean useDefaultLogging() {
    return true;
  }

  /** This function is called periodically during all modes, before the command scheduler */
  public void robotPeriodicBeforeScheduler() {}

  /** This function is called periodically during all modes, after the command scheduler */
  public void robotPeriodicAfterScheduler() {}

  /** This function is called once when the robot is disabled. */
  public void disabledInit() {}

  /** This function is called periodically when disabled. */
  public void disabledPeriodic() {}

  /**
   * This function is called when the robot enters autonomous mode. The autonomousCommand command
   * found by {@link AbstractRobotContainer#getAutonomousCommand()} is already scheduled (if not
   * null) and is the preferred way to program an autonomous routine. This function could be used
   * when working without the command scheduler
   */
  public void autonomousInit() {}

  /** This function is called periodically during autonomous. */
  public void autonomousPeriodic() {}

  /**
   * This function is called once when teleop is enabled. The autonomous command is automatically
   * canceled and does not need to be done in this function
   */
  public void teleopInit() {}

  /** This function is called periodically during operator control. */
  public void teleopPeriodic() {}

  /**
   * This function is called once when test mode is enabled. Without overriding, this function
   * cancels all commands
   */
  public void testInit() {
    // Cancels all running commands at the start of test mode.
    CommandScheduler.getInstance().cancelAll();
  }

  /** This function is called periodically during test mode. */
  public void testPeriodic() {}

  /** This function is called once when the robot is first started up. */
  public void simulationInit() {}

  /** This function is called periodically whilst in simulation. */
  public void simulationPeriodic() {}
}
