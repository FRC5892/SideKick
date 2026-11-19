package frc.robot.generic.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

public class PowerUtil {
  private static final List<DoubleSupplier> currentSupplier = new ArrayList<>();

  public static void addCurrentSupplier(DoubleSupplier supplier) {
    currentSupplier.add(supplier);
  }

  public static double getCurrent() {
    double totalCurrent = 0;
    for (DoubleSupplier doubleSupplier : currentSupplier) {
      totalCurrent += doubleSupplier.getAsDouble();
    }
    return totalCurrent;
  }
}