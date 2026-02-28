package frc.robot.testing2026;

import com.ctre.phoenix6.CANBus;
import com.pathplanner.lib.auto.AutoBuilder;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.wpilibj.GenericHID;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.sysid.SysIdRoutine;
import frc.robot.Constants;
import frc.robot.generic.Robot;
import frc.robot.generic.commands.DriveCommands;
import frc.robot.generic.subsystems.drive.Drive;
import frc.robot.generic.subsystems.vision.Vision;
import frc.robot.generic.subsystems.vision.VisionConstants;
import frc.robot.generic.subsystems.vision.VisionIO;
import frc.robot.generic.subsystems.vision.VisionIOPhotonVision;
import frc.robot.generic.subsystems.vision.VisionIOPhotonVisionSim;
import frc.robot.generic.util.AbstractRobotContainer;
import frc.robot.generic.util.RobotConfig;
import frc.robot.generic.util.SwerveBuilder;
import frc.robot.testing2026.subsystems.shooter.Shooter;
import org.littletonrobotics.junction.AutoLogOutputManager;
import org.littletonrobotics.junction.networktables.LoggedDashboardChooser;
import org.littletonrobotics.junction.networktables.LoggedNetworkNumber;

public class RobotContainer implements AbstractRobotContainer {
  public static RobotConfig config = RobotConfig.defaultCommandBased(RobotContainer::new);

  // Controller
  private final CommandXboxController controller = new CommandXboxController(0);

  private final CANBus canBus = new CANBus();

  // Subsystems
  private final Drive drive = SwerveBuilder.buildDefaultDrive(controller);
  private final Shooter shooter;
  private final Vision vision;

  // Dashboard inputs
  private final LoggedDashboardChooser<Command> autoChooser;

  /** The container for the robot. Contains subsystems, OI devices, and commands. */
  public RobotContainer() {
    AutoLogOutputManager.addPackage("frc.robot.testing2026");
    shooter = new Shooter(canBus);

    switch (Constants.currentMode) {
      case REAL -> {
        vision =
            new Vision(
                drive::addVisionMeasurement,
                new VisionIOPhotonVision(
                    VisionConstants.camera0Name, VisionConstants.robotToCamera0));
      }
      case SIM -> {
        vision =
            new Vision(
                drive::addVisionMeasurement,
                new VisionIOPhotonVisionSim(
                    VisionConstants.camera0Name, VisionConstants.robotToCamera0, drive::getPose));
      }
      default -> {
        vision = new Vision(drive::addVisionMeasurement, new VisionIO() {});
      }
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
    controller.start().onTrue(shooter.getTurret().updateFromAbsoluteCommand());
    // controller.a().onTrue(ShootCommands.shoot(shooter));

    // controller.x().onTrue(RobotState.getInstance().setGoalCommand(Goal.LEFT));
    // controller.b().onTrue(RobotState.getInstance().setGoalCommand(Goal.RIGHT));
    // controller.y().onTrue(RobotState.getInstance().setGoalCommand(Goal.HUB));
    // controller.x().onTrue(shooter.getTurret().gotoPosition(() -> Degree.of(-120)));
    // controller.a().onTrue(shooter.getTurret().gotoPosition(() -> Degree.of(0)));
    // controller.b().onTrue(shooter.getTurret().gotoPosition(() -> Degree.of(120)));
    // controller.a().whileTrue(shooter.getHood().dutyCycleTestCommand(0.1));
    // controller.b().whileTrue(shooter.getHood().dutyCycleTestCommand(-0.1));

    controller.rightBumper().onTrue(shooter.getHood().gotoAngle(() -> Rotation2d.fromDegrees(19)));
    controller.leftBumper().onTrue(shooter.getHood().gotoAngle(() -> Rotation2d.fromDegrees(38)));
    // controller
    //     .axisMagnitudeGreaterThan(XboxController.Axis.kLeftTrigger.value, 0.1)
    //     .whileTrue(
    //         shooter.getFlywheel().setpointTestCommand(() -> controller.getLeftTriggerAxis() *
    // 100));
    controller
        .a()
        .whileTrue(
            shooter.tuneCommand(
                new LoggedNetworkNumber("/Tuning/SpeedRPS", 0),
                new LoggedNetworkNumber("/Tuning/HoodAngle", 18.575)));
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
