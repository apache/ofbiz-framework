import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil

Map downloadGmailSignaturesCtx = [
        userLogin: userLogin
]

Map downloadGmailSignaturesCtxResp = dispatcher.runSync("downloadGmailSignatures", downloadGmailSignaturesCtx)

if (!ServiceUtil.isSuccess(downloadGmailSignaturesCtxResp)) {
    request.setAttribute("_ERROR_MESSAGE_", ServiceUtil.getErrorMessage(downloadGmailSignaturesCtxResp))
    return "error"
}

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

return "success"

