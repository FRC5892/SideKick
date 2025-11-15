#!/usr/bin/env python3
"""
Simple tuner launcher that reads from config file.

This reads tuner_config.ini for all settings so drivers
never have to touch Python code.
"""

import sys
import os
import configparser

# Add to path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from driver_station_tuner import run_tuner, TunerConfig


def load_config():
    """Load configuration from tuner_config.ini file."""
    config_file = os.path.join(os.path.dirname(__file__), 'tuner_config.ini')
    
    if not os.path.exists(config_file):
        print("ERROR: tuner_config.ini not found!")
        print("Programmers: Create tuner_config.ini file")
        sys.exit(1)
    
    parser = configparser.ConfigParser()
    parser.read(config_file)
    
    # Read settings
    enabled = parser.getboolean('tuner', 'enabled', fallback=True)
    team_number = parser.getint('tuner', 'team_number', fallback=0)
    
    # Optional settings
    iterations = parser.getint('optimization', 'iterations_per_coefficient', fallback=20)
    update_rate = parser.getfloat('optimization', 'update_rate_hz', fallback=10.0)
    log_dir = parser.get('logging', 'log_directory', fallback='./tuner_logs')
    log_console = parser.getboolean('logging', 'log_to_console', fallback=True)
    
    return {
        'enabled': enabled,
        'team_number': team_number,
        'iterations': iterations,
        'update_rate': update_rate,
        'log_dir': log_dir,
        'log_console': log_console
    }


def main():
    """Main entry point."""
    print("=" * 60)
    print("FRC SHOOTER TUNER")
    print("=" * 60)
    print()
    
    # Load configuration
    try:
        settings = load_config()
    except Exception as e:
        print(f"ERROR loading config: {e}")
        print("Programmers: Check tuner_config.ini file")
        input("Press Enter to exit...")
        sys.exit(1)
    
    # Check if enabled
    if not settings['enabled']:
        print("❌ Tuner is DISABLED")
        print("   (Set enabled = True in tuner_config.ini)")
        print()
        input("Press Enter to exit...")
        return
    
    # Calculate robot IP
    team_number = settings['team_number']
    if team_number > 0:
        team_str = str(team_number).zfill(4)
        server_ip = f"10.{team_str[:2]}.{team_str[2:]}.2"
        print(f"✅ Team {team_number}")
        print(f"   Robot IP: {server_ip}")
    else:
        print("⚠️  Team number not set in config")
        print("   Trying USB connection...")
        server_ip = "10.0.0.2"
    
    print()
    print("🎯 Starting tuner...")
    print("   Press Ctrl+C to stop")
    print()
    print("=" * 60)
    print()
    
    # Create config
    config = TunerConfig()
    config.TUNER_ENABLED = settings['enabled']
    config.NT_SERVER_IP = server_ip
    config.N_CALLS_PER_COEFFICIENT = settings['iterations']
    config.TUNER_UPDATE_RATE_HZ = settings['update_rate']
    config.LOG_DIRECTORY = settings['log_dir']
    config.LOG_TO_CONSOLE = settings['log_console']
    
    # Run tuner
    try:
        run_tuner(server_ip=server_ip, config=config)
        print("\n\n✅ Tuning complete!")
    except KeyboardInterrupt:
        print("\n\n✅ Tuner stopped")
    except Exception as e:
        print(f"\n\n❌ Error: {e}")
        print("\nCommon fixes:")
        print("1. Robot is on and connected")
        print("2. Not in match mode")
        print("3. Dependencies installed (pip install -r driver_station_tuner/requirements.txt)")
        input("\nPress Enter to exit...")
        sys.exit(1)


if __name__ == "__main__":
    main()
