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

/**
 * Manages shooting interlocks for Bayesian tuner data quality.
 * 
 * <p>Provides two safety interlocks that can be enabled/disabled via NetworkTables:
 * <ul>
 *   <li><b>Shot Logged Interlock:</b> Prevents shooting until previous shot is logged (Hit/Miss clicked)</li>
 *   <li><b>Coefficients Updated Interlock:</b> Prevents shooting until tuner updates coefficients</li>
 * </ul>
 * 
 * <p>Both interlocks default to DISABLED for normal operation.
 * Enable them in tuner_config.ini for maximum data quality during tuning sessions.
 * 
 * <p><b>Usage:</b><pre>
 * ShooterInterlock interlock = new ShooterInterlock();
 * 
 * // Check before allowing shot
 * if (interlock.canShoot()) {
 *   shooter.fire();
 *   interlock.onShotFired();  // Mark that shot was taken
 * }
 * 
 * // Call in periodic()
 * interlock.periodic();
 * </pre>
 */
public class ShooterInterlock {
  
  private final BooleanEntry requireShotLoggedEntry;
  private final BooleanEntry requireCoefficientsUpdatedEntry;
  private final BooleanEntry shotLoggedEntry;
  private final BooleanEntry coefficientsUpdatedEntry;
  private final BooleanEntry canShootEntry;
  
  private boolean lastShotLogged = true;  // Start true so first shot is allowed
  private boolean lastCoefficientsUpdated = true;  // Start true so first shot is allowed
  
  private boolean requireShotLogged = false;
  private boolean requireCoefficientsUpdated = false;

  /**
   * Creates a new ShooterInterlock instance.
   * 
   * <p>Initializes NetworkTables entries:
   * <ul>
   *   <li>/FiringSolver/Interlock/RequireShotLogged - Enable/disable shot logging interlock</li>
   *   <li>/FiringSolver/Interlock/RequireCoefficientsUpdated - Enable/disable coefficient interlock</li>
   *   <li>/FiringSolver/Interlock/ShotLogged - Set true when driver logs hit/miss</li>
   *   <li>/FiringSolver/Interlock/CoefficientsUpdated - Set true when tuner updates coefficients</li>
   *   <li>/FiringSolver/Interlock/CanShoot - Output: true if robot is allowed to shoot</li>
   * </ul>
   */
  public ShooterInterlock() {
    var table = NetworkTableInstance.getDefault().getTable("FiringSolver/Interlock");
    
    // Configuration entries (set by tuner daemon from tuner_config.ini)
    requireShotLoggedEntry = table.getBooleanTopic("RequireShotLogged").getEntry(false);
    requireCoefficientsUpdatedEntry = table.getBooleanTopic("RequireCoefficientsUpdated").getEntry(false);
    
    // Status entries (managed by tuner daemon and ShotResultLogger)
    shotLoggedEntry = table.getBooleanTopic("ShotLogged").getEntry(true);
    coefficientsUpdatedEntry = table.getBooleanTopic("CoefficientsUpdated").getEntry(true);
    
    // Output entry (computed by this class)
    canShootEntry = table.getBooleanTopic("CanShoot").getEntry(true);
    
    // Initialize to safe defaults
    requireShotLoggedEntry.set(false);
    requireCoefficientsUpdatedEntry.set(false);
    shotLoggedEntry.set(true);
    coefficientsUpdatedEntry.set(true);
    canShootEntry.set(true);
  }

  /**
   * Call this in periodic() to update interlock status.
   * 
   * <p>Reads configuration and status from NetworkTables and updates canShoot output.
   */
  public void periodic() {
    // Read configuration
    requireShotLogged = requireShotLoggedEntry.get();
    requireCoefficientsUpdated = requireCoefficientsUpdatedEntry.get();
    
    // Read status flags
    lastShotLogged = shotLoggedEntry.get();
    lastCoefficientsUpdated = coefficientsUpdatedEntry.get();
    
    // Compute if shooting is allowed
    boolean canShoot = computeCanShoot();
    canShootEntry.set(canShoot);
  }

  /**
   * Checks if robot is allowed to shoot based on active interlocks.
   * 
   * @return true if shooting is allowed, false if blocked by an interlock
   */
  public boolean canShoot() {
    return computeCanShoot();
  }
  
  private boolean computeCanShoot() {
    // If shot logging interlock is enabled, check if last shot was logged
    if (requireShotLogged && !lastShotLogged) {
      return false;  // Blocked: waiting for driver to log previous shot
    }
    
    // If coefficients interlock is enabled, check if coefficients were updated
    if (requireCoefficientsUpdated && !lastCoefficientsUpdated) {
      return false;  // Blocked: waiting for tuner to update coefficients
    }
    
    // All checks passed
    return true;
  }

  /**
   * Call this immediately after robot fires a shot.
   * 
   * <p>Resets the interlock flags so robot will wait for:
   * <ul>
   *   <li>Driver to log hit/miss (if that interlock is enabled)</li>
   *   <li>Tuner to update coefficients (if that interlock is enabled)</li>
   * </ul>
   */
  public void onShotFired() {
    // Reset flags - robot must now wait for these to be set true again
    shotLoggedEntry.set(false);
    coefficientsUpdatedEntry.set(false);
  }

  /**
   * Gets the current interlock status as a human-readable string.
   * 
   * @return Status string describing which interlocks are active and blocking
   */
  public String getInterlockStatus() {
    if (canShoot()) {
      return "Ready to shoot";
    }
    
    StringBuilder status = new StringBuilder("Blocked: ");
    
    if (requireShotLogged && !lastShotLogged) {
      status.append("Waiting for shot to be logged | ");
    }
    
    if (requireCoefficientsUpdated && !lastCoefficientsUpdated) {
      status.append("Waiting for coefficient update | ");
    }
    
    // Remove trailing " | "
    if (status.length() > 9) {
      status.setLength(status.length() - 3);
    }
    
    return status.toString();
  }

  /**
   * Checks if shot logging interlock is currently enabled.
   * 
   * @return true if robot must wait for shot logging
   */
  public boolean isRequireShotLogged() {
    return requireShotLogged;
  }

  /**
   * Checks if coefficient update interlock is currently enabled.
   * 
   * @return true if robot must wait for coefficient updates
   */
  public boolean isRequireCoefficientsUpdated() {
    return requireCoefficientsUpdated;
  }
}
