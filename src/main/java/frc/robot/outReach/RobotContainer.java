// Copyright 2021-2025 FRC 6328
// http://github.com/Mechanical-Advantage
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// version 3 as published by the Free Software Foundation or
// available in the root directory of this project.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.

package frc.robot.outReach;

import com.ctre.phoenix6.CANBus;
import com.pathplanner.lib.auto.AutoBuilder;
import edu.wpi.first.wpilibj.GenericHID;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.sysid.SysIdRoutine;
import frc.robot.Constants;
import frc.robot.generic.Robot;
import frc.robot.generic.commands.DriveCommands;
import frc.robot.generic.subsystems.drive.Drive;
import frc.robot.generic.util.AbstractRobotContainer;
import frc.robot.generic.util.LoggedDIO.HardwareDIO;
import frc.robot.generic.util.LoggedDIO.NoOppDio;
import frc.robot.generic.util.LoggedDIO.SimDIO;
import frc.robot.generic.util.LoggedTalon.NoOppTalonFX;
import frc.robot.generic.util.LoggedTalon.PhoenixTalonFX;
import frc.robot.generic.util.LoggedTalon.SimpleMotorSim;
import frc.robot.generic.util.RobotConfig;
import frc.robot.generic.util.SwerveBuilder;
import frc.robot.outReach.subsystems.turret.Turret;
import org.littletonrobotics.junction.networktables.LoggedDashboardChooser;

/**
 * This class is where the bulk of the robot should be declared. Since Command-based is a
 * "declarative" paradigm, very little robot logic should actually be handled in the {@link Robot}
 * periodic methods (other than the scheduler calls). Instead, the structure of the robot (including
 * subsystems, commands, and button mappings) should be declared here.
 */
public class RobotContainer implements AbstractRobotContainer {
  public static RobotConfig config = RobotConfig.defaultCommandBased(RobotContainer::new);

  // Controller
  private final CommandXboxController controller = new CommandXboxController(0);

  private final CANBus canBus = new CANBus();

  // Subsystems
  private final Drive drive = SwerveBuilder.buildDefaultDrive(controller, 0.75);
  private final Turret turret;

  // Dashboard inputs
  private final LoggedDashboardChooser<Command> autoChooser;

  /** The container for the robot. Contains subsystems, OI devices, and commands. */
  public RobotContainer() {
    switch (Constants.currentMode) {
      case REAL:
        System.out.println("Starting REAL OUTREACH");
        turret =
            new Turret(
                new PhoenixTalonFX(20, canBus, "Turret"),
                new HardwareDIO("ForwardLimit", 1),
                new HardwareDIO("ReverseLimit", 0));
        break;
      case SIM:
        turret =
            new Turret(
                new SimpleMotorSim(0, canBus, "Turret", 10, 10),
                SimDIO.fromNT("ForwardLimit"),
                SimDIO.fromNT("ReverseLimit"));
        break;
      default:
        turret =
            new Turret(
                new NoOppTalonFX("Turret", 0),
                new NoOppDio("ForwardLimit"),
                new NoOppDio("ReverseLimit"));
        break;
    }

    // Set up auto routines
    autoChooser = new LoggedDashboardChooser<>("Auto Choices", AutoBuilder.buildAutoChooser());

    // Set up SysId routines
    autoChooser.addOption(
        "Drive Wheel Radius Characterization", DriveCommands.wheelRadiusCharacterization(drive));
    autoChooser.addOption(
        "Drive Simple FF Characterization", DriveCommands.feedforwardCharacterization(drive));
    autoChooser.addOption(
        "Drive SysId (Quasistatic Forward)",
        drive.sysIdQuasistatic(SysIdRoutine.Direction.kForward));
    autoChooser.addOption(
        "Drive SysId (Quasistatic Reverse)",
        drive.sysIdQuasistatic(SysIdRoutine.Direction.kReverse));
    autoChooser.addOption(
        "Drive SysId (Dynamic Forward)", drive.sysIdDynamic(SysIdRoutine.Direction.kForward));
    autoChooser.addOption(
        "Drive SysId (Dynamic Reverse)", drive.sysIdDynamic(SysIdRoutine.Direction.kReverse));

    // Configure the button bindings
    configureButtonBindings();
  }

  /**
   * Use this method to define your button->command mappings. Buttons can be created by
   * instantiating a {@link GenericHID} or one of its subclasses ({@link
   * edu.wpi.first.wpilibj.Joystick} or {@link XboxController}), and then passing it to a {@link
   * edu.wpi.first.wpilibj2.command.button.JoystickButton}.
   */
  private void configureButtonBindings() {
    controller.a().onTrue(turret.turnToRotationCommand(0.5));
    controller.b().onTrue(turret.turnToRotationCommand(0));
  }

  /**
   * Use this to pass the autonomous command to the main {@link Robot} class.
   *
   * @return the command to run in autonomous
   */
  public Command getAutonomousCommand() {
    return autoChooser.get();
  }
}
