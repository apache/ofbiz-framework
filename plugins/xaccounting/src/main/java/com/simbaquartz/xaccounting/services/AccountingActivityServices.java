package com.simbaquartz.xaccounting.services;

import com.fidelissd.zcp.xcommon.collections.FastMap;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import java.util.Map;

public class AccountingActivityServices {

    private static String module = "AccountingActivityServices";

    /**
     * Register a create new task activity.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> fsdRegisterCapturePaymentActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String userLoginId = (String) userLogin.get("userLoginId");
        String invoiceId = (String) context.get("invoiceId");
        String amount = (String) context.get("amount");

        try {
            Map<String, Object> registerCapturePaymentActivityCtx = FastMap.newInstance();
            registerCapturePaymentActivityCtx.put("userLogin", userLogin);
            registerCapturePaymentActivityCtx.put("invoiceId", invoiceId);
            registerCapturePaymentActivityCtx.put("amount", amount);
            registerCapturePaymentActivityCtx.put("creatorId", userLoginId);
            registerCapturePaymentActivityCtx.put("createdTime", UtilDateTime.nowDateString("yyyy-MM-dd'T'HH:mm:ssZ"));

            Map<String, Object> registerCapturePaymentActivityResponse = dispatcher.runSync("registerCapturePaymentActivity", registerCapturePaymentActivityCtx);
            if (!ServiceUtil.isSuccess(registerCapturePaymentActivityResponse)) {
                return registerCapturePaymentActivityResponse;
            }

            serviceResult.put("activityId", registerCapturePaymentActivityResponse.get("activityId"));
        } catch (Exception e) {
            e.printStackTrace();
            Debug.logError(e, module);
            return  ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

}
