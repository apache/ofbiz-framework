package com.simbaquartz.xparty.helpers;

import com.fidelissd.zcp.xcommon.enums.CommonContentTypesEnum;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.party.content.PartyContentWrapper;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import java.util.Map;

/**
 * PartyHelper
 */
public class PartyContentHelper
{
    public static final String module = PartyContentHelper.class.getName();

    /**
     * Returns customers logo image URI and associated content id. Will traverse up the hierarchy until a logo image is found for one of the parent parties.
     *
     * @param dctx    The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */

    public static Map<String, Object> getCustomerLogo(DispatchContext dctx, Map<String, ? extends Object> context)
    {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String customerPartyId = (String) context.get("customerPartyId");

        GenericValue partyLogoImageContent = PartyContentWrapper.getFirstPartyContentByType(customerPartyId, null,
            CommonContentTypesEnum.PHOTO.getTypeId(), delegator);

        boolean partyLogoFound = false;
        String partyLogoImageContentId = "";

        if (UtilValidate.isNotEmpty(partyLogoImageContent))
        {
            //logo image not found, traverse up the hierarchy until it is found
            partyLogoFound = true;
            partyLogoImageContentId = partyLogoImageContent.getString("contentId");
        }
        else
        {
            Map<String, Object> serviceResp = null;
            String partyIdToCheck = customerPartyId;
            do
            {
                if (serviceResp != null && ServiceUtil.isSuccess(serviceResp))
                {
                    GenericValue parentOrg = (GenericValue) serviceResp.get("parentOrg");
                    partyIdToCheck = parentOrg.getString("partyId");

                    partyLogoImageContent = PartyContentWrapper.getFirstPartyContentByType(partyIdToCheck, null,
                        CommonContentTypesEnum.PHOTO.getTypeId(), delegator);
                    if (UtilValidate.isNotEmpty(partyLogoImageContent))
                    {
                        partyLogoFound = true;
                        partyLogoImageContentId = partyLogoImageContent.getString("contentId");
                    }
                }

                try
                {
                    serviceResp = dispatcher.runSync("getCustomerParentOrgForPartyGroupId",
                            UtilMisc.toMap("userLogin", userLogin, "partyGroupPartyId", partyIdToCheck));
                }
                catch (GenericServiceException e)
                {
                    Debug.logError("An error occurred while trying to get parent customer information for party id : " + partyIdToCheck + ". Details: " + e.getMessage(), module);
                    return ServiceUtil.returnError("An error occurred while trying to get parent customer information");
                }
            }
            while (!partyLogoFound && !ServiceUtil.isFailure(serviceResp));
        }

        result.put("partyLogoImageContentId", partyLogoImageContentId);
        return result;
    }
}
