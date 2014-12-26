/*
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
 */

import org.ofbiz.base.util.*
import org.ofbiz.entity.*
import org.ofbiz.entity.condition.*
import org.ofbiz.entity.util.*
import org.ofbiz.product.product.*
import org.ofbiz.entity.util.EntityUtilProperties;

context.nowTimestampString = UtilDateTime.nowTimestamp().toString();

context.assocTypes = from("ProductAssocType").queryList();

context.featureTypes = from("ProductFeatureType").queryList();

// add/remove feature types
addedFeatureTypes = (HashMap) session.getAttribute("addedFeatureTypes");
if (addedFeatureTypes == null) {
    addedFeatureTypes = [:];
    session.setAttribute("addedFeatureTypes", addedFeatureTypes);
}

featuresByType = new HashMap();
String[] addFeatureTypeId = request.getParameterValues("addFeatureTypeId");
List addFeatureTypeIdList = [];
if (addFeatureTypeId) {
    addFeatureTypeIdList.addAll(Arrays.asList(addFeatureTypeId));
}

addFeatureTypeIdIter = addFeatureTypeIdList.iterator();
while (addFeatureTypeIdIter) {
    String curFeatureTypeId = addFeatureTypeIdIter.next();
    GenericValue featureType = from("ProductFeatureType").where("productFeatureTypeId", curFeatureTypeId).queryOne();
    if ((featureType) && !addedFeatureTypes.containsKey(curFeatureTypeId)) {
        addedFeatureTypes.put(curFeatureTypeId, featureType);
    }
}

String[] removeFeatureTypeId = request.getParameterValues("removeFeatureTypeId");
if (removeFeatureTypeId) {
    for (int i = 0; i < removeFeatureTypeId.length; i++) {
        GenericValue featureType = from("ProductFeatureType").where("productFeatureTypeId", addFeatureTypeId[i]).queryOne();
        if ((featureType) && addedFeatureTypes.containsKey(removeFeatureTypeId[i])) {
            addedFeatureTypes.remove(removeFeatureTypeId[i]);
            featuresByType.remove(removeFeatureTypeId[i]);
        }
    }
}
Iterator iter = addedFeatureTypes.values().iterator();
while (iter) {
    GenericValue featureType = (GenericValue)iter.next();
    featuresByType.put(featureType.productFeatureTypeId, featureType.getRelated("ProductFeature", null, ['description'], false));
}

context.addedFeatureTypeIds = addedFeatureTypes.keySet();
context.addedFeatureTypes = addedFeatureTypes;
context.featuresByType = featuresByType;

productId = parameters.get("productId");
if (!productId) {
    productId = parameters.get("PRODUCT_ID");
}
if (!productId) {
    productId = request.getAttribute("productId");
}
if (productId) {
    context.productId = productId;
}

product = from("Product").where("productId", productId).queryOne();
assocProducts = [];
featureFloz = [:];
featureMl = [:];
featureNtwt = [:];
featureGrams = [:];
featureHazmat = [:];
featureSalesThru = [:];
featureThruDate = [:];
selFeatureDesc = [:];
BigDecimal floz = null;
BigDecimal ml = null;
BigDecimal ntwt = null;
BigDecimal grams = null;
String hazmat = "nbsp;";
String salesthru = null;
String thrudate = null;
String productFeatureTypeId = request.getParameter("productFeatureTypeId");
context.productFeatureTypeId = productFeatureTypeId;

