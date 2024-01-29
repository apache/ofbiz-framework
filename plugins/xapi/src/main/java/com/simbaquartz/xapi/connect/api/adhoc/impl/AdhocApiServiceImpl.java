package com.simbaquartz.xapi.connect.api.adhoc.impl;

import com.simbaquartz.xapi.connect.api.adhoc.AdhocApiService;
import com.simbaquartz.xapi.connect.api.security.LoggedInUser;
import com.simbaquartz.xapi.connect.utils.ApiResponseUtil;
import com.fidelissd.zcp.xcommon.util.TimestampUtil;
import com.simbaquartz.xparty.services.account.AccountServices;
import com.simbaquartz.xparty.utils.PartyTypesEnum;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GenericServiceException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdhocApiServiceImpl extends AdhocApiService {
    private static final String module = AdhocApiServiceImpl.class.getName();

    @Override
    public Response addOrganizationIdToTimeEntries(SecurityContext securityContext) {
        try {
            // fetch all time-entries
            List<GenericValue> timeEntries = delegator.findAll("TimeEntry", false);

            // fetch ownerPartyId's org id from party-relationship
            for (GenericValue timeEntry : timeEntries) {
                String ownerPartyId = timeEntry.getString("ownerPartyId");

                GenericValue partyRelationshipGv = EntityQuery.use(delegator)
                        .from("PartyRelationship")
                        .where("partyIdTo", ownerPartyId, "roleTypeIdFrom", "INTERNAL_ORGANIZATIO", "roleTypeIdTo", "EMPLOYEE")
                        .queryFirst();
                if (UtilValidate.isNotEmpty(partyRelationshipGv)) {
                    String accountPartyId = partyRelationshipGv.getString("partyIdFrom");
                    timeEntry.set("organizationId", accountPartyId);
                    timeEntry.store();
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError("There was a problem adding org-id to time-entries. Error " + e.getMessage(), module);
            e.printStackTrace();
            return ApiResponseUtil.prepareOkResponse("error: " + e.getMessage());
        }
        return ApiResponseUtil.prepareOkResponse("Done");
    }

    @Override
    public Response bulkUpdateIsSyncedFlagForTimeEntries(SecurityContext securityContext) {
        try {
            // fetch all timeEntry-Event assocs
            List<GenericValue> eventAssocs = EntityQuery.use(delegator).from("EventAssociation").where("eventAssocTypeId",
                    "EVNT_ASSOC_TIME_ENTY").queryList();

            for (GenericValue eventAssoc : eventAssocs) {
                String timeEntryId = eventAssoc.getString("idFrom");
                String eventId = eventAssoc.getString("idTo");

                GenericValue eventGv = EntityQuery.use(delegator)
                        .from("Event")
                        .where("eventId", eventId)
                        .queryFirst();
                if (UtilValidate.isNotEmpty(eventGv)) {
                    String googleEventId = eventGv.getString("googleEventId");
                    if (UtilValidate.isNotEmpty(googleEventId)) {
                        GenericValue timeEntryGv = EntityQuery.use(delegator)
                                .from("TimeEntry")
                                .where("timeEntryId", timeEntryId)
                                .queryFirst();
                        if (UtilValidate.isNotEmpty(timeEntryGv)) {
                            timeEntryGv.set("isSynced", "Y");
                            timeEntryGv.store();
                        }
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError("There was a problem adding org-id to time-entries. Error " + e.getMessage(), module);
            e.printStackTrace();
            return ApiResponseUtil.prepareOkResponse("error: " + e.getMessage());
        }
        return ApiResponseUtil.prepareOkResponse("Done");
    }
}
