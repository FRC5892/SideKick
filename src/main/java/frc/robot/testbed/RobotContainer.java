package frc.robot.testbed;

import frc.robot.generic.util.AbstractRobotContainer;
import frc.robot.generic.util.RobotConfig;

public class RobotContainer implements AbstractRobotContainer {
  public static RobotConfig config =
      RobotConfig.defaultCommandBased(frc.robot.outReach.RobotContainer::new);
}
