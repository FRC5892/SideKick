# FOR DRIVERS - You Don't Need to Do Anything!

## The Tuner Runs Automatically

When programmers have set it up and enabled it:
- ✅ It starts automatically when you boot the Driver Station computer
- ✅ It runs in the background (you won't see it)
- ✅ It automatically tunes the robot when connected
- ✅ It automatically stops during matches
- ✅ You literally don't do anything

---

## Seriously, That's It

**You don't:**
- ❌ Click anything
- ❌ Configure anything
- ❌ Start anything
- ❌ Stop anything
- ❌ Think about it

**It just works when:**
- Programmers enabled it (`enabled = True` in config file)
- Robot is connected
- Not in a match

---

## If Programmers Ask You to Check It

**Where to find logs:**

Look in folder: `tuner_logs/`

Two types of files:
1. `tuner_daemon.log` - Did it start? (programmers care about this)
2. `bayesian_tuner_*.csv` - Tuning data (open in Excel)

---

## If Something Seems Wrong

1. Reboot the Driver Station computer
2. Connect to robot
3. Tell a programmer

That's it. You're not expected to debug it.

---

## Summary

**Normal operation:** You do nothing, it just works

**Problem?** Tell a programmer

**That's literally all you need to know.**
