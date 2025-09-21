package frc.robot.generic.util;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;

public interface AbstractRobotContainer {
  default Command getAutonomousCommand() {
    return Commands.print("Default getAutonomousCommand!");
  }
}
