# FRC Shooter Bayesian Tuner

A **Driver Station-only** Bayesian optimization tuner for the FiringSolutionSolver. This system automatically tunes shooting coefficients based on real shot feedback using scikit-optimize's Gaussian Process Bayesian Optimization.

## 🎯 Quick Start for Drivers

**All you need to do:**

1. Install dependencies:
   ```bash
   pip install -r driver_station_tuner/requirements.txt
   ```

2. Edit your team number in `run_tuner.py`:
   ```python
   TUNER_ENABLED = True
   TEAM_NUMBER = 1234  # Your team number
   ```

3. Run the tuner:
   ```bash
   python driver_station_tuner/run_tuner.py
   ```

That's it! The tuner will:
- ✅ Automatically connect to your robot via NetworkTables
- ✅ Run in the background without blocking other Driver Station tasks
- ✅ Automatically disable during matches (FMS connected)
- ✅ Tune coefficients one at a time in priority order
- ✅ Log all data to CSV files for analysis
- ✅ Provide status feedback via NetworkTables

## 📋 Table of Contents

- [Overview](#overview)
- [How It Works](#how-it-works)
- [Configuration Guide](#configuration-guide)
- [Tuning Coefficients](#tuning-coefficients)
- [Adjusting Tuning Order](#adjusting-tuning-order)
- [Adjusting Tuning Ranges](#adjusting-tuning-ranges)
- [Bayesian Optimization Settings](#bayesian-optimization-settings)
- [Data Logging](#data-logging)
- [Safety Features](#safety-features)
- [Troubleshooting](#troubleshooting)
- [Advanced Usage](#advanced-usage)

## 🔍 Overview

### What This Does

The Bayesian Tuner automatically optimizes your robot's shooting parameters by:

1. **Observing** shot results (hit/miss, distance, velocity, angle) from NetworkTables
2. **Learning** which coefficient values improve accuracy using Bayesian optimization (scikit-optimize)
3. **Suggesting** better coefficient values based on past results
4. **Updating** coefficients in NetworkTables so your robot uses improved values
5. **Repeating** until optimal values are found

### Key Features

- **Single Toggle Enable/Disable**: Just set `TUNER_ENABLED = True` or `False`
- **Automatic Safety**: Disables during matches and when NT disconnects
- **Background Operation**: Runs in its own thread, doesn't block Driver Station
- **Bayesian Optimization**: Uses scikit-optimize for intelligent parameter exploration
- **Sequential Tuning**: Tunes one coefficient at a time in priority order
- **Adaptive Step Sizes**: Starts with large steps, automatically shrinks as it converges
- **Full Logging**: Every shot logged to CSV with timestamps and all parameters
- **Driver Feedback**: Optional status display in NetworkTables

## 🔬 How It Works

### Bayesian Optimization with scikit-optimize

This tuner uses **scikit-optimize (skopt)**, a state-of-the-art Bayesian optimization library that:

1. **Builds a probabilistic model** (Gaussian Process) of how each coefficient affects shot accuracy
2. **Intelligently explores** the parameter space using Expected Improvement acquisition function
3. **Converges faster** than grid search or random search by learning from previous shots
4. **Adapts step sizes** automatically based on convergence

### Tuning Process

```
For each coefficient in order:
  ├─ Initialize Bayesian Optimizer (skopt)
  ├─ Start with N random explorations (default: 5)
  ├─ Then use Bayesian optimization to suggest values
  ├─ For each suggested value:
  │   ├─ Write to NetworkTables
  │   ├─ Wait for M shots (default: 3)
  │   ├─ Aggregate hit/miss results
  │   ├─ Report back to optimizer
  │   └─ Optimizer updates its model
  ├─ Continue until converged or max iterations
  └─ Move to next coefficient
```

### Convergence Detection

The optimizer determines convergence when:
- Maximum iterations reached (`N_CALLS_PER_COEFFICIENT`)
- Step size shrinks below minimum threshold
- Recent scores show low variance (system is stable)

## ⚙️ Configuration Guide

### Main Configuration File: `config.py`

All tuning parameters are centralized in `driver_station_tuner/config.py`. The `TunerConfig` class contains all settings with clear documentation.

### Quick Settings

Edit these in `config.py` or override in your run script:

```python
from tuner import TunerConfig

config = TunerConfig()

# Enable/disable tuner
config.TUNER_ENABLED = True

# Set your team's robot IP
config.NT_SERVER_IP = "10.12.34.2"  # Team 1234

# Logging location
config.LOG_DIRECTORY = "./tuner_logs"

# How many iterations per coefficient before moving to next
config.N_CALLS_PER_COEFFICIENT = 20

# How often to check for new data (Hz)
config.TUNER_UPDATE_RATE_HZ = 10.0
```

## 🎛️ Tuning Coefficients

### Available Coefficients

All coefficients are defined in `config.py` in the `COEFFICIENTS` dictionary. Each coefficient has:

| Property | Description | Example |
|----------|-------------|---------|
| `name` | Coefficient identifier | `"kDragCoefficient"` |
| `default_value` | Starting value | `0.003` |
| `min_value` | Minimum allowed value | `0.001` |
| `max_value` | Maximum allowed value | `0.006` |
| `initial_step_size` | Starting step size for exploration | `0.001` |
| `step_decay_rate` | How fast step size shrinks (0-1) | `0.9` |
| `is_integer` | Whether value must be an integer | `False` |
| `enabled` | Whether to tune this coefficient | `True` |
| `nt_key` | NetworkTables key path | `"/Tuning/FiringSolver/DragCoefficient"` |

### Coefficient Definitions

#### kDragCoefficient
- **What it does**: Models air resistance on the projectile
- **Default**: 0.003
- **Range**: 0.001 to 0.006
- **Impact**: Higher values = more drag = shorter shots
- **Priority**: 🔥 High (tuned first)

#### kAirDensity
- **What it does**: Air density for drag calculations
- **Default**: 1.225 kg/m³
- **Range**: 1.10 to 1.30
- **Impact**: Affects drag calculations (varies with humidity/pressure)
- **Priority**: Disabled by default (constant in code)
- **Note**: Currently hardcoded in FiringSolutionSolver.java

#### kVelocityIterationCount
- **What it does**: Iterations for velocity convergence
- **Default**: 20
- **Range**: 10 to 50 (integer)
- **Impact**: More iterations = more accurate but slower
- **Priority**: 🔥 High (tuned second)

#### kAngleIterationCount
- **What it does**: Iterations for angle convergence
- **Default**: 20
- **Range**: 10 to 50 (integer)
- **Impact**: More iterations = more accurate but slower
- **Priority**: 🔥 High (tuned third)

#### kVelocityTolerance
- **What it does**: Convergence threshold for velocity (m/s)
- **Default**: 0.01
- **Range**: 0.005 to 0.05
- **Impact**: Smaller = more precise but needs more iterations
- **Priority**: Medium (tuned fourth)

#### kAngleTolerance
- **What it does**: Convergence threshold for angle (radians)
- **Default**: 0.0001
- **Range**: 0.00001 to 0.001
- **Impact**: Smaller = more precise but needs more iterations
- **Priority**: Medium (tuned fifth)

#### kLaunchHeight
- **What it does**: Height of launcher above ground (meters)
- **Default**: 0.8
- **Range**: 0.75 to 0.85
- **Impact**: Offset for trajectory calculations
- **Priority**: Low (tuned last)

## 📊 Adjusting Tuning Order

### Changing the Order

Edit the `TUNING_ORDER` list in `config.py`:

```python
class TunerConfig:
    # Tune in this order (most important first)
    TUNING_ORDER: List[str] = [
        "kDragCoefficient",           # 1st - Most impact on accuracy
        "kAirDensity",                # 2nd - If enabled
        "kVelocityIterationCount",    # 3rd - Computation vs accuracy
        "kAngleIterationCount",       # 4th - Computation vs accuracy
        "kVelocityTolerance",         # 5th - Fine-tuning
        "kAngleTolerance",            # 6th - Fine-tuning
        "kLaunchHeight",              # 7th - Physical measurement
    ]
```

**Why order matters:**
- The tuner optimizes **one coefficient at a time** sequentially
- Coefficients tuned earlier are frozen while later ones are tuned
- Put high-impact coefficients first for faster overall improvement
- Put physical measurements (like launch height) last

### Enabling/Disabling Coefficients

To skip a coefficient, set `enabled = False`:

```python
config.COEFFICIENTS["kAirDensity"].enabled = False
```

Or in `run_tuner.py`:

```python
config = TunerConfig()
config.COEFFICIENTS["kAirDensity"].enabled = False
config.COEFFICIENTS["kLaunchHeight"].enabled = False
```

## 🎚️ Adjusting Tuning Ranges

### Changing Value Ranges

Edit the coefficient definition in `config.py`:

```python
"kDragCoefficient": CoefficientConfig(
    name="kDragCoefficient",
    default_value=0.003,
    min_value=0.001,      # ← Lower bound
    max_value=0.006,      # ← Upper bound
    initial_step_size=0.001,
    step_decay_rate=0.9,
    is_integer=False,
    enabled=True,
    nt_key="/Tuning/FiringSolver/DragCoefficient",
),
```

**Guidelines for setting ranges:**
- **Too narrow**: Might miss optimal value
- **Too wide**: Takes longer to converge
- **Rule of thumb**: ±50% of default value is usually good
- **For iteration counts**: Usually 10-50 is sufficient

### Adjusting Step Sizes

The `initial_step_size` controls how aggressively the optimizer explores:

```python
"kDragCoefficient": CoefficientConfig(
    # ...
    initial_step_size=0.001,  # ← Larger = faster but less precise
    step_decay_rate=0.9,      # ← How fast it shrinks (0.9 = 10% per iteration)
    # ...
),
```

**Step size decay:**
- Starts with `initial_step_size`
- Shrinks by `step_decay_rate` each iteration
- Stops when it reaches `MIN_STEP_SIZE_RATIO` (default: 0.1x initial)

**Example:**
```
Iteration 0: step = 0.001
Iteration 1: step = 0.001 * 0.9 = 0.0009
Iteration 2: step = 0.0009 * 0.9 = 0.00081
...
Iteration N: step = 0.0001 (minimum)
```

### Step Size Settings

In `config.py`:

```python
class TunerConfig:
    # Enable step size decay
    STEP_SIZE_DECAY_ENABLED: bool = True

    # Minimum step size as ratio of initial (0.1 = 10% of initial)
    MIN_STEP_SIZE_RATIO: float = 0.1
```

## 🧠 Bayesian Optimization Settings

### Core Parameters

```python
class TunerConfig:
    # Number of random points before Bayesian optimization starts
    # These help build the initial model
    N_INITIAL_POINTS: int = 5

    # Maximum optimization iterations per coefficient
    N_CALLS_PER_COEFFICIENT: int = 20

    # Acquisition function: "EI" (Expected Improvement) recommended
    # Other options: "LCB" (Lower Confidence Bound), "PI" (Probability of Improvement)
    ACQUISITION_FUNCTION: str = "EI"
```

### What These Mean

#### N_INITIAL_POINTS
- **Purpose**: Random exploration before optimization starts
- **Default**: 5
- **Impact**: More points = better initial model but takes longer
- **Recommendation**: 3-10 depending on coefficient complexity

#### N_CALLS_PER_COEFFICIENT
- **Purpose**: Maximum iterations before moving to next coefficient
- **Default**: 20
- **Impact**: More iterations = better optimization but longer tuning
- **Recommendation**: 15-30 for most use cases

#### ACQUISITION_FUNCTION
- **EI (Expected Improvement)**: Balances exploration vs exploitation (recommended)
- **LCB (Lower Confidence Bound)**: More conservative, less exploration
- **PI (Probability of Improvement)**: More aggressive, risk of local optima

### Shot Validation Settings

```python
class TunerConfig:
    # Minimum shots to accumulate before updating optimizer
    MIN_VALID_SHOTS_BEFORE_UPDATE: int = 3

    # Maximum consecutive invalid shots before stopping
    MAX_CONSECUTIVE_INVALID_SHOTS: int = 5
```

**Why wait for multiple shots?**
- Reduces noise from random misses
- More stable optimization signal
- Better statistical confidence

## 📁 Data Logging

### Log Files

Every tuning session creates a CSV log in `LOG_DIRECTORY`:

```
tuner_logs/
└── bayesian_tuner_20231115_143022.csv
```

### Log Contents

Each shot is logged with:

| Column | Description |
|--------|-------------|
| `timestamp` | ISO timestamp |
| `session_time_s` | Seconds since tuner started |
| `coefficient_name` | Which coefficient is being tuned |
| `coefficient_value` | Current value of that coefficient |
| `step_size` | Current step size |
| `iteration` | Iteration number for this coefficient |
| `shot_hit` | True/False |
| `shot_distance` | Distance to target (m) |
| `shot_angle_rad` | Launch angle (radians) |
| `shot_velocity_mps` | Exit velocity (m/s) |
| `nt_connected` | NetworkTables connection status |
| `match_mode` | Whether in match mode |
| `tuner_status` | Current tuner status message |
| `all_coefficients` | Snapshot of all coefficient values |

### Analyzing Logs

Use pandas to analyze:

```python
import pandas as pd

df = pd.read_csv('tuner_logs/bayesian_tuner_20231115_143022.csv')

# Hit rate by coefficient
hit_rates = df.groupby('coefficient_name')['shot_hit'].mean()
print(hit_rates)

# Best values
best = df[df['shot_hit'] == True].groupby('coefficient_name')['coefficient_value'].mean()
print(best)
```

## 🛡️ Safety Features

### Automatic Disabling

The tuner automatically stops when:

1. **Match Mode Detected**: FMS connected (prevents tuning during competition)
2. **NetworkTables Disconnects**: Lost connection to robot
3. **Too Many Invalid Shots**: Data quality too low
4. **Abnormal Readings**: Detects sensor issues

### Coefficient Clamping

All values are automatically clamped to valid ranges:

```python
# Invalid value
suggested_value = 10.0

# Automatically clamped
actual_value = config.clamp(suggested_value)  # -> 0.006 (max)
```

### Safe Shutdown

Press `Ctrl+C` for graceful shutdown:
- Stops tuning thread
- Disconnects from NetworkTables
- Closes log files properly
- No data loss

## 🔧 Troubleshooting

### Tuner Won't Start

**Check:**
1. Is `TUNER_ENABLED = True`?
2. Is NetworkTables connecting? Check robot IP
3. Are dependencies installed? Run `pip install -r requirements.txt`
4. Check logs for error messages

### Not Tuning During Practice

**Possible causes:**
1. Match mode detected (FMS connected) - disable FMS for tuning
2. NetworkTables disconnected - check robot connection
3. No new shot data - robot must be shooting
4. Coefficient already converged - check logs

### Tuning Too Slow

**Solutions:**
1. Decrease `N_CALLS_PER_COEFFICIENT` (e.g., 15 instead of 20)
2. Decrease `N_INITIAL_POINTS` (e.g., 3 instead of 5)
3. Increase `MIN_VALID_SHOTS_BEFORE_UPDATE` (less frequent updates)
4. Disable coefficients you don't need to tune

### Tuning Not Converging

**Solutions:**
1. Increase `N_CALLS_PER_COEFFICIENT` (more iterations)
2. Check if ranges are too wide - narrow them
3. Increase `initial_step_size` for faster exploration
4. Check data quality - are shots consistent?

### Values Not Updating in NT

**Check:**
1. NetworkTables key paths match between tuner and robot
2. Robot is in tuning mode (Constants.tuningMode = true)
3. Check NT logs for write errors
4. Verify robot code is reading from correct NT keys

## 🚀 Advanced Usage

### Custom Configuration

Create a custom config class:

```python
from tuner import TunerConfig

class MyTeamConfig(TunerConfig):
    # Override defaults
    TEAM_NUMBER = 1234
    NT_SERVER_IP = "10.12.34.2"
    LOG_DIRECTORY = "./logs/tuner"

    # Custom tuning order
    TUNING_ORDER = [
        "kDragCoefficient",
        "kLaunchHeight",
    ]

    # Faster convergence
    N_CALLS_PER_COEFFICIENT = 15
    MIN_STEP_SIZE_RATIO = 0.05

# Use it
from tuner import run_tuner
run_tuner(config=MyTeamConfig())
```

### Programmatic Control

```python
from tuner import BayesianTunerCoordinator, TunerConfig

config = TunerConfig()
tuner = BayesianTunerCoordinator(config)

# Start tuner
tuner.start(server_ip="10.12.34.2")

# Monitor status
while not tuner.optimizer.is_complete():
    status = tuner.get_status()
    print(f"Tuning: {status['tuning_status']}")
    time.sleep(5)

# Stop when done
tuner.stop()
```

### Adding New Coefficients

To add a new coefficient to tune:

1. **Add to robot code** (Java):
   ```java
   private static final LoggedTunableNumber kMyNewParameter =
       new LoggedTunableNumber("FiringSolver/MyNewParameter", 1.0);
   ```

2. **Add to config.py**:
   ```python
   "kMyNewParameter": CoefficientConfig(
       name="kMyNewParameter",
       default_value=1.0,
       min_value=0.5,
       max_value=2.0,
       initial_step_size=0.1,
       step_decay_rate=0.9,
       is_integer=False,
       enabled=True,
       nt_key="/Tuning/FiringSolver/MyNewParameter",
   ),
   ```

3. **Add to tuning order**:
   ```python
   TUNING_ORDER: List[str] = [
       "kDragCoefficient",
       "kMyNewParameter",  # Add here
       # ...
   ]
   ```

## 📚 Dependencies

- **scikit-optimize** (skopt): Bayesian optimization engine
- **pynetworktables**: FRC NetworkTables communication
- **numpy**: Numerical operations
- **pandas**: Data analysis (optional)

## 📝 License

This code follows the licensing of the SideKick repository. See repository root for details.

## 🤝 Contributing

To improve the tuner:

1. Test changes with real robot data
2. Document all configuration options
3. Add unit tests for new features
4. Update this README

## 📞 Support

For issues or questions:
1. Check this README first
2. Review log files in `tuner_logs/`
3. Check NetworkTables connection
4. Consult your team's software lead
