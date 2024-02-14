

package com.simbaquartz.xaccounting.services.stripe.subscription;

import com.fidelissd.zcp.xcommon.util.AppConfigUtil;
import com.fidelissd.zcp.xcommon.util.AppConfigUtil.ApplicationPreferences;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionCollection;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.SubscriptionItemCollection;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.SubscriptionItemUpdateParams;
import com.stripe.param.SubscriptionUpdateParams;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ServiceUtil;


public class StripeSubscriptionServices {

  private static final String module = StripeSubscriptionServices.class.getName();

  /**
   * Service to create a Customer in Stripe Portal.
   */
  public static Map<String, Object> createSubscriptionInStripe(DispatchContext dctx,
      Map<String, Object> context) throws GenericServiceException {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Debug.logInfo("Service createSubscriptionInStripe invoked...", module);

    String stripeCustomerId = (String) context.get("stripeCustomerId");
    String stripePlanId = (String) context.get("stripePlanId");

    Delegator mainDelegator = DelegatorFactory.getDelegator("default");
    String stripeClientApiKey = AppConfigUtil
        .getAppPreference(mainDelegator, ApplicationPreferences.STRIPE_API_SECRET_KEY);
    if (UtilValidate.isEmpty(stripeClientApiKey)) {
      return ServiceUtil.returnError("Stripe Configuration Details not found.");
    }
    Stripe.apiKey = stripeClientApiKey;
    SubscriptionCreateParams params = SubscriptionCreateParams.builder()
        .setCustomer(stripeCustomerId)
        .addItem(SubscriptionCreateParams.Item.builder()
            .setPlan(stripePlanId)
            .build())
        //.addExpand("latest_invoice.payment_intent")
        .build();

    String stripeSubscriptionId = null;
    try {
      Subscription subscription = Subscription.create(params);
      stripeSubscriptionId = subscription.getId();
    } catch (StripeException e) {
      e.printStackTrace();
      return ServiceUtil
          .returnError("Error creating a subscription in stripe. Error: " + e.getMessage());
    }

    serviceResult.put("stripeSubscriptionId", stripeSubscriptionId);
    return serviceResult;
  }


  public static Map<String, Object> retrieveSubscriptionsFromStripe(DispatchContext dctx,
      Map<String, Object> context) throws GenericServiceException {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Debug.logInfo("Service retrieveSubscriptionsFromStripe invoked...", module);

    String stripeCustomerId = (String) context.get("stripeCustomerId");
    String subscriptionStatus = (String) context.get("subscriptionStatus");
    if (UtilValidate.isEmpty(subscriptionStatus)) {
      subscriptionStatus = "active"; // by default only get active subscriptions
    }

    Delegator mainDelegator = DelegatorFactory.getDelegator("default");
    String stripeClientApiKey =
        AppConfigUtil
            .getAppPreference(mainDelegator, ApplicationPreferences.STRIPE_API_SECRET_KEY);
    if (UtilValidate.isEmpty(stripeClientApiKey)) {
      return ServiceUtil.returnError("Stripe Configuration Details not found.");
    }
    Stripe.apiKey = stripeClientApiKey;

    Map<String, Object> params = new HashMap<>();
    params.put("limit", 3);
    params.put("status", subscriptionStatus);
    params.put("customer", stripeCustomerId);

    List<Subscription> subscriptions;
    try {
      SubscriptionCollection subscriptionCollection = Subscription.list(params);
      subscriptions = subscriptionCollection.getData();
    } catch (StripeException e) {
      e.printStackTrace();
      return ServiceUtil
          .returnError("Error retrieving subscriptions from Stripe. Error: " + e.getMessage());
    }
    //Debug.logInfo("Stripe subscriptions retrieved, subscriptions: " + subscriptions, module);
    serviceResult.put("stripeSubscriptions", subscriptions);
    return serviceResult;
  }


