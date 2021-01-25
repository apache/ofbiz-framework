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
package org.apache.ofbiz.product.product;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericPK;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.model.DynamicViewEntity;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelKeyMap;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * Product Services
 */
public final class ProductUtilServices {

    private static final String MODULE = ProductUtilServices.class.getName();
    private static final String RESOURCE = "ProductUiLabels";
    private static final String RES_ERROR = "ProductErrorUiLabels";

    private ProductUtilServices() {
    }

    /**
     * First expire all ProductAssocs for all disc variants, then disc all virtuals that have all expired variant ProductAssocs
     */
    public static Map<String, Object> discVirtualsWithDiscVariants(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        Locale locale = (Locale) context.get("locale");
        String errMsg = null;
        EntityCondition conditionOne = EntityCondition.makeCondition(UtilMisc.toList(
                EntityCondition.makeCondition("isVariant", EntityOperator.EQUALS, "Y"),
                EntityCondition.makeCondition("salesDiscontinuationDate", EntityOperator.NOT_EQUAL, null),
                EntityCondition.makeCondition("salesDiscontinuationDate", EntityOperator.LESS_THAN_EQUAL_TO, nowTimestamp)), EntityOperator.AND);

        try (EntityListIterator eliOne = EntityQuery.use(delegator).from("Product").where(conditionOne).queryIterator()) {
            GenericValue productOne = null;
            int numSoFarOne = 0;
            while ((productOne = eliOne.next()) != null) {
                String virtualProductId = ProductWorker.getVariantVirtualId(productOne);
                GenericValue virtualProduct = EntityQuery.use(delegator).from("Product").where("productId", virtualProductId).queryOne();
                if (virtualProduct == null) {
                    continue;
                }
                List<GenericValue> passocList = EntityQuery.use(delegator).from("ProductAssoc")
                        .where("productId", virtualProductId, "productIdTo", productOne.get("productId"), "productAssocTypeId", "PRODUCT_VARIANT")
                        .filterByDate()
                        .queryList();
                if (!passocList.isEmpty()) {
                    for (GenericValue passoc : passocList) {
                        passoc.set("thruDate", nowTimestamp);
                        passoc.store();
                    }

                    numSoFarOne++;
                    if (numSoFarOne % 500 == 0) {
                        Debug.logInfo("Expired variant ProductAssocs for " + numSoFarOne + " sales discontinued variant products.", MODULE);
                    }
                }
            }
            // get all non-discontinued virtuals, see if all variant ProductAssocs are expired, if discontinue
            EntityCondition condition = EntityCondition.makeCondition(UtilMisc.toList(
                    EntityCondition.makeCondition("isVirtual", EntityOperator.EQUALS, "Y"),
                    EntityCondition.makeCondition(EntityCondition.makeCondition("salesDiscontinuationDate", EntityOperator.EQUALS, null),
                            EntityOperator.OR, EntityCondition.makeCondition("salesDiscontinuationDate", EntityOperator.GREATER_THAN_EQUAL_TO,
                                    nowTimestamp))), EntityOperator.AND);
            try (EntityListIterator eli = EntityQuery.use(delegator).from("Product").where(condition).queryIterator()) {
                GenericValue product = null;
                int numSoFar = 0;
                while ((product = eli.next()) != null) {
                    List<GenericValue> passocList = EntityQuery.use(delegator).from("ProductAssoc").where("productId", product.get("productId"),
                            "productAssocTypeId", "PRODUCT_VARIANT").filterByDate().queryList();
                    if (passocList.isEmpty()) {
                        product.set("salesDiscontinuationDate", nowTimestamp);
                        delegator.store(product);

                        numSoFar++;
                        if (numSoFar % 500 == 0) {
                            Debug.logInfo("Sales discontinued " + numSoFar + " virtual products that have no valid variants.", MODULE);
                        }
                    }
                }
            } catch (GenericEntityException e) {
                Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.toString());
                errMsg = UtilProperties.getMessage(RES_ERROR, "productutilservices.entity_error_running_discVirtualsWithDiscVariants",
                        messageMap, locale);
                Debug.logError(e, errMsg, MODULE);
                return ServiceUtil.returnError(errMsg);
            }
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.toString());
            errMsg = UtilProperties.getMessage(RES_ERROR, "productutilservices.entity_error_running_discVirtualsWithDiscVariants",
                    messageMap, locale);
            Debug.logError(e, errMsg, MODULE);
            return ServiceUtil.returnError(errMsg);
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * for all disc products, remove from category memberships
     */
    public static Map<String, Object> removeCategoryMembersOfDiscProducts(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        Locale locale = (Locale) context.get("locale");
        String errMsg = null;
        EntityCondition condition = EntityCondition.makeCondition(UtilMisc.toList(
                EntityCondition.makeCondition("salesDiscontinuationDate", EntityOperator.NOT_EQUAL, null),
                EntityCondition.makeCondition("salesDiscontinuationDate", EntityOperator.LESS_THAN_EQUAL_TO, nowTimestamp)), EntityOperator.AND);

        try (EntityListIterator eli = EntityQuery.use(delegator).from("Product").where(condition).queryIterator()) {
            GenericValue product = null;
            int numSoFar = 0;
            while ((product = eli.next()) != null) {
                String productId = product.getString("productId");
                List<GenericValue> productCategoryMemberList = EntityQuery.use(delegator).from("ProductCategoryMember").where("productId",
                        productId).queryList();
                if (!productCategoryMemberList.isEmpty()) {
                    for (GenericValue productCategoryMember : productCategoryMemberList) {
                        // coded this way rather than a removeByAnd so it can be easily changed...
                        productCategoryMember.remove();
                    }
                    numSoFar++;
                    if (numSoFar % 500 == 0) {
                        Debug.logInfo("Removed category members for " + numSoFar + " sales discontinued products.", MODULE);
                    }
                }
            }
            Debug.logInfo("Completed - Removed category members for " + numSoFar + " sales discontinued products.", MODULE);
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.toString());
            errMsg = UtilProperties.getMessage(RES_ERROR, "productutilservices.entity_error_running_removeCategoryMembersOfDiscProducts",
                    messageMap, locale);
            Debug.logError(e, errMsg, MODULE);
            return ServiceUtil.returnError(errMsg);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> removeDuplicateOpenEndedCategoryMembers(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        Locale locale = (Locale) context.get("locale");
        String errMsg = null;
        DynamicViewEntity dve = new DynamicViewEntity();
        dve.addMemberEntity("PCM", "ProductCategoryMember");
        dve.addAlias("PCM", "productId", null, null, null, Boolean.TRUE, null);
        dve.addAlias("PCM", "productCategoryId", null, null, null, Boolean.TRUE, null);
        dve.addAlias("PCM", "fromDate", null, null, null, null, null);
        dve.addAlias("PCM", "thruDate", null, null, null, null, null);
        dve.addAlias("PCM", "productIdCount", "productId", null, null, null, "count");

        EntityCondition condition = EntityCondition.makeCondition(UtilMisc.toList(
                EntityCondition.makeCondition("fromDate", EntityOperator.LESS_THAN, nowTimestamp),
                EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null)), EntityOperator.AND);
        EntityCondition havingCond = EntityCondition.makeCondition("productIdCount", EntityOperator.GREATER_THAN, 1L);

        try (EntityListIterator eli = EntityQuery.use(delegator).select("productId", "productCategoryId", "productIdCount").from(dve)
                .where(condition).having(havingCond).queryIterator()) {
            GenericValue pcm = null;
            int numSoFar = 0;
            while ((pcm = eli.next()) != null) {
                List<GenericValue> productCategoryMemberList = EntityQuery.use(delegator).from("ProductCategoryMember").where("productId",
                        pcm.get("productId"), "productCategoryId", pcm.get("productCategoryId")).queryList();
                if (productCategoryMemberList.size() > 1) {
                    // remove all except the first...
                    productCategoryMemberList.remove(0);
                    for (GenericValue productCategoryMember : productCategoryMemberList) {
                        productCategoryMember.remove();
                    }
                    numSoFar++;
                    if (numSoFar % 500 == 0) {
                        Debug.logInfo("Removed category members for " + numSoFar + " products with duplicate category members.", MODULE);
                    }
                }
            }
            Debug.logInfo("Completed - Removed category members for " + numSoFar + " products with duplicate category members.", MODULE);
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.toString());
            errMsg = UtilProperties.getMessage(RES_ERROR, "productutilservices.entity_error_running_removeDuplicateOpenEndedCategoryMembers",
                    messageMap, locale);
            Debug.logError(e, errMsg, MODULE);
            return ServiceUtil.returnError(errMsg);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> makeStandAloneFromSingleVariantVirtuals(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        Locale locale = (Locale) context.get("locale");
        String errMsg = null;

        Debug.logInfo("Starting makeStandAloneFromSingleVariantVirtuals", MODULE);

        DynamicViewEntity dve = new DynamicViewEntity();
        dve.addMemberEntity("PVIRT", "Product");
        dve.addMemberEntity("PVA", "ProductAssoc");
        dve.addViewLink("PVIRT", "PVA", Boolean.FALSE, UtilMisc.toList(new ModelKeyMap("productId", "productId")));
        dve.addAlias("PVIRT", "productId", null, null, null, Boolean.TRUE, null);
        dve.addAlias("PVIRT", "salesDiscontinuationDate", null, null, null, null, null);
        dve.addAlias("PVA", "productAssocTypeId", null, null, null, null, null);
        dve.addAlias("PVA", "fromDate", null, null, null, null, null);
        dve.addAlias("PVA", "thruDate", null, null, null, null, null);
        dve.addAlias("PVA", "productIdToCount", "productIdTo", null, null, null, "count-distinct");
        EntityCondition condition = EntityCondition.makeCondition(UtilMisc.toList(
                EntityCondition.makeCondition("productAssocTypeId", EntityOperator.EQUALS, "PRODUCT_VARIANT"),
                EntityCondition.makeCondition(EntityCondition.makeCondition("salesDiscontinuationDate", EntityOperator.EQUALS, null),
                        EntityOperator.OR,
                        EntityCondition.makeCondition("salesDiscontinuationDate", EntityOperator.GREATER_THAN, nowTimestamp))), EntityOperator.AND);
        EntityCondition havingCond = EntityCondition.makeCondition("productIdToCount", EntityOperator.EQUALS, 1L);
        EntityQuery eq = EntityQuery.use(delegator)
                .select("productId", "productIdToCount")
                .from(dve)
                .where(condition)
                .having(havingCond);

        try (EntityListIterator eliOne = eq.queryIterator()) {
            List<GenericValue> valueList = eliOne.getCompleteList();

            Debug.logInfo("Found " + valueList.size() + " virtual products with one variant to turn into a stand alone product.", MODULE);

            int numWithOneOnly = 0;
            for (GenericValue value : valueList) {
                // has only one variant period, is it valid? should already be discontinued if not
                String productId = value.getString("productId");
                List<GenericValue> paList = EntityQuery.use(delegator).from("ProductAssoc").where("productId", productId, "productAssocTypeId",
                        "PRODUCT_VARIANT").filterByDate().queryList();
                // verify the query; tested on a bunch, looks good
                if (paList.size() != 1) {
                    Debug.logInfo("Virtual product with ID " + productId + " should have 1 assoc, has " + paList.size(), MODULE);
                } else {
                    // for all virtuals with one variant move all info from virtual to variant and remove virtual, make variant as not a variant
                    dispatcher.runSync("mergeVirtualWithSingleVariant", UtilMisc.<String, Object>toMap("productId", productId, "removeOld",
                            Boolean.TRUE, "userLogin", userLogin));
                    numWithOneOnly++;
                    if (numWithOneOnly % 100 == 0) {
                        Debug.logInfo("Made " + numWithOneOnly + " virtual products with only one valid variant stand-alone products.", MODULE);
                    }
                }
            }

            EntityCondition conditionWithDates = EntityCondition.makeCondition(UtilMisc.toList(
                    EntityCondition.makeCondition("productAssocTypeId", EntityOperator.EQUALS, "PRODUCT_VARIANT"),
                    EntityCondition.makeCondition(EntityCondition.makeCondition("salesDiscontinuationDate", EntityOperator.EQUALS, null),
                            EntityOperator.OR, EntityCondition.makeCondition("salesDiscontinuationDate", EntityOperator.GREATER_THAN, nowTimestamp)),
                    EntityCondition.makeCondition("fromDate", EntityOperator.LESS_THAN_EQUAL_TO, nowTimestamp),
                    EntityCondition.makeCondition(EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null), EntityOperator.OR,
                            EntityCondition.makeCondition("thruDate", EntityOperator.GREATER_THAN_EQUAL_TO, nowTimestamp))), EntityOperator.AND);
            eq = EntityQuery.use(delegator).select("productId", "productIdToCount").from(dve)
                    .where(conditionWithDates).having(havingCond);
            try (EntityListIterator eliMulti = eq.queryIterator()) {
                List<GenericValue> valueMultiList = eliMulti.getCompleteList();
                Debug.logInfo("Found " + valueMultiList.size() + " virtual products with one VALID variant to pull the variant from "
                        + "to make a stand alone product.", MODULE);

                int numWithOneValid = 0;
                for (GenericValue value : valueMultiList) {
                    // has only one valid variant
                    String productId = value.getString("productId");

                    List<GenericValue> paList = EntityQuery.use(delegator).from("ProductAssoc").where("productId", productId, "productAssocTypeId",
                            "PRODUCT_VARIANT").filterByDate().queryList();

                    // verify the query; tested on a bunch, looks good
                    if (paList.size() != 1) {
                        Debug.logInfo("Virtual product with ID " + productId + " should have 1 assoc, has " + paList.size(), MODULE);
                    } else {
                        // for all virtuals with one valid variant move info from virtual to variant, put variant in categories from virtual, remove
                        // virtual from all categories but leave "family" otherwise intact, mark variant as not a variant
                        dispatcher.runSync("mergeVirtualWithSingleVariant", UtilMisc.<String, Object>toMap("productId", productId, "removeOld",
                                Boolean.FALSE, "userLogin", userLogin));

                        numWithOneValid++;
                        if (numWithOneValid % 100 == 0) {
                            Debug.logInfo("Made " + numWithOneValid + " virtual products with one valid variant stand-alone products.", MODULE);
                        }
                    }
                }
                Debug.logInfo("Found virtual products with one valid variant: " + numWithOneValid + ", with one variant only: " + numWithOneOnly,
                        MODULE);
            } catch (GenericEntityException e) {
                Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.toString());
                errMsg = UtilProperties.getMessage(RES_ERROR, "productutilservices.entity_error_running_makeStandAloneFromSingleVariantVirtuals",
                        messageMap, locale);
                Debug.logError(e, errMsg, MODULE);
                return ServiceUtil.returnError(errMsg);
            }
        } catch (GenericEntityException | GenericServiceException e) {
            Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.toString());
            errMsg = UtilProperties.getMessage(RES_ERROR, "productutilservices.entity_error_running_makeStandAloneFromSingleVariantVirtuals",
                    messageMap, locale);
            Debug.logError(e, errMsg, MODULE);
            return ServiceUtil.returnError(errMsg);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> mergeVirtualWithSingleVariant(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        String productId = (String) context.get("productId");
        Boolean removeOldBool = (Boolean) context.get("removeOld");
        boolean removeOld = removeOldBool;
        Locale locale = (Locale) context.get("locale");
        String errMsg = null;

        Boolean testBool = (Boolean) context.get("test");
        boolean test = false;
        if (testBool != null) {
            test = testBool;
        }

        try {
            GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
            Debug.logInfo("Processing virtual product with one variant with ID: " + productId + " and name: "
                    + product.getString("internalName"), MODULE);

            List<GenericValue> paList = EntityQuery.use(delegator).from("ProductAssoc").where("productId", productId, "productAssocTypeId",
                    "PRODUCT_VARIANT").filterByDate().queryList();
            if (paList.size() > 1) {
                Map<String, String> messageMap = UtilMisc.toMap("productId", productId);
                errMsg = UtilProperties.getMessage(RES_ERROR, "productutilservices.found_more_than_one_valid_variant_for_virtual_ID",
                        messageMap, locale);
                Debug.logInfo(errMsg, MODULE);
                return ServiceUtil.returnError(errMsg);
            }

            if (paList.isEmpty()) {
                Map<String, String> messageMap = UtilMisc.toMap("productId", productId);
                errMsg = UtilProperties.getMessage(RES_ERROR, "productutilservices.did_not_find_any_valid_variants_for_virtual_ID",
                        messageMap, locale);
                Debug.logInfo(errMsg, MODULE);
                return ServiceUtil.returnError(errMsg);
            }

            GenericValue productAssoc = EntityUtil.getFirst(paList);
            if (removeOld) {
                // remove the productAssoc before getting down so it isn't copied over...
                if (test) {
                    Debug.logInfo("Test mode, would remove: " + productAssoc, MODULE);
                } else {
                    productAssoc.remove();
                }
            } else {
                // don't remove, just expire to avoid running again in the future
                productAssoc.set("thruDate", nowTimestamp);
                if (test) {
                    Debug.logInfo("Test mode, would store: " + productAssoc, MODULE);
                } else {
                    productAssoc.store();
                }
            }
            String variantProductId = productAssoc.getString("productIdTo");

            // Product
            GenericValue variantProduct = EntityQuery.use(delegator).from("Product").where("productId", variantProductId).queryOne();

            Debug.logInfo("--variant has ID: " + variantProductId + " and name: " + variantProduct.getString("internalName"), MODULE);

            // start with the values from the virtual product, override from the variant...
            GenericValue newVariantProduct = delegator.makeValue("Product", product);
            newVariantProduct.setAllFields(variantProduct, false, "", null);
            newVariantProduct.set("isVariant", "N");
            if (test) {
                Debug.logInfo("Test mode, would store: " + newVariantProduct, MODULE);
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
                    Debug.logInfo("Test mode, would remove related ProductKeyword with dummy key: "
                            + product.getRelatedDummyPK("ProductKeyword"), MODULE);
                    Debug.logInfo("Test mode, would remove: " + product, MODULE);
                } else {
                    product.removeRelated("ProductKeyword");
                    product.remove();
                }
            }

            if (test) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "ProductMergeVirtualWithSingleVariant", locale));
            }
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.toString());
            errMsg = UtilProperties.getMessage(RES_ERROR, "productutilservices.entity_error_running_makeStandAloneFromSingleVariantVirtuals",
                    messageMap, locale);
            Debug.logError(e, errMsg, MODULE);
            return ServiceUtil.returnError(errMsg);
        }

        return ServiceUtil.returnSuccess();
    }

    protected static void duplicateRelated(GenericValue product, String title, String relatedEntityName, String productIdField, String
            variantProductId, Timestamp nowTimestamp, boolean removeOld, Delegator delegator, boolean test) throws GenericEntityException {
        List<GenericValue> relatedList = EntityUtil.filterByDate(product.getRelated(title + relatedEntityName, null, null, false), nowTimestamp);
        for (GenericValue relatedValue : relatedList) {
            GenericValue newRelatedValue = (GenericValue) relatedValue.clone();
            newRelatedValue.set(productIdField, variantProductId);

            // create a new one? see if one already exists with different from/thru dates
            ModelEntity modelEntity = relatedValue.getModelEntity();
            if (modelEntity.isField("fromDate")) {
                GenericPK findValue = newRelatedValue.getPrimaryKey();
                // can't just set to null, need to remove the value so it isn't a constraint in the query
                findValue.remove("fromDate");
                List<GenericValue> existingValueList = EntityQuery.use(delegator).from(relatedEntityName).where(findValue)
                        .filterByDate(nowTimestamp).queryList();
                if (!existingValueList.isEmpty()) {
                    if (test) {
                        Debug.logInfo("Found " + existingValueList.size() + " existing values for related entity name: "
                                + relatedEntityName + ", not copying, findValue is: " + findValue, MODULE);
                    }
                    continue;
                }
                newRelatedValue.set("fromDate", nowTimestamp);
            }

            if (EntityQuery.use(delegator).from(relatedEntityName).where(EntityCondition.makeCondition(newRelatedValue.getPrimaryKey(),
                    EntityOperator.AND)).queryCount() == 0) {
                if (test) {
                    Debug.logInfo("Test mode, would create: " + newRelatedValue, MODULE);
                } else {
                    newRelatedValue.create();
                }
            }
        }
        if (removeOld) {
            if (test) {
                Debug.logInfo("Test mode, would remove related " + title + relatedEntityName + " with dummy key: "
                        + product.getRelatedDummyPK(title + relatedEntityName), MODULE);
            } else {
                product.removeRelated(title + relatedEntityName);
            }
        }
    }


    /**
     * reset all product image names with a certain pattern, ex: /images/products/${size}/${productId}.jpg
     * NOTE: only works on fields of Product right now
     */
    public static Map<String, Object> setAllProductImageNames(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String pattern = (String) context.get("pattern");
        Locale locale = (Locale) context.get("locale");
        String errMsg = null;

        if (UtilValidate.isEmpty(pattern)) {
            Map<String, Object> imageContext = new HashMap<>();
            imageContext.putAll(context);
            imageContext.put("tenantId", delegator.getDelegatorTenantId());
            String imageFilenameFormat = EntityUtilProperties.getPropertyValue("catalog", "image.filename.format", delegator);
            String imageUrlPrefix = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue("catalog",
                    "image.url.prefix", delegator), imageContext);
            imageUrlPrefix = imageUrlPrefix.endsWith("/") ? imageUrlPrefix.substring(0, imageUrlPrefix.length() - 1) : imageUrlPrefix;
            pattern = imageUrlPrefix + "/" + imageFilenameFormat;
        }

        try (EntityListIterator eli = EntityQuery.use(delegator).from("Product").queryIterator()) {
            GenericValue product = null;
            int numSoFar = 0;
            while ((product = eli.next()) != null) {
                String productId = (String) product.get("productId");
                Map<String, String> smallMap = UtilMisc.toMap("size", "small", "productId", productId);
                Map<String, String> mediumMap = UtilMisc.toMap("size", "medium", "productId", productId);
                Map<String, String> largeMap = UtilMisc.toMap("size", "large", "productId", productId);
                Map<String, String> detailMap = UtilMisc.toMap("size", "detail", "productId", productId);

                if ("Y".equals(product.getString("isVirtual"))) {
                    // find the first variant, use it's ID for the names...
                    List<GenericValue> productAssocList = EntityQuery.use(delegator).from("ProductAssoc").where("productId",
                            productId, "productAssocTypeId", "PRODUCT_VARIANT").filterByDate().queryList();
                    if (!productAssocList.isEmpty()) {
                        GenericValue productAssoc = EntityUtil.getFirst(productAssocList);
                        smallMap.put("productId", productAssoc.getString("productIdTo"));
                        mediumMap.put("productId", productAssoc.getString("productIdTo"));
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
                    Debug.logInfo("Image URLs set for " + numSoFar + " products.", MODULE);
                }
            }
            Debug.logInfo("Completed - Image URLs set for " + numSoFar + " products.", MODULE);
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.toString());
            errMsg = UtilProperties.getMessage(RES_ERROR, "productutilservices.entity_error_running_setAllProductImageNames", messageMap, locale);
            Debug.logError(e, errMsg, MODULE);
            return ServiceUtil.returnError(errMsg);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> clearAllVirtualProductImageNames(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String errMsg = null;

        try (EntityListIterator eli = EntityQuery.use(delegator).from("Product").where("isVirtual", "Y").queryIterator()) {
            GenericValue product = null;
            int numSoFar = 0;
            while ((product = eli.next()) != null) {
                product.set("smallImageUrl", null);
                product.set("mediumImageUrl", null);
                product.set("largeImageUrl", null);
                product.set("detailImageUrl", null);
                product.store();
                numSoFar++;
                if (numSoFar % 500 == 0) {
                    Debug.logInfo("Image URLs cleared for " + numSoFar + " products.", MODULE);
                }
            }
            Debug.logInfo("Completed - Image URLs set for " + numSoFar + " products.", MODULE);
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.toString());
            errMsg = UtilProperties.getMessage(RES_ERROR, "productutilservices.entity_error_running_clearAllVirtualProductImageNames",
                    messageMap, locale);
            Debug.logError(e, errMsg, MODULE);
            return ServiceUtil.returnError(errMsg);
        }

        return ServiceUtil.returnSuccess();
    }


    public static Map<String, Object> attachProductFeaturesToCategory(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String productCategoryId = (String) context.get("productCategoryId");
        String doSubCategoriesStr = (String) context.get("doSubCategories");
        Locale locale = (Locale) context.get("locale");
        String errMsg = null;

        // default to true
        boolean doSubCategories = !"N".equals(doSubCategoriesStr);
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        Set<String> productFeatureTypeIdsToExclude = new HashSet<>();
        String excludeProp = EntityUtilProperties.getPropertyValue("prodsearch", "attach.feature.type.exclude", delegator);
        if (UtilValidate.isNotEmpty(excludeProp)) {
            List<String> typeList = StringUtil.split(excludeProp, ",");
            productFeatureTypeIdsToExclude.addAll(typeList);
        }

        Set<String> productFeatureTypeIdsToInclude = null;
        String includeProp = EntityUtilProperties.getPropertyValue("prodsearch", "attach.feature.type.include", delegator);
        if (UtilValidate.isNotEmpty(includeProp)) {
            List<String> typeList = StringUtil.split(includeProp, ",");
            if (!typeList.isEmpty()) {
                productFeatureTypeIdsToInclude = new LinkedHashSet<>(typeList);
            }
        }

        try {
            attachProductFeaturesToCategory(productCategoryId, productFeatureTypeIdsToInclude, productFeatureTypeIdsToExclude,
                    delegator, doSubCategories, nowTimestamp);
        } catch (GenericEntityException e) {
            Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.toString());
            errMsg = UtilProperties.getMessage(RES_ERROR, "productutilservices.error_in_attachProductFeaturesToCategory", messageMap, locale);
            Debug.logError(e, errMsg, MODULE);
            return ServiceUtil.returnError(errMsg);
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Get all features associated with products and associate them with a feature group attached to the category for each feature type;
     * includes products associated with this category only, but will also associate all feature groups of sub-categories with this category,
     * optionally calls this method for all sub-categories too
     */
    public static void attachProductFeaturesToCategory(String productCategoryId, Set<String> productFeatureTypeIdsToInclude, Set<String>
            productFeatureTypeIdsToExclude, Delegator delegator, boolean doSubCategories, Timestamp nowTimestamp) throws GenericEntityException {
        if (nowTimestamp == null) {
            nowTimestamp = UtilDateTime.nowTimestamp();
        }

        // do sub-categories first so all feature groups will be in place
        List<GenericValue> subCategoryList = EntityQuery.use(delegator).from("ProductCategoryRollup").where("parentProductCategoryId",
                productCategoryId).queryList();
        if (doSubCategories) {
            for (GenericValue productCategoryRollup : subCategoryList) {
                attachProductFeaturesToCategory(productCategoryRollup.getString("productCategoryId"), productFeatureTypeIdsToInclude,
                        productFeatureTypeIdsToExclude, delegator, true, nowTimestamp);
            }
        }

        // now get all features for this category and make associated feature groups
        Map<String, Set<String>> productFeatureIdByTypeIdSetMap = new HashMap<>();
        List<GenericValue> productCategoryMemberList = EntityQuery.use(delegator).from("ProductCategoryMember").where("productCategoryId",
                productCategoryId).queryList();
        for (GenericValue productCategoryMember : productCategoryMemberList) {
            String productId = productCategoryMember.getString("productId");
            EntityCondition condition = EntityCondition.makeCondition(UtilMisc.toList(
                    EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId),
                    EntityCondition.makeCondition("fromDate", EntityOperator.LESS_THAN_EQUAL_TO, nowTimestamp),
                    EntityCondition.makeCondition(EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null),
                            EntityOperator.OR, EntityCondition.makeCondition("thruDate", EntityOperator.GREATER_THAN_EQUAL_TO,
                                    nowTimestamp))), EntityOperator.AND);

            try (EntityListIterator productFeatureAndApplEli = EntityQuery.use(delegator).from("ProductFeatureAndAppl")
                    .where(condition).queryIterator()) {
                GenericValue productFeatureAndAppl = null;
                while ((productFeatureAndAppl = productFeatureAndApplEli.next()) != null) {
                    String productFeatureId = productFeatureAndAppl.getString("productFeatureId");
                    String productFeatureTypeId = productFeatureAndAppl.getString("productFeatureTypeId");
                    if (UtilValidate.isNotEmpty(productFeatureTypeIdsToInclude) && !productFeatureTypeIdsToInclude.contains(productFeatureTypeId)) {
                        continue;
                    }
                    if (productFeatureTypeIdsToExclude != null && productFeatureTypeIdsToExclude.contains(productFeatureTypeId)) {
                        continue;
                    }
                    Set<String> productFeatureIdSet = productFeatureIdByTypeIdSetMap.get(productFeatureTypeId);
                    if (productFeatureIdSet == null) {
                        productFeatureIdSet = new HashSet<>();
                        productFeatureIdByTypeIdSetMap.put(productFeatureTypeId, productFeatureIdSet);
                    }
                    productFeatureIdSet.add(productFeatureId);
                }

                for (Map.Entry<String, Set<String>> entry : productFeatureIdByTypeIdSetMap.entrySet()) {
                    String productFeatureTypeId = entry.getKey();
                    Set<String> productFeatureIdSet = entry.getValue();

                    String productFeatureGroupId = productCategoryId + "_" + productFeatureTypeId;
                    if (productFeatureGroupId.length() > 20) {
                        Debug.logWarning("Manufactured productFeatureGroupId was greater than 20 characters, means that we had some long"
                                + "productCategoryId and/or productFeatureTypeId values, at the category part should be unique since it is first,"
                                + "so if the feature type isn't unique it just means more than one type of feature will go into the category...",
                                MODULE);
                        productFeatureGroupId = productFeatureGroupId.substring(0, 20);
                    }

                    GenericValue productFeatureGroup = EntityQuery.use(delegator).from("ProductFeatureGroup").where("productFeatureGroupId",
                            productFeatureGroupId).queryOne();
                    if (productFeatureGroup == null) {
                        // auto-create the group
                        String description = "Feature Group for type [" + productFeatureTypeId + "] features in category [" + productCategoryId + "]";
                        productFeatureGroup = delegator.makeValue("ProductFeatureGroup", UtilMisc.toMap("productFeatureGroupId",
                                productFeatureGroupId, "description", description));
                        productFeatureGroup.create();

                        GenericValue productFeatureCatGrpAppl = delegator.makeValue("ProductFeatureCatGrpAppl",
                                UtilMisc.toMap("productFeatureGroupId", productFeatureGroupId, "productCategoryId", productCategoryId,
                                        "fromDate", nowTimestamp));
                        productFeatureCatGrpAppl.create();
                    }

                    // now put all of the features in the group, if there is not already a valid feature placement there...
                    for (String productFeatureId : productFeatureIdSet) {
                        condition = EntityCondition.makeCondition(UtilMisc.toList(
                                EntityCondition.makeCondition("productFeatureId", EntityOperator.EQUALS, productFeatureId),
                                EntityCondition.makeCondition("productFeatureGroupId", EntityOperator.EQUALS, productFeatureGroupId),
                                EntityCondition.makeCondition("fromDate", EntityOperator.LESS_THAN_EQUAL_TO, nowTimestamp),
                                EntityCondition.makeCondition(EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null),
                                        EntityOperator.OR, EntityCondition.makeCondition("thruDate", EntityOperator.GREATER_THAN_EQUAL_TO,
                                                nowTimestamp))), EntityOperator.AND);
                        if (EntityQuery.use(delegator).from("ProductFeatureGroupAppl").where(condition).queryCount() == 0) {
                            // if no valid ones, create one
                            GenericValue productFeatureGroupAppl = delegator.makeValue("ProductFeatureGroupAppl",
                                    UtilMisc.toMap("productFeatureGroupId", productFeatureGroupId, "productFeatureId", productFeatureId,
                                            "fromDate", nowTimestamp));
                            productFeatureGroupAppl.create();
                        }
                    }
                }

                // now get all feature groups associated with sub-categories and associate them with this category
                for (GenericValue productCategoryRollup : subCategoryList) {
                    String subProductCategoryId = productCategoryRollup.getString("productCategoryId");
                    condition = EntityCondition.makeCondition(UtilMisc.toList(
                            EntityCondition.makeCondition("productCategoryId", EntityOperator.EQUALS, subProductCategoryId),
                            EntityCondition.makeCondition("fromDate", EntityOperator.LESS_THAN_EQUAL_TO, nowTimestamp),
                            EntityCondition.makeCondition(EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null), EntityOperator.OR,
                                    EntityCondition.makeCondition("thruDate", EntityOperator.GREATER_THAN_EQUAL_TO, nowTimestamp))),
                            EntityOperator.AND);
                    try (EntityListIterator productFeatureCatGrpApplEli = EntityQuery.use(delegator).from("ProductFeatureCatGrpAppl")
                            .where(condition).queryIterator()) {
                        GenericValue productFeatureCatGrpAppl = null;
                        while ((productFeatureCatGrpAppl = productFeatureCatGrpApplEli.next()) != null) {
                            String productFeatureGroupId = productFeatureCatGrpAppl.getString("productFeatureGroupId");
                            EntityCondition checkCondition = EntityCondition.makeCondition(UtilMisc.toList(
                                    EntityCondition.makeCondition("productCategoryId", EntityOperator.EQUALS, productCategoryId),
                                    EntityCondition.makeCondition("productFeatureGroupId", EntityOperator.EQUALS, productFeatureGroupId),
                                    EntityCondition.makeCondition("fromDate", EntityOperator.LESS_THAN_EQUAL_TO, nowTimestamp),
                                    EntityCondition.makeCondition(EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null),
                                            EntityOperator.OR, EntityCondition.makeCondition("thruDate", EntityOperator.GREATER_THAN_EQUAL_TO,
                                                    nowTimestamp))), EntityOperator.AND);
                            if (EntityQuery.use(delegator).from("ProductFeatureCatGrpAppl").where(checkCondition).queryCount() == 0) {
                                // if no valid ones, create one
                                GenericValue productFeatureGroupAppl = delegator.makeValue("ProductFeatureCatGrpAppl",
                                        UtilMisc.toMap("productFeatureGroupId", productFeatureGroupId, "productCategoryId", productCategoryId,
                                                "fromDate", nowTimestamp));
                                productFeatureGroupAppl.create();
                            }
                        }
                    }
                }
            }
        }
    }
}

