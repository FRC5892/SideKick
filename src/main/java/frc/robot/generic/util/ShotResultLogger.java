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
 * Dashboard button handler for logging shot results to the Bayesian tuner.
 *
 * <h2>Purpose</h2>
 *
 * <p>Creates two clickable buttons in the dashboard (AdvantageScope/Shuffleboard) that drivers use
 * to log whether each shot hit or missed the target. This provides the critical feedback data that
 * the Bayesian optimizer needs to improve shooting accuracy.
 *
 * <h2>Dashboard Button Locations</h2>
 *
 * <ul>
 *   <li><b>/FiringSolver/LogHit</b> - Click when shot HITS the target (✅ GREEN button)
 *   <li><b>/FiringSolver/LogMiss</b> - Click when shot MISSES the target (❌ RED button)
 * </ul>
 *
 * <h2>How It Works</h2>
 *
 * <ol>
 *   <li>Driver observes shot result (hit or miss)
 *   <li>Driver clicks appropriate button in dashboard
 *   <li>This subsystem detects the button press in periodic()
 *   <li>Calls {@link FiringSolutionSolver#logShotResult(boolean)} to log to AdvantageKit
 *   <li>Sets ShotLogged flag for {@link ShooterInterlock} (if interlock enabled)
 *   <li>Resets button to OFF automatically
 * </ol>
 *
 * <h2>Integration with Tuner</h2>
 *
 * <p>The Python Bayesian tuner reads the logged hit/miss data from NetworkTables along with all the
 * robot state at shot time (distance, velocity, angles, current coefficients) to learn patterns and
 * optimize shooting parameters.
 *
 * <h2>Maintenance Notes</h2>
 *
 * <ul>
 *   <li>This subsystem must be instantiated in RobotContainer for buttons to work
 *   <li>Buttons automatically reset after each click (no driver cleanup needed)
 *   <li>Edge detection (lastXValue) prevents multiple logs from single click
 *   <li>ShotLogged flag integrates with optional shooting interlock system
 * </ul>
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * // In RobotContainer.java:
 * private final ShotResultLogger shotLogger;
 *
 * public RobotContainer() {
 *   shotLogger = new ShotResultLogger();
 *   // Buttons now appear in dashboard - drivers just click!
 * }
 * }</pre>
 *
 * @see FiringSolutionSolver#logShotResult(boolean)
 * @see ShooterInterlock
 */
public class ShotResultLogger extends SubsystemBase {

  // NetworkTables entries for dashboard buttons
  private final BooleanEntry hitButton; // Driver clicks when shot hits
  private final BooleanEntry missButton; // Driver clicks when shot misses
  private final BooleanEntry shotLoggedFlag; // For shooting interlock system

  // Edge detection - remember previous button states to detect clicks
  private boolean lastHitValue = false; // Was hit button pressed last cycle?
  private boolean lastMissValue = false; // Was miss button pressed last cycle?

  /**
   * Creates a new ShotResultLogger subsystem.
   *
   * <p>Initializes NetworkTables entries for the dashboard buttons and interlock flag:
   *
   * <ul>
   *   <li><b>/FiringSolver/LogHit</b> - Hit button (driver clicks, system reads)
   *   <li><b>/FiringSolver/LogMiss</b> - Miss button (driver clicks, system reads)
   *   <li><b>/FiringSolver/Interlock/ShotLogged</b> - Signals shot was logged (system writes)
   * </ul>
   *
   * <p>All buttons start in the OFF (false) state. The ShotLogged flag starts TRUE so the first
   * shot is allowed (if interlock is enabled).
   */
  public ShotResultLogger() {
    // Get the main FiringSolver table for dashboard buttons
    var table = NetworkTableInstance.getDefault().getTable("FiringSolver");

    // Create dashboard button entries
    // Drivers see these as clickable buttons/toggles in AdvantageScope or Shuffleboard
    hitButton = table.getBooleanTopic("LogHit").getEntry(false);
    missButton = table.getBooleanTopic("LogMiss").getEntry(false);

    // Create interlock flag entry (for optional shooting interlock feature)
    // This tells the robot it's OK to shoot again after shot is logged
    var interlockTable = NetworkTableInstance.getDefault().getTable("FiringSolver/Interlock");
    shotLoggedFlag = interlockTable.getBooleanTopic("ShotLogged").getEntry(true);

    // Initialize all entries to safe defaults
    hitButton.set(false); // Button starts OFF
    missButton.set(false); // Button starts OFF
    shotLoggedFlag.set(true); // Start TRUE so first shot is allowed
  }

  /**
   * Periodic function called every robot loop (~20ms / 50Hz).
   *
   * <p>Monitors the dashboard buttons for clicks using edge detection:
   *
   * <ol>
   *   <li>Read current button state from NetworkTables
   *   <li>Compare to previous state (lastXValue)
   *   <li>If changed from false→true, driver just clicked!
   *   <li>Process the click (log result, set flags, reset button)
   *   <li>Update lastXValue for next cycle
   * </ol>
   *
   * <p>This runs continuously, but only acts when a button click is detected.
   */
  @Override
  public void periodic() {
    // ============================================================
    // Check HIT button for clicks
    // ============================================================

    boolean currentHit = hitButton.get(); // Read current state from dashboard

    // Edge detection: has button gone from OFF to ON? (driver just clicked)
    if (currentHit && !lastHitValue) {
      // ✅ HIT button was just clicked!

      // Log the hit to AdvantageKit (writes to NetworkTables)
      FiringSolutionSolver.logShotResult(true); // true = HIT

      // Set interlock flag = true (allows next shot if interlock is enabled)
      // If interlock is disabled, this does nothing but doesn't hurt
      shotLoggedFlag.set(true);

      // Reset button to OFF so it's ready for next shot
      // Driver doesn't need to manually reset it
      hitButton.set(false);
    }

    // Remember current state for next cycle (for edge detection)
    lastHitValue = currentHit;

    // ============================================================
    // Check MISS button for clicks (same logic as above)
    // ============================================================

    boolean currentMiss = missButton.get(); // Read current state from dashboard

    // Edge detection: has button gone from OFF to ON? (driver just clicked)
    if (currentMiss && !lastMissValue) {
      // ❌ MISS button was just clicked!

      // Log the miss to AdvantageKit (writes to NetworkTables)
      FiringSolutionSolver.logShotResult(false); // false = MISS

      // Set interlock flag = true (allows next shot if interlock is enabled)
      shotLoggedFlag.set(true);

      // Reset button to OFF so it's ready for next shot
      missButton.set(false);
    }

    // Remember current state for next cycle (for edge detection)
    lastMissValue = currentMiss;
  }
}
