# FOR DRIVERS - Simple Instructions

## The Tuner Runs Automatically

When programmers have set it up and enabled it:
- ✅ It starts automatically when you boot the Driver Station computer
- ✅ It runs in the background (you won't see it)
- ✅ It automatically tunes the robot when connected
- ✅ It automatically stops during matches
- ✅ You don't need to start or configure anything

---

## What You DO Need to Do

**After each shot, click a button in the dashboard:**

| Dashboard Button | When to Click |
|------------------|---------------|
| **FiringSolver/LogHit** | Shot hit the target ✅ |
| **FiringSolver/LogMiss** | Shot missed the target ❌ |

These buttons appear in AdvantageScope or Shuffleboard under the FiringSolver table.

See `SHOT_LOGGING_BUTTONS.md` for detailed instructions on finding and using these buttons.

---

## Where to Find Logs

If programmers ask you to check logs:

Look in folder: `tuner_logs/`

Two types of files:
1. `tuner_daemon.log` - System status (programmers care about this)
2. `bayesian_tuner_*.csv` - Tuning data (open in Excel)

---

## If Something Seems Wrong

1. Check that you're clicking the dashboard buttons after each shot
2. Reboot the Driver Station computer
3. Connect to robot
4. Tell a programmer

That's it. You're not expected to debug it.

---

## Summary

**Normal operation:**
- Tuner runs automatically in background
- You click dashboard buttons to log hits/misses
- Everything else is automatic

**Problem?** Tell a programmer
