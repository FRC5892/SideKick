package frc.robot.generic.util.LoggedTalon.TalonFXS;

import com.ctre.phoenix6.CANBus;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.math.system.plant.LinearSystemId;
import edu.wpi.first.wpilibj.simulation.DCMotorSim;
import frc.robot.util.LoggedTalon.Follower.PhoenixTalonFollower;
import frc.robot.util.LoggedTalon.TalonInputs;

// Maintainer notes:
// The file is currently a copy of LoggedTalonFX, with all instances of TalonFX replaced with
// TalonFXS. Thanks CTRE, much appreciated
public class TalonFXSSimpleMotorSim extends BaseTalonFXSSim {
  private final DCMotorSim motorSim;

  /**
   * A simple motor sim representing a {@link LoggedTalonFXS}
   *
   * <p>This sim is enhanced with CTRE's high fidelity simulation
   *
   * @param canID The motor's CAN ID
   * @param canBus The motor's CAN Bus
   * @param name The Motors Name. This <strong>MUST NOT</strong> be changed in replay.
   * @param J_KgMetersSquared The inertia of the system, in Kgm^2. See {@link
   *     LinearSystemId#createDCMotorSystem(DCMotor, double, double)}
   * @param gearReduction The gear reduction of the system. See {@link
   *     LinearSystemId#createDCMotorSystem(DCMotor, double, double)}
   * @param followers Followers, if any. Followers will share the same output as the leader. All
   *     followers are designed to be physically connected to the leader and as such their velocity
   *     and position are not accessible separately. The current number off followers
   *     <strong>MUST</strong> be passed into simulation and replay.
   */
  public TalonFXSSimpleMotorSim(
      int canID,
      CANBus canBus,
      String name,
      double J_KgMetersSquared,
      double gearReduction,
      PhoenixTalonFollower... followers) {
    super(canID, canBus, name, followers);
    motorSim =
        new DCMotorSim(
            LinearSystemId.createDCMotorSystem(
                DCMotor.getMinion(followers.length + 1), J_KgMetersSquared, gearReduction),
            DCMotor.getMinion(followers.length + 1));
  }

  @Override
  protected void simulationPeriodic(TalonInputs inputs) {
    motorSim.setInputVoltage(motorSimState.getMotorVoltage());
    motorSim.update(0.02);
    motorSimState.setRotorVelocity(motorSim.getAngularVelocity());
    motorSimState.setRawRotorPosition(motorSim.getAngularPosition());
  }
}