if (product) {
    context.product = product;

    // get categories
    allCategories = from("ProductCategory").where(EntityCondition.makeCondition(EntityCondition.makeCondition("showInSelect", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition("showInSelect", EntityOperator.NOT_EQUAL, "N")))
        .orderBy("description").queryList();

    categoryMembers = product.getRelated("ProductCategoryMember", null, null, false);
    categoryMembers = EntityUtil.filterByDate(categoryMembers);
    context.allCategories = allCategories;
    context.productCategoryMembers = categoryMembers;

    productFeatureAndAppls = product.getRelated("ProductFeatureAndAppl", null, null, false);

    // get standard features for this product
    standardFeatureAppls = EntityUtil.filterByAnd(productFeatureAndAppls, [productFeatureApplTypeId : "STANDARD_FEATURE"]);
    productFeatureTypeLookup = [:];
    standardFeatureLookup = [:];
    Iterator standardFeatureApplIter = standardFeatureAppls.iterator();
    while (standardFeatureApplIter) {
        GenericValue standardFeatureAndAppl = (GenericValue) standardFeatureApplIter.next();
        GenericValue featureType = standardFeatureAndAppl.getRelatedOne("ProductFeatureType", true);
        productFeatureTypeLookup.put(standardFeatureAndAppl.getString("productFeatureId"), featureType);
        standardFeatureLookup.put(standardFeatureAndAppl.getString("productFeatureId"), standardFeatureAndAppl);
    }
    context.standardFeatureLookup = standardFeatureLookup;
    context.standardFeatureAppls = standardFeatureAppls;

    // get selectable features for this product
    List selectableFeatureAppls = EntityUtil.filterByAnd(productFeatureAndAppls, [productFeatureApplTypeId : 'SELECTABLE_FEATURE']);
    selectableFeatureLookup = [:];
    // get feature types that are deleteable from selectable features section
    Set selectableFeatureTypes = new HashSet();

    Iterator selectableFeatureAndApplIter = selectableFeatureAppls.iterator();
    while (selectableFeatureAndApplIter) {
        GenericValue selectableFeatureAndAppl = (GenericValue) selectableFeatureAndApplIter.next();
        GenericValue featureType = selectableFeatureAndAppl.getRelatedOne("ProductFeatureType", true);
        productFeatureTypeLookup.put(selectableFeatureAndAppl.productFeatureId, featureType);
        selectableFeatureLookup.put(selectableFeatureAndAppl.productFeatureId, selectableFeatureAndAppl);
        selectableFeatureTypes.add(featureType);
    }
    context.selectableFeatureLookup = selectableFeatureLookup;
    context.selectableFeatureAppls = selectableFeatureAppls;
    context.selectableFeatureTypes = selectableFeatureTypes;

    if ("Y".equalsIgnoreCase(product.isVariant)) {
        Set distinguishingFeatures = ProductWorker.getVariantDistinguishingFeatures(product);
        context.distinguishingFeatures = distinguishingFeatures;
        Iterator distinguishingFeatureIter = distinguishingFeatures.iterator();
        while (distinguishingFeatureIter) {
            distFeature = (GenericValue) distinguishingFeatureIter.next();
            featureType = distFeature.getRelatedOne("ProductFeatureType", true);
            if (!productFeatureTypeLookup.containsKey(distFeature.productFeatureId)) {
                productFeatureTypeLookup.put(distFeature.productFeatureId, featureType);
            }
        }
    }
    context.productFeatureTypeLookup = productFeatureTypeLookup;


    // get shipping dimensions and weights for single product
    prodFeaturesFiltered = EntityUtil.filterByAnd(productFeatureAndAppls, [productFeatureTypeId : 'AMOUNT', uomId : 'VLIQ_ozUS']);
    if (prodFeaturesFiltered) {
        try {
            floz = ((GenericValue)prodFeaturesFiltered.get(0)).getBigDecimal("numberSpecified");
        } catch (Exception e) {
            floz = null;
        }
        context.floz = floz;
    }
    prodFeaturesFiltered = EntityUtil.filterByAnd(productFeatureAndAppls, [productFeatureTypeId : 'AMOUNT', uomId : 'VLIQ_ml']);
    if (prodFeaturesFiltered) {
        try {
            ml = ((GenericValue)prodFeaturesFiltered.get(0)).getBigDecimal("numberSpecified");
        } catch (Exception e) {
            ml = null;
        }
        context.ml = ml;
    }
    prodFeaturesFiltered = EntityUtil.filterByAnd(productFeatureAndAppls, [productFeatureTypeId : 'AMOUNT', uomId : 'WT_g']);
    if (prodFeaturesFiltered) {
        try {
            grams = ((GenericValue)prodFeaturesFiltered.get(0)).getBigDecimal("numberSpecified");
        } catch (Exception e) {
            grams = null;
        }
        context.grams = grams;
    }
    prodFeaturesFiltered = EntityUtil.filterByAnd(productFeatureAndAppls, [productFeatureTypeId : 'AMOUNT', uomId : 'WT_oz']);
    if (prodFeaturesFiltered) {
        try {
            ntwt = ((GenericValue)prodFeaturesFiltered.get(0)).getBigDecimal("numberSpecified");
        } catch (Exception e) {
            ntwt = null;
        }
        context.ntwt = ntwt;
    }
    prodFeaturesFiltered = EntityUtil.filterByAnd(productFeatureAndAppls, [productFeatureTypeId : 'HAZMAT']);
    if (prodFeaturesFiltered) {
        try {
            hazmat = ((GenericValue)prodFeaturesFiltered.get(0)).getString("description");
        } catch (Exception e) {
            hazmat = "nbsp;";
        }
        if (hazmat == null) {
            hazmat = "nbsp;";
        }
        context.hazmat = hazmat;
    }
    java.sql.Timestamp salesThru = product.getTimestamp("salesDiscontinuationDate");
    if (!salesThru) {
        salesthru = "[&nbsp;]";
    } else if (salesThru.after(new java.util.Date())) {
        salesthru = "<span style='color: blue'>[x]</span>";
    } else {
        salesthru = "<span style='color: red'>[x]</span>";
    }
    context.salesthru = salesthru;
    thrudate = "";
    context.thrudate = thrudate;

    // get all variants - associations first
    productAssocs = product.getRelated("MainProductAssoc", [productAssocTypeId : 'PRODUCT_VARIANT'], null, false);
    Iterator productAssocIter = productAssocs.iterator();
    // get shipping dimensions and weights for all the variants
    while (productAssocIter) {
        // now get the variant product
        productAssoc = (GenericValue)productAssocIter.next();
        assocProduct = productAssoc.getRelatedOne("AssocProduct", false);
        if (assocProduct) {
            assocProducts.add(assocProduct);
            assocProductFeatureAndAppls = assocProduct.getRelated("ProductFeatureAndAppl", null, null, false);
            prodFeaturesFiltered = EntityUtil.filterByAnd(assocProductFeatureAndAppls, [productFeatureTypeId : 'AMOUNT', uomId : 'VLIQ_ozUS']);
            if (prodFeaturesFiltered) {
                featureFloz.put(assocProduct.productId, ((GenericValue)prodFeaturesFiltered.get(0)).getBigDecimal("numberSpecified"));
            }
            prodFeaturesFiltered = EntityUtil.filterByAnd(assocProductFeatureAndAppls, [productFeatureTypeId : 'AMOUNT', uomId : 'VLIQ_ml']);
            if (prodFeaturesFiltered) {
                featureMl.put(assocProduct.productId, ((GenericValue)prodFeaturesFiltered.get(0)).getBigDecimal("numberSpecified"));
            }
            prodFeaturesFiltered = EntityUtil.filterByAnd(assocProductFeatureAndAppls, [productFeatureTypeId : 'AMOUNT', uomId : 'WT_g']);
            if (prodFeaturesFiltered) {
                featureGrams.put(assocProduct.productId, ((GenericValue)prodFeaturesFiltered.get(0)).getBigDecimal("numberSpecified"));
            }
            prodFeaturesFiltered = EntityUtil.filterByAnd(assocProductFeatureAndAppls, [productFeatureTypeId : 'AMOUNT', uomId : 'WT_oz']);
            if (prodFeaturesFiltered) {
                featureNtwt.put(assocProduct.productId, ((GenericValue)prodFeaturesFiltered.get(0)).getBigDecimal("numberSpecified"));
            }
            prodFeaturesFiltered = EntityUtil.filterByAnd(assocProductFeatureAndAppls, [productFeatureTypeId : 'HAZMAT']);
            if (prodFeaturesFiltered) {
                featureHazmat.put(assocProduct.productId,
                    ((GenericValue)prodFeaturesFiltered.get(0)).getString("description"));
            } else {
                featureHazmat.put(assocProduct.productId, "&nbsp;");
            }
            salesThru = assocProduct.getTimestamp("salesDiscontinuationDate");
            if (!salesThru) {
                featureSalesThru.put(assocProduct.productId, "<span style='color: blue'>[&nbsp;]</span>");
            } else if (salesThru.after(new java.util.Date())) {
                featureSalesThru.put(assocProduct.productId, "<span style='color: blue'>[x]</span>");
            } else {
                featureSalesThru.put(assocProduct.productId, "<span style='color: red'>[x]</span>");
            }
            java.sql.Timestamp thruDate = productAssoc.getTimestamp("thruDate");
            if (!thruDate) {
                featureThruDate.put(assocProduct.productId, "<span style='color: blue'>[&nbsp;]</span>");
            } else if (thruDate.after(new java.util.Date())) {
                featureThruDate.put(assocProduct.productId, "<span style='color: blue'>[x]</span>");
            } else {
                featureThruDate.put(assocProduct.productId, "<span style='color: red'>[x]</span>");
            }

            prodFeaturesFiltered = EntityUtil.filterByAnd(assocProductFeatureAndAppls, [productFeatureTypeId : productFeatureTypeId]);
            if (prodFeaturesFiltered) {
                // this is used for the selectable feature descriptions section; only include here iff the description is also associated with the virtual product as a selectable feature, ie if this is a distinguishing feature
                String curSelDescription = ((GenericValue) prodFeaturesFiltered.get(0)).getString("description");
                testProductFeatureAndAppls = EntityUtil.filterByAnd(productFeatureAndAppls, [productFeatureTypeId : productFeatureTypeId, description : curSelDescription, productFeatureApplTypeId : 'SELECTABLE_FEATURE']);
                if (testProductFeatureAndAppls) {
                    selFeatureDesc.put(assocProduct.productId, curSelDescription);
                }
            }
        }
    }
    assocProducts = EntityUtil.orderBy(assocProducts, ['internalName']);
    context.assocProducts = assocProducts;
    context.productAssocs = productAssocs;
}

context.featureFloz = featureFloz;
context.featureMl = featureMl;
context.featureNtwt = featureNtwt;
context.featureGrams = featureGrams;
context.featureHazmat = featureHazmat;
context.featureSalesThru = featureSalesThru;
context.featureThruDate = featureThruDate;
context.selFeatureDesc = selFeatureDesc;

// get "all" category id
String allCategoryId = EntityUtilProperties.getPropertyValue("catalog", "all.product.category", delegator);
context.allCategoryId = allCategoryId;

// show the publish or unpublish section
prodCatMembs = from("ProductCategoryMember").where("productCategoryId", allCategoryId, "productId", productId).queryList();
//don't filter by date, show all categories: prodCatMembs = EntityUtil.filterByDate(prodCatMembs);

String showPublish = "false";
if (prodCatMembs.size() == 0) {
    showPublish = "true";
}
context.showPublish = showPublish;
