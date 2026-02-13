package frc.robot.generic.util.LoggedTalon;

import static edu.wpi.first.units.Units.*;

import com.ctre.phoenix6.configs.FeedbackConfigs;
import com.ctre.phoenix6.configs.MotionMagicConfigs;
import com.ctre.phoenix6.configs.Slot0Configs;
import com.ctre.phoenix6.configs.SlotConfigs;
import com.ctre.phoenix6.controls.ControlRequest;
import com.ctre.phoenix6.hardware.TalonFX;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.units.measure.*;
import edu.wpi.first.wpilibj.Alert;
import edu.wpi.first.wpilibj.DriverStation;
import frc.robot.Constants;
import frc.robot.generic.util.LoggedTalon.Follower.PhoenixTalonFollower;
import frc.robot.generic.util.LoggedTalon.TalonFX.LoggedTalonFX;
import frc.robot.generic.util.LoggedTalon.TalonFX.NoOppTalonFX;
import frc.robot.generic.util.LoggedTalon.TalonFX.PhoenixTalonFX;
import frc.robot.generic.util.LoggedTalon.TalonFX.TalonFXFlywheelSim;
import frc.robot.generic.util.LoggedTalon.TalonFX.TalonFXSimpleMotorSim;
import frc.robot.generic.util.LoggedTalon.TalonFXS.LoggedTalonFXS;
import frc.robot.generic.util.LoggedTalon.TalonFXS.NoOppTalonFXS;
import frc.robot.generic.util.LoggedTalon.TalonFXS.PhoenixTalonFXS;
import frc.robot.generic.util.LoggedTunableMeasure;
import frc.robot.generic.util.LoggedTunableNumber;
import java.util.function.Consumer;
import org.littletonrobotics.junction.Logger;
import org.littletonrobotics.junction.inputs.LoggableInputs;

/**
 *
 *
 * <h2>Talon-style motor controller</h1>
 *
 * <p>compliant with AdvantageKit with a few quality of life improvements, designed to keep an api
 * similar to Non-AKit programming
 *
 * <p>Depending on the implementation, this object might interface with hardware, a simulation, or a
 * log file
 *
 * <h1>{@link LoggedTalon#periodic()} must be called at the beginning of subsystem periodic
 * functions</h1>
 *
 * <p>See the specific controllers for configuration and other device specifics.
 *
 * <h4>QOL improvements</h3>
 *
 * <ul>
 *   <li>Method-chaining api to keep constructors concise. See {@link #withPIDTunable(SlotConfigs,
 *       LoggedTalon...)}
 *   <li>Connection alerts to the driver dashboard through wpilib {@link Alert}
 *   <li>Automatic logging of needed data: connection state, applied voltage, torque current, supply
 *       current, temperature, velocity (leader only), position (leader only)
 *   <li>Easy PID and Motion Magic tuning through {@link #withPIDTunable(SlotConfigs,
 *       LoggedTalon...)} and {@link #withMMPIDTuning(SlotConfigs, MotionMagicConfigs,
 *       LoggedTalon...)},
 *   <li>Easy and automatic addition of followers: simply pass {@link PhoenixTalonFollower} into
 *       constructors, like {@link PhoenixTalonFX#PhoenixTalonFX}
 *   <li>Setpoint checking and other math: {@link #atSetpoint(Angle, Angle)}
 * </ul>
 *
 * <h4>Usage</h4>
 *
 * <pre>{@code
 * // RobotContainer
 * private final Subsystem1 subsystem1;
 * private final CANBus canBus = new CANBus("rio");
 * public RobotContainer() {
 *   switch (Constants.currentMode) {
 *     case REAL -> { // Real robot
 *       // Names much match and be unique
 *       subsystem1 = new Subsystem1(new PhoenixTalonFX(20, canBus, "Subsystem1Motor"));
 *     }
 *     case SIM -> { // Simulate using Phoenix's high fidelity simulation.
 *     // Other options, like FlywheelSim, are availible
 *       subsystem1 = new Subsystem1(new TalonFXSimpleMotorSim(21, canBus, Subsystem1Motor", 0.5, 1));
 *     }
 *     default -> { // Replay: do nothing
 *       subsystem1 = new Subsystem1(new NoOppTalonFX("Subsystem1Motor", 0));
 *     }
 *   }
 * }
 * // Subsystem1
 * private final LoggedTalonFX motor;
 * public Subsystem1(LoggedTalonFX motor) {
 *   // Look how easy that is!
 *   this.motor = motor
 *    .withConfig(
 *      LoggedTalonFX.buildStandardConfig(40, 40) // This rest is not needed if this motor does not need pid control
 *      .withSlot0(
 *        new Slot0Configs()
 *        .withKP(0).withKI(0).withKD(0)
 *        .withKS(0).withKV(0)
 *      )
 *     )
 *    .withPIDTunable(SlotConfigs.from(config.Slot0));
 * }
 * @Override
 * public final void periodic() {
 *  motor.periodic();
 * }
 * // Now use it like any other TalonFX
 * private final PositionVoltage positionControl = new PositionVoltage(0);
 * public Command move(Angle position) {
 *   return runOnce(()->motor.setControl(positionControl.withPosition(position)));
 * }
 * }</pre>
 *
 * @see LoggedTalonFX
 * @see LoggedTalonFXS
 * @see PhoenixTalonFX
 * @see PhoenixTalonFXS
 * @see TalonFXSimpleMotorSim
 * @see TalonFXFlywheelSim
 */
