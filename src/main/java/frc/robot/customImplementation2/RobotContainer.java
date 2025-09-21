package frc.robot.customImplementation2;

import frc.robot.generic.util.AbstractRobotContainer;
import frc.robot.generic.util.RobotConfig;

public class RobotContainer implements AbstractRobotContainer {
  public static RobotConfig config = RobotConfig.defaultCommandBased(RobotContainer::new);
}
