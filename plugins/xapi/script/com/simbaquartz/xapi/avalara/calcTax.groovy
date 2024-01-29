import com.simbaquartz.xapi.helper.AvalaraTaxHelper
import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.base.util.UtilGenerics
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityUtilProperties
import org.apache.ofbiz.product.store.ProductStoreWorker
import org.apache.ofbiz.service.ServiceUtil


String isAvalaraActive = EntityUtilProperties.getPropertyValue("avalara.properties", "avalara.enable.integration", delegator)
if (!"Y".equals(isAvalaraActive)) {
    return ServiceUtil.returnError("Avalara tax integration is not enabled. Please contact support team.")
}

AvalaraTaxHelper avaTaxHelper = new AvalaraTaxHelper(dispatcher, delegator, context.userLogin, locale)

Map paramMap = prepareParamMap()

if (!avaTaxHelper.isValidShippingAddress(paramMap.shippingAddress)) {
    return ServiceUtil.returnError("Shipping address is missing or incomplete")
}
List<GenericValue> itemProductList = UtilGenerics.checkList(context.get("itemProductList"))
if (itemProductList) {
    Map requestMap = avaTaxHelper.getCalcTaxRequestMap(paramMap)
    String serviceUrl = EntityUtilProperties.getPropertyValue("avalara.properties", "avalara.tax.transaction.url", delegator)
    Map response = avaTaxHelper.invokeService(requestMap, serviceUrl)

    //handle avalara errors
    if (response.error) {
        // Return tax as zero for invalid address
        if (response.error.details) {
            List refersToList = response.error.details.refersTo
            if (refersToList) {
                for (String refersTo : refersToList) {
                    if (refersTo.contains("Addresses")) {
                        Debug.logError("############ Provided address " + refersTo +" is invalid. Hence not calculating tax and defaulting it zero", "calcTax")
                        Debug.logError("############ Error from avalara side : "+ response.error.message, "calcTax")
                        Map<String, Object> result = ServiceUtil.returnSuccess()
                        result.put("orderAdjustments", [])
                        result.put("itemAdjustments", [])
                        return result
                    }
                }
            }
        }
        return ServiceUtil.returnError(response.error.message)
    }

    //handle our side errors
    if (ServiceUtil.isError(response)) {
        return response
    }

    List taxLines = response.get("lines")
    itemTaxAdjustments = []
    orderAdjustments = []
    avaTaxHelper.getItemAndShippingTaxAdjustments(taxLines, itemTaxAdjustments, orderAdjustments, paramMap.productStoreId)

    itemTaxAdjustments.removeAll(Collections.singleton(null))
    orderAdjustments.remove(null)
    Map<String, Object> result = ServiceUtil.returnSuccess()
    result.put("orderAdjustments", orderAdjustments)
    result.put("itemAdjustments", itemTaxAdjustments)
    return result

} else {
    return ServiceUtil.returnError("No product line found to calculate tax.")
}



Map prepareParamMap(){
    final GUEST_PARTYID = "GUEST"

    Map reqParameters = [:]
    String facilityId = context.get("facilityId")
    String productStoreId = context.get("productStoreId")
    if (!productStoreId) {
        productStoreId = EntityUtilProperties.getPropertyValue("general.properties", "default.productStoreId", "9000", delegator)
    }
    if (!facilityId) {
        facilityId = ProductStoreWorker.determineSingleFacilityForStore(delegator, productStoreId)
    }

    if (!facilityId) {
        facilityId = ProductStoreWorker.determineFacilityListForStore(delegator, productStoreId)[0]
    }
    reqParameters.facilityId = facilityId
    reqParameters.billToPartyId = context.get("billToPartyId") ?: GUEST_PARTYID
    reqParameters.productStoreId = productStoreId
    reqParameters.ref1List = UtilGenerics.checkList(context.get("ref1List"))
    reqParameters.itemProductList = UtilGenerics.checkList(context.get("itemProductList"))
    reqParameters.itemPriceList = UtilGenerics.checkList(context.get("itemPriceList"))
    reqParameters.itemAmountList = UtilGenerics.checkList(context.get("itemAmountList"))
    reqParameters.itemQuantityList = UtilGenerics.checkList(context.get("itemQuantityList"))
    reqParameters.shippingAddress = (GenericValue) context.get("shippingAddress")
    reqParameters.shipFromAddress = (GenericValue) context.get("shipFromAddress")
    reqParameters.orderShippingAmount = context.orderShippingAmount
    return reqParameters
}
