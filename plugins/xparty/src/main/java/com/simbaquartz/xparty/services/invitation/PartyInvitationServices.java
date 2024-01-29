package com.simbaquartz.xparty.services.invitation;

import com.fidelissd.zcp.xcommon.enums.AccountUserRoleTypesEnum;
import com.fidelissd.zcp.xcommon.services.CommonHelper;
import com.fidelissd.zcp.xcommon.services.contact.EmailTypesEnum;
import com.fidelissd.zcp.xcommon.util.AppConfigUtil;
import com.fidelissd.zcp.xcommon.util.JWTUtils;
import com.simbaquartz.xparty.helpers.AxPartyHelper;
import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
import com.simbaquartz.xparty.hierarchy.PartyGroupForPartyUtils;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.fidelissd.zcp.xcommon.collections.FastList;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import com.fidelissd.zcp.xcommon.collections.FastSet;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.EntityFindOptions;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/** For inviting project team members to join an organization. */
public class PartyInvitationServices {
  public static final String module = PartyInvitationServices.class.getName();
  public static final String resource = "PartyUiLabels";
  public static final String EMAIL_INVITE_TEAM_MEMBER_TYPE_ID = "EML_INVT_PRTY";
  public static final String EMAIL_REINVITE_TEAM_MEMBER_TYPE_ID = "EML_REINVT_PRTY";
  public static final String EMAIL_NTF_INVTR_ON_INV_ACC_TYPE_ID = "EML_NTF_INVTR";
  public static final String EMAIL_NTF_INVTR_ON_INV_EXP_TYPE_ID = "EML_INVTR_INVEXP";
  public static final String EMAIL_NTF_INVTR_AFTR_THIRD_INV_TYPE_ID = "EML_INVTR_AFTRTRINV";

  public static class PartyInvitationErrorMessages {
    public static final String INVITATION_ALREADY_EXISTS =
        "An invitation already exists for the input email.";
  }

  public enum InvitationStatusIds {
    SENT("PARTYINV_SENT"),
    PENDING("PARTYINV_PENDING"),
    ACCEPTED("PARTYINV_ACCEPTED"),
    DECLINED("PARTYINV_DECLINED"),
    CANCELLED("PARTYINV_CANCELLED");

    private String statusId;

    InvitationStatusIds(String statusId) {
      this.statusId = statusId;
    }

    public String getStatusId() {
      return statusId;
    }
  }

