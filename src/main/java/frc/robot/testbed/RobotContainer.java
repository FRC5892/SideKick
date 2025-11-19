// Copyright 2021-2025 FRC 6328
// http://github.com/Mechanical-Advantage
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// version 3 as published by the Free Software Foundation or
// available in the root directory of this project.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.

package frc.robot.testbed;

import com.pathplanner.lib.auto.AutoBuilder;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.generic.subsystems.drive.Drive;
import frc.robot.generic.util.AbstractRobotContainer;
import frc.robot.generic.util.RobotConfig;
import frc.robot.generic.util.SwerveBuilder;
import frc.robot.testbed.subsystems.TestMotor;
import java.util.function.DoubleSupplier;
import org.littletonrobotics.junction.networktables.LoggedDashboardChooser;

/**
 * Testbed robot container for remotely testing motors and prototypes on a swerve drivetrain
 * without any programming work.
 *
 * This testbed provides:
 * <ul>
 *   <li>Generic swerve drive controlled by driver controller (port 0)
 *   <li>4 motors bound to codriver joysticks for direct voltage control (CAN IDs 10-13)
 *   <li>2 motors bound to codriver paddles for synchronized forward/backward control (CAN IDs
 *       14-15)
 *   <li>4 motors bound to codriver buttons with dashboard-settable velocities/positions (CAN IDs
 *       20-23)
 *   <li>Automatic motor type detection (TalonFX vs Spark Max) via CAN bus interrogation
 *   <li>Emergency stop controls for safety
 * </ul>
 *
 * <b>Codriver Controller Layout (Port 1):</b>
 * <ul>
 *   <li>Left Joystick Y -> JoystickMotor1 (CAN 10)
 *   <li>Left Joystick X -> JoystickMotor2 (CAN 11)
 *   <li>Right Joystick Y -> JoystickMotor3 (CAN 12)
 *   <li>Right Joystick X -> JoystickMotor4 (CAN 13)
 *   <li>Left Trigger (Paddle) -> PaddleMotor1 & PaddleMotor2 forward (CAN 14-15)
 *   <li>Right Trigger (Paddle) -> PaddleMotor1 & PaddleMotor2 backward (CAN 14-15)
 *   <li>A Button -> ButtonMotor1 velocity control from dashboard (CAN 20)
 *   <li>B Button -> ButtonMotor2 velocity control from dashboard (CAN 21)
 *   <li>X Button -> ButtonMotor3 position control from dashboard (CAN 22)
 *   <li>Y Button -> ButtonMotor4 position control from dashboard (CAN 23)
 *   <li>Left Bumper -> Stop paddle motors
 *   <li>Right Bumper -> Stop joystick motors
 *   <li>Back Button -> EMERGENCY STOP ALL TEST MOTORS
 * </ul>
 *
 * <b>Dashboard Control:</b> Button-controlled motors read target velocity (RPM) or position
 * (rotations) from NetworkTables at "TestMotors/{MotorName}/TargetVelocity" and
 * "TestMotors/{MotorName}/TargetPosition". Position control moves slowly without PID until the
 * encoder reaches the target.
 *
 * <b>Motor Detection:</b> Each motor's type (TalonFX or Spark Max) is automatically detected at
 * startup by interrogating the CAN bus. Motors are configured appropriately based on detection
 * results. Detection is transparent - motors just work with whatever controller is present without
 * exposing type information to the dashboard.
 */
public class RobotContainer implements AbstractRobotContainer {
  public static RobotConfig config = RobotConfig.defaultCommandBased(RobotContainer::new);

  // Constants for motor control
  private static final double JOYSTICK_DEADBAND = 0.1;
  private static final double MAX_VOLTAGE = 12.0;

  // Controllers
  private final CommandXboxController driverController = new CommandXboxController(0);
  private final CommandXboxController codriverController = new CommandXboxController(1);

  // Subsystems - using generic swerve drive
  private final Drive drive = SwerveBuilder.buildDefaultDrive(driverController);

