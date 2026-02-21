package frc.robot.generic.util.LoggedAnalogInput;

import java.util.function.DoubleSupplier;
import org.littletonrobotics.junction.networktables.LoggedNetworkNumber;

public class SimAnalogInput extends LoggedAnalogInput {

  private final DoubleSupplier simValue;

  public SimAnalogInput(String name, DoubleSupplier simValue) {
    super(name);
    this.simValue = simValue;
  }

  @Override
  protected void updateInputs(AnalogInputsAutoLogged inputs) {
    inputs.value = simValue.getAsDouble();
  }

  public static SimAnalogInput fromNT(String name) {
    LoggedNetworkNumber networkValue = new LoggedNetworkNumber("/Tuning/simInput/dio" + name, 0);
    return new SimAnalogInput(name, networkValue::get);
  }

  @Override
  public LoggedAnalogInput withAverageBits(int bits) {
    return this;
  }
}
