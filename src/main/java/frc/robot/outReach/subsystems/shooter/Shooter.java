// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.outReach.subsystems.shooter;
import frc.robot.generic.util.LoggedTalon.LoggedTalonFX;

import java.util.function.DoubleSupplier;

import com.ctre.phoenix6.configs.CurrentLimitsConfigs;
import com.ctre.phoenix6.configs.FeedbackConfigs;
import com.ctre.phoenix6.configs.MotorOutputConfigs;
import com.ctre.phoenix6.configs.Slot0Configs;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.NeutralOut;
import com.ctre.phoenix6.controls.VelocityDutyCycle;
import com.ctre.phoenix6.controls.VelocityVoltage;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.NeutralModeValue;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class Shooter extends SubsystemBase {
  private final LoggedTalonFX shooterMotor;
  private final VelocityVoltage velocityPIDRequest = new VelocityVoltage(0);
  private final NeutralOut neutralOut = new NeutralOut();

  /** Creates a new Torrent. */
  public Shooter(LoggedTalonFX shooterMotor) {
    var config = 
      new TalonFXConfiguration()
        .withCurrentLimits(
          new CurrentLimitsConfigs().withStatorCurrentLimit(30).withSupplyCurrentLimit(60))
          .withSlot0(new Slot0Configs().withKP(0).withKI(0).withKD(0).withKS(0).withKV(0))
          .withFeedback(new FeedbackConfigs().withSensorToMechanismRatio(0))
          .withMotorOutput(new MotorOutputConfigs().withNeutralMode(NeutralModeValue.Coast));

    this.shooterMotor = shooterMotor.withConfig(config); 
  }
public Command spinToVelocityAndRotationsCommand(DoubleSupplier velocityRPS) {
  return runEnd(()->{
    shooterMotor.setControl(velocityPIDRequest.withVelocity(velocityRPS.getAsDouble()));
  }, ()-> {
    shooterMotor.setControl(neutralOut);
  });
}
 

@Override
  public void periodic() {
    // This method will be called once per scheduler run
    shooterMotor.periodic();
  }
}
  