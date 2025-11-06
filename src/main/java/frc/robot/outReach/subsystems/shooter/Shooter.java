// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.outReach.subsystems.shooter;

import com.ctre.phoenix6.configs.CurrentLimitsConfigs;
import com.ctre.phoenix6.configs.FeedbackConfigs;
import com.ctre.phoenix6.configs.MotionMagicConfigs;
import com.ctre.phoenix6.configs.MotorOutputConfigs;
import com.ctre.phoenix6.configs.Slot0Configs;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.MotionMagicVoltage;
import com.ctre.phoenix6.signals.NeutralModeValue;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.generic.util.LoggedDIO.LoggedDIO;
import frc.robot.generic.util.LoggedTalon.LoggedTalonFX;
import java.util.function.DoubleSupplier;

public class Shooter extends SubsystemBase {

  private final LoggedTalonFX turretMotor;
  private final MotionMagicVoltage mmControl = new MotionMagicVoltage(0);
  private final LoggedDIO forwardLimit;
  private final LoggedDIO reverseLimit;
  private boolean positionControl = true;

  /** Creates a new Shooter. */
  public Shooter(LoggedTalonFX turretMotor, LoggedDIO forwardLimit, LoggedDIO reverseLimit) {
    var config =
        new TalonFXConfiguration()
            .withCurrentLimits(
                new CurrentLimitsConfigs().withStatorCurrentLimit(20).withSupplyCurrentLimit(10))
            .withSlot0(new Slot0Configs().withKP(0).withKI(0).withKD(0).withKS(0).withKV(0))
            .withMotionMagic(
                new MotionMagicConfigs()
                    .withMotionMagicCruiseVelocity(0.5)
                    .withMotionMagicAcceleration(2))
            .withFeedback(new FeedbackConfigs().withSensorToMechanismRatio(10))
            .withMotorOutput(new MotorOutputConfigs().withNeutralMode(NeutralModeValue.Brake));
    this.turretMotor = turretMotor.withConfig(config).withMMPIDTuning(config);

    this.forwardLimit = forwardLimit.withReversed(true);
    this.reverseLimit = reverseLimit.withReversed(true);
  }

  public Command turnToRotationCommand(DoubleSupplier rotation) {
    return runOnce(
        () -> {
          positionControl = true;
          turretMotor.setControl(
              mmControl
                  .withPosition(rotation.getAsDouble())
                  .withLimitForwardMotion(forwardLimit.get())
                  .withLimitReverseMotion(reverseLimit.get()));
        });
  }

  public Command turnToRotationCommand(double rotation) {
    return runOnce(
        () -> {
          positionControl = true;
          turretMotor.setControl(
              mmControl
                  .withPosition(rotation)
                  .withLimitForwardMotion(forwardLimit.get())
                  .withLimitReverseMotion(reverseLimit.get()));
        });
  }

  @Override
  public void periodic() {
    // This method will be called once per scheduler run
    if (positionControl) {
      turretMotor.setControl(
          mmControl
              .withLimitForwardMotion(forwardLimit.get())
              .withLimitReverseMotion(reverseLimit.get()));
    }
  }
}
