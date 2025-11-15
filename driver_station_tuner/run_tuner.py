#!/usr/bin/env python3
"""
Example usage script for the FRC Shooter Bayesian Tuner.

This script demonstrates how to run the tuner from the Driver Station.
Drivers only need to set TUNER_ENABLED = True and optionally configure
their team number.
"""

import sys
import logging
from driver_station_tuner import run_tuner, TunerConfig


def main():
    """Main entry point for the tuner."""
    
    # ========== DRIVER CONFIGURATION ==========
    # Set to True to enable tuner, False to disable
    TUNER_ENABLED = True
    
    # Set your team number (e.g., 1234 becomes "10.12.34.2")
    TEAM_NUMBER = 0  # Replace with your team number
    
    # Optional: Set custom server IP if not using team number
    # SERVER_IP = "10.12.34.2"  # Uncomment and set if needed
    SERVER_IP = None
    # ==========================================
    
    # Configure team-specific NT server IP
    if TEAM_NUMBER > 0 and SERVER_IP is None:
        # Convert team number to IP (e.g., 1234 -> 10.12.34.2)
        team_str = str(TEAM_NUMBER).zfill(4)
        SERVER_IP = f"10.{team_str[:2]}.{team_str[2:]}.2"
        print(f"Using team {TEAM_NUMBER} server IP: {SERVER_IP}")
    
    # Create custom config
    config = TunerConfig()
    config.TUNER_ENABLED = TUNER_ENABLED
    
    if SERVER_IP:
        config.NT_SERVER_IP = SERVER_IP
    
    # Optional: Customize other settings
    # config.LOG_DIRECTORY = "./my_tuner_logs"
    # config.TUNER_UPDATE_RATE_HZ = 5.0
    # config.N_CALLS_PER_COEFFICIENT = 30
    
    # Optional: Disable specific coefficients
    # config.COEFFICIENTS["kAirDensity"].enabled = False
    
    if not TUNER_ENABLED:
        print("Tuner is DISABLED. Set TUNER_ENABLED = True to enable.")
        sys.exit(0)
    
    # Run the tuner
    try:
        print("="*60)
        print("FRC Shooter Bayesian Tuner")
        print("="*60)
        print(f"Server IP: {config.NT_SERVER_IP}")
        print(f"Log Directory: {config.LOG_DIRECTORY}")
        print(f"Tuning {len(config.get_enabled_coefficients_in_order())} coefficients")
        print("="*60)
        print("\nStarting tuner... Press Ctrl+C to stop.\n")
        
        run_tuner(server_ip=SERVER_IP, config=config)
        
    except KeyboardInterrupt:
        print("\n\nTuner stopped by user.")
    except Exception as e:
        print(f"\n\nError: {e}")
        logging.exception("Fatal error in tuner")
        sys.exit(1)


if __name__ == "__main__":
    main()
