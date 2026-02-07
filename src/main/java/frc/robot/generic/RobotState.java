// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.generic;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import lombok.Getter;
import lombok.Setter;

/** Add your docs here. */
public class RobotState {
  private static RobotState instance;

  public static RobotState getInstance() {
    if (instance == null) {
      instance = new RobotState();
    }
    return instance;
  }

  @Getter @Setter private ChassisSpeeds robotRelativeVelocity = new ChassisSpeeds();
  @Getter @Setter private Pose2d robotPosition = new Pose2d();
}
