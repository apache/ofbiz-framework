package com.simbaquartz.xapi.helper;

import com.fidelissd.zcp.xcommon.collections.FastList;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import org.apache.commons.codec.binary.Base64;
import org.apache.ofbiz.base.conversion.ConversionException;
import org.apache.ofbiz.base.conversion.JSONConverters;
import org.apache.ofbiz.base.lang.JSON;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.party.contact.ContactMechWorker;
import org.apache.ofbiz.product.product.ProductWorker;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import java.io.*;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.util.*;

public class AvalaraTaxHelper {
    private LocalDispatcher dispatcher;
    private Delegator delegator;
    private Locale locale;
    private GenericValue userLogin;
    public static final String module = AvalaraTaxHelper.class.getName();

    public AvalaraTaxHelper(LocalDispatcher dispatcher, Delegator delegator, GenericValue userLogin, Locale locale) {
        this.delegator = delegator;
        this.dispatcher = dispatcher;
        this.userLogin = userLogin;
        this.locale=locale;
    }

    public boolean isValidShippingAddress(Map address) {
        if (UtilValidate.isEmpty(address)) {
            return false;
        }
        if (UtilValidate.isNotEmpty(address.get("postalCode"))) {
            return true;
        }
        if (UtilValidate.isEmpty(address.get("address1")) || UtilValidate.isEmpty(address.get("city")) || UtilValidate.isEmpty(address.get("stateProvinceGeoId"))) {
            return false;
        }
        return true;
    }

    public Map<String, Object> getCalcTaxRequestMap(Map reqParamMap) {
        Map reqMap = FastMap.newInstance();
        GenericValue shipFromAddress = (GenericValue) reqParamMap.get("shipFromAddress");
        if(UtilValidate.isEmpty(shipFromAddress)) {
            shipFromAddress = getFacilityAddress((String) reqParamMap.get("facilityId"));
        }
        GenericValue shippingAddress = (GenericValue) reqParamMap.get("shippingAddress");
        Date currentDate = new java.sql.Date(Calendar.getInstance().getTimeInMillis());
        reqMap.put("companyCode", getCompanyCode((String) reqParamMap.get("productStoreId")));
        reqMap.put("type", "SalesOrder");
        reqMap.put("date", currentDate.toString());
        reqMap.put("customerCode", reqParamMap.get("billToPartyId"));
        reqMap.put("addresses", getAddressList(shippingAddress, shipFromAddress));
        reqMap.put("lines", getLinesForCalcTax(reqParamMap));
        return reqMap;
    }

    public GenericValue getFacilityAddress(String facilityId) {
        GenericValue facilityAddress = null;
        try {
            GenericValue facilityContactMech = ContactMechWorker.getFacilityContactMechByPurpose(delegator, facilityId, UtilMisc.toList("SHIP_ORIG_LOCATION"));
            if (UtilValidate.isNotEmpty(facilityContactMech)) {
                facilityAddress = delegator.findOne("PostalAddress", UtilMisc.toMap("contactMechId", facilityContactMech.getString("contactMechId")), false);
            }
        } catch (GenericEntityException e) {
            Debug.logError("Failed to get Facility Address ["+ facilityId+ "] due to exception " + e, module);
        }
        return facilityAddress;
    }

    private String getCompanyCode(String productStoreId) {
        String companyCode = null;
        if(UtilValidate.isNotEmpty(productStoreId)){
            try {
                GenericValue productStore = delegator.findOne("ProductStore", UtilMisc.toMap("productStoreId", productStoreId),false);
                if(UtilValidate.isNotEmpty(productStore)){
                    companyCode = productStore.getString("payToPartyId");
                }

            } catch (GenericEntityException e) {
                Debug.logError("Failed to get ProductStore ["+ productStoreId+ "] due to exception " + e, module);
            }
        }
        return companyCode;
    }

    static Map<String, Object> getAddressList(GenericValue shippingAddress, GenericValue facilityAdrress) {
        Map<String, Object> addresses = FastMap.newInstance();
        if (UtilValidate.isNotEmpty(shippingAddress)){
            addresses.put("shipTo", prepareAddressMap(shippingAddress));
        }
        if (UtilValidate.isNotEmpty(facilityAdrress)){
            addresses.put("shipFrom", prepareAddressMap(facilityAdrress));
        }
        return addresses;
    }

    private static Map prepareAddressMap(GenericValue address) {
        Map addressMap = FastMap.newInstance();
        if(UtilValidate.isNotEmpty(address)){
            addressMap.put("line1", address.getString("address1"));
            addressMap.put("line2", address.getString("address2"));
            addressMap.put("city", address.getString("city"));
            addressMap.put("region", address.getString("stateProvinceGeoId"));
            addressMap.put("country", address.getString("countryGeoId"));
            addressMap.put("postalCode", address.getString("postalCode"));
        }
        return addressMap;
    }

