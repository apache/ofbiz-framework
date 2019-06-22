import org.apache.ofbiz.base.util.UtilDateTime;

if (parameters.orderId) {
    orderItems = from("OrderItem").where("orderId", parameters.orderId).queryList();
    context.orderItems = orderItems;
    context.orderContentTypes = from("OrderContentType").queryList();
    context.fromDate = UtilDateTime.nowTimestamp();
}
