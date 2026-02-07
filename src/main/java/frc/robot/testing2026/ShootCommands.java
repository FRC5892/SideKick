package frc.robot.testing2026;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.testing2026.subsystems.shooter.Shooter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ShootCommands {
  public static Command shoot(Shooter shooter) {
    return Commands.race(
        shooter.getFlywheel().aimCommand(),
        shooter.getHood().aimCommand(),
        shooter.getTurret().aimCommand());
  }
}
