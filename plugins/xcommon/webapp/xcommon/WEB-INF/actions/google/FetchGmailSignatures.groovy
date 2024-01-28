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

partyId = parameters.partyId;

if(UtilValidate.isEmpty(partyId)) {
    partyId = userLogin.partyId;
}

// Get Gmail signatures
Map<String, Object> getPartySignaturesResp = dispatcher.runSync("getGmailSignaturePartyContent",
        UtilMisc.toMap("userLogin", userLogin, "partyId", partyId));
if(ServiceUtil.isSuccess(getPartySignaturesResp)) {
    String signatureText = "";

    List<String> contentIds = (List<String>) getPartySignaturesResp.get("contentIds");
    if(UtilValidate.isNotEmpty(contentIds)) {
        contentId = contentIds.get(0);

        GenericValue content = delegator.findOne("Content", UtilMisc.toMap("contentId", contentId), true);
        GenericValue dataResource = delegator.getRelatedOne("DataResource", content, true);
        GenericValue electronicText = dataResource.getRelatedOne("ElectronicText", false);
        signatureText = electronicText.textData;
        context.signature = signatureText

        //update the one in session as well
        session.setAttribute("loggedInUserEmailSignature", signatureText)
    }
}

