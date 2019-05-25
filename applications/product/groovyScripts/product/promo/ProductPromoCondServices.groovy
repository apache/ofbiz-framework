/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License") you may not use this file except in compliance
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
 */

import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.order.shoppingcart.ShoppingCart
import org.apache.ofbiz.order.shoppingcart.ShoppingCartItem
import org.apache.ofbiz.order.shoppingcart.product.ProductPromoWorker
import org.apache.ofbiz.service.GenericServiceException
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.calendar.RecurrenceInfo
import org.apache.ofbiz.service.calendar.RecurrenceInfoException

import java.math.MathContext
import java.math.RoundingMode
import java.sql.Timestamp

/*
 * ================================================================
 * ProductPromoCond Services
 * ================================================================
 */

/**
 * This function return success if the conditions have been met for the product amount
 * @return result
 */
def productAmount() {
    Map result = success()
    int compareBase = -1
    result.operatorEnumId = "PPC_EQ"

    GenericValue productPromoCond = parameters.productPromoCond
    ShoppingCart cart = parameters.shoppingCart
    String condValue = productPromoCond.condValue

    // this type of condition requires items involved to not be involved in any other quantity consuming cond/action, and does not pro-rate the price, just uses the base price
    BigDecimal amountNeeded = BigDecimal.ZERO
    if (condValue) {
        amountNeeded = new BigDecimal(condValue)
    }

    Set<String> productIds = ProductPromoWorker.getPromoRuleCondProductIds(productPromoCond, delegator, nowTimestamp)

    List<ShoppingCartItem> lineOrderedByBasePriceList = cart.getLineListOrderedByBasePrice(false)
    Iterator<ShoppingCartItem> lineOrderedByBasePriceIter = lineOrderedByBasePriceList.iterator()
    while (amountNeeded.compareTo(BigDecimal.ZERO) > 0 && lineOrderedByBasePriceIter.hasNext()) {
        ShoppingCartItem cartItem = lineOrderedByBasePriceIter.next()
        // only include if it is in the productId Set for this check and if it is not a Promo (GWP) item
        GenericValue product = cartItem.getProduct()
        String parentProductId = cartItem.getParentProductId()
        boolean passedItemConds = ProductPromoWorker.checkConditionsForItem(productPromoCond, cart, cartItem, delegator, dispatcher, nowTimestamp)
        boolean checkAmount = passedItemConds && !cartItem.getIsPromo() &&
                (productIds.contains(cartItem.getProductId()) || (parentProductId && productIds.contains(parentProductId))) &&
                (!product || !product.includeInPromotions || 'N' != product.includeInPromotions)
        if (checkAmount) {
            MathContext generalRounding = new MathContext(10)
            BigDecimal basePrice = cartItem.getBasePrice()
            // get a rough price, round it up to an integer
            BigDecimal quantityNeeded = amountNeeded.divide(basePrice, generalRounding).setScale(0, RoundingMode.CEILING)

            // reduce amount still needed to qualify for promo (amountNeeded)
            BigDecimal quantity = cartItem.addPromoQuantityCandidateUse(quantityNeeded, productPromoCond, false)
            // get pro-rated amount based on discount
            amountNeeded = amountNeeded.subtract(quantity.multiply(basePrice))
        }
    }

    // if amountNeeded > 0 then the promo condition failed, so remove candidate promo uses and increment the promoQuantityUsed to restore it
    if (amountNeeded.compareTo(BigDecimal.ZERO) > 0) {
        // failed, reset the entire rule, ie including all other conditions that might have been done before
        cart.resetPromoRuleUse(productPromoCond.productPromoId, productPromoCond.productPromoRuleId)
        compareBase = -1
    } else {
        // we got it, the conditions are in place...
        compareBase = 0
        // NOTE: don't confirm promo rule use here, wait until actions are complete for the rule to do that
    }
    result.compareBase = compareBase
    return result
}

