import org.ofbiz.base.util.UtilMisc
import org.ofbiz.entity.GenericValue
import org.ofbiz.service.ServiceUtil

def invoiceId = parameters.invoiceId;

// Get associated Order Id and then fetch Attributes for the order (invoke fetchOrderAttributes service)
List<GenericValue> associatedOrderBillings = delegator.findByAnd("OrderItemBilling", UtilMisc.toMap("invoiceId", invoiceId), null, false);
List invoiceAttributes = new ArrayList();
if(associatedOrderBillings!=null && associatedOrderBillings.size()>0) {
    GenericValue entry = associatedOrderBillings.get(0);
    def orderId = entry.getString("orderId");
    def orderAttrsResp = dispatcher.runSync("fetchOrderAttributes", UtilMisc.toMap("orderId", orderId, "userLogin", userLogin));
    if( ServiceUtil.isSuccess(orderAttrsResp) ) {
        invoiceAttributes = orderAttrsResp.get("orderAttributes");
    }
}
def serviceResponse = ServiceUtil.returnSuccess();
serviceResponse.put("invoiceAttributes", invoiceAttributes);
return serviceResponse;