  /**
   * Update an existing subscription 1. change quantity - user licenses count 2. change plan -
   */
  public static Map<String, Object> updateStripeSubscription(DispatchContext dctx,
      Map<String, Object> context) throws GenericServiceException {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Debug.logInfo("Service updateStripeSubscription invoked...", module);

    String stripeSubscriptionId = (String) context.get("stripeSubscriptionId");
    Long userLicenseCount = (Long) context.get("userLicenseCount");
    String stripePlanId = (String) context.get("stripePlanId");

    Debug.logInfo("Updating subscription " + stripeSubscriptionId + " with user license count to: "
        + userLicenseCount, module);

    Delegator mainDelegator = DelegatorFactory.getDelegator("default");
    String stripeClientApiKey = AppConfigUtil
        .getAppPreference(mainDelegator, ApplicationPreferences.STRIPE_API_SECRET_KEY);
    if (UtilValidate.isEmpty(stripeClientApiKey)) {
      return ServiceUtil.returnError("Stripe Configuration Details not found.");
    }
    Stripe.apiKey = stripeClientApiKey;

    try {
      Subscription subscription = Subscription.retrieve(stripeSubscriptionId);
      SubscriptionItemCollection subscriptionItemCollection = subscription.getItems();
      List<SubscriptionItem> subscriptionItems = subscriptionItemCollection.getData();

      if (UtilValidate.isNotEmpty(subscriptionItems)) {
        SubscriptionItem item = subscriptionItems.get(0);
        SubscriptionItemUpdateParams.Builder paramsBuilder = SubscriptionItemUpdateParams.builder();

        if (UtilValidate.isNotEmpty(stripePlanId)) {
          paramsBuilder.setPlan(stripePlanId);
        }
        paramsBuilder.setQuantity(userLicenseCount);

        SubscriptionItemUpdateParams params = paramsBuilder.build();
        item.update(params);
      }

    } catch (StripeException e) {
      e.printStackTrace();
      return ServiceUtil.returnError(
          "Error trying to update Subscription with id: " + stripeSubscriptionId + ". Error: " + e
              .getMessage());
    }

    return serviceResult;
  }

  /**
   * Cancels the subscription - sets to get cancelled at end of period this can be resumed before
   * end of period is reached
   */
  public static Map<String, Object> cancelStripeSubscription(DispatchContext dctx,
      Map<String, Object> context) throws GenericServiceException {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Debug.logInfo("Service cancelStripeSubscription invoked...", module);

    String stripeSubscriptionId = (String) context.get("stripeSubscriptionId");
    Debug.logInfo("Cancelling subscription " + stripeSubscriptionId, module);

    Delegator mainDelegator = DelegatorFactory.getDelegator("default");
    String stripeClientApiKey = AppConfigUtil
        .getAppPreference(mainDelegator, ApplicationPreferences.STRIPE_API_SECRET_KEY);
    if (UtilValidate.isEmpty(stripeClientApiKey)) {
      return ServiceUtil.returnError("Stripe Configuration Details not found.");
    }
    Stripe.apiKey = stripeClientApiKey;

    try {
      Subscription subscription = Subscription.retrieve(stripeSubscriptionId);

      SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
          .setCancelAtPeriodEnd(true)
          .build();
      subscription.update(params);

            /*if(UtilValidate.isNotEmpty(subscription)) {
                Subscription deletedSubscription = subscription.cancel();
            }*/

    } catch (StripeException e) {
      e.printStackTrace();
      return ServiceUtil.returnError(
          "Error trying to cancel Subscription with id: " + stripeSubscriptionId + ". Error: " + e
              .getMessage());
    }

    return serviceResult;
  }

  /**
   * resumes an subscription set to cancel at end of period. cannot be used after reaching end of
   * period.
   */
  public static Map<String, Object> resumeStripeSubscription(DispatchContext dctx,
      Map<String, Object> context) throws GenericServiceException {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Debug.logInfo("Service resumeStripeSubscription invoked...", module);

    String stripeSubscriptionId = (String) context.get("stripeSubscriptionId");
    Debug.logInfo("Resuming subscription " + stripeSubscriptionId, module);

    Delegator mainDelegator = DelegatorFactory.getDelegator("default");
    String stripeClientApiKey = AppConfigUtil
        .getAppPreference(mainDelegator, ApplicationPreferences.STRIPE_API_SECRET_KEY);
    if (UtilValidate.isEmpty(stripeClientApiKey)) {
      return ServiceUtil.returnError("Stripe Configuration Details not found.");
    }
    Stripe.apiKey = stripeClientApiKey;

    try {
      Subscription subscription = Subscription.retrieve(stripeSubscriptionId);

      SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
          .setCancelAtPeriodEnd(false)
          .build();
      subscription.update(params);

    } catch (StripeException e) {
      e.printStackTrace();
      return ServiceUtil.returnError(
          "Error trying to resume Subscription with id: " + stripeSubscriptionId + ". Error: " + e
              .getMessage());
    }

    return serviceResult;
  }


}