  /**
   * Sends an invitation to a party.
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> inviteTeamMember(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String projectId = (String) context.get("projectId");
    String fromPartyId = (String) context.get("fromPartyId");
    String companyPartyId = (String) context.get("companyPartyId");
    String inviteeFirstName = (String) context.get("firstName");
    String inviteeLastName = (String) context.get("lastName");
    String inviteeEmail = (String) context.get("email");
    String inviteeExtension = (String) context.get("extension");
    String inviteeCountryCode = (String) context.get("countryCode");
    String inviteeAreaCode = (String) context.get("areaCode");
    String inviteeContactNumber = (String) context.get("contactNumber");
    String inviteePartyRegion = (String) context.get("partyRegion");
    String inviteeDesignation = (String) context.get("designation");
    String inviterOrgName = (String) context.get("companyName");
    String parentPartyEmail = (String) context.get("parentPartyEmail");
    List<String> roles = (List<String>) context.get("roles");
    String profile = (String) context.get("profile");

    String inviteePartyId = null;
    try {
      GenericValue partyInvitation =
          EntityQuery.use(delegator)
              .from("PartyInvitation")
              .where("emailAddress", inviteeEmail, "groupPartyId", companyPartyId)
              .queryFirst();
      if (UtilValidate.isNotEmpty(partyInvitation)) {
        Debug.logError("Invitation already exists with userLoginId : " + inviteeEmail, module);
        return ServiceUtil.returnError(PartyInvitationErrorMessages.INVITATION_ALREADY_EXISTS);
      }

      GenericValue userLoginResponse =
          EntityQuery.use(delegator)
              .from("UserLogin")
              .where("userLoginId", inviteeEmail)
              .queryFirst();

      if (UtilValidate.isNotEmpty(userLoginResponse)) {
        // existing record found, dirty data, do clean up first.
        List<GenericValue> recordsToRemove = UtilMisc.toList(userLoginResponse);

        List<GenericValue> userLoginAccessTokens =
            EntityQuery.use(delegator)
                .from("AccessToken")
                .where("userLoginId", inviteeEmail)
                .queryList();

        if (UtilValidate.isNotEmpty(userLoginAccessTokens)) {
          delegator.removeAll(userLoginAccessTokens);
        }

        List<GenericValue> userLoginContactMechs =
            EntityQuery.use(delegator)
                .from("ContactMech")
                .where("lastVerifiedBy", inviteeEmail)
                .queryList();

        if (UtilValidate.isNotEmpty(userLoginContactMechs)) {
          userLoginContactMechs.forEach(
              userLoginContactMech -> {
                // remove all comm events
                try {
                  List<GenericValue> userLoginCommEvents =
                      EntityQuery.use(delegator)
                          .from("CommunicationEvent")
                          .where("contactMechIdTo", userLoginContactMech.getString("contactMechId"))
                          .queryList();

                  userLoginCommEvents.forEach(
                      userLoginCommEvent -> {
                        try {
                          dispatcher.runSync(
                              "deleteCommunicationEvent",
                              UtilMisc.toMap(
                                  "userLogin",
                                  HierarchyUtils.getSysUserLogin(delegator),
                                  "communicationEventId",
                                  userLoginCommEvent.getString("communicationEventId")));
                        } catch (GenericServiceException e) {
                          Debug.logError(e, module);
                        }
                      });

                } catch (GenericEntityException e) {
                  Debug.logError(e, module);
                }
              });

          // remove all attached contents events
          try {
            List<GenericValue> userLoginCreatedContents =
                EntityQuery.use(delegator)
                    .from("Content")
                    .where("createdByUserLogin", inviteeEmail)
                    .queryList();

            userLoginCreatedContents.forEach(
                userLoginCreatedContent -> {
                  try {
                    dispatcher.runSync(
                        "removeContentAndRelated",
                        UtilMisc.toMap(
                            "userLogin",
                            HierarchyUtils.getSysUserLogin(delegator),
                            "contentId",
                            userLoginCreatedContent.getString("contentId")));
                  } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                  }
                });
          } catch (GenericEntityException e) {
            Debug.logError(e, module);
          }
          // remove all attached events
          try {
            List<GenericValue> userLoginCreatedEvents =
                EntityQuery.use(delegator)
                    .from("Event")
                    .where("createdByUserLogin", inviteeEmail)
                    .queryList();

            userLoginCreatedEvents.forEach(
                userLoginCreatedEvent -> {
                  try {
                    dispatcher.runSync(
                        "removeEventCascade",
                        UtilMisc.toMap(
                            "userLogin",
                            HierarchyUtils.getSysUserLogin(delegator),
                            "eventId",
                            userLoginCreatedEvent.getString("eventId")));
                  } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                  }
                });
          } catch (GenericEntityException e) {
            Debug.logError(e, module);
          }

          delegator.removeAll(userLoginContactMechs);
        }

        delegator.removeAll(recordsToRemove);
        userLoginResponse = null;
      }

      if (UtilValidate.isEmpty(userLoginResponse)) {
        Map<String, Object> createPersonCtx = new HashMap<>();
        createPersonCtx.put("userLogin", userLogin);
        createPersonCtx.put("firstName", inviteeFirstName);
        createPersonCtx.put("lastName", inviteeLastName);

        Map<String, Object> createPersonCtxResponse =
            dispatcher.runSync("createPerson", createPersonCtx);
        if (!ServiceUtil.isSuccess(createPersonCtxResponse)) {
          return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createPersonCtxResponse));
        }

        inviteePartyId = (String) createPersonCtxResponse.get("partyId");

        // create primary email address
        if (UtilValidate.isNotEmpty(inviteeEmail)) {
          Map<String, Object> createPartyEmailAddressCtx = FastMap.newInstance();
          createPartyEmailAddressCtx.put("userLogin", userLogin);
          createPartyEmailAddressCtx.put("emailAddress", inviteeEmail);
          createPartyEmailAddressCtx.put("contactMechPurposeTypeId", EmailTypesEnum.PRIMARY.getTypeId());
          createPartyEmailAddressCtx.put("partyId", inviteePartyId);

          Map<String, Object> createPartyEmailAddressResult =
              dispatcher.runSync("createPartyEmailAddress", createPartyEmailAddressCtx);
          if (!ServiceUtil.isSuccess(createPersonCtxResponse)) {
            return ServiceUtil.returnError(
                ServiceUtil.getErrorMessage(createPartyEmailAddressResult));
          }
        }

        if(UtilValidate.isNotEmpty(inviteeContactNumber)) {
          Map<String, Object> createPhoneCtx = FastMap.newInstance();
          createPhoneCtx.put("userLogin", userLogin);
          createPhoneCtx.put("partyId", inviteePartyId);
          createPhoneCtx.put("countryCode", inviteeCountryCode);
          createPhoneCtx.put("areaCode", inviteeAreaCode);
          createPhoneCtx.put("contactNumber", inviteeContactNumber);
          createPhoneCtx.put("contactMechPurposeTypeId", "PRIMARY_PHONE");
          createPhoneCtx.put("extension", inviteeExtension);

          Map<String, Object> createPersonPhoneCtxResponse =
                  dispatcher.runSync("createPartyTelecomNumber", createPhoneCtx);
          if (!ServiceUtil.isSuccess(createPersonPhoneCtxResponse)) {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createPersonPhoneCtxResponse));
          }
          String partyContactMechId = (String) createPersonPhoneCtxResponse.get("contactMechId");
          if (UtilValidate.isNotEmpty(inviteePartyRegion)) {
            Map<String, Object> createContactPhoneRegionCtx =
                    UtilMisc.toMap(
                            "userLogin",
                            userLogin,
                            "contactMechId",
                            partyContactMechId,
                            "partyRegion",
                            inviteePartyRegion);
            Map<String, Object> createContactPhoneRegionResult = null;
            try {
              createContactPhoneRegionResult =
                      dispatcher.runSync(
                              "createTelecomNumberRegionForParty", createContactPhoneRegionCtx);
            } catch (GenericServiceException e) {
              return ServiceUtil.returnError(
                      ServiceUtil.getErrorMessage(createContactPhoneRegionResult));
            }
          }
        }

        if (UtilValidate.isEmpty(inviteeDesignation)) {
          Map<String, Object> createPartyAttributeCtx = new HashMap<>();
          createPartyAttributeCtx.put("userLogin", userLogin);
          createPartyAttributeCtx.put("partyId", inviteePartyId);
          createPartyAttributeCtx.put("attrName", "Designation"); // defining new Attr name
          createPartyAttributeCtx.put("attrValue", inviteeDesignation);

          Map<String, Object> createPartyAttributeResponse =
              dispatcher.runSync("createPartyAttribute", createPartyAttributeCtx);
          if (!ServiceUtil.isSuccess(createPartyAttributeResponse)) {
            return ServiceUtil.returnError(
                ServiceUtil.getErrorMessage(createPartyAttributeResponse));
          }
        }

        boolean isAppMember =
            HierarchyUtils.checkPartyRole(delegator, inviteePartyId, "APP_MEMBER");
        if (!isAppMember) {
          Map<String, Object> createPartyRoleCtx = new HashMap<>();
          createPartyRoleCtx.put("userLogin", userLogin);
          createPartyRoleCtx.put("partyId", inviteePartyId);
          createPartyRoleCtx.put("roleTypeId", "APP_MEMBER");

          Map<String, Object> createPartyRoleResponse =
              dispatcher.runSync("createPartyRole", createPartyRoleCtx);
          if (!ServiceUtil.isSuccess(createPartyRoleResponse)) {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createPartyRoleResponse));
          }
        }

        // give manager role to userlogin if not added
        boolean isManager =
            HierarchyUtils.checkPartyRole(delegator, (String) userLogin.get("partyId"), "MANAGER");
        if (!isManager) {
          Map<String, Object> createPartyRoleCtx = new HashMap<>();
          createPartyRoleCtx.put("userLogin", userLogin);
          createPartyRoleCtx.put("partyId", userLogin.get("partyId"));
          createPartyRoleCtx.put("roleTypeId", "MANAGER");

          Map<String, Object> createPartyRoleResponse =
              dispatcher.runSync("createPartyRole", createPartyRoleCtx);
          if (!ServiceUtil.isSuccess(createPartyRoleResponse)) {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createPartyRoleResponse));
          }
        }

        String tempPassword = RandomStringUtils.randomAlphanumeric(10);
        // create user login for partyId
        Map<String, Object> createUserLoginCtx = new HashMap<>();
        createUserLoginCtx.put("userLogin", userLogin);
        createUserLoginCtx.put("userLoginId", inviteeEmail);
        createUserLoginCtx.put("currentPassword", tempPassword);
        createUserLoginCtx.put("currentPasswordVerify", tempPassword);
        createUserLoginCtx.put("requirePasswordChange", "Y");
        createUserLoginCtx.put("partyId", inviteePartyId);

        Map<String, Object> createUserLoginCtxResponse =
            dispatcher.runSync("createUserLogin", createUserLoginCtx);
        if (!ServiceUtil.isSuccess(createUserLoginCtxResponse)) {
          return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createUserLoginCtxResponse));
        }

        String applicationAccountType = roles.get(0);
        String applicationAccountTypeId = null;
        if (UtilValidate.areEqual(
            AccountUserRoleTypesEnum.ADMIN.getRoleName(), applicationAccountType)) {
          // user wants to change the permissions to admin
          applicationAccountTypeId = AccountUserRoleTypesEnum.ADMIN.getRole();
        } else if (UtilValidate.areEqual(
            AccountUserRoleTypesEnum.MANAGER.getRoleName(), applicationAccountType)) {
          // user wants to change the permissions to manager
          applicationAccountTypeId = AccountUserRoleTypesEnum.MANAGER.getRole();
        } else if (UtilValidate.areEqual(
            AccountUserRoleTypesEnum.MEMBER.getRoleName(), applicationAccountType)) {
          // user wants to change the permissions to member
          applicationAccountTypeId = AccountUserRoleTypesEnum.MEMBER.getRole();
        } else {
          // return error. 4xx, due to invalid input
          return ServiceUtil.returnError("no role found");
        }

        // call service to add the permissions
        Map<String, Object> changeAppUserPermissionsServiceResponse = FastMap.newInstance();
        if (UtilValidate.isNotEmpty(inviteePartyId)
            && UtilValidate.isNotEmpty(applicationAccountTypeId)) {
          try {
            changeAppUserPermissionsServiceResponse =
                dispatcher.runSync(
                    "changeAppUserPermissions",
                    UtilMisc.toMap(
                        "userLogin", userLogin,
                        "accountId", inviteeEmail,
                        "groupId", applicationAccountTypeId));
            if (ServiceUtil.isError(changeAppUserPermissionsServiceResponse)) {
              return ServiceUtil.returnError(
                  ServiceUtil.getErrorMessage(changeAppUserPermissionsServiceResponse));
            }
          } catch (GenericServiceException e) {
            return ServiceUtil.returnError(
                ServiceUtil.getErrorMessage(changeAppUserPermissionsServiceResponse));
          }
        }
      }

      /**
       * adding user to security group permission PARTYADMIN to allow handle the leads' CRUD
       * operations
       */
      try {
        Map<String, Object> addUserLoginToSecurityGroupResp =
            dispatcher.runSync(
                "addUserLoginToSecurityGroup",
                UtilMisc.toMap(
                    "userLogin",
                    userLogin,
                    "groupId",
                    "PARTYMGR_ADMIN",
                    "userLoginId",
                    inviteeEmail));

        if (ServiceUtil.isError(addUserLoginToSecurityGroupResp)) {
          String serviceError = ServiceUtil.getErrorMessage(addUserLoginToSecurityGroupResp);

          Debug.logError(
              "Error encountered while adding user to default PARTYMGR_ADMIN "
                  + "permission group, using service addUserLoginToSecurityGroup : "
                  + serviceError,
              module);
          return ServiceUtil.returnError(serviceError);
        }
      } catch (GenericServiceException e) {
        Debug.logError(e, "Exception while calling service addUserLoginToSecurityGroup : ", module);
        return ServiceUtil.returnError(e.getMessage());
      }

      // looping through all the roles passed for the user
      // Commennting this code as reporting not required at the moment
      //            for (String role : roles) {
      //                //getting parentRoleTypeId if exists
      //                GenericValue roleType =
      // EntityQuery.use(delegator).from("RoleType").where("roleTypeId", role).queryOne();
      //                if (UtilValidate.isNotEmpty(roleType.get("parentTypeId"))) {
      //                    //fetching all parties associated with parentRoleTypeId
      //                    List<GenericValue> partyRoles =
      // EntityQuery.use(delegator).from("PartyRole").where("roleTypeId",
      // roleType.get("parentTypeId")).queryList();
      //                    if (UtilValidate.isNotEmpty(partyRoles)) {
      //                        //looping through all the parties having role parentTypeId
      //                        for (GenericValue partyRole : partyRoles) {
      //                            Map<String, Object> createPartyRelationshipCtx = new
      // HashMap<>();
      //                            createPartyRelationshipCtx.put("userLogin", userLogin);
      //                            createPartyRelationshipCtx.put("partyIdFrom",
      // partyRole.get("partyId"));
      //                            createPartyRelationshipCtx.put("partyIdTo", inviteePartyId);
      //                            createPartyRelationshipCtx.put("roleTypeIdFrom",
      // roleType.get("parentTypeId"));
      //                            createPartyRelationshipCtx.put("roleTypeIdTo", role);
      //                            createPartyRelationshipCtx.put("partyRelationshipTypeId",
      // "REPORTS_TO");
      //
      //                            //creating relationship of parent and current role id as
      // reporting to
      //                            Map<String, Object> createPartyRelationshipResponse =
      // dispatcher.runSync("createPartyRelationship", createPartyRelationshipCtx);
      //                            if (!ServiceUtil.isSuccess(createPartyRelationshipResponse)) {
      //                                return
      // ServiceUtil.returnError(ServiceUtil.getErrorMessage(createPartyRelationshipResponse));
      //                            }
      //                        }
      //                    }
      //                }
      //            }

