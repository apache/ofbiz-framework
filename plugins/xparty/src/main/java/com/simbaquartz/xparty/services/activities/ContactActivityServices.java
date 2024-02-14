package com.simbaquartz.xparty.services.activities;

import com.fidelissd.zcp.xcommon.collections.FastList;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import com.simbaquartz.xparty.helpers.AxPartyHelper;
import com.simbaquartz.xparty.helpers.PartyContactHelper;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.party.contact.ContactHelper;
import org.apache.ofbiz.party.party.PartyHelper;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ContactActivityServices {

    public static final String module = ContactActivityServices.class.getName();

    public static Map<String, Object> getPartyActivityLogs(DispatchContext dctx, Map context) throws GenericEntityException, GenericServiceException {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();

        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String partyId = (String) context.get("partyId");
        String type = (String) context.get("activityType");
        Integer startIndex = (Integer) context.get("startIndex");
        Integer viewSize = (Integer) context.get("viewSize");
        List<Map<String, Object>> activities = FastList.newInstance();

        try {
            List<GenericValue> acts = EntityQuery.use(delegator).from("Activity").where("activityFor", partyId).orderBy("-lastUpdatedStamp").queryList();

            for (GenericValue act : acts) {

                String activityId = (String) act.get("activityId");

                Map<String, Object> readActivityCtx = FastMap.newInstance();
                readActivityCtx.put("userLogin", userLogin);
                readActivityCtx.put("activityId", activityId);
                Map readActivityResponse = FastMap.newInstance();

                try {
                    readActivityResponse = dispatcher.runSync("readActivity", readActivityCtx);

                } catch (GenericServiceException e) {
                    Debug.logError("An Exception occurred while calling the readActivity service" + e.getMessage(), module);
                }

                String description = "";
                String dataDescription = "";
                String activityType = "";
                String verb = (String) readActivityResponse.get("verb");
                String objectId = (String) readActivityResponse.get("objectId");
                String objectName = (String) readActivityResponse.get("objectName");
                String emailAddress = (String) readActivityResponse.get("emailAddress");
                String phoneNumber = (String) readActivityResponse.get("phoneNumber");
                String contactPartyName = (String) readActivityResponse.get("contactPartyName");
                String oldContactPartyName = (String) readActivityResponse.get("oldContactPartyName");
                String emailRecipients = (String) readActivityResponse.get("emailRecipients");
                String callLogSummary = (String) readActivityResponse.get("callLogSummary");

                SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

                if ("Add party email".equals(verb)) {
                    description = "added email " + objectName;
                    activityType = "email";
                } else if ("Update party email".equals(verb)) {
                    description = "updated email from " + emailAddress + " to " + objectName;
                    activityType = "email";
                } else if ("Add party phone".equals(verb)) {
                    description = "added phone " + objectName;
                    activityType = "phone";
                } else if ("Update party phone".equals(verb)) {
                    description = "updated phone from " + phoneNumber + " to " + objectName;
                    activityType = "phone";
                } else if ("Delete party phone".equals(verb)) {
                    description = "deleted phone " + objectName;
                    activityType = "phone";
                } else if ("Delete party email".equals(verb)) {
                    description = "deleted email " + objectName;
                    activityType = "email";
                } else if ("update party name".equals(verb)) {
                    description = "updated name " + objectName;
                    activityType = "personal";
                } else if ("Add note to party".equals(verb)) {
                    description = "added a note.";
                    activityType = "note";
                    dataDescription = objectName;
                } else if ("delete party note".equals(verb)) {
                    description = "deleted a note.";
                    dataDescription = objectName;
                    activityType = "note";
                } else if ("Add party about info".equals(verb)) {
                    description = "added party about info" + objectName;
                    activityType = "aboutInfo";
                } else if ("Update party about info".equals(verb)) {
                    description = "updated party about info";
                    activityType = "aboutInfo";
                } else if ("Delete party about info".equals(verb)) {
                    description = "deleted party about info";
                    activityType = "aboutInfo";
                } else if ("update party note".equals(verb)) {
                    description = "updated a note.";
                    dataDescription = objectName;
                    activityType = "note";
                } else if ("log a party call".equals(verb)) {
                    description = "logged a call with " + contactPartyName + " for " + objectName;
                    activityType = "phone";
                } else if ("call log summary".equals(verb)) {
                    description = "sent a call log summary.";
                    String sendTo = "";
                    boolean firstEmail = true;
                    if (UtilValidate.isNotEmpty(emailRecipients)) {
                        String[] recipients = emailRecipients.split(",");
                        for (String recipient : recipients) {
                            String recipientEmail = PartyContactHelper.getEmailAddressForPartyId(recipient, delegator);
                            if (UtilValidate.isNotEmpty(recipientEmail)) {
                                if (firstEmail) {
                                    firstEmail = false;
                                } else {
                                    sendTo = sendTo + ", ";
                                }
                                sendTo = sendTo + recipientEmail;
                            }
                        }
                    }
                    dataDescription = callLogSummary + "' to " + sendTo +
                            " logged for " + objectName + " with recipient " + contactPartyName;
                    activityType = "email";
                } else if ("send an email".equals(verb)) {

                    description = "sent an email.";
                    String sendTo = "";
                    boolean firstEmail = true;
                    if (UtilValidate.isNotEmpty(emailRecipients)) {
                        String[] recipients = emailRecipients.split(",");
                        for (String recipient : recipients) {
                            if (firstEmail) {
                                firstEmail = false;
                            } else {
                                sendTo = sendTo + ", ";
                            }
                            sendTo = sendTo + recipient;
                        }
                    }
                    dataDescription = "'" + objectName + "' sent to " + sendTo;
                    activityType = "email";
                } else if ("set the employer".equals(verb)) {
                    if (UtilValidate.isNotEmpty(oldContactPartyName)) {
                        description = "changed the employer from " + oldContactPartyName + " to " + contactPartyName + " for " + objectName;
                    } else {
                        description = "set the employer for " + objectName + " as " + contactPartyName;
                    }
                    activityType = "employer";
                }

                Map actor = preparePersonInfoMap((String) readActivityResponse.get("actorId"), delegator);
                Date d = null;
                String startTime = (String) readActivityResponse.get("startTime");

                if (UtilValidate.isNotEmpty(startTime)) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                        d = sdf.parse(startTime);
                    } catch (ParseException e) {
                        Debug.logError(e, "Cannot parse date string: " + startTime, module);
                    }
                }

                if (UtilValidate.isNotEmpty(description)) {
                    Map<String, Object> activity = new HashMap<String, Object>();
                    activity.put("actor", actor);
                    activity.put("description", description);
                    activity.put("dataDescription", dataDescription);
                    activity.put("startTime", UtilDateTime.toDateString(d, "EEE, MMM d yyyy, h:mm a"));
                    activity.put("startTimeTs", UtilDateTime.toTimestamp(d));
                    activity.put("activityType", activityType);
                    if (UtilValidate.isNotEmpty(type)) {
                        if (type.equals(activityType)) {
                            activities.add(activity);
                        }
                    } else {
                        activities.add(activity);
                    }

                }

            }
        } catch (GenericEntityException e) {
            e.printStackTrace();
        }

        int totalActivities = activities.size();
        int count = 0;
        List<Map<String, Object>> filteredActivities = FastList.newInstance();
        for (Map<String, Object> activity : activities) {
            if (count < startIndex) {
                count++;// skip
            } else if (count < startIndex + viewSize) {
                filteredActivities.add(activity);
                count++;
            }
        }

        serviceResult.put("contactActivities", filteredActivities);
        serviceResult.put("totalActivities", totalActivities);

        return serviceResult;
    }

    public static Map preparePersonInfoMap(String partyId, Delegator delegator) throws GenericEntityException {
        String displayName = AxPartyHelper.getPartyName(delegator, partyId);
        GenericValue party = EntityQuery.use(delegator).from("Party").where("partyId", partyId).queryOne();
        GenericValue partyEmail = EntityUtil.getFirst(ContactHelper.getContactMechByType(party, "EMAIL_ADDRESS", false));

        String email = "";
        if (UtilValidate.isNotEmpty(partyEmail)) {
            email = partyEmail.getString("infoString");
        }

        Map<String, Object> personMap = FastMap.newInstance();
        personMap.put("partyId", partyId);
        personMap.put("displayName", displayName);
        personMap.put("email", email);
        return personMap;
    }


    /**
     * Register a add email/phone party activity.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerAddPartyEmailAndPhoneActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String userLoginId = (String) userLogin.get("userLoginId");
        String partyId = (String) context.get("partyId");
        String email = (String) context.get("emailAddress");

        try {
            String partyFormattedPhoneNumber = "";
            if (UtilValidate.isEmpty(email)) {
                String contactMechId = (String) context.get("contactMechId");
                GenericValue phoneRecord = EntityQuery.use(delegator).from("TelecomNumber").where("contactMechId", contactMechId).queryOne();
                if (UtilValidate.isNotEmpty(phoneRecord)) {
                    String countryCode = phoneRecord.getString("countryCode");
                    String areaCode = phoneRecord.getString("areaCode");
                    String contactNumber = phoneRecord.getString("contactNumber");
                    partyFormattedPhoneNumber = AxPartyHelper.getFormattedPhoneNumber(delegator, contactMechId, countryCode, areaCode, contactNumber, null);

                }
            }
            Map<String, Object> registerAddPartyEmailActivityCtx = FastMap.newInstance();
            registerAddPartyEmailActivityCtx.put("userLogin", context.get("userLogin"));
            registerAddPartyEmailActivityCtx.put("actorId", userLoginId);
            registerAddPartyEmailActivityCtx.put("partyId", partyId);
            registerAddPartyEmailActivityCtx.put("objectId", partyId);
            if (UtilValidate.isNotEmpty(email)) {
                registerAddPartyEmailActivityCtx.put("verb", "Add party email");
                registerAddPartyEmailActivityCtx.put("objectName", email);
            } else {
                registerAddPartyEmailActivityCtx.put("verb", "Add party phone");
                registerAddPartyEmailActivityCtx.put("objectName", partyFormattedPhoneNumber);
            }
            registerAddPartyEmailActivityCtx.put("startTime", UtilDateTime.nowDateString("yyyy-MM-dd'T'HH:mm:ssZ"));

            Map<String, Object> registerAddPartyEmailActivityResp = dispatcher.runSync("registerActivity", registerAddPartyEmailActivityCtx);
            if (!ServiceUtil.isSuccess(registerAddPartyEmailActivityResp)) {
                return registerAddPartyEmailActivityResp;
            }

            serviceResult.put("activityId", registerAddPartyEmailActivityResp.get("activityId"));
        } catch (GenericEntityException | GenericServiceException e) {
            e.printStackTrace();
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }


    /**
     * Register a add email party activity.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerUpdatePartyEmailActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String userLoginId = (String) userLogin.get("userLoginId");
        String contactMechId = (String) context.get("oldContactMechId");
        String partyId = (String) context.get("partyId");
        String email = (String) context.get("emailAddress");
        String activityId = "";

        try {
            String oldEmailAddress = "";
            GenericValue emailRecord = EntityQuery.use(delegator).from("ContactMech").where("contactMechId", contactMechId).queryOne();
            if (UtilValidate.isNotEmpty(emailRecord)) {
                oldEmailAddress = emailRecord.getString("infoString");
            }
            if (!oldEmailAddress.equals(email)) {
                Map<String, Object> registerAddPartyEmailActivityCtx = FastMap.newInstance();
                registerAddPartyEmailActivityCtx.put("userLogin", context.get("userLogin"));
                registerAddPartyEmailActivityCtx.put("verb", "Update party email");
                registerAddPartyEmailActivityCtx.put("actorId", userLoginId);
                registerAddPartyEmailActivityCtx.put("partyId", partyId);
                registerAddPartyEmailActivityCtx.put("objectId", partyId);
                registerAddPartyEmailActivityCtx.put("objectName", email);
                registerAddPartyEmailActivityCtx.put("emailAddress", oldEmailAddress);
                registerAddPartyEmailActivityCtx.put("startTime", UtilDateTime.nowDateString("yyyy-MM-dd'T'HH:mm:ssZ"));

                Map<String, Object> registerAddPartyEmailActivityResp = dispatcher.runSync("registerActivity", registerAddPartyEmailActivityCtx);
                if (!ServiceUtil.isSuccess(registerAddPartyEmailActivityResp)) {
                    return registerAddPartyEmailActivityResp;
                }

                activityId = (String) registerAddPartyEmailActivityResp.get("activityId");
            }
        } catch (GenericEntityException | GenericServiceException e) {
            e.printStackTrace();
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        serviceResult.put("activityId", activityId);
        return serviceResult;
    }

    /**
     * Register a update party telecom activity.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerUpdatePartyPhoneActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String userLoginId = (String) userLogin.get("userLoginId");
        String contactMechId = (String) context.get("contactMechId");
        String partyId = (String) context.get("partyId");
        String oldContactMechId = (String) context.get("oldContactMechId");
        String activityId = "";

        try {

            String partyFormattedOldPhoneNumber = "";
            if (UtilValidate.isNotEmpty(oldContactMechId)) {
                GenericValue phoneRecord = EntityQuery.use(delegator).from("TelecomNumber").where("contactMechId", oldContactMechId).queryOne();
                if (UtilValidate.isNotEmpty(phoneRecord)) {
                    String countryCode = phoneRecord.getString("countryCode");
                    String areaCode = phoneRecord.getString("areaCode");
                    String contactNumber = phoneRecord.getString("contactNumber");
                    partyFormattedOldPhoneNumber = AxPartyHelper.getFormattedPhoneNumber(delegator, oldContactMechId, countryCode, areaCode, contactNumber, null);

                }
            }

            String partyFormattedNewPhoneNumber = "";
            if (UtilValidate.isNotEmpty(contactMechId)) {
                GenericValue phoneRecord = EntityQuery.use(delegator).from("TelecomNumber").where("contactMechId", contactMechId).queryOne();
                if (UtilValidate.isNotEmpty(phoneRecord)) {
                    String countryCode = phoneRecord.getString("countryCode");
                    String areaCode = phoneRecord.getString("areaCode");
                    String contactNumber = phoneRecord.getString("contactNumber");
                    partyFormattedNewPhoneNumber = AxPartyHelper.getFormattedPhoneNumber(delegator, contactMechId, countryCode, areaCode, contactNumber, null);

                }
            }
            if (!partyFormattedNewPhoneNumber.equals(partyFormattedOldPhoneNumber)) {
                Map<String, Object> registerAddPartyPhoneActivityCtx = FastMap.newInstance();
                registerAddPartyPhoneActivityCtx.put("userLogin", context.get("userLogin"));
                registerAddPartyPhoneActivityCtx.put("verb", "Update party phone");
                registerAddPartyPhoneActivityCtx.put("actorId", userLoginId);
                registerAddPartyPhoneActivityCtx.put("partyId", partyId);
                registerAddPartyPhoneActivityCtx.put("objectId", partyId);
                registerAddPartyPhoneActivityCtx.put("objectName", partyFormattedNewPhoneNumber);
                registerAddPartyPhoneActivityCtx.put("phoneNumber", partyFormattedOldPhoneNumber);
                registerAddPartyPhoneActivityCtx.put("startTime", UtilDateTime.nowDateString("yyyy-MM-dd'T'HH:mm:ssZ"));


                Map<String, Object> registerAddPartyEmailActivityResp = dispatcher.runSync("registerActivity", registerAddPartyPhoneActivityCtx);
                if (!ServiceUtil.isSuccess(registerAddPartyEmailActivityResp)) {
                    return registerAddPartyEmailActivityResp;
                }

                activityId = (String) registerAddPartyEmailActivityResp.get("activityId");
            }
        } catch (GenericEntityException | GenericServiceException e) {
            e.printStackTrace();
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        serviceResult.put("activityId", activityId);

        return serviceResult;
    }

    public static Map<String, Object> registerDeletePartyPhoneActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String userLoginId = (String) userLogin.get("userLoginId");
        String partyId = (String) context.get("partyId");
        String partyPhoneNumber = (String) context.get("partyPhoneNumber");

        try {

            Map<String, Object> registerDeletePartyPhoneActivityCtx = FastMap.newInstance();
            registerDeletePartyPhoneActivityCtx.put("userLogin", context.get("userLogin"));
            registerDeletePartyPhoneActivityCtx.put("actorId", userLoginId);
            registerDeletePartyPhoneActivityCtx.put("partyId", partyId);
            registerDeletePartyPhoneActivityCtx.put("objectId", partyId);
            registerDeletePartyPhoneActivityCtx.put("verb", "Delete party phone");
            registerDeletePartyPhoneActivityCtx.put("objectName", partyPhoneNumber);
            registerDeletePartyPhoneActivityCtx.put("startTime", UtilDateTime.nowDateString("yyyy-MM-dd'T'HH:mm:ssZ"));

            Map<String, Object> registerAddPartyEmailActivityResp = dispatcher.runSync("registerActivity", registerDeletePartyPhoneActivityCtx);
            if (!ServiceUtil.isSuccess(registerAddPartyEmailActivityResp)) {
                return registerAddPartyEmailActivityResp;
            }

            serviceResult.put("activityId", registerAddPartyEmailActivityResp.get("activityId"));
        } catch (GenericServiceException e) {
            e.printStackTrace();
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    public static Map<String, Object> registerDeletePartyEmailActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String userLoginId = (String) userLogin.get("userLoginId");
        String partyId = (String) context.get("partyId");
        String emailAddress = (String) context.get("emailAddress");

        try {

            Map<String, Object> registerDeletePartyPhoneActivityCtx = FastMap.newInstance();
            registerDeletePartyPhoneActivityCtx.put("userLogin", context.get("userLogin"));
            registerDeletePartyPhoneActivityCtx.put("actorId", userLoginId);
            registerDeletePartyPhoneActivityCtx.put("partyId", partyId);
            registerDeletePartyPhoneActivityCtx.put("objectId", partyId);
            registerDeletePartyPhoneActivityCtx.put("verb", "Delete party email");
            registerDeletePartyPhoneActivityCtx.put("objectName", emailAddress);
            registerDeletePartyPhoneActivityCtx.put("startTime", UtilDateTime.nowDateString("yyyy-MM-dd'T'HH:mm:ssZ"));

            Map<String, Object> registerAddPartyEmailActivityResp = dispatcher.runSync("registerActivity", registerDeletePartyPhoneActivityCtx);
            if (!ServiceUtil.isSuccess(registerAddPartyEmailActivityResp)) {
                return registerAddPartyEmailActivityResp;
            }

            serviceResult.put("activityId", registerAddPartyEmailActivityResp.get("activityId"));
        } catch (GenericServiceException e) {
            e.printStackTrace();
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    public static Map<String, Object> registerUpdatePersonActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String userLoginId = (String) userLogin.get("userLoginId");
        String partyId = (String) context.get("partyId");
        String partyName = PartyHelper.getPartyName(delegator, partyId, false);

        try {

            Map<String, Object> registerDeletePartyPhoneActivityCtx = FastMap.newInstance();
            registerDeletePartyPhoneActivityCtx.put("userLogin", context.get("userLogin"));
            registerDeletePartyPhoneActivityCtx.put("actorId", userLoginId);
            registerDeletePartyPhoneActivityCtx.put("partyId", partyId);
            registerDeletePartyPhoneActivityCtx.put("objectId", partyId);
            registerDeletePartyPhoneActivityCtx.put("verb", "update party name");
            registerDeletePartyPhoneActivityCtx.put("objectName", partyName);
            registerDeletePartyPhoneActivityCtx.put("startTime", UtilDateTime.nowDateString("yyyy-MM-dd'T'HH:mm:ssZ"));

            Map<String, Object> registerAddPartyEmailActivityResp = dispatcher.runSync("registerActivity", registerDeletePartyPhoneActivityCtx);
            if (!ServiceUtil.isSuccess(registerAddPartyEmailActivityResp)) {
                return registerAddPartyEmailActivityResp;
            }

            serviceResult.put("activityId", registerAddPartyEmailActivityResp.get("activityId"));
        } catch (GenericServiceException e) {
            e.printStackTrace();
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        return serviceResult;
    }

    /**
     * Register information about a note added to a party.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerAddPartyNoteActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        try {

            GenericValue userLogin = (GenericValue) context.get("userLogin");
            String userLoginId = (String) userLogin.get("userLoginId");
            String partyId = (String) userLogin.get("partyId");

            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", userLogin);
            ctx.put("verb", "Add note to party");
            ctx.put("partyId", context.get("partyId"));
            ctx.put("actorType", context.get("adderType"));
            ctx.put("actorId",  partyId);
            ctx.put("actorUrl", context.get("adderUrl"));
            ctx.put("actorName", context.get("adderName"));
            ctx.put("objectType", "Party note");
            ctx.put("startTime", UtilDateTime.nowDateString("yyyy-MM-dd'T'HH:mm:ssZ"));
            ctx.put("endTime", context.get("addedTime"));
            String noteInfo = (String) context.get("note");
            String noteId = (String) context.get("noteId");

            if (UtilValidate.isNotEmpty(noteInfo)) {
                ctx.put("objectName", noteInfo);
                ctx.put("noteInfo", noteInfo);
            } else {
                GenericValue noteData = EntityQuery.use(delegator).from("NoteData").where("noteId", noteId).queryOne();
                noteInfo = noteData.getString("noteInfo");
                ctx.put("objectName", noteInfo);
                ctx.put("noteName", noteInfo);
            }

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }
            serviceResult.put("activityId", resp.get("activityId"));

        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        return serviceResult;
    }

    public static Map<String, Object> registerDeletePartyNoteActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        try {

            GenericValue userLogin = (GenericValue) context.get("userLogin");
            String userLoginId = (String) userLogin.get("userLoginId");
            String partyId = (String) userLogin.get("partyId");

            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", userLogin);
            ctx.put("verb", "delete party note");
            ctx.put("partyId", context.get("partyId"));
            ctx.put("actorType", context.get("adderType"));
            ctx.put("actorId",  partyId);
            ctx.put("actorUrl", context.get("adderUrl"));
            ctx.put("actorName", context.get("adderName"));
            ctx.put("objectType", "Party note");
            ctx.put("startTime", UtilDateTime.nowDateString("yyyy-MM-dd'T'HH:mm:ssZ"));
            ctx.put("endTime", context.get("addedTime"));
            String noteInfo = (String) context.get("noteInfo");

            if (UtilValidate.isNotEmpty(noteInfo)) {
                ctx.put("objectName", noteInfo);
                ctx.put("noteInfo", noteInfo);
            }

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                Debug.logError("An error occured in service registerActivity", module);
                return resp;
            }
            serviceResult.put("activityId", resp.get("activityId"));

        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        return serviceResult;
    }

    public static Map<String, Object> registerUpdatePartyNoteActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        try {

            GenericValue userLogin = (GenericValue) context.get("userLogin");
            String userLoginId = (String) userLogin.get("userLoginId");
            String partyId = (String) userLogin.get("partyId");

            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", userLogin);
            ctx.put("verb", "update party note");
            ctx.put("partyId", context.get("partyId"));
            ctx.put("actorType", context.get("adderType"));
            ctx.put("actorId",  partyId);
            ctx.put("actorUrl", context.get("adderUrl"));
            ctx.put("actorName", context.get("adderName"));
            ctx.put("objectType", "Party note");
            ctx.put("startTime", UtilDateTime.nowDateString("yyyy-MM-dd'T'HH:mm:ssZ"));
            ctx.put("endTime", context.get("addedTime"));
            String noteInfo = (String) context.get("noteInfo");

            if (UtilValidate.isNotEmpty(noteInfo)) {
                ctx.put("objectName", noteInfo);
                ctx.put("noteInfo", noteInfo);
            }

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                Debug.logError("An error occured in service registerActivity", module);
                return resp;
            }
            serviceResult.put("activityId", resp.get("activityId"));

        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        return serviceResult;
    }

    /**
     * Register create call log activity.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerCreateCallLogActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        try {
            GenericValue userLogin = (GenericValue) context.get("userLogin");
            String userLoginId = (String) userLogin.get("userLoginId");
            String partyIdFrom = (String) context.get("partyIdFrom");
            String partyIdTo = (String) context.get("partyIdTo");
            String callLogDuration = (String) context.get("callLogDuration");
            String opportunityId =(String) context.get("opportunityId");
            String displayName = "";

            if (UtilValidate.isNotEmpty(partyIdFrom))
                displayName = AxPartyHelper.getPartyName(delegator, partyIdFrom);

            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", userLogin);
            ctx.put("verb", "log a party call");
            if(UtilValidate.isNotEmpty(opportunityId)){
                ctx.put("partyId", opportunityId);
            }else{
                ctx.put("partyId", partyIdFrom);
            }
            ctx.put("actorType", context.get("adderType"));
            ctx.put("actorId", partyIdTo);
            ctx.put("actorUrl", context.get("adderUrl"));
            ctx.put("actorName", context.get("adderName"));
            ctx.put("objectType", "Call log");
            ctx.put("startTime", UtilDateTime.nowDateString("yyyy-MM-dd'T'HH:mm:ssZ"));
            ctx.put("endTime", context.get("addedTime"));
            ctx.put("objectName", callLogDuration);
            ctx.put("contactPartyName", displayName);
            ctx.put("objectId", partyIdTo);

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                Debug.logError("An error occured in service registerActivity", module);
                return resp;
            }
            serviceResult.put("activityId", resp.get("activityId"));

        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        return serviceResult;
    }

    /**
     * Register set party employer activity.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerSetEmployerActivity(DispatchContext dctx, Map<String, Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        try {
            GenericValue userLogin = (GenericValue) context.get("userLogin");
            String userLoginId = (String) userLogin.get("partyId");
            String employerPartyId = (String) context.get("employerPartyId");
            String employerName = (String) context.get("employerName");
            String partyId = (String) context.get("partyId");
            String displayName = "";
            if (UtilValidate.isNotEmpty(partyId))
                displayName = AxPartyHelper.getPartyName(delegator, partyId);

            String companyName = "";
            if (UtilValidate.isNotEmpty(employerName)) {
                companyName = employerName;
            } else if (UtilValidate.isNotEmpty(employerPartyId)) {
                companyName = AxPartyHelper.getPartyName(delegator, employerPartyId);
            }
            Map<String, Object> ctx = FastMap.newInstance();
            ctx.put("userLogin", userLogin);
            ctx.put("verb", "set the employer");
            ctx.put("partyId", partyId);
            ctx.put("actorType", context.get("adderType"));
            ctx.put("actorId", userLoginId);
            ctx.put("actorUrl", context.get("adderUrl"));
            ctx.put("actorName", context.get("adderName"));
            ctx.put("objectType", "Employer");
            ctx.put("startTime", UtilDateTime.nowDateString("yyyy-MM-dd'T'HH:mm:ssZ"));
            ctx.put("endTime", context.get("addedTime"));
            ctx.put("objectName", displayName);
            ctx.put("contactPartyName", companyName);
            ctx.put("objectId", userLoginId);

            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                Debug.logError("An error occurred in service registerActivity", module);
                return resp;
            }
            serviceResult.put("activityId", resp.get("activityId"));

        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        return serviceResult;
    }

    /**
     * Register information about party create, update or delete.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> registerPartyAboutInfoActivity(DispatchContext dctx, Map<String, Object> context) {

        if (Debug.verboseOn()) {
            Debug.logVerbose("Entering service method registerPartyAboutInfoActivity.", module);
        }

        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        try {

            GenericValue userLogin = (GenericValue) context.get("userLogin");
            String actorPartyId = (String) userLogin.get("partyId");

            Map<String, Object> ctx = new HashMap<>();
            ctx.put("userLogin", userLogin);
            ctx.put("partyId", context.get("partyId"));
            ctx.put("actorType", context.get("adderType"));
            ctx.put("actorId", actorPartyId);
            ctx.put("actorUrl", context.get("adderUrl"));
            ctx.put("actorName", context.get("adderName"));
            ctx.put("objectType", "Party about info");
            ctx.put("startTime", UtilDateTime.nowDateString("yyyy-MM-dd'T'HH:mm:ssZ"));
            ctx.put("endTime", context.get("addedTime"));

            String operation = (String) context.get("operation");
            if ("create".equalsIgnoreCase(operation)) {
                ctx.put("verb", "Add party about info");
                String aboutText = (String) context.get("aboutText");
                ctx.put("objectName", aboutText);
            } else if ("update".equalsIgnoreCase(operation)) {
                ctx.put("verb", "Update party about info");
                String aboutText = (String) context.get("aboutText");
                ctx.put("objectName", aboutText);

            } else if ("delete".equalsIgnoreCase(operation)) {
                ctx.put("verb", "Delete party about info");

            }
            Map<String, Object> resp = dispatcher.runSync("registerActivity", ctx);
            if (!ServiceUtil.isSuccess(resp)) {
                return resp;
            }
            serviceResult.put("activityId", resp.get("activityId"));


        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        if (Debug.verboseOn()) {
            Debug.logVerbose("Exit service method registerPartyAboutInfoActivity.", module);
        }
        return serviceResult;
    }

}
