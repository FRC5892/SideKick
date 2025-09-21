package frc.robot.generic.util;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.Constants;
import frc.robot.generic.commands.DriveCommands;
import frc.robot.generic.subsystems.drive.*;

public class SwerveBuilder {
  /**
   * Initializes a Drive Subsystem using default devices and controls. For documentation on the used
   * devices, see Reservations.md in the root of this project.
   *
   * <p>The default controls are the following:
   *
   * <table border="1">
   *     <tr><td>left joystick:</td><td>field relative translation</td></tr>
   *     <tr><td>right joystick:</td><td>field relative rotation, left-right (x axis) only</td></tr>
   * </table>
   *
   * <strong>This function should only be called once.</strong>
   *
   * @param driveController the controller to bind buttons to, traditionally in port 0
   * @return Drive subsystem, for further integration
   */
  public static Drive buildDefaultDrive(CommandXboxController driveController) {
    Drive drive;
    switch (Constants.currentMode) {
      case REAL:
        // Real robot, instantiate hardware IO implementations
        drive =
            new Drive(
                new GyroIOPigeon2(),
                new ModuleIOSpark(0),
                new ModuleIOSpark(1),
                new ModuleIOSpark(2),
                new ModuleIOSpark(3));
        break;

      case SIM:
        // Sim robot, instantiate physics sim IO implementations
        drive =
            new Drive(
                new GyroIO() {},
                new ModuleIOSim(),
                new ModuleIOSim(),
                new ModuleIOSim(),
                new ModuleIOSim());
        break;

      default:
        // Replayed robot, disable IO implementations
        drive =
            new Drive(
                new GyroIO() {},
                new ModuleIO() {},
                new ModuleIO() {},
                new ModuleIO() {},
                new ModuleIO() {});
        break;
    }
    drive.setDefaultCommand(
        DriveCommands.joystickDrive(
            drive,
            () -> -driveController.getLeftY(),
            () -> -driveController.getLeftX(),
            () -> -driveController.getRightX()));
    driveController
        .y()
        .onTrue(
            Commands.runOnce(
                    () ->
                        drive.setPose(
                            new Pose2d(drive.getPose().getTranslation(), new Rotation2d())),
                    drive)
                    .andThen(drive.resetModulesToAbsolute()) // Resets modules when button pressed
                .ignoringDisable(true));
    return drive;
  }
}
