

package com.simbaquartz.xaccounting.services.stripe.subscription;

import com.fidelissd.zcp.xcommon.util.AppConfigUtil;
import com.fidelissd.zcp.xcommon.util.AppConfigUtil.ApplicationPreferences;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentMethod;
import com.stripe.model.PaymentMethodCollection;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.PaymentMethodAttachParams;
import java.util.HashMap;
import java.util.Map;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;


public class StripeCustomerServices {
    private static final String module = StripeCustomerServices.class.getName();

    /**
     * Service to create a Customer in Stripe Portal.
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> createCustomerInStripe(DispatchContext dctx, Map<String, Object> context) throws GenericServiceException {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Debug.logInfo("Service createCustomerInStripe invoked...", module);

        String customerPartyId = (String) context.get("customerPartyId");
        String email = (String) context.get("email");
        String organizationName = (String) context.get("organizationName");
        String address = (String) context.get("address");
        String address2 = (String) context.get("address2");
        String city = (String) context.get("city");
        String state = (String) context.get("state");
        String zip = (String) context.get("zip");
        String country = (String) context.get("country");
        String phone = (String) context.get("phone");

        String stripeCustomerId = null;

        Delegator mainDelegator = DelegatorFactory.getDelegator("default");
        String stripeClientApiKey = AppConfigUtil
            .getAppPreference(mainDelegator, ApplicationPreferences.STRIPE_API_SECRET_KEY);
        if (UtilValidate.isEmpty(stripeClientApiKey)) {
            return ServiceUtil.returnError("Stripe Configuration Details not found.");
        }
        Stripe.apiKey = stripeClientApiKey;

        CustomerCreateParams.Address addressForStripe = CustomerCreateParams.Address.builder()
                .setLine1(address)
                .setLine2(address2)
                .setCity(city)
                .setState(state)
                .setCountry(country)
                .setPostalCode(zip)
                .build();

        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(email)
                .setName(organizationName)
                .setPhone(phone)
                .setAddress(addressForStripe)
                .putMetadata("partyId", customerPartyId)
                .build();
        try {
            Customer customer = Customer.create(params);
            stripeCustomerId = customer.getId();
            Debug.logInfo("Customer created in Stripe, the customer id is: " + stripeCustomerId, module);

            // Update the externalId for this party
            GenericValue party = delegator.findOne("Party", UtilMisc.toMap("partyId",customerPartyId), false);
            if(UtilValidate.isNotEmpty(party)) {
                party.setString("externalId", stripeCustomerId);
                delegator.store(party);
            }
        } catch (StripeException e) {
            e.printStackTrace();
            return ServiceUtil.returnError("Error creating a customer in stripe. Error: " + e.getMessage());
        } catch (GenericEntityException e) {
            e.printStackTrace();
        }

        serviceResult.put("stripeCustomerId", stripeCustomerId);
        return serviceResult;
    }


    public static Map<String, Object> retrieveCustomerFromStripe(DispatchContext dctx, Map<String, Object> context) throws GenericServiceException {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();

        String stripeCustomerId = (String) context.get("stripeCustomerId");
        Debug.logInfo("Retrieving stripe customer details for customer id: " + stripeCustomerId, module);

        Delegator mainDelegator = DelegatorFactory.getDelegator("default");
        String stripeClientApiKey = AppConfigUtil
            .getAppPreference(mainDelegator, ApplicationPreferences.STRIPE_API_SECRET_KEY);
        if (UtilValidate.isEmpty(stripeClientApiKey)) {
            return ServiceUtil.returnError("Stripe Configuration Details not found.");
        }
        Stripe.apiKey = stripeClientApiKey;

        Customer stripeCustomer ;
        try {
            stripeCustomer = Customer.retrieve(stripeCustomerId);

            // Return payment methods
            Map<String, Object> params = new HashMap<>();
            params.put("customer", stripeCustomerId);
            params.put("type", "card");

            PaymentMethodCollection paymentMethods = PaymentMethod.list(params);
            serviceResult.put("paymentMethods", paymentMethods);

        } catch (StripeException e) {
            e.printStackTrace();
            return ServiceUtil.returnError("Error retrieving customer info from Stripe. Error: " + e.getMessage());
        }
        if(stripeCustomer.getDeleted()!=null && stripeCustomer.getDeleted()) {
            serviceResult.put("stripeCustomer", null);
        } else {
            serviceResult.put("stripeCustomer", stripeCustomer);
        }
        return serviceResult;
    }

    public static Map<String, Object> updateCustomerPaymentMethod(DispatchContext dctx, Map<String, Object> context) throws GenericServiceException {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();

        String stripeCustomerId = (String) context.get("stripeCustomerId");
        String paymentMethodId = (String) context.get("paymentMethodId");
        Debug.logInfo("Updating payment method for customer " + stripeCustomerId, module);

        Delegator mainDelegator = DelegatorFactory.getDelegator("default");
        String stripeClientApiKey = AppConfigUtil
            .getAppPreference(mainDelegator, ApplicationPreferences.STRIPE_API_SECRET_KEY);
        if (UtilValidate.isEmpty(stripeClientApiKey)) {
            return ServiceUtil.returnError("Stripe Configuration Details not found.");
        }
        Stripe.apiKey = stripeClientApiKey;

        try {
            PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
            PaymentMethodAttachParams paymentMethodAttachParams =
                    PaymentMethodAttachParams.builder()
                            .setCustomer(stripeCustomerId)
                            .build();

            paymentMethod.attach(paymentMethodAttachParams);

            // Update Customer and Set customer default PM
            Customer customer = Customer.retrieve(stripeCustomerId);
            CustomerUpdateParams customerUpdateParams = CustomerUpdateParams.builder()
                    .setInvoiceSettings(
                            CustomerUpdateParams.InvoiceSettings.builder()
                                    .setDefaultPaymentMethod(paymentMethodId)
                                    .build())
                    .build();

            customer.update(customerUpdateParams);

        } catch (StripeException e) {
            e.printStackTrace();
            return ServiceUtil.returnError("Error updating payment method for customer. Error: " + e.getMessage());
        }
        return serviceResult;
    }
}