/**
 * This function return success if the conditions have been met for the product total
 * @return result
 */
def productTotal() {
    Map result = success()
    int compareBase = -1

    GenericValue productPromoCond = parameters.productPromoCond
    ShoppingCart cart = parameters.shoppingCart
    String condValue = productPromoCond.condValue
    Timestamp nowTimestamp = UtilDateTime.nowTimestamp()

    if (condValue) {
        BigDecimal amountNeeded = new BigDecimal(condValue)
        BigDecimal amountAvailable = BigDecimal.ZERO

        Set<String> productIds = ProductPromoWorker.getPromoRuleCondProductIds(productPromoCond, delegator, nowTimestamp)

        List<ShoppingCartItem> lineOrderedByBasePriceList = cart.getLineListOrderedByBasePrice(false)
        for (ShoppingCartItem cartItem : lineOrderedByBasePriceList) {
            // only include if it is in the productId Set for this check and if it is not a Promo (GWP) item
            GenericValue product = cartItem.getProduct()
            String parentProductId = cartItem.getParentProductId()
            boolean passedItemConds = ProductPromoWorker.checkConditionsForItem(productPromoCond, cart, cartItem, delegator, dispatcher, nowTimestamp)
            if (passedItemConds && !cartItem.getIsPromo() 
                    && (productIds.contains(cartItem.getProductId()) || (parentProductId && productIds.contains(parentProductId)))
                    && (!product || 'N' != product.includeInPromotions)) {

                // just count the entire sub-total of the item
                amountAvailable = amountAvailable.add(cartItem.getItemSubTotal())
            }
        }
        compareBase = amountAvailable.compareTo(amountNeeded)
    }
    result.compareBase = Integer.valueOf(compareBase)
    return result
}

/**
 * This function return success if the conditions have been met for the product quantity
 * @return result
 */
def productQuant() {
    Map result = success()

    GenericValue productPromoCond = parameters.productPromoCond
    String operatorEnumId = productPromoCond.operatorEnumId
    ShoppingCart cart = parameters.shoppingCart
    String condValue = productPromoCond.condValue
    Timestamp nowTimestamp = UtilDateTime.nowTimestamp()

    if (!operatorEnumId) {
        // if the operator is not specified in the condition, then assume as default PPC_EQ (for backward compatibility)
        operatorEnumId = "PPC_EQ"
    }
    BigDecimal quantityNeeded = BigDecimal.ONE
    if (condValue) {
        quantityNeeded = new BigDecimal(condValue)
    }

    Set<String> productIds = ProductPromoWorker.getPromoRuleCondProductIds(productPromoCond, delegator, nowTimestamp)

    List<ShoppingCartItem> lineOrderedByBasePriceList = cart.getLineListOrderedByBasePrice(false)
    Iterator<ShoppingCartItem> lineOrderedByBasePriceIter = lineOrderedByBasePriceList.iterator()
    while (quantityNeeded.compareTo(BigDecimal.ZERO) > 0 && lineOrderedByBasePriceIter.hasNext()) {
        ShoppingCartItem cartItem = lineOrderedByBasePriceIter.next()
        // only include if it is in the productId Set for this check and if it is not a Promo (GWP) item
        GenericValue product = cartItem.getProduct()
        String parentProductId = cartItem.getParentProductId()
        boolean passedItemConds = ProductPromoWorker.checkConditionsForItem(productPromoCond, cart, cartItem, delegator, dispatcher, nowTimestamp)
        if (passedItemConds && !cartItem.getIsPromo() &&
                (productIds.contains(cartItem.getProductId()) || (parentProductId && productIds.contains(parentProductId))) &&
                (!product || 'N' != product.includeInPromotions)) {
            // reduce quantity still needed to qualify for promo (quantityNeeded)
            quantityNeeded = quantityNeeded.subtract(cartItem.addPromoQuantityCandidateUse(quantityNeeded, productPromoCond, "PPC_EQ" != operatorEnumId))
        }
    }

    // if quantityNeeded > 0 then the promo condition failed, so remove candidate promo uses and increment the promoQuantityUsed to restore it
    if (quantityNeeded.compareTo(BigDecimal.ZERO) > 0) {
        // failed, reset the entire rule, ie including all other conditions that might have been done before
        cart.resetPromoRuleUse(productPromoCond.productPromoId, productPromoCond.productPromoRuleId)
        compareBase = -1
    } else {
        // we got it, the conditions are in place...
        compareBase = 0
        // NOTE: don't confirm rpomo rule use here, wait until actions are complete for the rule to do that
    }
    result.compareBase = Integer.valueOf(compareBase)
    return result
}