public abstract class LoggedTalon<T extends LoggedTalon<T>> {
  protected final String name;
  private final TalonInputsAutoLogged inputs = new TalonInputsAutoLogged();
  private final Alert[] connectionAlerts;
  private boolean pidTuning = false;
  private boolean mmTuning = false;

  private LoggedTunableNumber[] tunableNumbers = null;
  private SlotConfigs tunedConfigs = null;
  private MotionMagicConfigs mmTunedConfigs = null;
  private LoggedTalon<?>[] tuningFollowers = null;
  protected final int followers;

  private final MutAngularVelocity velocity = RadiansPerSecond.mutable(0);
  private final MutAngle position = Radian.mutable(0);

  /**
   * Create a new {@code LoggedTalon}
   *
   * <p>Not designed to be called from user code
   *
   * <p>For replay NOOP, use {@link NoOppTalonFX} or {@link NoOppTalonFXS}
   *
   * @see NoOppTalonFX#NoOppTalonFX
   * @see NoOppTalonFXS#NoOppTalonFXS
   * @see PhoenixTalonFX#PhoenixTalonFX
   * @see PhoenixTalonFXS#PhoenixTalonFXS
   * @see TalonFXSimpleMotorSim#TalonFXSimpleMotorSim
   * @see TalonFXFlywheelSim#TalonFXFlywheelSim
   * @param name The name of this instance. This <strong>MUST</strong> be unique and <strong>MUST
   *     NOT</strong> be changed in replay.
   * @param followers The number of followers this instance has. This is assumed to be accurate.
   *     This <strong>MUST NOT</strong> be changed in replay.
   */
  @SuppressWarnings("resource")
  public LoggedTalon(String name, int followers) {
    this.followers = followers;
    this.name = name;
    this.connectionAlerts = new Alert[followers + 1];
    this.connectionAlerts[0] =
        new Alert("Motor " + name + " is not connected", Alert.AlertType.kError);
    if (followers != 0) {
      for (int i = 1; i <= followers; i++) {
        connectionAlerts[i] =
            new Alert(
                "Motor " + name + " follower " + i + " is not connected", Alert.AlertType.kError);
      }
    }
    inputs.torqueCurrentAmps = new double[followers + 1];
    inputs.temperatureC = new double[followers + 1];
    inputs.connected = new boolean[followers + 1];
    inputs.supplyCurrentAmps = new double[followers + 1];
    inputs.appliedVolts = new double[followers + 1];
    inputs.positionRot = 0;
    inputs.velocityRotPS = 0;
  }

