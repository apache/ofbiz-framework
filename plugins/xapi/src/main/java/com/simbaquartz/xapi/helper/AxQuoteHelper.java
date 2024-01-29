package com.simbaquartz.xapi.helper;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;

public class AxQuoteHelper {
  private static final String module = AxQuoteHelper.class.getName();

  public static Boolean isExistingQuote(Delegator delegator, String quoteId) {
    if (UtilValidate.isEmpty(getQuote(delegator, quoteId))) {
      return false;
    }
    return true;
  }

  public static GenericValue getQuote(Delegator delegator, String quoteId) {
    GenericValue quote = null;
    try {
      quote = EntityQuery.use(delegator).from("Quote").where("quoteId", quoteId).cache().queryOne();
    } catch (GenericEntityException e) {
      Debug.logError(e, module);
    }

    return quote;
  }
}