    private List getLinesForCalcTax(Map reqParamMap) {
        List lines = FastList.newInstance();
        List<GenericValue> itemProductList = (List) reqParamMap.get("itemProductList");
        List<BigDecimal> itemPriceList = (List) reqParamMap.get("itemPriceList");
        List<BigDecimal> itemAmountList = (List) reqParamMap.get("itemAmountList");
        List<BigDecimal> itemQuantityList = (List) reqParamMap.get("itemQuantityList");
        List<String> ref1List = (List) reqParamMap.get("ref1List");

        for (int i = 0; i < itemProductList.size(); i++) {
            GenericValue product = itemProductList.get(i);
            String ref1 = null;
            if (UtilValidate.isNotEmpty(ref1List)) {
                ref1 = ref1List.get(i);
            }
            BigDecimal quantity = itemQuantityList.get(i);
            BigDecimal amount = itemPriceList.get(i);
            BigDecimal amountTotal = itemAmountList.get(i);

            if (UtilValidate.isEmpty(amountTotal)) {
                amountTotal = amount.multiply(quantity);

            }

            Map<String, Object> itemInfo = FastMap.newInstance();
            itemInfo.put("number", i);
            if (UtilValidate.isNotEmpty(ref1)) {
                itemInfo.put("ref1", ref1);
            }
            itemInfo.put("quantity", quantity);
            itemInfo.put("amount", amountTotal);
            itemInfo.put("itemCode", product.getString("productId"));

            String taxCode = getTaxCode(product);
            itemInfo.put("taxCode", taxCode);
            lines.add(itemInfo);
        }
        BigDecimal shippingAmount = (BigDecimal) reqParamMap.get("orderShippingAmount");
        if(UtilValidate.isNotEmpty(shippingAmount) && BigDecimal.ZERO.compareTo(shippingAmount) != 0) {
            Map<String, Object> itemInfo = FastMap.newInstance();
            itemInfo.put("number", itemProductList.size());
            itemInfo.put("quantity", BigDecimal.ONE);
            itemInfo.put("amount", shippingAmount);
            itemInfo.put("itemCode", "SHIPPING_CHARGE");
            String taxCode = EntityUtilProperties.getPropertyValue("avalara.properties", "avalara.shipping.taxCode", "FR", delegator);
            itemInfo.put("taxCode", taxCode);
            lines.add(itemInfo);
        }
        return lines;
    }

    public Map<String, Object> invokeService(Map reqMap, String urlString) {

        try {
            JSON json = JSON.from(reqMap);
            String jsonStr = json.toString();
            URL postUrl=new URL(urlString);
            Timestamp startTime = UtilDateTime.nowTimestamp();
            Debug.logInfo("Avalara Request url Object +++++ " + urlString, module);
            Debug.logInfo("Avalara Request Json Object +++++ " + jsonStr, module);
            HttpURLConnection connection = (HttpURLConnection) postUrl.openConnection();
            int connectionTimeOut = Integer.parseInt(EntityUtilProperties.getPropertyValue("avalara.properties", "avalara.connection.timeout", "15000", delegator));
            connection.setConnectTimeout(connectionTimeOut);
            connection.setReadTimeout(connectionTimeOut);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Length", Integer.toString(jsonStr.getBytes("UTF8").length));
            connection.setRequestProperty("Content-Type","text/json");
            connection.setRequestProperty("Date", (String) reqMap.get("date"));
            connection.setRequestProperty("Authorization", getAvalaraAuthString());
            OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
            out.write(jsonStr);
            out.close();
            Timestamp endTime = UtilDateTime.nowTimestamp();
            Debug.log("####################### Overall time taken by https tax call : "+UtilDateTime.getInterval(startTime, endTime));
            return getResponseMap(connection);
        } catch (Exception e) {
            Debug.logError(e, "Error communicating with Avalara: " + e.toString(), module);
            return ServiceUtil.returnError(e.toString());
        }
    }

    private String getAvalaraAuthString() {
        String authorizationString = null;
        String userId = EntityUtilProperties.getPropertyValue("avalara.properties", "avalara.username", delegator);
        String password = EntityUtilProperties.getPropertyValue("avalara.properties", "avalara.password", delegator);
        if (UtilValidate.isNotEmpty(userId) && UtilValidate.isNotEmpty(password)) {
            String userString = userId + ':' + password;
            authorizationString = "Basic " + new String(Base64.encodeBase64(userString.getBytes()));
        }
        return authorizationString;
    }

