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
package org.apache.ofbiz.product.product.promo

import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.order.shoppingcart.CartItemModifyException
import org.apache.ofbiz.order.shoppingcart.ShoppingCart
import org.apache.ofbiz.order.shoppingcart.ShoppingCartItem
import org.apache.ofbiz.order.shoppingcart.product.ProductPromoWorker
import org.apache.ofbiz.order.shoppingcart.product.ProductPromoWorker.ActionResultInfo
import org.apache.ofbiz.service.GenericServiceException
import org.apache.ofbiz.service.ServiceUtil

/*
 * ================================================================
 * ProductPromoAction Services
 * ================================================================
 */

/**
 * This function return success if conditions are valid and generate gift with purchase
 * @return result
 */
Map productGWP() {
    Map result = success()

    GenericValue productPromoAction = parameters.productPromoAction
    ActionResultInfo actionResultInfo = parameters.actionResultInfo
    ShoppingCart cart = parameters.shoppingCart
    String productStoreId = cart.getProductStoreId()

    // the code was in there for this, so even though I don't think we want to restrict this,
    // just adding this flag to make it easy to change could make option dynamic, but now implied by the use limit
    boolean allowMultipleGwp = true

    Integer itemLoc = ProductPromoWorker.findPromoItem(productPromoAction, cart)
    if (!allowMultipleGwp && itemLoc != null) {
        if (Debug.verboseOn()) {
            logVerbose('Not adding promo item, already there action: ' + productPromoAction)
        }
        actionResultInfo.ranAction = false
    } else {
        BigDecimal quantity
        if (productPromoAction.quantity != null) {
            quantity = productPromoAction.getBigDecimal('quantity')
        } else {
            if (productPromoAction.get('useCartQuantity') == 'Y') {
                quantity = BigDecimal.ZERO
                List<ShoppingCartItem> used = ProductPromoWorker.getCartItemsUsed(cart, productPromoAction)
                for (ShoppingCartItem item : used) {
                    BigDecimal available = item.getPromoQuantityAvailable()
                    quantity = quantity.add(available).add(item.getPromoQuantityCandidateUseActionAndAllConds(productPromoAction))
                    item.addPromoQuantityCandidateUse(available, productPromoAction, false)
                }
            } else {
                quantity = BigDecimal.ZERO
            }
        }

        List<String> optionProductIds = [] as Queue
        String productId = productPromoAction.getString('productId')

        GenericValue product = null
        if (productId) {
            product = from('Product').where('productId', productId).cache().queryOne()
            if (product == null) {
                String errMsg = 'GWP Product not found with ID [' + productId + '] for ProductPromoAction [' +
                        productPromoAction.get('productPromoId') + ':' + productPromoAction.get('productPromoRuleId')
                + ':' + productPromoAction.get('productPromoActionSeqId') + ']'
                logError(errMsg)
                throw new CartItemModifyException(errMsg)
            }
            if (product.getString('isVirtual') == 'Y') {
                List<GenericValue> productAssocs = EntityUtil.filterByDate(
                        product.getRelated('MainProductAssoc', UtilMisc.toMap('productAssocTypeId', 'PRODUCT_VARIANT'),
                                UtilMisc.toList('sequenceNum'), true))
                for (GenericValue productAssoc : productAssocs) {
                    optionProductIds.add(productAssoc.getString('productIdTo'))
                }
                productId = null
                product = null
            } else {
                // check inventory on this product, make sure it is available before going on
                //NOTE: even though the store may not require inventory for purchase, we will always require inventory for gifts
                try {
                    // get the quantity in cart for inventory check
                    BigDecimal quantityAlreadyInCart = BigDecimal.ZERO
                    List<ShoppingCartItem> matchingItems = cart.findAllCartItems(productId)
                    for (ShoppingCartItem item : matchingItems) {
                        quantityAlreadyInCart = quantityAlreadyInCart.add(item.getQuantity())
                    }
                    Map<String, Object> invReqResult = dispatcher.runSync('isStoreInventoryAvailable',
                            [productStoreId: productStoreId, productId: productId, product: product, quantity: quantity.add(quantityAlreadyInCart)])
                    if (ServiceUtil.isError(invReqResult)) {
                        logError('Error calling isStoreInventoryAvailable service, result is: ' + invReqResult)
                        throw new CartItemModifyException(ServiceUtil.getErrorMessage(invReqResult))
                    } else if ('Y' != invReqResult.get('available')) {
                        logWarning(UtilProperties.getMessage(resource_error, 'OrderNotApplyingGwpBecauseProductIdIsOutOfStockForProductPromoAction',
                                UtilMisc.toMap('productId', productId, 'productPromoAction', productPromoAction), cart.getLocale()))
                        productId = null
                        product = null
                    }
                } catch (GenericServiceException e) {
                    String errMsg = 'Fatal error calling inventory checking services: ' + e
                    logError(e, errMsg)
                    throw new CartItemModifyException(errMsg)
                }
            }
        }

        // support multiple gift options if products are attached to the action, or if the productId on the action is a virtual product
        Set<String> productIds = ProductPromoWorker.getPromoRuleActionProductIds(productPromoAction, delegator, nowTimestamp)
        optionProductIds.addAll(productIds)

        // make sure these optionProducts have inventory...
        Iterator<String> optionProductIdIter = optionProductIds.iterator()
        while (optionProductIdIter.hasNext()) {
            String optionProductId = optionProductIdIter.next()

            try {
                // get the quantity in cart for inventory check
                BigDecimal quantityAlreadyInCart = BigDecimal.ZERO
                List<ShoppingCartItem> matchingItems = cart.findAllCartItems(optionProductId)
                for (ShoppingCartItem item : matchingItems) {
                    quantityAlreadyInCart = quantityAlreadyInCart.add(item.getQuantity())
                }

                Map<String, Object> invReqResult = dispatcher.runSync('isStoreInventoryAvailable',
                        [productStoreId: productStoreId, productId: optionProductId, product: product, quantity: quantity.add(quantityAlreadyInCart)])
                if (ServiceUtil.isError(invReqResult)) {
                    logError('Error calling isStoreInventoryAvailable service, result is: ' + invReqResult)
                    throw new CartItemModifyException(ServiceUtil.getErrorMessage(invReqResult))
                } else if ('Y' != invReqResult.get('available')) {
                    optionProductIdIter.remove()
                }
            } catch (GenericServiceException e) {
                String errMsg = 'Fatal error calling inventory checking services: ' + e
                logError(e, errMsg)
                throw new CartItemModifyException(errMsg)
            }
        }

        // check to see if any desired productIds have been selected for this promo action
        String alternateGwpProductId = cart.getDesiredAlternateGiftByAction(productPromoAction.getPrimaryKey())
        if (alternateGwpProductId) {
            // also check to make sure this isn't a spoofed ID somehow, check to see if it is in the Set
            if (optionProductIds.contains(alternateGwpProductId)) {
                if (!productId) {
                    optionProductIds.add(productId)
                }
                optionProductIds.remove(alternateGwpProductId)
                productId = alternateGwpProductId
                product = from('Product').where('productId', productId).cache().queryOne()
            } else {
                logWarning(UtilProperties.getMessage(resource_error,
                        'OrderAnAlternateGwpProductIdWasInPlaceButWasEitherNotValidOrIsNoLongerInStockForId',
                        [alternateGwpProductId: alternateGwpProductId], cart.getLocale()))
            }
        }

        // if product is null, get one from the productIds set
        if (!product && optionProductIds.size() > 0) {
            // get the first from an iterator and remove it since it will be the current one
            Iterator<String> optionProductIdTempIter = optionProductIds.iterator()
            productId = optionProductIdTempIter.next()
            optionProductIdTempIter.remove()
            product = from('Product').where('productId', productId).cache().queryOne()
        }
        if (!product) {
            // no product found to add as GWP, just return
            return [*:result, actionResultInfo: parameters.actionResultInfo]
        }

        // pass null for cartLocation to add to end of cart, pass false for doPromotions to avoid infinite recursion
        ShoppingCartItem gwpItem = null
        try {
            // just leave the prodCatalogId null, this line won't be associated with a catalog
            gwpItem = ShoppingCartItem.makeItem(null, product, null, quantity, null, null, null, null, null, null,
                    null, null, null, null, null, null, dispatcher, cart, Boolean.FALSE, Boolean.TRUE, null, Boolean.FALSE, Boolean.FALSE)
            if (optionProductIds.size() > 0) {
                gwpItem.setAlternativeOptionProductIds(optionProductIds)
            } else {
                gwpItem.setAlternativeOptionProductIds(null)
            }
        } catch (CartItemModifyException e) {
            logError(e.getMessage())
            throw e
        }

        BigDecimal discountAmount = quantity * (gwpItem.getBasePrice()).negate()
        ProductPromoWorker.doOrderItemPromoAction(productPromoAction, gwpItem, discountAmount, 'amount', delegator)

        // set promo after create note that to setQuantity we must clear this flag, setQuantity, then re-set the flag
        gwpItem.setIsPromo(true)
        if (Debug.verboseOn()) {
            logVerbose('gwpItem adjustments: ' + gwpItem.getAdjustments())
        }

        actionResultInfo.ranAction = true
        actionResultInfo.totalDiscountAmount = discountAmount
        result.actionResultInfo = actionResultInfo
        return result
    }
}

