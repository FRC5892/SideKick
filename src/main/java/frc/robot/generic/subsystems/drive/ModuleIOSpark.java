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

package frc.robot.generic.subsystems.drive;

import static frc.robot.generic.subsystems.drive.DriveConstants.*;
import static frc.robot.generic.util.SparkUtil.*;

import com.ctre.phoenix6.StatusCode;
import com.ctre.phoenix6.configs.CANcoderConfiguration;
import com.ctre.phoenix6.configs.MagnetSensorConfigs;
import com.ctre.phoenix6.hardware.CANcoder;
import com.ctre.phoenix6.signals.SensorDirectionValue;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.spark.ClosedLoopSlot;
import com.revrobotics.spark.SparkBase;
import com.revrobotics.spark.SparkBase.ControlType;
import com.revrobotics.spark.SparkBase.PersistMode;
import com.revrobotics.spark.SparkBase.ResetMode;
import com.revrobotics.spark.SparkClosedLoopController;
import com.revrobotics.spark.SparkClosedLoopController.ArbFFUnits;
import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.config.ClosedLoopConfig.FeedbackSensor;
import com.revrobotics.spark.config.SparkBaseConfig.IdleMode;
import com.revrobotics.spark.config.SparkMaxConfig;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.filter.Debouncer;
import edu.wpi.first.math.geometry.Rotation2d;
import java.util.Queue;
import java.util.function.DoubleSupplier;
import org.littletonrobotics.junction.Logger;

/**
 * Module IO implementation for Spark Flex drive motor controller, Spark Max turn motor controller,
 * and CANcoder absolute encoder. Adapted for NEO motors (SparkMax) + CTRE CANCoder absolute.
 *
 * <p>CHANGES:
 * - Uses CANCoder absolute for absolute heading
 * - Ensures signed math consistency with zeroRotation
 * - Logs radians/degrees outputs for debugging
 * - Removes illegal imports (Angle/Signal)
 */
public class ModuleIOSpark implements ModuleIO {
  private final Rotation2d zeroRotation;

  private final SparkBase driveSpark;
  private final SparkBase turnSpark;
  private final RelativeEncoder driveEncoder;
  private final RelativeEncoder turnEncoder;

  private final SparkClosedLoopController driveController;
  private final SparkClosedLoopController turnController;

  private final Queue<Double> timestampQueue;
  private final Queue<Double> drivePositionQueue;
  private final Queue<Double> turnPositionQueue;

  private final CANcoder absoluteEncoder;

  private final Debouncer driveConnectedDebounce = new Debouncer(0.5);
  private final Debouncer turnConnectedDebounce = new Debouncer(0.5);

  // Cached cancoder connection status - updated only in resetToAbsolute()
  private boolean lastCancoderConnected = false;

  private final int module;

  public ModuleIOSpark(int module) {
    this.module = module;
    zeroRotation =
        switch (module) {
          case 0 -> frontLeftZeroRotation;
          case 1 -> frontRightZeroRotation;
          case 2 -> backLeftZeroRotation;
          case 3 -> backRightZeroRotation;
          default -> new Rotation2d();
        };

    driveSpark =
        new SparkMax(
            switch (module) {
              case 0 -> frontLeftDriveCanId;
              case 1 -> frontRightDriveCanId;
              case 2 -> backLeftDriveCanId;
              case 3 -> backRightDriveCanId;
              default -> 0;
            },
            MotorType.kBrushless);

    turnSpark =
        new SparkMax(
            switch (module) {
              case 0 -> frontLeftTurnCanId;
              case 1 -> frontRightTurnCanId;
              case 2 -> backLeftTurnCanId;
              case 3 -> backRightTurnCanId;
              default -> 0;
            },
            MotorType.kBrushless);

    absoluteEncoder =
        new CANcoder(
            switch (module) {
              case 0 -> frontLeftCanCoderId;
              case 1 -> frontRightCanCoderId;
              case 2 -> backLeftCanCoderId;
              case 3 -> backRightCanCoderId;
              default -> 0;
            });

    // Configure CANcoder direction (per module)
    absoluteEncoder
        .getConfigurator()
        .apply(
            new CANcoderConfiguration()
                .withMagnetSensor(
                    new MagnetSensorConfigs()
                        .withSensorDirection(
                            module == 0 || module == 2
                                ? SensorDirectionValue.CounterClockwise_Positive
                                : SensorDirectionValue.Clockwise_Positive)));

    driveEncoder = driveSpark.getEncoder();
    turnEncoder = turnSpark.getEncoder();
    driveController = driveSpark.getClosedLoopController();
    turnController = turnSpark.getClosedLoopController();

    var driveConfig = new SparkMaxConfig();
    driveConfig
        .idleMode(IdleMode.kBrake)
        .smartCurrentLimit(driveMotorCurrentLimit)
        .voltageCompensation(12.0);
    driveConfig
        .encoder
        .positionConversionFactor(driveEncoderPositionFactor)
        .velocityConversionFactor(driveEncoderVelocityFactor);
    driveConfig
        .closedLoop
        .feedbackSensor(FeedbackSensor.kPrimaryEncoder)
        .pidf(driveKp, 0.0, driveKd, 0.0);

    tryUntilOk(
        driveSpark,
        5,
        () ->
            driveSpark.configure(
                driveConfig, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters));
    tryUntilOk(driveSpark, 5, () -> driveEncoder.setPosition(0.0));

    var turnConfig = new SparkMaxConfig();
    turnConfig
        .inverted(turnInverted)
        .idleMode(IdleMode.kBrake)
        .smartCurrentLimit(turnMotorCurrentLimit)
        .voltageCompensation(12.0);
    turnConfig
        .encoder
        .positionConversionFactor(turnEncoderPositionFactor)
        .velocityConversionFactor(turnEncoderVelocityFactor);
    turnConfig
        .closedLoop
        .feedbackSensor(FeedbackSensor.kPrimaryEncoder)
        .positionWrappingEnabled(true)
        .positionWrappingInputRange(turnPIDMinInput, turnPIDMaxInput)
        .pidf(turnKp, 0.0, turnKd, 0.0);

    tryUntilOk(
        turnSpark,
        5,
        () ->
            turnSpark.configure(
                turnConfig, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters));

    timestampQueue = SparkOdometryThread.getInstance().makeTimestampQueue();
    drivePositionQueue =
        SparkOdometryThread.getInstance().registerSignal(driveSpark, driveEncoder::getPosition);
    turnPositionQueue =
        SparkOdometryThread.getInstance().registerSignal(turnSpark, turnEncoder::getPosition);

    resetToAbsolute();
  }

