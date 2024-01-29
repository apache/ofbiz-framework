

package com.simbaquartz.xparty.hierarchy.orderentity;

import com.simbaquartz.xparty.hierarchy.interfaces.HierarchyRolesEnum;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.simbaquartz.xparty.hierarchy.orderentity.OrderEntityUtils.getOrderEntityRoleParties;

public class OrderEntityServices
{
    public static final String module = OrderEntityServices.class.getName();
    public static final String resource = "HierarchyUiLabels";
    public static final String resource_error = "HierarchyErrorUiLabels";

    @SuppressWarnings("unchecked")
    public static <T extends Enum<T> & HierarchyRolesEnum> Map<String, Object> getCustRequestParties(DispatchContext ctx, Map<String, ? extends Object> context)
    {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Security security = ctx.getSecurity();
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> successResult = ServiceUtil.returnSuccess();

        String custRequestId = (String) context.get("custRequestId");
        Class<T> personRoleEnum = (Class<T>) context.get("personRoleEnum");

        List<GenericValue> custRequestParties = null;
        try {
            custRequestParties = getOrderEntityRoleParties(delegator, OrderEntityType.CUST_REQUEST, personRoleEnum, custRequestId);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,
                    "OrderEntityCustRequestPartyError",UtilMisc.toMap("custRequestId",custRequestId),locale)  + e.toString());
        }

        successResult.put("custRequestParties", custRequestParties);
        return successResult;
    }

}
