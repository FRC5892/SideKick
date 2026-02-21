// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.generic.util.LoggedAnalogInput;

import edu.wpi.first.wpilibj.AnalogInput;
import edu.wpi.first.wpilibj.RobotController;

/** Add your docs here. */
public class HardwareAnalogInput extends LoggedAnalogInput {
  private final AnalogInput hardware;

  public HardwareAnalogInput(String name, int id) {
    super(name);
    hardware = new AnalogInput(id);
  }

  @Override
  protected void updateInputs(AnalogInputsAutoLogged inputs) {
    inputs.value = hardware.getAverageVoltage() / RobotController.getVoltage5V();
  }

  @Override
  public LoggedAnalogInput withAverageBits(int bits) {
    hardware.setAverageBits(bits);
    return this;
  }
}
