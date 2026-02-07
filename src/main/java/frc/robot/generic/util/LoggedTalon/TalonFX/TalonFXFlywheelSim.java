package frc.robot.generic.util.LoggedTalon.TalonFX;

import com.ctre.phoenix6.CANBus;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.math.system.plant.LinearSystemId;
import edu.wpi.first.wpilibj.simulation.FlywheelSim;
import frc.robot.generic.util.LoggedTalon.Follower.PhoenixTalonFollower;
import frc.robot.generic.util.LoggedTalon.TalonInputs;

public class TalonFXFlywheelSim extends BaseTalonFXSim {
  private final FlywheelSim motorSim;

  /**
   * A simple flywheel sim representing a {@link LoggedTalonFX}
   *
   * <p>This sim is enhanced with CTRE's high fidelity simulation
   *
   * @param canID The motor's CAN ID
   * @param canBus The motor's CAN Bus
   * @param name The Motors Name. This <strong>MUST NOT</strong> be changed in replay.
   * @param J_KgMetersSquared The inertia of the system, in Kgm^2. See {@link
   *     LinearSystemId#createFlywheelSystem(DCMotor, double, double)}
   * @param gearReduction The gear reduction of the system. See {@link
   *     LinearSystemId#createFlywheelSystem(DCMotor, double, double)}
   * @param followers Followers, if any. Followers will share the same output as the leader. All
   *     followers are designed to be physically connected to the leader and as such their velocity
   *     and position are not accessible separately. The current number off followers
   *     <strong>MUST</strong> be passed into simulation and replay.
   */
  public TalonFXFlywheelSim(
      int canID,
      CANBus canBus,
      String name,
      double J_KgMetersSquared,
      double gearReduction,
      PhoenixTalonFollower... followers) {
    super(canID, canBus, name, followers);
    motorSim =
        new FlywheelSim(
            LinearSystemId.createFlywheelSystem(
                DCMotor.getKrakenX60Foc(followers.length + 1), J_KgMetersSquared, gearReduction),
            DCMotor.getKrakenX60Foc(followers.length + 1));
  }

  @Override
  protected void simulationPeriodic(TalonInputs inputs) {
    motorSim.setInputVoltage(motorSimState.getMotorVoltage());
    motorSim.update(0.02);
    motorSimState.setRotorVelocity(motorSim.getAngularVelocity());
  }
}
