package frc.robot.testing2026.subsystems.shooter;

import static edu.wpi.first.units.Units.Degrees;
import static edu.wpi.first.units.Units.Rotations;

import com.ctre.phoenix6.configs.CommutationConfigs;
import com.ctre.phoenix6.configs.CurrentLimitsConfigs;
import com.ctre.phoenix6.configs.ExternalFeedbackConfigs;
import com.ctre.phoenix6.configs.MotionMagicConfigs;
import com.ctre.phoenix6.configs.MotorOutputConfigs;
import com.ctre.phoenix6.configs.Slot0Configs;
import com.ctre.phoenix6.configs.SlotConfigs;
import com.ctre.phoenix6.configs.TalonFXSConfiguration;
import com.ctre.phoenix6.controls.DutyCycleOut;
import com.ctre.phoenix6.controls.MotionMagicDutyCycle;
import com.ctre.phoenix6.controls.NeutralOut;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.MotorArrangementValue;
import com.ctre.phoenix6.signals.NeutralModeValue;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rectangle2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.units.measure.Angle;
import edu.wpi.first.units.measure.MutAngle;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Command.InterruptionBehavior;
import edu.wpi.first.wpilibj2.command.FunctionalCommand;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.generic.RobotState;
import frc.robot.generic.util.LoggedTalon.TalonFXS.LoggedTalonFXS;
import frc.robot.generic.util.LoggedTunableMeasure;
import frc.robot.generic.util.LoggedTunableNumber;
import frc.robot.testing2026.FieldConstants.LinesHorizontal;
import frc.robot.testing2026.FieldConstants.LinesVertical;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.Setter;
import org.littletonrobotics.junction.AutoLogOutput;
import org.littletonrobotics.junction.Logger;

public class Hood extends SubsystemBase {
  /* Hardware */
  private final LoggedTalonFXS motor;

  /* Movement Constants */
  private final LoggedTunableMeasure<MutAngle> downPosition =
      new LoggedTunableMeasure<>("Hood/DownPosition", Degrees.mutable(18.575));
  private final LoggedTunableMeasure<MutAngle> stowPosition =
      new LoggedTunableMeasure<>("Hood/StowAngle", Degrees.mutable(15));
  public static LoggedTunableNumber stowTrenchGapOffset =
      new LoggedTunableNumber("Hood/stowTrenchGapOffset", 0, "m");
  private final LoggedTunableMeasure<MutAngle> tolerance =
      new LoggedTunableMeasure<>("Hood/Tolerance", Degrees.mutable(5));
  /* Homing */
  private final LoggedTunableNumber homingVoltage =
      new LoggedTunableNumber("Hood/Homing/Voltage", 4, "v");
  private final LoggedTunableNumber homingConfirmationVoltage =
      new LoggedTunableNumber("Hood/Homing/ConfirmVoltage", 4, "v");
  private final LoggedTunableMeasure<MutAngle> homingSwitchPosition =
      new LoggedTunableMeasure<>("Hood/Homing/HomePosition", Rotations.mutable(0));
  private final LoggedTunableMeasure<MutAngle> homingConfirmPosition =
      new LoggedTunableMeasure<>("Hood/Homing/ConfirmPosition", Rotations.mutable(0.1));

  /* State */
  /** The target position of the motor. 0 is the hood resting on the turret. */
  @AutoLogOutput private final MutAngle targetPosition = Degrees.mutable(0);

  @AutoLogOutput private boolean positionControl = false;
  @AutoLogOutput @Setter private boolean homed = true;
  @AutoLogOutput @Getter private boolean atSetpoint = false;
  @AutoLogOutput private Rectangle2d[] trenchAreas = new Rectangle2d[4];

  /* Control  Requests*/
  private final NeutralOut neutralControl = new NeutralOut();
  private final MotionMagicDutyCycle mmControl = new MotionMagicDutyCycle(targetPosition);
  private final DutyCycleOut dutyCycleOut = new DutyCycleOut(0);

  public Hood(LoggedTalonFXS motor) {
    this.motor = motor;
    updateTrenchAreas();
    var config =
        new TalonFXSConfiguration()
            .withCommutation(
                new CommutationConfigs().withMotorArrangement(MotorArrangementValue.Minion_JST))
            .withSlot0(new Slot0Configs().withKP(14).withKI(0).withKD(0).withKS(0.017).withKV(0.2))
            .withMotionMagic(
                new MotionMagicConfigs()
                    .withMotionMagicCruiseVelocity(15)
                    .withMotionMagicAcceleration(30))
            .withMotorOutput(
                new MotorOutputConfigs()
                    .withNeutralMode(NeutralModeValue.Brake)
                    .withInverted(InvertedValue.CounterClockwise_Positive))
            .withCurrentLimits(new CurrentLimitsConfigs().withStatorCurrentLimit(15))
            .withExternalFeedback(new ExternalFeedbackConfigs().withSensorToMechanismRatio(36));
    // 12t pulley -> 24t, 10t gear -> 180t spur gear
    // 2:1 * 18:1 = 36:1 overall
    motor.withConfig(config).withMMPIDTuning(SlotConfigs.from(config.Slot0), config.MotionMagic);
    setDefaultCommand(aimCommand());
    new Trigger(this::shouldStow).whileTrue(stowCommand());

    SmartDashboard.putData("Hood/SetHomed", runOnce(() -> setHomed(true)).ignoringDisable(true));
  }