/**
 * This function return success, if conditions are valid shipping will be set free
 * @return result
 */
Map productActFreeShip() {
    Map result = success()

    GenericValue productPromoAction = parameters.productPromoAction
    ActionResultInfo actionResultInfo = parameters.actionResultInfo
    ShoppingCart cart = parameters.shoppingCart
    // this may look a bit funny: on each pass all rules that do free shipping will set their owrule id for it,
    // and on unapply if the promo and rule ids are the same then it will clear it essentially on any pass
    // through the promos and rules if any free shipping should be there, it will be there
    cart.addFreeShippingProductPromoAction(productPromoAction)
    // don't consider this as a cart change?
    actionResultInfo.ranAction = true
    // should probably set the totalDiscountAmount to something, but we have no idea what it will be, so leave at 0, will still get run
    result.actionResultInfo = actionResultInfo
    return result
}

/**
 * This function return success, if conditions are valid so X Product for Y% Discount
 * @return result
 */
Map productDISC() {
    Map result = success()

    GenericValue productPromoAction = parameters.productPromoAction
    ActionResultInfo actionResultInfo = parameters.actionResultInfo
    ShoppingCart cart = parameters.shoppingCart

    BigDecimal quantityDesired = productPromoAction.quantity ? productPromoAction.getBigDecimal('quantity') : BigDecimal.ONE
    BigDecimal startingQuantity = quantityDesired
    BigDecimal discountAmountTotal = BigDecimal.ZERO

    Set<String> productIds = ProductPromoWorker.getPromoRuleActionProductIds(productPromoAction, delegator, nowTimestamp)

    List<ShoppingCartItem> lineOrderedByBasePriceList = cart.getLineListOrderedByBasePrice(false)
    Iterator<ShoppingCartItem> lineOrderedByBasePriceIter = lineOrderedByBasePriceList.iterator()
    while (quantityDesired > 0 && lineOrderedByBasePriceIter.hasNext()) {
        ShoppingCartItem cartItem = lineOrderedByBasePriceIter.next()
        // only include if it is in the productId Set for this check and if it is not a Promo (GWP) item
        GenericValue product = cartItem.getProduct()
        String parentProductId = cartItem.getParentProductId()
        boolean passedItemConds = ProductPromoWorker.checkConditionsForItem(productPromoAction, cart, cartItem, delegator, dispatcher, nowTimestamp)

        if (passedItemConds && !cartItem.getIsPromo()
                && (productIds.contains(cartItem.getProductId()) || (parentProductId && productIds.contains(parentProductId)))
                && (!product || 'N' != product.includeInPromotions)) {
            // reduce quantity still needed to qualify for promo (quantityNeeded)
            BigDecimal quantityUsed = cartItem.addPromoQuantityCandidateUse(quantityDesired, productPromoAction, false)
            if (quantityUsed > 0) {
                quantityDesired = quantityDesired.subtract(quantityUsed)

                // create an adjustment and add it to the cartItem that implements the promotion action
                BigDecimal percentModifier = productPromoAction.amount ? productPromoAction.getBigDecimal('amount').movePointLeft(2) : BigDecimal.ZERO
                BigDecimal lineAmount = quantityUsed * cartItem.getBasePrice() * cartItem.getRentalAdjustment()
                BigDecimal discountAmount = lineAmount * percentModifier.negate()
                discountAmountTotal = discountAmountTotal.add(discountAmount)
                // not doing this any more, now distributing among conditions and actions (see call below):
                // doOrderItemPromoAction(productPromoAction, cartItem, discountAmount, "amount", delegator)
            }
        }
    }

    if (quantityDesired == startingQuantity) {
        // couldn't find any (or enough) cart items to give a discount to, don't consider action run
        actionResultInfo.ranAction = false
        // clear out any action uses for this so they don't become part of anything else
        cart.resetPromoRuleUse(productPromoAction.getString('productPromoId'), productPromoAction.getString('productPromoRuleId'))
    } else {
        BigDecimal totalAmount = ProductPromoWorker.getCartItemsUsedTotalAmount(cart, productPromoAction)
        if (Debug.verboseOn()) {
            logVerbose('Applying promo [' + productPromoAction.getPrimaryKey() + ']\n totalAmount='
                    + totalAmount + ', discountAmountTotal=' + discountAmountTotal)
        }
        ProductPromoWorker.distributeDiscountAmount(discountAmountTotal, totalAmount,
                ProductPromoWorker.getCartItemsUsed(cart, productPromoAction), productPromoAction, delegator)
        actionResultInfo.ranAction = true
        actionResultInfo.totalDiscountAmount = discountAmountTotal
        actionResultInfo.quantityLeftInAction = quantityDesired
    }
    result.actionResultInfo = actionResultInfo
    return result
}

