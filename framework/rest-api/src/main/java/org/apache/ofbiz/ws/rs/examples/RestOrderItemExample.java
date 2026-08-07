package org.apache.ofbiz.ws.rs.examples;

import org.apache.ofbiz.base.model.DomainModel;

public final class RestOrderItemExample extends DomainModel {

    private String productId;
    private Integer quantity;

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
