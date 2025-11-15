# Dashboard Button Guide for Shot Logging

## Easy Shot Result Logging via Dashboard

The Bayesian tuner needs to know if each shot hit or missed the target. Drivers can easily log this using **dashboard buttons** in AdvantageScope or Shuffleboard.

### Dashboard Button Locations

In AdvantageScope or Shuffleboard, find these buttons under **FiringSolver**:

| Button Path | Action | When to Click |
|-------------|--------|---------------|
| **FiringSolver/LogHit** | Log HIT ✅ | Shot hit the target |
| **FiringSolver/LogMiss** | Log MISS ❌ | Shot missed the target |

### How to Use

1. **After each shot**, the driver or coach observes if it hit the target
2. **Click "LogHit"** in the dashboard if the shot **HIT** the target
3. **Click "LogMiss"** in the dashboard if the shot **MISSED** the target

That's it! The tuner automatically:
- Records the shot result via NetworkTables
- Combines it with distance, angle, and velocity data
- Uses Bayesian optimization to improve the parameters
- Updates the robot's shooting coefficients

### Setting Up the Dashboard

#### In AdvantageScope:
1. Open AdvantageScope and connect to the robot
2. Navigate to the **NetworkTables** tab
3. Find **FiringSolver** → **LogHit** and **LogMiss**
4. These appear as boolean toggles - click to activate

#### In Shuffleboard:
1. Open Shuffleboard and connect to the robot
2. Add widgets for:
   - `NetworkTables/FiringSolver/LogHit` (Toggle Button widget)
   - `NetworkTables/FiringSolver/LogMiss` (Toggle Button widget)
3. Click these buttons after each shot

### Tips for Best Results

- ✅ **Log every shot** - More data = better optimization
- ✅ **Be accurate** - Only click Hit if it truly hit
- ✅ **Click quickly** - Log right after the shot while it's fresh
- ✅ **During practice** - This is for practice tuning, not matches

### Why Dashboard Buttons?

- **Easy access** - Visible on any device running the dashboard
- **No controller needed** - Works from driver station computer or coach laptop
- **Multiple people can log** - Driver, coach, or observer can all access
- **Visual feedback** - Can see when button is pressed in the dashboard

### Technical Details

When you click these buttons:
- The button state changes in NetworkTables (`FiringSolver/LogHit` or `LogMiss`)
- `ShotResultLogger` subsystem monitors these buttons in its periodic method
- When pressed, it calls `FiringSolutionSolver.logShotResult(true/false)`
- This logs the result to AdvantageKit
- The Bayesian tuner daemon reads this from NetworkTables
- The tuner combines shot result with firing parameters
- Optimization updates happen automatically in the background

### Already Configured

This is already set up in:
- `src/main/java/frc/robot/generic/util/ShotResultLogger.java` (button handler)
- `src/main/java/frc/robot/outReach/RobotContainer.java` (subsystem initialization)

No additional setup needed - just click the buttons in your dashboard!
