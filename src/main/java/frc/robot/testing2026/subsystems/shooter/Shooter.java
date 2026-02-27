package frc.robot.testing2026.subsystems.shooter;

import com.ctre.phoenix6.CANBus;
import com.ctre.phoenix6.signals.MotorAlignmentValue;
import edu.wpi.first.wpilibj2.command.ParallelCommandGroup;
import frc.robot.Constants;
import frc.robot.generic.util.LoggedAnalogInput.HardwareAnalogInput;
import frc.robot.generic.util.LoggedAnalogInput.NoOppAnalogInput;
import frc.robot.generic.util.LoggedAnalogInput.SimAnalogInput;
import frc.robot.generic.util.LoggedDIO.NoOppDio;
import frc.robot.generic.util.LoggedDIO.SimDIO;
import frc.robot.generic.util.LoggedTalon.Follower.PhoenixTalonFollower;
import frc.robot.generic.util.LoggedTalon.TalonFX.NoOppTalonFX;
import frc.robot.generic.util.LoggedTalon.TalonFX.PhoenixTalonFX;
import frc.robot.generic.util.LoggedTalon.TalonFX.TalonFXFlywheelSim;
import frc.robot.generic.util.LoggedTalon.TalonFX.TalonFXSimpleMotorSim;
import frc.robot.generic.util.LoggedTalon.TalonFXS.NoOppTalonFXS;
import frc.robot.generic.util.LoggedTalon.TalonFXS.PhoenixTalonFXS;
import frc.robot.generic.util.LoggedTalon.TalonFXS.TalonFXSSimpleMotorSim;
import lombok.Getter;

/** Container for shooting bits. This class will initialize the proper IO interfaces. */
public class Shooter {
  @Getter private final Flywheel flywheel;
  @Getter private final Hood hood;
  @Getter private final Turret turret;

  public Shooter(CANBus bus) {
    switch (Constants.currentMode) {
      case REAL -> {
        // flywheel =
        //     new Flywheel(
        //         new PhoenixTalonFX(
        //             25,
        //             bus,
        //             "Flywheel",
        //             new PhoenixTalonFollower(26, MotorAlignmentValue.Opposed)));
        hood = new Hood(new PhoenixTalonFXS(27, bus, "Hood"));
        flywheel = new Flywheel(new NoOppTalonFX("Flywheel", 1));
        turret =
            new Turret(
                new PhoenixTalonFX(28, bus, "Turret"),
                SimDIO.fromNT("TurretReverse"),
                SimDIO.fromNT("TurretForward"),
                new HardwareAnalogInput("TurretPot", 0));
      }
      case SIM -> {
        flywheel =
            new Flywheel(
                new TalonFXFlywheelSim(
                    25,
                    bus,
                    "Flywheel",
                    0.0007567661,
                    1 / 1.25,
                    new PhoenixTalonFollower(26, MotorAlignmentValue.Opposed)));
        hood = new Hood(new TalonFXSSimpleMotorSim(27, bus, "Hood", 0.0017154536, 1.3));
        turret =
            new Turret(
                new TalonFXSimpleMotorSim(28, bus, "Turret", 0.0307668163, 1.25),
                SimDIO.fromNT("TurretReverse"),
                SimDIO.fromNT("TurretForward"),
                SimAnalogInput.fromNT("TurretPot"));
      }
      default -> {
        flywheel = new Flywheel(new NoOppTalonFX("Flywheel", 1));
        hood = new Hood(new NoOppTalonFXS("Hood", 0));
        turret =
            new Turret(
                new NoOppTalonFX("Turret", 0),
                new NoOppDio("TurretReverse"),
                new NoOppDio("TurretForward"),
                new NoOppAnalogInput("TurretPot"));
      }
    }
  }

  public ParallelCommandGroup homeCommand() {
    return new ParallelCommandGroup(/*hood.homingCommand(), */ turret.homingCommand());
  }
}
