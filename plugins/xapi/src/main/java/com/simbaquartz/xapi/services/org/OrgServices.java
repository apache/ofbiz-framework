package com.simbaquartz.xapi.services.org;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

import java.util.Map;

/**
 * Created by mande on 11/16/2019.
 */
public class OrgServices {
    private static final String module = OrgServices.class.getName();

    /**
     * Validates if the input email id has a verified flag value of true or false.
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> updateClientBaseConfigurations(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        Boolean isVerified = false;
        Delegator delegator = dctx.getDelegator();
        String emailIdToVerify = (String) context.get("emailIdToVerify");

        // quote id prefix
        // PartyAcctgPreference <PartyAcctgPreference partyId="FSD" taxFormId="US_IRS_1120" cogsMethodId="COGS_LIFO" baseCurrencyUomId="USD" invoiceSeqCustMethId="INV_HOOK_ENF_SEQ"
        // invoiceIdPrefix="FSDCI" quoteIdPrefix="CQ" orderIdPrefix="CO" errorGlJournalId="ERROR_JOURNAL" />


        try {
            GenericValue onboardingProgressGv = delegator.findOne("OnBoardingProgress", UtilMisc.toMap("setUpName", "isEmailVerificationComplete", "userLoginId", emailIdToVerify), false);
            //Return a valid error from the Rest service in case of already verified Email in the Resend Email Verification Rest Service.
            if (UtilValidate.isNotEmpty(onboardingProgressGv) && "Y".equalsIgnoreCase(onboardingProgressGv.getString("flag"))) {
                isVerified = true;
            }
        } catch (GenericEntityException ex) {
            String errorMessage = "An error occurred while trying to validate if the email id is verified or not.";
            Debug.logError(ex, errorMessage, module);
            return ServiceUtil.returnError(errorMessage);
        }

        serviceResult.put("isVerified", isVerified);

        return serviceResult;
    }
}
