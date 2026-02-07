package frc.robot.generic.util.LoggedTalon.TalonFXS;

import com.ctre.phoenix6.configs.MotionMagicConfigs;
import com.ctre.phoenix6.configs.SlotConfigs;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.configs.TalonFXSConfiguration;
import com.ctre.phoenix6.hardware.TalonFXS;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.NeutralModeValue;
import edu.wpi.first.units.measure.Angle;
import edu.wpi.first.wpilibj.Alert;
import frc.robot.util.LoggedTalon.Follower.PhoenixTalonFollower;
import frc.robot.util.LoggedTalon.LoggedTalon;
import frc.robot.util.LoggedTalon.TalonFX.LoggedTalonFX;
import frc.robot.util.LoggedTalon.TalonFX.PhoenixTalonFX;
import frc.robot.util.LoggedTalon.TalonFX.TalonFXFlywheelSim;
import frc.robot.util.LoggedTalon.TalonFX.TalonFXSimpleMotorSim;
import java.util.function.Function;

/**
 *
 *
 * <h2>TalonFXS motor controller</h1>
 *
 * <p>compliant with AdvantageKit with a few quality of life improvements, designed to keep an api
 * similar to Non-AKit programming
 *
 * <p>contains configuration functions that only apply to a {@link TalonFXS}
 *
 * <p>Depending on the implementation, this object might interface with hardware, a simulation, or a
 * log file
 *
 * <h1>{@link #periodic()} must be called at the beginning of subsystem periodic functions</h1>
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
 *       constructors, like {@link PhoenixTalonFXS#PhoenixTalonFXS}
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
 *       subsystem1 = new Subsystem1(new PhoenixTalonFXS(20, canBus, "Subsystem1Motor"));
 *     }
 *     case SIM -> { // Simulate using Phoenix's high fidelity simulation.
 *     // Other options, like FlywheelSim, are availible
 *       subsystem1 = new Subsystem1(new TalonFXSimpleMotorSim(21, canBus, Subsystem1Motor", 0.5, 1));
 *     }
 *     default -> { // Replay: do nothing
 *       subsystem1 = new Subsystem1(new NoOppTalonFXS("Subsystem1Motor", 0));
 *     }
 *   }
 * }
 * // Subsystem1
 * private final LoggedTalonFXS motor;
 * public Subsystem1(LoggedTalonFXS motor) {
 *   // Look how easy that is!
 *   this.motor = motor
 *    .withConfig(
 *      LoggedTalonFXS.buildStandardConfig(40, 40) // This rest is not needed if this motor does not need pid control
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
 * // Now use it like any other TalonFXS
 * private final PositionVoltage positionControl = new PositionVoltage(0);
 * public Command move(Angle position) {
 *   return runOnce(()->motor.setControl(positionControl.withPosition(position)));
 * }
 * }</pre>
 *
 * @see TalonFXSimpleMotorSim
 * @see TalonFXFlywheelSim
 * @see LoggedTalon
 * @see LoggedTalonFX
 * @see PhoenixTalonFX
 */
// Maintainer notes:
// The file is currently a copy of LoggedTalonFX, with all instances of TalonFX replaced with
// TalonFXS. Thanks CTRE, much appreciated
public abstract class LoggedTalonFXS extends LoggedTalon<LoggedTalonFXS> {

  /**
   * @see LoggedTalon#LoggedTalon
   */
  public LoggedTalonFXS(String name, int followers) {
    super(name, followers);
  }

  /**
   * Build a standard config with a few commonly adjusted values
   *
   * @param statorCurrentLimit Stator current limit in amps This limit directly affects max torque
   * @param supplyCurrentLimit Supply current limit in amps This limit affects brownouts and battery
   *     usage
   * @param invert Motor direction. Default: CCW positive
   * @param neutralMode Neutral mode. Default: Coast
   * @return the built config
   */
  public static TalonFXSConfiguration buildStandardConfig(
      double statorCurrentLimit,
      double supplyCurrentLimit,
      InvertedValue invert,
      NeutralModeValue neutralMode) {
    var config = new TalonFXSConfiguration();
    config.CurrentLimits.StatorCurrentLimit = statorCurrentLimit;
    config.CurrentLimits.SupplyCurrentLimit = supplyCurrentLimit;
    config.MotorOutput.Inverted = invert;
    config.MotorOutput.NeutralMode = neutralMode;
    return config;
  }

