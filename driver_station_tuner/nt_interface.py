"""
NetworkTables interface module for the Bayesian Tuner.

This module handles all NetworkTables communication including:
- Reading shot data and match mode status
- Writing updated coefficient values
- Connection management and error handling
- Status feedback to drivers
"""

import time
from typing import Optional, Dict, Any
from dataclasses import dataclass
import logging

try:
    from networktables import NetworkTables
except ImportError:
    # Provide a mock for testing without pynetworktables
    class NetworkTables:
        @staticmethod
        def initialize(server=None):
            pass
        
        @staticmethod
        def isConnected():
            return False
        
        @staticmethod
        def getTable(name):
            return None


logger = logging.getLogger(__name__)


@dataclass
class ShotData:
    """Container for shot data from NetworkTables."""
    
    hit: bool
    distance: float
    angle: float
    velocity: float
    timestamp: float
    
    def is_valid(self) -> bool:
        """Check if shot data is valid."""
        return (
            isinstance(self.hit, bool)
            and isinstance(self.distance, (int, float))
            and isinstance(self.angle, (int, float))
            and isinstance(self.velocity, (int, float))
            and self.distance > 0
            and self.velocity > 0
        )


class NetworkTablesInterface:
    """Interface for NetworkTables communication."""
    
    def __init__(self, config):
        """
        Initialize NetworkTables interface.
        
        Args:
            config: TunerConfig instance with NT settings
        """
        self.config = config
        self.connected = False
        self.last_connection_attempt = 0.0
        self.shot_data_listeners = []
        
        # Tables
        self.root_table = None
        self.tuning_table = None
        self.firing_solver_table = None
        
        # Last shot data
        self.last_shot_timestamp = 0.0
        self.last_shot_data: Optional[ShotData] = None
        
        logger.info("NetworkTables interface initialized")
    
    def connect(self, server_ip: Optional[str] = None) -> bool:
        """
        Connect to NetworkTables server.
        
        Args:
            server_ip: IP address of robot/server. If None, uses config default.
        
        Returns:
            True if connected successfully, False otherwise
        """
        current_time = time.time()
        
        # Throttle connection attempts
        if current_time - self.last_connection_attempt < self.config.NT_RECONNECT_DELAY_SECONDS:
            return self.connected
        
        self.last_connection_attempt = current_time
        
        try:
            if server_ip is None:
                server_ip = self.config.NT_SERVER_IP
            
            logger.info(f"Attempting to connect to NetworkTables at {server_ip}")
            NetworkTables.initialize(server=server_ip)
            
            # Wait for connection
            timeout = self.config.NT_TIMEOUT_SECONDS
            start_time = time.time()
            
            while not NetworkTables.isConnected():
                if time.time() - start_time > timeout:
                    logger.warning(f"Connection timeout after {timeout}s")
                    return False
                time.sleep(0.1)
            
            # Get tables
            self.root_table = NetworkTables.getTable("")
            self.tuning_table = NetworkTables.getTable("/Tuning")
            self.firing_solver_table = NetworkTables.getTable(self.config.NT_SHOT_DATA_TABLE)
            
            self.connected = True
            logger.info("Connected to NetworkTables successfully")
            return True
            
        except Exception as e:
            logger.error(f"Failed to connect to NetworkTables: {e}")
            self.connected = False
            return False
    
    def is_connected(self) -> bool:
        """Check if connected to NetworkTables."""
        try:
            self.connected = NetworkTables.isConnected()
        except Exception as e:
            logger.error(f"Error checking connection status: {e}")
            self.connected = False
        
        return self.connected
    
    def disconnect(self):
        """Disconnect from NetworkTables."""
        try:
            # NetworkTables doesn't have an explicit disconnect in pynetworktables
            self.connected = False
            logger.info("Disconnected from NetworkTables")
        except Exception as e:
            logger.error(f"Error during disconnect: {e}")
    
    def read_coefficient(self, nt_key: str, default_value: float) -> float:
        """
        Read a coefficient value from NetworkTables.
        
        Args:
            nt_key: NetworkTables key path
            default_value: Default value if key doesn't exist
        
        Returns:
            Current coefficient value
        """
        if not self.is_connected():
            logger.warning(f"Not connected, returning default for {nt_key}")
            return default_value
        
        try:
            value = self.tuning_table.getNumber(nt_key, default_value)
            return value
        except Exception as e:
            logger.error(f"Error reading {nt_key}: {e}")
            return default_value
    
    def write_coefficient(self, nt_key: str, value: float) -> bool:
        """
        Write a coefficient value to NetworkTables.
        
        Args:
            nt_key: NetworkTables key path
            value: Value to write
        
        Returns:
            True if write succeeded, False otherwise
        """
        if not self.is_connected():
            logger.warning(f"Not connected, cannot write {nt_key}")
            return False
        
        try:
            # Remove '/Tuning' prefix if present since we're using tuning_table
            key = nt_key.replace("/Tuning/", "")
            self.tuning_table.putNumber(key, value)
            logger.debug(f"Wrote {key} = {value}")
            return True
        except Exception as e:
            logger.error(f"Error writing {nt_key}: {e}")
            return False
    
    def read_shot_data(self) -> Optional[ShotData]:
        """
        Read the latest shot data from NetworkTables.
        
        Returns:
            ShotData object if new data available, None otherwise
        """
        if not self.is_connected():
            return None
        
        try:
            # Check if there's new shot data
            # We detect new shots by monitoring the Hit key's timestamp
            current_timestamp = time.time()
            
            # Read shot data
            hit = self.firing_solver_table.getBoolean("Hit", False)
            distance = self.firing_solver_table.getNumber("Distance", 0.0)
            
            # Read from solution subtable
            solution_table = self.firing_solver_table.getSubTable("Solution")
            angle = solution_table.getNumber("pitchRadians", 0.0)
            velocity = solution_table.getNumber("exitVelocity", 0.0)
            
            # Create shot data object
            shot_data = ShotData(
                hit=hit,
                distance=distance,
                angle=angle,
                velocity=velocity,
                timestamp=current_timestamp
            )
            
            # Only return if data is valid and seems to be new
            if shot_data.is_valid() and current_timestamp > self.last_shot_timestamp + 0.5:
                self.last_shot_timestamp = current_timestamp
                self.last_shot_data = shot_data
                logger.debug(f"New shot data: hit={hit}, distance={distance:.2f}, angle={angle:.3f}, velocity={velocity:.2f}")
                return shot_data
            
            return None
            
        except Exception as e:
            logger.error(f"Error reading shot data: {e}")
            return None
    
    def is_match_mode(self) -> bool:
        """
        Check if robot is in match mode (FMS attached).
        
        Returns:
            True if in match mode, False otherwise
        """
        if not self.is_connected():
            return False
        
        try:
            # Check FMSInfo for FMS control data
            fms_table = NetworkTables.getTable("/FMSInfo")
            
            # If FMSControlData exists and is not 0, we're in a match
            fms_control = fms_table.getNumber("FMSControlData", 0)
            return fms_control != 0
            
        except Exception as e:
            logger.error(f"Error checking match mode: {e}")
            return False
    
    def write_status(self, status: str):
        """
        Write tuner status message to NetworkTables for driver feedback.
        
        Args:
            status: Status message string
        """
        if not self.is_connected():
            return
        
        try:
            self.firing_solver_table.putString("TunerStatus", status)
            logger.debug(f"Status: {status}")
        except Exception as e:
            logger.error(f"Error writing status: {e}")
    
    def read_all_coefficients(self, coefficients: Dict[str, Any]) -> Dict[str, float]:
        """
        Read all coefficient values from NetworkTables.
        
        Args:
            coefficients: Dict of CoefficientConfig objects
        
        Returns:
            Dict mapping coefficient names to current values
        """
        values = {}
        for name, coeff in coefficients.items():
            values[name] = self.read_coefficient(coeff.nt_key, coeff.default_value)
        
        return values
    
    def write_all_coefficients(self, coefficient_values: Dict[str, float]) -> bool:
        """
        Write multiple coefficient values to NetworkTables.
        
        Args:
            coefficient_values: Dict mapping coefficient names to values
        
        Returns:
            True if all writes succeeded, False otherwise
        """
        success = True
        for name, value in coefficient_values.items():
            if name in self.config.COEFFICIENTS:
                coeff = self.config.COEFFICIENTS[name]
                if not self.write_coefficient(coeff.nt_key, value):
                    success = False
        
        return success