  // Test motors for prototype testing
  // 4 motors for joystick control (CAN IDs 10-13)
  private final TestMotor joystickMotor1 = new TestMotor(10, "JoystickMotor1");
  private final TestMotor joystickMotor2 = new TestMotor(11, "JoystickMotor2");
  private final TestMotor joystickMotor3 = new TestMotor(12, "JoystickMotor3");
  private final TestMotor joystickMotor4 = new TestMotor(13, "JoystickMotor4");

  // Paddle motors (CAN IDs 14-15)
  private final TestMotor paddleMotor1 = new TestMotor(14, "PaddleMotor1");
  private final TestMotor paddleMotor2 = new TestMotor(15, "PaddleMotor2");

  // Button-controlled motors with dashboard settings (CAN IDs 20-23)
  private final TestMotor buttonMotor1 = new TestMotor(20, "ButtonMotor1");
  private final TestMotor buttonMotor2 = new TestMotor(21, "ButtonMotor2");
  private final TestMotor buttonMotor3 = new TestMotor(22, "ButtonMotor3");
  private final TestMotor buttonMotor4 = new TestMotor(23, "ButtonMotor4");

  // Dashboard
  private final LoggedDashboardChooser<Command> autoChooser;

  /** The container for the testbed robot. Contains subsystems, OI devices, and commands. */
  public RobotContainer() {
    // Set up auto routines (using generic implementation)
    autoChooser = new LoggedDashboardChooser<>("Auto Choices", AutoBuilder.buildAutoChooser());

    // Configure button bindings
    configureButtonBindings();
  }

  /**
   * Creates a joystick motor control command with deadband.
   *
   * @param motor The motor to control
   * @param axisSupplier Supplier for the joystick axis value
   * @return Command that controls the motor based on axis input
   */
  private Command createJoystickMotorCommand(TestMotor motor, DoubleSupplier axisSupplier) {
    return Commands.run(
        () -> {
          double value = -axisSupplier.getAsDouble();
          if (Math.abs(value) > JOYSTICK_DEADBAND) {
            motor.setVoltage(value * MAX_VOLTAGE);
          } else {
            motor.stop();
          }
        },
        motor);
  }

  /**
   * Creates a paddle motor control command for trigger-based control.
   *
   * @param motor The motor to control
   * @return Command that controls the motor based on trigger inputs
   */
  private Command createPaddleMotorCommand(TestMotor motor) {
    return Commands.run(
        () -> {
          double leftTrigger = codriverController.getLeftTriggerAxis();
          double rightTrigger = codriverController.getRightTriggerAxis();
          if (leftTrigger > JOYSTICK_DEADBAND) {
            motor.setVoltage(leftTrigger * MAX_VOLTAGE);
          } else if (rightTrigger > JOYSTICK_DEADBAND) {
            motor.setVoltage(-rightTrigger * MAX_VOLTAGE);
          } else {
            motor.stop();
          }
        },
        motor);
  }

