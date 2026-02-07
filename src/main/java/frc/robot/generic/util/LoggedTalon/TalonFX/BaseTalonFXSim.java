package frc.robot.generic.util.LoggedTalon.TalonFX;

import com.ctre.phoenix6.CANBus;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.sim.TalonFXSimState;
import edu.wpi.first.wpilibj.RobotController;
import frc.robot.util.LoggedTalon.Follower.PhoenixTalonFollower;
import frc.robot.util.LoggedTalon.TalonInputs;
import java.util.function.Function;

/**
 * The base of all high-fidelity TalonFX simulations.
 *
 * <p>Use {@link #motorSimState}, {@link #afterConfigApplied(TalonFXConfiguration)}, and {@link
 * #simulationPeriodic(TalonInputs)}
 */
public abstract class BaseTalonFXSim extends PhoenixTalonFX {
  protected TalonFXConfiguration config = new TalonFXConfiguration();

  protected final TalonFXSimState motorSimState;

  public BaseTalonFXSim(int canID, CANBus canBus, String name, PhoenixTalonFollower... followers) {
    super(canID, canBus, name, followers);
    motorSimState = super.talonFX[0].getSimState();
  }

  /** {@inheritDoc} */
  @Override
  public LoggedTalonFX withSimConfig(
      Function<TalonFXConfiguration, TalonFXConfiguration> configFunction) {
    withConfig(configFunction.apply(this.config));
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public LoggedTalonFX withConfig(TalonFXConfiguration config) {
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
  public void afterConfigApplied(TalonFXConfiguration config) {}

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
   * <p>This function is called before {@link PhoenixTalonFX} updates
   *
   * <p>Use {@link #motorSimState} to set the position and velocity of the motor read everywhere
   * else based on the simulation
   *
   * @param inputs Inputs reference that can be read or modified.
   */
  protected abstract void simulationPeriodic(TalonInputs inputs);
}
