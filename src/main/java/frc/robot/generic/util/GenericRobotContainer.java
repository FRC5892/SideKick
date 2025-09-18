package frc.robot.generic.util;

import edu.wpi.first.wpilibj2.command.Command;

public interface GenericRobotContainer {
  /**
   * Returns the Autonomous Command. This command is automatically scheduled when Autonomous Starts
   */
  Command getAutonomousCommand();

  /** This function is called periodically during all modes. */
  default void robotPeriodic() {}

  /** This function is called once when the robot is first started up. */
  default void simulationInit() {}

  /** This function is called periodically whilst in simulation. */
  default void simulationPeriodic() {}
}
