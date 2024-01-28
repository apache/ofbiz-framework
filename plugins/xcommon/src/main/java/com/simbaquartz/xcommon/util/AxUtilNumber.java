package com.simbaquartz.xcommon.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class AxUtilNumber {
  /**
   * Method to turn a number such as "0.9853" into a nicely formatted percent, "98.53".
   *
   * @param count    The number object representing the count example 3 in 3 of 10
   * @param total    The number object representing the total value, example 10 in 3 of 10
   * @return          The percentage value string or "" if there were errors.
   */
  public static BigDecimal toPercent(Number count, Number total) {
    // convert to BigDecimal
    if (!(count instanceof BigDecimal)) {
      count = new BigDecimal(count.doubleValue());
    }
    if (!(total instanceof BigDecimal)) {
      total = new BigDecimal(total.doubleValue());
    }

    // cast it so we can use BigDecimal methods
    BigDecimal bdCount = (BigDecimal) count;
    BigDecimal bdTotal = (BigDecimal) total;

    // multiply by 100 and set the scale
    bdCount = bdCount.divide(bdTotal,2, RoundingMode.HALF_UP).multiply(new BigDecimal(100.0));

    return bdCount;
  }

}
