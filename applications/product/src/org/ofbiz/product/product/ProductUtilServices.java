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
package org.ofbiz.product.product;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericPK;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.model.DynamicViewEntity;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelKeyMap;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

/**
 * Product Services
 */
public class ProductUtilServices {

    public static final String module = ProductUtilServices.class.getName();
    public static final String resource = "ProductUiLabels";

    /** First expirt all ProductAssocs for all disc variants, then disc all virtuals that have all expired variant ProductAssocs */
    public static Map discVirtualsWithDiscVariants(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        Locale locale = (Locale) context.get("locale");
        String errMsg = null;

        try {
            EntityCondition conditionOne = new EntityConditionList(UtilMisc.toList(
                    new EntityExpr("isVariant", EntityOperator.EQUALS, "Y"),
                    new EntityExpr("salesDiscontinuationDate", EntityOperator.NOT_EQUAL, null),
                    new EntityExpr("salesDiscontinuationDate", EntityOperator.LESS_THAN_EQUAL_TO, nowTimestamp)
                    ), EntityOperator.AND);
            EntityListIterator eliOne = delegator.findListIteratorByCondition("Product", conditionOne, null, null);
            GenericValue productOne = null;
            int numSoFarOne = 0;
            while ((productOne = (GenericValue) eliOne.next()) != null) {
                String virtualProductId = ProductWorker.getVariantVirtualId(productOne);
                GenericValue virtualProduct = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", virtualProductId));
                if (virtualProduct == null) {
                    continue;
                }
                List passocList = delegator.findByAnd("ProductAssoc", UtilMisc.toMap("productId", virtualProductId, "productIdTo", productOne.get("productId"), "productAssocTypeId", "PRODUCT_VARIANT"));
                passocList = EntityUtil.filterByDate(passocList, nowTimestamp);
                if (passocList.size() > 0) {
                    Iterator passocIter = passocList.iterator();
                    while (passocIter.hasNext()) {
                        GenericValue passoc = (GenericValue) passocIter.next();
                        passoc.set("thruDate", nowTimestamp);
                        passoc.store();
                    }

                    numSoFarOne++;
                    if (numSoFarOne % 500 == 0) {
                        Debug.logInfo("Expired variant ProductAssocs for " + numSoFarOne + " sales discontinued variant products.", module);
                    }
                }
            }
            eliOne.close();

            // get all non-discontinued virtuals, see if all variant ProductAssocs are expired, if discontinue
            EntityCondition condition = new EntityConditionList(UtilMisc.toList(
                    new EntityExpr("isVirtual", EntityOperator.EQUALS, "Y"),
                    new EntityExpr(new EntityExpr("salesDiscontinuationDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr("salesDiscontinuationDate", EntityOperator.GREATER_THAN_EQUAL_TO, nowTimestamp))
                    ), EntityOperator.AND);
            EntityListIterator eli = delegator.findListIteratorByCondition("Product", condition, null, null);
            GenericValue product = null;
            int numSoFar = 0;
            while ((product = (GenericValue) eli.next()) != null) {
                List passocList = delegator.findByAnd("ProductAssoc", UtilMisc.toMap("productId", product.get("productId"), "productAssocTypeId", "PRODUCT_VARIANT"));
                passocList = EntityUtil.filterByDate(passocList, nowTimestamp);
                if (passocList.size() == 0) {
                    product.set("salesDiscontinuationDate", nowTimestamp);

                    numSoFar++;
                    if (numSoFar % 500 == 0) {
                        Debug.logInfo("Sales discontinued " + numSoFar + " virtual products that have no valid variants.", module);
                    }
                }
            }
            eli.close();
        } catch (GenericEntityException e) {
            Map messageMap = UtilMisc.toMap("errMessage", e.toString());
            errMsg = UtilProperties.getMessage(resource,"productutilservices.entity_error_running_discVirtualsWithDiscVariants", messageMap, locale);
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        return ServiceUtil.returnSuccess();
    }

    /** for all disc products, remove from category memberships */
    public static Map removeCategoryMembersOfDiscProducts(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        Locale locale = (Locale) context.get("locale");
        String errMsg = null;

        try {
            EntityCondition condition = new EntityConditionList(UtilMisc.toList(
                    new EntityExpr("salesDiscontinuationDate", EntityOperator.NOT_EQUAL, null),
                    new EntityExpr("salesDiscontinuationDate", EntityOperator.LESS_THAN_EQUAL_TO, nowTimestamp)
                    ), EntityOperator.AND);
            EntityListIterator eli = delegator.findListIteratorByCondition("Product", condition, null, null);
            GenericValue product = null;
            int numSoFar = 0;
            while ((product = (GenericValue) eli.next()) != null) {
                String productId = product.getString("productId");
                List productCategoryMemberList = delegator.findByAnd("ProductCategoryMember", UtilMisc.toMap("productId", productId));
                if (productCategoryMemberList.size() > 0) {
                    Iterator productCategoryMemberIter = productCategoryMemberList.iterator();
                    while (productCategoryMemberIter.hasNext()) {
                        GenericValue productCategoryMember = (GenericValue) productCategoryMemberIter.next();
                        // coded this way rather than a removeByAnd so it can be easily changed...
                        productCategoryMember.remove();
                    }
                    numSoFar++;
                    if (numSoFar % 500 == 0) {
                        Debug.logInfo("Removed category members for " + numSoFar + " sales discontinued products.", module);
                    }
                }
            }
            eli.close();
            Debug.logInfo("Completed - Removed category members for " + numSoFar + " sales discontinued products.", module);
        } catch (GenericEntityException e) {
            Map messageMap = UtilMisc.toMap("errMessage", e.toString());
            errMsg = UtilProperties.getMessage(resource,"productutilservices.entity_error_running_removeCategoryMembersOfDiscProducts", messageMap, locale);
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map removeDuplicateOpenEndedCategoryMembers(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        Locale locale = (Locale) context.get("locale");
        String errMsg = null;

        try {
            DynamicViewEntity dve = new DynamicViewEntity();
            dve.addMemberEntity("PCM", "ProductCategoryMember");
            dve.addAlias("PCM", "productId", null, null, null, Boolean.TRUE, null);
            dve.addAlias("PCM", "productCategoryId", null, null, null, Boolean.TRUE, null);
            dve.addAlias("PCM", "fromDate", null, null, null, null, null);
            dve.addAlias("PCM", "thruDate", null, null, null, null, null);
            dve.addAlias("PCM", "productIdCount", "productId", null, null, null, "count");

            EntityCondition condition = new EntityConditionList(UtilMisc.toList(
                    new EntityExpr("fromDate", EntityOperator.LESS_THAN, nowTimestamp),
                    new EntityExpr("thruDate", EntityOperator.EQUALS, null)
                    ), EntityOperator.AND);
            EntityCondition havingCond = new EntityExpr("productIdCount", EntityOperator.GREATER_THAN, new Long(1));
            EntityListIterator eli = delegator.findListIteratorByCondition(dve, condition, havingCond, UtilMisc.toList("productId", "productCategoryId", "productIdCount"), null, null);
            GenericValue pcm = null;
            int numSoFar = 0;
            while ((pcm = (GenericValue) eli.next()) != null) {
                List productCategoryMemberList = delegator.findByAnd("ProductCategoryMember", UtilMisc.toMap("productId", pcm.get("productId"), "productCategoryId", pcm.get("productCategoryId")));
                if (productCategoryMemberList.size() > 1) {
                    // remove all except the first...
                    productCategoryMemberList.remove(0);
                    Iterator productCategoryMemberIter = productCategoryMemberList.iterator();
                    while (productCategoryMemberIter.hasNext()) {
                        GenericValue productCategoryMember = (GenericValue) productCategoryMemberIter.next();
                        productCategoryMember.remove();
                    }
                    numSoFar++;
                    if (numSoFar % 500 == 0) {
                        Debug.logInfo("Removed category members for " + numSoFar + " products with duplicate category members.", module);
                    }
                }
            }
            eli.close();
            Debug.logInfo("Completed - Removed category members for " + numSoFar + " products with duplicate category members.", module);
        } catch (GenericEntityException e) {
            Map messageMap = UtilMisc.toMap("errMessage", e.toString());
            errMsg = UtilProperties.getMessage(resource,"productutilservices.entity_error_running_removeDuplicateOpenEndedCategoryMembers", messageMap, locale);
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map makeStandAloneFromSingleVariantVirtuals(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        Locale locale = (Locale) context.get("locale");
        String errMsg = null;

        Debug.logInfo("Starting makeStandAloneFromSingleVariantVirtuals", module);

        DynamicViewEntity dve = new DynamicViewEntity();
        dve.addMemberEntity("PVIRT", "Product");
        dve.addMemberEntity("PVA", "ProductAssoc");
        //dve.addMemberEntity("PVAR", "Product");
        dve.addViewLink("PVIRT", "PVA", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("productId", "productId")));
        //dve.addViewLink("PVA", "PVAR", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("productIdTo", "productId")));
        dve.addAlias("PVIRT", "productId", null, null, null, Boolean.TRUE, null);
        dve.addAlias("PVIRT", "salesDiscontinuationDate", null, null, null, null, null);
        dve.addAlias("PVA", "productAssocTypeId", null, null, null, null, null);
        dve.addAlias("PVA", "fromDate", null, null, null, null, null);
        dve.addAlias("PVA", "thruDate", null, null, null, null, null);
        dve.addAlias("PVA", "productIdToCount", "productIdTo", null, null, null, "count-distinct");
        //dve.addAlias("PVAR", "variantProductId", "productId", null, null, null, null);

        try {
            EntityCondition condition = new EntityConditionList(UtilMisc.toList(
                    new EntityExpr("productAssocTypeId", EntityOperator.EQUALS, "PRODUCT_VARIANT"),
                    new EntityExpr(new EntityExpr("salesDiscontinuationDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr("salesDiscontinuationDate", EntityOperator.GREATER_THAN, nowTimestamp))
                    ), EntityOperator.AND);
            EntityCondition havingCond = new EntityExpr("productIdToCount", EntityOperator.EQUALS, new Long(1));
            EntityListIterator eliOne = delegator.findListIteratorByCondition(dve, condition, havingCond, UtilMisc.toList("productId", "productIdToCount"), null, null);
            List valueList = eliOne.getCompleteList();
            eliOne.close();

            Debug.logInfo("Found " + valueList.size() + " virtual products with one variant to turn into a stand alone product.", module);

            int numWithOneOnly = 0;
            Iterator valueIter = valueList.iterator();
            while (valueIter.hasNext()) {
                // has only one variant period, is it valid? should already be discontinued if not
                GenericValue value = (GenericValue) valueIter.next();

                String productId = value.getString("productId");
                List paList = delegator.findByAnd("ProductAssoc", UtilMisc.toMap("productId", productId, "productAssocTypeId", "PRODUCT_VARIANT"));
                // verify the query; tested on a bunch, looks good
                if (paList.size() != 1) {
                    Debug.logInfo("Virtual product with ID " + productId + " should have 1 assoc, has " + paList.size(), module);
                } else {
                    //if (numWithOneOnly < 100) {
                    //    Debug.logInfo("Virtual product ID to make stand-alone: " + productId, module);
                    //}
                    // for all virtuals with one variant move all info from virtual to variant and remove virtual, make variant as not a variant
                    dispatcher.runSync("mergeVirtualWithSingleVariant", UtilMisc.toMap("productId", productId, "removeOld", Boolean.TRUE, "userLogin", userLogin));

                    numWithOneOnly++;
                    if (numWithOneOnly % 100 == 0) {
                        Debug.logInfo("Made " + numWithOneOnly + " virtual products with only one valid variant stand-alone products.", module);
                    }
                }
            }

            EntityCondition conditionWithDates = new EntityConditionList(UtilMisc.toList(
                    new EntityExpr("productAssocTypeId", EntityOperator.EQUALS, "PRODUCT_VARIANT"),
                    new EntityExpr(new EntityExpr("salesDiscontinuationDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr("salesDiscontinuationDate", EntityOperator.GREATER_THAN, nowTimestamp)),
                    new EntityExpr("fromDate", EntityOperator.LESS_THAN_EQUAL_TO, nowTimestamp),
                    new EntityExpr(new EntityExpr("thruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr("thruDate", EntityOperator.GREATER_THAN_EQUAL_TO, nowTimestamp))
                    ), EntityOperator.AND);
            EntityListIterator eliMulti = delegator.findListIteratorByCondition(dve, conditionWithDates, havingCond, UtilMisc.toList("productId", "productIdToCount"), null, null);
            List valueMultiList = eliMulti.getCompleteList();
            eliMulti.close();

            Debug.logInfo("Found " + valueMultiList.size() + " virtual products with one VALID variant to pull the variant from to make a stand alone product.", module);

            int numWithOneValid = 0;
            Iterator valueMultiIter = valueMultiList.iterator();
            while (valueMultiIter.hasNext()) {
                GenericValue value = (GenericValue) valueMultiIter.next();
                // has only one valid variant
                String productId = value.getString("productId");

                List paList = EntityUtil.filterByDate(delegator.findByAnd("ProductAssoc", UtilMisc.toMap("productId", productId, "productAssocTypeId", "PRODUCT_VARIANT")), nowTimestamp);

                // verify the query; tested on a bunch, looks good
                if (paList.size() != 1) {
                    Debug.logInfo("Virtual product with ID " + productId + " should have 1 assoc, has " + paList.size(), module);
                } else {
                    // for all virtuals with one valid variant move info from virtual to variant, put variant in categories from virtual, remove virtual from all categories but leave "family" otherwise intact, mark variant as not a variant
                    dispatcher.runSync("mergeVirtualWithSingleVariant", UtilMisc.toMap("productId", productId, "removeOld", Boolean.FALSE, "userLogin", userLogin));

                    numWithOneValid++;
                    if (numWithOneValid % 100 == 0) {
                        Debug.logInfo("Made " + numWithOneValid + " virtual products with one valid variant stand-alone products.", module);
                    }
                }
            }

            Debug.logInfo("Found virtual products with one valid variant: " + numWithOneValid + ", with one variant only: " + numWithOneOnly, module);
        } catch (GenericEntityException e) {
            Map messageMap = UtilMisc.toMap("errMessage", e.toString());
            errMsg = UtilProperties.getMessage(resource,"productutilservices.entity_error_running_makeStandAloneFromSingleVariantVirtuals", messageMap, locale);
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } catch (GenericServiceException e) {
            Map messageMap = UtilMisc.toMap("errMessage", e.toString());
            errMsg = UtilProperties.getMessage(resource,"productutilservices.entity_error_running_makeStandAloneFromSingleVariantVirtuals", messageMap, locale);
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map mergeVirtualWithSingleVariant(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        String productId = (String) context.get("productId");
        Boolean removeOldBool = (Boolean) context.get("removeOld");
        boolean removeOld = removeOldBool.booleanValue();
        Locale locale = (Locale) context.get("locale");
        String errMsg = null;

        Boolean testBool = (Boolean) context.get("test");
        boolean test = false;
        if (testBool != null) {
            test = testBool.booleanValue();
        }

        try {
            GenericValue product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId));
            Debug.logInfo("Processing virtual product with one variant with ID: " + productId + " and name: " + product.getString("internalName"), module);

            List paList = EntityUtil.filterByDate(delegator.findByAnd("ProductAssoc", UtilMisc.toMap("productId", productId, "productAssocTypeId", "PRODUCT_VARIANT")), nowTimestamp);
            if (paList.size() > 1) {
                Map messageMap = UtilMisc.toMap("productId", productId);
                errMsg = UtilProperties.getMessage(resource,"productutilservices.found_more_than_one_valid_variant_for_virtual_ID", messageMap, locale);
                Debug.logInfo(errMsg, module);
                return ServiceUtil.returnError(errMsg);
            }

            if (paList.size() == 0) {
                Map messageMap = UtilMisc.toMap("productId", productId);
                errMsg = UtilProperties.getMessage(resource,"productutilservices.did_not_find_any_valid_variants_for_virtual_ID", messageMap, locale);
                Debug.logInfo(errMsg, module);
                return ServiceUtil.returnError(errMsg);
            }

            GenericValue productAssoc = EntityUtil.getFirst(paList);
            if (removeOld) {
                // remove the productAssoc before getting down so it isn't copied over...
                if (test) {
                    Debug.logInfo("Test mode, would remove: " + productAssoc, module);
                } else {
                    productAssoc.remove();
                }
            } else {
                // don't remove, just expire to avoid running again in the future
                productAssoc.set("thruDate", nowTimestamp);
                if (test) {
                    Debug.logInfo("Test mode, would store: " + productAssoc, module);
                } else {
                    productAssoc.store();
                }
            }
            String variantProductId = productAssoc.getString("productIdTo");

            // Product
            GenericValue variantProduct = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", variantProductId));

            Debug.logInfo("--variant has ID: " + variantProductId + " and name: " + variantProduct.getString("internalName"), module);

            // start with the values from the virtual product, override from the variant...
            GenericValue newVariantProduct = delegator.makeValue("Product", product);
            newVariantProduct.setAllFields(variantProduct, false, "", null);
            newVariantProduct.set("isVariant", "N");
            if (test) {
                Debug.logInfo("Test mode, would store: " + newVariantProduct, module);
            } else {
                newVariantProduct.store();
            }

            // ProductCategoryMember - always remove these to pull the virtual from any categories it might have been in
            duplicateRelated(product, "", "ProductCategoryMember", "productId", variantProductId, nowTimestamp, true, delegator, test);

            // ProductFeatureAppl
            duplicateRelated(product, "", "ProductFeatureAppl", "productId", variantProductId, nowTimestamp, removeOld, delegator, test);

            // ProductContent
            duplicateRelated(product, "", "ProductContent", "productId", variantProductId, nowTimestamp, removeOld, delegator, test);

            // ProductPrice
            duplicateRelated(product, "", "ProductPrice", "productId", variantProductId, nowTimestamp, removeOld, delegator, test);

            // GoodIdentification
            duplicateRelated(product, "", "GoodIdentification", "productId", variantProductId, nowTimestamp, removeOld, delegator, test);

            // ProductAttribute
            duplicateRelated(product, "", "ProductAttribute", "productId", variantProductId, nowTimestamp, removeOld, delegator, test);

            // ProductAssoc
            duplicateRelated(product, "Main", "ProductAssoc", "productId", variantProductId, nowTimestamp, removeOld, delegator, test);
            duplicateRelated(product, "Assoc", "ProductAssoc", "productIdTo", variantProductId, nowTimestamp, removeOld, delegator, test);

            if (removeOld) {
                if (test) {
                    Debug.logInfo("Test mode, would remove related ProductKeyword with dummy key: " + product.getRelatedDummyPK("ProductKeyword"), module);
                    Debug.logInfo("Test mode, would remove: " + product, module);
                } else {
                    product.removeRelated("ProductKeyword");
                    product.remove();
                }
            }

            if (test) {
                return ServiceUtil.returnError("Test mode - returning error to get a rollback");
            }
        } catch (GenericEntityException e) {
            Map messageMap = UtilMisc.toMap("errMessage", e.toString());
            errMsg = UtilProperties.getMessage(resource,"productutilservices.entity_error_running_makeStandAloneFromSingleVariantVirtuals", messageMap, locale);
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        return ServiceUtil.returnSuccess();
    }

    protected static void duplicateRelated(GenericValue product, String title, String relatedEntityName, String productIdField, String variantProductId, Timestamp nowTimestamp, boolean removeOld, GenericDelegator delegator, boolean test) throws GenericEntityException {
        List relatedList = EntityUtil.filterByDate(product.getRelated(title + relatedEntityName), nowTimestamp);
        Iterator relatedIter = relatedList.iterator();
        while (relatedIter.hasNext()) {
            GenericValue relatedValue = (GenericValue) relatedIter.next();
            GenericValue newRelatedValue = (GenericValue) relatedValue.clone();
            newRelatedValue.set(productIdField, variantProductId);

            // create a new one? see if one already exists with different from/thru dates
            ModelEntity modelEntity = relatedValue.getModelEntity();
            if (modelEntity.isField("fromDate")) {
                GenericPK findValue = newRelatedValue.getPrimaryKey();
                // can't just set to null, need to remove the value so it isn't a constraint in the query
                //findValue.set("fromDate", null);
                findValue.remove("fromDate");
                List existingValueList = EntityUtil.filterByDate(delegator.findByAnd(relatedEntityName, findValue), nowTimestamp);
                if (existingValueList.size() > 0) {
                    if (test) {
                        Debug.logInfo("Found " + existingValueList.size() + " existing values for related entity name: " + relatedEntityName + ", not copying, findValue is: " + findValue, module);
                    }
                    continue;
                }
                newRelatedValue.set("fromDate", nowTimestamp);
            }

            if (delegator.findCountByAnd(relatedEntityName, newRelatedValue.getPrimaryKey()) == 0) {
                if (test) {
                    Debug.logInfo("Test mode, would create: " + newRelatedValue, module);
                } else {
                    newRelatedValue.create();
                }
            }
        }
        if (removeOld) {
            if (test) {
                Debug.logInfo("Test mode, would remove related " + title + relatedEntityName + " with dummy key: " + product.getRelatedDummyPK(title + relatedEntityName), module);
            } else {
                product.removeRelated(title + relatedEntityName);
            }
        }
    }


    /** reset all product image names with a certain pattern, ex: /images/products/${size}/${productId}.jpg
     * NOTE: only works on fields of Product right now
     */
    public static Map setAllProductImageNames(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        String pattern = (String) context.get("pattern");
        Locale locale = (Locale) context.get("locale");
        String errMsg = null;

        if (UtilValidate.isEmpty(pattern)) {
            String imageFilenameFormat = UtilProperties.getPropertyValue("catalog", "image.filename.format");
            String imageUrlPrefix = UtilProperties.getPropertyValue("catalog", "image.url.prefix");
            pattern = imageUrlPrefix + "/" + imageFilenameFormat;
        }

        try {
            EntityListIterator eli = delegator.findListIteratorByCondition("Product", null, null, null);
            GenericValue product = null;
            int numSoFar = 0;
            while ((product = (GenericValue) eli.next()) != null) {
                String productId = (String) product.get("productId");
                Map smallMap = UtilMisc.toMap("size", "small", "productId", productId);
                Map mediumMap = UtilMisc.toMap("size", "medium", "productId", productId);
                Map largeMap = UtilMisc.toMap("size", "large", "productId", productId);
                Map detailMap = UtilMisc.toMap("size", "detail", "productId", productId);

                if ("Y".equals(product.getString("isVirtual"))) {
                    // find the first variant, use it's ID for the names...
                    List productAssocList = EntityUtil.filterByDate(delegator.findByAnd("ProductAssoc", UtilMisc.toMap("productId", productId, "productAssocTypeId", "PRODUCT_VARIANT")), nowTimestamp);
                    if (productAssocList.size() > 0) {
                        GenericValue productAssoc = EntityUtil.getFirst(productAssocList);
                        smallMap.put("productId", productAssoc.get("productIdTo"));
                        mediumMap.put("productId", productAssoc.get("productIdTo"));
                        product.set("smallImageUrl", FlexibleStringExpander.expandString(pattern, smallMap));
                        product.set("mediumImageUrl", FlexibleStringExpander.expandString(pattern, mediumMap));
                    } else {
                        product.set("smallImageUrl", null);
                        product.set("mediumImageUrl", null);
                    }
                    product.set("largeImageUrl", null);
                    product.set("detailImageUrl", null);
                } else {
                    product.set("smallImageUrl", FlexibleStringExpander.expandString(pattern, smallMap));
                    product.set("mediumImageUrl", FlexibleStringExpander.expandString(pattern, mediumMap));
                    product.set("largeImageUrl", FlexibleStringExpander.expandString(pattern, largeMap));
                    product.set("detailImageUrl", FlexibleStringExpander.expandString(pattern, detailMap));
                }

                product.store();
                numSoFar++;
                if (numSoFar % 500 == 0) {
                    Debug.logInfo("Image URLs set for " + numSoFar + " products.", module);
                }
            }
            eli.close();
            Debug.logInfo("Completed - Image URLs set for " + numSoFar + " products.", module);
        } catch (GenericEntityException e) {
            Map messageMap = UtilMisc.toMap("errMessage", e.toString());
            errMsg = UtilProperties.getMessage(resource,"productutilservices.entity_error_running_setAllProductImageNames", messageMap, locale);
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map clearAllVirtualProductImageNames(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String errMsg = null;

        try {
            EntityListIterator eli = delegator.findListIteratorByCondition("Product", new EntityExpr("isVirtual", EntityOperator.EQUALS, "Y"), null, null);
            GenericValue product = null;
            int numSoFar = 0;
            while ((product = (GenericValue) eli.next()) != null) {
                product.set("smallImageUrl", null);
                product.set("mediumImageUrl", null);
                product.set("largeImageUrl", null);
                product.set("detailImageUrl", null);
                product.store();
                numSoFar++;
                if (numSoFar % 500 == 0) {
                    Debug.logInfo("Image URLs cleared for " + numSoFar + " products.", module);
                }
            }
            eli.close();
            Debug.logInfo("Completed - Image URLs set for " + numSoFar + " products.", module);
        } catch (GenericEntityException e) {
            Map messageMap = UtilMisc.toMap("errMessage", e.toString());
            errMsg = UtilProperties.getMessage(resource,"productutilservices.entity_error_running_clearAllVirtualProductImageNames", messageMap, locale);
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        return ServiceUtil.returnSuccess();
    }

    // set category descriptions from longDescriptions
    /*
allCategories = delegator.findAll("ProductCategory");
allCatIter = allCategories.iterator();
while (allCatIter.hasNext()) {
   cat = allCatIter.next();
   if (UtilValidate.isEmpty(cat.getString("description"))) {
       StringBuffer description = new StringBuffer(cat.getString("longDescription").toLowerCase());
       description.setCharAt(0, Character.toUpperCase(description.charAt(0)));
       for (int i=0; i<description.length() - 1; i++) {
           if (description.charAt(i) == ' ') {
               description.setCharAt(i+1, Character.toUpperCase(description.charAt(i+1)));
           }
       }
       Debug.logInfo("new description: " + description, "ctc.bsh");
              cat.put("description", description.toString());
       cat.store();
   }
}
     */



    public static Map attachProductFeaturesToCategory(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        String productCategoryId = (String) context.get("productCategoryId");
        String doSubCategoriesStr = (String) context.get("doSubCategories");
        Locale locale = (Locale) context.get("locale");
        String errMsg = null;

        // default to true
        boolean doSubCategories = !"N".equals(doSubCategoriesStr);
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        Set productFeatureTypeIdsToExclude = new HashSet();
        String excludeProp = UtilProperties.getPropertyValue("prodsearch", "attach.feature.type.exclude");
        if (UtilValidate.isNotEmpty(excludeProp)) {
            List typeList = StringUtil.split(excludeProp, ",");
            productFeatureTypeIdsToExclude.addAll(typeList);
        }

        Set productFeatureTypeIdsToInclude = null;
        String includeProp = UtilProperties.getPropertyValue("prodsearch", "attach.feature.type.include");
        if (UtilValidate.isNotEmpty(includeProp)) {
            List typeList = StringUtil.split(includeProp, ",");
            if (typeList.size() > 0) {
                productFeatureTypeIdsToInclude = new HashSet(typeList);
            }
        }

        try {
            attachProductFeaturesToCategory(productCategoryId, productFeatureTypeIdsToInclude, productFeatureTypeIdsToExclude, delegator, doSubCategories, nowTimestamp);
        } catch (GenericEntityException e) {
            Map messageMap = UtilMisc.toMap("errMessage", e.toString());
            errMsg = UtilProperties.getMessage(resource,"productutilservices.error_in_attachProductFeaturesToCategory", messageMap, locale);
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }

        return ServiceUtil.returnSuccess();
    }

    /** Get all features associated with products and associate them with a feature group attached to the category for each feature type;
     * includes products associated with this category only, but will also associate all feature groups of sub-categories with this category, optionally calls this method for all sub-categories too
     */
    public static void attachProductFeaturesToCategory(String productCategoryId, Set productFeatureTypeIdsToInclude, Set productFeatureTypeIdsToExclude, GenericDelegator delegator, boolean doSubCategories, Timestamp nowTimestamp) throws GenericEntityException {
        if (nowTimestamp == null) {
            nowTimestamp = UtilDateTime.nowTimestamp();
        }

        // do sub-categories first so all feature groups will be in place
        List subCategoryList = delegator.findByAnd("ProductCategoryRollup", UtilMisc.toMap("parentProductCategoryId", productCategoryId));
        if (doSubCategories) {
            Iterator subCategoryIter = subCategoryList.iterator();
            while (subCategoryIter.hasNext()) {
                GenericValue productCategoryRollup = (GenericValue) subCategoryIter.next();
                attachProductFeaturesToCategory(productCategoryRollup.getString("productCategoryId"), productFeatureTypeIdsToInclude, productFeatureTypeIdsToExclude, delegator, true, nowTimestamp);
            }
        }

        // now get all features for this category and make associated feature groups
        Map productFeatureIdByTypeIdSetMap = new HashMap();
        List productCategoryMemberList = delegator.findByAnd("ProductCategoryMember", UtilMisc.toMap("productCategoryId", productCategoryId));
        Iterator productCategoryMemberIter = productCategoryMemberList.iterator();
        while (productCategoryMemberIter.hasNext()) {
            GenericValue productCategoryMember = (GenericValue) productCategoryMemberIter.next();
            String productId = productCategoryMember.getString("productId");
            EntityCondition condition = new EntityConditionList(UtilMisc.toList(
                    new EntityExpr("productId", EntityOperator.EQUALS, productId),
                    new EntityExpr("fromDate", EntityOperator.LESS_THAN_EQUAL_TO, nowTimestamp),
                    new EntityExpr(new EntityExpr("thruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr("thruDate", EntityOperator.GREATER_THAN_EQUAL_TO, nowTimestamp))
            ), EntityOperator.AND);
            EntityListIterator productFeatureAndApplEli = delegator.findListIteratorByCondition("ProductFeatureAndAppl", condition, null, null);
            GenericValue productFeatureAndAppl = null;
            while ((productFeatureAndAppl = (GenericValue) productFeatureAndApplEli.next()) != null) {
                String productFeatureId = productFeatureAndAppl.getString("productFeatureId");
                String productFeatureTypeId = productFeatureAndAppl.getString("productFeatureTypeId");
                if (productFeatureTypeIdsToInclude != null && productFeatureTypeIdsToInclude.size() > 0 && !productFeatureTypeIdsToInclude.contains(productFeatureTypeId)) {
                    continue;
                }
                if (productFeatureTypeIdsToExclude != null && productFeatureTypeIdsToExclude.contains(productFeatureTypeId)) {
                    continue;
                }
                Set productFeatureIdSet = (Set) productFeatureIdByTypeIdSetMap.get(productFeatureTypeId);
                if (productFeatureIdSet == null) {
                    productFeatureIdSet = new HashSet();
                    productFeatureIdByTypeIdSetMap.put(productFeatureTypeId, productFeatureIdSet);
                }
                productFeatureIdSet.add(productFeatureId);
            }
            productFeatureAndApplEli.close();
        }

        Iterator productFeatureIdByTypeIdSetIter = productFeatureIdByTypeIdSetMap.entrySet().iterator();
        while (productFeatureIdByTypeIdSetIter.hasNext()) {
            Map.Entry entry = (Map.Entry) productFeatureIdByTypeIdSetIter.next();
            String productFeatureTypeId = (String) entry.getKey();
            Set productFeatureIdSet = (Set) entry.getValue();

            String productFeatureGroupId = productCategoryId + "_" + productFeatureTypeId;
            if (productFeatureGroupId.length() > 20) {
                Debug.logWarning("Manufactured productFeatureGroupId was greater than 20 characters, means that we had some long productCategoryId and/or productFeatureTypeId values, at the category part should be unique since it is first, so if the feature type isn't unique it just means more than one type of feature will go into the category...", module);
                productFeatureGroupId = productFeatureGroupId.substring(0, 20);
            }

            GenericValue productFeatureGroup = delegator.findByPrimaryKey("ProductFeatureGroup", UtilMisc.toMap("productFeatureGroupId", productFeatureGroupId));
            if (productFeatureGroup == null) {
                // auto-create the group
                String description = "Feature Group for type [" + productFeatureTypeId + "] features in category [" + productCategoryId + "]";
                productFeatureGroup = delegator.makeValue("ProductFeatureGroup", UtilMisc.toMap("productFeatureGroupId", productFeatureGroupId, "description", description));
                productFeatureGroup.create();

                GenericValue productFeatureCatGrpAppl = delegator.makeValue("ProductFeatureCatGrpAppl", UtilMisc.toMap("productFeatureGroupId", productFeatureGroupId, "productCategoryId", productCategoryId, "fromDate", nowTimestamp));
                productFeatureCatGrpAppl.create();
            }

            // now put all of the features in the group, if there is not already a valid feature placement there...
            Iterator productFeatureIdIter = productFeatureIdSet.iterator();
            while (productFeatureIdIter.hasNext()) {
                String productFeatureId = (String) productFeatureIdIter.next();
                EntityCondition condition = new EntityConditionList(UtilMisc.toList(
                        new EntityExpr("productFeatureId", EntityOperator.EQUALS, productFeatureId),
                        new EntityExpr("productFeatureGroupId", EntityOperator.EQUALS, productFeatureGroupId),
                        new EntityExpr("fromDate", EntityOperator.LESS_THAN_EQUAL_TO, nowTimestamp),
                        new EntityExpr(new EntityExpr("thruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr("thruDate", EntityOperator.GREATER_THAN_EQUAL_TO, nowTimestamp))
                ), EntityOperator.AND);
                if (delegator.findCountByCondition("ProductFeatureGroupAppl", condition, null) == 0) {
                    // if no valid ones, create one
                    GenericValue productFeatureGroupAppl = delegator.makeValue("ProductFeatureGroupAppl", UtilMisc.toMap("productFeatureGroupId", productFeatureGroupId, "productFeatureId", productFeatureId, "fromDate", nowTimestamp));
                    productFeatureGroupAppl.create();
                }
            }
        }

        // now get all feature groups associated with sub-categories and associate them with this category
        Iterator subCategoryIter = subCategoryList.iterator();
        while (subCategoryIter.hasNext()) {
            GenericValue productCategoryRollup = (GenericValue) subCategoryIter.next();
            String subProductCategoryId = productCategoryRollup.getString("productCategoryId");
            EntityCondition condition = new EntityConditionList(UtilMisc.toList(
                    new EntityExpr("productCategoryId", EntityOperator.EQUALS, subProductCategoryId),
                    new EntityExpr("fromDate", EntityOperator.LESS_THAN_EQUAL_TO, nowTimestamp),
                    new EntityExpr(new EntityExpr("thruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr("thruDate", EntityOperator.GREATER_THAN_EQUAL_TO, nowTimestamp))
            ), EntityOperator.AND);
            EntityListIterator productFeatureCatGrpApplEli = delegator.findListIteratorByCondition("ProductFeatureCatGrpAppl", condition, null, null);
            GenericValue productFeatureCatGrpAppl = null;
            while ((productFeatureCatGrpAppl = (GenericValue) productFeatureCatGrpApplEli.next()) != null) {
                String productFeatureGroupId = productFeatureCatGrpAppl.getString("productFeatureGroupId");
                EntityCondition checkCondition = new EntityConditionList(UtilMisc.toList(
                        new EntityExpr("productCategoryId", EntityOperator.EQUALS, productCategoryId),
                        new EntityExpr("productFeatureGroupId", EntityOperator.EQUALS, productFeatureGroupId),
                        new EntityExpr("fromDate", EntityOperator.LESS_THAN_EQUAL_TO, nowTimestamp),
                        new EntityExpr(new EntityExpr("thruDate", EntityOperator.EQUALS, null), EntityOperator.OR, new EntityExpr("thruDate", EntityOperator.GREATER_THAN_EQUAL_TO, nowTimestamp))
                ), EntityOperator.AND);
                if (delegator.findCountByCondition("ProductFeatureCatGrpAppl", checkCondition, null) == 0) {
                    // if no valid ones, create one
                    GenericValue productFeatureGroupAppl = delegator.makeValue("ProductFeatureCatGrpAppl", UtilMisc.toMap("productFeatureGroupId", productFeatureGroupId, "productCategoryId", productCategoryId, "fromDate", nowTimestamp));
                    productFeatureGroupAppl.create();
                }
            }
            productFeatureCatGrpApplEli.close();
        }
    }

    public static Map removeAllFeatureGroupsForCategory(DispatchContext dctx, Map context) {
        return ServiceUtil.returnSuccess();
    }

    public static void getFeatureGroupsForCategory(String productCategoryId, Set productFeatureGroupIdsToRemove, GenericDelegator delegator, boolean doSubCategories, Timestamp nowTimestamp) throws GenericEntityException {

    }
}

