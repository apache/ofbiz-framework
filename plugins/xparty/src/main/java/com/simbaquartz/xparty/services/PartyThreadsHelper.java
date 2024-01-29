package com.simbaquartz.xparty.services;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;

public class PartyThreadsHelper implements Runnable {
    public static final String module = PartyThreadsHelper.class.getName();
    private final String partyId;
    private Delegator delegator;
    private GenericValue userLogin;
    private LocalDispatcher dispatcher;

    PartyThreadsHelper(String partyId, Delegator delegator, GenericValue userLogin, LocalDispatcher dispatcher) {
        this.partyId = partyId;
        this.delegator = delegator;
        this.userLogin = userLogin;
        this.dispatcher = dispatcher;
    }

    @Override
    public void run() {
        Debug.logInfo("Updating party details for party " + partyId, module);
        try {
            dispatcher.runSync("populateBasicInformationForParty", UtilMisc.toMap("partyId", partyId, "overrideExistingValues", true, "userLogin", userLogin));
        } catch (GenericServiceException e) {
            Debug.logError(e, "An error occurred while updating party details.", module);
        }

    }
}