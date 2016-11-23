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
package org.apache.ofbiz.pricat;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;

/**
 * Interface of pricat parser.
 * 
 */
public interface InterfacePricatParser {
    
    public static final String PARSE_EXCEL = "parse_excel";
    
    public static final String CONFIRM = "confirm_action";
    
    public static final String[] messageLabels = new String[] {"FORMAT_DEFAULT", "FORMAT_WARNING", "FORMAT_HEADLINE", "FORMAT_NOTE", "FORMAT_OK", "FORMAT_ERROR", "FORMAT_THROWABLE"};
    
    public static final List<String> messages = Collections.unmodifiableList(Arrays.asList(messageLabels));
    
    public static final String tempFilesFolder = "runtime/pricat/";
    
    public static final String FileDateTimePattern = "yyyyMMddHHmmss";
    
    public static final String defaultColorName = "DefaultColor";
    
    public static final String defaultDimensionName = "DefaultDimension";
    
    public static final String defaultCategoryName = "DefaultCategory";
    
    public static final String EXCEL_TEMPLATE_TYPE = "excelTemplateType";
    
    public static final String FACILITY_ID = "facilityId";
    
    public static final String resource = "PricatUiLabels";
    
    public static final String PRICAT_FILE = "__PRICAT_FILE__";

    public static final String DEFAULT_PRICAT_TYPE = "ApacheOFBiz";
    
    public static final Map<String, String> PricatTypeLabels = UtilMisc.toMap(DEFAULT_PRICAT_TYPE, "ApacheOFBizPricatTemplate", "SamplePricat", "SamplePricatTemplate");
    
    public static final int HISTORY_MAX_FILENUMBER = UtilProperties.getPropertyAsInteger("pricat.properties", "pricat.history.max.filenumber", 20);
    
    abstract void parsePricatExcel();
    
    public void writeCommentsToFile(XSSFWorkbook workbook, XSSFSheet sheet);

    public void initBasicConds(List<String> orgPartyIds);

    public boolean existsCurrencyId(XSSFSheet sheet);

    abstract void parseRowByRow(XSSFSheet sheet);

    abstract boolean parseCellContentsAndStore(XSSFRow row, List<Object> cellContents) throws GenericTransactionException;
    
    public Map<String, Object> updateSkuPrice(String skuId, String ownerPartyId, BigDecimal memberPrice);

    abstract String updateSku(XSSFRow row, String productId, String ownerPartyId, String facilityId, String barcode, BigDecimal inventory,
            String colorId, String color, String dimensionId, String dimension, BigDecimal listPrice, BigDecimal averageCost);

    public Map<String, Object> updateColorAndDimension(String productId, String ownerPartyId, String color, String dimension);
    
    public Map<String, Object> getDimensionIds(String productId, String ownerPartyId, String dimension);
    
    public Map<String, Object> getColorIds(String productId, String ownerPartyId, String color);

    abstract String getProductId(XSSFRow row, String brandId, String modelName, String productName, String productCategoryId, String ownerPartyId, BigDecimal listPrice);

    public String getBrandId(String brandName, String ownerPartyId);

    abstract Object getCellContent(List<Object> cellContents, String colName);

    abstract String getProductCategoryId(List<Object> cellContents, String ownerPartyId);

    abstract boolean isFacilityOk(XSSFRow row, String facilityName, String facilityId);

    abstract List<Object> getCellContents(XSSFRow row, List<Object[]> colNames, int size);

    abstract boolean isTableHeaderMatched(XSSFSheet sheet);

    abstract boolean isVersionSupported(XSSFSheet sheet);

    abstract boolean containsDataRows(XSSFSheet sheet);

    public boolean isNumOfSheetsOK(XSSFWorkbook workbook);

    abstract void setFacilityId(String selectedFacilityId);

    public void endExcelImportHistory(String logFileName, String thruReasonId);
    
    public boolean hasErrorMessages();
}
