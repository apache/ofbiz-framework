

package com.fidelissd.zcp.xcommon.util.hierarchy;

import java.util.List;
import java.util.Map;

import com.fidelissd.zcp.xcommon.collections.FastList;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import com.fidelissd.zcp.xcommon.enums.PartyTypesEnum;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

public class HierarchyUtils {
    public static final String module = HierarchyUtils.class.getName();
    public static final String resource_error = "HierarchyUtils";

    public static GenericValue getSysUserLogin(Delegator delegator) {
        GenericValue userLogin = null;

        try {
            userLogin = delegator.findOne("UserLogin", UtilMisc.toMap("userLoginId", "system"), true);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error finding party in getPartyByPartyId", module);
        }
        return userLogin;
    }

    public static String getSysPartyId(Delegator delegator) {
        GenericValue userLogin = getSysUserLogin(delegator);
        return UtilValidate.isNotEmpty(userLogin) ? userLogin.getString("partyId") : "system";
    }

    public static boolean isGovtPartyGroup(GenericValue party)
    {
        return getPartyType(party).equals("GOVERNMENT_ORG") || getPartyType(party).equals("GOVERNMENT_LOC") || getPartyType(party).equals("GOVERNMENT_AGENCY") ;
    }

    public static GenericValue getUserLogin(Delegator delegator, String userLoginId) {
        GenericValue userLogin = null;

        try {
            userLogin = delegator.findOne("UserLogin", UtilMisc.toMap("userLoginId", userLoginId), true);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error finding party in getPartyByPartyId", module);
        }
        return userLogin;
    }

    public static GenericValue getUserLoginByPartyId(Delegator delegator, String partyId)
    {
        GenericValue userLogin = null;

        try {
            userLogin = EntityQuery.use(delegator).from("UserLogin").where("partyId", partyId, "enabled", "Y").orderBy("-lastUpdatedStamp").queryFirst();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error finding party in getPartyByPartyId", module);
        }
        return userLogin;
    }

