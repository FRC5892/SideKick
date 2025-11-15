# Quick Start Guide - FRC Shooter Bayesian Tuner

## For Drivers (3 Steps)

### Step 1: Install Dependencies

Open a terminal/command prompt and run:

```bash
pip install -r driver_station_tuner/requirements.txt
```

### Step 2: Configure Your Team

Edit `driver_station_tuner/run_tuner.py` and set your team number:

```python
# Line 17-18
TUNER_ENABLED = True
TEAM_NUMBER = 1234  # ← Change this to YOUR team number
```

### Step 3: Run the Tuner

```bash
python driver_station_tuner/run_tuner.py
```

That's it! The tuner will:
- Connect to your robot automatically
- Start tuning coefficients one at a time
- Log all data to `tuner_logs/` folder
- Display status updates in the console

Press `Ctrl+C` to stop.

---

## What It Does

The tuner watches your robot's shots and automatically adjusts the shooting parameters to improve accuracy. It uses Bayesian optimization (machine learning) to find the best values intelligently.

**It tunes these parameters in order:**
1. Drag coefficient (air resistance)
2. Velocity iteration count (accuracy vs speed)
3. Angle iteration count (accuracy vs speed)
4. Velocity tolerance (how precise)
5. Angle tolerance (how precise)
6. Launch height (physical measurement)

---

## When to Use It

✅ **DO use during:**
- Practice sessions
- Test shots with the robot
- After changing robot hardware

❌ **DON'T use during:**
- Competition matches (auto-disables anyway)
- When robot is not shooting
- If NetworkTables is unstable

---

## Checking Results

### View Logs

Logs are saved in `tuner_logs/` as CSV files:
```
tuner_logs/bayesian_tuner_20231115_143022.csv
```

Open with Excel, Google Sheets, or any spreadsheet program to analyze:
- Hit rates per coefficient
- Best values found
- Shot data over time

### View Status in NetworkTables

The tuner writes status to:
```
/FiringSolver/TunerStatus
```

Shows:
- Current coefficient being tuned
- Iteration number
- Current step size

---

## Common Issues

### "No module named 'numpy'"
**Solution:** Run `pip install -r driver_station_tuner/requirements.txt`

### "Cannot connect to NetworkTables"
**Solution:** 
1. Check robot IP in the script
2. Make sure robot is on and connected
3. Check that NetworkTables is enabled on robot

### "Tuner not doing anything"
**Solution:**
1. Make sure `TUNER_ENABLED = True`
2. Check that robot is shooting (tuner needs shot data)
3. Check console for error messages

### "Values not updating on robot"
**Solution:**
1. Make sure robot code is in tuning mode (`Constants.tuningMode = true`)
2. Check NetworkTables keys match between tuner and robot code
3. Verify robot code is reading from `/Tuning/FiringSolver/*` keys

---

## Advanced Options

### Change How Many Iterations

Edit `run_tuner.py`:

```python
config = TunerConfig()
config.N_CALLS_PER_COEFFICIENT = 15  # Default is 20
```

### Disable Specific Coefficients

```python
config.COEFFICIENTS["kLaunchHeight"].enabled = False
```

### Change Tuning Order

Edit `driver_station_tuner/config.py`, line ~70:

```python
TUNING_ORDER: List[str] = [
    "kDragCoefficient",      # Tune first
    "kVelocityIterationCount",  # Then this
    # ... etc
]
```

---

## Getting Help

1. Read the full [README.md](README.md) for detailed documentation
2. Check the log files in `tuner_logs/`
3. Run tests: `python driver_station_tuner/run_tests.py`
4. Ask your team's software lead

---

## Safety Features

The tuner automatically stops when:
- ⚠️ Match mode detected (FMS connected)
- ⚠️ NetworkTables disconnects
- ⚠️ Too many invalid shots received
- ⚠️ You press Ctrl+C

All coefficient values are automatically clamped to safe ranges.

---

## Need More Info?

📖 Full documentation: [README.md](README.md)

🧪 Run tests: `python driver_station_tuner/run_tests.py`

📊 View logs: Open CSV files in `tuner_logs/` folder
