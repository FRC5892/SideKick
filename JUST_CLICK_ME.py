#!/usr/bin/env python3
"""
FRC SHOOTER TUNER - CONFIGURED BY PROGRAMMERS, USED BY DRIVERS

=================================================================
PROGRAMMERS: Set these once, commit to repo
DRIVERS: Just double-click this file, nothing else needed!
=================================================================
"""

# ============================================================
# PROGRAMMER CONFIGURATION (Set once, drivers never touch)
# ============================================================

TUNER_ENABLED = True  # Programmers: Set to True/False
TEAM_NUMBER = 0       # Programmers: Set your team number (e.g., 1234)

# ============================================================
# DRIVERS: Don't change anything! Just double-click this file.
# ============================================================

import sys
import os

# Add to path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from driver_station_tuner import run_tuner, TunerConfig

def main():
    print("=" * 60)
    print("FRC SHOOTER TUNER")
    print("=" * 60)
    print()
    
    if not TUNER_ENABLED:
        print("❌ Tuner is DISABLED")
        print("   (Programmers: Edit TUNER_ENABLED = True to enable)")
        print()
        return
    
    # Calculate robot IP from team number
    if TEAM_NUMBER > 0:
        team_str = str(TEAM_NUMBER).zfill(4)
        server_ip = f"10.{team_str[:2]}.{team_str[2:]}.2"
        print(f"✅ Team {TEAM_NUMBER}")
        print(f"   Robot IP: {server_ip}")
    else:
        print("⚠️  Team number not configured")
        print("   (Programmers: Set TEAM_NUMBER in this file)")
        print()
        # Try USB connection
        server_ip = "10.0.0.2"
        print(f"   Trying USB connection: {server_ip}")
    
    print()
    print("🎯 Starting tuner...")
    print("   • Drivers: Press Ctrl+C to stop")
    print("   • Everything is automatic from here")
    print()
    print("=" * 60)
    print()
    
    # Configure and run
    config = TunerConfig()
    config.TUNER_ENABLED = TUNER_ENABLED
    config.NT_SERVER_IP = server_ip
    
    try:
        run_tuner(server_ip=server_ip, config=config)
        print("\n\n✅ Tuning complete!")
    except KeyboardInterrupt:
        print("\n\n✅ Tuner stopped by driver")
    except Exception as e:
        print(f"\n\n❌ Error: {e}")
        print("\n   Check:")
        print("   1. Robot is on and connected")
        print("   2. Dependencies installed (programmers check requirements.txt)")
        print("   3. Not in match mode")

if __name__ == "__main__":
    main()