  public Command aimCommand() {
    return run(
        () -> {
          if (homed) {
            this.requestAngle(ShotCalculator.getInstance().calculateShot().hoodAngle());
          }
        });
  }

  public Command stowCommand() {
    return startEnd(
            () -> {
              this.requestAngle(new Rotation2d(stowPosition.get()));
            },
            () -> {})
        .withInterruptBehavior(InterruptionBehavior.kCancelIncoming);
  }

  // public Command homingCommand() {
  //   return new FunctionalCommand(()->{this.positionControl=false;
  //     motor.setControl(dutyCycleOut.withOutput(0));
  //   }, ()->{}), null, null, null)
  // }

  /**
   * A command that requests the turret to move to a robot-relative angle. The command completes
   * imminently, without waiting for a tolerance to be achieved.
   *
   * @param angle a supplier of the target angle. The angle is relative to vertical. 0 is vertical,
   *     90 is horizontal
   * @return the command
   */
  public Command requestAngle(Supplier<Rotation2d> angle) {
    return runOnce(
        () -> {
          requestAngle(angle.get());
        });
  }

  /**
   * @param angle the target angle relative to vertical. 0 is vertical, 90 is horizontal
   */
  public void requestAngle(Rotation2d angle) {
    Logger.recordOutput("Hood/RequestedAngle", angle.getDegrees(), "deg");
    angleToPosition(angle, targetPosition);
    motor.setControl(mmControl.withPosition(targetPosition));
  }

  /**
   * A command that commands the Turret to move to a angle. This command ends when the setpoint is
   * archived
   *
   * @param angle a supplier of the target angle. The angle is relative to vertical. 0 is vertical,
   *     90 is horizontal
   * @return the command
   */
  public Command gotoAngle(Supplier<Rotation2d> angle) {
    // I really shouldn't but by creating a functional command I don't create 5 extra objects by
    // separating this out.
    return new FunctionalCommand(
        () -> requestAngle(angle.get()),
        () -> {}, // Nothing to do periodically. Motion is controlled in the periodic function
        (i) -> {},
        () -> atSetpoint,
        this);
  }

  public boolean shouldStow() {
    boolean shouldStow = false;
    final Pose2d pose = RobotState.getInstance().getRobotPosition();
    for (int i = 0; i < trenchAreas.length; i++) {
      if (trenchAreas[i].contains(pose.getTranslation())) {
        shouldStow = true;
        break;
      }
    }
    Logger.recordOutput("Hood/ShouldStow", shouldStow);
    return shouldStow;
  }

  private void updateTrenchAreas() {
    double offset = stowTrenchGapOffset.get();
    trenchAreas =
        new Rectangle2d[] {
          new Rectangle2d(
              new Translation2d(
                  LinesVertical.starting - offset, LinesHorizontal.leftTrenchOpenStart),
              new Translation2d(
                  LinesVertical.neutralZoneNear + offset, LinesHorizontal.leftTrenchOpenEnd)),
          new Rectangle2d(
              new Translation2d(
                  (2 * LinesVertical.center) - LinesVertical.starting - offset,
                  LinesHorizontal.leftTrenchOpenStart),
              new Translation2d(
                  LinesVertical.neutralZoneFar + offset, LinesHorizontal.leftTrenchOpenEnd)),
          new Rectangle2d(
              new Translation2d(
                  LinesVertical.starting - offset, LinesHorizontal.rightTrenchOpenStart),
              new Translation2d(
                  LinesVertical.neutralZoneNear + offset, LinesHorizontal.rightTrenchOpenEnd)),
          new Rectangle2d(
              new Translation2d(
                  (2 * LinesVertical.center) - LinesVertical.starting - offset,
                  LinesHorizontal.rightTrenchOpenStart),
              new Translation2d(
                  LinesVertical.neutralZoneFar + offset, LinesHorizontal.rightTrenchOpenEnd))
        };
  }

  @Override
  public final void periodic() {
    motor.periodic();
    atSetpoint = motor.atSetpoint(targetPosition, tolerance.get());
    Logger.recordOutput("Hood/Angle", positionToAngle(motor.getPosition()).getDegrees(), "deg");

    ShotCalculator.getInstance().clearCache();
    LoggedTunableNumber.ifChanged(this, (value) -> this.updateTrenchAreas(), stowTrenchGapOffset);
  }

  /**
   * Converts the robot relative angle to motor position. This is just a helper; would be static if
   * it didn't use tuned values.
   *
   * <p>This is a c style function for them GC savings.
   *
   * @param angle the angle to convert, relative to vertical down.
   * @param positionOut a reference to the position to write to. This is mutated rather than
   *     returned to save on GC.
   */
  private void angleToPosition(Rotation2d angle, MutAngle positionOut) {
    positionOut.mut_setBaseUnitMagnitude(
        angle.getRadians() - downPosition.get().baseUnitMagnitude());
  }

  /**
   * Converts the motor position to a robot relative angle. This is just a helper; would be static
   * if it didn't use tuned values.
   *
   * @param position the position to convert
   * @return the angle, relative to vertical down
   */
  private Rotation2d positionToAngle(Angle position) {
    return new Rotation2d(position.baseUnitMagnitude() + downPosition.get().baseUnitMagnitude());
  }

  public Command dutyCycleTestCommand(double dutyCycle) {
    return startEnd(
        () -> {
          this.positionControl = false;
          motor.setControl(dutyCycleOut.withOutput(dutyCycle));
        },
        () -> {
          motor.setControl(dutyCycleOut.withOutput(0));
        });
  }
}