  /**
   * Configures button bindings for testbed motor control.
   *
   * <b>Joystick Control:</b> The four joystick motors receive continuous voltage control (-12V
   * to +12V) proportional to joystick deflection. A 10% deadband is applied to prevent drift.
   *
   * <b>Paddle Control:</b> The two paddle motors run synchronized - both move forward when left
   * trigger is pressed, both move backward when right trigger is pressed. Speed is proportional to
   * trigger pressure.
   *
   * <b>Button Control:</b> The four button motors are controlled via dashboard values. Press and
   * hold A/B for velocity control or X/Y for position control. Release to stop. Target values are
   * read from NetworkTables.
   *
   * <b>Emergency Stops:</b>
   * <ul>
   *   <li>Left Bumper: Stop paddle motors only
   *   <li>Right Bumper: Stop joystick motors only
   *   <li>Back Button: STOP ALL TEST MOTORS (emergency)
   * </ul>
   */
  private void configureButtonBindings() {
    // ========================================
    // JOYSTICK MOTORS: Direct voltage control from joysticks with deadband
    // ========================================

    joystickMotor1.setDefaultCommand(
        createJoystickMotorCommand(joystickMotor1, codriverController::getLeftY));
    joystickMotor2.setDefaultCommand(
        createJoystickMotorCommand(joystickMotor2, codriverController::getLeftX));
    joystickMotor3.setDefaultCommand(
        createJoystickMotorCommand(joystickMotor3, codriverController::getRightY));
    joystickMotor4.setDefaultCommand(
        createJoystickMotorCommand(joystickMotor4, codriverController::getRightX));

    // ========================================
    // PADDLE MOTORS: Synchronized trigger control
    // ========================================

    paddleMotor1.setDefaultCommand(createPaddleMotorCommand(paddleMotor1));
    paddleMotor2.setDefaultCommand(createPaddleMotorCommand(paddleMotor2));

    // ========================================
    // BUTTON MOTORS: Dashboard-controlled velocity and position
    // ========================================

    // A button: Velocity control for ButtonMotor1 (CAN ID 20)
    // Hold to run at velocity from "TestMotors/ButtonMotor1/TargetVelocity" on dashboard
    codriverController
        .a()
        .whileTrue(
            Commands.run(
                () -> {
                  buttonMotor1.setVelocity(buttonMotor1.targetVelocity);
                },
                buttonMotor1))
        .onFalse(buttonMotor1.stopCommand());

    // B button: Velocity control for ButtonMotor2 (CAN ID 21)
    // Hold to run at velocity from "TestMotors/ButtonMotor2/TargetVelocity" on dashboard
    codriverController
        .b()
        .whileTrue(
            Commands.run(
                () -> {
                  buttonMotor2.setVelocity(buttonMotor2.targetVelocity);
                },
                buttonMotor2))
        .onFalse(buttonMotor2.stopCommand());

    // X button: Position control for ButtonMotor3 (CAN ID 22)
    // Hold to move slowly to position from "TestMotors/ButtonMotor3/TargetPosition" on dashboard
    // Uses simple control without PID - moves at constant slow speed until target reached
    codriverController
        .x()
        .whileTrue(
            Commands.run(
                () -> {
                  buttonMotor3.setPosition(buttonMotor3.targetPosition);
                },
                buttonMotor3))
        .onFalse(buttonMotor3.stopCommand());

    // Y button: Position control for ButtonMotor4 (CAN ID 23)
    // Hold to move slowly to position from "TestMotors/ButtonMotor4/TargetPosition" on dashboard
    // Uses simple control without PID - moves at constant slow speed until target reached
    codriverController
        .y()
        .whileTrue(
            Commands.run(
                () -> {
                  buttonMotor4.setPosition(buttonMotor4.targetPosition);
                },
                buttonMotor4))
        .onFalse(buttonMotor4.stopCommand());

    // ========================================
    // EMERGENCY STOP CONTROLS
    // ========================================

    // Left bumper: Stop paddle motors only
    codriverController
        .leftBumper()
        .onTrue(Commands.runOnce(() -> {
          paddleMotor1.stop();
          paddleMotor2.stop();
        }));

    // Right bumper: Stop joystick motors only
    codriverController
        .rightBumper()
        .onTrue(Commands.runOnce(() -> {
          joystickMotor1.stop();
          joystickMotor2.stop();
          joystickMotor3.stop();
          joystickMotor4.stop();
        }));

    // Back button: EMERGENCY STOP - Stop ALL test motors immediately
    codriverController
        .back()
        .onTrue(Commands.runOnce(() -> {
          joystickMotor1.stop();
          joystickMotor2.stop();
          joystickMotor3.stop();
          joystickMotor4.stop();
          paddleMotor1.stop();
          paddleMotor2.stop();
          buttonMotor1.stop();
          buttonMotor2.stop();
          buttonMotor3.stop();
          buttonMotor4.stop();
        }));
  }

  /**
   * Use this to pass the autonomous command to the main {@link
   * frc.robot.generic.Robot} class.
   *
   * @return the command to run in autonomous
   */
  @Override
  public Command getAutonomousCommand() {
    return autoChooser.get();
  }
}