  /**
   * Build a standard config with a few commonly adjusted values The NeutralMode is Coast
   *
   * @param statorCurrentLimit Stator current limit in amps. This limit directly affects max torque
   * @param supplyCurrentLimit Supply current limit in amps. This limit affects brownouts and
   *     battery usage
   * @param invert Motor direction. Default: CCW positive
   * @return the built config
   */
  public static TalonFXSConfiguration buildStandardConfig(
      double statorCurrentLimit, double supplyCurrentLimit, InvertedValue invert) {
    return LoggedTalonFXS.buildStandardConfig(
        statorCurrentLimit, supplyCurrentLimit, invert, NeutralModeValue.Coast);
  }

  /**
   * Build a standard config with a few commonly adjusted values The InvertValue is CCW positive
   *
   * @param statorCurrentLimit Stator current limit in amps. This limit directly affects max torque
   * @param supplyCurrentLimit Supply current limit in amps. This limit affects brownouts and
   *     battery usage
   * @param neutralMode Neutral mode. Default: Coast
   * @return the built config
   */
  public static TalonFXSConfiguration buildStandardConfig(
      double statorCurrentLimit, double supplyCurrentLimit, NeutralModeValue neutralMode) {
    return LoggedTalonFXS.buildStandardConfig(
        statorCurrentLimit,
        supplyCurrentLimit,
        InvertedValue.CounterClockwise_Positive,
        neutralMode);
  }

  /**
   * Build a standard config with a few commonly adjusted values The InvertValue is CCW positive
   *
   * @param statorCurrentLimit Stator current limit in amps. This limit directly affects max torque
   * @param supplyCurrentLimit Supply current limit in amps. This limit affects brownouts and
   *     battery usage
   * @return the built config
   */
  public static TalonFXSConfiguration buildStandardConfig(
      double statorCurrentLimit, double supplyCurrentLimit) {
    return LoggedTalonFXS.buildStandardConfig(
        statorCurrentLimit,
        supplyCurrentLimit,
        InvertedValue.CounterClockwise_Positive,
        NeutralModeValue.Coast);
  }

  /**
   * Enable PID and Motion Magic position tuning to this motor. NOOP if {@code Constants.tuningMode
   * != true}
   *
   * @param config the current and default config. This config is assumed to be applied. Slot 0 is
   *     used.
   * @param followers other LoggedTalons that should also get this config. This is useful if
   *     multiple motors share tuning values but are controlled independently (like in a swerve
   *     drivetrain)
   * @return {@code this} for method chaining
   */
  public LoggedTalonFXS withMMPIDTuning(TalonFXConfiguration config, LoggedTalon<?>... followers) {
    return withMMPIDTuning(SlotConfigs.from(config.Slot0), config.MotionMagic);
  }

  protected LoggedTalonFXS self() {
    return this;
  }

  /**
   * Apply a config until it succeeds.
   *
   * @param config the config to apply
   * @return this for daisy-chaining
   */
  public abstract LoggedTalonFXS withConfig(TalonFXSConfiguration config);

  /**
   * Apply a config for simulation. This is ignored by a real IO interface. This should be called
   * after a normal config is applied.
   *
   * @param config Function to generate a config. This will only be called in simulation. Takes in
   *     the current config, modifies it for simulation, and returns it. This is meant to make the
   *     simulation resemble reality as much as possible.
   * @return This for daisy-chaining
   */
  public abstract LoggedTalonFXS withSimConfig(
      Function<TalonFXSConfiguration, TalonFXSConfiguration> config);

  /**
   * Apply a config more quickly, up to 3 times.
   *
   * <p>This is designed for extra tuning logic not covered in {@link #withPIDTunable(SlotConfigs,
   * LoggedTalon...)} or{@link #withMMPIDTuning(SlotConfigs, MotionMagicConfigs, LoggedTalon...)}
   *
   * @param config The config to apply
   */
  public abstract void quickApplyConfig(TalonFXSConfiguration config);
}
