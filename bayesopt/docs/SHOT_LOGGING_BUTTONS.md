# Dashboard Button Guide for Shot Logging

## 🎯 Quick Start - Which Button is Which?

| Button Name | When to Click | Color | Location |
|-------------|---------------|-------|----------|
| **LogHit** | Shot HIT the target ✅ | 🟢 GREEN | FiringSolver/LogHit |
| **LogMiss** | Shot MISSED the target ❌ | 🔴 RED | FiringSolver/LogMiss |

**Simple rule: Hit = LogHit, Miss = LogMiss. That's it!**

---

## 📱 Finding the Buttons (Step-by-Step)

### In AdvantageScope (Recommended for Drivers)

**Step 1:** Open AdvantageScope and connect to your robot

**Step 2:** Click the **"NetworkTables"** tab on the left side

**Step 3:** Look for the **"FiringSolver"** folder and expand it

**Step 4:** You'll see:
```
📁 FiringSolver/
   ├── 🔘 LogHit    ← HIT button (click when shot hits)
   ├── 🔘 LogMiss   ← MISS button (click when shot misses)
   └── ... (other robot data)
```

**Step 5:** Click the button that matches what happened:
- Shot hit? Click **LogHit**
- Shot missed? Click **LogMiss**

The button will flash and reset automatically - you're done!

### In Shuffleboard (Good for Custom Layouts)

**Step 1:** Open Shuffleboard and connect to your robot

**Step 2:** Right-click on your layout → **"Add..."** → **"NetworkTables"**

**Step 3:** Find and add these two entries:
- `FiringSolver/LogHit` → Choose **"Toggle Button"** widget
- `FiringSolver/LogMiss` → Choose **"Toggle Button"** widget

**Step 4:** Customize for clarity (IMPORTANT!)
- **LogHit button**: Set background color to **GREEN** (#00FF00)
- **LogMiss button**: Set background color to **RED** (#FF0000)
- **Make them LARGE** - At least 100x100 pixels each
- **Add text labels** - "✅ HIT" and "❌ MISS"

**Step 5:** Arrange side-by-side for quick access

**Step 6:** Save your layout! (**File → Save Layout**)

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
