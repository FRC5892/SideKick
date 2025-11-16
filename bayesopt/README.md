# Bayesian Optimization Tuner for FRC Shooter

This directory contains all files related to the Bayesian optimization tuner for automatically tuning shooter coefficients.

## Directory Structure

```
bayesopt/
├── README.md                    # This file
├── TODO.txt                     # Task tracking
├── config/                      # Configuration files
│   ├── COEFFICIENT_TUNING.py    # Coefficient definitions and tuning parameters
│   └── TUNER_TOGGLES.ini        # Main on/off switches
├── docs/                        # Documentation
│   ├── AUTO_START_SETUP.md
│   ├── CODE_REVIEW_SUMMARY.md
│   ├── DASHBOARD_BUTTON_GUIDE.md
│   ├── DEPLOYMENT_READY.md
│   ├── DRIVERS_START_HERE.md
│   ├── FINAL_REVIEW.md
│   ├── IMPLEMENTATION_COMPLETE.md
│   ├── SHOT_LOGGING_BUTTONS.md
│   └── TUNER_README.md
├── scripts/                     # Executable scripts
│   ├── RUN_TUNER.bat            # Windows launcher
│   ├── RUN_TUNER.sh             # Linux/Mac launcher
│   └── tuner_daemon.py          # Auto-start daemon
└── tuner/                       # Python package
    ├── __init__.py
    ├── config.py                # Configuration loading
    ├── logger.py                # Logging functionality
    ├── nt_interface.py          # NetworkTables interface
    ├── optimizer.py             # Bayesian optimization
    ├── tuner.py                 # Main coordinator
    ├── requirements.txt         # Python dependencies
    ├── run_tests.py             # Test runner
    ├── tests/                   # Unit tests
    │   ├── __init__.py
    │   ├── test_config.py
    │   ├── test_logger.py
    │   └── test_optimizer.py
    ├── MAINTAINER_GUIDE.md      # Technical documentation
    ├── QUICKSTART.md            # Quick start guide
    └── README.md                # Package documentation
```

## Quick Start

### Installation

1. Install Python dependencies:
   ```bash
   pip install -r bayesopt/tuner/requirements.txt
   ```

### Running the Tuner

#### Option 1: Auto-start Daemon (Recommended)
```bash
# Linux/Mac
cd bayesopt/scripts
./RUN_TUNER.sh

# Windows
cd bayesopt\scripts
RUN_TUNER.bat
```

#### Option 2: Manual Python Execution
```bash
cd bayesopt/scripts
python3 tuner_daemon.py
```

### Configuration

- **Main Toggles**: Edit `bayesopt/config/TUNER_TOGGLES.ini`
- **Tuning Parameters**: Edit `bayesopt/config/COEFFICIENT_TUNING.py`

## Documentation

- **For Drivers**: See `bayesopt/docs/DRIVERS_START_HERE.md`
- **For Setup**: See `bayesopt/docs/AUTO_START_SETUP.md`
- **For Developers**: See `bayesopt/tuner/MAINTAINER_GUIDE.md`
- **Technical Details**: See `bayesopt/tuner/README.md`

## Testing

Run unit tests:
```bash
cd bayesopt/tuner
python3 run_tests.py
```

Or run specific tests:
```bash
python3 -m unittest tuner.tests.test_config
python3 -m unittest tuner.tests.test_optimizer
python3 -m unittest tuner.tests.test_logger
```

## Usage from Code

```python
import sys
sys.path.insert(0, 'bayesopt')

from tuner import run_tuner, TunerConfig

# Run with default settings
run_tuner()

# Or with custom configuration
config = TunerConfig()
config.TUNER_ENABLED = True
run_tuner(server_ip="10.58.92.2")
```
