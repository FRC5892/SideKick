"""
Configuration module for the FRC Shooter Bayesian Tuner.

This module defines all tunable coefficients, their ranges, tuning order,
and system-wide settings for the Bayesian optimization tuner.
"""

from dataclasses import dataclass
from typing import Dict, List, Tuple


@dataclass
class CoefficientConfig:
    """Configuration for a single tunable coefficient."""
    
    name: str
    default_value: float
    min_value: float
    max_value: float
    initial_step_size: float
    step_decay_rate: float
    is_integer: bool
    enabled: bool
    nt_key: str  # NetworkTables key path
    
    def clamp(self, value: float) -> float:
        """Clamp value to valid range."""
        clamped = max(self.min_value, min(self.max_value, value))
        if self.is_integer:
            clamped = round(clamped)
        return clamped


class TunerConfig:
    """Global configuration for the Bayesian tuner system."""
    
    # Master enable/disable toggle
    TUNER_ENABLED: bool = True
    
    # NetworkTables configuration
    NT_SERVER_IP: str = "10.TE.AM.2"  # Replace TE.AM with your team number
    NT_TIMEOUT_SECONDS: float = 5.0
    NT_RECONNECT_DELAY_SECONDS: float = 2.0
    
    # NetworkTables keys for shot data
    NT_SHOT_DATA_TABLE: str = "/FiringSolver"
    NT_SHOT_HIT_KEY: str = "/FiringSolver/Hit"
    NT_SHOT_DISTANCE_KEY: str = "/FiringSolver/Distance"
    NT_SHOT_ANGLE_KEY: str = "/FiringSolver/Solution/pitchRadians"
    NT_SHOT_VELOCITY_KEY: str = "/FiringSolver/Solution/exitVelocity"
    NT_TUNER_STATUS_KEY: str = "/FiringSolver/TunerStatus"
    
    # Match mode detection key (DS_Attached && FMS_Attached)
    NT_MATCH_MODE_KEY: str = "/FMSInfo/FMSControlData"
    
    # Tuning parameters
    TUNING_ORDER: List[str] = [
        "kDragCoefficient",
        "kAirDensity",
        "kVelocityIterationCount",
        "kAngleIterationCount",
        "kVelocityTolerance",
        "kAngleTolerance",
        "kLaunchHeight",
    ]
    
    # Bayesian optimization settings
    N_INITIAL_POINTS: int = 5  # Random points before Bayesian optimization starts
    N_CALLS_PER_COEFFICIENT: int = 20  # Max optimization iterations per coefficient
    ACQUISITION_FUNCTION: str = "EI"  # Expected Improvement
    
    # Safety and validation
    MIN_VALID_SHOTS_BEFORE_UPDATE: int = 3
    MAX_CONSECUTIVE_INVALID_SHOTS: int = 5
    ABNORMAL_READING_THRESHOLD: float = 3.0  # Standard deviations
    
    # Logging configuration
    LOG_DIRECTORY: str = "./tuner_logs"
    LOG_FILENAME_PREFIX: str = "bayesian_tuner"
    LOG_TO_CONSOLE: bool = True
    
    # Threading configuration
    TUNER_UPDATE_RATE_HZ: float = 10.0  # How often to check for new data
    GRACEFUL_SHUTDOWN_TIMEOUT_SECONDS: float = 5.0
    
    # Step size decay configuration
    STEP_SIZE_DECAY_ENABLED: bool = True
    MIN_STEP_SIZE_RATIO: float = 0.1  # Minimum step size as ratio of initial
    
    # Coefficient definitions
    COEFFICIENTS: Dict[str, CoefficientConfig] = {
        "kDragCoefficient": CoefficientConfig(
            name="kDragCoefficient",
            default_value=0.003,
            min_value=0.001,
            max_value=0.006,
            initial_step_size=0.001,
            step_decay_rate=0.9,
            is_integer=False,
            enabled=True,
            nt_key="/Tuning/FiringSolver/DragCoefficient",
        ),
        "kAirDensity": CoefficientConfig(
            name="kAirDensity",
            default_value=1.225,
            min_value=1.10,
            max_value=1.30,
            initial_step_size=0.05,
            step_decay_rate=0.9,
            is_integer=False,
            enabled=False,  # Air density is constant in FiringSolutionSolver (1.225)
            nt_key="/Tuning/FiringSolver/AirDensity",
        ),
        "kVelocityIterationCount": CoefficientConfig(
            name="kVelocityIterationCount",
            default_value=20,
            min_value=10,
            max_value=50,
            initial_step_size=5,
            step_decay_rate=0.85,
            is_integer=True,
            enabled=True,
            nt_key="/Tuning/FiringSolver/VelocityIterations",
        ),
        "kAngleIterationCount": CoefficientConfig(
            name="kAngleIterationCount",
            default_value=20,
            min_value=10,
            max_value=50,
            initial_step_size=5,
            step_decay_rate=0.85,
            is_integer=True,
            enabled=True,
            nt_key="/Tuning/FiringSolver/AngleIterations",
        ),
        "kVelocityTolerance": CoefficientConfig(
            name="kVelocityTolerance",
            default_value=0.01,
            min_value=0.005,
            max_value=0.05,
            initial_step_size=0.005,
            step_decay_rate=0.9,
            is_integer=False,
            enabled=True,
            nt_key="/Tuning/FiringSolver/VelocityTolerance",
        ),
        "kAngleTolerance": CoefficientConfig(
            name="kAngleTolerance",
            default_value=0.0001,
            min_value=0.00001,
            max_value=0.001,
            initial_step_size=0.0001,
            step_decay_rate=0.9,
            is_integer=False,
            enabled=True,
            nt_key="/Tuning/FiringSolver/AngleTolerance",
        ),
        "kLaunchHeight": CoefficientConfig(
            name="kLaunchHeight",
            default_value=0.8,
            min_value=0.75,
            max_value=0.85,
            initial_step_size=0.01,
            step_decay_rate=0.9,
            is_integer=False,
            enabled=True,
            nt_key="/Tuning/FiringSolver/LaunchHeight",
        ),
    }
    
    @classmethod
    def get_enabled_coefficients_in_order(cls) -> List[CoefficientConfig]:
        """Get list of enabled coefficients in tuning order."""
        return [
            cls.COEFFICIENTS[name]
            for name in cls.TUNING_ORDER
            if name in cls.COEFFICIENTS and cls.COEFFICIENTS[name].enabled
        ]
    
    @classmethod
    def validate_config(cls) -> List[str]:
        """Validate configuration and return list of warnings/errors."""
        warnings = []
        
        # Check that all coefficients in tuning order exist
        for name in cls.TUNING_ORDER:
            if name not in cls.COEFFICIENTS:
                warnings.append(f"Coefficient '{name}' in TUNING_ORDER not found in COEFFICIENTS")
        
        # Check coefficient configurations
        for name, coeff in cls.COEFFICIENTS.items():
            if coeff.min_value >= coeff.max_value:
                warnings.append(f"{name}: min_value must be < max_value")
            
            if coeff.default_value < coeff.min_value or coeff.default_value > coeff.max_value:
                warnings.append(f"{name}: default_value outside valid range")
            
            if coeff.initial_step_size <= 0:
                warnings.append(f"{name}: initial_step_size must be positive")
            
            if not 0 < coeff.step_decay_rate <= 1.0:
                warnings.append(f"{name}: step_decay_rate must be in (0, 1]")
        
        # Check system parameters
        if cls.N_INITIAL_POINTS < 1:
            warnings.append("N_INITIAL_POINTS must be >= 1")
        
        if cls.N_CALLS_PER_COEFFICIENT < cls.N_INITIAL_POINTS:
            warnings.append("N_CALLS_PER_COEFFICIENT must be >= N_INITIAL_POINTS")
        
        if cls.TUNER_UPDATE_RATE_HZ <= 0:
            warnings.append("TUNER_UPDATE_RATE_HZ must be positive")
        
        return warnings
