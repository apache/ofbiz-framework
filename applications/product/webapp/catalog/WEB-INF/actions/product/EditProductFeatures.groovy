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

import org.ofbiz.entity.*;


productFeatureAndAppls = delegator.findByAnd('ProductFeatureAndAppl',
        ['productId' : productId],
        ['sequenceNum', 'productFeatureApplTypeId', 'productFeatureTypeId', 'description']);
if (productFeatureAndAppls != null) {
    context.put('productFeatureAndAppls', productFeatureAndAppls);
}

productFeatureCategories = delegator.findAll('ProductFeatureCategory', ['description']);
if (productFeatureCategories != null) {
    context.put('productFeatureCategories', productFeatureCategories);
}

productFeatureApplTypes = delegator.findAll('ProductFeatureApplType', ['description']);
if (productFeatureApplTypes != null) {
    context.put('productFeatureApplTypes', productFeatureApplTypes);
}

productFeatureGroups = delegator.findAll('ProductFeatureGroup', ['description']);
if (productFeatureGroups != null) {
    context.put('productFeatureGroups', productFeatureGroups);
}

productFeatureTypes = delegator.findAll('ProductFeatureType', ['description']);
if (productFeatureTypes != null) {
    context.put('productFeatureTypes', productFeatureTypes);
}

