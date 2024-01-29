package com.simbaquartz.xparty.services;

import com.fidelissd.zcp.xcommon.models.email.EmailAddress;
import com.simbaquartz.xparty.helpers.PartyContentHelper;
import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
import com.simbaquartz.xparty.hierarchy.PartyGroupForPartyUtils;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import org.apache.ofbiz.base.util.*;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class PartyPersonalDetails {

    public static final String module = PartyContentHelper.class.getName();
    private static final int MYTHREADS = 30;

    public static Map<String, Object> createPartyWorkExperience(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();

        Delegator delegator = dctx.getDelegator();
        String partyId = (String) context.get("partyId");
        String companyName = (String) context.get("companyName");
        String jobProfile = (String) context.get("jobProfile");
        Timestamp startDate = (Timestamp) context.get("startDate");
        Timestamp endDate = (Timestamp) context.get("endDate");

        GenericValue userLogin = (GenericValue) context.get("userLogin");

        try {
            String workExperienceId = delegator.getNextSeqId("PartyWorkExperience");
            GenericValue pastExperience = delegator.makeValue("PartyWorkExperience");
            pastExperience.set("partyId", partyId);
            pastExperience.set("companyName", companyName);
            pastExperience.set("jobProfile", jobProfile);
            pastExperience.set("workExperienceId", workExperienceId);
            if (UtilValidate.isNotEmpty(endDate)) {
                pastExperience.set("endDate", endDate);
            }
            if (UtilValidate.isEmpty(startDate)) {
                pastExperience.set("startDate", UtilDateTime.nowTimestamp());
            } else {
                pastExperience.set("startDate", startDate);
            }
            delegator.create(pastExperience);
            serviceResult.put("workExperienceId", workExperienceId);
        } catch (GenericEntityException ex) {
            Debug.logError(ex, module);
            return ServiceUtil.returnError(ex.getMessage());
        }

        return serviceResult;
    }

    public static Map<String, Object> updatePartyWorkExperience(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();

        Delegator delegator = dctx.getDelegator();
        String partyId = (String) context.get("partyId");
        String workExperienceId = (String) context.get("workExperienceId");
        String companyName = (String) context.get("companyName");
        String jobProfile = (String) context.get("jobProfile");
        Timestamp startDate = (Timestamp) context.get("startDate");
        Timestamp endDate = (Timestamp) context.get("endDate");

        try {
            GenericValue updatePastExperience = EntityQuery.use(delegator).from("PartyWorkExperience").where("partyId", partyId, "workExperienceId", workExperienceId).queryOne();
            if (UtilValidate.isNotEmpty(updatePastExperience)) {
                if (UtilValidate.isNotEmpty(companyName)) {
                    updatePastExperience.set("companyName", companyName);
                }
                if (UtilValidate.isNotEmpty(jobProfile)) {
                    updatePastExperience.set("jobProfile", jobProfile);
                }
                if (UtilValidate.isNotEmpty(startDate)) {
                    updatePastExperience.set("startDate", startDate);
                }
                if (UtilValidate.isNotEmpty(endDate)) {
                    updatePastExperience.set("endDate", endDate);
                }
                updatePastExperience.store();
            }
        } catch (GenericEntityException ex) {
            Debug.logError(ex, module);
            return ServiceUtil.returnError(ex.getMessage());
        }
        return serviceResult;
    }

    public static Map<String, Object> deletePartyWorkExperience(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();

        Delegator delegator = dctx.getDelegator();
        String partyId = (String) context.get("partyId");
        String workExperienceId = (String) context.get("workExperienceId");

        try {
            GenericValue pastWorkExperience = EntityQuery.use(delegator).from("PartyWorkExperience").where("partyId", partyId, "workExperienceId", workExperienceId).queryOne();
            if (UtilValidate.isNotEmpty(pastWorkExperience)) {

                pastWorkExperience.remove();
            }
        } catch (GenericEntityException ex) {
            Debug.logError(ex, module);
            return ServiceUtil.returnError(ex.getMessage());
        }

        return serviceResult;
    }

    public static Map<String, Object> fsdAddPartyFamilyDetails(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String partyId = (String) context.get("partyId");
        String memberFirstName = (String) context.get("memberFirstName");
        String memberLastName = (String) context.get("memberLastName");
        Timestamp memberDOB = (Timestamp) context.get("memberDOB");
        String memberRelationship = (String) context.get("memberRelationship");
        String memberNote = (String) context.get("memberNote");
        try {
            String memberId = delegator.getNextSeqId("PartyFamilyMember");
            GenericValue addPartyFamilyMember = delegator.makeValue("PartyFamilyMember");
            addPartyFamilyMember.set("partyId", partyId);
            addPartyFamilyMember.set("memberFirstName", memberFirstName);
            addPartyFamilyMember.set("memberLastName", memberLastName);
            addPartyFamilyMember.set("memberDOB", memberDOB);
            addPartyFamilyMember.set("memberRelationship", memberRelationship);
            addPartyFamilyMember.set("memberId", memberId);
            if (UtilValidate.isNotEmpty(memberNote))
                addPartyFamilyMember.set("memberNotes", memberNote);

            delegator.create(addPartyFamilyMember);
            serviceResult.put("memberId", memberId);
        } catch (GenericEntityException e) {
            Debug.logError("An Exception occurred while calling the fsdAddPartyFamilyDetails service" + e.getMessage(), module);
            return ServiceUtil.returnError(e.getMessage());
        }
        return serviceResult;
    }

    public static Map<String, Object> fsdUpdatePartyFamilyDetails(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        String partyId = (String) context.get("partyId");
        String memberId = (String) context.get("memberId");
        String memberFirstName = (String) context.get("memberFirstName");
        String memberLastName = (String) context.get("memberLastName");
        Timestamp memberDOB = (Timestamp) context.get("memberDOB");
        String memberRelationship = (String) context.get("memberRelationship");
        String memberNote = (String) context.get("memberNote");

        try {
            GenericValue updateFamilyMemberDetails = EntityQuery.use(delegator).from("PartyFamilyMember").where("partyId", partyId, "memberId", memberId).queryOne();
            if (UtilValidate.isNotEmpty(updateFamilyMemberDetails)) {
                updateFamilyMemberDetails.set("memberFirstName", memberFirstName);
                updateFamilyMemberDetails.set("memberLastName", memberLastName);
                updateFamilyMemberDetails.set("memberDOB", memberDOB);

                if (UtilValidate.isNotEmpty(memberRelationship))
                    updateFamilyMemberDetails.set("memberRelationship", memberRelationship);

                updateFamilyMemberDetails.set("memberNotes", memberNote);

                updateFamilyMemberDetails.store();
            }
        } catch (GenericEntityException ex) {
            Debug.logError(ex, module);
            return ServiceUtil.returnError(ex.getMessage());
        }
        return serviceResult;
    }

    public static Map<String, Object> fsdDeletePartyFamilyDetails(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        String partyId = (String) context.get("partyId");
        String memberId = (String) context.get("memberId");

        try {
            GenericValue familyMemberDetails = EntityQuery.use(delegator).from("PartyFamilyMember").where("partyId", partyId, "memberId", memberId).queryOne();
            if (UtilValidate.isNotEmpty(familyMemberDetails))
                familyMemberDetails.remove();

        } catch (GenericEntityException ex) {
            Debug.logError(ex, module);
            return ServiceUtil.returnError(ex.getMessage());
        }

        return serviceResult;
    }

    public static Map<String, Object> createPartyEducationDetails(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();

        Delegator delegator = dctx.getDelegator();
        String partyId = (String) context.get("partyId");
        String degree = (String) context.get("degree");
        String notes = (String) context.get("notes");
        String university = (String) context.get("university");
        String startYear = (String) context.get("startYear");
        String endYear = (String) context.get("endYear");

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String userLoginId = (String) userLogin.get("userLoginId");


        try {
            String educationDetailId = delegator.getNextSeqId("PartyEducation");
            GenericValue educationDetails = delegator.makeValue("PartyEducation");
            educationDetails.set("partyId", partyId);
            educationDetails.set("degree", degree);
            educationDetails.set("notes", notes);
            educationDetails.set("university", university);
            educationDetails.set("educationDetailId", educationDetailId);
            if (UtilValidate.isNotEmpty(startYear)) {
                educationDetails.set("startYear", startYear);
            }
            if (UtilValidate.isNotEmpty(endYear)) {
                educationDetails.set("endYear", endYear);
            }
            delegator.create(educationDetails);
            serviceResult.put("educationDetailId", educationDetailId);
        } catch (GenericEntityException ex) {
            Debug.logError(ex, module);
            return ServiceUtil.returnError(ex.getMessage());
        }

        return serviceResult;
    }

    public static Map<String, Object> deletePartyHobbies(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();

        Delegator delegator = dctx.getDelegator();
        String partyId = (String) context.get("partyId");
        String hobbyId = (String) context.get("hobbyId");

        try {
            GenericValue deletePartyHobbies = EntityQuery.use(delegator).from("PartyHobby").where("partyId", partyId, "hobbyId", hobbyId).queryOne();
            if (UtilValidate.isNotEmpty(deletePartyHobbies)) {

                deletePartyHobbies.remove();
            }
        } catch (GenericEntityException ex) {
            Debug.logError(ex, module);
            return ServiceUtil.returnError(ex.getMessage());
        }

        return serviceResult;
    }

    public static Map<String, Object> updatePartyHobbies(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();

        Delegator delegator = dctx.getDelegator();
        String partyId = (String) context.get("partyId");
        String hobbyId = (String) context.get("hobbyId");
        String hobbyName = (String) context.get("hobbyName");
        String hobbyIconName = (String) context.get("hobbyIconName");


        try {
            GenericValue updateHobbiesDetails = EntityQuery.use(delegator).from("PartyHobby").where("partyId", partyId, "hobbyId", hobbyId).queryOne();
            if (UtilValidate.isNotEmpty(updateHobbiesDetails)) {
                if (UtilValidate.isNotEmpty(hobbyName)) {
                    updateHobbiesDetails.set("hobbyName", hobbyName);
                }
                if (UtilValidate.isNotEmpty(hobbyIconName)) {
                    updateHobbiesDetails.set("hobbyIconName", hobbyIconName);
                }
                updateHobbiesDetails.store();
            }
        } catch (GenericEntityException ex) {
            Debug.logError(ex, module);
            return ServiceUtil.returnError(ex.getMessage());
        }

        return serviceResult;
    }

    public static Map<String, Object> deleteEducationDetails(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();

        Delegator delegator = dctx.getDelegator();
        String partyId = (String) context.get("partyId");
        String educationDetailId = (String) context.get("educationDetailId");

        try {
            GenericValue deleteEducationDetails = EntityQuery.use(delegator).from("PartyEducation").where("partyId", partyId, "educationDetailId", educationDetailId).queryOne();
            if (UtilValidate.isNotEmpty(deleteEducationDetails)) {

                deleteEducationDetails.remove();
            }
        } catch (GenericEntityException ex) {
            Debug.logError(ex, module);
            return ServiceUtil.returnError(ex.getMessage());
        }

        return serviceResult;
    }

    public static Map<String, Object> createPartyHobbies(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();

        Delegator delegator = dctx.getDelegator();
        String partyId = (String) context.get("partyId");
        String hobbyName = (String) context.get("hobbyName");
        String hobbyIconName = (String) context.get("hobbyIconName");

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String userLoginId = (String) userLogin.get("userLoginId");

        try {
            String hobbyId = delegator.getNextSeqId("PartyHobby");
            GenericValue partyHobbies = delegator.makeValue("PartyHobby");
            partyHobbies.set("partyId", partyId);
            partyHobbies.set("hobbyName", hobbyName);
            partyHobbies.set("hobbyIconName", hobbyIconName);
            partyHobbies.set("hobbyId", hobbyId);
            delegator.create(partyHobbies);
            serviceResult.put("hobbyId", hobbyId);
        } catch (GenericEntityException ex) {
            Debug.logError(ex, module);
            return ServiceUtil.returnError(ex.getMessage());
        }

        return serviceResult;
    }

    public static Map<String, Object> updatePartyEducationDetails(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();

        Delegator delegator = dctx.getDelegator();
        String partyId = (String) context.get("partyId");
        String educationDetailId = (String) context.get("educationDetailId");
        String degree = (String) context.get("degree");
        String university = (String) context.get("university");
        String startYear = (String) context.get("startYear");
        String endYear = (String) context.get("endYear");

        try {
            GenericValue updateEducationDetails = EntityQuery.use(delegator).from("PartyEducation").where("partyId", partyId, "educationDetailId", educationDetailId).queryOne();
            if (UtilValidate.isNotEmpty(updateEducationDetails)) {
                if (UtilValidate.isNotEmpty(degree)) {
                    updateEducationDetails.set("degree", degree);
                }
                if (UtilValidate.isNotEmpty(university)) {
                    updateEducationDetails.set("university", university);
                }
                if (UtilValidate.isNotEmpty(startYear)) {
                    updateEducationDetails.set("startYear", startYear);
                }
                if (UtilValidate.isNotEmpty(endYear)) {
                    updateEducationDetails.set("endYear", endYear);
                }
                updateEducationDetails.store();
            }
        } catch (GenericEntityException ex) {
            Debug.logError(ex, module);
            return ServiceUtil.returnError(ex.getMessage());
        }
        return serviceResult;
    }

    /**
     *
     * @param dctx
     * @param context
     * @return
     * @throws Exception
     */
    public static Map<String, Object> syncPartyData(DispatchContext dctx, Map<String, Object> context) throws Exception {

        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String inputPartyId = (String) context.get("partyId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        // get all parties
        Map<String, Object> inputMap = FastMap.newInstance();
        inputMap.put("userLogin", context.get("userLogin"));

        if (UtilValidate.isNotEmpty(inputPartyId)) {
            inputMap.put("partyId", inputPartyId);
        } else {
            inputMap.put("showAll", "Y");
            inputMap.put("lookupFlag", "Y");
            inputMap.put("VIEW_INDEX", "0");
            inputMap.put("VIEW_SIZE", "10000");
        }

        Map<String, Object> searchPartiesResp = null;
        try {
            searchPartiesResp = dispatcher.runSync("findParty", inputMap);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        List<GenericValue> partyList = (List<GenericValue>) searchPartiesResp.get("partyList");

        Debug.logInfo("Found number of parties: " + partyList, module);

        ExecutorService executor = Executors.newFixedThreadPool(MYTHREADS);

        List<String> partyRecords = partyList.stream().map(e -> (String) e.get("partyId")).collect(Collectors.toList());
        String[] parties = partyRecords.stream()
                .toArray(String[]::new);
        for (int i = 0; i < parties.length; i++) {

            String partyId = parties[i];
            Runnable worker = new PartyThreadsHelper(partyId, delegator, userLogin, dispatcher);
            executor.execute(worker);
        }
        executor.shutdown();
        // Wait until all threads are finish
        while (!executor.isTerminated()) {

        }
        Debug.logInfo("Finished all threads for parties " + parties.length, module);

        return ServiceUtil.returnSuccess();
    }

    /**
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> fsdSetPartyDesignationService(DispatchContext dctx, Map<String, ? extends Object> context) throws GenericEntityException {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String partyId = (String) context.get("partyId");
        String designation = (String) context.get("designation");

        // add entry in party relationship entity as well
        String employer = "";
        GenericValue partyRec = HierarchyUtils.getPartyByPartyId(delegator, partyId);
        GenericValue parentPartyGroup = PartyGroupForPartyUtils.getPartyGroupForPartyId(partyRec);
        if (UtilValidate.isNotEmpty(parentPartyGroup)) {
            employer = parentPartyGroup.getString("partyId");
        }

        if (UtilValidate.isNotEmpty(employer)) {
            GenericValue partyRelationship = EntityQuery.use(delegator).from("PartyRelationship")
                    .where("partyIdTo", partyId, "partyIdFrom", employer).queryFirst();
            if (UtilValidate.isNotEmpty(partyRelationship)) {
                partyRelationship.set("positionTitle", designation);
                delegator.store(partyRelationship);
            }
        }

        GenericValue partyAttribute = EntityQuery.use(delegator).from("PartyAttribute")
                .where(UtilMisc.toMap("partyId", partyId, "attrName", "Designation")).queryOne();

        if (UtilValidate.isEmpty(partyAttribute)) {
            Map<String, Object> createPartyAttributeCtx = FastMap.newInstance();
            createPartyAttributeCtx.put("userLogin", userLogin);
            createPartyAttributeCtx.put("partyId", partyId);
            createPartyAttributeCtx.put("attrName", "Designation");
            createPartyAttributeCtx.put("attrValue", designation);

            Map<String, Object> createPartyAttributeResponse;
            try {
                createPartyAttributeResponse = dispatcher.runSync("createPartyAttribute", createPartyAttributeCtx);
                if (!ServiceUtil.isSuccess(createPartyAttributeResponse)) {
                    return createPartyAttributeResponse;
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }

            return ServiceUtil.returnSuccess();
        } else {
            Map<String, Object> updatePartyAttributeCtx = FastMap.newInstance();
            updatePartyAttributeCtx.put("userLogin", userLogin);
            updatePartyAttributeCtx.put("partyId", partyId);
            updatePartyAttributeCtx.put("attrName", "Designation");
            updatePartyAttributeCtx.put("attrValue", designation);

            Map<String, Object> updatePartyAttributeResponse;
            try {
                updatePartyAttributeResponse = dispatcher.runSync("updatePartyAttribute", updatePartyAttributeCtx);
                if (!ServiceUtil.isSuccess(updatePartyAttributeResponse)) {
                    return updatePartyAttributeResponse;
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }

            return ServiceUtil.returnSuccess();
        }
    }

    /**
     *
     * Get party communication events count.
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> getPartyCommEventsCount(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();

        String partyId = (String) context.get("partyId");
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        try {
            Map<String, Object> getPartyEmailLogsResp = dispatcher.runSync("getPartyEmailLogs",
                    UtilMisc.toMap("userLogin", userLogin,
                            "partyId", partyId));
            List<Map<String, Object>> emailLogs = (List<Map<String, Object>>) getPartyEmailLogsResp.get("emailLogs");


            Map<String, Object> getPartyCallLogsResp = dispatcher.runSync("getPartyCallLogs",
                    UtilMisc.toMap("userLogin", userLogin,
                            "partyId", partyId));
            List<Map<String, Object>> callLogs = (List<Map<String, Object>>) getPartyCallLogsResp.get("callLogs");
            int emailLogsCount = emailLogs.size();
            int callLogsCount = callLogs.size();

            Long totalCount = Long.valueOf(emailLogsCount) + Long.valueOf(callLogsCount);

            serviceResult.put("emails", Long.valueOf(emailLogsCount));
            serviceResult.put("calls", Long.valueOf(callLogsCount));
            serviceResult.put("totalCount", totalCount);


        } catch (GenericServiceException ex) {
            Debug.logError(ex, module);
            return ServiceUtil.returnError(ex.getMessage());
        }
        return serviceResult;
    }

    public static Map<String, Object> createOrUpdateEmailAddresses(DispatchContext dctx, Map<String, Object> context) {

        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String partyId = (String) context.get("partyId");
        LocalDispatcher dispatcher = dctx.getDispatcher();
        List<EmailAddress> emailsList = UtilGenerics.toList(context.get("emailList"));
        if (UtilValidate.isNotEmpty(emailsList)) {

            for (EmailAddress emailObject : emailsList) {

                if (UtilValidate.isNotEmpty(emailObject)) {
                    try {
                        GenericValue emailAddressInfo = EntityQuery.use(delegator).from("ContactMech").where("contactMechId", emailObject.getId()).queryOne();
                        if (UtilValidate.isNotEmpty(emailAddressInfo)) {

                            if (UtilValidate.isNotEmpty(emailObject.getEmailAddress())) {

                                Map<String, Object> updateEmailAddressResp = null;
                                try {
                                    updateEmailAddressResp = dispatcher.runSync("updatePartyEmailAddress",
                                            UtilMisc.toMap("partyId", partyId, "contactMechId", emailObject.getId(),
                                                    "emailAddress", emailObject.getEmailAddress(), "userLogin", userLogin));

                                } catch (GenericServiceException e) {
                                    Debug.logError(e, module);
                                    return ServiceUtil.returnError(e.getMessage());

                                }
                                if (ServiceUtil.isError(updateEmailAddressResp)) {
                                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(updateEmailAddressResp));
                                }
                            }

                            List<GenericValue> partyContactMechPurposeList = EntityQuery.use(delegator).from("PartyContactMechPurpose").where("partyId", partyId, "contactMechId", emailAddressInfo.get("contactMechId")).queryList();
                            if (UtilValidate.isNotEmpty(partyContactMechPurposeList)) {

                                if (UtilValidate.isNotEmpty(emailObject.getLabel())) {
                                    delegator.removeAll(partyContactMechPurposeList);

                                    String emailLabel = "";
                                    if(UtilValidate.isNotEmpty(emailObject.getLabel())) {
                                        emailLabel = emailObject.getLabel().toString();
                                    }

                                    Map<String, Object> createContactMechPurposeCtx = FastMap.newInstance();
                                    if(UtilValidate.isNotEmpty(emailObject.getOther()) && emailLabel.equals("OTHER_EMAIL")) {
                                        String otherLabel = emailObject.getOther().toString();
                                        GenericValue contactMech = EntityQuery.use(delegator).from("ContactMechPurposeType")
                                                .where("contactMechPurposeTypeId", otherLabel.toUpperCase(),
                                                        "parentTypeId", "APP_EMAIL_TYPES").queryOne();

                                        if(UtilValidate.isEmpty(contactMech)) {
                                            GenericValue createMechPurposeCtx = delegator.makeValue("ContactMechPurposeType");
                                            createMechPurposeCtx.set("contactMechPurposeTypeId", otherLabel.toUpperCase());
                                            createMechPurposeCtx.set("description", otherLabel);
                                            createMechPurposeCtx.set("parentTypeId", "APP_EMAIL_TYPES");
                                            delegator.create(createMechPurposeCtx);
                                            createContactMechPurposeCtx.put("contactMechPurposeTypeId", otherLabel.toUpperCase());
                                        } else {
                                            createContactMechPurposeCtx.put("contactMechPurposeTypeId", otherLabel.toUpperCase());
                                        }
                                    } else{
                                        createContactMechPurposeCtx.put("contactMechPurposeTypeId", emailObject.getLabel());
                                    }
                                    createContactMechPurposeCtx.put("partyId", partyId);
                                    createContactMechPurposeCtx.put("contactMechId", emailAddressInfo.get("contactMechId"));
                                    createContactMechPurposeCtx.put("userLogin", userLogin);

                                    Map<String, Object> createContactMechPurposeResp = null;
                                    try {
                                        createContactMechPurposeResp = dispatcher.runSync("createPartyContactMechPurpose", createContactMechPurposeCtx);

                                    } catch (GenericServiceException e) {
                                        Debug.logError(e, module);
                                        return ServiceUtil.returnError(e.getMessage());
                                    }

                                    if (ServiceUtil.isError(createContactMechPurposeResp)) {
                                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createContactMechPurposeResp));
                                    }
                                }

                            } else {

                                String emailLabel = "";
                                if(UtilValidate.isNotEmpty(emailObject.getLabel())) {
                                    emailLabel = emailObject.getLabel().toString();
                                }

                                Map<String, Object> createContactMechPurposeCtx = FastMap.newInstance();
                                if(UtilValidate.isNotEmpty(emailObject.getOther()) && emailLabel.equals("OTHER_EMAIL")) {
                                    String otherLabel = emailObject.getOther().toString();
                                    GenericValue contactMech = EntityQuery.use(delegator).from("ContactMechPurposeType")
                                            .where("contactMechPurposeTypeId", otherLabel.toUpperCase(),
                                                    "parentTypeId", "APP_EMAIL_TYPES").queryOne();

                                    if(UtilValidate.isEmpty(contactMech)) {
                                        GenericValue createMechPurposeCtx = delegator.makeValue("ContactMechPurposeType");
                                        createMechPurposeCtx.set("contactMechPurposeTypeId", otherLabel.toUpperCase());
                                        createMechPurposeCtx.set("description", otherLabel);
                                        createMechPurposeCtx.set("parentTypeId", "APP_EMAIL_TYPES");
                                        delegator.create(createMechPurposeCtx);
                                        createContactMechPurposeCtx.put("contactMechPurposeTypeId", otherLabel.toUpperCase());
                                    } else {
                                        createContactMechPurposeCtx.put("contactMechPurposeTypeId", otherLabel.toUpperCase());
                                    }
                                } else{
                                    createContactMechPurposeCtx.put("contactMechPurposeTypeId", emailObject.getLabel());
                                }
                                createContactMechPurposeCtx.put("partyId", partyId);
                                createContactMechPurposeCtx.put("contactMechId", emailAddressInfo.get("contactMechId"));
                                createContactMechPurposeCtx.put("userLogin", userLogin);

                                Map<String, Object> createContactMechPurposeResp = null;
                                try {
                                    createContactMechPurposeResp = dispatcher.runSync("createPartyContactMechPurpose", createContactMechPurposeCtx);

                                } catch (GenericServiceException e) {
                                    Debug.logError(e, module);
                                    return ServiceUtil.returnError(e.getMessage());
                                }

                                if (ServiceUtil.isError(createContactMechPurposeResp)) {
                                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createContactMechPurposeResp));
                                }
                            }
                            Boolean isDeleted = (Boolean) emailObject.getDeleted();
                            if (UtilValidate.isNotEmpty(isDeleted) && isDeleted) {

                                Map<String, Object> deletePartyContactMecResp = null;
                                try {

                                    deletePartyContactMecResp = dispatcher.runSync("deletePartyContactMech",
                                            UtilMisc.toMap("partyId", partyId, "contactMechId", emailAddressInfo.get("contactMechId"), "userLogin", userLogin));

                                    Map<String, Object> deletePartyEmailActivityResp = dispatcher.runSync("registerDeletePartyEmailActivity",
                                            UtilMisc.toMap("partyId", partyId, "emailAddress", emailAddressInfo.get("infoString"), "userLogin", userLogin));
                                    if (ServiceUtil.isError(deletePartyEmailActivityResp)) {
                                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(deletePartyEmailActivityResp));
                                    }
                                } catch (GenericServiceException e) {
                                    Debug.logError(e, module);
                                    return ServiceUtil.returnError(e.getMessage());
                                }

                                if (ServiceUtil.isError(deletePartyContactMecResp)) {
                                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(deletePartyContactMecResp));
                                }
                            }
                        } else {
                            String emailLabel = "";
                            if(UtilValidate.isNotEmpty(emailObject.getLabel())) {
                                emailLabel = emailObject.getLabel().toString();
                            }
                            Map<String, Object> createEmailAddressContext = FastMap.newInstance();
                            createEmailAddressContext.put("userLogin", userLogin);
                            createEmailAddressContext.put("partyId", partyId);
                            createEmailAddressContext.put("emailAddress", emailObject.getEmailAddress());
                            if(UtilValidate.isNotEmpty(emailObject.getOther()) && emailLabel.equals("OTHER_EMAIL")) {
                                String otherLabel = emailObject.getOther().toString();
                                GenericValue contactMech = EntityQuery.use(delegator).from("ContactMechPurposeType")
                                        .where("contactMechPurposeTypeId", otherLabel.toUpperCase(),
                                                "parentTypeId", "APP_EMAIL_TYPES").queryOne();

                                if(UtilValidate.isEmpty(contactMech)) {
                                    GenericValue createMechPurposeCtx = delegator.makeValue("ContactMechPurposeType");
                                    createMechPurposeCtx.set("contactMechPurposeTypeId", otherLabel.toUpperCase());
                                    createMechPurposeCtx.set("description", otherLabel);
                                    createMechPurposeCtx.set("parentTypeId", "APP_EMAIL_TYPES");
                                    delegator.create(createMechPurposeCtx);
                                    createEmailAddressContext.put("contactMechPurposeTypeId", otherLabel.toUpperCase());
                                } else {
                                    createEmailAddressContext.put("contactMechPurposeTypeId", otherLabel.toUpperCase());
                                }
                            } else{
                                createEmailAddressContext.put("contactMechPurposeTypeId", emailObject.getLabel());
                            }
                            // create new email address.
                            Map<String, Object> createEmailAddressResp = null;
                            try {
                                createEmailAddressResp = dispatcher.runSync("createPartyEmailAddress", createEmailAddressContext);

                            } catch (GenericServiceException e) {
                                Debug.logError(e, module);
                                return ServiceUtil.returnError(e.getMessage());
                            }

                            if (ServiceUtil.isError(createEmailAddressResp)) {
                                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createEmailAddressResp));
                            }
                        }
                    } catch (Exception e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(e.getMessage());
                    }
                }
            }
        }
        return ServiceUtil.returnSuccess();

    }
}
