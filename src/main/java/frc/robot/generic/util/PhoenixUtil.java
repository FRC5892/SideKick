// Copyright (c) 2021-2026 Littleton Robotics
// http://github.com/Mechanical-Advantage
//
// Use of this source code is governed by a BSD
// license that can be found in the LICENSE file
// at the root directory of this project.

package frc.robot.generic.util;

import com.ctre.phoenix6.BaseStatusSignal;
import com.ctre.phoenix6.CANBus;
import com.ctre.phoenix6.StatusCode;
import frc.robot.generic.Robot;
import java.util.function.Supplier;

public class PhoenixUtil {

  public static final double kOptimizedSignalFrequency = 0;
  public static final double kRioSignalUpdateFrequency = 1 / Robot.defaultPeriodSecs;

  /** Attempts to run the command until no error is produced. */
  public static void tryUntilOk(int maxAttempts, Supplier<StatusCode> command) {
    for (int i = 0; i < maxAttempts; i++) {
      var error = command.get();
      if (error.isOK()) break;
    }
  }

  // Copyright (c) 2025 FRC 6328
  // http://github.com/Mechanical-Advantage
  //
  // Use of this source code is governed by an MIT-style
  // license that can be found in the LICENSE file at
  // the root directory of this project.
  /** Signals for synchronized refresh. */
  private static BaseStatusSignal[] canivoreSignals = new BaseStatusSignal[0];

  private static BaseStatusSignal[] rioSignals = new BaseStatusSignal[0];

  /** Registers a set of signals for synchronized refresh. */
  public static void registerSignals(boolean canivore, BaseStatusSignal... signals) {
    if (canivore) {
      BaseStatusSignal[] newSignals = new BaseStatusSignal[canivoreSignals.length + signals.length];
      System.arraycopy(canivoreSignals, 0, newSignals, 0, canivoreSignals.length);
      System.arraycopy(signals, 0, newSignals, canivoreSignals.length, signals.length);
      canivoreSignals = newSignals;
    } else {
      BaseStatusSignal[] newSignals = new BaseStatusSignal[rioSignals.length + signals.length];
      System.arraycopy(rioSignals, 0, newSignals, 0, rioSignals.length);
      System.arraycopy(signals, 0, newSignals, rioSignals.length, signals.length);
      rioSignals = newSignals;
    }
  }

  public static void registerSignals(CANBus canBus, BaseStatusSignal... signals) {
    registerSignals(canBus.isNetworkFD(), signals);
  }

  /** Refresh all registered signals. */
  public static void refreshAll() {
    if (canivoreSignals.length > 0) {
      BaseStatusSignal.refreshAll(canivoreSignals);
    }
    if (rioSignals.length > 0) {
      BaseStatusSignal.refreshAll(rioSignals);
    }
  }

  private static final double CONNECTED_LATENCY_S = 0.500; // Phoenix default
}
