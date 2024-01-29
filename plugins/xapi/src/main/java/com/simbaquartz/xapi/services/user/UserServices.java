package com.simbaquartz.xapi.services.user;

import com.fidelissd.zcp.xcommon.services.contact.EmailTypesEnum;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

public class UserServices {
    private static final String module = UserServices.class.getName();

    /**
     * Validates if the input email id has a verified flag value of true or false.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> isEmailVerified(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        Boolean isVerified = false;
        Delegator delegator = dctx.getDelegator();
        List<EntityCondition> cond = new LinkedList<EntityCondition>();
        String emailIdToVerify = (String) context.get("emailIdToVerify");
        String partyId = (String) context.get("partyId");
        if(UtilValidate.isNotEmpty(partyId)) {
            try {
                GenericValue primaryEmail = EntityQuery.use(delegator).where("PartyContactMechPurpose")
                        .where("partyId", partyId, "contactMechPurposeTypeId", EmailTypesEnum.PRIMARY.getTypeId()).queryFirst();
                if(UtilValidate.isNotEmpty(primaryEmail)) {
                    String contactMechId = (String) primaryEmail.get("contactMechId");
                    cond.add(EntityCondition.makeCondition("contactMechId", contactMechId));

                }
            } catch (GenericEntityException e) {
                Debug.log(e, module);
            }
        }

        try {

            cond.add(EntityCondition.makeCondition("contactMechTypeId", "EMAIL_ADDRESS"));
            cond.add(EntityCondition.makeCondition("infoString", emailIdToVerify));

            GenericValue contactMechEmailVerificationResp = EntityQuery.use(delegator).from("ContactMech").where(cond).queryOne();
            if (UtilValidate.isNotEmpty(contactMechEmailVerificationResp) && "Y".equalsIgnoreCase(contactMechEmailVerificationResp.getString("isVerified"))) {
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