    public static GenericValue getPartyByUserLogin(Delegator delegator, String userLoginId) {
        GenericValue userLogin = null;
        GenericValue party = null;
        try {
            userLogin = delegator.findOne("UserLogin", UtilMisc.toMap("userLoginId", userLoginId), true);
            if (UtilValidate.isNotEmpty(userLogin)) {
                String userLoginPartyId = userLogin.getString("partyId");
                if (UtilValidate.isNotEmpty(userLoginPartyId)) {
                    party = delegator.findOne("Party", UtilMisc.toMap("partyId", userLoginPartyId), true);
                } else {
                    Debug.logWarning("Bad Data / User login doesn't have a valid party id associated. ID: " + userLoginId, module);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error finding party in getPartyByPartyId", module);
        }
        return party;
    }

    public static String getPartyIdByUserLogin(Delegator delegator, String userLoginId) {
        GenericValue userLogin = null;
        String userLoginPartyId = null;
        try {
            userLogin = delegator.findOne("UserLogin", UtilMisc.toMap("userLoginId", userLoginId), true);
            if (UtilValidate.isNotEmpty(userLogin)) {
                userLoginPartyId = userLogin.getString("partyId");
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error finding party in getPartyIdByUserLogin", module);
        }
        return userLoginPartyId;
    }

    public static GenericValue getPartyByPartyId(Delegator delegator, String partyId) {
        GenericValue party = null;
        try {
            party = delegator.findOne("Party", UtilMisc.toMap("partyId", partyId), false);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error finding party in getPartyByPartyId", module);
        }
        return party;
    }


    public static GenericValue getPartyByPartyId(Delegator delegator, String partyId, boolean useCache) {
        GenericValue party = null;
        try {
            party = delegator.findOne("Party", UtilMisc.toMap("partyId", partyId), useCache);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error finding party in getPartyByPartyId", module);
        }
        return party;
    }

    public static String getPartyByEmail(Delegator delegator, String email) {
        GenericValue party = null;
        String contactMechId = "";
        String partyId = "";
        try {
            party = EntityQuery.use(delegator).from("ContactMech").where("infoString", email, "contactMechTypeId", "EMAIL_ADDRESS").queryFirst();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error finding party in getPartyByEmail", module);
        }
        if (UtilValidate.isNotEmpty(party)) {
            contactMechId = party.getString("contactMechId");
            GenericValue partyContactMech = null;
            try {
                partyContactMech = EntityQuery.use(delegator).from("PartyContactMech").where("contactMechId", contactMechId).queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error finding party in getPartyByEmail", module);
            }
            if (UtilValidate.isNotEmpty(partyContactMech)) {
                partyId = partyContactMech.getString("partyId");
            }
        }
        return partyId;
    }

    public static GenericValue getPostalAddress(Delegator delegator, String contactMechId) {
        GenericValue postalAddress = null;
        try {
            postalAddress = EntityQuery.use(delegator).from("PostalAddress").where("contactMechId", contactMechId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error finding party in postal address", module);
        }
        return postalAddress;
    }

    public static String getPartyType(GenericValue party) {
        GenericValue partyType = null;
        try {
            partyType = party.getRelatedOne("PartyType", true);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error finding PartyType in getPartyType", module);
        }
        return partyType.getString("partyTypeId");
    }

    public static GenericValue getPartyGroup(Delegator delegator, String partyId) {
        GenericValue partyGroup = null;
        try {
            partyGroup = EntityQuery.use(delegator).from("PartyGroup").where("partyId", partyId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error finding PartyGroup in getPartyGroup", module);
        }
        return partyGroup;
    }

    public static List<GenericValue> getPartyRoles(Delegator delegator, String partyId) {
        GenericValue party = getPartyByPartyId(delegator, partyId);
        List<GenericValue> partyRole = null;
        try {
            partyRole = party.getRelated("PartyRole", null, null, true);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error finding PartyRole in checkPartyRole", module);
        }

        return partyRole;
    }

    public static List<GenericValue> getPartyRoles(GenericValue party) {
        return getPartyRoles(party.getDelegator(), party.getString("partyId"));
    }

    public static boolean isPerson(GenericValue party) {
        return getPartyType(party).equals(PartyTypesEnum.PERSON.getPartyTypeId());
    }

    public static boolean isCompany(GenericValue party) {
        return isPartyGroup(party);
    }

    public static boolean isTeam(GenericValue party) {
        return getPartyType(party).equals(PartyTypesEnum.TEAM.getPartyTypeId());
    }

    public static boolean isPartyGroup(GenericValue party) {
        return getPartyType(party).equals(PartyTypesEnum.COMPANY.getPartyTypeId());
    }

    // Given a GenericValue<Party> and a roleTypeId, return a boolean stating whether partyId has that role, or not.
    public static boolean checkPartyRole(GenericValue party, String roleTypeId) {
        return checkPartyRole(party.getDelegator(), party.getString("partyId"), roleTypeId);
    }

    // Given a partyId and a roleTypeId, return a boolean stating whether partyId has that role, or not.
    public static boolean checkPartyRole(Delegator delegator, String partyId, String roleTypeId) {
        GenericValue party = getPartyByPartyId(delegator, partyId);
        if (UtilValidate.isEmpty(party))
            return false;

        List<GenericValue> partyRole = null;
        try {
            partyRole = party.getRelated("PartyRole", UtilMisc.toMap("roleTypeId", roleTypeId), null, true);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error finding PartyRole in checkPartyRole", module);
        }

        return partyRole.size() > 0;
    }

    // Given a GenericValue<Party> and a list of roleTypeIds, return a List of booleans stating whether partyId has that role, or not.
    public static List<Boolean> checkPartyRoles(GenericValue party, List<String> roleTypeIds) {
        return checkPartyRoles(party.getDelegator(), party.getString("partyId"), roleTypeIds);
    }

    // Given a partyId and a list of roleTypeIds, return a List of booleans stating whether partyId has that role, or not.
    public static List<Boolean> checkPartyRoles(Delegator delegator, String partyId, List<String> roleTypeIds) {
        List<Boolean> hasRoleList = FastList.newInstance();
        for (String roleTypeId : roleTypeIds) {
            hasRoleList.add(checkPartyRole(delegator, partyId, roleTypeId));
        }
        return hasRoleList;
    }

    // Given a partyId and a list of roleTypeIds, return a booleans stating whether partyId has ALL of the roles, or not.
    public static boolean checkPartyRolesAnd(Delegator delegator, String partyId, List<String> roleTypeIds) {
        List<Boolean> values = checkPartyRoles(delegator, partyId, roleTypeIds);

        if (values != null) {
            for (boolean value : values) {
                if (!value)
                    return false;
            }
            return true;
        } else
            return false;
    }

    // Given a GenericValue<Party> and a list of roleTypeIds, return a booleans stating whether partyId has ALL of the roles, or not.
    public static boolean checkPartyRolesAnd(GenericValue party, List<String> roleTypeIds) {
        return checkPartyRolesAnd(party.getDelegator(), party.getString("partyId"), roleTypeIds);
    }

    // Given a partyId and a list of roleTypeIds, return a booleans stating whether partyId has ANY of the roles, or not.
    public static boolean checkPartyRolesOr(Delegator delegator, String partyId, List<String> roleTypeIds) {
        List<Boolean> values = checkPartyRoles(delegator, partyId, roleTypeIds);

        if (values != null) {
            for (boolean value : values) {
                if (value)
                    return true;
            }
            return false;
        } else
            return false;
    }

    // Given a GenericValue<Party> and a list of roleTypeIds, return a booleans stating whether partyId has ANY of the roles, or not.
    public static boolean checkPartyRolesOr(GenericValue party, List<String> roleTypeIds) {
        return checkPartyRolesOr(party.getDelegator(), party.getString("partyId"), roleTypeIds);
    }

    public static boolean checkSpecialConditionPermission(Map<String, Object> context) throws GenericServiceException {
        final String module = "checkSpecialConditionPermission";
        DispatchContext dctx = (DispatchContext) context.get("dctx");
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> serviceResult = FastMap.newInstance();

        serviceResult = dispatcher.runSync("specialConditionPermissionCheck", UtilMisc.toMap("userLogin", context.get("userLogin")));
        if (ServiceUtil.isError(serviceResult))
            return false;

        return UtilGenerics.cast(serviceResult.get("hasPermission"));
    }

    /**
     * Given a roleTypeId, return all party roles
     *
     * @param delegator
     * @param roleTypeId
     * @return list of party roles
     */
    public static List<GenericValue> getPartyRolesByRole(Delegator delegator, String roleTypeId) throws GenericEntityException {
        return EntityQuery.use(delegator).from("PartyRole").where(UtilMisc.toMap("roleTypeId", roleTypeId)).queryList();
    }

    //check party relationship type
    public static boolean checkPartyRelationshipType(Delegator delegator, String partyIdTo, String partyIdFrom, String partyRelationshipTypeId) {
        GenericValue partyRelationshipRecord = null;
        try {
            partyRelationshipRecord = EntityQuery.use(delegator).from("PartyRelationship")
                    .where("partyIdTo", partyIdTo, "partyIdFrom", partyIdFrom, "partyRelationshipTypeId", partyRelationshipTypeId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error finding PartyRelationship in checkPartyRelationshipType", module);
        }
        if (UtilValidate.isNotEmpty(partyRelationshipRecord)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Given a tagName and a projectItemId, return a boolean stating whether tag is associated with issue, or not.
     * @param delegator
     * @param tagName
     * @param projectItemId
     * @return
     */
    public static boolean checkIssueTagAssociation(Delegator delegator, String tagName, String projectItemId) {
        tagName = tagName.trim();
        GenericValue projectItemTag = null;
        try {
            projectItemTag = EntityQuery.use(delegator).from("ProjectItemTag").where("tagName", tagName).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error finding projectItemTag in checkIssueTagAssociation", module);
        }

        if (UtilValidate.isNotEmpty(projectItemTag)) {
            try {
                GenericValue ProjectItemTagAssoc = EntityQuery.use(delegator).from("ProjectItemTagAssoc").where("projectItemId", projectItemId, "projectItemTagId", projectItemTag.get("projectItemTagId")).queryOne();
                if (UtilValidate.isNotEmpty(ProjectItemTagAssoc)) {
                    return true;
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error finding ProjectItemTagAssoc in checkIssueTagAssociation", module);
            }
        }
        return false;
    }

    public static String getPartyName(Delegator delegator, String partyId) {
        return getPartyNameInDetail(delegator, partyId, false, false, false);
    }


    public static String getPartyName(Delegator delegator, String partyId, boolean lastNameFirst, boolean usePersonalTitle, boolean useSuffix) {
        return getPartyNameInDetail(delegator, partyId, lastNameFirst, usePersonalTitle, useSuffix);
    }

    /**
     * Utility to get product store from the account id (organization id )
     */
    public static GenericValue getProductStoreForPartyGroup(Delegator delegator,
        String accountId) {
        try {
            GenericValue productStore = EntityQuery.use(delegator).from("ProductStore").where
                ("payToPartyId", accountId)
                .queryFirst();
            if (UtilValidate.isNotEmpty(productStore)) {
                return productStore;
            }

        } catch (GenericEntityException e) {
            Debug.logError(e, "Error finding ProductStore for account : " + accountId, module);
            return null;
        }
        return null;
    }

    /**
     * Utility to get product store id from the account id (organization id )
     */
    public static String getProductStoreIdForPartyGroup(Delegator delegator, String accountId) {
        GenericValue productStore = getProductStoreForPartyGroup(delegator, accountId);
        if (UtilValidate.isNotEmpty(productStore)) {
            return productStore.getString("productStoreId");
        }
        return null;
    }

    public static String getPartyNameInDetail(Delegator delegator, String partyId, boolean lastNameFirst, boolean usePersonalTitle, boolean useSuffix) {
        GenericValue partyObject = null;
        try {
            partyObject = EntityQuery.use(delegator).from("Party").where("partyId", partyId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error finding Party in getPartyName", module);
        }
        if (partyObject == null) {
            return partyId;
        } else {
            if(UtilValidate.isNotEmpty(partyObject.getString("displayName"))) {
                return partyObject.getString("displayName");
            } else {
                // add display name if not added
                String name = "";
                if(HierarchyUtils.isPerson(partyObject)) {
                    GenericValue personObj = null;
                    try{
                        personObj = EntityQuery.use(delegator).from("Person").where("partyId", partyId).queryOne();
                        if (UtilValidate.isNotEmpty(personObj)) {
                            name = formatPartyNameObject(personObj, lastNameFirst, usePersonalTitle, useSuffix);
                        }
                        partyObject.set("displayName", name);
                        delegator.store(partyObject);
                        return name;
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Error finding Party in getPartyName", module);
                    }
                } else if(HierarchyUtils.isPartyGroup(partyObject)) {
                    GenericValue partyGroupRec = null;
                    try{
                        partyGroupRec = EntityQuery.use(delegator).from("PartyGroup").where("partyId", partyId).queryOne();
                        if (UtilValidate.isNotEmpty(partyGroupRec)) {
                            name = formatPartyNameObject(partyGroupRec, lastNameFirst, usePersonalTitle, useSuffix);
                        }

                        partyObject.set("displayName", name);
                        delegator.store(partyObject);
                        return name;
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Error finding Party in getPartyName", module);
                    }
                } else if(HierarchyUtils.isGovtPartyGroup(partyObject)) {
                    GenericValue partyGroupRec = null;
                    try{
                        partyGroupRec = EntityQuery.use(delegator).from("PartyGroup").where("partyId", partyId).queryOne();
                        if (UtilValidate.isNotEmpty(partyGroupRec)) {
                            name = formatPartyNameObject(partyGroupRec, lastNameFirst, usePersonalTitle, useSuffix);
                        }

                        partyObject.set("displayName", name);
                        delegator.store(partyObject);
                        return name;
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Error finding Party in getPartyName", module);
                    }
                }

            }
            return partyId;
        }
    }

    public static String formatPartyNameObject(GenericValue partyValue, boolean lastNameFirst, boolean usePersonalTitle, boolean useSuffix) {
        if (partyValue == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        ModelEntity modelEntity = partyValue.getModelEntity();
        if (modelEntity.isField("firstName") && modelEntity.isField("middleName") && modelEntity.isField("lastName")) {
            if(usePersonalTitle)
                result.append(UtilFormatOut.ifNotEmpty(partyValue.getString("personalTitle"), "", " "));
            if (lastNameFirst) {
                if (UtilFormatOut.checkNull(partyValue.getString("lastName")) != null) {
                    result.append(UtilFormatOut.checkNull(partyValue.getString("lastName")));
                    if (partyValue.getString("firstName") != null) {
                        result.append(", ");
                    }
                }
                result.append(UtilFormatOut.checkNull(partyValue.getString("firstName")));
            } else {
                result.append(UtilFormatOut.ifNotEmpty(partyValue.getString("firstName"), "", " "));
                result.append(UtilFormatOut.ifNotEmpty(partyValue.getString("middleName"), "", " "));
                result.append(UtilFormatOut.checkNull(partyValue.getString("lastName")));
                if(modelEntity.isField("nickname")){
                    if(UtilValidate.isNotEmpty(partyValue.getString("nickname")))
                        result.append(" (" + partyValue.getString("nickname") + ")" );
                }
            }
            if(useSuffix)
                result.append(UtilFormatOut.ifNotEmpty(partyValue.getString("suffix"), " ", ""));

        }
        if (modelEntity.isField("groupName") && partyValue.get("groupName") != null) {
            result.append(partyValue.getString("groupName"));
        }
        return result.toString();
    }

}