  /**
   * This periodic function must be called at the top of the subsystem's periodic().
   *
   * <p>This function handles connection alerts, tuning changes, and logging (including the
   * all-important {@link Logger#processInputs(String, LoggableInputs)}
   */
  public void periodic() {
    this.updateInputs(inputs);
    Logger.processInputs("Motors/" + name, inputs);
    if (pidTuning) {
      LoggedTunableNumber.ifChanged(this, this::applyAllTuningChanges, tunableNumbers);
    }
    for (int i = 0; i < followers + 1; i++) {
      connectionAlerts[i].set(!inputs.connected[i]);
    }
  }

  /**
   * Apply all tuning changes. This is the callback from {@link
   * LoggedTunableNumber#ifChanged(Object, Consumer, LoggedTunableNumber...)}
   *
   * <p>This function respects {@link #tuningFollowers}
   *
   * @param values array of all tunable numbers. Expected in the order kP, kI, kD, kG, kS, kV, kA,
   *     CruiseVelocity, Acceleration, Jerk
   */
  private void applyAllTuningChanges(double[] values) {
    applyTuningChange(values);
    for (LoggedTalon<?> tuningFollower : tuningFollowers) {
      tuningFollower.applyTuningChange(values);
    }
  }

  /**
   * Apply tuning changes to this device only
   *
   * @param values array of all tunable numbers. In the same order as {@link
   *     #applyAllTuningChanges(double[])}
   */
  private void applyTuningChange(double[] values) {
    if (tunedConfigs != null) {
      tunedConfigs.kP = values[0];
      tunedConfigs.kI = values[1];
      tunedConfigs.kD = values[2];
      tunedConfigs.kG = values[3];
      tunedConfigs.kS = values[4];
      tunedConfigs.kV = values[5];
      tunedConfigs.kA = values[6];
      quickApplyConfig(tunedConfigs);
    }
    if (mmTunedConfigs != null) {
      mmTunedConfigs.MotionMagicCruiseVelocity = values[7];
      mmTunedConfigs.MotionMagicAcceleration = values[8];
      mmTunedConfigs.MotionMagicJerk = values[9];
      quickApplyConfig(mmTunedConfigs);
    }
  }

  /**
   * Enable PID tuning to this motor. NOOP if {@code Constants.tuningMode != true}
   *
   * @param defaultValues the current and default config. This config is assumed to be applied.
   * @param followers other LoggedTalons that should also get this config. This is useful if
   *     multiple motors share tuning values but are controlled independently (like in a swerve
   *     drivetrain)
   * @return {@code this} for method chaining
   */
  public T withPIDTunable(SlotConfigs defaultValues, LoggedTalon<?>... followers) {
    if (!Constants.tuningMode) return self();
    if (pidTuning) {
      DriverStation.reportWarning("Attempted to initiate PID tuning twice", true);
    }
    pidTuning = true;

    this.tunableNumbers =
        new LoggedTunableNumber[] {
          new LoggedTunableNumber(name + "/kP", defaultValues.kP),
          new LoggedTunableNumber(name + "/kI", defaultValues.kI),
          new LoggedTunableNumber(name + "/kD", defaultValues.kD),
          new LoggedTunableNumber(name + "/kG", defaultValues.kG),
          new LoggedTunableNumber(name + "/kS", defaultValues.kS),
          new LoggedTunableNumber(name + "/kV", defaultValues.kV),
          new LoggedTunableNumber(name + "/kA", defaultValues.kA),
        };

    tunedConfigs = defaultValues;
    tuningFollowers = followers;

    return self();
  }