    private Map<String, Object> getResponseMap(HttpURLConnection connection) throws IOException, ConversionException {
        Map<String, Object> result = FastMap.newInstance();
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
            InputStream inputStream = connection.getInputStream();
            String jsonString = toStringFromInputStream(inputStream);
            Debug.logInfo("Avalara response Json ++++" + jsonString, module);
            JSON jsonObject = JSON.from(jsonString);
            JSONConverters.JSONToMap jsonMap = new JSONConverters.JSONToMap();
            result = jsonMap.convert(jsonObject);
        } else {
            JSON errorJson = JSON.from(connection.getErrorStream());
            JSONConverters.JSONToMap jsonMap = new JSONConverters.JSONToMap();
            return jsonMap.convert(errorJson);
        }
        return result;
    }

    public String toStringFromInputStream(InputStream inputStream) {
        StringBuilder response = new StringBuilder();
        try{
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            String inputLine;
            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine);
            }
        }
        catch (IOException e) {
            Debug.logError("Avalara response parsing problem." + e.getMessage(), module);
        }
        return response.toString();
    }

    public void getItemAndShippingTaxAdjustments(List<Map<String, Object>> taxLines, List<List<GenericValue>> itemTaxAdjustments, List<GenericValue> orderAdjustments, String productStoreId) throws GenericEntityException {

        // TODO: Need a proper way to get TaxAuthorityGlAccount for now copy part from WebOE
        String taxAuthorityRateSeqId = EntityUtilProperties.getPropertyValue("avalara.properties", "avalara.taxAuthorityRateSeqId", "9001", delegator);

        GenericValue taxAuthorityRateProduct = delegator.findOne("TaxAuthorityRateProduct", UtilMisc.toMap("taxAuthorityRateSeqId", taxAuthorityRateSeqId), false);
        GenericValue taxAuthorityGlAccount = null;
        if (UtilValidate.isNotEmpty(taxAuthorityRateProduct)) {
            String organizationPartyId = getCompanyCode(productStoreId);

            taxAuthorityGlAccount = delegator.findOne("TaxAuthorityGlAccount",
                    UtilMisc.toMap("taxAuthGeoId", taxAuthorityRateProduct.getString("taxAuthGeoId"),
                                   "taxAuthPartyId", taxAuthorityRateProduct.getString("taxAuthPartyId"),
                                   "organizationPartyId", organizationPartyId), false);

        }

        for (Map<String, Object> taxLine : taxLines) {
            Map<String, Object> adjustment = FastMap.newInstance();

            BigDecimal taxAmount = new BigDecimal(taxLine.get("tax").toString());
            adjustment.put("orderAdjustmentTypeId", "SALES_TAX");
            adjustment.put("amount", taxAmount);
            String comments = "";
            List<Map<String, Object>> taxDetails = (List<Map<String, Object>>) taxLine.get("details");
            for (Map<String, Object> taxDetail : taxDetails) {
                adjustment.put("sourcePercentage", new BigDecimal(taxDetail.get("rate").toString()));
                if (comments.length() == 0) {
                    comments =  taxDetail.get("region") + ":" + taxDetail.get("jurisName") + ":"+ taxDetail.get("rate");
                } else {
                    comments =  comments + taxDetail.get("region") + ":" + taxDetail.get("jurisName") + ":"+ taxDetail.get("rate");
                }
            }
            adjustment.put("comments", comments);
            adjustment.put("description", taxLine.get("ref1"));
            if(UtilValidate.isNotEmpty(taxAuthorityGlAccount)) {
                adjustment.put("taxAuthGeoId", taxAuthorityGlAccount.getString("taxAuthGeoId"));
                adjustment.put("taxAuthPartyId", taxAuthorityGlAccount.getString("taxAuthPartyId"));
                adjustment.put("overrideGlAccountId", taxAuthorityGlAccount.getString("glAccountId"));
            }
            GenericValue orderAdjustment= delegator.makeValue("OrderAdjustment",adjustment);
            if (!("FR".equals(taxLine.get("taxCode")) || "SHIPPING_CHARGE".equals(taxLine.get("itemCode")))) {
                itemTaxAdjustments.add(UtilMisc.toList(orderAdjustment));
            } else {
                orderAdjustments.add(orderAdjustment);
            }
        }
    }

    public String getTaxCode(GenericValue product) {
        String productId = product.getString("productId");
        List<String> productIds = FastList.newInstance();
        String taxCode = null;

        if ("N".equals(product.getString("taxable"))) {
            //This system tax code has a non-taxable default so should only be used when the user has conducted due diligence
            // to determine that the associated transactions are exempt from tax.
            return "ON030000";
        }

        productIds.add(productId);
        GenericValue parentProduct = ProductWorker.getParentProduct(productId, delegator);
        if (UtilValidate.isNotEmpty(parentProduct)) {
            productIds.add(parentProduct.getString("productId"));
        }

        try {
            GenericValue category = EntityQuery.use(delegator).from("ProductCategoryAndMember")
                    .where(EntityCondition.makeCondition(
                            UtilMisc.toList(
                                    EntityCondition.makeCondition("productId", EntityOperator.IN, productIds),
                                    EntityCondition.makeCondition("productCategoryTypeId", EntityOperator.EQUALS, "TAX_CATEGORY")
                            ), EntityOperator.AND))
                    .filterByDate()
                    .queryFirst();

            if (UtilValidate.isNotEmpty(category)) {
                taxCode = category.getString("categoryName");
            } else {
                // Fallback case when tax code is not found using category then read it from property
                // and if it is not there as well then Use Durable Medical Equipment tax Code.
                taxCode = EntityUtilProperties.getPropertyValue("avalara.properties", "avalara.itemCode.taxCode", "PC040206", delegator);
            }

        } catch (GenericEntityException e) {
            Debug.logError("Failed to get Product ["+ productId+ "] due to exception " + e, module);

        }
        return taxCode;
    }

}
