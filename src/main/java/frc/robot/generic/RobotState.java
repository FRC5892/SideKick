// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.generic;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rectangle2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.testing2026.FieldConstants;
import frc.robot.testing2026.FieldConstants.LinesHorizontal;
import frc.robot.testing2026.FieldConstants.LinesVertical;
import frc.robot.testing2026.subsystems.shooter.ShotCalculator.Goal;
import lombok.Getter;
import lombok.Setter;
import org.littletonrobotics.junction.AutoLogOutput;
import org.littletonrobotics.junction.AutoLogOutputManager;

/** Add your docs here. */
public class RobotState {
  private static RobotState instance;

  public static RobotState getInstance() {
    if (instance == null) {
      instance = new RobotState();
    }
    return instance;
  }

  private RobotState() {
    AutoLogOutputManager.addObject(this);
  }

  @Getter @Setter private boolean autoGoal = true;

  public Goal updateGoal() {
    if (!autoGoal) {
      return goal;
    }
    if (topTarget.contains(robotPosition.getTranslation())) {
      goal = Goal.RIGHT;
    } else if (bottomTarget.contains(robotPosition.getTranslation())) {
      goal = Goal.LEFT;
    } else {
      goal = Goal.HUB;
    }
    return goal;
  }

  public Command setGoalCommand(Goal goal) {
    return Commands.runOnce(() -> setGoal(goal));
  }

  @AutoLogOutput @Getter @Setter private Goal goal;
  @Getter @Setter private ChassisSpeeds robotRelativeVelocity = new ChassisSpeeds();
  @Getter @Setter private Pose2d robotPosition = new Pose2d();

  @AutoLogOutput
  private static final Rectangle2d topTarget =
      new Rectangle2d(
          new Translation2d(LinesVertical.starting, LinesHorizontal.leftTrenchOpenStart),
          new Translation2d(FieldConstants.fieldWidth * 2, LinesHorizontal.center));

  @AutoLogOutput
  private static Rectangle2d bottomTarget =
      new Rectangle2d(
          new Translation2d(LinesVertical.starting, LinesHorizontal.rightTrenchOpenEnd),
          new Translation2d(
              FieldConstants.fieldWidth * 2,
              (2 * LinesHorizontal.center) - LinesHorizontal.center));
}
