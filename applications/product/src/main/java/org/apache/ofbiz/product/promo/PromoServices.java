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
package org.apache.ofbiz.product.promo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilIO;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * Promotions Services
 */
public class PromoServices {

    public final static String module = PromoServices.class.getName();
    public static final String resource = "ProductUiLabels";
    private final static char[] smartChars = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y',
            'Z', '2', '3', '4', '5', '6', '7', '8', '9' };

    public static Map<String, Object> createProductPromoCodeSet(DispatchContext dctx, Map<String, ? extends Object> context) {
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Long quantity = (Long) context.get("quantity");
        int codeLength = (Integer) context.get("codeLength");
        String promoCodeLayout = (String) context.get("promoCodeLayout");

        // For PromoCodes we give the option not to use chars that are easy to mix up like 0<>O, 1<>I, ...
        boolean useSmartLayout = false;
        boolean useNormalLayout = false;
        if ("smart".equals(promoCodeLayout)) {
            useSmartLayout = true;
        } else if ("normal".equals(promoCodeLayout)) {
            useNormalLayout = true;
        }

        String newPromoCodeId = "";
        StringBuilder bankOfNumbers = new StringBuilder();
        bankOfNumbers.append(UtilProperties.getMessage(resource, "ProductPromoCodesCreated", locale));
        for (long i = 0; i < quantity; i++) {
            Map<String, Object> createProductPromoCodeMap = null;
            boolean foundUniqueNewCode = false;
            long count = 0;

            while (!foundUniqueNewCode) {
                if (useSmartLayout) {
                    newPromoCodeId = RandomStringUtils.random(codeLength, smartChars);
                } else if (useNormalLayout) {
                    newPromoCodeId = RandomStringUtils.randomAlphanumeric(codeLength);
                }
                GenericValue existingPromoCode = null;
                try {
                    existingPromoCode = EntityQuery.use(delegator).from("ProductPromoCode").where("productPromoCodeId", newPromoCodeId).cache().queryOne();
                }
                catch (GenericEntityException e) {
                    Debug.logWarning("Could not find ProductPromoCode for just generated ID: " + newPromoCodeId, module);
                }
                if (existingPromoCode == null) {
                    foundUniqueNewCode = true;
                }

                count++;
                if (count > 999999) {
                    return ServiceUtil.returnError("Unable to locate unique PromoCode! Length [" + codeLength + "]");
                }
            }
            try {
                Map<String, Object> newContext = dctx.makeValidContext("createProductPromoCode", ModelService.IN_PARAM, context);
                newContext.put("productPromoCodeId", newPromoCodeId);
                createProductPromoCodeMap = dispatcher.runSync("createProductPromoCode", newContext);
            } catch (GenericServiceException err) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ProductPromoCodeCannotBeCreated", locale), null, null, null);
            }
            if (ServiceUtil.isError(createProductPromoCodeMap)) {
                // what to do here? try again?
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ProductPromoCodeCannotBeCreated", locale), null, null, createProductPromoCodeMap);
            }
            bankOfNumbers.append((String) createProductPromoCodeMap.get("productPromoCodeId"));
            bankOfNumbers.append(",");
        }

        return ServiceUtil.returnSuccess(bankOfNumbers.toString());
    }

    public static Map<String, Object> purgeOldStoreAutoPromos(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String productStoreId = (String) context.get("productStoreId");
        Locale locale = (Locale) context.get("locale");
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        List<EntityCondition> condList = new LinkedList<>();
        if (UtilValidate.isEmpty(productStoreId)) {
            condList.add(EntityCondition.makeCondition("productStoreId", EntityOperator.EQUALS, productStoreId));
        }
        condList.add(EntityCondition.makeCondition("userEntered", EntityOperator.EQUALS, "Y"));
        condList.add(EntityCondition.makeCondition("thruDate", EntityOperator.NOT_EQUAL, null));
        condList.add(EntityCondition.makeCondition("thruDate", EntityOperator.LESS_THAN, nowTimestamp));

        try (EntityListIterator eli = EntityQuery.use(delegator).from("ProductStorePromoAndAppl").where(condList).queryIterator()) {
            GenericValue productStorePromoAndAppl = null;
            while ((productStorePromoAndAppl = eli.next()) != null) {
                GenericValue productStorePromo = delegator.makeValue("ProductStorePromoAppl");
                productStorePromo.setAllFields(productStorePromoAndAppl, true, null, null);
                productStorePromo.remove();
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error removing expired ProductStorePromo records: " + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "ProductPromoCodeCannotBeRemoved", UtilMisc.toMap("errorString", e.toString()), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> importPromoCodesFromFile(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");

        // check the uploaded file
        ByteBuffer fileBytes = (ByteBuffer) context.get("uploadedFile");
        if (fileBytes == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "ProductPromoCodeImportUploadedFileNotValid", locale));
        }

        String encoding = System.getProperty("file.encoding");
        String file = Charset.forName(encoding).decode(fileBytes).toString();
        // get the createProductPromoCode Model
        ModelService promoModel;
        try {
            promoModel = dispatcher.getDispatchContext().getModelService("createProductPromoCode");
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // make a temp context for invocations
        Map<String, Object> invokeCtx = promoModel.makeValid(context, ModelService.IN_PARAM);

        // read the bytes into a reader
        BufferedReader reader = new BufferedReader(new StringReader(file));
        List<Object> errors = new LinkedList<>();
        int lines = 0;
        String line;

        // read the uploaded file and process each line
        try {
            while ((line = reader.readLine()) != null) {
                // check to see if we should ignore this line
                if (line.length() > 0 && !line.startsWith("#")) {
                    if (line.length() <= 20) {
                        // valid promo code
                        Map<String, Object> inContext = new HashMap<>();
                        inContext.putAll(invokeCtx);
                        inContext.put("productPromoCodeId", line);
                        Map<String, Object> result = dispatcher.runSync("createProductPromoCode", inContext);
                        if (result != null && ServiceUtil.isError(result)) {
                            errors.add(line + ": " + ServiceUtil.getErrorMessage(result));
                        }
                    } else {
                        // not valid ignore and notify
                        errors.add(line + UtilProperties.getMessage(resource, "ProductPromoCodeInvalidCode", locale));
                    }
                    ++lines;
                }
            }
        } catch (IOException | GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                Debug.logError(e, module);
            }
        }

        // return errors or success
        if (errors.size() > 0) {
            return ServiceUtil.returnError(errors);
        } else if (lines == 0) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "ProductPromoCodeImportEmptyFile", locale));
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> importPromoCodeEmailsFromFile(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String productPromoCodeId = (String) context.get("productPromoCodeId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        ByteBuffer bytebufferwrapper = (ByteBuffer) context.get("uploadedFile");

        if (bytebufferwrapper == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ProductPromoCodeImportUploadedFileNotValid", locale));
        }

        byte[] wrapper = bytebufferwrapper.array();

      // read the bytes into a reader
        BufferedReader reader = new BufferedReader(new StringReader(new String(wrapper, UtilIO.getUtf8())));
        List<Object> errors = new LinkedList<>();
        int lines = 0;
        String line;

        // read the uploaded file and process each line
        try {
            while ((line = reader.readLine()) != null) {
                if (line.length() > 0 && !line.startsWith("#")) {
                    if (UtilValidate.isEmail(line)) {
                        // valid email address
                        Map<String, Object> result = dispatcher.runSync("createProductPromoCodeEmail", UtilMisc.<String, Object>toMap("productPromoCodeId",
                                productPromoCodeId, "emailAddress", line, "userLogin", userLogin));
                        if (result != null && ServiceUtil.isError(result)) {
                            errors.add(line + ": " + ServiceUtil.getErrorMessage(result));
                        }
                    } else {
                        // not valid ignore and notify
                        errors.add(line + ": is not a valid email address");
                    }
                    ++lines;
                }
            }
        } catch (IOException | GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                Debug.logError(e, module);
            }
        }

        // return errors or success
        if (errors.size() > 0) {
            return ServiceUtil.returnError(errors);
        } else if (lines == 0) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "ProductPromoCodeImportEmptyFile", locale));
        }

        return ServiceUtil.returnSuccess();
    }
}