/**
 * This function return success, if conditions are valid so X Product for Y Discount
 */
Map productAMDISC() {
    Map result = success()

    GenericValue productPromoAction = parameters.productPromoAction
    ActionResultInfo actionResultInfo = parameters.actionResultInfo
    ShoppingCart cart = parameters.shoppingCart

    BigDecimal quantityDesired = productPromoAction.quantity ? productPromoAction.getBigDecimal('quantity') : BigDecimal.ONE
    BigDecimal startingQuantity = quantityDesired
    BigDecimal discountAmountTotal = BigDecimal.ZERO

    Set<String> productIds = ProductPromoWorker.getPromoRuleActionProductIds(productPromoAction, delegator, nowTimestamp)

    List<ShoppingCartItem> lineOrderedByBasePriceList = cart.getLineListOrderedByBasePrice(false)
    Iterator<ShoppingCartItem> lineOrderedByBasePriceIter = lineOrderedByBasePriceList.iterator()
    while (quantityDesired > 0 && lineOrderedByBasePriceIter.hasNext()) {
        ShoppingCartItem cartItem = lineOrderedByBasePriceIter.next()
        // only include if it is in the productId Set for this check and if it is not a Promo (GWP) item
        String parentProductId = cartItem.getParentProductId()
        GenericValue product = cartItem.getProduct()
        boolean passedItemConds = ProductPromoWorker.checkConditionsForItem(productPromoAction, cart, cartItem, delegator, dispatcher, nowTimestamp)
        if (passedItemConds && !cartItem.getIsPromo() &&
                (productIds.contains(cartItem.getProductId()) || (parentProductId != null && productIds.contains(parentProductId))) &&
                (!product || 'N' != product.getString('includeInPromotions'))) {
            // reduce quantity still needed to qualify for promo (quantityNeeded)
            BigDecimal quantityUsed = cartItem.addPromoQuantityCandidateUse(quantityDesired, productPromoAction, false)
            quantityDesired = quantityDesired.subtract(quantityUsed)

            // create an adjustment and add it to the cartItem that implements the promotion action
            BigDecimal discount = productPromoAction.amount ? productPromoAction.getBigDecimal('amount') : BigDecimal.ZERO
            // don't allow the discount to be greater than the price
            if (discount > cartItem.getBasePrice() * cartItem.getRentalAdjustment()) {
                discount = cartItem.getBasePrice() * cartItem.getRentalAdjustment()
            }
            BigDecimal discountAmount = quantityUsed * discount.negate()
            discountAmountTotal = discountAmountTotal.add(discountAmount)
            // not doing this any more, now distributing among conditions and actions (see call below):
            // doOrderItemPromoAction(productPromoAction, cartItem, discountAmount, "amount", delegator)
        }
    }

    if (quantityDesired == startingQuantity) {
        // couldn't find any cart items to give a discount to, don't consider action run
        actionResultInfo.ranAction = false
    } else {
        BigDecimal totalAmount = ProductPromoWorker.getCartItemsUsedTotalAmount(cart, productPromoAction)
        if (Debug.verboseOn()) {
            logVerbose('Applying promo [' + productPromoAction.getPrimaryKey() + ']\n totalAmount='
                    + totalAmount + ', discountAmountTotal=' + discountAmountTotal)
        }
        ProductPromoWorker.distributeDiscountAmount(discountAmountTotal, totalAmount,
                ProductPromoWorker.getCartItemsUsed(cart, productPromoAction), productPromoAction, delegator)
        actionResultInfo.ranAction = true
        actionResultInfo.totalDiscountAmount = discountAmountTotal
        actionResultInfo.quantityLeftInAction = quantityDesired
    }
    result.actionResultInfo = actionResultInfo
    return result
}