      if (UtilValidate.isNotEmpty(companyPartyId)) {
        // create party relationship with company
        GenericValue partyRelationWithCompany =
            EntityQuery.use(delegator)
                .from("PartyRelationship")
                .where(
                    "partyIdFrom",
                    companyPartyId,
                    "partyIdTo",
                    inviteePartyId,
                    "roleTypeIdFrom",
                    "_NA_",
                    "roleTypeIdTo",
                    "APP_MEMBER",
                    "partyRelationshipTypeId",
                    "MEMBERSHIP")
                .queryOne();
        if (UtilValidate.isEmpty(partyRelationWithCompany)) {
          Map<String, Object> createPartyRelationshipCtx = FastMap.newInstance();
          createPartyRelationshipCtx.put("userLogin", userLogin);
          createPartyRelationshipCtx.put("partyIdFrom", companyPartyId);
          createPartyRelationshipCtx.put("partyIdTo", inviteePartyId);
          createPartyRelationshipCtx.put("roleTypeIdFrom", "_NA_");
          createPartyRelationshipCtx.put("roleTypeIdTo", "APP_MEMBER");
          createPartyRelationshipCtx.put("partyRelationshipTypeId", "MEMBERSHIP");

          Map<String, Object> createPartyRelationshipResponse =
              dispatcher.runSync("createPartyRelationship", createPartyRelationshipCtx);
          if (!ServiceUtil.isSuccess(createPartyRelationshipResponse)) {
            return ServiceUtil.returnError(
                ServiceUtil.getErrorMessage(createPartyRelationshipResponse));
          }
        }
      }
      if (UtilValidate.isNotEmpty(companyPartyId)) {
        // create party relationship with company as company employee
        GenericValue partyRelationWithCompany =
            EntityQuery.use(delegator)
                .from("PartyRelationship")
                .where(
                    "partyIdFrom",
                    companyPartyId,
                    "partyIdTo",
                    inviteePartyId,
                    "roleTypeIdFrom",
                    "INTERNAL_ORGANIZATIO",
                    "roleTypeIdTo",
                    "EMPLOYEE",
                    "partyRelationshipTypeId",
                    "EMPLOYMENT")
                .queryOne();
        if (UtilValidate.isEmpty(partyRelationWithCompany)) {

          Map<String, Object> createPartyRoleCtx = new HashMap<>();
          createPartyRoleCtx.put("userLogin", userLogin);
          createPartyRoleCtx.put("partyId", inviteePartyId);
          createPartyRoleCtx.put("roleTypeId", "EMPLOYEE");

          Map<String, Object> createPartyRoleResponse =
              dispatcher.runSync("createPartyRole", createPartyRoleCtx);
          if (!ServiceUtil.isSuccess(createPartyRoleResponse)) {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createPartyRoleResponse));
          }

          Map<String, Object> createPartyRelationshipCtx = FastMap.newInstance();
          createPartyRelationshipCtx.put("userLogin", userLogin);
          createPartyRelationshipCtx.put("partyIdFrom", companyPartyId);
          createPartyRelationshipCtx.put("partyIdTo", inviteePartyId);
          createPartyRelationshipCtx.put("roleTypeIdFrom", "INTERNAL_ORGANIZATIO");
          createPartyRelationshipCtx.put("roleTypeIdTo", "EMPLOYEE");
          createPartyRelationshipCtx.put("partyRelationshipTypeId", "EMPLOYMENT");

