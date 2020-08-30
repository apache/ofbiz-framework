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
import org.apache.poi.ss.usermodel.CellType;

public class ImportProductServices {

    private static final String MODULE = ImportProductServices.class.getName();
    private static final String RESOURCE = "ProductUiLabels";

    /**
     * This method is responsible to import spreadsheet data into "Product" and
     * "InventoryItem" entities into database. The method uses the
     * ImportProductHelper class to perform its operation. The method uses "Apache
     * POI" api for importing spreadsheet (xls files) data.
     * Note : Create the spreadsheet directory in the ofbiz home folder and keep
     * your xls files in this folder only.
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
        List<File> fileItems = new LinkedList<>();

        if (UtilValidate.isNotEmpty(path)) {
            File importDir = new File(path);
            if (importDir.isDirectory() && importDir.canRead()) {
                File[] files = importDir.listFiles();
                // loop for all the containing xls file in the spreadsheet
                // directory
                if (files == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "FileFilesIsNull", locale));
                }
                for (File file : files) {
                    if (file.getName().toUpperCase(Locale.getDefault()).endsWith("XLS")) {
                        fileItems.add(file);
                    }
                }
            } else {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "ProductProductImportDirectoryNotFound", locale));
            }
        } else {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "ProductProductImportPathNotSpecified", locale));
        }

        if (fileItems.size() < 1) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "ProductProductImportPathNoSpreadsheetExists", locale) + path);
        }

        for (File item: fileItems) {
            // read all xls file and create workbook one by one.
            List<Map<String, Object>> products = new LinkedList<>();
            List<Map<String, Object>> inventoryItems = new LinkedList<>();
            POIFSFileSystem fs = null;
            HSSFWorkbook wb = null;
            try {
                fs = new POIFSFileSystem(new FileInputStream(item));
                wb = new HSSFWorkbook(fs);
            } catch (IOException e) {
                Debug.logError("Unable to read or create workbook from file", MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
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
                    cell2.setCellType(CellType.STRING);
                    String productId = cell2.getRichStringCellValue().toString();
                    // read QOH from ninth column
                    HSSFCell cell5 = row.getCell(5);
                    BigDecimal quantityOnHand = BigDecimal.ZERO;
                    if (cell5 != null && cell5.getCellType() == CellType.NUMERIC) {
                        quantityOnHand = new BigDecimal(cell5.getNumericCellValue());
                    }

                    // check productId if null then skip creating inventory item
                    // too.
                    boolean productExists = ImportProductHelper.checkProductExists(productId, delegator);

                    if (!"".equalsIgnoreCase(productId.trim()) && !productExists) {
                        products.add(ImportProductHelper.prepareProduct(productId));
                        if (quantityOnHand.compareTo(BigDecimal.ZERO) >= 0) {
                            inventoryItems.add(ImportProductHelper.prepareInventoryItem(productId, quantityOnHand,
                                    delegator.getNextSeqId("InventoryItem")));
                        } else {
                            inventoryItems.add(ImportProductHelper.prepareInventoryItem(productId, BigDecimal.ZERO, delegator
                                    .getNextSeqId("InventoryItem")));
                        }
                    }
                    int rowNum = row.getRowNum() + 1;
                    if (!row.toString().trim().equalsIgnoreCase("") && productExists) {
                        Debug.logWarning("Row number " + rowNum + " not imported from " + item.getName(), MODULE);
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
                        Debug.logError("Cannot store product", MODULE);
                        return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                                "ProductProductImportCannotStoreProduct", locale));
                    }
                }
            }
            int uploadedProducts = products.size() + 1;
            if (!products.isEmpty()) {
                Debug.logInfo("Uploaded " + uploadedProducts + " products from file " + item.getName(), MODULE);
            }
        }
        return ServiceUtil.returnSuccess();
    }
}