Map productPrice() {
    Map result = success()

    GenericValue productPromoAction = parameters.productPromoAction
    ActionResultInfo actionResultInfo = parameters.actionResultInfo
    ShoppingCart cart = parameters.shoppingCart

    // with this we want the set of used items to be one price, so total the price for all used items,
    // subtract the amount we want them to cost, and create an adjustment for what is left
    BigDecimal quantityDesired = productPromoAction.quantity ? productPromoAction.getBigDecimal('quantity') : BigDecimal.ONE
    BigDecimal desiredAmount = productPromoAction.amount ? productPromoAction.getBigDecimal('amount') : BigDecimal.ZERO
    BigDecimal totalAmount = BigDecimal.ZERO

    Set<String> productIds = ProductPromoWorker.getPromoRuleActionProductIds(productPromoAction, delegator, nowTimestamp)

    List<ShoppingCartItem> cartItemsUsed = [] as Queue
    List<ShoppingCartItem> lineOrderedByBasePriceList = cart.getLineListOrderedByBasePrice(false)
    Iterator<ShoppingCartItem> lineOrderedByBasePriceIter = lineOrderedByBasePriceList.iterator()
    while (quantityDesired > 0 && lineOrderedByBasePriceIter.hasNext()) {
        ShoppingCartItem cartItem = lineOrderedByBasePriceIter.next()
        // only include if it is in the productId Set for this check and if it is not a Promo (GWP) item
        String parentProductId = cartItem.getParentProductId()
        GenericValue product = cartItem.getProduct()
        boolean passedItemConds = ProductPromoWorker.checkConditionsForItem(productPromoAction, cart, cartItem, delegator, dispatcher, nowTimestamp)

        if (passedItemConds &&
                !cartItem.getIsPromo() &&
                (productIds.contains(cartItem.getProductId()) || (parentProductId != null && productIds.contains(parentProductId))) &&
                (product == null || 'N' != product.getString('includeInPromotions'))) {
            // reduce quantity still needed to qualify for promo (quantityNeeded)
            BigDecimal quantityUsed = cartItem.addPromoQuantityCandidateUse(quantityDesired, productPromoAction, false)
            if (quantityUsed > 0) {
                quantityDesired = quantityDesired.subtract(quantityUsed)
                totalAmount = totalAmount.add(quantityUsed * cartItem.getBasePrice() * cartItem.getRentalAdjustment())
                cartItemsUsed.add(cartItem)
            }
        }
    }

    if (totalAmount > desiredAmount && quantityDesired == 0) {
        BigDecimal discountAmountTotal = totalAmount.subtract(desiredAmount).negate()
        ProductPromoWorker.distributeDiscountAmount(discountAmountTotal, totalAmount, cartItemsUsed, productPromoAction, delegator)
        actionResultInfo.ranAction = true
        actionResultInfo.totalDiscountAmount = discountAmountTotal
        // no use setting the quantityLeftInAction because that does not apply for buy X for $Y type promotions, it is all or nothing
    } else {
        actionResultInfo.ranAction = false
        // clear out any action uses for this so they don't become part of anything else
        cart.resetPromoRuleUse(productPromoAction.getString('productPromoId'), productPromoAction.getString('productPromoRuleId'))
    }
    result.actionResultInfo = actionResultInfo
    return result
}

