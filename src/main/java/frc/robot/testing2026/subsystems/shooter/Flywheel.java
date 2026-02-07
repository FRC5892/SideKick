package frc.robot.testing2026.subsystems.shooter;

import static edu.wpi.first.units.Units.RPM;
import static edu.wpi.first.units.Units.RotationsPerSecond;

import com.ctre.phoenix6.controls.MotionMagicVelocityTorqueCurrentFOC;
import edu.wpi.first.units.measure.AngularVelocity;
import edu.wpi.first.units.measure.MutAngularVelocity;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.generic.util.LoggedTalon.TalonFX.LoggedTalonFX;
import frc.robot.generic.util.LoggedTunableMeasure;
import lombok.Getter;
import org.littletonrobotics.junction.AutoLogOutput;

public class Flywheel extends SubsystemBase {

  private final LoggedTalonFX motor;

  private final MotionMagicVelocityTorqueCurrentFOC mmControl =
      new MotionMagicVelocityTorqueCurrentFOC(0);
  @Getter @AutoLogOutput private boolean atSetpoint = false;
  private final LoggedTunableMeasure<MutAngularVelocity> tolerance =
      new LoggedTunableMeasure<>("Flywheel/Tolerance", RPM.mutable(5));

  public Flywheel(LoggedTalonFX motor) {
    this.motor = motor;
    setDefaultCommand(aimCommand());
  }

  public void setSetpoint(AngularVelocity velocity) {
    motor.setControl(mmControl.withVelocity(velocity));
  }

  public Command aimCommand() {
    return run(
        () -> {
          setSetpoint(
              RotationsPerSecond.of(
                  ShotCalculator.getInstance().calculateShot().flywheelSpeedRotPerSec()));
        });
  }

  @Override
  public void periodic() {
    motor.periodic();
    atSetpoint = motor.atSetpoint(mmControl.getVelocityMeasure(), tolerance.get());
    ShotCalculator.getInstance().clearCache();
  }
}
