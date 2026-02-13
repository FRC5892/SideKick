package frc.robot.testing2026.subsystems.shooter;

import static edu.wpi.first.units.Units.Degrees;
import static edu.wpi.first.units.Units.Rotations;

import com.ctre.phoenix6.configs.CurrentLimitsConfigs;
import com.ctre.phoenix6.configs.FeedbackConfigs;
import com.ctre.phoenix6.configs.MotionMagicConfigs;
import com.ctre.phoenix6.configs.MotorOutputConfigs;
import com.ctre.phoenix6.configs.Slot0Configs;
import com.ctre.phoenix6.configs.SlotConfigs;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.NeutralModeValue;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rectangle2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.units.measure.MutAngle;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Command.InterruptionBehavior;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.generic.RobotState;
import frc.robot.generic.util.LoggedDIO.LoggedDIO;
import frc.robot.generic.util.LoggedTalon.TalonFX.LoggedTalonFX;
import frc.robot.generic.util.LoggedTunableMeasure;
import frc.robot.generic.util.LoggedTunableNumber;
import frc.robot.testing2026.FieldConstants.LinesHorizontal;
import frc.robot.testing2026.FieldConstants.LinesVertical;
import frc.robot.testing2026.GenericPositionMechanismSubsystem;
import org.littletonrobotics.junction.AutoLogOutput;

public class Hood extends GenericPositionMechanismSubsystem {
  private final LoggedTunableMeasure<MutAngle> stowPosition =
      new LoggedTunableMeasure<>("Hood/StowAngle", Degrees.mutable(15));

  public static LoggedTunableNumber stowTrenchGapOffset =
      new LoggedTunableNumber("Hood/stowTrenchGapOffset", 0, "m");

  @AutoLogOutput(key = "Hood/TrenchAreas")
  public static Rectangle2d[] trenchAreas = new Rectangle2d[4];

  public Hood(LoggedTalonFX motor, LoggedDIO reverseLimit, LoggedDIO forwardLimit) {
    super(
        "Hood",
        motor,
        reverseLimit,
        forwardLimit,
        new LoggedTunableNumber("Hood/Homing/Voltage", 4, "v"),
        new LoggedTunableNumber("Hood/Homing/ConfirmVoltage", 4, "v"),
        new LoggedTunableMeasure<>("Hood/Homing/homePosition", Rotations.mutable(0))::get,
        new LoggedTunableMeasure<>("Hood/Homing/homePosition", Rotations.mutable(0.1))::get,
        new LoggedTunableMeasure<>("Hood/Tolerance", Degrees.mutable(5))::get);
    updateTrenchAreas();
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
    new Trigger(this::shouldStow).whileTrue(stowCommand());

    SmartDashboard.putData("Hood/SetHomed", runOnce(() -> setHomed(true)).ignoringDisable(true));
  }

  public Command aimCommand() {
    return run(
        () -> {
          if (homed) {
            this.requestPosition(ShotCalculator.getInstance().calculateShot().hoodAngle());
          }
        });
  }

  public Command stowCommand() {
    return startEnd(
            () -> {
              this.requestPosition(new Rotation2d(stowPosition.get()));
            },
            () -> {})
        .withInterruptBehavior(InterruptionBehavior.kCancelIncoming);
  }

  @AutoLogOutput(key = "Hood/ShouldStow")
  public boolean shouldStow() {
    final Pose2d pose = RobotState.getInstance().getRobotPosition();
    for (int i = 0; i < trenchAreas.length; i++) {
      if (trenchAreas[i].contains(pose.getTranslation())) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected void periodicUser() {
    ShotCalculator.getInstance().clearCache();
    LoggedTunableNumber.ifChanged(this, (value) -> this.updateTrenchAreas(), stowTrenchGapOffset);
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
}
