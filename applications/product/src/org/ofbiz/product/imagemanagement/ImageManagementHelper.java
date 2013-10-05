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

package org.ofbiz.product.imagemanagement;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;

public class ImageManagementHelper {

    static String module = ImageManagementHelper.class.getName();

    public static String getInternalImageUrl(HttpServletRequest request, String productId) {
        String internalImageUrl = null;
        if (request == null) return internalImageUrl; 
        try {
            Delegator delegator = (Delegator) request.getAttribute("delegator");
            List<GenericValue> defaultImageList = delegator.findByAnd("ProductContentAndInfo", UtilMisc.toMap("productId", productId, "productContentTypeId", "DEFAULT_IMAGE", "statusId", "IM_APPROVED", "drIsPublic", "N"), UtilMisc.toList("sequenceNum"), false);
            if (UtilValidate.isNotEmpty(defaultImageList)) {
                GenericValue productContent = EntityUtil.getFirst(defaultImageList);
                if (UtilValidate.isNotEmpty(productContent.get("drObjectInfo"))) {
                    internalImageUrl = (String) productContent.get("drObjectInfo");
                }
            } else {
                List<GenericValue> productContentList = delegator.findByAnd("ProductContentAndInfo", UtilMisc.toMap("productId", productId, "productContentTypeId", "IMAGE", "statusId", "IM_APPROVED", "drIsPublic", "N"), UtilMisc.toList("sequenceNum"), false);
                if (UtilValidate.isNotEmpty(productContentList)) {
                    GenericValue productContent = EntityUtil.getFirst(productContentList);
                    if (UtilValidate.isNotEmpty(productContent.get("drObjectInfo"))) {
                        internalImageUrl = (String) productContent.get("drObjectInfo");
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot get internal image url", module);
        }
        return internalImageUrl;
    }
}
