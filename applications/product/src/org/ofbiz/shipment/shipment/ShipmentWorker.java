/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.ofbiz.shipment.shipment;

import java.math.BigDecimal;
import java.util.List;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;

/**
 * ShipmentWorker - Worker methods for Shipment and related entities
 */
public class ShipmentWorker {

    public static final String module = ShipmentWorker.class.getName();

    /*
     * Returns the value of a given ShipmentPackageContent record.  Calculated by working out the total value (from the OrderItems) of all ItemIssuances
     * for the ShipmentItem then dividing that by the total quantity issued for the same to get an average item value then multiplying that by the package
     * content quantity.
     * Note: No rounding of the calculation is performed so you will need to round it to the accuracy that you require
     */
    public static BigDecimal getShipmentPackageContentValue(GenericValue shipmentPackageContent) {
        BigDecimal quantity = shipmentPackageContent.getBigDecimal("quantity");

        BigDecimal value = new BigDecimal("0");

        // lookup the issuance to find the order
        List<GenericValue> issuances = null;
        try {
            GenericValue shipmentItem = shipmentPackageContent.getRelatedOne("ShipmentItem");
            issuances = shipmentItem.getRelated("ItemIssuance");
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        BigDecimal totalIssued = BigDecimal.ZERO;
        BigDecimal totalValue = BigDecimal.ZERO;
        if (UtilValidate.isNotEmpty(issuances)) {
            for (GenericValue issuance : issuances) {
                // we only need one
                BigDecimal issuanceQuantity = issuance.getBigDecimal("quantity");
                BigDecimal issuanceCancelQuantity = issuance.getBigDecimal("cancelQuantity");
                if (issuanceCancelQuantity != null) {
                    issuanceQuantity = issuanceQuantity.subtract(issuanceCancelQuantity);
                }
                // get the order item
                GenericValue orderItem = null;
                try {
                    orderItem = issuance.getRelatedOne("OrderItem");
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                }

                if (orderItem != null) {
                    // get the value per unit - (base price * amount)
                    BigDecimal selectedAmount = orderItem.getBigDecimal("selectedAmount");
                    if (selectedAmount == null || selectedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                        selectedAmount = BigDecimal.ONE;
                    }

                    BigDecimal unitPrice = orderItem.getBigDecimal("unitPrice");
                    BigDecimal itemValue = unitPrice.multiply(selectedAmount);

                    // total value for package (per unit * quantity)
                    totalIssued = totalIssued.add(issuanceQuantity);
                    totalValue = totalValue.add(itemValue.multiply(issuanceQuantity));
                }
            }
        }
        // take the average value of the issuances and multiply it by the shipment package content quantity
        value = totalValue.divide(totalIssued, 10, BigDecimal.ROUND_HALF_EVEN).multiply(quantity);
        return value;
    }
}

