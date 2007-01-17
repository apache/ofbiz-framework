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

package org.ofbiz.order.thirdparty.taxware;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilURL;
import org.ofbiz.datafile.DataFile;
import org.ofbiz.datafile.DataFileException;
import org.ofbiz.datafile.ModelRecord;
import org.ofbiz.datafile.ModelField;
import org.ofbiz.datafile.Record;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;


/**
 * TaxwareUTL - Taxware Universal Tax Link
 * Requires taxcommon.class found w/ UTL.
 */
public class TaxwareUTL {

    public static final String module = TaxwareUTL.class.getName();

    // default data files
    DataFile outHead = null;
    DataFile outItem = null;

    double shippingAmount = 0.00;
    List orderAdjustments = new ArrayList();
    List itemAdjustments = new ArrayList();

    boolean setShipping = false;
    GenericValue shipToAddress = null;

    // list of records to process
    List records = new ArrayList();
    boolean processed = false;

    public TaxwareUTL() throws TaxwareException {
        init();
    }

    public int process() throws TaxwareException {
        // make sure we have everything before processing
        checkFields();

        if (processed)
            throw new TaxwareException("Cannot re-process records.");
        processed = true;

        Iterator i = records.iterator();

        while (i.hasNext()) {
            Record rec = (Record) i.next();

            rec = makeItemData(rec);
            outItem.addRecord(rec);
        }

        // create a shipping item
        if (shippingAmount > 0) {
            Record shipping = outItem.makeRecord("outItem");

            shipping = makeItemData(shipping);
            shipping.set("FREIGHT_AMOUNT", new Double(shippingAmount));
            outItem.addRecord(shipping);
        }

        // make the header file
        Record header = outHead.makeRecord("outHead");

        header.set("NUMBER_RECORDS", new Long(outItem.getRecords().size()));
        header.set("PROCESS_INDICATOR", "1");
        outHead.addRecord(header);

        int returnCode = -1;

        try {
            // add the header
            StringBuffer outBuffer = new StringBuffer();

            outBuffer.append(outHead.writeDataFile());

            // append the items
            outBuffer.append(outItem.writeDataFile());

            // print out the datafile
            if (Debug.verboseOn()) Debug.logVerbose("::Out String::", module);
            if (Debug.verboseOn()) Debug.logVerbose("\"" + outBuffer.toString() + "\"", module);

            File outFile = new File("TAXWARE-TEST.IN");
            FileOutputStream fos = null;

            try {
                fos = new FileOutputStream(outFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            outHead.writeDataFile(fos);
            outItem.writeDataFile(fos);
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            outItem.writeDataFile("TaxwareTest.in");

            StringBuffer retBuffer = taxCalc(outBuffer);

            // make the return data file
            returnCode = processOutFile(retBuffer);
        } catch (DataFileException dfe) {
            throw new TaxwareException("Problems with the data file.", dfe);
        }

        return returnCode;
    }

    public void setShipping(double shippingAmount) {
        this.shippingAmount = shippingAmount;
        setShipping = true;
    }

    public void setShipAddress(GenericValue v) {
        this.shipToAddress = v;
    }

    public void addItem(GenericValue product, double linePrice, double itemShipping) {
        Record record = outItem.makeRecord("outItem");

        if (product.get("taxable") == null || product.getString("taxable").equalsIgnoreCase("Y")) {
            if (product.get("taxCategory") != null)
                record.set("COMMODITY_PRODUCT_CODE", product.get("taxCategory"));
            else
                record.set("COMMODITY_PRODUCT_CODE", "DEFAULT");
            record.set("PART_NUMBER", product.get("productId"));
            record.set("LINE_ITEM_AMOUNT", new Double(linePrice));
            if (itemShipping > 0)
                record.set("FREIGHT_AMOUNT", new Double(itemShipping));
        }
        records.add(record);
    }

    public List getItemAdjustments() {
        return itemAdjustments;
    }

    public List getOrderAdjustments() {
        return orderAdjustments;
    }

    private void init() throws TaxwareException {
        TaxwareUTL.loadLib();
        outHead = createDataFile("TaxwareOutHead");
        outItem = createDataFile("TaxwareOutItem");
    }

    private Record makeItemData(Record record) {
        addStaticData(record);
        addAddresses(record);
        record.set("TAXSEL_PARM", "3");
        record.set("SYSTEM_INDICATOR", "1");
        record.set("INVOICE_DATE", new java.sql.Date(new Date().getTime()));
        return record;
    }

    private void addStaticData(Record record) {
        // grab a taxware properties file and get static data
        record.set("COMPANY_ID", UtilProperties.getPropertyValue("taxware.properties", "COMPANY_ID", " "));
        record.set("AUDIT_FILE_INDICATOR", UtilProperties.getPropertyValue("taxware.properties", "AUDIT_FILE_INDICATOR", "2"));
        record.set("SF_COUNTRY_CODE", UtilProperties.getPropertyValue("taxware.properties", "SF_COUNTRY_CODE", ""));
        record.set("SF_STATE_PROVINCE", UtilProperties.getPropertyValue("taxware.properties", "SF_STATE_PROVINCE", " "));
        record.set("SF_CITY", UtilProperties.getPropertyValue("taxware.properties", "SF_CITY", " "));
        record.set("SF_POSTAL_CODE", UtilProperties.getPropertyValue("taxware.properties", "SF_POSTAL_CODE", " "));
        record.set("POO_COUNTRY_CODE", UtilProperties.getPropertyValue("taxware.properties", "POO_COUNTRY_CODE", ""));
        record.set("POO_STATE_PROVINCE", UtilProperties.getPropertyValue("taxware.properties", "POO_STATE_PROVINCE", " "));
        record.set("POO_CITY", UtilProperties.getPropertyValue("taxware.properties", "POO_CITY", " "));
        record.set("POO_POSTAL_CODE", UtilProperties.getPropertyValue("taxware.properties", "POO_POSTAL_CODE", " "));
        record.set("POA_COUNTRY_CODE", UtilProperties.getPropertyValue("taxware.properties", "POA_COUNTRY_CODE", ""));
        record.set("POA_STATE_PROVINCE", UtilProperties.getPropertyValue("taxware.properties", "POA_STATE_PROVINCE", " "));
        record.set("POA_CITY", UtilProperties.getPropertyValue("taxware.properties", "POA_CITY", " "));
        record.set("POA_POSTAL_CODE", UtilProperties.getPropertyValue("taxware.properties", "POA_POSTAL_CODE", " "));
    }

    private void addAddresses(Record record) {
        // set the address info from the value objects
        if (shipToAddress != null) {
            // set the ship to address
            if (shipToAddress.get("countryGeoId") == null) {
                record.set("ST_COUNTRY_CODE", "US");
            } else if (shipToAddress.getString("countryGeoId").equals("USA")) {
                record.set("ST_COUNTRY_CODE", "US");
            } else {
                record.set("ST_COUNTRY_CODE", shipToAddress.get("countryGeoId"));
            }
            record.set("ST_COUNTRY_CODE", "US");
            record.set("ST_STATE_PROVINCE", shipToAddress.get("stateProvinceGeoId"));
            record.set("ST_CITY", shipToAddress.get("city"));
            record.set("ST_POSTAL_CODE", shipToAddress.get("postalCode"));
        }
    }

    private DataFile createDataFile(String dataFile) throws TaxwareException {
        DataFile df = null;

        try {
            df = DataFile.makeDataFile(UtilURL.fromResource("org/ofbiz/thirdparty/taxware/TaxwareFiles.xml"), dataFile);
        } catch (DataFileException e) {
            Debug.logError(e, module);
            throw new TaxwareException("Cannot load datafile.");
        }
        return df;
    }

    public static void loadLib() throws TaxwareException {
        try {
            System.loadLibrary("taxcommon");
        } catch (UnsatisfiedLinkError e) {
            Debug.logError(e, module);
            throw new TaxwareException("Cannot load libtaxcommon.so/taxcommon.dll.", e);
        }
    }

    private StringBuffer taxCalc(StringBuffer outBuffer) throws DataFileException, TaxwareException {
        StringBuffer inBuffer = new StringBuffer();
        int result = callTaxware(outBuffer.toString(), inBuffer);

        if (Debug.verboseOn()) Debug.logVerbose("Taxware Return: " + result, module);
        if (result != 1)
            throw new TaxwareException("Taxware processing failed (" + result + ")");

        if (Debug.verboseOn()) Debug.logVerbose("::Return String::", module);
        if (Debug.verboseOn()) Debug.logVerbose("\"" + inBuffer.toString() + "\"", module);
        return inBuffer;
    }

    private int callTaxware(String inString, StringBuffer outBuffer) throws TaxwareException {
        try {
            return taxcommon.CalculateTax(inString, outBuffer);
        } catch (Exception e) {
            throw new TaxwareException("Problems running JNI wrapper.", e);
        }
    }

    private void checkFields() throws TaxwareException {
        if (!setShipping)
            throw new TaxwareException("Shipping amount has not been set.");
        if (shipToAddress == null)
            throw new TaxwareException("Shipping address has not been set.");
        if (records.size() == 0)
            throw new TaxwareException("No items have been defined.");
    }

    private int processOutFile(StringBuffer retBuffer) throws DataFileException, TaxwareException {
        DataFile retHead = createDataFile("TaxwareInHead");
        DataFile retItem = createDataFile("TaxwareInItem");
        String headStr = retBuffer.toString().substring(0, 283);
        String itemStr = retBuffer.toString().substring(284);

        if (Debug.verboseOn()) Debug.logVerbose("Return Size: " + retBuffer.length(), module);
        GenericDelegator delegator = shipToAddress.getDelegator();

        retHead.readDataFile(headStr);
        retItem.readDataFile(itemStr);

        List retRecords = retItem.getRecords();
        Iterator i = retRecords.iterator();

        if (Debug.verboseOn()) Debug.logVerbose("Returned Records: " + retRecords.size(), module);
        if (Debug.verboseOn()) Debug.logVerbose("Sent Items: " + records.size(), module);

        while (i.hasNext()) {
            Record rec = (Record) i.next();
            ModelRecord model = rec.getModelRecord();

            // make the adjustment lists
            if (itemAdjustments.size() < records.size()) {
                List currentItem = new ArrayList();

                if (rec.getDouble("TAX_AMT_COUNTRY").doubleValue() > 0) {
                    if (Debug.verboseOn()) Debug.logVerbose("Country Tax Amount: " + rec.getDouble("TAX_AMT_COUNTRY"), module);
                    Double rate = new Double(rec.getDouble("TAX_RATE_COUNTRY").doubleValue() * 100);
                    String type = rec.getString("TAX_TYPE_COUNTRY").equals("S") ? "SALES TAX" : "USE TAX";
                    String jur = rec.get("JUR_COUNTRY") != null ? rec.getString("JUR_COUNTRY").trim() : "";
                    String comments = jur + "|" + type + "|" + rate.toString();

                    currentItem.add(delegator.makeValue("OrderAdjustment",
                            UtilMisc.toMap("amount", rec.getDouble("TAX_AMT_COUNTRY"),
                                "orderAdjustmentTypeId", "SALES_TAX", "comments", comments)));
                }

                if (rec.getDouble("TAX_AMT_STATE").doubleValue() > 0) {
                    Double rate = new Double(rec.getDouble("TAX_RATE_STATE").doubleValue() * 100);
                    String type = rec.getString("TAX_TYPE_STATE").equals("S") ? "SALES TAX" : "USE TAX";
                    String jur = rec.get("JUR_STATE") != null ? rec.getString("JUR_STATE").trim() : "";
                    String comments = jur + "|" + type + "|" + rate.toString();

                    currentItem.add(delegator.makeValue("OrderAdjustment",
                            UtilMisc.toMap("amount", rec.getDouble("TAX_AMT_STATE"),
                                "orderAdjustmentTypeId", "SALES_TAX", "comments", comments)));
                }

                if (rec.getDouble("TAX_AMT_COUNTY").doubleValue() > 0) {
                    Double rate = new Double(rec.getDouble("TAX_RATE_COUNTY").doubleValue() * 100);
                    String type = rec.getString("TAX_TYPE_COUNTY").equals("S") ? "SALES TAX" : "USE TAX";
                    String jur = rec.get("JUR_COUNTY_CODE") != null ? rec.getString("JUR_COUNTY_CODE").trim() : "";
                    String comments = jur + "|" + type + "|" + rate.toString();

                    currentItem.add(delegator.makeValue("OrderAdjustment",
                            UtilMisc.toMap("amount", rec.getDouble("TAX_AMT_COUNTY"),
                                "orderAdjustmentTypeId", "SALES_TAX", "comments", comments)));
                }

                if (rec.getDouble("TAX_AMT_CITY").doubleValue() > 0) {
                    Double rate = new Double(rec.getDouble("TAX_RATE_CITY").doubleValue() * 100);
                    String type = rec.getString("TAX_TYPE_CITY").equals("S") ? "SALES TAX" : "USE TAX";
                    String jur = rec.get("JUR_CITY") != null ? rec.getString("JUR_CITY").trim() : "";
                    String comments = jur + "|" + type + "|" + rate.toString();

                    currentItem.add(delegator.makeValue("OrderAdjustment",
                            UtilMisc.toMap("amount", rec.getDouble("TAX_AMT_CITY"),
                                "orderAdjustmentTypeId", "SALES_TAX", "comments", comments)));
                }

                if (rec.getDouble("TAX_AMT_SEC_STATE").doubleValue() > 0) {
                    Double rate = new Double(rec.getDouble("TAX_RATE_SEC_STATE").doubleValue() * 100);
                    String type = rec.getString("TAX_TYPE_SEC_STATE").equals("S") ? "SALES TAX" : "USE TAX";
                    String jur = rec.get("JUR_SEC_STATE") != null ? rec.getString("JUR_SEC_STATE").trim() : "";
                    String comments = jur + "|" + type + "|" + rate.toString();

                    currentItem.add(delegator.makeValue("OrderAdjustment",
                            UtilMisc.toMap("amount", rec.getDouble("TAX_AMT_SEC_STATE"),
                                "orderAdjustmentTypeId", "SALES_TAX", "comments", comments)));
                }

                if (rec.getDouble("TAX_AMT_SEC_COUNTY").doubleValue() > 0) {
                    Double rate = new Double(rec.getDouble("TAX_RATE_SEC_COUNTY").doubleValue() * 100);
                    String type = rec.getString("TAX_TYPE_SEC_COUNTY").equals("S") ? "SALES TAX" : "USE TAX";
                    String jur = rec.get("JUR_SEC_COUNTY_CODE") != null ? rec.getString("JUR_SEC_COUNTY_CODE").trim() : "";
                    String comments = jur + "|" + type + "|" + rate.toString();

                    currentItem.add(delegator.makeValue("OrderAdjustment",
                            UtilMisc.toMap("amount", rec.getDouble("TAX_AMT_SEC_COUNTY"),
                                "orderAdjustmentTypeId", "SALES_TAX", "comments", comments)));
                }

                if (rec.getDouble("TAX_AMT_SEC_CITY").doubleValue() > 0) {
                    Double rate = new Double(rec.getDouble("TAX_RATE_SEC_CITY").doubleValue() * 100);
                    String type = rec.getString("TAX_TYPE_SEC_CITY").equals("S") ? "SALES TAX" : "USE TAX";
                    String jur = rec.get("JUR_SEC_CITY") != null ? rec.getString("JUR_SEC_CITY").trim() : "";
                    String comments = jur + "|" + type + "|" + rate.toString();

                    currentItem.add(delegator.makeValue("OrderAdjustment",
                            UtilMisc.toMap("amount", rec.getDouble("TAX_AMT_SEC_CITY"),
                                "orderAdjustmentTypeId", "SALES_TAX", "comments", comments)));
                }

                // add a list of adjustments to the adjustment list
                itemAdjustments.add(currentItem);

            } else if (orderAdjustments.size() == 0) {
                if (rec.getDouble("TAX_AMT_COUNTRY").doubleValue() > 0) {
                    Double rate = new Double(rec.getDouble("TAX_RATE_COUNTRY").doubleValue() * 100);
                    String type = rec.getString("TAX_TYPE_COUNTRY").equals("S") ? "SALES TAX" : "USE TAX";
                    String jur = rec.get("JUR_COUNTRY") != null ? rec.getString("JUR_COUNTRY").trim() : "";
                    String comments = jur + "|" + type + "|" + rate.toString();

                    orderAdjustments.add(delegator.makeValue("OrderAdjustment",
                            UtilMisc.toMap("amount", rec.getDouble("TAX_AMT_COUNTRY"),
                                "orderAdjustmentTypeId", "SALES_TAX", "comments", comments)));
                }

                if (rec.getDouble("TAX_AMT_STATE").doubleValue() > 0) {
                    Double rate = new Double(rec.getDouble("TAX_RATE_STATE").doubleValue() * 100);
                    String type = rec.getString("TAX_TYPE_STATE").equals("S") ? "SALES TAX" : "USE TAX";
                    String jur = rec.get("JUR_STATE") != null ? rec.getString("JUR_STATE").trim() : "";
                    String comments = jur + "|" + type + "|" + rate.toString();

                    orderAdjustments.add(delegator.makeValue("OrderAdjustment",
                            UtilMisc.toMap("amount", rec.getDouble("TAX_AMT_STATE"),
                                "orderAdjustmentTypeId", "SALES_TAX", "comments", comments)));
                }

                if (rec.getDouble("TAX_AMT_COUNTY").doubleValue() > 0) {
                    Double rate = new Double(rec.getDouble("TAX_RATE_COUNTY").doubleValue() * 100);
                    String type = rec.getString("TAX_TYPE_COUNTY").equals("S") ? "SALES TAX" : "USE TAX";
                    String jur = rec.get("JUR_COUNTY_CODE") != null ? rec.getString("JUR_COUNTY_CODE").trim() : "";
                    String comments = jur + "|" + type + "|" + rate.toString();

                    orderAdjustments.add(delegator.makeValue("OrderAdjustment",
                            UtilMisc.toMap("amount", rec.getDouble("TAX_AMT_COUNTY"),
                                "orderAdjustmentTypeId", "SALES_TAX", "comments", comments)));
                }

                if (rec.getDouble("TAX_AMT_CITY").doubleValue() > 0) {
                    Double rate = new Double(rec.getDouble("TAX_RATE_CITY").doubleValue() * 100);
                    String type = rec.getString("TAX_TYPE_CITY").equals("S") ? "SALES TAX" : "USE TAX";
                    String jur = rec.get("JUR_CITY") != null ? rec.getString("JUR_CITY").trim() : "";
                    String comments = jur + "|" + type + "|" + rate.toString();

                    orderAdjustments.add(delegator.makeValue("OrderAdjustment",
                            UtilMisc.toMap("amount", rec.getDouble("TAX_AMT_CITY"),
                                "orderAdjustmentTypeId", "SALES_TAX", "comments", comments)));
                }

                if (rec.getDouble("TAX_AMT_SEC_STATE").doubleValue() > 0) {
                    Double rate = new Double(rec.getDouble("TAX_RATE_SEC_STATE").doubleValue() * 100);
                    String type = rec.getString("TAX_TYPE_SEC_STATE").equals("S") ? "SALES TAX" : "USE TAX";
                    String jur = rec.get("JUR_SEC_STATE") != null ? rec.getString("JUR_SEC_STATE").trim() : "";
                    String comments = jur + "|" + type + "|" + rate.toString();

                    orderAdjustments.add(delegator.makeValue("OrderAdjustment",
                            UtilMisc.toMap("amount", rec.getDouble("TAX_AMT_SEC_STATE"),
                                "orderAdjustmentTypeId", "SALES_TAX", "comments", comments)));
                }

                if (rec.getDouble("TAX_AMT_SEC_COUNTY").doubleValue() > 0) {
                    Double rate = new Double(rec.getDouble("TAX_RATE_SEC_COUNTY").doubleValue() * 100);
                    String type = rec.getString("TAX_TYPE_SEC_COUNTY").equals("S") ? "SALES TAX" : "USE TAX";
                    String jur = rec.get("JUR_SEC_COUNTY_CODE") != null ? rec.getString("JUR_SEC_COUNTY_CODE").trim() : "";
                    String comments = jur + "|" + type + "|" + rate.toString();

                    orderAdjustments.add(delegator.makeValue("OrderAdjustment",
                            UtilMisc.toMap("amount", rec.getDouble("TAX_AMT_SEC_COUNTY"),
                                "orderAdjustmentTypeId", "SALES_TAX", "comments", comments)));
                }

                if (rec.getDouble("TAX_AMT_SEC_CITY").doubleValue() > 0) {
                    Double rate = new Double(rec.getDouble("TAX_RATE_SEC_CITY").doubleValue() * 100);
                    String type = rec.getString("TAX_TYPE_SEC_CITY").equals("S") ? "SALES TAX" : "USE TAX";
                    String jur = rec.get("JUR_SEC_CITY") != null ? rec.getString("JUR_SEC_CITY").trim() : "";
                    String comments = jur + "|" + type + "|" + rate.toString();

                    orderAdjustments.add(delegator.makeValue("OrderAdjustment",
                            UtilMisc.toMap("amount", rec.getDouble("TAX_AMT_SEC_CITY"),
                                "orderAdjustmentTypeId", "SALES_TAX", "comments", comments)));
                }

            } else {
                throw new TaxwareException("Invalid number of return adjustments.");
            }

            for (int a = 0; a < model.fields.size(); a++) {
                ModelField mf = (ModelField) model.fields.get(a);
                String name = mf.name;
                String value = rec.getString(name);

                if (Debug.verboseOn()) Debug.logVerbose("Field: " + name + " => " + value, module);
            }
        }
        return retRecords.size();
    }
}
