package frc.robot.generic.util.LoggedTalon.TalonFXS;

import com.ctre.phoenix6.BaseStatusSignal;
import com.ctre.phoenix6.CANBus;
import com.ctre.phoenix6.StatusSignal;
import com.ctre.phoenix6.configs.MotionMagicConfigs;
import com.ctre.phoenix6.configs.SlotConfigs;
import com.ctre.phoenix6.configs.TalonFXSConfiguration;
import com.ctre.phoenix6.controls.ControlRequest;
import com.ctre.phoenix6.controls.Follower;
import com.ctre.phoenix6.hardware.TalonFXS;
import com.ctre.phoenix6.signals.MotorAlignmentValue;
import edu.wpi.first.math.filter.Debouncer;
import edu.wpi.first.units.measure.*;
import frc.robot.generic.util.LoggedTalon.Follower.PhoenixTalonFollower;
import frc.robot.generic.util.LoggedTalon.TalonInputs;
import frc.robot.generic.util.PhoenixUtil;
import java.util.function.Function;

// Maintainer notes:
// The file is currently a copy of LoggedTalonFX, with all instances of TalonFX replaced with
// TalonFXS. Thanks CTRE, much appreciated
public class PhoenixTalonFXS extends LoggedTalonFXS {
  protected final TalonFXS[] talonFX;
  private final Debouncer[] connectionDebouncer;

  private final StatusSignal<Voltage>[] voltageSignal;
  private final StatusSignal<Current>[] torqueCurrentSignal;
  private final StatusSignal<Current>[] supplyCurrentSignal;
  private final StatusSignal<Temperature>[] temperatureSignal;
  private final StatusSignal<AngularVelocity> velocitySignal;
  private final StatusSignal<Angle> positionSignal;

  /**
   * Create a TalonFXS that actually interacts with hardware (or CRTE's high fidelity simulation)
   *
   * @param canID The motor's CAN ID
   * @param canBus The motor's CAN Bus
   * @param name The Motors Name. This <strong>MUST NOT</strong> be changed in replay.
   * @param followers Followers, if any. Followers will share the same output as the leader. All
   *     followers are designed to be physically connected to the leader and as such their velocity
   *     and position are not accessible separately. The current number off followers
   *     <strong>MUST</strong> be passed into simulation and replay.
   */
  @SuppressWarnings({"unchecked", "resource"})
  public PhoenixTalonFXS(int canID, CANBus canBus, String name, PhoenixTalonFollower... followers) {

    super(name, followers.length);

    talonFX = new TalonFXS[followers.length + 1];
    connectionDebouncer = new Debouncer[followers.length + 1];
    // The trust me bro guarantee
    voltageSignal = (StatusSignal<Voltage>[]) new StatusSignal[followers.length + 1];
    torqueCurrentSignal = (StatusSignal<Current>[]) new StatusSignal[followers.length + 1];
    supplyCurrentSignal = (StatusSignal<Current>[]) new StatusSignal[followers.length + 1];
    temperatureSignal = (StatusSignal<Temperature>[]) new StatusSignal[followers.length + 1];

    Follower follower = new Follower(canID, MotorAlignmentValue.Aligned);
    for (int i = 0; i <= followers.length; i++) {
      if (i == 0) {
        talonFX[0] = new TalonFXS(canID, canBus);
      } else {
        talonFX[i] = new TalonFXS(followers[i - 1].canid(), canBus);
        talonFX[i].setControl(follower.withMotorAlignment(followers[i - 1].opposeDirection()));
      }
      connectionDebouncer[i] = new Debouncer(0.5);
      voltageSignal[i] = talonFX[i].getMotorVoltage();
      torqueCurrentSignal[i] = talonFX[i].getTorqueCurrent();
      supplyCurrentSignal[i] = talonFX[i].getSupplyCurrent();
      temperatureSignal[i] = talonFX[i].getDeviceTemp();
      BaseStatusSignal.setUpdateFrequencyForAll(
          PhoenixUtil.kRioSignalUpdateFrequency,
          voltageSignal[i],
          torqueCurrentSignal[i],
          supplyCurrentSignal[i],
          temperatureSignal[i]);
      talonFX[i].optimizeBusUtilization(PhoenixUtil.kOptimizedSignalFrequency);
    }
    velocitySignal = talonFX[0].getVelocity();
    positionSignal = talonFX[0].getPosition();

    PhoenixUtil.registerSignals(canBus, voltageSignal);
    PhoenixUtil.registerSignals(canBus, torqueCurrentSignal);
    PhoenixUtil.registerSignals(canBus, supplyCurrentSignal);
    PhoenixUtil.registerSignals(canBus, velocitySignal, positionSignal);
  }

  /** {@inheritDoc} */
  @Override
  public void setControl(ControlRequest controlRequest) {
    talonFX[0].setControl(controlRequest);
  }

  @Override
  protected void updateInputs(TalonInputs inputs) {
    for (int i = 0; i <= super.followers; i++) {
      inputs.connected[i] =
          connectionDebouncer[i].calculate(
              BaseStatusSignal.isAllGood(
                      voltageSignal[i],
                      torqueCurrentSignal[i],
                      supplyCurrentSignal[i],
                      temperatureSignal[i])
                  && (i != 0 || BaseStatusSignal.isAllGood(positionSignal, velocitySignal)));
      inputs.appliedVolts[i] = voltageSignal[i].getValueAsDouble();
      inputs.torqueCurrentAmps[i] = torqueCurrentSignal[i].getValueAsDouble();
      inputs.supplyCurrentAmps[i] = supplyCurrentSignal[i].getValueAsDouble();
      inputs.temperatureC[i] = temperatureSignal[i].getValueAsDouble();
    }
    inputs.positionRot = positionSignal.getValueAsDouble();
    inputs.velocityRotPS = velocitySignal.getValueAsDouble();
  }

  /** {@inheritDoc} */
  @Override
  public LoggedTalonFXS withConfig(TalonFXSConfiguration config) {
    PhoenixUtil.tryUntilOk(5, () -> talonFX[0].getConfigurator().apply(config));
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
  public void quickApplyConfig(TalonFXSConfiguration config) {
    PhoenixUtil.tryUntilOk(3, () -> talonFX[0].getConfigurator().apply(config));
  }

  /** {@inheritDoc} */
  @Override
  public void quickApplyConfig(SlotConfigs config) {
    PhoenixUtil.tryUntilOk(3, () -> talonFX[0].getConfigurator().apply(config));
  }

  /** {@inheritDoc} */
  @Override
  public void quickApplyConfig(MotionMagicConfigs config) {
    PhoenixUtil.tryUntilOk(3, () -> talonFX[0].getConfigurator().apply(config));
  }

  /** {@inheritDoc} */
  @Override
  public void setPosition(Angle position) {
    talonFX[0].setPosition(position);
  }
}
