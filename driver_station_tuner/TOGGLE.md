# Boolean Toggle - How to Enable/Disable the Tuner

## The One-Line Solution

**To ENABLE the tuner:**
```python
TUNER_ENABLED = True
```

**To DISABLE the tuner:**
```python
TUNER_ENABLED = False
```

That's it! Just one boolean variable.

---

## Where to Find It

Open: `driver_station_tuner/run_tuner.py`

Look for line 20:

```python
def main():
    """Main entry point for the tuner."""

    # ========== DRIVER CONFIGURATION ==========
    # Set to True to enable tuner, False to disable
    TUNER_ENABLED = True    # ← Change this line!

    # Set your team number (e.g., 1234 becomes "10.12.34.2")
    TEAM_NUMBER = 0  # Replace with your team number
```

---

## Examples

### Example 1: Enable for Practice
```python
TUNER_ENABLED = True
TEAM_NUMBER = 1234
```
Run: `python driver_station_tuner/run_tuner.py`
Result: ✅ Tuner starts and begins optimizing

### Example 2: Disable for Match
```python
TUNER_ENABLED = False
TEAM_NUMBER = 1234
```
Run: `python driver_station_tuner/run_tuner.py`
Result: ✅ Prints "Tuner is DISABLED" and exits

---

## Automatic Safety Disabling

Even with `TUNER_ENABLED = True`, the tuner **automatically disables** when:

1. ⚠️ **Match mode detected** (FMS/Field Management System connected)
   - Checks NetworkTables `/FMSInfo/FMSControlData`
   - Pauses tuning during competition matches

2. ⚠️ **NetworkTables disconnects**
   - Loses connection to robot
   - Pauses until connection restored

3. ⚠️ **Too many invalid shots**
   - Data quality too low
   - Safety stop to prevent bad updates

So you can safely leave `TUNER_ENABLED = True` and the system will protect itself!

---

## Alternative: Config File Toggle

You can also control it programmatically:

```python
from driver_station_tuner import TunerConfig

config = TunerConfig()
config.TUNER_ENABLED = True  # or False

# Then use the config
from driver_station_tuner import run_tuner
run_tuner(config=config)
```

---

## Verification

Check if tuner is enabled in your config:

```python
from driver_station_tuner import TunerConfig
config = TunerConfig()
print(f"Tuner enabled: {config.TUNER_ENABLED}")
```

---

## Summary

| Setting | Result |
|---------|--------|
| `TUNER_ENABLED = True` | ✅ Tuner runs (subject to safety checks) |
| `TUNER_ENABLED = False` | ❌ Tuner does not start |

**Simple. One boolean. One line of code.**