Map productOrderPercent() {
    Map result = success()

    GenericValue productPromoAction = parameters.productPromoAction
    ActionResultInfo actionResultInfo = parameters.actionResultInfo
    ShoppingCart cart = parameters.shoppingCart

    BigDecimal percentage = productPromoAction.amount ? productPromoAction.getBigDecimal('amount') : BigDecimal.ZERO
    percentage = percentage.movePointLeft(2).negate()
    Set<String> productIds = ProductPromoWorker.getPromoRuleActionProductIds(productPromoAction, delegator, nowTimestamp)
    BigDecimal amount = BigDecimal.ZERO
    if (productIds) {
        amount = cart.getSubTotalForPromotions(productIds) * percentage
    } else {
        amount = cart.getSubTotalForPromotions() * percentage
    }
    if (amount != 0) {
        ProductPromoWorker.doOrderPromoAction(productPromoAction, cart, amount, 'amount', delegator)
        actionResultInfo.ranAction = true
        actionResultInfo.totalDiscountAmount = amount
    }
    result.actionResultInfo = actionResultInfo
    return result
}

Map productOrderAmount() {
    Map result = success()

    GenericValue productPromoAction = parameters.productPromoAction
    ActionResultInfo actionResultInfo = parameters.actionResultInfo
    ShoppingCart cart = parameters.shoppingCart

    BigDecimal amount = (productPromoAction.amount ? productPromoAction.getBigDecimal('amount') : BigDecimal.ZERO).negate()
    // if amount is greater than the order sub total, set equal to order sub total, this normally wouldn't happen because
    // there should be a condition that the order total be above a certain amount, but just in case...
    BigDecimal subTotal = cart.getSubTotalForPromotions()
    if (amount.negate() > subTotal) {
        amount = subTotal.negate()
    }
    if (amount != 0) {
        ProductPromoWorker.doOrderPromoAction(productPromoAction, cart, amount, 'amount', delegator)
        actionResultInfo.ranAction = true
        actionResultInfo.totalDiscountAmount = amount
    }
    result.actionResultInfo = actionResultInfo
    return result
}

