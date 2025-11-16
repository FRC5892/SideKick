# Quick Start Guide - FRC Shooter Bayesian Tuner

## For Programmers (One-Time Setup)

### Step 1: Install Dependencies

```bash
pip install -r bayesopt/tuner/requirements.txt
```

### Step 2: Configure

Edit `tuner_config.ini` (already set to team 5892):
```ini
[tuner]
enabled = True
team_number = 5892
```

### Step 3: Set Up Auto-Start

**Windows:**
- Add `RUN_TUNER.bat` to Startup folder (Win+R → `shell:startup`)

**Mac:**
- Add `RUN_TUNER.sh` to Login Items

**Linux:**
- See `AUTO_START_SETUP.md` for systemd service setup

### Step 4: Deploy Robot Code

The robot code includes `ShotResultLogger` subsystem which creates dashboard buttons for logging shot results.

---

## For Drivers

### What the Tuner Does Automatically

- ✅ Starts when Driver Station computer boots
- ✅ Connects to robot (10.58.92.2)
- ✅ Runs in background
- ✅ Reads shot data from robot
- ✅ Optimizes shooting parameters
- ✅ Updates robot coefficients
- ✅ Stops during competition matches

### What Drivers Do

**After each shot, click a dashboard button:**

- **FiringSolver/LogHit** → Shot hit target ✅
- **FiringSolver/LogMiss** → Shot missed target ❌

Find these buttons in:
- **AdvantageScope**: NetworkTables → FiringSolver
- **Shuffleboard**: Add boolean widgets for these entries

See `SHOT_LOGGING_BUTTONS.md` for detailed dashboard setup.

---

## Where Logs Are Saved

- **System logs**: `tuner_logs/tuner_daemon.log`
- **Tuning data**: `tuner_logs/bayesian_tuner_YYYYMMDD_HHMMSS.csv`

---

## Configuration

All settings in `tuner_config.ini`:

```ini
[tuner]
enabled = True        # Master on/off switch
team_number = 5892    # FRC team number

[optimization]
iterations_per_coefficient = 20  # Tuning iterations
update_rate_hz = 10.0           # Check rate

[logging]
log_directory = ./tuner_logs
log_to_console = True
```

Advanced coefficient settings in `bayesopt/tuner/config.py`

---

## Verification

After setup:

1. **Reboot Driver Station computer**
2. **Connect to robot**
3. **Check** `tuner_logs/tuner_daemon.log` for "Tuner running"
4. **Open dashboard** and verify FiringSolver/LogHit and LogMiss buttons exist

---

## Troubleshooting

**Tuner not starting?**
- Check `tuner_logs/tuner_daemon.log`
- Verify `enabled = True` in config
- Check dependencies installed

**Can't find dashboard buttons?**
- Robot code must be deployed with `ShotResultLogger`
- Check NetworkTables is connected
- Look under FiringSolver table

**Not tuning?**
- Drivers must click buttons after each shot
- Check robot is shooting (generates data)
- Verify not in match mode (FMS attached)

---

## Documentation

- `SHOT_LOGGING_BUTTONS.md` - Dashboard button guide
- `DRIVERS_START_HERE.md` - Driver instructions
- `AUTO_START_SETUP.md` - Detailed setup
- `README.md` - Complete technical docs
- `MAINTAINER_GUIDE.md` - Code maintenance

---

## Quick Reference

| Task | Action |
|------|--------|
| Enable tuner | Set `enabled = True` in `tuner_config.ini` |
| Disable tuner | Set `enabled = False` in `tuner_config.ini` |
| Log hit | Click **FiringSolver/LogHit** in dashboard |
| Log miss | Click **FiringSolver/LogMiss** in dashboard |
| View logs | Open `tuner_logs/*.csv` in Excel |
| Check status | Read `tuner_logs/tuner_daemon.log` |
