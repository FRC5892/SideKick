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

package frc.robot;

import edu.wpi.first.wpilibj.RobotBase;
import frc.robot.generic.util.RobotConfig;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * This class defines the runtime mode used by AdvantageKit. The mode is always "real" when running
 * on a roboRIO. Change the value of "simMode" to switch between "sim" (physics sim) and "replay"
 * (log replay from a file).
 */
public final class Constants {
  public static final Mode simMode = Mode.SIM;
  public static final Mode currentMode = RobotBase.isReal() ? Mode.REAL : simMode;
  public static final boolean tuningMode = true;
  @Getter private static final Robot currentRobot = Robot.TESTING_2026;

  public static enum Mode {
    /** Running on a real robot. */
    REAL,

    /** Running a physics simulator. */
    SIM,

    /** Replaying from a log file. */
    REPLAY
  }

  @RequiredArgsConstructor
  public enum Robot {
    OUTREACH(frc.robot.outReach.RobotContainer.config),
    TESTBED(frc.robot.testbed.RobotContainer.config),
    CUSTOM_IMPL_1(frc.robot.customImplementation1.RobotContainer.config),
    CUSTOM_IMPL_2(frc.robot.customImplementation2.RobotContainer.config),
    TESTING_2026(frc.robot.testing2026.RobotContainer.config);

    public final RobotConfig config;
  }
}
