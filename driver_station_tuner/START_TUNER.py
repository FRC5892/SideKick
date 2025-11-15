#!/usr/bin/env python3
"""
SUPER SIMPLE TUNER LAUNCHER - Just double-click or run this!

For drivers: This is the ONLY file you need to touch.
"""

# ============================================================
# DRIVER SETTINGS - Change these two lines and you're done!
# ============================================================

ENABLE_TUNER = True  # True to run, False to disable

YOUR_TEAM_NUMBER = 0  # Example: 1234, 5678, etc.

# ============================================================
# That's it! Don't change anything below this line.
# ============================================================

import sys
import os

# Add parent directory to path so we can import driver_station_tuner
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from driver_station_tuner import run_tuner, TunerConfig

def main():
    print("=" * 60)
    print("FRC SHOOTER TUNER - EASY MODE")
    print("=" * 60)
    
    if not ENABLE_TUNER:
        print("\n❌ Tuner is DISABLED")
        print("   To enable: Set ENABLE_TUNER = True")
        print("\n")
        return
    
    if YOUR_TEAM_NUMBER == 0:
        print("\n⚠️  WARNING: Team number not set!")
        print("   Edit this file and set YOUR_TEAM_NUMBER")
        print("\n   Example: YOUR_TEAM_NUMBER = 1234")
        print("\n")
        
        # Try to connect anyway with default IP
        server_ip = input("Enter robot IP address (or press Enter for 10.0.0.2): ").strip()
        if not server_ip:
            server_ip = "10.0.0.2"
    else:
        # Calculate robot IP from team number
        team_str = str(YOUR_TEAM_NUMBER).zfill(4)
        server_ip = f"10.{team_str[:2]}.{team_str[2:]}.2"
        print(f"\n✅ Team {YOUR_TEAM_NUMBER}")
        print(f"   Robot IP: {server_ip}")
    
    print("\n🎯 Starting tuner...")
    print("   Press Ctrl+C to stop\n")
    print("=" * 60)
    print()
    
    # Configure and run
    config = TunerConfig()
    config.TUNER_ENABLED = ENABLE_TUNER
    config.NT_SERVER_IP = server_ip
    
    try:
        run_tuner(server_ip=server_ip, config=config)
    except KeyboardInterrupt:
        print("\n\n✅ Tuner stopped by user")
    except Exception as e:
        print(f"\n\n❌ Error: {e}")
        print("\n   Check:")
        print("   1. Robot is on and connected")
        print("   2. Dependencies installed: pip install -r requirements.txt")
        print("   3. Team number is correct")

if __name__ == "__main__":
    main()