          Map<String, Object> createPartyRelationshipResponse =
              dispatcher.runSync("createPartyRelationship", createPartyRelationshipCtx);
          if (!ServiceUtil.isSuccess(createPartyRelationshipResponse)) {
            return ServiceUtil.returnError(
                ServiceUtil.getErrorMessage(createPartyRelationshipResponse));
          }
        }
      }

      // send email to new party to inform them about their credential details
      GenericValue emailInfo =
          EntityQuery.use(delegator)
              .from("ProductStoreEmailSetting")
              .where("emailType", EMAIL_INVITE_TEAM_MEMBER_TYPE_ID)
              .cache(true)
              .queryOne();

      if (UtilValidate.isEmpty(emailInfo)) {
        String errorMessage =
            "Party invitation email data is missing, please load seed data with email setting type: "
                + EMAIL_INVITE_TEAM_MEMBER_TYPE_ID;
        Debug.logError(errorMessage, module);
        ServiceUtil.returnError(errorMessage);
      }

      Timestamp nowTimeStamp = UtilDateTime.nowTimestamp();

      String serverRootUrl =
          EntityUtilProperties.getPropertyValue("appconfig", "app.domain.url", delegator);
      // Invitee details, to whom invitation is sent
      Map<String, String> inviteeNameDetails =
          AxPartyHelper.getPartyNameDetails(
              delegator, HierarchyUtils.getPartyByPartyId(delegator, inviteePartyId));

      // Inviter details, who initiated the invitation
      String inviterPartyId = fromPartyId;
      GenericValue inviterPartyObj = HierarchyUtils.getPartyByPartyId(delegator, inviterPartyId);
      Map<String, String> inviterNameDetails =
          AxPartyHelper.getPartyNameDetails(delegator, inviterPartyObj);
      String inviterFirstName = inviterNameDetails.get("firstName");
      String inviterLastName = inviterNameDetails.get("lastName");
      String inviterEmail = userLogin.getString("userLoginId"); // email is the user login id

      Map inviterAndInviteeJwtMap =
          getInviterAndInviteeJwtMap(
              nowTimeStamp,
              inviteeNameDetails,
              inviteeEmail,
              inviterFirstName,
              inviterLastName,
              inviterEmail,
              inviterOrgName);

      String token = JWTUtils.generateJwt(inviterAndInviteeJwtMap);

      Map<String, Object> bodyParameters = FastMap.newInstance();
      bodyParameters.put("email", inviteeEmail);
      String url = serverRootUrl + "/auth/invite/accept?token=" + token;
      bodyParameters.put("serverRootUrl", url);
      bodyParameters.put("invitationAcceptanceUrl", url);
      bodyParameters.put("fullName", inviteeFirstName + " " + inviteeLastName);
      bodyParameters.put("organization", inviterOrgName);
      bodyParameters.put("invitedBy", parentPartyEmail);

      bodyParameters.put("inviteeName", inviteeFirstName);
      bodyParameters.put("inviteeFirstName", inviteeFirstName);
      bodyParameters.put("inviteeLastName", inviteeLastName);
      bodyParameters.put("inviteeEmail", inviteeEmail);

      bodyParameters.put("inviterName", inviterFirstName);
      bodyParameters.put("inviterFirstName", inviterFirstName);
      bodyParameters.put("inviterLastName", inviterLastName);
      bodyParameters.put("inviterEmail", inviterEmail);
      bodyParameters.put("inviterOrgName", inviterOrgName);

      // application org details
      Map appConfig = AppConfigUtil.getInstance(delegator).getAppConfiguration();
      bodyParameters.putAll(appConfig);

      Map<String, Object> emailCtx = FastMap.newInstance();
      emailCtx.put("userLogin", userLogin);
      emailCtx.put("contentType", emailInfo.get("contentType"));
      emailCtx.put("subject", emailInfo.get("subject"));
      emailCtx.put("sendFrom", emailInfo.get("fromAddress"));
      emailCtx.put("sendCc", emailInfo.get("ccAddress"));
      emailCtx.put("sendBcc", emailInfo.get("bccAddress"));
      emailCtx.put("sendTo", inviteeEmail);
      emailCtx.put("bodyParameters", bodyParameters);
      emailCtx.put("bodyScreenUri", (String) emailInfo.get("bodyScreenLocation"));

      Map<String, Object> sendResp = dispatcher.runSync("sendMailFromScreen", emailCtx);

      if (!ServiceUtil.isSuccess(sendResp)) {
        return ServiceUtil.returnError(
            "ProductStoreEmailSetting "
                + emailInfo.get("emailType")
                + " not found. Unable to send message.");
      }

      // create a party invitation.
      Timestamp now = UtilDateTime.nowTimestamp();
      // set expiration date, add 24 hours to now timestamp
      Timestamp expirationDate = getInvitationExpirationDate(now);

      Map<String, Object> createPartyInvitationResponse;
      try {
        String partyInvitationId = delegator.getNextSeqId("PartyInvitation");
        createPartyInvitationResponse =
            dispatcher.runSync(
                "createPartyInvitation",
                UtilMisc.toMap(
                    "userLogin", userLogin,
                    "partyInvitationId", partyInvitationId,
                    "partyIdFrom", fromPartyId,
                    "toName", inviteeFirstName.trim() + " " + inviteeLastName.trim(),
                    "emailAddress", inviteeEmail,
                    "partyId", inviteePartyId,
                    "statusId", InvitationStatusIds.PENDING.getStatusId(),
                    "groupPartyId", companyPartyId,
                    "lastInviteDate`", now,
                    "expirationDate", expirationDate));
        if (ServiceUtil.isError(createPartyInvitationResponse)) {
          return ServiceUtil.returnError(
              ServiceUtil.getErrorMessage(createPartyInvitationResponse));
        }
      } catch (GenericServiceException e) {
        Debug.logError(e, module);
        return ServiceUtil.returnError(e.getMessage());
      }

      serviceResult.put("partyId", inviteePartyId);
    } catch (GenericEntityException | GenericServiceException e) {
      Debug.logError(e, module);
      return ServiceUtil.returnError(e.getMessage());
    }

    return serviceResult;
  }

  // invitation expiration threshold
  private static final Integer INVITATION_EXPIRATION_THRESHOLD_IN_HOURS = 24;

  /**
   * Returns the expiration date for an invitation, 24 hours after the invitation date.
   *
   * @param invitationDate
   * @return
   */
  public static Timestamp getInvitationExpirationDate(Timestamp invitationDate) {
    // set expiration date, add 24 hours to now timestamp
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(invitationDate.getTime());
    cal.add(Calendar.HOUR, INVITATION_EXPIRATION_THRESHOLD_IN_HOURS);
    return new Timestamp(cal.getTime().getTime());
  }

  /**
   * Updates resource request status.
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> updateResourceRequestStatus(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String requestStatusId = (String) context.get("requestStatusId");
    Locale locale = (Locale) context.get("locale");
    String partyId = (String) context.get("partyId");
    String custRequestId = "";

    if (UtilValidate.isNotEmpty(requestStatusId)) {
      try {
        GenericValue custRequestStatus =
            EntityQuery.use(delegator).from("CustRequest").where("fromPartyId", partyId).queryOne();
        if (UtilValidate.isNotEmpty(custRequestStatus)) {
          custRequestId = (String) custRequestStatus.get("custRequestId");
        }
        GenericValue custStatusDesc =
            EntityQuery.use(delegator)
                .from("StatusItem")
                .where("statusId", requestStatusId, "statusTypeId", "CUSTREQ_STTS")
                .queryOne();

        if (UtilValidate.isNotEmpty(custRequestStatus)) {

          // check that status is defined as a valid change CRQ_ACCEPTED
          GenericValue statusValidChange =
              EntityQuery.use(delegator)
                  .from("StatusValidChange")
                  .where(
                      "statusId",
                      custRequestStatus.getString("statusId"),
                      "statusIdTo",
                      requestStatusId)
                  .queryOne();
          if (statusValidChange == null) {
            String errorMsg =
                "Cannot change party status from "
                    + custRequestStatus.getString("statusId")
                    + " to "
                    + requestStatusId;
            Debug.logWarning(errorMsg, module);
            return ServiceUtil.returnError(
                UtilProperties.getMessage(
                    resource,
                    "PartyStatusCannotBeChanged",
                    UtilMisc.toMap(
                        "partyFromStatusId",
                        custRequestStatus.getString("statusId"),
                        "partyToStatusId",
                        requestStatusId),
                    locale));
          }

          custRequestStatus.set("statusId", requestStatusId);
          custRequestStatus.set("description", custStatusDesc.getString("description"));

          delegator.store(custRequestStatus);
        }
      } catch (GenericEntityException e) {
        Debug.logError(e, e.getMessage(), module);
        return ServiceUtil.returnError(e.getMessage());
      }
    }

    result.put("custRequestId", custRequestId);
    return result;
  }

  /**
   * Returns the list of invitations pending sent by the input partyIdFrom.
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> searchPartyInvitations(
      DispatchContext dctx, Map<String, Object> context) throws GenericEntityException {
    Map<String, Object> result = ServiceUtil.returnSuccess();

    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();

    String keyword = (String) context.get("keyword");
    Integer viewSize = (Integer) context.get("viewSize");
    Integer startIndex = (Integer) context.get("startIndex");
    String invitedByPartyId = (String) context.get("invitedByPartyId");
    String inviteeName = (String) context.get("inviteeName");
    String inviteeEmail = (String) context.get("inviteeEmail");
    String invitedToJoinOrgPartyId = (String) context.get("invitedToJoinOrgPartyId");
    String invitedToJoinTeamPartyId = (String) context.get("invitedToJoinTeamPartyId");
    String statusId = (String) context.get("statusId");
    String sortBy = (String) context.get("sortBy");

    List<Map> invitationRecords = new LinkedList<>();
    long resultSize;

    try {

      if (UtilValidate.isEmpty(keyword)) keyword = "";

      if (viewSize == null || viewSize < 0) viewSize = 100;

      if (startIndex == null || startIndex <= 0) startIndex = 0;

      Set<String> fieldToSelect = FastSet.newInstance();

      fieldToSelect.add("partyInvitationId");
      fieldToSelect.add("toName");
      fieldToSelect.add("partyId");
      fieldToSelect.add("partyIdFrom");
      fieldToSelect.add("groupPartyId");
      fieldToSelect.add("teamPartyId");
      fieldToSelect.add("remindedCount");
      fieldToSelect.add("emailAddress");
      fieldToSelect.add("lastInviteDate");
      fieldToSelect.add("createdStamp");
      fieldToSelect.add("lastUpdatedStamp");
      fieldToSelect.add("expirationDate");

      Map<String, Object> searchInvitationsCtx =
          UtilMisc.toMap(
              "toName_op", "contains",
              "toName_ic", "Y",
              "emailAddress_op", "contains",
              "emailAddress_ic", "Y",
              "noConditionFind", "N");
      if (UtilValidate.isNotEmpty(invitedByPartyId))
        searchInvitationsCtx.put("partyIdFrom", invitedByPartyId);

      if (UtilValidate.isNotEmpty(inviteeName)) searchInvitationsCtx.put("toName", inviteeName);

      if (UtilValidate.isNotEmpty(inviteeEmail))
        searchInvitationsCtx.put("emailAddress", inviteeEmail);

      if (UtilValidate.isNotEmpty(invitedToJoinOrgPartyId))
        searchInvitationsCtx.put("groupPartyId", invitedToJoinOrgPartyId);

      if (UtilValidate.isNotEmpty(invitedToJoinTeamPartyId))
        searchInvitationsCtx.put("teamPartyId", invitedToJoinTeamPartyId);

      if (UtilValidate.isNotEmpty(statusId)) searchInvitationsCtx.put("statusId", statusId);

      Debug.logInfo("Invoking performFind with inputFields : " + searchInvitationsCtx, module);

      Map performFindCtx =
          UtilMisc.toMap(
              "inputFields", searchInvitationsCtx,
              "entityName", "PartyInvitationAndDetail",
              "orderBy", sortBy,
              "viewIndex", startIndex,
              "viewSize", viewSize);

      Map<String, Object> searchInvitationsResult =
          dispatcher.runSync("performFindList", performFindCtx);

      List<GenericValue> resultPartialList = (List) searchInvitationsResult.get("list");

      // get total count for pagination
      resultSize = (Integer) searchInvitationsResult.get("listSize");

      for (GenericValue invitations : resultPartialList) {
        String invitationStatusId = invitations.getString("statusId");
        String invitationStatusDescription = invitationStatusId;
        if (UtilValidate.isNotEmpty(invitationStatusId)) {
          GenericValue invitationStatusObj =
              EntityQuery.use(delegator)
                  .from("StatusItem")
                  .where("statusId", statusId)
                  .cache()
                  .queryOne();
          if (UtilValidate.isNotEmpty(invitationStatusObj)) {
            invitationStatusDescription = invitationStatusObj.getString("description");
          }
        }

        Map invitationRecord =
            UtilMisc.toMap(
                "invitationId", invitations.getString("partyInvitationId"),
                "statusId", invitationStatusId,
                "status", invitationStatusDescription,
                "inviteeId", invitations.getString("partyId"),
                "inviteeEmail", invitations.getString("emailAddress"),
                "inviteeToName", invitations.getString("toName"),
                "inviteeDisplayName", invitations.getString("toDisplayName"),
                "inviteePhotoUrl", invitations.getString("toPhotoUrl"),
                "inviterId", invitations.getString("partyIdFrom"),
                "inviterPhotoUrl", invitations.getString("fromPhotoUrl"),
                "inviterDisplayName", invitations.getString("fromDisplayName"),
                "inviterEmail", invitations.getString("fromEmail"),
                "lastInvitedAt", invitations.getTimestamp("lastInviteDate"),
                "expirationDate", invitations.getTimestamp("expirationDate"),
                "acceptedDate", invitations.getTimestamp("acceptedDate"),
                "remindedCount", invitations.getLong("remindedCount"));

        invitationRecords.add(invitationRecord);
      }
    } catch (Exception e) {
      Debug.logError(e, e.getMessage(), module);
      return ServiceUtil.returnError(e.getMessage());
    }

    result.put("searchResults", invitationRecords);
    result.put("resultSize", resultSize);
    return result;
  }

  /**
   * Resends a party invitation, checks for a valid invitation, updates last invited on date as
   * well.
   *
   * @param dctx
   * @param context
   * @return
   * @throws GenericEntityException
   */
  public static Map<String, Object> resendPartyInvitation(
      DispatchContext dctx, Map<String, Object> context) throws GenericEntityException {
    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String invitationId = (String) context.get("invitationId");

    try {
      GenericValue partyInvitation =
          EntityQuery.use(delegator)
              .from("PartyInvitation")
              .where("partyInvitationId", invitationId)
              .queryOne();
      if (UtilValidate.isEmpty(partyInvitation)) {
        Debug.logError("No invitation found with invitationId : " + invitationId, module);
        return ServiceUtil.returnError(
            "No invitation found with id # "
                + invitationId
                + ", please provide a valid invitation id to proceed.");
      }

      GenericValue emailInfo =
          EntityQuery.use(delegator)
              .from("ProductStoreEmailSetting")
              .where("emailType", EMAIL_REINVITE_TEAM_MEMBER_TYPE_ID)
              .cache(true)
              .queryOne();

      Timestamp nowTimeStamp = UtilDateTime.nowTimestamp();

      String serverRootUrl =
          EntityUtilProperties.getPropertyValue("appconfig", "app.domain.url", delegator);

      // Invitee details, to whom invitation is sent
      String inviteePartyId = partyInvitation.getString("partyId");
      Map<String, String> inviteeNameDetails =
          AxPartyHelper.getPartyNameDetails(
              delegator, HierarchyUtils.getPartyByPartyId(delegator, inviteePartyId));
      String inviteeFirstName = inviteeNameDetails.get("firstName");
      String inviteeLastName = inviteeNameDetails.get("lastName");
      String inviteeEmail = partyInvitation.getString("emailAddress");

      // Inviter details, who initiated the invitation
      String inviterPartyId = partyInvitation.getString("partyIdFrom");
      GenericValue inviterPartyObj = HierarchyUtils.getPartyByPartyId(delegator, inviterPartyId);
      Map<String, String> inviterNameDetails =
          AxPartyHelper.getPartyNameDetails(delegator, inviterPartyObj);
      String inviterFirstName = inviterNameDetails.get("firstName");
      String inviterLastName = inviterNameDetails.get("lastName");
      String inviterEmail = userLogin.getString("userLoginId"); // email is the user login id

      GenericValue inviterOrgObj = PartyGroupForPartyUtils.getPartyGroupForPartyId(inviterPartyObj);
      String inviterOrgName = "";
      if (UtilValidate.isNotEmpty(inviterOrgObj)) {
        inviterOrgName = inviterOrgObj.getString("groupName");
      }

      Map inviterAndInviteeJwtMap =
          getInviterAndInviteeJwtMap(
              nowTimeStamp,
              inviteeNameDetails,
              inviteeEmail,
              inviterFirstName,
              inviterLastName,
              inviterEmail,
              inviterOrgName);

      String token = JWTUtils.generateJwt(inviterAndInviteeJwtMap);

      Map<String, Object> bodyParameters = FastMap.newInstance();
      bodyParameters.put("email", inviteeEmail);
      String url = serverRootUrl + "/auth/invite/accept?token=" + token;
      bodyParameters.put("serverRootUrl", url);
      bodyParameters.put("invitationAcceptanceUrl", url);
      bodyParameters.put("partyName", inviteeFirstName + " " + inviteeLastName);

      bodyParameters.put("inviteeName", inviteeFirstName);
      bodyParameters.put("inviteeFirstName", inviteeFirstName);
      bodyParameters.put("inviteeLastName", inviteeLastName);
      bodyParameters.put("inviteeEmail", inviteeEmail);

      bodyParameters.put("inviterName", inviterFirstName);
      bodyParameters.put("inviterFirstName", inviterFirstName);
      bodyParameters.put("inviterLastName", inviterLastName);
      bodyParameters.put("inviterEmail", inviterEmail);
      bodyParameters.put("inviterOrgName", inviterOrgName);

      // application org details
      Map appConfig = AppConfigUtil.getInstance(delegator).getAppConfiguration();
      bodyParameters.putAll(appConfig);

      Map<String, Object> emailCtx = FastMap.newInstance();
      emailCtx.put("userLogin", userLogin);
      emailCtx.put("partyId", inviteePartyId);
      emailCtx.put("contentType", emailInfo.get("contentType"));
      emailCtx.put("subject", emailInfo.get("subject"));
      emailCtx.put("sendFrom", emailInfo.get("fromAddress"));
      emailCtx.put("sendCc", emailInfo.get("ccAddress"));
      emailCtx.put("sendBcc", emailInfo.get("bccAddress"));
      emailCtx.put("sendTo", inviteeEmail);
      emailCtx.put("bodyParameters", bodyParameters);
      emailCtx.put("bodyScreenUri", emailInfo.get("bodyScreenLocation"));

      Map<String, Object> sendResp = dispatcher.runSync("sendMailFromScreen", emailCtx);

      if (!ServiceUtil.isSuccess(sendResp)) {
        return ServiceUtil.returnError(
            "ProductStoreEmailSetting "
                + emailInfo.get("emailType")
                + " not found. Unable to send message.");
      }

      try {
        partyInvitation.set("statusId", InvitationStatusIds.PENDING.getStatusId());
        partyInvitation.set("lastInviteDate", nowTimeStamp);

        // update expiration date by pushing to 24 hours from now.
        partyInvitation.set("expirationDate", getInvitationExpirationDate(nowTimeStamp));

        partyInvitation.store();
      } catch (GenericEntityException e) {
        Debug.logError(e, module);
        return ServiceUtil.returnError(e.getMessage());
      }

    } catch (GenericServiceException e) {
      Debug.logError(e, e.getMessage(), module);
      return ServiceUtil.returnError(e.getMessage());
    }

    return result;
  }

  private static Map getInviterAndInviteeJwtMap(
      Timestamp nowTimeStamp,
      Map<String, String> inviteeNameDetails,
      String inviteeEmail,
      String inviterFirstName,
      String inviterLastName,
      String inviterEmail,
      String inviterOrgName) {
    Map dataMap = FastMap.newInstance();
    dataMap.put("userLoginId", inviteeEmail);
    dataMap.put("nowTimeStamp", nowTimeStamp);
    dataMap.put("UUID", UUID.randomUUID());
    dataMap.put("firstName", inviteeNameDetails.get("firstName"));
    dataMap.put("lastName", inviteeNameDetails.get("lastName"));

    dataMap.put("inviterName", inviterFirstName);
    dataMap.put("inviterFirstName", inviterFirstName);
    dataMap.put("inviterLastName", inviterLastName);
    dataMap.put("inviterEmail", inviterEmail);
    dataMap.put("inviterOrgName", inviterOrgName);
    return dataMap;
  }

  /**
   * To get list of pending or accepted resource invitations.
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> getPendingOrAcceptedInvitations(
      DispatchContext dctx, Map<String, Object> context) throws GenericEntityException {
    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String keyword = (String) context.get("keyword");
    Integer viewSize = (Integer) context.get("viewSize");
    Integer startIndex = (Integer) context.get("startIndex");
    String groupPartyId = (String) context.get("groupPartyId");

    List<Object> invitationRecords = new LinkedList<>();
    long resultSize = 0;

    String query = "";
    if ("pending".equalsIgnoreCase(keyword)) {
      query = InvitationStatusIds.PENDING.getStatusId();
    } else {
      query = InvitationStatusIds.ACCEPTED.getStatusId();
    }

    try {

      if (UtilValidate.isEmpty(keyword)) keyword = "";

      if (viewSize == null || viewSize < 0) viewSize = 10;

      if (startIndex == null || startIndex <= 0) startIndex = 0;

      int lowIndex = startIndex + 1;
      int highIndex = (startIndex) + viewSize;

      EntityFindOptions efo = new EntityFindOptions();
      efo.setMaxRows(highIndex);
      efo.setResultSetType(EntityFindOptions.TYPE_SCROLL_INSENSITIVE);
      efo.setDistinct(true);

      // order by
      List<String> orderBy = FastList.newInstance();
      orderBy.add("-lastInviteDate");

      Set<String> fieldToSelect = FastSet.newInstance();
      fieldToSelect.add("partyInvitationId");
      fieldToSelect.add("toName");
      fieldToSelect.add("partyId");
      fieldToSelect.add("partyIdFrom");
      fieldToSelect.add("emailAddress");
      fieldToSelect.add("lastInviteDate");
      fieldToSelect.add("createdStamp");
      fieldToSelect.add("lastUpdatedStamp");

      EntityCondition cond =
          EntityCondition.makeCondition(
              EntityOperator.AND,
              EntityCondition.makeCondition("statusId", query),
              EntityCondition.makeCondition("groupPartyId", groupPartyId));

      TransactionUtil.begin();
      EntityListIterator searchResult =
          delegator.find("PartyInvitation", cond, null, fieldToSelect, orderBy, efo);
      List<GenericValue> resultPartialList =
          searchResult.getPartialList(lowIndex, highIndex - lowIndex + 1);

      // get total count for pagination
      resultSize = searchResult.getResultsSizeAfterPartialList();

      for (GenericValue invitationPendingAcceptance : resultPartialList) {
        Map<String, Object> inviteMap = new HashMap<>();
        inviteMap.put(
            "partyInvitationId", invitationPendingAcceptance.getString("partyInvitationId"));
        inviteMap.put("toName", invitationPendingAcceptance.getString("toName"));
        Map partyIdFromInfo =
            AxPartyHelper.getPartyBasicDetails(
                delegator, invitationPendingAcceptance.getString("partyIdFrom"));
        inviteMap.put("partyIdFrom", partyIdFromInfo);
        inviteMap.put("emailAddress", invitationPendingAcceptance.getString("emailAddress"));
        inviteMap.put("lastInviteDate", invitationPendingAcceptance.getString("lastInviteDate"));
        inviteMap.put("createdStamp", invitationPendingAcceptance.getString("createdStamp"));
        inviteMap.put(
            "lastUpdatedStamp", invitationPendingAcceptance.getString("lastUpdatedStamp"));

        invitationRecords.add(inviteMap);
      }

      TransactionUtil.commit();
      searchResult.close();
    } catch (Exception e) {
      Debug.logError(e, e.getMessage(), module);
      return ServiceUtil.returnError(e.getMessage());
    }

    result.put("searchResults", invitationRecords);
    result.put("resultSize", resultSize);
    return result;
  }

  public static Map<String, Object> acceptUserInvitation(
      DispatchContext dctx, Map<String, Object> context) throws GenericEntityException {

    Map<String, Object> serviceResult = ServiceUtil.returnSuccess();

    LocalDispatcher dispatcher = dctx.getDispatcher();
    Delegator delegator = dctx.getDelegator();
    Locale locale = (Locale) context.get("locale");

    GenericValue userLogin = (GenericValue) context.get("userLogin");

    String emailAddress = (String) context.get("emailAddress");
    String password = (String) context.get("password");
    emailAddress = emailAddress.trim();
    String partyId = "";

    try {
      // update userLogin password
      GenericValue userloginRec =
          EntityQuery.use(delegator)
              .from("UserLogin")
              .where("userLoginId", emailAddress)
              .queryOne();

      if (UtilValidate.isNotEmpty(userloginRec)) {
        partyId = userloginRec.getString("partyId");
        Map<String, Object> updatePasswordUserLoginCtx = FastMap.newInstance();
        updatePasswordUserLoginCtx.put("userLogin", userLogin);
        updatePasswordUserLoginCtx.put("userLoginId", emailAddress);
        updatePasswordUserLoginCtx.put("newPassword", password);
        updatePasswordUserLoginCtx.put("newPasswordVerify", password);
        Map updatePasswordUserLoginResponse =
            dispatcher.runSync("updatePassword", updatePasswordUserLoginCtx);
        if (ServiceUtil.isError(updatePasswordUserLoginResponse)) {
          return ServiceUtil.returnError(
              ServiceUtil.getErrorMessage(updatePasswordUserLoginResponse));
        }
      }

      // update the status of invitation record to accepted
      GenericValue existingInvitation =
          EntityQuery.use(delegator)
              .from("PartyInvitation")
              .where("emailAddress", emailAddress)
              .queryFirst();

      if (UtilValidate.isNotEmpty(existingInvitation)) {
        // update the existing entry
        Map updatePartyInvitationResponse =
            dispatcher.runSync(
                "updatePartyInvitation",
                UtilMisc.toMap(
                    "partyInvitationId", existingInvitation.getString("partyInvitationId"),
                    "partyId", partyId,
                    "acceptedDate", UtilDateTime.nowTimestamp(),
                    "statusId", InvitationStatusIds.ACCEPTED.getStatusId(),
                    "userLogin", userLogin));
        if (ServiceUtil.isError(updatePartyInvitationResponse)) {
          return ServiceUtil.returnError(
              ServiceUtil.getErrorMessage(updatePartyInvitationResponse));
        }
      }

      Map<String, Object> findPartyFromEmailAddressResp =
          dispatcher.runSync(
              "findPartyFromEmailAddress",
              UtilMisc.<String, Object>toMap(
                  "address", emailAddress, "caseInsensitive", "Y", "userLogin", userLogin));
      if (UtilValidate.isNotEmpty(findPartyFromEmailAddressResp.get("contactMechId"))) {
        Timestamp nowTimeStamp = UtilDateTime.nowTimestamp();
        GenericValue contactMech =
            EntityQuery.use(delegator)
                .from("ContactMech")
                .where("contactMechId", findPartyFromEmailAddressResp.get("contactMechId"))
                .queryOne();
        contactMech.put("isVerified", "Y");
        contactMech.put("lastVerifiedAt", nowTimeStamp);
        contactMech.put("lastVerifiedBy", emailAddress);
        contactMech.store();
      }

      // run a sanity check to make sure Party table has .email/displayName/PhotoUrl updated.
      // fix the display name
      dispatcher.runSync(
          "populateBasicInformationForParty",
          UtilMisc.toMap("partyId", partyId, "userLogin", userLogin));

    } catch (Exception e) {
      return ServiceUtil.returnError(e.getMessage());
    }

    return serviceResult;
  }

  /**
   * Revokes an existing invitation, does not delete any party records associated with it.
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> revokePartyInvitation(
      DispatchContext dctx, Map<String, Object> context) throws GenericEntityException {
    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String invitationId = (String) context.get("invitationId");

    Debug.logInfo(
        "Revoking party invitation #"
            + invitationId
            + " requested by user login: "
            + userLogin.getString("userLoginId"),
        module);

    try {
      dispatcher.runSync(
          "deletePartyInvitation",
          UtilMisc.toMap("partyInvitationId", invitationId, "userLogin", userLogin));
    } catch (GenericServiceException e) {
      Debug.logError(e, module);
    }

    return result;
  }

  /**
   * Service to resend invitation email to notify the invitee after 24 hours. This service auto
   * renews the link, update last invited date and reminder count.
   *
   * @param dctx
   * @param context
   * @return
   * @throws GenericEntityException
   */
  public static Map<String, Object> resendPartyInvitationScheduler(
      DispatchContext dctx, Map<String, Object> context) throws GenericEntityException {

    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();
    GenericValue userLogin = (GenericValue) context.get("userLogin");

    try {

      Timestamp now = UtilDateTime.nowTimestamp();
      EntityConditionList<EntityExpr> cond =
          EntityCondition.makeCondition(
              UtilMisc.toList(
                  EntityCondition.makeCondition("expirationDate", EntityOperator.EQUALS, now),
                  EntityCondition.makeCondition("expirationDate", EntityOperator.LESS_THAN, now)),
              EntityOperator.OR);

      EntityConditionList<EntityCondition> conditions =
          EntityCondition.makeCondition(
              UtilMisc.toList(
                  cond,
                  EntityCondition.makeCondition(
                      "statusId",
                      EntityOperator.EQUALS,
                      InvitationStatusIds.PENDING.getStatusId())),
              EntityOperator.AND);

      List<GenericValue> partyInvitations =
          EntityQuery.use(delegator).from("PartyInvitation").where(conditions).queryList();
      if (UtilValidate.isEmpty(partyInvitations)) {
        Debug.logError("No pending invitations found.", module);
        return ServiceUtil.returnSuccess();
      }

      GenericValue emailInfo =
          EntityQuery.use(delegator)
              .from("ProductStoreEmailSetting")
              .where("emailType", EMAIL_REINVITE_TEAM_MEMBER_TYPE_ID)
              .cache(true)
              .queryOne();

      String serverRootUrl =
          EntityUtilProperties.getPropertyValue("appconfig", "app.domain.url", delegator);

      for (GenericValue partyInvitation : partyInvitations) {
        Long existingRemindedCount = partyInvitation.getLong("remindedCount");
        if (UtilValidate.isEmpty(existingRemindedCount)) {
          existingRemindedCount = 0l;
        }

        if (existingRemindedCount < 3) {
          // Invitee details, to whom invitation is sent
          String inviteePartyId = partyInvitation.getString("partyId");
          Map<String, String> inviteeNameDetails =
              AxPartyHelper.getPartyNameDetails(
                  delegator, HierarchyUtils.getPartyByPartyId(delegator, inviteePartyId));
          String inviteeFirstName = inviteeNameDetails.get("firstName");
          String inviteeLastName = inviteeNameDetails.get("lastName");
          String inviteeEmail = partyInvitation.getString("emailAddress");

          // Inviter details, who initiated the invitation
          String inviterPartyId = partyInvitation.getString("partyIdFrom");
          GenericValue inviterPartyObj =
              HierarchyUtils.getPartyByPartyId(delegator, inviterPartyId);
          Map<String, String> inviterNameDetails =
              AxPartyHelper.getPartyNameDetails(delegator, inviterPartyObj);
          String inviterFirstName = inviterNameDetails.get("firstName");
          String inviterLastName = inviterNameDetails.get("lastName");
          GenericValue inviterEmailGv =
              AxPartyHelper.findPartyLatestEmailAddress(inviterPartyId, delegator);
          String inviterEmail = inviterEmailGv.getString("infoString");

          GenericValue inviterOrgObj =
              PartyGroupForPartyUtils.getPartyGroupForPartyId(inviterPartyObj);
          String inviterOrgName = "";
          if (UtilValidate.isNotEmpty(inviterOrgObj)) {
            inviterOrgName = inviterOrgObj.getString("groupName");
          }

          Map inviterAndInviteeJwtMap =
              getInviterAndInviteeJwtMap(
                  now,
                  inviteeNameDetails,
                  inviteeEmail,
                  inviterFirstName,
                  inviterLastName,
                  inviterEmail,
                  inviterOrgName);

          String token = JWTUtils.generateJwt(inviterAndInviteeJwtMap);

          Map<String, Object> bodyParameters = FastMap.newInstance();
          bodyParameters.put("email", inviteeEmail);
          String url = serverRootUrl + "/auth/invite/accept?token=" + token;
          bodyParameters.put("serverRootUrl", url);
          bodyParameters.put("invitationAcceptanceUrl", url);
          bodyParameters.put("partyName", inviteeFirstName + " " + inviteeLastName);

          bodyParameters.put("inviteeName", inviteeFirstName);
          bodyParameters.put("inviteeFirstName", inviteeFirstName);
          bodyParameters.put("inviteeLastName", inviteeLastName);
          bodyParameters.put("inviteeEmail", inviteeEmail);

          bodyParameters.put("inviterName", inviterFirstName);
          bodyParameters.put("inviterFirstName", inviterFirstName);
          bodyParameters.put("inviterLastName", inviterLastName);
          bodyParameters.put("inviterEmail", inviterEmail);
          bodyParameters.put("inviterOrgName", inviterOrgName);

          // application org details
          Map appConfig = AppConfigUtil.getInstance(delegator).getAppConfiguration();
          bodyParameters.putAll(appConfig);

          Map<String, Object> emailCtx = FastMap.newInstance();
          emailCtx.put("userLogin", userLogin);
          emailCtx.put("partyId", inviteePartyId);
          emailCtx.put("contentType", emailInfo.get("contentType"));
          emailCtx.put("subject", emailInfo.get("subject"));
          emailCtx.put("sendFrom", emailInfo.get("fromAddress"));
          emailCtx.put("sendCc", emailInfo.get("ccAddress"));
          emailCtx.put("sendBcc", emailInfo.get("bccAddress"));
          emailCtx.put("sendTo", inviteeEmail);
          emailCtx.put("bodyParameters", bodyParameters);
          emailCtx.put("bodyScreenUri", emailInfo.get("bodyScreenLocation"));

          Map<String, Object> sendResp = dispatcher.runSync("sendMailFromScreen", emailCtx);

          if (!ServiceUtil.isSuccess(sendResp)) {
            return ServiceUtil.returnError(
                "ProductStoreEmailSetting "
                    + emailInfo.get("emailType")
                    + " not found. Unable to send message.");
          }
        } else if (existingRemindedCount == 3) {
          // notify the inviter that invitee has not accepted the invitation  yet, along with the
          // reminder button
          dispatcher.runSync(
              "notifyInviterAfterThirdInvitation",
              UtilMisc.toMap(
                  "userLogin",
                  userLogin,
                  "invitationId",
                  partyInvitation.getString("partyInvitationId")));
        }

        Long remindedCount = 1L;
        if (UtilValidate.isNotEmpty(existingRemindedCount)) {
          remindedCount = existingRemindedCount + remindedCount;
        }
        partyInvitation.set("statusId", InvitationStatusIds.PENDING.getStatusId());
        partyInvitation.set("remindedCount", remindedCount);
        partyInvitation.set("lastInviteDate", now);

        partyInvitation.store();
      }

    } catch (GenericServiceException | GenericEntityException e) {
      Debug.logError(e, e.getMessage(), module);
      return ServiceUtil.returnError(e.getMessage());
    }

    return result;
  }

  /**
   * Validate if an email already has an invitation accepted or pending.
   *
   * @param dctx
   * @param context
   * @return
   */
  public static Map<String, Object> checkInvitationByEmail(
      DispatchContext dctx, Map<String, Object> context) {
    Map<String, Object> result = ServiceUtil.returnSuccess();

    Delegator delegator = dctx.getDelegator();

    String email = (String) context.get("email");
    result.put("email", email);
    boolean isInvited = false;
    String status = null;
    String invitationId = null;
    try {
      GenericValue partyInvitationGv =
          EntityQuery.use(delegator)
              .from("PartyInvitation")
              .where("emailAddress", email)
              .queryOne();
      if (UtilValidate.isNotEmpty(partyInvitationGv)) {
        isInvited = true;
        String statusId = partyInvitationGv.getString("statusId");
        status = CommonHelper.getStatusItemDesc(delegator, statusId);
        invitationId = partyInvitationGv.getString("partyInvitationId");
      }
    } catch (GenericEntityException | GenericServiceException e) {
      Debug.logError(
          "An error occurred while invoking fetching the party invitation status.", module);
    }
    result.put("isInvited", isInvited);
    result.put("status", status);
    result.put("invitationId", invitationId);
    return result;
  }

  /**
   * Notify inviter once the invitation has been accepted.
   *
   * @param dctx
   * @param context
   * @return
   * @throws GenericEntityException
   */
  public static Map<String, Object> notifyInviterOnInvitationAcceptance(
      DispatchContext dctx, Map<String, Object> context) throws GenericEntityException {

    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String emailAddress = (String) context.get("emailAddress");

    try {

      GenericValue partyInvitation =
          EntityQuery.use(delegator)
              .from("PartyInvitation")
              .where("emailAddress", emailAddress)
              .queryFirst();
      if (UtilValidate.isEmpty(partyInvitation)) {
        Debug.logError("No invitation found with email : " + emailAddress, module);
        return ServiceUtil.returnError(
            "No invitation found with id # "
                + emailAddress
                + ", please provide a valid invitation id to proceed.");
      }

      GenericValue emailInfo =
          EntityQuery.use(delegator)
              .from("ProductStoreEmailSetting")
              .where("emailType", EMAIL_NTF_INVTR_ON_INV_ACC_TYPE_ID)
              .cache(true)
              .queryOne();

      // Invitee details, to whom invitation is sent
      String inviteePartyId = partyInvitation.getString("partyId");
      Map<String, String> inviteeNameDetails =
          AxPartyHelper.getPartyNameDetails(
              delegator, HierarchyUtils.getPartyByPartyId(delegator, inviteePartyId));
      String inviteeFirstName = inviteeNameDetails.get("firstName");
      String inviteeLastName = inviteeNameDetails.get("lastName");
      String inviteeEmail = partyInvitation.getString("emailAddress");

      // Inviter details, who initiated the invitation
      String inviterPartyId = partyInvitation.getString("partyIdFrom");
      GenericValue inviterPartyObj = HierarchyUtils.getPartyByPartyId(delegator, inviterPartyId);
      Map<String, String> inviterNameDetails =
          AxPartyHelper.getPartyNameDetails(delegator, inviterPartyObj);
      String inviterFirstName = inviterNameDetails.get("firstName");
      String inviterLastName = inviterNameDetails.get("lastName");
      GenericValue inviterEmailGv =
          AxPartyHelper.findPartyLatestEmailAddress(inviterPartyId, delegator);
      String inviterEmail = inviterEmailGv.getString("infoString");

      GenericValue inviterOrgObj = PartyGroupForPartyUtils.getPartyGroupForPartyId(inviterPartyObj);
      String inviterOrgName = "";
      if (UtilValidate.isNotEmpty(inviterOrgObj)) {
        inviterOrgName = inviterOrgObj.getString("groupName");
      }

      Map<String, Object> bodyParameters = FastMap.newInstance();
      bodyParameters.put("email", inviteeEmail);
      bodyParameters.put("partyName", inviteeFirstName + " " + inviteeLastName);

      bodyParameters.put("inviteeName", inviteeFirstName);
      bodyParameters.put("inviteeFirstName", inviteeFirstName);
      bodyParameters.put("inviteeLastName", inviteeLastName);
      bodyParameters.put("inviteeEmail", inviteeEmail);

      bodyParameters.put("inviterName", inviterFirstName);
      bodyParameters.put("inviterFirstName", inviterFirstName);
      bodyParameters.put("inviterLastName", inviterLastName);
      bodyParameters.put("inviterEmail", inviterEmail);
      bodyParameters.put("inviterOrgName", inviterOrgName);

      // application org details
      Map appConfig = AppConfigUtil.getInstance(delegator).getAppConfiguration();
      bodyParameters.putAll(appConfig);

      Map<String, Object> emailCtx = FastMap.newInstance();
      emailCtx.put("userLogin", userLogin);
      emailCtx.put("partyId", partyInvitation.getString("partyIdFrom"));
      emailCtx.put("contentType", emailInfo.get("contentType"));
      emailCtx.put("subject", emailInfo.get("subject"));
      emailCtx.put("sendFrom", emailInfo.get("fromAddress"));
      emailCtx.put("sendCc", emailInfo.get("ccAddress"));
      emailCtx.put("sendBcc", emailInfo.get("bccAddress"));
      emailCtx.put("sendTo", inviterEmail);
      emailCtx.put("bodyParameters", bodyParameters);
      emailCtx.put("bodyScreenUri", emailInfo.get("bodyScreenLocation"));

      Map<String, Object> sendResp = dispatcher.runSync("sendMailFromScreen", emailCtx);

      if (!ServiceUtil.isSuccess(sendResp)) {
        return ServiceUtil.returnError(
            "ProductStoreEmailSetting "
                + emailInfo.get("emailType")
                + " not found. Unable to send message.");
      }

    } catch (GenericServiceException e) {
      Debug.logError(e, e.getMessage(), module);
      return ServiceUtil.returnError(e.getMessage());
    }

    return result;
  }

  /**
   * Notify the inviter via email requesting a new link, add links in the email to send fresh
   * invitation.
   *
   * @param dctx
   * @param context
   * @return
   * @throws GenericEntityException
   */
  public static Map<String, Object> notifyInviterOnInvitationExpiry(
      DispatchContext dctx, Map<String, Object> context) throws GenericEntityException {

    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String invitationId = (String) context.get("invitationId");

    try {

      GenericValue partyInvitation =
          EntityQuery.use(delegator)
              .from("PartyInvitation")
              .where("partyInvitationId", invitationId)
              .queryOne();
      if (UtilValidate.isEmpty(partyInvitation)) {
        Debug.logError("No invitation found with invitationId : " + invitationId, module);
        return ServiceUtil.returnError(
            "No invitation found with id # "
                + invitationId
                + ", please provide a valid invitation id to proceed.");
      }

      GenericValue emailInfo =
          EntityQuery.use(delegator)
              .from("ProductStoreEmailSetting")
              .where("emailType", EMAIL_NTF_INVTR_ON_INV_EXP_TYPE_ID)
              .cache(true)
              .queryOne();

      Map<String, Object> bodyParameters =
          getInviterAndInviteeBodyParamMap(delegator, partyInvitation);

      String serverRootUrl =
          EntityUtilProperties.getPropertyValue("appconfig", "app.domain.url", delegator);
      String url = serverRootUrl + "/admin/members/invitations/pending/" + invitationId;
      bodyParameters.put("serverRootUrl", url);
      bodyParameters.put("sendInvitationUrl", url);

      // application org details
      Map appConfig = AppConfigUtil.getInstance(delegator).getAppConfiguration();
      bodyParameters.putAll(appConfig);

      Map<String, Object> emailCtx = FastMap.newInstance();
      emailCtx.put("userLogin", userLogin);
      emailCtx.put("partyId", partyInvitation.getString("partyIdFrom"));
      emailCtx.put("contentType", emailInfo.get("contentType"));
      emailCtx.put("subject", emailInfo.get("subject"));
      emailCtx.put("sendFrom", emailInfo.get("fromAddress"));
      emailCtx.put("sendCc", emailInfo.get("ccAddress"));
      emailCtx.put("sendBcc", emailInfo.get("bccAddress"));
      emailCtx.put("sendTo", (String) bodyParameters.get("inviterEmail"));
      emailCtx.put("bodyParameters", bodyParameters);
      emailCtx.put("bodyScreenUri", emailInfo.get("bodyScreenLocation"));

      Map<String, Object> sendResp = dispatcher.runSync("sendMailFromScreen", emailCtx);

      if (!ServiceUtil.isSuccess(sendResp)) {
        return ServiceUtil.returnError(
            "ProductStoreEmailSetting "
                + emailInfo.get("emailType")
                + " not found. Unable to send message.");
      }

    } catch (GenericServiceException e) {
      Debug.logError(e, e.getMessage(), module);
      return ServiceUtil.returnError(e.getMessage());
    }

    return result;
  }

  /**
   * Notify the inviter via email, if the third invitation reminder is not accepted by the invitee.
   *
   * @param dctx
   * @param context
   * @return
   * @throws GenericEntityException
   */
  public static Map<String, Object> notifyInviterAfterThirdInvitation(
      DispatchContext dctx, Map<String, Object> context) throws GenericEntityException {

    Map<String, Object> result = ServiceUtil.returnSuccess();
    Delegator delegator = dctx.getDelegator();
    LocalDispatcher dispatcher = dctx.getDispatcher();
    GenericValue userLogin = (GenericValue) context.get("userLogin");
    String invitationId = (String) context.get("invitationId");

    try {

      GenericValue partyInvitation =
          EntityQuery.use(delegator)
              .from("PartyInvitation")
              .where("partyInvitationId", invitationId)
              .queryOne();
      if (UtilValidate.isEmpty(partyInvitation)) {
        Debug.logError("No invitation found with invitationId : " + invitationId, module);
        return ServiceUtil.returnError(
            "No invitation found with id # "
                + invitationId
                + ", please provide a valid invitation id to proceed.");
      }

      GenericValue emailInfo =
          EntityQuery.use(delegator)
              .from("ProductStoreEmailSetting")
              .where("emailType", EMAIL_NTF_INVTR_AFTR_THIRD_INV_TYPE_ID)
              .cache(true)
              .queryOne();

      Map<String, Object> bodyParameters =
          getInviterAndInviteeBodyParamMap(delegator, partyInvitation);

      String serverRootUrl =
          EntityUtilProperties.getPropertyValue("appconfig", "app.domain.url", delegator);
      String url = serverRootUrl + "/admin/members/invitations/pending/" + invitationId;
      bodyParameters.put("serverRootUrl", url);
      bodyParameters.put("sendInvitationUrl", url);

      // application org details
      Map appConfig = AppConfigUtil.getInstance(delegator).getAppConfiguration();
      bodyParameters.putAll(appConfig);

      Map<String, Object> emailCtx = FastMap.newInstance();
      emailCtx.put("userLogin", userLogin);
      emailCtx.put("partyId", partyInvitation.getString("partyIdFrom"));
      emailCtx.put("contentType", emailInfo.get("contentType"));
      emailCtx.put("subject", emailInfo.get("subject"));
      emailCtx.put("sendFrom", emailInfo.get("fromAddress"));
      emailCtx.put("sendCc", emailInfo.get("ccAddress"));
      emailCtx.put("sendBcc", emailInfo.get("bccAddress"));
      emailCtx.put("sendTo", (String) bodyParameters.get("inviterEmail"));
      emailCtx.put("bodyParameters", bodyParameters);
      emailCtx.put("bodyScreenUri", emailInfo.get("bodyScreenLocation"));

      Map<String, Object> sendResp = dispatcher.runSync("sendMailFromScreen", emailCtx);

      if (!ServiceUtil.isSuccess(sendResp)) {
        return ServiceUtil.returnError(
            "ProductStoreEmailSetting "
                + emailInfo.get("emailType")
                + " not found. Unable to send email.");
      }

    } catch (GenericServiceException e) {
      Debug.logError(e, e.getMessage(), module);
      return ServiceUtil.returnError(e.getMessage());
    }

    return result;
  }

  private static Map<String, Object> getInviterAndInviteeBodyParamMap(
      Delegator delegator, GenericValue partyInvitation) {
    Map<String, Object> bodyParameter = FastMap.newInstance();
    // Invitee details, to whom invitation is sent
    String inviteePartyId = partyInvitation.getString("partyId");
    Map<String, String> inviteeNameDetails =
        AxPartyHelper.getPartyNameDetails(
            delegator, HierarchyUtils.getPartyByPartyId(delegator, inviteePartyId));
    String inviteeFirstName = inviteeNameDetails.get("firstName");
    String inviteeLastName = inviteeNameDetails.get("lastName");
    String inviteeEmail = partyInvitation.getString("emailAddress");

    // Inviter details, who initiated the invitation
    String inviterPartyId = partyInvitation.getString("partyIdFrom");
    GenericValue inviterPartyObj = HierarchyUtils.getPartyByPartyId(delegator, inviterPartyId);
    Map<String, String> inviterNameDetails =
        AxPartyHelper.getPartyNameDetails(delegator, inviterPartyObj);
    String inviterFirstName = inviterNameDetails.get("firstName");
    String inviterLastName = inviterNameDetails.get("lastName");
    GenericValue inviterEmailGv =
        AxPartyHelper.findPartyLatestEmailAddress(inviterPartyId, delegator);
    String inviterEmail = inviterEmailGv.getString("infoString");

    GenericValue inviterOrgObj = PartyGroupForPartyUtils.getPartyGroupForPartyId(inviterPartyObj);
    String inviterOrgName = "";
    if (UtilValidate.isNotEmpty(inviterOrgObj)) {
      inviterOrgName = inviterOrgObj.getString("groupName");
    }

    bodyParameter.put("email", inviteeEmail);
    bodyParameter.put("partyName", inviteeFirstName + " " + inviteeLastName);
    bodyParameter.put("inviteeName", inviteeFirstName);
    bodyParameter.put("inviteeFirstName", inviteeFirstName);
    bodyParameter.put("inviteeLastName", inviteeLastName);
    bodyParameter.put("inviteeEmail", inviteeEmail);
    bodyParameter.put("inviterFirstName", inviterFirstName);
    bodyParameter.put("inviterLastName", inviterLastName);
    bodyParameter.put("inviterEmail", inviterEmail);
    bodyParameter.put("inviterOrgName", inviterOrgName);
    return bodyParameter;
  }
}