Map productSpecialPrice() {
    Map result = success()

    GenericValue productPromoAction = parameters.productPromoAction
    ActionResultInfo actionResultInfo = parameters.actionResultInfo
    ShoppingCart cart = parameters.shoppingCart

    // if there are productIds associated with the action then restrict to those productIds, otherwise apply for all products
    Set<String> productIds = ProductPromoWorker.getPromoRuleActionProductIds(productPromoAction, delegator, nowTimestamp)

    // go through the cart items and for each product that has a specialPromoPrice use that price
    for (ShoppingCartItem cartItem : cart.items()) {
        String itemProductId = cartItem.getProductId()
        if (!itemProductId
            || productIds && !productIds.contains(itemProductId)
            || !cartItem.getSpecialPromoPrice()) {
            continue
        }

        // get difference between basePrice and specialPromoPrice and adjust for that
        BigDecimal difference = (cartItem.getBasePrice() * cartItem.getRentalAdjustment()).subtract(cartItem.getSpecialPromoPrice()).negate()

        if (difference != 0) {
            BigDecimal quantityUsed = cartItem.addPromoQuantityCandidateUse(cartItem.getQuantity(), productPromoAction, false)
            if (quantityUsed > BigDecimal.ZERO) {
                BigDecimal amount = difference * quantityUsed
                ProductPromoWorker.doOrderItemPromoAction(productPromoAction, cartItem, amount, 'amount', delegator)
                actionResultInfo.ranAction = true
                actionResultInfo.totalDiscountAmount = amount
            }
        }
    }
    result.actionResultInfo = actionResultInfo
    return result
}

