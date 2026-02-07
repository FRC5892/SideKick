package frc.robot.generic.util.LoggedTalon.TalonFX;

import com.ctre.phoenix6.configs.MotionMagicConfigs;
import com.ctre.phoenix6.configs.SlotConfigs;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.ControlRequest;
import com.ctre.phoenix6.hardware.TalonFX;
import edu.wpi.first.units.measure.Angle;
import frc.robot.util.LoggedTalon.LoggedTalon;
import frc.robot.util.LoggedTalon.TalonFXS.NoOppTalonFXS;
import frc.robot.util.LoggedTalon.TalonInputs;
import java.util.function.Function;

/**
 * NOOP implementation of a {@link TalonFX}
 *
 * <p>This is designed to be used in replay when the logger is replaying all inputs
 *
 * @see LoggedTalon
 * @see LoggedTalonFX
 * @see NoOppTalonFXS
 */
public class NoOppTalonFX extends LoggedTalonFX {

  /**
   * Create a TalonFX that does nothing.
   *
   * <p>This is design for replay purposes where the logger will supply inputs.
   *
   * @param name The name of this instance. This <strong>MUST</strong> match the real and sim
   *     implementations.
   * @param followers The number of followers. This <strong>MUST</strong> match the real and sim
   *     implementations.
   */
  public NoOppTalonFX(String name, int followers) {
    super(name, followers);
  }

  /**
   * Command the motor.
   *
   * <p>This is equivalent to {@link TalonFX#setControl(ControlRequest)}
   *
   * @param controlRequest The request
   * @see TalonFX#setControl(ControlRequest)
   */
  @Override
  public void setControl(ControlRequest controlRequest) {}

  /**
   * Update inputs.
   *
   * <p>This is the "periodic" function accessible to the hardware implementation
   *
   * <p>This function should update the supplied inputs reference with the current data available
   *
   * @param inputs The inputs to update
   */
  @Override
  protected void updateInputs(TalonInputs inputs) {}

  /**
   * Update SlotConfigs in a timely manner. This function is designed to be used for tuning, and is
   * used internally by {@link #withPIDTunable(SlotConfigs, LoggedTalon...)}
   *
   * @param config The config to apply
   */
  @Override
  public void quickApplyConfig(SlotConfigs config) {}

  /**
   * Update MotionMagicConfigs in a timely manner. This function is designed to be used for tuning,
   * and is used internally by {@link #withMMPIDTuning(SlotConfigs, MotionMagicConfigs,
   * LoggedTalon...)}
   *
   * @param config The config to apply
   */
  @Override
  public void quickApplyConfig(MotionMagicConfigs config) {}

  /**
   * Set the position of the relative encoder inside the motor. This function acts identically to
   * {@link TalonFX#setPosition(Angle)}
   *
   * @param position value to set to
   */
  @Override
  public void setPosition(Angle position) {}

  /**
   * Apply a config until it succeeds.
   *
   * @param config the config to apply
   * @return this for daisy-chaining
   */
  @Override
  public LoggedTalonFX withConfig(TalonFXConfiguration config) {
    return this;
  }

  /**
   * Apply a config for simulation. This is ignored by a real IO interface. This should be called
   * after a normal config is applied.
   *
   * @param config Function to generate a config. This will only be called in simulation. Takes in
   *     the current config, modifies it for simulation, and returns it. This is meant to make the
   *     simulation resemble reality as much as possible.
   * @return This for daisy-chaining
   */
  @Override
  public LoggedTalonFX withSimConfig(Function<TalonFXConfiguration, TalonFXConfiguration> config) {
    return this;
  }

  /**
   * Apply a config more quickly, up to 3 times.
   *
   * <p>This is designed for extra tuning logic not covered in {@link #withPIDTunable(SlotConfigs,
   * LoggedTalon...)} or{@link #withMMPIDTuning(SlotConfigs, MotionMagicConfigs, LoggedTalon...)}
   *
   * @param config The config to apply
   */
  @Override
  public void quickApplyConfig(TalonFXConfiguration config) {}
}
