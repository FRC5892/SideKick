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

package frc.robot.testbed.subsystems;

import static edu.wpi.first.units.Units.*;

import com.ctre.phoenix6.CANBus;
import com.ctre.phoenix6.controls.VelocityVoltage;
import com.ctre.phoenix6.controls.VoltageOut;
import com.revrobotics.spark.SparkBase.ControlType;
import com.revrobotics.spark.SparkBase.PersistMode;
import com.revrobotics.spark.SparkBase.ResetMode;
import com.revrobotics.spark.SparkClosedLoopController;
import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.config.SparkMaxConfig;
import edu.wpi.first.networktables.DoubleSubscriber;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;
import frc.robot.generic.util.LoggedTalon.LoggedTalonFX;
import frc.robot.generic.util.LoggedTalon.NoOppTalonFX;
import frc.robot.generic.util.LoggedTalon.PhoenixTalonFX;
import frc.robot.generic.util.LoggedTalon.SimpleMotorSim;
import frc.robot.testbed.util.MotorDetectUtil;

/**
 * Subsystem for controlling a single test motor (either Spark Max or TalonFX).
 *
 * This subsystem automatically detects the motor controller type at the given CAN ID by
 * interrogating the CAN bus and configures itself accordingly. Supports three control modes:
 * <ul>
 *   <li><b>Voltage Control:</b> Direct voltage output (-12V to +12V)
 *   <li><b>Velocity Control:</b> Closed-loop velocity control (RPM)
 *   <li><b>Position Control:</b> Simple position control without PID - moves slowly at constant
 *       speed until encoder reaches target
 * </ul>
 *
 * The subsystem reads target velocity and position values from NetworkTables dashboard under
 * "TestMotors/{name}/TargetVelocity" and "TestMotors/{name}/TargetPosition". Motor type detection
 * is performed internally and not exposed to the dashboard.
 *
 * <b>Motor Type Detection:</b> Uses {@link MotorDetectUtil#detectIsTalonFX(int)} to determine
 * if the motor is a TalonFX or Spark Max at startup. The appropriate hardware interface is then
 * instantiated based on the detection result. This detection is transparent - the motor just works
 * with whatever controller is present.
 */
public class TestMotor extends SubsystemBase {
  // Position control constants
  private static final double POSITION_MAX_VOLTAGE = 3.0; // Max voltage for position control
  private static final double POSITION_TOLERANCE = 0.05; // Tolerance in rotations
  private static final double POSITION_GAIN = 2.0; // Proportional gain for position control

  private final int canId;
  private final String name;
  private final boolean isTalonFX;
  private final CANBus canBus = new CANBus(); // CAN bus for TalonFX motors

  // Motor controllers
  private SparkMax sparkMotor;
  private SparkClosedLoopController sparkPID;
  private LoggedTalonFX talonMotor;
  private final VoltageOut talonVoltageControl = new VoltageOut(0);
  private final VelocityVoltage talonVelocityControl = new VelocityVoltage(0);

  // Control state
  public double targetVelocity = 0.0; // RPM for velocity control
  public double targetPosition = 0.0; // Rotations for position control
  private double currentOutput = 0.0; // Current voltage output
  private ControlMode controlMode = ControlMode.STOPPED;

  // Dashboard inputs for target values only
  private final DoubleSubscriber velocityInput;
  private final DoubleSubscriber positionInput;

  public enum ControlMode {
    STOPPED,
    VOLTAGE,
    VELOCITY,
    POSITION
  }

