# 🎯 FRC Bayesian Shooter Tuner - Quick Start

**Ultra-simple automatic shooter parameter optimization for FRC Team 5892**

---

## For Drivers: Just Click Buttons! 🎮

After each shot:
- Click **LogHit** (green) if shot hit
- Click **LogMiss** (red) if shot missed

That's it! The tuner runs automatically in the background.

📖 **See [DASHBOARD_BUTTON_GUIDE.md](DASHBOARD_BUTTON_GUIDE.md) for dashboard setup**

---

## For Programmers: Two Files to Edit 📝

### 1. **TUNER_TOGGLES.ini** - Three Main Switches
```ini
tuner_enabled = True                      # Turn tuner on/off
require_shot_logged = False               # Block shooting until logged
require_coefficients_updated = False      # Block shooting until optimized
```

###  2. **COEFFICIENT_TUNING.py** - What to Tune
```python
TUNING_ORDER = [
    "kDragCoefficient",          # Tune this first
    "kVelocityIterationCount",   # Then this
    # ... customize order here
]

COEFFICIENTS = {
    "kDragCoefficient": {
        "enabled": True,         # ← Turn on/off
        "min_value": 0.001,      # ← Safety limits
        "max_value": 0.006,
        "initial_step_size": 0.001,  # ← How much to change
        ...
    },
    ...
}
```

---

## File Structure

```
SideKick/
├── TUNER_TOGGLES.ini          ← EDIT: Three main on/off switches
├── COEFFICIENT_TUNING.py       ← EDIT: What to tune, how much, order
├── tuner_daemon.py             ← Auto-starts tuner in background
├── RUN_TUNER.bat/.sh           ← Add to system startup (one time)
│
├── DASHBOARD_BUTTON_GUIDE.md   ← Setup instructions for drivers
├── DRIVERS_START_HERE.md       ← Driver overview
├── AUTO_START_SETUP.md         ← One-time programmer setup
│
└── driver_station_tuner/       ← Python modules (don't edit these)
    ├── config.py               ← Loads from TUNER_TOGGLES.ini and COEFFICIENT_TUNING.py
    ├── optimizer.py            ← Bayesian optimization
    ├── nt_interface.py         ← NetworkTables + RoboRIO protection
    ├── logger.py               ← CSV data logging
    ├── tuner.py                ← Main coordinator
    └── tests/                  ← Unit tests
```

---

## What Makes This Perfect? ✨

### Zero Driver Burden
✅ Tuner auto-starts on computer boot
✅ Runs silently in background
✅ Drivers just click hit/miss buttons
✅ No configuration needed by drivers

### Easy for Programmers
✅ **Two simple files** to customize (TUNER_TOGGLES.ini, COEFFICIENT_TUNING.py)
✅ **No code changes** needed for most adjustments
✅ **Clear comments** explaining every setting
✅ **Examples** showing how to modify everything

### RoboRIO Protection
✅ **Rate limiting** prevents NT spam (5 Hz writes max, 20 Hz reads max)
✅ **Physical limits** reject impossible sensor readings
✅ **Iteration caps** prevent CPU overload (max 30, not 50)
✅ **Batch writes** reduce network traffic

### Safety
✅ **Auto-disable** during matches (FMS detection)
✅ **Coefficient clamping** to safe ranges
✅ **Invalid data rejection**
✅ **Optional interlocks** for intensive tuning

### Quality
✅ **Complete data capture** (17+ fields per shot)
✅ **Bayesian optimization** (smart, not random)
✅ **Adaptive step sizes** (big steps → fine tuning)
✅ **Full CSV logging** for offline analysis

---

## Setup (One Time) 🚀

1. **Install dependencies:**
   ```bash
   pip install -r driver_station_tuner/requirements.txt
   ```

2. **Configure (if needed):**
   - Edit `TUNER_TOGGLES.ini` (team number already 5892)
   - Edit `COEFFICIENT_TUNING.py` if you want different tuning

3. **Set up auto-start:**
   - Windows: Add `RUN_TUNER.bat` to Startup folder
   - Mac/Linux: See `AUTO_START_SETUP.md`

4. **Deploy robot code** with dashboard button handlers

Done! Tuner now starts automatically when computer boots.

---

## How It Works 🔧

```
1. Computer boots → tuner_daemon.py auto-starts
2. Reads TUNER_TOGGLES.ini (enabled=True, team=5892)
3. Reads COEFFICIENT_TUNING.py (what to tune, how much, order)
4. Connects to robot (10.58.92.2)
5. Robot shoots, driver clicks LogHit or LogMiss
6. Tuner captures ALL robot state (distance, velocity, angles, coefficients)
7. Bayesian optimizer analyzes patterns
8. Suggests improved coefficient value
9. Writes to NetworkTables (rate-limited to protect RoboRIO)
10. Robot uses new value for next shot
11. Repeat → progressively better accuracy
```

---

## Documentation

| File | For Who | Purpose |
|------|---------|---------|
| **THIS FILE** | Everyone | Quick overview |
| **TUNER_TOGGLES.ini** | Programmers | Three main switches |
| **COEFFICIENT_TUNING.py** | Programmers | Detailed tuning config |
| **DASHBOARD_BUTTON_GUIDE.md** | Drivers | Button setup guide |
| **DRIVERS_START_HERE.md** | Drivers | System overview |
| **AUTO_START_SETUP.md** | Programmers | Auto-start instructions |
| **SHOT_LOGGING_BUTTONS.md** | Drivers/Coaches | Quick reference |
| **driver_station_tuner/README.md** | Developers | Technical details |
| **driver_station_tuner/MAINTAINER_GUIDE.md** | Developers | Code architecture |

---

## Testing

Run unit tests:
```bash
python driver_station_tuner/run_tests.py
```

All 29 tests should pass ✅

---

## Common Tasks

### Disable the tuner
```ini
# TUNER_TOGGLES.ini
tuner_enabled = False
```

### Only tune drag coefficient
```python
# COEFFICIENT_TUNING.py
TUNING_ORDER = ["kDragCoefficient"]
```

### Make tuning more aggressive
```python
# COEFFICIENT_TUNING.py
"kDragCoefficient": {
    "initial_step_size": 0.002,  # Change from 0.001
    ...
}
```

### Tighten safety limits
```python
# COEFFICIENT_TUNING.py
"kDragCoefficient": {
    "min_value": 0.002,  # Change from 0.001
    "max_value": 0.004,  # Change from 0.006
    ...
}
```

### Reduce RoboRIO load
```python
# COEFFICIENT_TUNING.py
MAX_WRITE_RATE_HZ = 2.0  # Change from 5.0
```

---

## Questions?

- **Drivers:** See [DASHBOARD_BUTTON_GUIDE.md](DASHBOARD_BUTTON_GUIDE.md)
- **Setup:** See [AUTO_START_SETUP.md](AUTO_START_SETUP.md)
- **Technical:** See driver_station_tuner/README.md
- **Code:** See driver_station_tuner/MAINTAINER_GUIDE.md

---

**Built for FRC Team 5892 | Production Ready | Fully Tested | Well Documented**
