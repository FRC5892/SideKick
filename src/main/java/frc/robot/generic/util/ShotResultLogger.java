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

package frc.robot.generic.util;

import edu.wpi.first.networktables.BooleanEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

/**
 * Dashboard button handler for logging shot results.
 * 
 * Creates two buttons in NetworkTables/Dashboard:
 * - "Shot Hit" button - Press when shot hits target
 * - "Shot Miss" button - Press when shot misses target
 * 
 * Drivers click these buttons in AdvantageScope or Shuffleboard.
 */
public class ShotResultLogger extends SubsystemBase {
  
  private final BooleanEntry hitButton;
  private final BooleanEntry missButton;
  
  private boolean lastHitValue = false;
  private boolean lastMissValue = false;

  public ShotResultLogger() {
    // Create NetworkTables entries for dashboard buttons
    var table = NetworkTableInstance.getDefault().getTable("FiringSolver");
    
    hitButton = table.getBooleanTopic("LogHit").getEntry(false);
    missButton = table.getBooleanTopic("LogMiss").getEntry(false);
    
    // Set initial values
    hitButton.set(false);
    missButton.set(false);
  }

  @Override
  public void periodic() {
    // Check if Hit button was pressed
    boolean currentHit = hitButton.get();
    if (currentHit && !lastHitValue) {
      // Button was just pressed
      FiringSolutionSolver.logShotResult(true);
      
      // Reset the button
      hitButton.set(false);
    }
    lastHitValue = currentHit;
    
    // Check if Miss button was pressed
    boolean currentMiss = missButton.get();
    if (currentMiss && !lastMissValue) {
      // Button was just pressed
      FiringSolutionSolver.logShotResult(false);
      
      // Reset the button
      missButton.set(false);
    }
    lastMissValue = currentMiss;
  }
}
