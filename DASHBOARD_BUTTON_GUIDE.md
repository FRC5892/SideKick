# Dashboard Button Visual Guide for Drivers

## 🎯 Finding the Buttons in Your Dashboard

### In AdvantageScope (Recommended)

1. **Open AdvantageScope** and connect to the robot
2. **Click on the "NetworkTables" tab** on the left sidebar
3. **Expand the "FiringSolver" folder** in the tree view
4. **You'll see two buttons:**

```
📁 FiringSolver/
  ├── 🔘 LogHit        ← Click this when shot HITS the target
  ├── 🔘 LogMiss       ← Click this when shot MISSES the target
  └── ... (other data)
```

**Visual Appearance:**
- The buttons show as **boolean toggles** (checkboxes or toggle switches)
- They reset to OFF automatically after you click them
- Click once = logged, that's it!

### In Shuffleboard

1. **Open Shuffleboard** and connect to the robot
2. **Right-click on your layout** → "Add..." → "NetworkTables"
3. **Add these two entries:**
   - `FiringSolver/LogHit` → Choose **"Toggle Button"** widget
   - `FiringSolver/LogMiss` → Choose **"Toggle Button"** widget

4. **Customize for clarity:**
   - **LogHit button**: Change color to **GREEN** 
   - **LogMiss button**: Change color to **RED**
   - **Make them BIG** - drivers need to click quickly!

### Recommended Shuffleboard Layout

```
┌─────────────────────────────────────────┐
│  SHOT RESULT LOGGING                    │
├─────────────────────────────────────────┤
│                                         │
│  ┌──────────────────┐  ┌──────────────┐│
│  │                  │  │              ││
│  │   ✅ HIT         │  │   ❌ MISS    ││
│  │                  │  │              ││
│  │  (Click when     │  │ (Click when  ││
│  │   shot hits)     │  │  shot misses)││
│  │                  │  │              ││
│  └──────────────────┘  └──────────────┘│
│        GREEN                  RED       │
│                                         │
└─────────────────────────────────────────┘
```

**Tips for Setup:**
- Place buttons prominently - NOT hidden in a corner
- Make them at least 100x100 pixels each
- Put them side-by-side for easy access
- Label clearly with text overlays if possible

---

## 🎮 How to Use During Practice

### Simple Workflow:

1. **Robot shoots** → 📸 Observe the result
2. **Shot hits target?**
   - ✅ **YES** → Click **LogHit** (green button)
   - ❌ **NO** → Click **LogMiss** (red button)
3. **Done!** Button resets automatically

### Quick Reference Card

| What Happened? | Which Button? | Color |
|----------------|---------------|-------|
| 🎯 Shot hit the target | **LogHit** | 🟢 GREEN |
| 🎯 Shot missed the target | **LogMiss** | 🔴 RED |
| 🤷 Not sure / Can't see | DON'T CLICK | Wait for next shot |

---

## 🚦 Visual Indicators

### You'll Know It's Working When:

✅ **Button flashes briefly** when you click  
✅ **Button returns to OFF** automatically (0.1 seconds later)  
✅ **Tuner status updates** in NetworkTables (if you're watching)

### Troubleshooting:

❌ **Button stays ON forever**
- Robot might not be running ShotResultLogger
- Check robot code is deployed

❌ **Button doesn't do anything**
- Check NetworkTables connection (green icon in dashboard)
- Verify robot is not in match mode

❌ **Can't find the buttons**
- Make sure `ShotResultLogger` subsystem is initialized in RobotContainer
- Check robot code is deployed and running

---

## 🎨 Customizing Your Dashboard

### AdvantageScope Tips:

- **Arrange buttons in a dedicated panel** for shot logging
- **Dock the panel** to a prominent location (center-bottom works well)
- **Increase text size** in preferences for visibility
- **Save your layout** so you don't have to set up again

### Shuffleboard Pro Tips:

1. **Create a dedicated tab** called "TUNING" or "SHOT LOG"
2. **Use large toggle buttons** (not checkboxes)
3. **Custom colors:**
   - LogHit: `#00FF00` (bright green)
   - LogMiss: `#FF0000` (bright red)
4. **Add text labels** above each button
5. **Save the layout** - File → Save → "tuning_layout.json"

---

## 📸 Example Screenshots

### AdvantageScope Layout:
```
NetworkTables Tree:
└─ FiringSolver/
   ├─ LogHit: false    [TOGGLE]  ← HIT button
   ├─ LogMiss: false   [TOGGLE]  ← MISS button
   ├─ Distance: 5.23
   ├─ Solution/
   │  ├─ pitchRadians: 0.785
   │  └─ exitVelocity: 12.5
   └─ TunerStatus: "Tuning kDragCoefficient..."
```

### Shuffleboard Layout:
```
┌─────────────────────────────────────────────────────┐
│ Tab: TUNING                                         │
├─────────────────────────────────────────────────────┤
│                                                     │
│  ┌────────────────────┐  ┌────────────────────┐   │
│  │  ✅ HIT TARGET     │  │  ❌ MISSED TARGET  │   │
│  │                    │  │                    │   │
│  │  [LogHit: false]   │  │  [LogMiss: false]  │   │
│  │                    │  │                    │   │
│  │   CLICK WHEN       │  │   CLICK WHEN       │   │
│  │   SHOT HITS        │  │   SHOT MISSES      │   │
│  │                    │  │                    │   │
│  └────────────────────┘  └────────────────────┘   │
│          GREEN                     RED              │
│                                                     │
│  Current Tuning: kDragCoefficient (iter 5/20)     │
│  Shots This Session: 47   Hit Rate: 68%           │
│                                                     │
└─────────────────────────────────────────────────────┘
```

---

## 💡 Best Practices

### For Drivers:

- ✅ **Click immediately** after observing result (while memory is fresh)
- ✅ **Be honest** - accuracy matters more than high hit rate
- ✅ **One click per shot** - don't click multiple times
- ✅ **Skip ambiguous shots** - only log when you're certain

### For Coaches:

- 📹 **Record video** during practice to verify logged results
- 📊 **Monitor hit rate** - should be realistic (50-80% typical)
- 📝 **Note environmental changes** (lighting, battery voltage, etc.)
- 🔄 **Reset layout** if buttons get moved accidentally

### For Programmers:

- 🔧 **Test buttons** before competition (toggle in dashboard, check logs)
- 💾 **Share dashboard layouts** with team (commit to repo)
- 📱 **Multiple computers** can log (coach laptop, pit display, etc.)
- 🔍 **Monitor tuner_logs/** for data quality

---

## 🎯 Quick Troubleshooting

| Problem | Solution |
|---------|----------|
| Can't find buttons | Check FiringSolver folder in NetworkTables |
| Buttons don't work | Verify robot code deployed with ShotResultLogger |
| Buttons stay ON | Robot not running or NT disconnected |
| Too small / hard to click | Resize widgets in dashboard settings |
| Forgot which is which | HIT=Green/Left, MISS=Red/Right |

---

## 📞 Need Help?

Ask a programmer to:
1. Show you where the buttons are in YOUR dashboard
2. Set up a clear layout with big, labeled buttons
3. Test that clicking logs properly (check CSV files)
4. Save the layout so you don't lose it

**Remember: These buttons help the robot learn! Every accurate log improves the shooting. 🎯**
