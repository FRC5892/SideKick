package frc.robot.testing2026.subsystems.shooter;

import static edu.wpi.first.units.Units.Degree;
import static edu.wpi.first.units.Units.Degrees;
import static edu.wpi.first.units.Units.Rotations;

import com.ctre.phoenix6.configs.CurrentLimitsConfigs;
import com.ctre.phoenix6.configs.FeedbackConfigs;
import com.ctre.phoenix6.configs.MotionMagicConfigs;
import com.ctre.phoenix6.configs.MotorOutputConfigs;
import com.ctre.phoenix6.configs.Slot0Configs;
import com.ctre.phoenix6.configs.SlotConfigs;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.MotionMagicTorqueCurrentFOC;
import com.ctre.phoenix6.controls.NeutralOut;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.NeutralModeValue;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.units.measure.Angle;
import edu.wpi.first.units.measure.MutAngle;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.FunctionalCommand;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;
import frc.robot.Constants.Mode;
import frc.robot.RobotState;
import frc.robot.generic.util.LoggedDIO.LoggedDIO;
import frc.robot.generic.util.LoggedTalon.TalonFX.LoggedTalonFX;
import frc.robot.generic.util.LoggedTunableMeasure;
import frc.robot.generic.util.LoggedTunableNumber;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.Setter;
import org.littletonrobotics.junction.Logger;

public class Turret extends SubsystemBase {
  private final LoggedTalonFX motor;
  private final LoggedDIO reverseLimit;
  private final LoggedDIO forwardLimit;

  private final LoggedTunableMeasure<MutAngle> minAngle =
      new LoggedTunableMeasure<MutAngle>("Turret/MinAngle", Degrees.mutable(-190));
  private final LoggedTunableMeasure<MutAngle> maxAngle =
      new LoggedTunableMeasure<MutAngle>("Turret/MaxAngle", Degrees.mutable(200));

  private final LoggedTunableNumber homingVoltage =
      new LoggedTunableNumber("Turret/Homing/Voltage", 4, "v");
  private final LoggedTunableNumber homingConfirmationVoltage =
      new LoggedTunableNumber("Turret/Homing/ConfirmVoltage", 4, "v");
  private final LoggedTunableMeasure<MutAngle> homingSwitchPosition =
      new LoggedTunableMeasure<>("Turret/Homing/homePosition", Rotations.mutable(0));
  private final LoggedTunableMeasure<MutAngle> homingConfirmPosition =
      new LoggedTunableMeasure<>("Turret/Homing/homePosition", Rotations.mutable(0.1));
  private final LoggedTunableMeasure<MutAngle> tolerance =
      new LoggedTunableMeasure<>("Turret/Tolerance", Degrees.mutable(5));

  private final Translation3d turretVisual = new Translation3d(0, 0, Units.inchesToMeters(20));

  private final MotionMagicTorqueCurrentFOC mmControl = new MotionMagicTorqueCurrentFOC(0);
  private final NeutralOut neutralControl = new NeutralOut();

  private final MutAngle targetPosition = Degree.mutable(0);

  private boolean positionControl = false;
  @Setter private boolean homed = false;
  @Getter private boolean atSetpoint = false;

  public Turret(LoggedTalonFX motor, LoggedDIO reverseLimit, LoggedDIO forwardLimit) {
    this.motor = motor;
    this.reverseLimit = reverseLimit.withReversed(true);
    this.forwardLimit = forwardLimit.withReversed(true);

    var config =
        new TalonFXConfiguration()
            .withSlot0(new Slot0Configs().withKP(0).withKI(0).withKD(0).withKS(0).withKV(0))
            .withMotionMagic(
                new MotionMagicConfigs()
                    .withMotionMagicCruiseVelocity(15)
                    .withMotionMagicAcceleration(30))
            .withMotorOutput(
                new MotorOutputConfigs()
                    .withNeutralMode(NeutralModeValue.Brake)
                    .withInverted(InvertedValue.Clockwise_Positive))
            .withCurrentLimits(new CurrentLimitsConfigs().withStatorCurrentLimit(5))
            .withFeedback(new FeedbackConfigs().withSensorToMechanismRatio(15));
    motor.withConfig(config).withMMPIDTuning(SlotConfigs.from(config.Slot0), config.MotionMagic);
    setDefaultCommand(aimCommand());
    if (Constants.currentMode == Mode.SIM) {
      SmartDashboard.putData("Turret/SetHomed", setHomed());
    }
    // Preload so AdvantageKit can process logging stuff before the match starts.
    ShotCalculator.getInstance().calculateShot();
  }

  public Command aimCommand() {
    return run(
        () -> {
          if (homed) {
            this.requestPosition(
                ShotCalculator.getInstance().calculateShot().turretAngle().getMeasure());
          }
        });
  }

  /**
   * A command that requests the turret to move to a position. The command completes imminently,
   * without waiting for a tolerance to be achieved.
   *
   * @param position a supplier of the target position
   * @return the command
   */
  public Command requestPosition(Supplier<Angle> position) {
    return runOnce(
        () -> {
          requestPosition(position.get());
        });
  }

  public void requestPosition(Angle position) {

    targetPosition.mut_setBaseUnitMagnitude(
        MathUtil.inputModulus(
            position.baseUnitMagnitude(),
            minAngle.get().baseUnitMagnitude(),
            maxAngle.get().baseUnitMagnitude()));
    positionControl = true;
    setControl();
  }

  /**
   * A command that commands the Turret to move to a position. This command ends when the setpoint
   * is archived
   *
   * @param position a supplier of the target position
   * @return the command
   */
  public Command gotoPosition(Supplier<Angle> position) {
    // I really shouldn't but by creating a functional command I don't create 5 extra objects by
    // separating this out.
    return new FunctionalCommand(
        () -> requestPosition(position.get()),
        () -> {}, // Nothing to do periodically. Motion is controlled in the periodic function
        (i) -> {},
        () -> atSetpoint,
        this);
  }

  public Command setHomed() {
    return runOnce(() -> this.setHomed(true)).ignoringDisable(true);
  }

  @Override
  public final void periodic() {
    motor.periodic();
    reverseLimit.periodic();
    forwardLimit.periodic();
    atSetpoint = motor.atSetpoint(targetPosition, tolerance.get());
    Logger.recordOutput("Turret/AtSetpoint", atSetpoint);
    Logger.recordOutput("Turret/Homed", homed);
    Logger.recordOutput("Turret/PositionControl", positionControl);

    setControl();

    Logger.recordOutput(
        "Turret/VisibleSetpoint",
        new Pose3d(RobotState.getInstance().getRobotPosition())
            .transformBy(
                new Transform3d(
                    turretVisual,
                    new Rotation3d(Rotations.zero(), Rotations.zero(), targetPosition))));
  }

  private void setControl() {
    if (positionControl) {
      Logger.recordOutput("Turret/Target", targetPosition);
      motor.setControl(
          mmControl
              .withPosition(targetPosition)
              .withLimitReverseMotion(reverseLimit.get())
              .withLimitForwardMotion(forwardLimit.get()));
    } else {
      motor.setControl(neutralControl);
    }
  }
}
/*
Chassis
Handoff
 */