  /**
   * Enable PID tuning to this motor. NOOP if {@code Constants.tuningMode != true}
   *
   * @param config the current and default config. This config is assumed to be applied.
   * @param followers other LoggedTalons that should also get this config. This is useful if
   *     multiple motors share tuning values but are controlled independently (like in a swerve
   *     drivetrain)
   * @return {@code this} for method chaining
   */
  public T withPIDTunable(Slot0Configs config, LoggedTalon<?>... followers) {
    return withPIDTunable(SlotConfigs.from(config), followers);
  }

  /**
   * Enable PID and Motion Magic position tuning to this motor. NOOP if {@code Constants.tuningMode
   * != true}
   *
   * @param slotConfig the current and default config. This config is assumed to be applied.
   * @param mmConfig the current and default config. This config is assumed to be applied.
   * @param followers other LoggedTalons that should also get this config. This is useful if
   *     multiple motors share tuning values but are controlled independently (like in a swerve
   *     drivetrain)
   * @return {@code this} for method chaining
   */
  public T withMMPIDTuning(
      SlotConfigs slotConfig, MotionMagicConfigs mmConfig, LoggedTalon<?>... followers) {
    withPIDTunable(slotConfig, followers);
    if (!Constants.tuningMode) return self();
    this.mmTunedConfigs = mmConfig;
    if (mmTuning) {
      DriverStation.reportWarning("Attempted to initiate Motion Magic tuning twice", true);
    }
    mmTuning = true;

    final LoggedTunableNumber[] mmTunableNumbers =
        new LoggedTunableNumber[] {
          new LoggedTunableNumber(name + "/MM Cruise Velocity", mmConfig.MotionMagicCruiseVelocity),
          new LoggedTunableNumber(name + "/MM Acceleration", mmConfig.MotionMagicAcceleration),
          new LoggedTunableNumber(name + "/MM Jerk", mmConfig.MotionMagicJerk),
        };
    final LoggedTunableNumber[] copy =
        new LoggedTunableNumber[this.tunableNumbers.length + mmTunableNumbers.length];
    System.arraycopy(this.tunableNumbers, 0, copy, 0, this.tunableNumbers.length);
    System.arraycopy(
        mmTunableNumbers, 0, copy, this.tunableNumbers.length, mmTunableNumbers.length);
    this.tunableNumbers = copy;
    return self();
  }

  /**
   * Check if the motor position is within position tolerance. The current position of {@link
   * #getPosition()} is used.
   *
   * <p>This helper function exists because it is unit aware
   *
   * @param setpoint The setpoint.
   * @param tolerance The tolerance to be withing. This should be from a {@link LoggedTunableNumber}
   *     or {@link LoggedTunableMeasure}
   * @return If the current position is withing {@code tolerance} of the setpoint
   */
  public boolean atSetpoint(Angle setpoint, Angle tolerance) {
    return MathUtil.isNear(
        setpoint.baseUnitMagnitude(),
        getPosition().baseUnitMagnitude(),
        tolerance.baseUnitMagnitude());
  }

  /**
   * Check if the motor velocity is within velocity tolerance. The current velocity of {@link
   * #getVelocity()} is used.
   *
   * <p>This helper function exists because it is unit aware
   *
   * @param setpoint The setpoint.
   * @param tolerance The tolerance to be withing. This should be from a {@link LoggedTunableNumber}
   *     or {@link LoggedTunableMeasure}
   * @return If the current velocity is withing {@code tolerance} of the setpoint
   */
  public boolean atSetpoint(AngularVelocity setpoint, AngularVelocity tolerance) {
    return MathUtil.isNear(
        setpoint.baseUnitMagnitude(),
        getVelocity().baseUnitMagnitude(),
        tolerance.baseUnitMagnitude());
  }

  /**
   * Command the motor.
   *
   * <p>This is equivalent to {@link TalonFX#setControl(ControlRequest)}
   *
   * @see TalonFX#setControl(ControlRequest)
   * @param controlRequest The request
   */
  public abstract void setControl(ControlRequest controlRequest);

