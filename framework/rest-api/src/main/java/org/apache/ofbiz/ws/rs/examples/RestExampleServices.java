package org.apache.ofbiz.ws.rs.examples;

import java.util.Map;

import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

public class RestExampleServices {
    public static Map<String, Object> createRestOrderExample(DispatchContext dctx, Map<String, Object> context) {
        RestOrderExample order = (RestOrderExample) context.get("Order");
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("orderId", order.getOrderId());
        return result;
    }
}
