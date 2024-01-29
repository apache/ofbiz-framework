package com.simbaquartz.xapi.helper;

import com.simbaquartz.xapi.connect.api.account.utils.AccountUtils;
import com.fidelissd.zcp.xcommon.models.client.billing.BillingPlans;
import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

public class SubscriptionHelper {
  private static final String module = SubscriptionHelper.class.getName();

  public static BillingPlans getActiveBillingPlanForAccount(
      String accountPartyId, Delegator delegator, LocalDispatcher dispatcher) {
    // Retrieve Active Subscriptions
    String stripeCustomerId;
    BillingPlans billingPlan = null;

    try {
      GenericValue orgParty =
          delegator.findOne("Party", UtilMisc.toMap("partyId", accountPartyId), false);
      stripeCustomerId = orgParty.getString("externalId");
    } catch (GenericEntityException e) {
      Debug.logError(e, e.getMessage(), module);
      return null;
    }

    /*List<Subscription> stripeActiveSubscriptions = null;
    if (UtilValidate.isNotEmpty(stripeCustomerId)) {

      try {
        Map<String, Object> stripeSubscriptionsResp =
            dispatcher.runSync(
                "retrieveSubscriptionsFromStripe",
                UtilMisc.toMap(
                    "stripeCustomerId",
                    stripeCustomerId,
                    "subscriptionStatus",
                    "active",
                    "userLogin",
                    HierarchyUtils.getSysUserLogin(delegator)));

        stripeActiveSubscriptions =
            (List<com.stripe.model.Subscription>)
                stripeSubscriptionsResp.get("stripeSubscriptions");
        if (!ServiceUtil.isSuccess(stripeSubscriptionsResp)) {
          Debug.logError(
              "Problem retrieving subscriptions from Stripe. Resp: " + stripeSubscriptionsResp,
              module);
          return null;
        }
      } catch (GenericServiceException e) {
        Debug.logError(
            e, "Problem retrieving subscriptions from Stripe. Error: " + e.getMessage(), module);
        return null;
      }
    }

    if (UtilValidate.isNotEmpty(stripeActiveSubscriptions)) {
      com.stripe.model.Subscription stripeSubscription = stripeActiveSubscriptions.get(0);
      String subscribedPlanId = stripeSubscription.getPlan().getId();

      billingPlan = BillingPlans.fromValue(subscribedPlanId);
    }*/

    return billingPlan;
  }
}