  /**
   * Update inputs.
   *
   * <p>This is the "periodic" function accessible to the hardware implementation
   *
   * <p>This function should update the supplied inputs reference with the current data available
   *
   * @param inputs The inputs to update
   */
  protected abstract void updateInputs(TalonInputs inputs);

  /**
   * Update SlotConfigs in a timely manner. This function is designed to be used for tuning, and is
   * used internally by {@link #withPIDTunable(SlotConfigs, LoggedTalon...)}
   *
   * @param config The config to apply
   */
  public abstract void quickApplyConfig(SlotConfigs config);

  /**
   * Update MotionMagicConfigs in a timely manner. This function is designed to be used for tuning,
   * and is used internally by {@link #withMMPIDTuning(SlotConfigs, MotionMagicConfigs,
   * LoggedTalon...)}
   *
   * @param config The config to apply
   */
  public abstract void quickApplyConfig(MotionMagicConfigs config);

  public Voltage getPrimaryAppliedVoltage() {
    return getAppliedVoltage(0);
  }

  public Voltage getAppliedVoltage(int follower) {
    return Volt.of(getAppliedVoltageVolts(follower));
  }

  public double getPrimaryAppliedVoltageVolts() {
    return getAppliedVoltageVolts(0);
  }

  public double getAppliedVoltageVolts(int follower) {
    return this.inputs.appliedVolts[follower];
  }

  public Temperature getPrimaryTemperature() {
    return getTemperature(0);
  }

  public Temperature getTemperature(int follower) {
    return Celsius.of(this.inputs.temperatureC[follower]);
  }

  public double getPrimaryTemperatureC() {
    return getTemperatureC(0);
  }

  public double getTemperatureC(int follower) {
    return this.inputs.temperatureC[follower];
  }

  public Current getPrimaryTorqueCurrent() {
    return getTorqueCurrent(0);
  }

  public Current getTorqueCurrent(int follower) {
    return Amp.of(getTorqueCurrentAmps(follower));
  }

  public double getPrimaryTorqueCurrentAmps() {
    return getTorqueCurrentAmps(0);
  }

  public double getTorqueCurrentAmps(int follower) {
    return this.inputs.torqueCurrentAmps[follower];
  }

  public Current getPrimarySupplyCurrent() {
    return getSupplyCurrent(0);
  }

  public Current getSupplyCurrent(int follower) {
    return Amp.of(getSupplyCurrentAmps(follower));
  }

  public double getPrimarySupplyCurrentAmps() {
    return getSupplyCurrentAmps(0);
  }

  public double getSupplyCurrentAmps(int follower) {
    return this.inputs.supplyCurrentAmps[follower];
  }

  /**
   * Get the current position as a measure.
   *
   * <p>This function is equivalent to the {@link TalonFX#getPosition()}status signal, so the value
   * will change depending on {@link FeedbackConfigs#SensorToMechanismRatio}
   *
   * <p>Note: in some simulations (ie {@link TalonFXFlywheelSim}, this will always return 0
   *
   * @return the current position
   */
  public AngularVelocity getVelocity() {
    return velocity.mut_replace(this.inputs.velocityRotPS, RotationsPerSecond);
  }

  /**
   * Get the current position as a measure.
   *
   * <p>This function is equivalent to the {@link TalonFX#getPosition()}status signal, so the value
   * will change depending on {@link FeedbackConfigs#SensorToMechanismRatio}
   *
   * <p>Note: in some simulations (ie {@link TalonFXFlywheelSim}, this will always return 0
   *
   * @return the current position
   */
  public Angle getPosition() {
    return position.mut_replace(this.inputs.positionRot, Rotation);
  }

  /**
   * Set the position of the relative encoder inside the motor. This function acts identically to
   * {@link TalonFX#setPosition(Angle)}
   *
   * @param position value to set to
   */
  public abstract void setPosition(Angle position);

  protected abstract T self();
}
