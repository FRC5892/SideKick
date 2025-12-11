// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.generic.util;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.GenericHID;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import lombok.Setter;

/** Add your docs here. */
public class JoystickUtil {
  @Setter private static GenericHID driver;
  @Setter private static GenericHID coDriver;

  public static Command rumbleDriver(
      GenericHID.RumbleType rumbleType, double intensity, double timeSeconds) {
    if (driver == null) {
      DriverStation.reportWarning(
          "Driver Rumble Command Initilized without Driver controller set. Please use JoystickUtl.setdriver()",
          true);
      return Commands.print("Misconfigured Driver Rumble");
    }
    return Commands.runEnd(
            () -> {
              driver.setRumble(rumbleType, intensity);
            },
            () -> driver.setRumble(rumbleType, 0))
        .withTimeout(timeSeconds)
        .ignoringDisable(true);
  }

  public static Command rumbleCoDriver(
      GenericHID.RumbleType rumbleType, double intensity, double timeSeconds) {
    if (coDriver == null) {
      DriverStation.reportWarning(
          "CoDriver Rumble Command Initilized without coDriver controller set. Please use JoystickUtl.setCoDriver()",
          true);
      return Commands.print("Misconfigured CoDriver Rumble");
    }
    return Commands.runEnd(
            () -> {
              coDriver.setRumble(rumbleType, intensity);
            },
            () -> coDriver.setRumble(rumbleType, 0))
        .withTimeout(timeSeconds)
        .ignoringDisable(true);
  }

  public static Command rumbleBoth(
      GenericHID.RumbleType rumbleType, double intensity, double timeSeconds) {
    return Commands.parallel(rumbleCoDriver(rumbleType, intensity, timeSeconds));
  }
}