Map productShipCharge() {
    Map result = success()

    GenericValue productPromoAction = parameters.productPromoAction
    ActionResultInfo actionResultInfo = parameters.actionResultInfo
    ShoppingCart cart = parameters.shoppingCart

    BigDecimal percentage = (productPromoAction.amount ? (productPromoAction.getBigDecimal('amount').movePointLeft(2)) : BigDecimal.ZERO).negate()
    BigDecimal amount = cart.getTotalShipping() * percentage
    if (amount != 0) {
        int existingOrderPromoIndex = cart.getAdjustmentPromoIndex(productPromoAction.getString('productPromoId'))
        if (existingOrderPromoIndex != -1 && cart.getAdjustment(existingOrderPromoIndex).getBigDecimal('amount') == amount) {
            actionResultInfo.ranAction = false  // already ran, no need to repeat
        } else {
            if (existingOrderPromoIndex != -1 && cart.getAdjustment(existingOrderPromoIndex).getBigDecimal('amount') != amount) {
                cart.removeAdjustment(existingOrderPromoIndex)
            }
            ProductPromoWorker.doOrderPromoAction(productPromoAction, cart, amount, 'amount', delegator)
            actionResultInfo.ranAction = true
            actionResultInfo.totalDiscountAmount = amount
        }
    }
    result.actionResultInfo = actionResultInfo
    return result
}

Map productTaxPercent() {
    Map result = success()

    GenericValue productPromoAction = parameters.productPromoAction
    ActionResultInfo actionResultInfo = parameters.actionResultInfo
    ShoppingCart cart = parameters.shoppingCart

    BigDecimal percentage = (productPromoAction.amount ? (productPromoAction.getBigDecimal('amount').movePointLeft(2)) : BigDecimal.ZERO).negate()
    BigDecimal amount = cart.getTotalSalesTax() * percentage
    if (amount != 0) {
        ProductPromoWorker.doOrderPromoAction(productPromoAction, cart, amount, 'amount', delegator)
        actionResultInfo.ranAction = true
        actionResultInfo.totalDiscountAmount = amount
    }
    result.actionResultInfo = actionResultInfo
    return result
}