  @Override
  public void updateInputs(ModuleIOInputs inputs) {
    sparkStickyFault = false;
    ifOk(driveSpark, driveEncoder::getPosition, value -> inputs.drivePositionRad = value);
    ifOk(driveSpark, driveEncoder::getVelocity, value -> inputs.driveVelocityRadPerSec = value);
    ifOk(
        driveSpark,
        new DoubleSupplier[] {driveSpark::getAppliedOutput, driveSpark::getBusVoltage},
        values -> inputs.driveAppliedVolts = values[0] * values[1]);
    ifOk(driveSpark, driveSpark::getOutputCurrent, value -> inputs.driveCurrentAmps = value);
    inputs.driveConnected = driveConnectedDebounce.calculate(!sparkStickyFault);

    sparkStickyFault = false;
    ifOk(
        turnSpark,
        turnEncoder::getPosition,
        value -> inputs.turnPosition = new Rotation2d(value).minus(zeroRotation));
    ifOk(turnSpark, turnEncoder::getVelocity, value -> inputs.turnVelocityRadPerSec = value);
    ifOk(
        turnSpark,
        new DoubleSupplier[] {turnSpark::getAppliedOutput, turnSpark::getBusVoltage},
        values -> inputs.turnAppliedVolts = values[0] * values[1]);
    ifOk(turnSpark, turnSpark::getOutputCurrent, value -> inputs.turnCurrentAmps = value);
    inputs.turnConnected = turnConnectedDebounce.calculate(!sparkStickyFault);

    inputs.cancoderConnected = lastCancoderConnected;

    inputs.odometryTimestamps = timestampQueue.stream().mapToDouble(Double::doubleValue).toArray();
    inputs.odometryDrivePositionsRad =
        drivePositionQueue.stream().mapToDouble(Double::doubleValue).toArray();
    inputs.odometryTurnPositions =
        turnPositionQueue.stream()
            .map(value -> new Rotation2d(value).minus(zeroRotation))
            .toArray(Rotation2d[]::new);

    timestampQueue.clear();
    drivePositionQueue.clear();
    turnPositionQueue.clear();
  }

  @Override
  public void setDriveOpenLoop(double output) {
    driveSpark.setVoltage(output);
  }

  @Override
  public void setTurnOpenLoop(double output) {
    turnSpark.setVoltage(output);
  }

  @Override
  public void setDriveVelocity(double velocityRadPerSec) {
    double ffVolts = driveKs * Math.signum(velocityRadPerSec) + driveKv * velocityRadPerSec;
    driveController.setReference(
        velocityRadPerSec,
        ControlType.kVelocity,
        ClosedLoopSlot.kSlot0,
        ffVolts,
        ArbFFUnits.kVoltage);
  }

  @Override
  public void setTurnPosition(Rotation2d rotation) {
    double setpoint =
        MathUtil.inputModulus(rotation.plus(zeroRotation).getRadians(), -Math.PI, Math.PI);
    Logger.recordOutput("Drive/Module" + module + "/turn setpoint", setpoint);
    turnController.setReference(setpoint, ControlType.kPosition);
  }

  @Override
  public void resetToAbsolute() {
    var posSignal = absoluteEncoder.getAbsolutePosition();
    posSignal.refresh();
    StatusCode status = posSignal.getStatus();
    lastCancoderConnected = status.isOK();

    if (lastCancoderConnected) {
      double absoluteRadians = Rotation2d.fromRotations(posSignal.getValue()).getRadians();
      double adjustedRadians =
          MathUtil.inputModulus(
              absoluteRadians - zeroRotation.getRadians(), -Math.PI, Math.PI);

      Logger.recordOutput("Drive/Module" + module + "/AbsoluteRadians", absoluteRadians);
      Logger.recordOutput("Drive/Module" + module + "/AdjustedRadians", adjustedRadians);
      Logger.recordOutput(
          "Drive/Module" + module + "/EncoderDegrees", Math.toDegrees(adjustedRadians));

      tryUntilOk(turnSpark, 5, () -> turnEncoder.setPosition(adjustedRadians));
    } else {
      Logger.recordOutput("Drive/Module" + module + "/EncoderStatus", "CANCODER_DISCONNECTED");
    }
  }
}
