package frc.robot.generic.util.LoggedTalon.TalonFXS;

import com.ctre.phoenix6.configs.MotionMagicConfigs;
import com.ctre.phoenix6.configs.SlotConfigs;
import com.ctre.phoenix6.configs.TalonFXSConfiguration;
import com.ctre.phoenix6.controls.ControlRequest;
import com.ctre.phoenix6.hardware.TalonFXS;
import edu.wpi.first.units.measure.Angle;
import frc.robot.util.LoggedTalon.LoggedTalon;
import frc.robot.util.LoggedTalon.TalonFX.NoOppTalonFX;
import frc.robot.util.LoggedTalon.TalonInputs;
import java.util.function.Function;

/**
 * NOOP implementation of a {@link TalonFXS}
 *
 * <p>This is designed to be used in replay when the logger is replaying all inputs
 *
 * @see LoggedTalon
 * @see LoggedTalonFXS
 * @see NoOppTalonFX
 */
// Maintainer notes:
// The file is currently a copy of LoggedTalonFX, with all instances of TalonFX replaced with
// TalonFXS. Thanks CTRE, much appreciated
public class NoOppTalonFXS extends LoggedTalonFXS {

  /**
   * Create a TalonFXS that does nothing.
   *
   * <p>This is design for replay purposes where the logger will supply inputs.
   *
   * @param name The name of this instance. This <strong>MUST</strong> match the real and sim
   *     implementations.
   * @param followers The number of followers. This <strong>MUST</strong> match the real and sim
   *     implementations.
   */
  public NoOppTalonFXS(String name, int followers) {
    super(name, followers);
  }

  /** {@inheritDoc} */
  @Override
  public void setControl(ControlRequest controlRequest) {}

  /** {@inheritDoc} */
  @Override
  protected void updateInputs(TalonInputs inputs) {}

  /** {@inheritDoc} */
  @Override
  public void quickApplyConfig(SlotConfigs config) {}

  /** {@inheritDoc} */
  @Override
  public void quickApplyConfig(MotionMagicConfigs config) {}

  /** {@inheritDoc} */
  @Override
  public void setPosition(Angle position) {}

  /** {@inheritDoc} */
  @Override
  public LoggedTalonFXS withConfig(TalonFXSConfiguration config) {
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public LoggedTalonFXS withSimConfig(
      Function<TalonFXSConfiguration, TalonFXSConfiguration> config) {
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public void quickApplyConfig(TalonFXSConfiguration config) {}
}
