# Driver Station Button Mapping for Shot Logging

## Easy Shot Result Logging

The Bayesian tuner needs to know if each shot hit or missed the target. Drivers can easily log this using the Xbox controller buttons:

### Button Mappings

| Button | Action | Color Hint |
|--------|--------|------------|
| **X** | Log HIT ✅ | Green button (left) |
| **Y** | Log MISS ❌ | Yellow button (top) |

### How to Use

1. **After each shot**, the driver or coach observes if it hit the target
2. **Press X** if the shot **HIT** the target
3. **Press Y** if the shot **MISSED** the target

That's it! The tuner automatically:
- Records the shot result
- Combines it with distance, angle, and velocity data
- Uses Bayesian optimization to improve the parameters
- Updates the robot's shooting coefficients

### Tips for Best Results

- ✅ **Log every shot** - More data = better optimization
- ✅ **Be accurate** - Only press X if it truly hit
- ✅ **Press quickly** - Log right after the shot while it's fresh
- ✅ **During practice** - This is for practice tuning, not matches

### Why These Buttons?

- **X (green)** = Hit = Positive result = Easy to remember
- **Y (yellow)** = Miss = Warning/caution = Easy to remember
- Both buttons are on the right side of the controller, easy to reach
- Won't interfere with driving buttons (left stick, triggers, bumpers)

### Technical Details

When you press these buttons:
- The button press triggers `FiringSolutionSolver.logShotResult(true/false)`
- This logs the result to AdvantageKit via NetworkTables
- The Bayesian tuner daemon reads this from NetworkTables
- The tuner combines shot result with firing parameters
- Optimization updates happen automatically in the background

### Already Configured

This is already set up in:
- `src/main/java/frc/robot/outReach/RobotContainer.java` (lines 120-128)

No additional setup needed - just press the buttons!
