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
package org.apache.ofbiz.order.thirdparty.zipsales;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilURL;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.datafile.DataFile;
import org.apache.ofbiz.datafile.DataFileException;
import org.apache.ofbiz.datafile.Record;
import org.apache.ofbiz.datafile.RecordIterator;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * Zip-Sales Database Services
 */
public class ZipSalesServices {

    public static final String module = ZipSalesServices.class.getName();
    public static final String dataFile = "org/apache/ofbiz/order/thirdparty/zipsales/ZipSalesTaxTables.xml";
    public static final String flatTable = "FlatTaxTable";
    public static final String ruleTable = "FreightRuleTable";
    public static final String resource_error = "OrderErrorUiLabels";

    // date formatting
    private static final String DATE_PATTERN = "yyyyMMdd";

    // import table service
    public static Map<String, Object> importFlatTable(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String taxFileLocation = (String) context.get("taxFileLocation");
        String ruleFileLocation = (String) context.get("ruleFileLocation");
        Locale locale = (Locale) context.get("locale");

        // do security check
        if (!security.hasPermission("SERVICE_INVOKE_ANY", userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderYouDoNotHavePermissionToLoadTaxTables",locale));
        }

        // get a now stamp (we'll use 2000-01-01)
        Timestamp now = parseDate("20000101", null);

        // load the data file
        DataFile tdf = null;
        try {
            tdf = DataFile.makeDataFile(UtilURL.fromResource(dataFile), flatTable);
        } catch (DataFileException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderUnableToReadZipSalesDataFile",locale));
        }

        // locate the file to be imported
        URL tUrl = UtilURL.fromResource(taxFileLocation);
        if (tUrl == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderUnableToLocateTaxFileAtLocation", UtilMisc.toMap("taxFileLocation",taxFileLocation), locale));
        }

        RecordIterator tri = null;
        try {
            tri = tdf.makeRecordIterator(tUrl);
        } catch (DataFileException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemGettingTheRecordIterator",locale));
        }
        if (tri != null) {
            while (tri.hasNext()) {
                Record entry = null;
                try {
                    entry = tri.next();
                } catch (DataFileException e) {
                    Debug.logError(e, module);
                }
                GenericValue newValue = delegator.makeValue("ZipSalesTaxLookup");
                // PK fields
                newValue.set("zipCode", entry.getString("zipCode").trim());
                newValue.set("stateCode", entry.get("stateCode") != null ? entry.getString("stateCode").trim() : "_NA_");
                newValue.set("city", entry.get("city") != null ? entry.getString("city").trim() : "_NA_");
                newValue.set("county", entry.get("county") != null ? entry.getString("county").trim() : "_NA_");
                newValue.set("fromDate", parseDate(entry.getString("effectiveDate"), now));

                // non-PK fields
                newValue.set("countyFips", entry.get("countyFips"));
                newValue.set("countyDefault", entry.get("countyDefault"));
                newValue.set("generalDefault", entry.get("generalDefault"));
                newValue.set("insideCity", entry.get("insideCity"));
                newValue.set("geoCode", entry.get("geoCode"));
                newValue.set("stateSalesTax", entry.get("stateSalesTax"));
                newValue.set("citySalesTax", entry.get("citySalesTax"));
                newValue.set("cityLocalSalesTax", entry.get("cityLocalSalesTax"));
                newValue.set("countySalesTax", entry.get("countySalesTax"));
                newValue.set("countyLocalSalesTax", entry.get("countyLocalSalesTax"));
                newValue.set("comboSalesTax", entry.get("comboSalesTax"));
                newValue.set("stateUseTax", entry.get("stateUseTax"));
                newValue.set("cityUseTax", entry.get("cityUseTax"));
                newValue.set("cityLocalUseTax", entry.get("cityLocalUseTax"));
                newValue.set("countyUseTax", entry.get("countyUseTax"));
                newValue.set("countyLocalUseTax", entry.get("countyLocalUseTax"));
                newValue.set("comboUseTax", entry.get("comboUseTax"));

                try {
                    delegator.createOrStore(newValue);
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorWritingRecordsToTheDatabase",locale));
                }

                // console log
                Debug.logInfo(newValue.get("zipCode") + "/" + newValue.get("stateCode") + "/" + newValue.get("city") + "/" + newValue.get("county") + "/" + newValue.get("fromDate"), module);
            }
        }

        // load the data file
        DataFile rdf = null;
        try {
            rdf = DataFile.makeDataFile(UtilURL.fromResource(dataFile), ruleTable);
        } catch (DataFileException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderUnableToReadZipSalesDataFile",locale));
        }

        // locate the file to be imported
        URL rUrl = UtilURL.fromResource(ruleFileLocation);
        if (rUrl == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderUnableToLocateRuleFileFromLocation", UtilMisc.toMap("ruleFileLocation",ruleFileLocation), locale));
        }

        RecordIterator rri = null;
        try {
            rri = rdf.makeRecordIterator(rUrl);
        } catch (DataFileException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderProblemGettingTheRecordIterator",locale));
        }
        if (rri != null) {
            while (rri.hasNext()) {
                Record entry = null;
                try {
                    entry = rri.next();
                } catch (DataFileException e) {
                    Debug.logError(e, module);
                }
                if (UtilValidate.isNotEmpty(entry.getString("stateCode"))) {
                    GenericValue newValue = delegator.makeValue("ZipSalesRuleLookup");
                    // PK fields
                    newValue.set("stateCode", entry.get("stateCode") != null ? entry.getString("stateCode").trim() : "_NA_");
                    newValue.set("city", entry.get("city") != null ? entry.getString("city").trim() : "_NA_");
                    newValue.set("county", entry.get("county") != null ? entry.getString("county").trim() : "_NA_");
                    newValue.set("fromDate", parseDate(entry.getString("effectiveDate"), now));

                    // non-PK fields
                    newValue.set("idCode", entry.get("idCode") != null ? entry.getString("idCode").trim() : null);
                    newValue.set("taxable", entry.get("taxable") != null ? entry.getString("taxable").trim() : null);
                    newValue.set("shipCond", entry.get("shipCond") != null ? entry.getString("shipCond").trim() : null);

                    try {
                        // using storeAll as an easy way to create/update
                        delegator.storeAll(UtilMisc.toList(newValue));
                    } catch (GenericEntityException e) {
                        Debug.logError(e, module);
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderErrorWritingRecordsToTheDatabase",locale));
                    }

                    // console log
                    Debug.logInfo(newValue.get("stateCode") + "/" + newValue.get("city") + "/" + newValue.get("county") + "/" + newValue.get("fromDate"), module);
                }
            }
        }

        return ServiceUtil.returnSuccess();
    }

    // tax calc service
    public static Map<String, Object> flatTaxCalc(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        List<GenericValue> itemProductList = UtilGenerics.checkList(context.get("itemProductList"));
        List<BigDecimal> itemAmountList = UtilGenerics.checkList(context.get("itemAmountList"));
        List<BigDecimal> itemShippingList = UtilGenerics.checkList(context.get("itemShippingList"));
        BigDecimal orderShippingAmount = (BigDecimal) context.get("orderShippingAmount");
        GenericValue shippingAddress = (GenericValue) context.get("shippingAddress");

        // flatTaxCalc only uses the Zip + City from the address
        String stateProvince = shippingAddress.getString("stateProvinceGeoId");
        String postalCode = shippingAddress.getString("postalCode");
        String city = shippingAddress.getString("city");

        // setup the return lists.
        List<GenericValue> orderAdjustments = new LinkedList<>();
        List<List<GenericValue>> itemAdjustments = new LinkedList<>();

        // check for a valid state/province geo
        String validStates = EntityUtilProperties.getPropertyValue("zipsales", "zipsales.valid.states", delegator);
        if (UtilValidate.isNotEmpty(validStates)) {
            List<String> stateSplit = StringUtil.split(validStates, "|");
            if (!stateSplit.contains(stateProvince)) {
                Map<String, Object> result = ServiceUtil.returnSuccess();
                result.put("orderAdjustments", orderAdjustments);
                result.put("itemAdjustments", itemAdjustments);
                return result;
            }
        }

        try {
            // loop through and get per item tax rates
            for (int i = 0; i < itemProductList.size(); i++) {
                GenericValue product = itemProductList.get(i);
                BigDecimal itemAmount = itemAmountList.get(i);
                BigDecimal shippingAmount = itemShippingList.get(i);
                itemAdjustments.add(getItemTaxList(delegator, product, postalCode, city, itemAmount, shippingAmount, false));
            }
            if (orderShippingAmount.compareTo(BigDecimal.ZERO) > 0) {
                List<GenericValue> taxList = getItemTaxList(delegator, null, postalCode, city, BigDecimal.ZERO, orderShippingAmount, false);
                orderAdjustments.addAll(taxList);
            }
        } catch (GeneralException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("orderAdjustments", orderAdjustments);
        result.put("itemAdjustments", itemAdjustments);
        return result;
    }

    private static List<GenericValue>getItemTaxList(Delegator delegator, GenericValue item, String zipCode, String city, BigDecimal itemAmount, BigDecimal shippingAmount, boolean isUseTax) throws GeneralException {
        List<GenericValue> adjustments = new LinkedList<>();

        // check the item for tax status
        if (item != null && item.get("taxable") != null && "N".equals(item.getString("taxable"))) {
            // item not taxable
            return adjustments;
        }

        // lookup the records
        List<GenericValue> zipLookup = EntityQuery.use(delegator).from("ZipSalesTaxLookup").where("zipCode", zipCode).orderBy("-fromDate").queryList();
        if (UtilValidate.isEmpty(zipLookup)) {
            throw new GeneralException("The zip code entered is not valid.");
        }

        // the filtered list
        // TODO: taxLookup is always null, so filter by County will never be executed
        List<GenericValue> taxLookup = null;

        // only do filtering if there are more then one zip code found
        if (zipLookup != null && zipLookup.size() > 1) {
            // first filter by city
            List<GenericValue> cityLookup = EntityUtil.filterByAnd(zipLookup, UtilMisc.toMap("city", city.toUpperCase()));
            if (UtilValidate.isNotEmpty(cityLookup)) {
                if (cityLookup.size() > 1) {
                    // filter by county
                    List<GenericValue> countyLookup = EntityUtil.filterByAnd(taxLookup, UtilMisc.toMap("countyDefault", "Y"));
                    if (UtilValidate.isNotEmpty(countyLookup)) {
                        // use the county default
                        taxLookup = countyLookup;
                    } else {
                        // no county default; just use the first city
                        taxLookup = cityLookup;
                    }
                } else {
                    // just one city found; use that one
                    taxLookup = cityLookup;
                }
            } else {
                // no city found; lookup default city
                List<GenericValue> defaultLookup = EntityUtil.filterByAnd(zipLookup, UtilMisc.toMap("generalDefault", "Y"));
                if (UtilValidate.isNotEmpty(defaultLookup)) {
                    // use the default city lookup
                    taxLookup = defaultLookup;
                } else {
                    // no default found; just use the first from the zip lookup
                    taxLookup = zipLookup;
                }
            }
        } else {
            // zero or 1 zip code found; use it
            taxLookup = zipLookup;
        }

        // get the first one
        GenericValue taxEntry = null;
        if (UtilValidate.isNotEmpty(taxLookup)) {
            taxEntry = taxLookup.iterator().next();
        }

        if (taxEntry == null) {
            Debug.logWarning("No tax entry found for : " + zipCode + " / " + city + " - " + itemAmount, module);
            return adjustments;
        }

        String fieldName = "comboSalesTax";
        if (isUseTax) {
            fieldName = "comboUseTax";
        }

        BigDecimal comboTaxRate = taxEntry.getBigDecimal(fieldName);
        if (comboTaxRate == null) {
            Debug.logWarning("No Combo Tax Rate In Field " + fieldName + " @ " + zipCode + " / " + city + " - " + itemAmount, module);
            return adjustments;
        }

        // get state code
        String stateCode = taxEntry.getString("stateCode");

        // check if shipping is exempt
        boolean taxShipping = true;

        // look up the rules
        List<GenericValue> ruleLookup = null;
        try {
            ruleLookup = EntityQuery.use(delegator).from("ZipSalesRuleLookup").where("stateCode", stateCode).orderBy("-fromDate").queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        // filter out city
        if (ruleLookup != null && ruleLookup.size() > 1) {
            ruleLookup = EntityUtil.filterByAnd(ruleLookup, UtilMisc.toMap("city", city.toUpperCase()));
        }

        // no county captured; so filter by date
        if (ruleLookup != null && ruleLookup.size() > 1) {
            ruleLookup = EntityUtil.filterByDate(ruleLookup);
        }

        if (ruleLookup != null) {
            for (GenericValue rule : ruleLookup) {
                if (!taxShipping) {
                    // if we found an rule which passes no need to contine (all rules are ||)
                    break;
                }
                String idCode = rule.getString("idCode");
                String taxable = rule.getString("taxable");
                String condition = rule.getString("shipCond");
                if ("T".equals(taxable))  {
                    // this record is taxable
                    continue;
                } else {
                    // except if conditions are met
                    boolean qualify = false;
                    if (UtilValidate.isNotEmpty(condition)) {
                        char[] conditions = condition.toCharArray();
                        for (int i = 0; i < conditions.length; i++) {
                            switch (conditions[i]) {
                                case 'A' :
                                    // SHIPPING CHARGE SEPARATELY STATED ON INVOICE
                                    qualify = true; // OFBiz does this by default
                                    break;
                                case 'B' :
                                    // SHIPPING CHARGE SEPARATED ON INVOICE FROM HANDLING OR SIMILAR CHARGES
                                    qualify = false; // we do not support this currently
                                    break;
                                case 'C' :
                                    // ITEM NOT SOLD FOR GUARANTEED SHIPPED PRICE
                                    qualify = false; // we don't support this currently
                                    break;
                                case 'D' :
                                    // SHIPPING CHARGE IS COST ONLY
                                    qualify = false; // we assume a handling charge is included
                                    break;
                                case 'E' :
                                    // SHIPPED DIRECTLY TO PURCHASER
                                    qualify = true; // this is true, unless gifts do not count?
                                    break;
                                case 'F' :
                                    // SHIPPED VIA COMMON CARRIER
                                    qualify = true; // best guess default
                                    break;
                                case 'G' :
                                    // SHIPPED VIA CONTRACT CARRIER
                                    qualify = false; // best guess default
                                    break;
                                case 'H' :
                                    // SHIPPED VIA VENDOR EQUIPMENT
                                    qualify = false; // best guess default
                                    break;
                                case 'I' :
                                    // SHIPPED F.O.B. ORIGIN
                                    qualify = false; // no clue
                                    break;
                                case 'J' :
                                    // SHIPPED F.O.B. DESTINATION
                                    qualify = false; // no clue
                                    break;
                                case 'K' :
                                    // F.O.B. IS PURCHASERS OPTION
                                    qualify = false; // no clue
                                    break;
                                case 'L' :
                                    // SHIPPING ORIGINATES OR TERMINATES IN DIFFERENT STATES
                                    qualify = true; // not determined at order time, no way to know
                                    break;
                                case 'M' :
                                    // PROOF OF VENDOR ACTING AS SHIPPING AGENT FOR PURCHASER
                                    qualify = false; // no clue
                                    break;
                                case 'N' :
                                    // SHIPPED FROM VENDOR LOCATION
                                    qualify = true; // sure why not
                                    break;
                                case 'O' :
                                    // SHIPPING IS BY PURCHASER OPTION
                                    qualify = false; // most online stores require shipping
                                    break;
                                case 'P' :
                                    // CREDIT ALLOWED FOR SHIPPING CHARGE PAID BY PURCHASER TO CARRIER
                                    qualify = false; // best guess default
                                    break;
                                default: break;
                            }
                        }
                    }

                    if (qualify) {
                        if (isUseTax) {
                            if (idCode.indexOf('U') > 0) {
                                taxShipping = false;
                            }
                        } else {
                            if (idCode.indexOf('S') > 0) {
                                taxShipping = false;
                            }
                        }
                    }
                }
            }
        }

        BigDecimal taxableAmount = itemAmount;
        if (taxShipping) {
            //Debug.logInfo("Taxing shipping", module);
            taxableAmount = taxableAmount.add(shippingAmount);
        } else {
            Debug.logInfo("Shipping is not taxable", module);
        }

        // calc tax amount
        BigDecimal taxRate = comboTaxRate;
        BigDecimal taxCalc = taxableAmount.multiply(taxRate);

        adjustments.add(delegator.makeValue("OrderAdjustment", UtilMisc.toMap("amount", taxCalc, "orderAdjustmentTypeId", "SALES_TAX", "comments", taxRate, "description", "Sales Tax (" + stateCode + ")")));

        return adjustments;
    }

    // formatting methods
    private static Timestamp parseDate(String dateString, Timestamp useWhenNull) {
        Timestamp ts = null;
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);
        if (dateString != null) {
            try {
                ts = new Timestamp(dateFormat.parse(dateString).getTime());
            } catch (ParseException e) {
                Debug.logError(e, module);
            }
        }

        if (ts != null) {
            return ts;
        } else {
            return useWhenNull;
        }
    }
}
