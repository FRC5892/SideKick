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
 * <p>Creates two buttons in NetworkTables/Dashboard:
 * <ul>
 *   <li>"Shot Hit" button - Press when shot hits target</li>
 *   <li>"Shot Miss" button - Press when shot misses target</li>
 * </ul>
 * 
 * <p>Drivers click these buttons in AdvantageScope or Shuffleboard.
 * 
 * <p>When a button is clicked:
 * <ul>
 *   <li>Logs the result via {@link FiringSolutionSolver#logShotResult(boolean)}</li>
 *   <li>Sets the ShotLogged flag for {@link ShooterInterlock} if enabled</li>
 * </ul>
 */
public class ShotResultLogger extends SubsystemBase {
  
  private final BooleanEntry hitButton;
  private final BooleanEntry missButton;
  private final BooleanEntry shotLoggedFlag;
  
  private boolean lastHitValue = false;
  private boolean lastMissValue = false;

  /**
   * Creates a new ShotResultLogger.
   * 
   * <p>Initializes NetworkTables entries:
   * <ul>
   *   <li>/FiringSolver/LogHit - Dashboard button for logging hits</li>
   *   <li>/FiringSolver/LogMiss - Dashboard button for logging misses</li>
   *   <li>/FiringSolver/Interlock/ShotLogged - Flag for interlock system</li>
   * </ul>
   */
  public ShotResultLogger() {
    // Create NetworkTables entries for dashboard buttons
    var table = NetworkTableInstance.getDefault().getTable("FiringSolver");
    
    hitButton = table.getBooleanTopic("LogHit").getEntry(false);
    missButton = table.getBooleanTopic("LogMiss").getEntry(false);
    
    // Interlock flag - set to true when shot is logged
    var interlockTable = NetworkTableInstance.getDefault().getTable("FiringSolver/Interlock");
    shotLoggedFlag = interlockTable.getBooleanTopic("ShotLogged").getEntry(true);
    
    // Set initial values
    hitButton.set(false);
    missButton.set(false);
    shotLoggedFlag.set(true);  // Start true so first shot is allowed
  }

  @Override
  public void periodic() {
    // Check if Hit button was pressed
    boolean currentHit = hitButton.get();
    if (currentHit && !lastHitValue) {
      // Button was just pressed - log as HIT
      FiringSolutionSolver.logShotResult(true);
      
      // Set flag for interlock system (allows next shot if interlock enabled)
      shotLoggedFlag.set(true);
      
      // Reset the button
      hitButton.set(false);
    }
    lastHitValue = currentHit;
    
    // Check if Miss button was pressed
    boolean currentMiss = missButton.get();
    if (currentMiss && !lastMissValue) {
      // Button was just pressed - log as MISS
      FiringSolutionSolver.logShotResult(false);
      
      // Set flag for interlock system (allows next shot if interlock enabled)
      shotLoggedFlag.set(true);
      
      // Reset the button
      missButton.set(false);
    }
    lastMissValue = currentMiss;
  }
}