/**
 * This function return success if the conditions have been met for new accounts
 * @return result
 */
def productNewACCT() {
    // promotion description="Account Days Since Created"
    Map result = success()

    GenericValue productPromoCond = parameters.productPromoCond
    ShoppingCart cart = parameters.shoppingCart
    String condValue = productPromoCond.condValue
    Timestamp nowTimestamp = UtilDateTime.nowTimestamp()
    int compareBase = -1
    if (condValue) {
        BigDecimal acctDays = cart.getPartyDaysSinceCreated(nowTimestamp)
        if (acctDays) {
            compareBase = acctDays.compareTo(new BigDecimal(condValue))
        }
    }
    result.compareBase = Integer.valueOf(compareBase)
    return result
}

/**
 * This function return success if the conditions have been met for the party ID
 * @return result
 */
def productPartyID() {
    Map result = success()

    Map productPromoCond = parameters.productPromoCond
    ShoppingCart cart = parameters.shoppingCart
    String condValue = productPromoCond.condValue
    String partyId = cart.getPartyId()
    int compareBase = 1
    if (partyId && condValue) {
        compareBase = partyId.compareTo(condValue)
    }
    result.compareBase = Integer.valueOf(compareBase)
    return result
}

/**
 * This function return success if the conditions have been met for the party group member
 * @return result
 */
def productPartyGM() {
    Map result = success()

    GenericValue productPromoCond = parameters.productPromoCond
    ShoppingCart cart = parameters.shoppingCart
    String condValue = productPromoCond.condValue
    String partyId = cart.getPartyId()

    int compareBase = 1
    if (partyId && condValue) {
        String groupPartyId = condValue
        if (partyId == groupPartyId) {
            compareBase = 0
        } else {
            // look for PartyRelationship with partyRelationshipTypeId=GROUP_ROLLUP, the partyIdTo is the group member, so the partyIdFrom is the groupPartyId
            // and from/thru date within range
            List<GenericValue> partyRelationshipList = from("PartyRelationship").where("partyIdFrom", groupPartyId, "partyIdTo", partyId, "partyRelationshipTypeId", "GROUP_ROLLUP").cache(true).filterByDate().queryList()

            if (partyRelationshipList) {
                compareBase = 0
            } else {
                compareBase = ProductPromoWorker.checkConditionPartyHierarchy(delegator, nowTimestamp, groupPartyId, partyId)
            }
        }
    }
    result.compareBase = Integer.valueOf(compareBase)
    return result
}

/**
 * This function return success if the  conditions have been met for the party class
 * @return result
 */
def productPartyClass() {
    Map result = success()

    GenericValue productPromoCond = parameters.productPromoCond
    ShoppingCart cart = parameters.shoppingCart
    String condValue = productPromoCond.condValue
    String partyId = cart.getPartyId()

    int compareBase = 1
    if (partyId && condValue) {
        String partyClassificationGroupId = condValue
        // find any PartyClassification
        // and from/thru date within range
        List<GenericValue> partyClassificationList = from("PartyClassification").where("partyId", partyId, "partyClassificationGroupId", partyClassificationGroupId).cache(true).filterByDate().queryList()
        // then 0 (equals), otherwise 1 (not equals)
        compareBase = partyClassificationList? 0: 1
    }
    result.compareBase = Integer.valueOf(compareBase)
    return result
}

