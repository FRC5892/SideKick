# AUTO-START TUNER - Setup Instructions for Programmers

## Concept

**Drivers do NOTHING. The tuner runs automatically in the background.**

When the Driver Station computer starts:
1. Tuner daemon auto-starts
2. Checks if `enabled = True` in config
3. If enabled, connects to robot and starts tuning
4. If disabled, sleeps and does nothing

**Programmers:** Just set the boolean once
**Drivers:** Literally never think about it

---

## Setup (One-Time, Programmers Only)

### Step 1: Configure
Edit `tuner_config.ini`:
```ini
[tuner]
enabled = True        # ← Set this
team_number = 1234    # ← Set this
```

### Step 2: Install Auto-Start

#### On Windows Driver Station:

**Option A: Startup Folder (Easiest)**
1. Press `Win+R`, type `shell:startup`, press Enter
2. Create shortcut to `RUN_TUNER.bat` in that folder
3. Done! Runs on every login

**Option B: Task Scheduler (Better)**
1. Open Task Scheduler
2. Create Basic Task: "FRC Tuner"
3. Trigger: "When I log on"
4. Action: Start program `python.exe`
5. Arguments: `C:\path\to\tuner_daemon.py`
6. Done! Runs on every login

#### On Mac Driver Station:

1. Open System Preferences → Users & Groups
2. Click your user → Login Items
3. Click `+` and add `RUN_TUNER.sh`
4. Done! Runs on every login

#### On Linux:

Add to systemd:
```bash
sudo nano /etc/systemd/system/frc-tuner.service
```

Contents:
```ini
[Unit]
Description=FRC Shooter Tuner Daemon
After=network.target

[Service]
Type=simple
User=driver
WorkingDirectory=/path/to/SideKick
ExecStart=/usr/bin/python3 /path/to/SideKick/tuner_daemon.py
Restart=always

[Install]
WantedBy=multi-user.target
```

Enable:
```bash
sudo systemctl enable frc-tuner
sudo systemctl start frc-tuner
```

---

## How It Works

```
┌─────────────────────────────────────────────┐
│   Driver Station Computer Boots            │
└─────────────┬───────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────┐
│   tuner_daemon.py starts automatically      │
└─────────────┬───────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────┐
│   Reads tuner_config.ini                    │
│   • enabled = True/False                    │
│   • team_number = ????                      │
└─────────────┬───────────────────────────────┘
              │
         ┌────┴────┐
         │         │
         ▼         ▼
    enabled?    enabled?
       No         Yes
         │         │
         ▼         ▼
    ┌────────┐  ┌────────────────────────┐
    │ Sleep  │  │ Connect to robot       │
    │ Wait   │  │ Start tuning           │
    │ for    │  │ Run in background      │
    │ enable │  │ Drivers do nothing!    │
    └────────┘  └────────────────────────┘
```

---

## For Drivers

**What drivers need to do:** NOTHING

**Literally nothing. Don't even think about it.**

The tuner just works automatically when:
- ✅ Computer boots
- ✅ Robot connects
- ✅ Programmers enabled it

It automatically stops when:
- ✅ Match mode detected
- ✅ Robot disconnects

---

## Testing

After setting up auto-start:

1. **Reboot the Driver Station computer**
2. Connect to robot
3. Check `tuner_logs/tuner_daemon.log` file
4. Should see: "Tuner running in background"

That's it!

---

## To Enable/Disable (Programmers)

### Enable Tuner
```ini
# tuner_config.ini
[tuner]
enabled = True
```

Commit and push. Done.

### Disable Tuner
```ini
# tuner_config.ini
[tuner]
enabled = False
```

Commit and push. Done.

**No need to tell drivers anything. It just works.**

---

## Manual Testing (Before Auto-Start Setup)

To test without auto-start:

```bash
python tuner_daemon.py
```

Let it run. Check logs. If it works, set up auto-start.

---

## Logs

Daemon logs to: `tuner_logs/tuner_daemon.log`

Tuner data logs to: `tuner_logs/bayesian_tuner_*.csv`

Drivers never need to look at these. Programmers can review.

---

## Troubleshooting

**Daemon not starting?**
- Check auto-start is configured correctly
- Check Python is in PATH
- Check dependencies installed: `pip install -r driver_station_tuner/requirements.txt`

**Tuner not running?**
- Check `enabled = True` in config
- Check `tuner_daemon.log` for errors
- Check robot connection

**Tuner running when it shouldn't?**
- Set `enabled = False` in config
- Restart Driver Station computer (or restart daemon)

---

## Summary

**Programmers:**
1. Set `enabled = True` in `tuner_config.ini`
2. Set `team_number = ????`
3. Set up auto-start (one time)
4. Commit to repo

**Drivers:**
1. [Nothing]

**Result:**
- Tuner runs automatically
- No clicks needed
- No configuration needed by drivers
- Just works™
