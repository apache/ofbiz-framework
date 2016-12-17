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
package org.apache.ofbiz.pricat.sample;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.POIXMLException;
import org.apache.poi.hssf.usermodel.HSSFDataFormatter;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.commons.fileupload.FileItem;
import org.apache.ofbiz.pricat.AbstractPricatParser;
import org.apache.ofbiz.htmlreport.InterfaceReport;
import org.apache.ofbiz.order.finaccount.FinAccountHelper;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * Sample pricat excel parser.
 * 
 */
public class SamplePricatParser extends AbstractPricatParser {
    
    public static final String module = SamplePricatParser.class.getName();

    public static final Map<String, List<Object[]>> ColNamesList = UtilMisc.toMap("V1.1", genExcelHeaderNames("V1.1"));

    public static final int headerRowNo = 4;
    
    private List<String> headerColNames = new ArrayList<String>();
    
    public SamplePricatParser(LocalDispatcher dispatcher, Delegator delegator, Locale locale, InterfaceReport report, Map<String, String[]> facilities, File pricatFile, GenericValue userLogin) {
        super(dispatcher, delegator, locale, report, facilities, pricatFile, userLogin);
    }

    /**
     * Parse pricat excel file in xlsx format.
     * 
     */
    public void parsePricatExcel(boolean writeFile) {
        XSSFWorkbook workbook = null;
        try {
            // 1. read the pricat excel file
            FileInputStream is = new FileInputStream(pricatFile);
            
            // 2. use POI to load this bytes
            report.print(UtilProperties.getMessage(resource, "ParsePricatFileStatement", new Object[] { pricatFile.getName() }, locale), InterfaceReport.FORMAT_DEFAULT);
            try {
                workbook = new XSSFWorkbook(is);
                report.println(UtilProperties.getMessage(resource, "ok", locale), InterfaceReport.FORMAT_OK);
            } catch(IOException e) {
                report.println(e);
                report.println(UtilProperties.getMessage(resource, "PricatSuggestion", locale), InterfaceReport.FORMAT_ERROR);
                return;
            } catch(POIXMLException e) {
                report.println(e);
                report.println(UtilProperties.getMessage(resource, "PricatSuggestion", locale), InterfaceReport.FORMAT_ERROR);
                return;
            }
            
            // 3. only first sheet will be parsed
            // 3.1 verify the file has a sheet at least
            formatter = new HSSFDataFormatter(locale);
            isNumOfSheetsOK(workbook);
            
            // 3.2 verify the version is supported
            XSSFSheet sheet = workbook.getSheetAt(0);
            if (!isVersionSupported(sheet)) {
                return;
            }
            
            // 3.3 get currencyId
            existsCurrencyId(sheet);

            // 3.4 verify the table header row is just the same as column names, if not, print error and return
            if (!isTableHeaderMatched(sheet)) {
                return;
            }
            
            // 3.5 verify the first table has 6 rows at least
            containsDataRows(sheet);
            
            if (UtilValidate.isNotEmpty(errorMessages)) {
                report.println(UtilProperties.getMessage(resource, "HeaderContainsError", locale), InterfaceReport.FORMAT_ERROR);
                return;
            }
            
            // 4. parse data
            // 4.1 parse row by row and store the contents into database
            parseRowByRow(sheet);
            if (UtilValidate.isNotEmpty(errorMessages)) {
                report.println(UtilProperties.getMessage(resource, "DataContainsError", locale), InterfaceReport.FORMAT_ERROR);
                if (writeFile) {
                    sequenceNum = report.getSequenceNum();
                    writeCommentsToFile(workbook, sheet);
                }
            }
            
            // 5. clean up the log files and commented Excel files
            cleanupLogAndCommentedExcel();
        } catch (IOException e) {
            report.println(e);
            Debug.logError(e, module);
        } finally {
            if (UtilValidate.isNotEmpty(fileItems)) {
                // remove tmp files
                FileItem fi = null;
                for (int i = 0; i < fileItems.size(); i++) {
                    fi = fileItems.get(i);
                    fi.delete();
                }
            }
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    Debug.logError(e, module);
                }
            }
        }
    }

    public boolean existsCurrencyId(XSSFSheet sheet) {
        report.print(UtilProperties.getMessage(resource, "StartCheckCurrencyId", locale), InterfaceReport.FORMAT_NOTE);
        XSSFCell currencyIdCell = sheet.getRow(2).getCell(1);
        currencyId = currencyIdCell.getStringCellValue().trim().toUpperCase();
        if (UtilValidate.isEmpty(currencyId)) {
            String errorMessage = UtilProperties.getMessage(resource, "CurrencyIdRequired", locale);
            report.println(errorMessage, InterfaceReport.FORMAT_ERROR);
            errorMessages.put(new CellReference(currencyIdCell), errorMessage);
            return false;
        } else {
            try {
                GenericValue currencyUom = delegator.findOne("Uom", UtilMisc.toMap("uomId", currencyId), false);
                if (!"CURRENCY_MEASURE".equals(currencyUom.getString("uomTypeId"))) {
                    String errorMessage = UtilProperties.getMessage(resource, "CurrencyIdNotCurrency", new Object[] {currencyId}, locale);
                    report.println(errorMessage, InterfaceReport.FORMAT_ERROR);
                    errorMessages.put(new CellReference(currencyIdCell), errorMessage);
                    return false;
                }
            } catch(GenericEntityException e) {
                String errorMessage = UtilProperties.getMessage(resource, "CurrencyIdNotFound", new Object[] {currencyId}, locale);
                report.println(errorMessage, InterfaceReport.FORMAT_ERROR);
                errorMessages.put(new CellReference(currencyIdCell), errorMessage);
                return false;
            }
            report.print(UtilProperties.getMessage(resource, "CurrencyIdIs", new Object[] {currencyId}, locale), InterfaceReport.FORMAT_NOTE);
            report.println(" ... " + UtilProperties.getMessage(resource, "ok", locale), InterfaceReport.FORMAT_OK);
        }
        return true;
    }

    public void parseRowByRow(XSSFSheet sheet) {
        int rows = sheet.getPhysicalNumberOfRows();
        List<Object[]> colNames = ColNamesList.get(pricatFileVersion);
        int colNumber = colNames.size();

        int emptyRowStart = -1;
        int emptyRowEnd = -1;
        for (int i = headerRowNo + 1; i < rows; i++) {
            XSSFRow row = sheet.getRow(i);
            if (UtilValidate.isEmpty(row) || isEmptyRow(row, colNumber, false)) {
                if (emptyRowStart == -1) {
                    report.print("(" + (i + 1) + ") ", InterfaceReport.FORMAT_NOTE);
                    emptyRowStart = i;
                } else {
                    emptyRowEnd = i;
                }
                continue;
            } else {
                if (emptyRowStart != -1) {
                    if (emptyRowEnd != -1) {
                        report.print(" - (" + (emptyRowEnd + 1) + ") ", InterfaceReport.FORMAT_NOTE);
                    }
                    report.print(UtilProperties.getMessage(resource, "ExcelEmptyRow", locale), InterfaceReport.FORMAT_NOTE);
                    report.println(" ... " + UtilProperties.getMessage(resource, "skipped", locale), InterfaceReport.FORMAT_NOTE);
                    emptyRowStart = -1;
                    emptyRowEnd = -1;
                }
            }
            report.print("(" + (i + 1) + ") ", InterfaceReport.FORMAT_NOTE);
            List<Object> cellContents = getCellContents(row, colNames, colNumber);
            try {
                if (parseCellContentsAndStore(row, cellContents)) {
                    report.println(" ... " + UtilProperties.getMessage(resource, "ok", locale), InterfaceReport.FORMAT_OK);
                } else {
                    report.println(" ... " + UtilProperties.getMessage(resource, "skipped", locale), InterfaceReport.FORMAT_NOTE);
                }
            } catch (GenericTransactionException e) {
                report.println(e);
            }
        }
        if (emptyRowEnd != -1) {
            report.print(" - (" + (emptyRowEnd + 1) + ") ", InterfaceReport.FORMAT_NOTE);
            report.print(UtilProperties.getMessage(resource, "ExcelEmptyRow", locale), InterfaceReport.FORMAT_NOTE);
            report.println(" ... " + UtilProperties.getMessage(resource, "skipped", locale), InterfaceReport.FORMAT_NOTE);
        }
    }

    /**
     * Check data according to business logic. If data is ok, store it.
     * 
     * @param row
     * @param cellContents
     * @return
     * @throws GenericTransactionException 
     */
    public boolean parseCellContentsAndStore(XSSFRow row, List<Object> cellContents) throws GenericTransactionException {
        if (UtilValidate.isEmpty(cellContents)) {
            return false;
        }
        switch(pricatFileVersion) {
            case "V1.1":
            default:
                return parseCellContentsAndStoreV1_X(row, cellContents);
        }
    }
    
    private boolean parseCellContentsAndStoreV1_X(XSSFRow row, List<Object> cellContents) throws GenericTransactionException {
        if (UtilValidate.isEmpty(cellContents)) {
            return false;
        }
        // 1. check if facilityId is in the facilities belong to the user, or if the name is correct for the id
        String facilityName = (String) getCellContent(cellContents, "Facility Name");
        String facilityId = (String) getCellContent(cellContents, "FacilityId");
        if (!isFacilityOk(row, facilityName, facilityId)) 
            return false;
        
        // 2. get productCategoryId
        String ownerPartyId = facilities.get(facilityId)[1];
        String productCategoryId = getProductCategoryId(cellContents, ownerPartyId);
        
        // 3. get productFeatureId of brand
        String brandName = (String) getCellContent(cellContents, "Brand");
        String brandId = getBrandId(brandName, ownerPartyId);
        if (UtilValidate.isEmpty(brandId)) {
            return false;
        }

        // 4. get productId from brandId, model name
        String modelName = (String) getCellContent(cellContents, "Style No");
        String productName = (String) getCellContent(cellContents, "Product Name");
        BigDecimal listPrice = (BigDecimal) getCellContent(cellContents, "List Price");
        String productId = getProductId(row, brandId, modelName, productName, productCategoryId, ownerPartyId, listPrice);
        if (UtilValidate.isEmpty(productId) || UtilValidate.isEmpty(listPrice)) {
            return false;
        }

        // 5. update color and size if necessary
        String color = (String) getCellContent(cellContents, "Color");
        if (UtilValidate.isEmpty(color) || UtilValidate.isEmpty(color.trim())) {
            color = defaultColorName;
        }
        String dimension = (String) getCellContent(cellContents, "Size");
        if (UtilValidate.isEmpty(dimension) || UtilValidate.isEmpty(dimension.trim())) {
            dimension = defaultDimensionName;
        }
        Map<String, Object> features = updateColorAndDimension(productId, ownerPartyId, color, dimension);
        if (ServiceUtil.isError(features)) {
            if (features.containsKey("index") && String.valueOf(features.get("index")).contains("0")) {
                int cell = headerColNames.indexOf("Color");
                XSSFCell colorCell = row.getCell(cell);
                errorMessages.put(new CellReference(colorCell), UtilProperties.getMessage(resource, "PricatColorError", locale));
            }
            if (features.containsKey("index") && String.valueOf(features.get("index")).contains("1")) {
                int cell = headerColNames.indexOf("Size");
                XSSFCell colorCell = row.getCell(cell);
                errorMessages.put(new CellReference(colorCell), UtilProperties.getMessage(resource, "PricatDimensionError", locale));
            }
            return false;
        }
        String colorId = (String) features.get("colorId");
        String dimensionId = (String) features.get("dimensionId");
        
        // 6. update skuIds by productId
        String barcode = (String) getCellContent(cellContents, "Barcode");
        BigDecimal inventory = (BigDecimal) getCellContent(cellContents, "Stock Qty");
        BigDecimal averageCost = (BigDecimal) getCellContent(cellContents, "Average Cost");
        String skuId = updateSku(row, productId, ownerPartyId, facilityId, barcode, inventory, colorId, color, dimensionId, dimension, listPrice, averageCost);
        if (UtilValidate.isEmpty(skuId)) {
            return false;
        }
        
        // 7. store prices
        BigDecimal memberPrice = (BigDecimal) getCellContent(cellContents, "Member Price");
        Map<String, Object> results = updateSkuPrice(skuId, ownerPartyId, memberPrice);
        if (ServiceUtil.isError(results)) {
            return false;
        }
        
        return true;
    }


    public String updateSku(XSSFRow row, String productId, String ownerPartyId, String facilityId, String barcode, BigDecimal inventory,
            String colorId, String color, String dimensionId, String dimension, BigDecimal listPrice, BigDecimal averageCost) {
        return "sampleSkuId";
    }

    public String getProductId(XSSFRow row, String brandId, String modelName, String productName, String productCategoryId, String ownerPartyId, BigDecimal listPrice) {
        return "sampleProductId";
    }

    public Object getCellContent(List<Object> cellContents, String colName) {
        if (UtilValidate.isNotEmpty(headerColNames) && headerColNames.contains(colName)) {
            return cellContents.get(headerColNames.indexOf(colName));
        }
        return null;
    }

    public String getProductCategoryId(List<Object> cellContents, String ownerPartyId) {
        return "sampleProductCategoryId";
    }

    public boolean isFacilityOk(XSSFRow row, String facilityName, String facilityId) {
        if (!facilities.containsKey(facilityId)) {
            if (UtilValidate.isEmpty(facilityId) && facilities.keySet().size() == 1) {
                if (UtilValidate.isEmpty(facilityName)) {
                    return true;
                } else {
                    String theFacilityId = (String) facilities.keySet().toArray()[0];
                    String name = facilities.get(theFacilityId)[0];
                    if (!name.equals(facilityName)) {
                        String errorMessage = UtilProperties.getMessage(resource, "FacilityNameNotMatchId", new Object[]{theFacilityId, name, facilityName}, locale);
                        report.println();
                        report.print(errorMessage, InterfaceReport.FORMAT_ERROR);
                        XSSFCell cell = row.getCell(0);
                        errorMessages.put(new CellReference(cell), errorMessage);
                        return false;
                    }
                }
            } else {
                String errorMessage = UtilProperties.getMessage(resource, "FacilityNotBelongToYou", new Object[]{facilityName, facilityId}, locale);
                report.println();
                report.print(errorMessage, InterfaceReport.FORMAT_ERROR);
                XSSFCell cell = row.getCell(1);
                errorMessages.put(new CellReference(cell), errorMessage);
                return false;
            }
        } else {
            String name = facilities.get(facilityId)[0];
            if (!name.equals(facilityName)) {
                String errorMessage = UtilProperties.getMessage(resource, "FacilityNameNotMatchId", new Object[]{facilityId, name, facilityName}, locale);
                report.println();
                report.print(errorMessage, InterfaceReport.FORMAT_ERROR);
                XSSFCell cell = row.getCell(0);
                errorMessages.put(new CellReference(cell), errorMessage);
                return false;
            }
        }
        return true;
    }

    public boolean isTableHeaderMatched(XSSFSheet sheet) {
        List<Object[]> columnNames = ColNamesList.get(pricatFileVersion);
        short cols = sheet.getRow(headerRowNo).getLastCellNum();
        report.print(UtilProperties.getMessage(resource, "StartCheckHeaderColNum", new Object[] {pricatFileVersion}, locale), InterfaceReport.FORMAT_NOTE);
        if (cols != columnNames.size()) {
            report.print(UtilProperties.getMessage(resource, "HeaderColNumNotMatch", new Object[] {String.valueOf(cols), String.valueOf(columnNames.size())}, locale), InterfaceReport.FORMAT_WARNING);
            if (cols < columnNames.size()) {
                report.println(UtilProperties.getMessage(resource, "HeaderColNumShortThanRequired", new Object[] {String.valueOf(columnNames.size())}, locale), InterfaceReport.FORMAT_ERROR);
                return false;
            } else {
                report.println(UtilProperties.getMessage(resource, "UseHeaderColNum", new Object[] {String.valueOf(columnNames.size())}, locale), InterfaceReport.FORMAT_WARNING);
                cols = (short) columnNames.size();
            }
        } else {
            report.println(UtilProperties.getMessage(resource, "ok", locale), InterfaceReport.FORMAT_OK);
        }
        
        report.print(UtilProperties.getMessage(resource, "StartCheckHeaderColLabel", new Object[] {pricatFileVersion}, locale), InterfaceReport.FORMAT_NOTE);
        boolean foundLabelNotMatch = false;
        for (int i = 0; i < cols; i++) {
            String coltext = sheet.getRow(headerRowNo).getCell(i).getStringCellValue().trim();
            headerColNames.add(coltext);
            Object[] versionColumn = columnNames.get(i);
            if (!coltext.equals(versionColumn[0])) {
                report.println(UtilProperties.getMessage(resource, "HeaderColLabelNotMatch", new Object[] {String.valueOf(headerRowNo + 1), String.valueOf(i + 1), coltext, versionColumn[0]}, locale), InterfaceReport.FORMAT_ERROR);
                foundLabelNotMatch = true;
            } else {
                report.print(" " + coltext, InterfaceReport.FORMAT_NOTE);
                if (i < cols - 1) {
                    report.print(",", InterfaceReport.FORMAT_NOTE);
                }
            }
        }
        if (foundLabelNotMatch) {
            report.println();
            return false;
        }
        report.println(" ... " + UtilProperties.getMessage(resource, "ok", locale), InterfaceReport.FORMAT_OK);
        return true;
    }

    public boolean isVersionSupported(XSSFSheet sheet) {
        report.print(UtilProperties.getMessage(resource, "StartCheckPricatVersion", locale), InterfaceReport.FORMAT_NOTE);
        pricatFileVersion = sheet.getRow(2).getCell(0).getStringCellValue().trim();
        if (ColNamesList.containsKey(pricatFileVersion)) {
            report.print(" " + pricatFileVersion + " ... ", InterfaceReport.FORMAT_NOTE);
            report.println(UtilProperties.getMessage(resource, "ok", locale), InterfaceReport.FORMAT_OK);
        } else {
            report.println(UtilProperties.getMessage(resource, "error", locale), InterfaceReport.FORMAT_ERROR);
            report.println(UtilProperties.getMessage(resource, "PricatVersionNotSupport", new Object[] {pricatFileVersion}, locale), InterfaceReport.FORMAT_ERROR);
            return false;
        }
        return true;
    }

    public boolean containsDataRows(XSSFSheet sheet) {
        int rows = sheet.getPhysicalNumberOfRows();
        if (rows > headerRowNo + 1) {
            report.println(UtilProperties.getMessage(resource, "PricatTableRows", new Object[] {String.valueOf(headerRowNo + 1), String.valueOf(rows - headerRowNo - 1), sheet.getSheetName()}, locale), InterfaceReport.FORMAT_NOTE);
        } else {
            report.println(UtilProperties.getMessage(resource, "PricatNoDataRows", new Object[] {sheet.getSheetName()}, locale), InterfaceReport.FORMAT_ERROR);
            return false;
        }
        return true;
    }

    /**
     * The Object[] have 4 elements, they are:
     * 1. Header Label Name.
     * 2. Cell data type to return.
     * 3. Boolean value to indicate whether the column is required.
     * 4. Boolean value to indicate whether the column is a price when cell data type is BigDecimal, this element is optional.
     * 
     * @param version
     * @return List of Object[]
     */
    private static List<Object[]> genExcelHeaderNames(String version){
        switch (version) {
            case "V1.1":
            default:
                return genExcelHeaderNamesV1_1();
        }
    }

    /**
     * Get V1.1 pricat excel header names and attributes. 
     * 
     * @return list of Object[]
     */
    private static List<Object[]> genExcelHeaderNamesV1_1() {
        List<Object[]> listHeaderName = new ArrayList<Object[]>();
        listHeaderName.add(new Object[] {"Facility Name", 
                                         XSSFCell.CELL_TYPE_STRING,
                                         Boolean.TRUE});
        listHeaderName.add(new Object[] {"FacilityId",
                                         XSSFCell.CELL_TYPE_STRING,
                                         Boolean.TRUE});
        listHeaderName.add(new Object[] {"Category L1",
                                         XSSFCell.CELL_TYPE_STRING,
                                         Boolean.FALSE});
        listHeaderName.add(new Object[] {"Category L2",
                                         XSSFCell.CELL_TYPE_STRING,
                                         Boolean.FALSE});
        listHeaderName.add(new Object[] {"Category L3",
                                         XSSFCell.CELL_TYPE_STRING,
                                         Boolean.FALSE});
        listHeaderName.add(new Object[] {"Category L4",
                                         XSSFCell.CELL_TYPE_STRING,
                                         Boolean.FALSE});
        listHeaderName.add(new Object[] {"Brand",
                                         XSSFCell.CELL_TYPE_STRING,
                                         Boolean.TRUE});
        listHeaderName.add(new Object[] {"Style No",
                                         XSSFCell.CELL_TYPE_STRING,
                                         Boolean.TRUE});
        listHeaderName.add(new Object[] {"Product Name",
                                         XSSFCell.CELL_TYPE_STRING,
                                         Boolean.TRUE});
        listHeaderName.add(new Object[] {"Color",
                                         XSSFCell.CELL_TYPE_STRING,
                                         Boolean.FALSE});
        listHeaderName.add(new Object[] {"Size",
                                         XSSFCell.CELL_TYPE_STRING,
                                         Boolean.FALSE});
        listHeaderName.add(new Object[] {"Barcode",
                                         XSSFCell.CELL_TYPE_STRING,
                                         Boolean.FALSE});
        listHeaderName.add(new Object[] {"Stock Qty",
                                         XSSFCell.CELL_TYPE_NUMERIC,
                                         Boolean.TRUE});
        listHeaderName.add(new Object[] {"Average Cost",
                                         XSSFCell.CELL_TYPE_NUMERIC,
                                         Boolean.TRUE,
                                         Boolean.TRUE});
        listHeaderName.add(new Object[] {"List Price",
                                         XSSFCell.CELL_TYPE_NUMERIC,
                                         Boolean.TRUE,
                                         Boolean.TRUE});
        listHeaderName.add(new Object[] {"Member Price",
                                            XSSFCell.CELL_TYPE_NUMERIC,
                                            Boolean.FALSE,
                                            Boolean.TRUE});
        return listHeaderName;
    }

    @Override
    public void parsePricatExcel() {
        parsePricatExcel(true);
    }

    /**
     * Get data by version definition.
     * 
     * @param row
     * @param colNames 
     * @param size 
     * @return
     */
    public List<Object> getCellContents(XSSFRow row, List<Object[]> colNames, int size) {
        List<Object> results = new ArrayList<Object>();
        boolean foundError = false;
        if (isEmptyRow(row, size, true)) {
            return null;
        }
        
        // check and get data
        for (int i = 0; i < size; i++) {
            XSSFCell cell = null;
            if (row.getPhysicalNumberOfCells() > i) {
                cell = row.getCell(i);
            }
            if (cell == null) {
                if (((Boolean) colNames.get(i)[2]).booleanValue() && (facilities.keySet().size() > 1 || (facilities.keySet().size() == 1 && i >= 2))) {
                    report.print(UtilProperties.getMessage(resource, "ErrorColCannotEmpty", new Object[] {colNames.get(i)[0]}, locale), InterfaceReport.FORMAT_WARNING);
                    cell = row.createCell(i);
                    errorMessages.put(new CellReference(cell), UtilProperties.getMessage(resource, "ErrorColCannotEmpty", new Object[] {colNames.get(i)[0]}, locale));
                    foundError = true;
                    results.add(null);
                    continue;
                } else {
                    cell = row.createCell(i);
                }
            }
            int cellType = cell.getCellType();
            String cellValue = formatter.formatCellValue(cell);
            if (UtilValidate.isNotEmpty(cellValue) && UtilValidate.isNotEmpty(cellValue.trim())) {
                if (cellType == XSSFCell.CELL_TYPE_FORMULA) {
                    try {
                        cellValue = BigDecimal.valueOf(cell.getNumericCellValue()).setScale(FinAccountHelper.decimals, FinAccountHelper.rounding).toString();
                    } catch (IllegalStateException e) {
                        try {
                            cellValue = cell.getStringCellValue();
                        } catch (IllegalStateException e1) {
                            // do nothing
                        }
                    }
                    report.print(((i == 0)?"":", ") + cellValue, InterfaceReport.FORMAT_NOTE);
                } else {
                    report.print(((i == 0)?"":", ") + cellValue, InterfaceReport.FORMAT_NOTE);
                }
            } else {
                report.print(((i == 0)?"":","), InterfaceReport.FORMAT_NOTE);
            }
            if (((Boolean) colNames.get(i)[2]).booleanValue() && UtilValidate.isEmpty(cellValue) && (facilities.keySet().size() > 1 || (facilities.keySet().size() == 1 && i >= 2))) {
                report.print(UtilProperties.getMessage(resource, "ErrorColCannotEmpty", new Object[] {colNames.get(i)[0]}, locale), InterfaceReport.FORMAT_WARNING);
                errorMessages.put(new CellReference(cell), UtilProperties.getMessage(resource, "ErrorColCannotEmpty", new Object[] {colNames.get(i)[0]}, locale));
                foundError = true;
                results.add(null);
                continue;
            }
            if (((Boolean) colNames.get(i)[2]).booleanValue() && cellType != (int) colNames.get(i)[1]) {
                // String warningMessage = "";
                if ((int) colNames.get(i)[1] == XSSFCell.CELL_TYPE_STRING) {
                    if (UtilValidate.isNotEmpty(cellValue) && UtilValidate.isNotEmpty(cellValue.trim())) {
                        results.add(cellValue);
                    } else {
                        results.add(null);
                    }
                } else if ((int) colNames.get(i)[1] == XSSFCell.CELL_TYPE_NUMERIC) {
                    if (cell.getCellType() != XSSFCell.CELL_TYPE_STRING) {
                        cell.setCellType(XSSFCell.CELL_TYPE_STRING);
                    }
                    try {
                        results.add(BigDecimal.valueOf(Double.parseDouble(cell.getStringCellValue())).setScale(FinAccountHelper.decimals, FinAccountHelper.rounding));
                    } catch (NumberFormatException e) {
                        results.add(null);
                        errorMessages.put(new CellReference(cell), UtilProperties.getMessage(resource, "ErrorParseValueToNumeric", locale));
                    }
                }
            } else {
                if (UtilValidate.isEmpty(cellValue) || UtilValidate.isEmpty(cellValue.trim())) {
                    results.add(null);
                    continue;
                }
                if ((int) colNames.get(i)[1] == XSSFCell.CELL_TYPE_STRING) {
                    if (cell.getCellType() == XSSFCell.CELL_TYPE_STRING) {
                        cellValue = cell.getStringCellValue().trim();
                        results.add(cellValue);
                    } else {
                        results.add(cellValue.trim());
                    }
                } else if ((int) colNames.get(i)[1] == XSSFCell.CELL_TYPE_NUMERIC) {
                    if (cell.getCellType() == XSSFCell.CELL_TYPE_STRING) {
                        try {
                            results.add(BigDecimal.valueOf(Double.valueOf(cell.getStringCellValue())));
                        } catch (NumberFormatException e) {
                            results.add(null);
                            errorMessages.put(new CellReference(cell), UtilProperties.getMessage(resource, "ErrorParseValueToNumeric", locale));
                        }
                    } else if (cell.getCellType() == XSSFCell.CELL_TYPE_NUMERIC) {
                        try {
                            results.add(BigDecimal.valueOf(cell.getNumericCellValue()).setScale(FinAccountHelper.decimals, FinAccountHelper.rounding));
                        } catch (NumberFormatException e) {
                            results.add(null);
                            errorMessages.put(new CellReference(cell), UtilProperties.getMessage(resource, "ErrorParseValueToNumeric", locale));
                        }
                    } else {
                        try {
                            results.add(BigDecimal.valueOf(Double.valueOf(cellValue)).setScale(FinAccountHelper.decimals, FinAccountHelper.rounding));
                        } catch (NumberFormatException e) {
                            results.add(null);
                            errorMessages.put(new CellReference(cell), UtilProperties.getMessage(resource, "ErrorParseValueToNumeric", locale));
                        }
                    }
                }
            }
        }
        if (foundError) {
            return null;
        }
        return results;
    }

    protected int getHeaderRowNo() {
        return headerRowNo;
    }
}