/**
 * This function return success if the conditions have been met for the role type
 * @return result
 */
def productRoleType() {
    Map result = success()

    GenericValue productPromoCond = parameters.productPromoCond
    ShoppingCart cart = parameters.shoppingCart
    String condValue = productPromoCond.condValue
    String partyId = cart.getPartyId()

    int compareBase = 1
    if (partyId && condValue) {
        // if a PartyRole exists for this partyId and the specified roleTypeId
        GenericValue partyRole = from("PartyRole").where("partyId", partyId, "roleTypeId", condValue).cache(true).queryOne()
        // then 0 (equals), otherwise 1 (not equals)
        compareBase = partyRole? 0: 1
    }
    result.compareBase = Integer.valueOf(compareBase)
    return result
}

/**
 * This function return success if the conditions have been met for the shipping destination
 * @return result
 */
def productGeoID() {
    Map result = success()

    GenericValue productPromoCond = parameters.productPromoCond
    String condValue = productPromoCond.condValue
    ShoppingCart cart = parameters.shoppingCart
    GenericValue shippingAddress = cart.getShippingAddress()

    int compareBase = 1
    if (condValue && shippingAddress) {
        if (condValue == shippingAddress.countryGeoId
                || condValue == shippingAddress.countyGeoId
                || condValue == shippingAddress.postalCodeGeoId
                || condValue == shippingAddress.stateProvinceGeoId) {
            compareBase = 0
        } else {
            List<GenericValue> geoAssocList = from("GeoAssoc").where("geoIdTo", condValue).queryList()
            for (GenericValue geo : geoAssocList) {
                if (geo.geoId == shippingAddress.countryGeoId
                        || geo.geoId == shippingAddress.countyGeoId
                        || geo.geoId == shippingAddress.postalCodeGeoId
                        || geo.geoId == shippingAddress.stateProvinceGeoId) {
                    compareBase = 0
                    break
                }
            }
        }
    }
    result.compareBase = Integer.valueOf(compareBase)
    return result
}

/**
 * This function return success if the conditions have been met for the product order total
 * @return result
 */
def productOrderTotal() {
    Map result = success()
    int compareBase = 1

    GenericValue productPromoCond = parameters.productPromoCond
    ShoppingCart cart = parameters.shoppingCart
    String condValue = productPromoCond.condValue

    if (condValue) {
        BigDecimal orderSubTotal = cart.getSubTotalForPromotions()
        if (Debug.infoOn()) Debug.logInfo("Doing order total compare: orderSubTotal=" + orderSubTotal, module)
        compareBase = orderSubTotal.compareTo(new BigDecimal(condValue))
    }
    result.compareBase = Integer.valueOf(compareBase)
    return result
}

/**
 * This function return success if the conditions have been met for the product order sub-total X in last Y Months
 * @return result
 */