  /**
   * Creates a new TestMotor subsystem.
   *
   * @param canId The CAN ID of the motor
   * @param name The name of the motor (for dashboard)
   */
  public TestMotor(int canId, String name) {
    this.canId = canId;
    this.name = name;

    // Detect motor type
    switch (Constants.currentMode) {
      case REAL:
        this.isTalonFX = MotorDetectUtil.detectIsTalonFX(canId);
        System.out.println(
            "[TestMotor] "
                + name
                + " (CAN ID "
                + canId
                + "): Detected as "
                + (isTalonFX ? "TalonFX" : "Spark"));

        if (isTalonFX) {
          talonMotor = new PhoenixTalonFX(canId, canBus, name);
        } else {
          sparkMotor = new SparkMax(canId, MotorType.kBrushless);
          var config = new SparkMaxConfig();
          sparkMotor.configure(config, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
          sparkPID = sparkMotor.getClosedLoopController();
        }
        break;

      case SIM:
        this.isTalonFX = MotorDetectUtil.detectIsTalonFX(canId);
        System.out.println(
            "[TestMotor] "
                + name
                + " (CAN ID "
                + canId
                + "): Simulating as "
                + (isTalonFX ? "TalonFX" : "Spark"));

        if (isTalonFX) {
          talonMotor = new SimpleMotorSim(canId, null, name, 1.0, 1.0);
        } else {
          // For Spark in sim, we'll just create a real Spark object since there's no sim version
          sparkMotor = new SparkMax(canId, MotorType.kBrushless);
          var config = new SparkMaxConfig();
          sparkMotor.configure(config, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
          sparkPID = sparkMotor.getClosedLoopController();
        }
        break;

      default:
        this.isTalonFX = MotorDetectUtil.detectIsTalonFX(canId);
        if (isTalonFX) {
          talonMotor = new NoOppTalonFX(name, canId);
        } else {
          // For replay mode with Spark, use a real Spark in no-op mode
          sparkMotor = new SparkMax(canId, MotorType.kBrushless);
          var config = new SparkMaxConfig();
          sparkMotor.configure(config, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
          sparkPID = sparkMotor.getClosedLoopController();
        }
        break;
    }

    // Set up dashboard inputs for target values only
    // Motor type detection is internal - not exposed to dashboard
    var table = NetworkTableInstance.getDefault().getTable("TestMotors/" + name);
    velocityInput = table.getDoubleTopic("TargetVelocity").subscribe(0.0);
    positionInput = table.getDoubleTopic("TargetPosition").subscribe(0.0);

    // Publish default target values
    table.getDoubleTopic("TargetVelocity").publish().set(0.0);
    table.getDoubleTopic("TargetPosition").publish().set(0.0);
  }

  @Override
  public void periodic() {
    // Read target values from dashboard
    targetVelocity = velocityInput.get();
    targetPosition = positionInput.get();

    // Update motor periodic (for TalonFX)
    if (isTalonFX && talonMotor != null) {
      talonMotor.periodic();
    }
  }

  /** Sets the motor output voltage (-12 to 12 volts). */
  public void setVoltage(double voltage) {
    controlMode = ControlMode.VOLTAGE;
    currentOutput = voltage;

    if (isTalonFX && talonMotor != null) {
      talonMotor.setControl(talonVoltageControl.withOutput(voltage));
    } else if (sparkMotor != null) {
      sparkMotor.setVoltage(voltage);
    }
  }

  /** Sets the motor velocity in RPM. */
  public void setVelocity(double rpm) {
    controlMode = ControlMode.VELOCITY;
    targetVelocity = rpm;

    if (isTalonFX && talonMotor != null) {
      // Convert RPM to rotations per second for TalonFX
      double rps = rpm / 60.0;
      talonMotor.setControl(talonVelocityControl.withVelocity(rps));
    } else if (sparkMotor != null && sparkPID != null) {
      sparkPID.setReference(rpm, ControlType.kVelocity);
    }
  }

  /**
   * Sets the motor position in rotations using simple control without PID.
   *
   * As requested, this moves the motor slowly towards the target position at a constant low
   * speed (max 3V) without using PID control. The motor speed is proportional to the error for
   * smooth approach, and stops when within 0.05 rotations of the target.
   *
   * @param rotations Target position in rotations
   */
  public void setPosition(double rotations) {
    controlMode = ControlMode.POSITION;
    targetPosition = rotations;

    // Simple position control - move slowly towards target without PID
    double currentPosition = getCurrentPosition();

    double error = targetPosition - currentPosition;

    // Proportional voltage based on error, capped at max voltage
    double voltage =
        Math.signum(error) * Math.min(Math.abs(error) * POSITION_GAIN, POSITION_MAX_VOLTAGE);

    // Stop if close enough
    if (Math.abs(error) < POSITION_TOLERANCE) {
      voltage = 0.0;
    }

    setVoltage(voltage);
  }

  /** Gets the current motor position in rotations. */
  private double getCurrentPosition() {
    if (isTalonFX && talonMotor != null) {
      return talonMotor.getPosition().in(Rotations);
    } else if (sparkMotor != null) {
      return sparkMotor.getEncoder().getPosition();
    }
    return 0.0;
  }

  /** Stops the motor. */
  public void stop() {
    controlMode = ControlMode.STOPPED;
    setVoltage(0.0);
  }

  /** Returns a command that sets the motor to a specific voltage. */
  public Command setVoltageCommand(double voltage) {
    return runOnce(() -> setVoltage(voltage));
  }

  /** Returns a command that enables velocity control from dashboard. */
  public Command enableVelocityControlCommand() {
    return runOnce(() -> controlMode = ControlMode.VELOCITY);
  }

  /** Returns a command that enables position control from dashboard. */
  public Command enablePositionControlCommand() {
    return runOnce(() -> controlMode = ControlMode.POSITION);
  }

  /** Returns a command that stops the motor. */
  public Command stopCommand() {
    return runOnce(this::stop);
  }

  public String getName() {
    return name;
  }

  public int getCanId() {
    return canId;
  }

  public boolean isTalonFX() {
    return isTalonFX;
  }
}
