/*
 * *****************************************************************************************
 *  * Copyright (c) Fidelis Sustainability Distribution, LLC 2015. - All Rights Reserved     *
 *  * Unauthorized copying of this file, via any medium is strictly prohibited               *
 *  * Proprietary and confidential                                                           *
 *  * Written by Forrest Rae <forrest.rae@fidelissd.com>, August, 2016                       *
 *  *****************************************************************************************
 */


import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil

partyId = parameters.partyId

if(UtilValidate.isEmpty(partyId)) {
    partyId = userLogin.partyId
}
if(UtilValidate.isNotEmpty(partyId)){
    // Check if connected with Google
    Map<String, Object> resp = dispatcher.runSync("checkIfGoogleAuthorized",
            UtilMisc.toMap("userLogin", userLogin, "partyId", partyId))

    if(ServiceUtil.isSuccess(resp)) {
        boolean isGoogleAuthorized = resp.get("isAuthorized")
        context.isGoogleAuthorized = isGoogleAuthorized
        session.setAttribute("isMailAccountConnected", isGoogleAuthorized)
        String loggedInUserConnectedMailboxEmailId = resp.get("emailAddress")
        context.emailAddress = loggedInUserConnectedMailboxEmailId

        //make sure value is available in session as well
        if( UtilValidate.isNotEmpty(loggedInUserConnectedMailboxEmailId) ){
            session.setAttribute("isMailAccountConnected", isGoogleAuthorized)
            session.setAttribute("loggedInUserConnectedMailboxEmailId", loggedInUserConnectedMailboxEmailId)

            // Get Gmail signatures
            Map<String, Object> getPartySignaturesResp = dispatcher.runSync("getGmailSignaturePartyContent",
                    UtilMisc.toMap("userLogin", userLogin, "partyId", userLogin.partyId))
            if(ServiceUtil.isSuccess(getPartySignaturesResp)) {
                String signatureText = ""

                List<String> contentIds = (List<String>) getPartySignaturesResp.get("contentIds")
                if(UtilValidate.isNotEmpty(contentIds)) {
                    contentId = contentIds.get(0)

                    GenericValue content = delegator.findOne("Content", UtilMisc.toMap("contentId", contentId), true)
                    GenericValue dataResource = delegator.getRelatedOne("DataResource", content, true)
                    GenericValue electronicText = dataResource.getRelatedOne("ElectronicText", false)
                    signatureText = electronicText.textData
                    context.signatureText = signatureText
                    session.setAttribute("loggedInUserEmailSignature", signatureText)
                }
            }

            Map <String, Object> allParties = dispatcher.runSync("getAllPartiesFromEmailList", UtilMisc.toMap("emails", UtilMisc.toList(loggedInUserConnectedMailboxEmailId)));
            def parties = (List <GenericValue>) allParties.get("parties");
            if(UtilValidate.isNotEmpty(parties)) {
                GenericValue connectedParty = parties.get(0);
                String connectedPartyId = connectedParty.partyId;
                List<GenericValue> latestCommunicationEvents = from("CommunicationEventAndRole").where(["partyId" : connectedPartyId]).orderBy("-datetimeStarted").maxRows(5).queryList()
                context.latestCommunicationEvents=latestCommunicationEvents
            }
        }
    }else {
        context.isGoogleAuthorized = false
        //remove values from session as well just to be sure
        session.setAttribute("isMailAccountConnected", false)
    }

}
