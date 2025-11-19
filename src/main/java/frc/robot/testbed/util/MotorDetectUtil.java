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

package frc.robot.testbed.util;

import com.ctre.phoenix6.StatusCode;
import com.ctre.phoenix6.hardware.TalonFX;
import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.SparkMax;

/**
 * Utility class for detecting motor types on the CAN bus.
 *
 * <p>This class interrogates the CAN bus to determine if a device at a given CAN ID is a TalonFX or
 * Spark Max motor controller by attempting to communicate with each type and checking for
 * successful responses.
 */
public class MotorDetectUtil {

  /**
   * Detects if the device at the given CAN ID is a TalonFX motor controller.
   *
   * <p>This method attempts to create and communicate with both TalonFX and Spark Max controllers
   * to determine which one responds successfully. The detection works by:
   *
   * <ol>
   *   <li>Attempting to read firmware version from a TalonFX at the CAN ID
   *   <li>If successful, returns true (device is TalonFX)
   *   <li>If failed, attempts to communicate with a Spark Max at the CAN ID
   *   <li>If Spark responds, returns false (device is Spark)
   *   <li>If neither responds, defaults to TalonFX (false positive is safer)
   * </ol>
   *
   * @param id The CAN ID to check
   * @return true if the device is detected as a TalonFX, false if detected as a Spark
   */
  public static boolean detectIsTalonFX(int id) {
    return detectIsTalonFX(id, "");
  }

  /**
   * Detects if the device at the given CAN ID is a TalonFX motor controller.
   *
   * @param id The CAN ID to check
   * @param canbus The name of the CAN bus (empty string for default "rio")
   * @return true if the device is detected as a TalonFX, false if detected as a Spark
   */
  public static boolean detectIsTalonFX(int id, String canbus) {
    // First, try to detect as TalonFX
    if (canDetectTalonFX(id, canbus)) {
      return true;
    }

    // If TalonFX detection failed, try Spark
    if (canDetectSpark(id)) {
      return false;
    }

    // If neither detected, default to TalonFX
    // (This is safer as TalonFX has more robust error handling)
    System.err.println(
        "[MotorDetectUtil] WARNING: Could not detect motor type at CAN ID "
            + id
            + ". Defaulting to TalonFX.");
    return true;
  }

  /**
   * Attempts to detect a TalonFX at the given CAN ID.
   *
   * @param id The CAN ID to check
   * @param canbus The name of the CAN bus
   * @return true if a TalonFX is detected and responds
   */
  private static boolean canDetectTalonFX(int id, String canbus) {
    try {
      // Create a temporary TalonFX instance
      TalonFX talon = new TalonFX(id, canbus);

      // Try to get the firmware version - this will fail if device doesn't exist or isn't a TalonFX
      // We use a small timeout to avoid blocking too long
      var versionStatus = talon.getVersion();

      // Check if we got a valid response
      boolean detected =
          versionStatus.getStatus() == StatusCode.OK && versionStatus.getValue() != null;

      // Clean up - close the TalonFX to free resources
      talon.close();

      return detected;
    } catch (Exception e) {
      // Any exception means it's not a TalonFX
      return false;
    }
  }

  /**
   * Attempts to detect a Spark Max at the given CAN ID.
   *
   * @param id The CAN ID to check
   * @return true if a Spark is detected and responds
   */
  private static boolean canDetectSpark(int id) {
    try {
      // Create a temporary Spark instance
      // Note: We have to guess brushless vs brushed. Brushless is more common.
      SparkMax spark = new SparkMax(id, MotorType.kBrushless);

      // Try to get firmware version - this will fail if device doesn't exist or isn't a Spark
      String firmwareVersion = spark.getFirmwareString();

      // Check if we got a valid response (non-empty firmware string)
      boolean detected = firmwareVersion != null && !firmwareVersion.isEmpty();

      // Clean up - close the Spark to free resources
      spark.close();

      return detected;
    } catch (Exception e) {
      // Any exception means it's not a Spark
      return false;
    }
  }

  /**
   * Returns a human-readable name for the motor controller type at the given CAN ID.
   *
   * @param id The CAN ID to check
   * @return "TalonFX" or "Spark" based on detection
   */
  public static String getMotorTypeName(int id) {
    return detectIsTalonFX(id) ? "TalonFX" : "Spark";
  }

  /**
   * Returns a human-readable name for the motor controller type at the given CAN ID.
   *
   * @param id The CAN ID to check
   * @param canbus The name of the CAN bus
   * @return "TalonFX" or "Spark" based on detection
   */
  public static String getMotorTypeName(int id, String canbus) {
    return detectIsTalonFX(id, canbus) ? "TalonFX" : "Spark";
  }

  /**
   * Scans a range of CAN IDs and returns information about detected devices.
   *
   * @param startId The first CAN ID to scan (inclusive)
   * @param endId The last CAN ID to scan (inclusive)
   * @return A string containing information about all detected devices
   */
  public static String scanCANBus(int startId, int endId) {
    StringBuilder result = new StringBuilder();
    result.append("CAN Bus Scan Results:\n");
    result.append("===================\n");

    for (int id = startId; id <= endId; id++) {
      String deviceType = "Not detected";

      if (canDetectTalonFX(id, "")) {
        deviceType = "TalonFX";
      } else if (canDetectSpark(id)) {
        deviceType = "Spark Max";
      }

      if (!deviceType.equals("Not detected")) {
        result.append(String.format("CAN ID %2d: %s\n", id, deviceType));
      }
    }

    return result.toString();
  }
}
