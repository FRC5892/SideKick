package frc.robot.generic.util.LoggedTalon.TalonFXS;

import com.ctre.phoenix6.CANBus;
import com.ctre.phoenix6.configs.TalonFXSConfiguration;
import com.ctre.phoenix6.sim.TalonFXSSimState;
import edu.wpi.first.wpilibj.RobotController;
import frc.robot.util.LoggedTalon.Follower.PhoenixTalonFollower;
import frc.robot.util.LoggedTalon.TalonInputs;
import java.util.function.Function;

/**
 * The base of all high-fidelity TalonFXS simulations.TalonFXFlywheelSim
 *
 * <p>Use {@link #motorSimState}, {@link #afterConfigApplied(TalonFXSConfiguration)}, and {@link
 * #simulationPeriodic(TalonInputs)}
 */
// Maintainer notes:
// The file is currently a copy of LoggedTalonFX, with all instances of TalonFX replaced with
// TalonFXS. Thanks CTRE, much appreciated
public abstract class BaseTalonFXSSim extends PhoenixTalonFXS {
  protected TalonFXSConfiguration config = new TalonFXSConfiguration();

  protected final TalonFXSSimState motorSimState;

  public BaseTalonFXSSim(int canID, CANBus canBus, String name, PhoenixTalonFollower... followers) {
    super(canID, canBus, name, followers);
    motorSimState = super.talonFX[0].getSimState();
  }

  /** {@inheritDoc} */
  @Override
  public LoggedTalonFXS withSimConfig(
      Function<TalonFXSConfiguration, TalonFXSConfiguration> configFunction) {
    withConfig(configFunction.apply(this.config));
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public LoggedTalonFXS withConfig(TalonFXSConfiguration config) {
    // Save the config in case the user wants to modify it with sim-only values
    this.config = config;

    super.withConfig(config);
    afterConfigApplied(config);
    return this;
  }

  /**
   * A function called after a config is changed
   *
   * @param config the new config.
   */
  public void afterConfigApplied(TalonFXSConfiguration config) {}

  @Override
  protected void updateInputs(TalonInputs inputs) {
    motorSimState.setSupplyVoltage(RobotController.getBatteryVoltage());
    simulationPeriodic(inputs);
    super.updateInputs(inputs);
  }

  /**
   * Simulation Periodic function of this instance
   *
   * <p>Update the sim state and
   *
   * <p>This function is called before {@link PhoenixTalonFXS} updates
   *
   * <p>Use {@link #motorSimState} to set the position and velocity of the motor read everywhere
   * else based on the simulation
   *
   * @param inputs Inputs reference that can be read or modified.
   */
  protected abstract void simulationPeriodic(TalonInputs inputs);
}