def productOrderHist() {
    // description="Order sub-total X in last Y Months"
    GenericValue productPromoCond = parameters.productPromoCond
    ShoppingCart cart = parameters.shoppingCart
    String condValue = productPromoCond.condValue
    String otherValue = productPromoCond.otherValue
    String partyId = cart.getPartyId()
    GenericValue userLogin = cart.getUserLogin()
    Map result = success()
    int compareBase = -1
    result.operatorEnumId = "PPC_GTE"

    if (partyId && userLogin && condValue) {
        // call the getOrderedSummaryInformation service to get the sub-total
        int monthsToInclude = 12
        if (otherValue != null) {
            monthsToInclude = Integer.parseInt(otherValue)
        }
        Map<String, Object> serviceIn = [partyId: partyId, roleTypeId: "PLACING_CUSTOMER", orderTypeId: "SALES_ORDER", statusId: "ORDER_COMPLETED",
                                         monthsToInclude: Integer.valueOf(monthsToInclude), userLogin: userLogin]
        try {
            Map<String, Object> serviceResult = run service: "getOrderedSummaryInformation", with: serviceIn
            if (ServiceUtil.isError(serviceResult)) {
                Debug.logError("Error calling getOrderedSummaryInformation service for the PPIP_ORST_HIST ProductPromo condition input value: " + ServiceUtil.getErrorMessage(result), module)
                return serviceResult
            } else {
                BigDecimal orderSubTotal = (BigDecimal) serviceResult.get("totalSubRemainingAmount")
                BigDecimal orderSubTotalAndCartSubTotal = orderSubTotal.add(cart.getSubTotal())
                if (Debug.verboseOn()) Debug.logVerbose("Doing order history sub-total compare: orderSubTotal=" + orderSubTotal + ", for the last " + monthsToInclude + " months.", module)
                compareBase = orderSubTotalAndCartSubTotal.compareTo(new BigDecimal(condValue))
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error getting order history sub-total in the getOrderedSummaryInformation service, evaluating condition to false.", module)
            return ServiceUtil.returnError("Error getting order history")
        }
    }
    result.compareBase = compareBase
    return result
}

/**
 * This function return success if the conditions have been met for the product order of the current year
 * @return result
 */
def productOrderYear() {
    Map result = success()
    compareBase = 1

    GenericValue productPromoCond = parameters.productPromoCond
    ShoppingCart cart = parameters.shoppingCart
    String condValue = productPromoCond.condValue
    GenericValue userLogin = cart.getUserLogin()
    String partyId = cart.getPartyId()

    // description="Order sub-total X since beginning of current year"
    if (partyId && userLogin  && condValue) {
        // call the getOrderedSummaryInformation service to get the sub-total
        Calendar calendar = Calendar.getInstance()
        calendar.setTime(nowTimestamp)
        int monthsToInclude = calendar.get(Calendar.MONTH) + 1
        Map<String, Object> serviceIn = UtilMisc.<String, Object> toMap("partyId", partyId,
                "roleTypeId", "PLACING_CUSTOMER",
                "orderTypeId", "SALES_ORDER",
                "statusId", "ORDER_COMPLETED",
                "monthsToInclude", Integer.valueOf(monthsToInclude),
                "userLogin", userLogin)
        try {
            Map<String, Object> serviceResult = dispatcher.runSync("getOrderedSummaryInformation", serviceIn)
            if (ServiceUtil.isError(result)) {
                Debug.logError("Error calling getOrderedSummaryInformation service for the PPIP_ORST_YEAR ProductPromo condition input value: " + ServiceUtil.getErrorMessage(result), module)
                return serviceResult
            } else {
                BigDecimal orderSubTotal = (BigDecimal) result.get("totalSubRemainingAmount")
                if (Debug.verboseOn()) Debug.logVerbose("Doing order history sub-total compare: orderSubTotal=" + orderSubTotal + ", for the last " + monthsToInclude + " months.", module)
                compareBase = orderSubTotal.compareTo(new BigDecimal((condValue)))

            }
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error getting order history sub-total in the getOrderedSummaryInformation service, evaluating condition to false.", module)
            return ServiceUtil.returnError("Error getting order history")
        }
    }
    result.compareBase = Integer.valueOf(compareBase)
    return result
}

/**
 * This function return success if the conditions have been met for the product order last year
 * @return result
 */
def productOrderLastYear() {
    // description="Order sub-total X since beginning of last year"
    Map result = success()
    compareBase = 1

    GenericValue productPromoCond = parameters.productPromoCond
    ShoppingCart cart = parameters.shoppingCart
    String condValue = productPromoCond.condValue
    GenericValue userLogin = cart.getUserLogin()
    String partyId = cart.getPartyId()

    if (partyId && userLogin  && condValue) {
        // call the getOrderedSummaryInformation service to get the sub-total

        Calendar calendar = Calendar.getInstance()
        calendar.setTime(nowTimestamp)
        int lastYear = calendar.get(Calendar.YEAR) - 1
        Calendar fromDateCalendar = Calendar.getInstance()
        fromDateCalendar.set(lastYear, 0, 0, 0, 0)
        Timestamp fromDate = new Timestamp(fromDateCalendar.getTime().getTime())
        Calendar thruDateCalendar = Calendar.getInstance()
        thruDateCalendar.set(lastYear, 12, 0, 0, 0)
        Timestamp thruDate = new Timestamp(thruDateCalendar.getTime().getTime())
        Map<String, Object> serviceIn = UtilMisc.toMap("partyId", partyId,
                "roleTypeId", "PLACING_CUSTOMER",
                "orderTypeId", "SALES_ORDER",
                "statusId", "ORDER_COMPLETED",
                "fromDate", fromDate,
                "thruDate", thruDate,
                "userLogin", userLogin)
        try {
            Map<String, Object> serviceResult = dispatcher.runSync("getOrderedSummaryInformation", serviceIn)
            if (ServiceUtil.isError(serviceResult)) {
                Debug.logError("Error calling getOrderedSummaryInformation service for the PPIP_ORST_LAST_YEAR ProductPromo condition input value: " + ServiceUtil.getErrorMessage(result), module)
                return serviceResult
            } else {
                Double orderSubTotal = (Double) result.get("totalSubRemainingAmount")
                if (Debug.verboseOn()) Debug.logVerbose("Doing order history sub-total compare: orderSubTotal=" + orderSubTotal + ", for last year.", module)
                compareBase = orderSubTotal.compareTo(Double.valueOf(condValue))
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error getting order history sub-total in the getOrderedSummaryInformation service, evaluating condition to false.", module)
            return ServiceUtil.returnError("Error getting order history")
        }
    }
    result.compareBase = Integer.valueOf(compareBase)
    return result
}

/**
 * This function return success if the conditions have been met for the product promo recurrence
 * @return result
 */
def productPromoRecurrence() {
    Map result = success()
    int compareBase = 1
    GenericValue productPromoCond = parameters.productPromoCond
    String condValue = productPromoCond.condValue

    if (condValue) {
        GenericValue recurrenceInfo = from("RecurrenceInfo").where("recurrenceInfoId", condValue).cache().queryOne();
        if (recurrenceInfo) {
            RecurrenceInfo recurrence = null
            try {
                recurrence = new RecurrenceInfo(recurrenceInfo)
            } catch (RecurrenceInfoException e) {
                Debug.logError(e, module)
            }

            // check the current recurrence
            if (recurrence && recurrence.isValidCurrent()) {
                compareBase = 0
            }
        }
    }

    result.compareBase = Integer.valueOf(compareBase)
    return result
}

/**
 * This function return success if the conditions have been met for the product total shipping
 * @return result
 */
def productShipTotal() {
    Map result = success()
    compareBase = 1

    GenericValue productPromoCond = parameters.productPromoCond
    String condValue = productPromoCond.condValue
    ShoppingCart cart = parameters.shoppingCart

    if (condValue) {
        BigDecimal orderTotalShipping = cart.getTotalShipping()
        if (Debug.verboseOn()) {
            Debug.logVerbose("Doing order total Shipping compare: ordertotalShipping=" + orderTotalShipping, module)
        }
        compareBase = orderTotalShipping.compareTo(new BigDecimal(condValue))
    }
    result.compareBase = Integer.valueOf(compareBase)
    return result
}

/**
 * This function do nothing except to return true for the product list price minimum amount
 * @return true
 */
def productListPriceMinAmount() {
    // does nothing on order level, only checked on item level, so ignore by always considering passed
    return true
}

/**
 * This function do nothing except to return true for the product list percent minimum amount
 * @return true
 */
def productListPriceMinPercent() {
    // does nothing on order level, only checked on item level, so ignore by always considering passed
    return true
}
