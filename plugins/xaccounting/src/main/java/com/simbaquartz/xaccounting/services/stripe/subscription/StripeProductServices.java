

package com.simbaquartz.xaccounting.services.stripe.subscription;

import com.fidelissd.zcp.xcommon.util.AppConfigUtil;
import com.fidelissd.zcp.xcommon.util.AppConfigUtil.ApplicationPreferences;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Plan;
import com.stripe.model.PlanCollection;
import com.stripe.model.Product;
import com.stripe.model.ProductCollection;
import com.stripe.param.PlanListParams;
import com.stripe.param.ProductListParams;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ServiceUtil;

import java.util.List;
import java.util.Map;


public class StripeProductServices {
    private static final String module = StripeProductServices.class.getName();

    /**
     * Service to get available products.
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> retrieveAvailableProducts(DispatchContext dctx, Map<String, Object> context) throws GenericServiceException {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();

        Debug.logInfo("Service retrieveAvailableProducts invoked...", module);

        Delegator mainDelegator = DelegatorFactory.getDelegator("default");
        String stripeClientApiKey = AppConfigUtil.getAppPreference(mainDelegator, ApplicationPreferences.STRIPE_API_SECRET_KEY);
        if (UtilValidate.isEmpty(stripeClientApiKey)) {
            return ServiceUtil.returnError("Stripe Configuration Details not found.");
        }
        Stripe.apiKey = stripeClientApiKey;

        /*Map<String, Object> params = new HashMap<>();
        params.put("limit", 10);*/

        List<Product> stripeProducts;
        try {
            ProductListParams params2 = ProductListParams.builder().setActive(true).build();
            ProductCollection productCollection = Product.list(params2);
            stripeProducts = productCollection.getData();
        } catch (StripeException e) {
            e.printStackTrace();
            return ServiceUtil.returnError("Error retrieving products from Stripe. Error: " + e.getMessage());
        }
        serviceResult.put("stripeProducts", stripeProducts);
        return serviceResult;
    }

    /**
     * Get list of available active plans from Stripe
     * @param dctx
     * @param context
     * @return
     * @throws GenericServiceException
     */
    public static Map<String, Object> retrieveAvailablePlans(DispatchContext dctx, Map<String, Object> context) throws GenericServiceException {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        Debug.logInfo("Service retrieveAvailablePlans invoked...", module);

        Delegator mainDelegator = DelegatorFactory.getDelegator("default");
        String stripeClientApiKey =
            AppConfigUtil.getAppPreference(mainDelegator, ApplicationPreferences.STRIPE_API_SECRET_KEY);
        if (UtilValidate.isEmpty(stripeClientApiKey)) {
            return ServiceUtil.returnError("Stripe Configuration Details not found.");
        }
        Stripe.apiKey = stripeClientApiKey;

        List<Plan> stripePlans;
        try {
            PlanListParams params = PlanListParams.builder().setActive(true).build();
            PlanCollection plans = Plan.list(params);

            stripePlans = plans.getData();
        } catch (StripeException e) {
            e.printStackTrace();
            return ServiceUtil.returnError("Error retrieving plans from Stripe. Error: " + e.getMessage());
        }
        serviceResult.put("stripePlans", stripePlans);
        return serviceResult;
    }


}
