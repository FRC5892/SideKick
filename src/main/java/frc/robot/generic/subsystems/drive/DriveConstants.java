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

package frc.robot.generic.subsystems.drive;

import static edu.wpi.first.units.Units.Degrees;

import com.ctre.phoenix6.signals.SensorDirectionValue;
import com.pathplanner.lib.config.ModuleConfig;
import com.pathplanner.lib.config.RobotConfig;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.math.util.Units;

public class DriveConstants {
  public static final double maxSpeedMetersPerSec = 19.8;
  public static final double odometryFrequency = 100.0; // Hz
  public static final double trackWidth = Units.inchesToMeters(12.5);
  public static final double wheelBase = Units.inchesToMeters(12.5);
  public static final double driveBaseRadius = Math.hypot(trackWidth / 2.0, wheelBase / 2.0);
  public static final Translation2d[] moduleTranslations =
      new Translation2d[] {
        new Translation2d(trackWidth / 2.0, wheelBase / 2.0),
        new Translation2d(trackWidth / 2.0, -wheelBase / 2.0),
        new Translation2d(-trackWidth / 2.0, wheelBase / 2.0),
        new Translation2d(-trackWidth / 2.0, -wheelBase / 2.0)
      };

  // Zeroed rotation values for each module, see setup instructions
  // CW = +
  /*
  If a wheel is overrotated, subtract degrees from its angle.  
  That means the wheel needs to rotate toward the robot’s absolute left 
  (counterclockwise when viewed from above).

  If a wheel is underrotated, add degrees to its angle.  
  That means the wheel needs to rotate toward the robot’s absolute right 
  (clockwise when viewed from above).

  To decide whether a wheel is over- or underrotated:
  Stand (or imagine standing) beside the robot, facing the same direction as its absolute front.

  - For front wheels:
      Overrotated → pointing toward the robot’s right (your left)
      Underrotated → pointing toward the robot’s left (your right)

  - For back wheels:
      Overrotated → pointing toward the robot’s right (your left)
      Underrotated → pointing toward the robot’s left (your right)
  */
  public static final Rotation2d frontLeftZeroRotation =
      new Rotation2d(-0.7853181997882291).minus(new Rotation2d(Degrees.of(16 + 5)));
  public static final Rotation2d frontRightZeroRotation =
      new Rotation2d(-1.9785268942462368).minus(new Rotation2d(Degrees.of(-7 - 3)));
  public static final Rotation2d backLeftZeroRotation =
      new Rotation2d(1.2279650529278354)
          .minus(new Rotation2d(Degrees.of(-8)))
          .minus(Rotation2d.kCCW_90deg);
  public static final Rotation2d backRightZeroRotation =
      new Rotation2d(-0.8600462118731901).minus(new Rotation2d(Degrees.of(17.5 + 2)));

  // Device CAN IDs
  public static final int pigeonCanId = 13;

  public static final int frontLeftDriveCanId = 5;
  public static final int backLeftDriveCanId = 6;
  public static final int frontRightDriveCanId = 19;
  public static final int backRightDriveCanId = 14;

  public static final int frontLeftTurnCanId = 4;
  public static final int backLeftTurnCanId = 7;
  public static final int frontRightTurnCanId = 18;
  public static final int backRightTurnCanId = 13;

  public static final int frontLeftCanCoderId = 12;
  public static final int backLeftCanCoderId = 10;
  public static final int frontRightCanCoderId = 11;
  public static final int backRightCanCoderId = 9;

  // Drive motor configuration
  public static final int driveMotorCurrentLimit = 50;
  public static final double wheelRadiusMeters = Units.inchesToMeters(2);
  public static final double driveMotorReduction = 5.14; // SDS mk4 with L4 gearing
  public static final DCMotor driveGearbox = DCMotor.getNeoVortex(1);

  // Drive encoder configuration
  public static final double driveEncoderPositionFactor =
      2 * Math.PI / driveMotorReduction; // Rotor Rotations -> Wheel Radians
  public static final double driveEncoderVelocityFactor =
      (2 * Math.PI) / 60.0 / driveMotorReduction; // Rotor RPM -> Wheel Rad/Sec

  // Drive PID configuration
  public static final double driveKp = 0.0;
  public static final double driveKd = 0.0;
  public static final double driveKs = 0.0;
  public static final double driveKv = 0.1;
  public static final double driveSimP = 0.05;
  public static final double driveSimD = 0.0;
  public static final double driveSimKs = 0.0;
  public static final double driveSimKv = 0.0789;

  // Turn motor configuration
  public static final boolean turnInverted = false;
  public static final int turnMotorCurrentLimit = 20;
  public static final double turnMotorReduction = 12.8;
  public static final DCMotor turnGearbox = DCMotor.getNeo550(1);

  // Turn encoder configuration
  public static final boolean turnEncoderInverted = true;
  public static final double turnEncoderPositionFactor =
      (2 * Math.PI) / 12.8; // Rotations -> Radians
  public static final double turnEncoderVelocityFactor =
      ((2 * Math.PI) / 60.0) / 12.8; // RPM -> Rad/Sec


  public static final SensorDirectionValue frontLeftTurnDirection =
      SensorDirectionValue.Clockwise_Positive;
  public static final SensorDirectionValue frontRightTurnDirection =
      SensorDirectionValue.Clockwise_Positive;
  public static final SensorDirectionValue backLeftTurnDirection =
      SensorDirectionValue.Clockwise_Positive;
  public static final SensorDirectionValue backRightTurnDirection =
      SensorDirectionValue.Clockwise_Positive;

  // Turn PID configuration
  public static final double turnKp = 0.4;
  public static final double turnKd = 0.0;
  public static final double turnSimP = 8.0;
  public static final double turnSimD = 0.0;
  public static final double turnPIDMinInput = 0; // Radians
  public static final double turnPIDMaxInput = 2 * Math.PI; // Radians

  // PathPlanner configuration
  public static final double robotMassKg = 74.088;
  public static final double robotMOI = 6.883;
  public static final double wheelCOF = 1.2;
  public static final RobotConfig ppConfig =
      new RobotConfig(
          robotMassKg,
          robotMOI,
          new ModuleConfig(
              wheelRadiusMeters,
              maxSpeedMetersPerSec,
              wheelCOF,
              driveGearbox.withReduction(driveMotorReduction),
              driveMotorCurrentLimit,
              1),
          moduleTranslations);
}
