package frc.robot.testing2026;

import com.ctre.phoenix6.controls.MotionMagicVoltage;
import com.ctre.phoenix6.controls.NeutralOut;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.units.measure.Angle;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.FunctionalCommand;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.generic.util.LoggedDIO.LoggedDIO;
import frc.robot.generic.util.LoggedTalon.TalonFX.LoggedTalonFX;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.Setter;
import org.littletonrobotics.junction.Logger;

public class GenericPositionMechanismSubsystem extends SubsystemBase {
  private final String name;

  protected final LoggedTalonFX motor;
  protected final LoggedDIO reverseLimit;
  protected final LoggedDIO forwardLimit;

  protected final DoubleSupplier homingVoltage;
  protected final DoubleSupplier homingConfirmationVoltage;
  protected final Supplier<Angle> homingSwitchPosition;
  protected final Supplier<Angle> homingConfirmPosition;
  protected final Supplier<Angle> tolerance;

  protected final MotionMagicVoltage mmControl;
  protected final NeutralOut neutralControl = new NeutralOut();
  protected Rotation2d targetPosition = Rotation2d.kZero;

  protected boolean positionControl = false;
  @Setter protected boolean homed = false;
  @Getter protected boolean atSetpoint = false;

  public GenericPositionMechanismSubsystem(
      String name,
      LoggedTalonFX motor,
      LoggedDIO reverseLimit,
      LoggedDIO forwardLimit,
      DoubleSupplier homingVoltage,
      DoubleSupplier homingConfirmationVoltage,
      Supplier<Angle> homingSwitchPosition,
      Supplier<Angle> homingConfirmPosition,
      Supplier<Angle> tolerance) {
    this.name = name;
    this.motor = motor;
    this.reverseLimit = reverseLimit.withReversed(true);
    this.forwardLimit = forwardLimit.withReversed(true);
    this.homingVoltage = homingVoltage;
    this.homingConfirmationVoltage = homingConfirmationVoltage;
    this.homingSwitchPosition = homingSwitchPosition;
    this.homingConfirmPosition = homingConfirmPosition;
    this.tolerance = tolerance;

    this.mmControl = new MotionMagicVoltage(targetPosition.getMeasure());
  }

  public Command homingCommand() {
    return MechanismUtil.buildHomingCommand(
            motor,
            reverseLimit,
            this,
            homingVoltage,
            false,
            homingSwitchPosition,
            homingConfirmationVoltage,
            homingConfirmPosition)
        .beforeStarting(() -> positionControl = false)
        .finallyDo(
            (i) -> {
              if (!i) {
                setHomed(true);
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
  public Command requestPosition(Supplier<Rotation2d> position) {
    return runOnce(
        () -> {
          requestPosition(position.get());
        });
  }

  public void requestPosition(Rotation2d position) {
    targetPosition = position;
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
  public Command gotoPosition(Supplier<Rotation2d> position) {
    // I really shouldn't but by creating a functional command I don't create 5 extra objects by
    // separating this out.
    return new FunctionalCommand(
        () -> requestPosition(position.get()),
        () -> {}, // Nothing to do periodically. Motion is controlled in the periodic function
        (i) -> {},
        () -> atSetpoint,
        this);
  }

  @Override
  public final void periodic() {
    motor.periodic();
    reverseLimit.periodic();
    forwardLimit.periodic();
    atSetpoint = motor.atSetpoint(targetPosition.getMeasure(), tolerance.get());
    Logger.recordOutput(name + "/AtSetpoint", atSetpoint);
    Logger.recordOutput(name + "/Homed", homed);
    Logger.recordOutput(name + "/PositionControl", positionControl);
    Logger.recordOutput(name + "/Target", targetPosition);

    periodicUser();
    setControl();
  }

  protected void periodicUser() {}

  private void setControl() {
    if (positionControl) {
      motor.setControl(
          mmControl
              .withPosition(targetPosition.getMeasure())
              .withLimitReverseMotion(reverseLimit.get())
              .withLimitForwardMotion(forwardLimit.get()));
    } else {
      motor.setControl(neutralControl);
    }
  }
}
