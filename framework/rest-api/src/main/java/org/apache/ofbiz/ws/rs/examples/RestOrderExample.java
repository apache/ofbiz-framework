package org.apache.ofbiz.ws.rs.examples;

import java.util.List;

import org.apache.ofbiz.base.model.DomainModel;

public final class RestOrderExample extends DomainModel {

    private String orderId;
    private String customerName;
    private List<RestOrderItemExample> items;

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }


    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public List<RestOrderItemExample> getItems() {
        return items;
    }

    public void setItems(List<RestOrderItemExample> items) {
        this.items = items;
    }
}
