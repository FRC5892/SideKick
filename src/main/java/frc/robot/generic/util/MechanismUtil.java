package frc.robot.generic.util;

import com.ctre.phoenix6.controls.NeutralOut;
import com.ctre.phoenix6.controls.VoltageOut;
import edu.wpi.first.units.measure.Angle;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.FunctionalCommand;
import edu.wpi.first.wpilibj2.command.Subsystem;
import frc.robot.generic.util.LoggedDIO.LoggedDIO;
import frc.robot.generic.util.LoggedTalon.LoggedTalon;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

public class MechanismUtil {

  /**
   * Build a command to home a simple mechanism
   *
   * <p>The normal configuration of this command builds a homing sequence that involved moving
   * toward the limit switch, backing off at a slower speed, and moving towards the limit switch to
   * gain a more precise position
   *
   * @param talon the motor that needs to be homed
   * @param limitSwitch the digital signal that flips to true when the mechanism is at it's stopping
   *     point
   * @param subsystem the subsystem that is required for this command
   * @param initialVoltage voltage to apply when homing the first attempt
   * @param forward if the limit switch is as far forward as the motor can travel, false if backward
   * @param switchPosition the position, to the motor, of the limit switch (likely 0)
   * @param confirmVoltage the voltage to apply during the confirmation step. Set to 0 to disable
   *     confirmation
   * @param confirmPosition the minimum position to back off to. This is used in a simple bang-bang
   *     controller, so it is possible for the mechanism to move past this point. If {@code
   *     confirmVoltage} is 0 to disable confirmation, this value is unread and can be null
   */
  public static Command buildHomingCommand(
      LoggedTalon<?> talon,
      LoggedDIO limitSwitch,
      Subsystem subsystem,
      DoubleSupplier initialVoltage,
      boolean forward,
      Supplier<Angle> switchPosition,
      DoubleSupplier confirmVoltage,
      Supplier<Angle> confirmPosition) {
    VoltageOut out = new VoltageOut(0);
    NeutralOut neutral = new NeutralOut();
    Runnable towardPeriodic =
        forward
            ? () -> talon.setControl(out.withLimitForwardMotion(limitSwitch.get()))
            : () -> talon.setControl(out.withLimitReverseMotion(limitSwitch.get()));
    Command command =
        new FunctionalCommand(
            () -> {
              out.withOutput(initialVoltage.getAsDouble() * (forward ? 1 : -1));
              towardPeriodic.run();
            },
            towardPeriodic,
            // Only set the position if we are done homing and confirm is not wanted
            confirmVoltage.getAsDouble() == 0
                ? (interrupted) -> {
                  talon.setControl(neutral);
                  if (!interrupted) {
                    talon.setPosition(switchPosition.get());
                  }
                }
                : (interrupted) -> {
                  talon.setControl(neutral);
                },
            limitSwitch,
            subsystem);
    if (confirmVoltage.getAsDouble() != 0) {
      Runnable awayPeriodic =
          forward
              ? () -> talon.setControl(out.withLimitReverseMotion(limitSwitch.get()))
              : () -> talon.setControl(out.withLimitForwardMotion(limitSwitch.get()));
      command =
          command.andThen(
              // Now away from stop
              new FunctionalCommand(
                  () -> {
                    out.withOutput(confirmVoltage.getAsDouble() * (forward ? -1 : 1));
                    awayPeriodic.run();
                  },
                  awayPeriodic,
                  (interrupted) -> talon.setControl(neutral),
                  forward
                      ? () -> talon.getPosition().lte(confirmPosition.get())
                      : () -> talon.getPosition().gte(confirmPosition.get()),
                  subsystem),
              // Now back to the stop
              new FunctionalCommand(
                  () -> {
                    out.withOutput(confirmVoltage.getAsDouble() * (forward ? 1 : -1));
                    towardPeriodic.run();
                  },
                  towardPeriodic,
                  (interrupted) -> {
                    talon.setControl(neutral);
                    if (!interrupted) {
                      talon.setPosition(switchPosition.get());
                    }
                  },
                  limitSwitch,
                  subsystem));
    }
    return command;
  }
}
