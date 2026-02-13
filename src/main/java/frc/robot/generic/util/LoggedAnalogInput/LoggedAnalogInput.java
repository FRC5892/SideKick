// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.generic.util.LoggedAnalogInput;

import java.util.function.DoubleSupplier;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.littletonrobotics.junction.AutoLog;
import org.littletonrobotics.junction.Logger;

/** Add your docs here. */
@RequiredArgsConstructor
public abstract class LoggedAnalogInput implements DoubleSupplier {
  private final AnalogInputsAutoLogged inputs = new AnalogInputsAutoLogged();

  @Getter(value = AccessLevel.PROTECTED)
  private final String name;

  public void periodic() {
    updateInputs(inputs);
    Logger.processInputs("DigitalInput/" + name, inputs);
  }

  protected abstract void updateInputs(AnalogInputsAutoLogged inputs);

  @AutoLog
  protected static class AnalogInputs {
    double value;
  }

  /**
   * Get the percentage of this analog channel
   *
   * @return the percentage
   */
  public double get() {
    return inputs.value;
  }

  @Override
  public double getAsDouble() {
    return get();
  }
}
