package frc.robot.testing2026.subsystems.shooter;

import com.ctre.phoenix6.CANBus;
import edu.wpi.first.wpilibj2.command.ParallelCommandGroup;
import frc.robot.Constants;
import frc.robot.generic.util.LoggedAnalogInput.HardwareAnalogInput;
import frc.robot.generic.util.LoggedAnalogInput.NoOppAnalogInput;
import frc.robot.generic.util.LoggedAnalogInput.SimAnalogInput;
import frc.robot.generic.util.LoggedDIO.HardwareDIO;
import frc.robot.generic.util.LoggedDIO.SimDIO;
import frc.robot.generic.util.LoggedTalon.Follower.PhoenixTalonFollower;
import frc.robot.generic.util.LoggedTalon.TalonFX.NoOppTalonFX;
import frc.robot.generic.util.LoggedTalon.TalonFX.PhoenixTalonFX;
import frc.robot.generic.util.LoggedTalon.TalonFX.TalonFXFlywheelSim;
import frc.robot.generic.util.LoggedTalon.TalonFX.TalonFXSimpleMotorSim;
import lombok.Getter;

/** Container for shooting bits. This class will initialize the proper IO interfaces. */
public class Shooter {
  @Getter private final Flywheel flywheel;
  @Getter private final Hood hood;
  @Getter private final Turret turret;

  public Shooter(CANBus bus) {
    switch (Constants.currentMode) {
      case REAL -> {
        flywheel =
            new Flywheel(
                new PhoenixTalonFX(25, bus, "Flywheel", new PhoenixTalonFollower(26, true)));
        hood =
            new Hood(
                new PhoenixTalonFX(27, bus, "Hood"),
                new HardwareDIO("HoodReverse", 1),
                new HardwareDIO("HoodForward", 2));
        turret =
            new Turret(
                new PhoenixTalonFX(28, bus, "Turret"),
                new HardwareDIO("TurretReverse", 2),
                new HardwareDIO("TurretForward", 3),
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
                    new PhoenixTalonFollower(26, true)));
        hood =
            new Hood(
                new TalonFXSimpleMotorSim(27, bus, "Hood", 0.0017154536, 1.3),
                SimDIO.fromNT("HoodReverse"),
                SimDIO.fromNT("HoodForward"));
        turret =
            new Turret(
                new TalonFXSimpleMotorSim(28, bus, "Turret", 0.0307668163, 1.25),
                SimDIO.fromNT("TurretReverse"),
                SimDIO.fromNT("TurretForward"),
                SimAnalogInput.fromNT("TurretPot"));
      }
      default -> {
        flywheel = new Flywheel(new NoOppTalonFX("Flywheel", 1));
        hood =
            new Hood(
                new NoOppTalonFX("Hood", 0),
                new HardwareDIO("HoodReverse", 1),
                new HardwareDIO("HoodForward", 2));
        turret =
            new Turret(
                new NoOppTalonFX("Turret", 0),
                new HardwareDIO("TurretReverse", 2),
                new HardwareDIO("TurretForward", 3),
                new NoOppAnalogInput("TurretPot"));
      }
    }
  }

  public ParallelCommandGroup homeCommand() {
    return new ParallelCommandGroup(hood.homingCommand(), turret.homingCommand());
  }
}
