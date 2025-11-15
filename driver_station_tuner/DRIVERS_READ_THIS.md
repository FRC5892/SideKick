# FOR DRIVERS - Super Simple Instructions

## You Only Need to Do 3 Things:

### 1. Install (First Time Only)

Open terminal/command prompt, type this:
```bash
pip install -r driver_station_tuner/requirements.txt
```
Wait for it to finish. That's it.

---

### 2. Edit ONE File

Open: **`START_TUNER.py`**

Change these TWO lines at the top:

```python
ENABLE_TUNER = True        # Set to False to disable

YOUR_TEAM_NUMBER = 1234    # Put your actual team number
```

Save the file.

---

### 3. Run It

**Double-click** `START_TUNER.py`

OR in terminal:
```bash
python START_TUNER.py
```

That's it! You're done!

---

## What You'll See

When it's working:
```
============================================================
FRC SHOOTER TUNER - EASY MODE
============================================================

✅ Team 1234
   Robot IP: 10.12.34.2

🎯 Starting tuner...
   Press Ctrl+C to stop
============================================================

Connected to NetworkTables successfully
Tuning kDragCoefficient (iter 0, step 0.001000)
```

To stop: Press **Ctrl+C**

---

## That's All You Need to Know!

The tuner will:
- ✅ Connect automatically
- ✅ Start tuning automatically
- ✅ Save logs automatically
- ✅ Stop during matches automatically

**You don't need to:**
- ❌ Configure anything else
- ❌ Watch it constantly
- ❌ Understand the code
- ❌ Touch any other files

---

## If Something Goes Wrong

### Can't connect?
- Check robot is on
- Check team number is correct

### Module not found?
- Run: `pip install -r driver_station_tuner/requirements.txt`

### Not doing anything?
- Make sure robot is shooting
- Check `ENABLE_TUNER = True`

---

## Where Are the Results?

Look in folder: **`tuner_logs/`**

Open the CSV file with Excel or Google Sheets.

---

## Want More Control?

Read the full docs: [README.md](README.md)

But honestly? You probably don't need to.

**Just set the two variables and run it. Done.**
