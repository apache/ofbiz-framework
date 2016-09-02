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

package org.apache.ofbiz.product.spreadsheetimport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

public class ImportProductServices {

    public static String module = ImportProductServices.class.getName();
    public static final String resource = "ProductUiLabels";
    
    /**
     * This method is responsible to import spreadsheet data into "Product" and
     * "InventoryItem" entities into database. The method uses the
     * ImportProductHelper class to perform its operation. The method uses "Apache
     * POI" api for importing spreadsheet (xls files) data.
     *
     * Note : Create the spreadsheet directory in the ofbiz home folder and keep
     * your xls files in this folder only.
     *
     * @param dctx the dispatch context
     * @param context the context
     * @return the result of the service execution
     * @throws IOException 
     */
    public static Map<String, Object> productImportFromSpreadsheet(DispatchContext dctx, Map<String, ? extends Object> context) throws IOException {
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        // System.getProperty("user.dir") returns the path upto ofbiz home
        // directory
        String path = System.getProperty("user.dir") + "/spreadsheet";
        List<File> fileItems = new LinkedList<File>();

        if (UtilValidate.isNotEmpty(path)) {
            File importDir = new File(path);
            if (importDir.isDirectory() && importDir.canRead()) {
                File[] files = importDir.listFiles();
                // loop for all the containing xls file in the spreadsheet
                // directory
                for (int i = 0; i < files.length; i++) {
                    if (files[i].getName().toUpperCase().endsWith("XLS")) {
                        fileItems.add(files[i]);
                    }
                }
            } else {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                        "ProductProductImportDirectoryNotFound", locale));
            }
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "ProductProductImportPathNotSpecified", locale));
        }

        if (fileItems.size() < 1) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                    "ProductProductImportPathNoSpreadsheetExists", locale) + path);
        }

        for (File item: fileItems) {
            // read all xls file and create workbook one by one.
            List<Map<String, Object>> products = new LinkedList<Map<String,Object>>();
            List<Map<String, Object>> inventoryItems = new LinkedList<Map<String,Object>>();
            POIFSFileSystem fs = null;
            HSSFWorkbook wb = null;
            try {
                fs = new POIFSFileSystem(new FileInputStream(item));
                wb = new HSSFWorkbook(fs);
            } catch (IOException e) {
                Debug.logError("Unable to read or create workbook from file", module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                        "ProductProductImportCannotCreateWorkbookFromFile", locale));
            }

            // get first sheet
            HSSFSheet sheet = wb.getSheetAt(0);
            wb.close();
            int sheetLastRowNumber = sheet.getLastRowNum();
            for (int j = 1; j <= sheetLastRowNumber; j++) {
                HSSFRow row = sheet.getRow(j);
                if (row != null) {
                    // read productId from first column "sheet column index
                    // starts from 0"
                    HSSFCell cell2 = row.getCell(2);
                    cell2.setCellType(HSSFCell.CELL_TYPE_STRING);
                    String productId = cell2.getRichStringCellValue().toString();
                    // read QOH from ninth column
                    HSSFCell cell5 = row.getCell(5);
                    BigDecimal quantityOnHand = BigDecimal.ZERO;
                    if (cell5 != null && cell5.getCellType() == HSSFCell.CELL_TYPE_NUMERIC)
                        quantityOnHand = new BigDecimal(cell5.getNumericCellValue());

                    // check productId if null then skip creating inventory item
                    // too.
                    boolean productExists = ImportProductHelper.checkProductExists(productId, delegator);

                    if (productId != null && !productId.trim().equalsIgnoreCase("") && !productExists) {
                        products.add(ImportProductHelper.prepareProduct(productId));
                        if (quantityOnHand.compareTo(BigDecimal.ZERO) >= 0)
                            inventoryItems.add(ImportProductHelper.prepareInventoryItem(productId, quantityOnHand,
                                    delegator.getNextSeqId("InventoryItem")));
                        else
                            inventoryItems.add(ImportProductHelper.prepareInventoryItem(productId, BigDecimal.ZERO, delegator
                                    .getNextSeqId("InventoryItem")));
                    }
                    int rowNum = row.getRowNum() + 1;
                    if (row.toString() != null && !row.toString().trim().equalsIgnoreCase("") && productExists) {
                        Debug.logWarning("Row number " + rowNum + " not imported from " + item.getName(), module);
                    }
                }
            }
            // create and store values in "Product" and "InventoryItem" entity
            // in database
            for (int j = 0; j < products.size(); j++) {
                GenericValue productGV = delegator.makeValue("Product", products.get(j));
                GenericValue inventoryItemGV = delegator.makeValue("InventoryItem", inventoryItems.get(j));
                if (!ImportProductHelper.checkProductExists(productGV.getString("productId"), delegator)) {
                    try {
                        delegator.create(productGV);
                        delegator.create(inventoryItemGV);
                    } catch (GenericEntityException e) {
                        Debug.logError("Cannot store product", module);
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource, 
                                "ProductProductImportCannotStoreProduct", locale));
                    }
                }
            }
            int uploadedProducts = products.size() + 1;
            if (products.size() > 0)
                Debug.logInfo("Uploaded " + uploadedProducts + " products from file " + item.getName(), module);
        }
        return ServiceUtil.returnSuccess();
    }
}
