/*
 * *****************************************************************************************
 *  * Copyright (c) Fidelis Sustainability Distribution, LLC 2015. - All Rights Reserved     *
 *  * Unauthorized copying of this file, via any medium is strictly prohibited               *
 *  * Proprietary and confidential                                                           *
 *  * Written by Forrest Rae <forrest.rae@fidelissd.com>, December, 2016                       *
 *  *****************************************************************************************
 */

package com.simbaquartz.xcommon.util;

import com.simbaquartz.xcommon.collections.FastMap;
import com.simbaquartz.xcommon.models.excel.ExcelColumn;
import com.simbaquartz.xcommon.models.excel.ExcelColumnType;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.CreationHelper;

public class UtilExport {
    private static final String module = UtilExport.class.getName();

    public static void flushExcelToResponse(String fileName, List<String> columnNames,
                                            List<Map<String, String>> records, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/vnd.ms-excel");
        HSSFWorkbook wb = prepareExcel(columnNames, records);
        String contentDispositionValue = "attachment;filename=CategoryMembers_"
                + fileName + ".xls";
        contentDispositionValue = contentDispositionValue.replaceAll(" ",
                "_");
        response.setHeader("Content-Disposition", contentDispositionValue);
        // Write the output
        OutputStream out = response.getOutputStream();
        wb.write(out);
        out.close();
    }

    public static void flushExcelToResponseWithFileName(String fileName, List<String> columnNames,
                                                        List<Map<String, String>> records, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/vnd.ms-excel");
        HSSFWorkbook wb = prepareExcel(columnNames, records);
        String contentDispositionValue = "attachment;filename="
                + fileName;
        contentDispositionValue = contentDispositionValue.replaceAll(" ",
                "_");
        response.setHeader("Content-Disposition", contentDispositionValue);
        // Write the output
        OutputStream out = response.getOutputStream();
        wb.write(out);
        out.close();
    }

    public static void flushExcelToOutputStream(List<String> columnNames,
                                                List<Map<String, String>> records, OutputStream out)
            throws ServletException, IOException {
        HSSFWorkbook wb = prepareExcel(columnNames, records);
        // Write the output
        wb.write(out);
        out.close();
    }

    public static HSSFWorkbook prepareExcelSheet(List<ExcelColumn> columns,
                                            List<Map<String, String>> records) {
        HSSFWorkbook workbook = null;
        try {
            workbook = new HSSFWorkbook();
            CellStyle numberCellStyle = workbook.createCellStyle();
            numberCellStyle.setDataFormat((short)3);
            CellStyle bigDecimalCellStyle = workbook.createCellStyle();
            bigDecimalCellStyle.setDataFormat((short)8);

            HSSFSheet sheet = workbook.createSheet("Sheet1");
            for (int colIndex = 0; colIndex < columns.size(); colIndex++) {
                sheet.setColumnWidth(colIndex, 3500);
            }

            HSSFRow rowHeader = sheet.createRow(0);

            for (int colIndex = 0; colIndex < columns.size(); colIndex++) {
                String columnName = columns.get(colIndex).getName();
                rowHeader.createCell(colIndex).setCellValue(columnName);
            }

            int rowCounter = 1;
            for (Map<String, String> record : records) {
                HSSFRow row = sheet.createRow(rowCounter);
                for (int colIndex = 0; colIndex < columns.size(); colIndex++) {
                    ExcelColumn column = columns.get(colIndex);
                    String columnName = column.getName();
                    if(column.getExcelColumnType().equals(ExcelColumnType.NUMBER)) {
                        Long lng = Long.parseLong(record.get(columnName));
                        HSSFCell cell = row.createCell(colIndex);
                        cell.setCellValue(lng);
                        cell.setCellStyle(numberCellStyle);
                    } else if(column.getExcelColumnType().equals(ExcelColumnType.CURRENCY)) {
                        BigDecimal bd = new BigDecimal(record.get(columnName));
                        HSSFCell cell = row.createCell(colIndex);
                        cell.setCellValue(bd.doubleValue());
                        cell.setCellStyle(bigDecimalCellStyle);
//                    } else if(column.getExcelColumnType().equals(ExcelColumnType.DATE)) {
//                        CellStyle dateCellStyle = workbook.createCellStyle();
//                        CreationHelper createHelper = workbook.getCreationHelper();
//                        dateCellStyle.setDataFormat(
//                            createHelper.createDataFormat().getFormat("MMM dd, yyyy"));
//                        HSSFCell cell = row.createCell(colIndex);
//                        cell.setCellValue(record.get(columnName));
//                        cell.setCellStyle(dateCellStyle);
                    } else {
                        row.createCell(colIndex).setCellValue(record.get(columnName));
                    }
                }
                rowCounter++;
            }

            // Auto-width columns
            for (int colIndex = 0; colIndex < columns.size(); colIndex++) {
                sheet.autoSizeColumn(colIndex);
            }
        } catch (Exception e) {
            Debug.logError("There was a error preparing excel file. Error: " + e.getMessage(), module);
            e.printStackTrace();
        }
        return workbook;
    }

    @Deprecated
    /**
     * use the new prepareExcelSheet with ExcelColumn parameter,
     * which has ability to define column types like Number, Currency etc
     */
    public static HSSFWorkbook prepareExcel(List<String> columnNames,
                                            List<Map<String, String>> records) {
        HSSFWorkbook workbook = null;
        try {
            workbook = new HSSFWorkbook();
            CellStyle numberCellStyle = workbook.createCellStyle();
            numberCellStyle.setDataFormat((short)3);
            CellStyle bigDecimalCellStyle = workbook.createCellStyle();
            bigDecimalCellStyle.setDataFormat((short)8);

            HSSFSheet sheet = workbook.createSheet("Sheet1");
            for (int colIndex = 0; colIndex < columnNames.size(); colIndex++) {
                sheet.setColumnWidth(colIndex, 3500);
            }

            HSSFRow rowHeader = sheet.createRow(0);

            for (int colIndex = 0; colIndex < columnNames.size(); colIndex++) {
                rowHeader.createCell(colIndex).setCellValue(columnNames.get(colIndex));
            }

            int rowCounter = 1;
            for (Map<String, String> record : records) {
                HSSFRow row = sheet.createRow(rowCounter);

                for (int colIndex = 0; colIndex < columnNames.size(); colIndex++) {
                    row.createCell(colIndex).setCellValue(record.get(columnNames.get(colIndex)));
                }

                rowCounter++;
            }

            // Auto-width columns
            for (int colIndex = 0; colIndex < columnNames.size(); colIndex++) {
                sheet.autoSizeColumn(colIndex);
            }
        } catch (Exception e) {
            Debug.logError("There was a error preparing excel file. Error: " + e.getMessage(), module);
            e.printStackTrace();
        }
        return workbook;
    }

    /**
     * @param inputWorkbook
     * @param columnNames   should be in the same order as listed in the excel
     * @param startFromRow  row to start from, use 1 to skip top row in case it has header names
     * @return
     */
    public static List<Map<String, Object>> readExcel(InputStream inputWorkbook, List<String> columnNames, int startFromRow) {
        Workbook w;
        try {
            List<Map<String, Object>> records = new ArrayList<Map<String, Object>>();

            w = Workbook.getWorkbook(inputWorkbook);
            // Get the master data sheet
            Sheet sheet = w.getSheet(0);
            int rows = sheet.getRows();
            int columns = sheet.getColumns();

            boolean dataParsed = false;
            // no spaces in product id
            for (int rowIndex = startFromRow; rowIndex < rows; rowIndex++) {
                if (dataParsed)
                    break;
                Map<String, Object> rowRecord = FastMap.newInstance();

                for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
                    Cell cell = sheet.getCell(columnIndex, rowIndex);
                    String cellValue = (cell.getContents() == null) ? "" : cell
                            .getContents();

                    //if first column has blank value break out;
                    if (columnIndex == 0 && UtilValidate.isEmpty(cellValue)) {
                        dataParsed = true;
                        break;
                    }

                    rowRecord.put(columnNames.get(columnIndex), cellValue);
                }

                if (!dataParsed) {
                    records.add(rowRecord);
                }
            }


            return records;
        } catch (BiffException e) {
            Debug.logError(e, module);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static List<String> readExcelColumns(InputStream inputWorkbook) {
        Workbook w;
        try {
            List<String> columnNames = new ArrayList<String>();

            w = Workbook.getWorkbook(inputWorkbook);
            // Get the master data sheet
            Sheet sheet = w.getSheet(0);
            int columns = sheet.getColumns();

            for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
                Cell cell = sheet.getCell(columnIndex, 0);
                String cellValue = (cell.getContents() == null) ? "" : cell
                        .getContents();

                //if first column has blank value break out;
                if (columnIndex == 0 && UtilValidate.isEmpty(cellValue)) {
                    break;
                }

                columnNames.add(cellValue);
            }

            return columnNames;
        } catch (BiffException e) {
            Debug.logError(e, module);
        } catch (IOException e) {
            Debug.logError(e, module);
        }

        return null;
    }

    public static void flushExcelToResponseWithFileNameForAwards(String fileName, List<String> columnNames,
                                                                 List<Map<String, String>> records, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/vnd.ms-excel");
        HSSFWorkbook wb = prepareExcelForAwards(columnNames, records);
        String contentDispositionValue = "attachment;filename="
                + fileName;
        contentDispositionValue = contentDispositionValue.replaceAll(" ",
                "_");
        response.setHeader("Content-Disposition", contentDispositionValue);
        // Write the output
        OutputStream out = response.getOutputStream();
        wb.write(out);
        out.close();
    }

    public static HSSFWorkbook prepareExcelForAwards(List<String> columnNames,
                                                     List<Map<String, String>> records) {
        HSSFWorkbook workbook = null;
        try {
            workbook = new HSSFWorkbook();
            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setDataFormat((short) 8);

            HSSFSheet sheet = workbook.createSheet("Sheet1");
            for (int colIndex = 0; colIndex < columnNames.size(); colIndex++) {
                sheet.setColumnWidth(colIndex, 3500);
            }

            HSSFRow rowHeader = sheet.createRow(0);

            for (int colIndex = 0; colIndex < columnNames.size(); colIndex++) {
                rowHeader.createCell(colIndex).setCellValue(columnNames.get(colIndex));
                ;
            }

            int rowCounter = 1;
            for (Map<String, String> record : records) {
                HSSFRow row = sheet.createRow(rowCounter);

                for (int colIndex = 0; colIndex < columnNames.size(); colIndex++) {
                    if (colIndex == 11 || colIndex == 12 || colIndex == 14 || colIndex == 15) {
                        BigDecimal bd = new BigDecimal(record.get(columnNames.get(colIndex)));
                        HSSFCell cell = row.createCell(colIndex);
                        cell.setCellValue(bd.doubleValue());
                        cell.setCellStyle(cellStyle);
                        //row.createCell(colIndex).setCellValue(bd.doubleValue());
                    } else {
                        HSSFCell cell = row.createCell(colIndex);
                        cell.setCellValue(record.get(columnNames.get(colIndex)).toString());
                        //   row.createCell(colIndex).setCellValue(record.get(columnNames.get(colIndex)).toString());
                    }
                    /*row.createCell(colIndex).setCellValue(record.get(columnNames.get(colIndex)));*/
                }

                rowCounter++;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return workbook;

    }

    public static void flushExcelToResponseWithFileNameForAwardCompletionStatus(String fileName, List<String> columnNames,
                                                                                List<Map<String, String>> records, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/vnd.ms-excel");
        HSSFWorkbook wb = prepareExcelForAwardCompletionStatus(columnNames, records);
        String contentDispositionValue = "attachment;filename="
                + fileName;
        contentDispositionValue = contentDispositionValue.replaceAll(" ",
                "_");
        response.setHeader("Content-Disposition", contentDispositionValue);
        // Write the output
        OutputStream out = response.getOutputStream();
        wb.write(out);
        out.close();
    }

    public static HSSFWorkbook prepareExcelForAwardCompletionStatus(List<String> columnNames,
                                                                    List<Map<String, String>> records) {
        HSSFWorkbook workbook = null;
        try {
            workbook = new HSSFWorkbook();
            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setDataFormat((short) 8);

            HSSFSheet sheet = workbook.createSheet("Sheet1");
            for (int colIndex = 0; colIndex < columnNames.size(); colIndex++) {
                sheet.setColumnWidth(colIndex, 3500);
            }

            HSSFRow rowHeader = sheet.createRow(0);

            for (int colIndex = 0; colIndex < columnNames.size(); colIndex++) {
                rowHeader.createCell(colIndex).setCellValue(columnNames.get(colIndex));
                ;
            }

            int rowCounter = 1;
            for (Map<String, String> record : records) {
                HSSFRow row = sheet.createRow(rowCounter);

                for (int colIndex = 0; colIndex < columnNames.size(); colIndex++) {
                    if (colIndex == 5) {
                        BigDecimal bd = new BigDecimal(record.get(columnNames.get(colIndex)));
                        HSSFCell cell = row.createCell(colIndex);
                        cell.setCellValue(bd.doubleValue());
                        cell.setCellStyle(cellStyle);
                        //row.createCell(colIndex).setCellValue(bd.doubleValue());
                    } else {
                        HSSFCell cell = row.createCell(colIndex);
                        cell.setCellValue(record.get(columnNames.get(colIndex)).toString());
                        //   row.createCell(colIndex).setCellValue(record.get(columnNames.get(colIndex)).toString());
                    }
                    /*row.createCell(colIndex).setCellValue(record.get(columnNames.get(colIndex)));*/
                }

                rowCounter++;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return workbook;

    }

    public static void flushExcelToResponseWithFileNameForFinanceReport(String fileName, List<String> columnNames,
                                                                        List<Map<String, String>> records, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/vnd.ms-excel");
        HSSFWorkbook wb = prepareExcelForFinanceReport(columnNames, records);
        String contentDispositionValue = "attachment;filename="
                + fileName;
        contentDispositionValue = contentDispositionValue.replaceAll(" ",
                "_");
        response.setHeader("Content-Disposition", contentDispositionValue);
        // Write the output
        OutputStream out = response.getOutputStream();
        wb.write(out);
        out.close();
    }

    public static HSSFWorkbook prepareExcelForFinanceReport(List<String> columnNames,
                                                            List<Map<String, String>> records) {
        HSSFWorkbook workbook = null;
        try {
            workbook = new HSSFWorkbook();
            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setDataFormat((short) 8);

            HSSFSheet sheet = workbook.createSheet("Sheet1");
            for (int colIndex = 0; colIndex < columnNames.size(); colIndex++) {
                sheet.setColumnWidth(colIndex, 3500);
            }

            HSSFRow rowHeader = sheet.createRow(0);

            for (int colIndex = 0; colIndex < columnNames.size(); colIndex++) {
                rowHeader.createCell(colIndex).setCellValue(columnNames.get(colIndex));
                ;
            }

            int rowCounter = 1;
            for (Map<String, String> record : records) {
                HSSFRow row = sheet.createRow(rowCounter);

                for (int colIndex = 0; colIndex < columnNames.size(); colIndex++) {
                    if (colIndex == 13 || colIndex == 14 || colIndex == 15 || colIndex == 18 || colIndex == 19 || colIndex == 20 || colIndex == 21 || colIndex == 22 || colIndex == 23 || colIndex == 24) {

                        BigDecimal bd = new BigDecimal(record.get(columnNames.get(colIndex)));
                        HSSFCell cell = row.createCell(colIndex);
                        cell.setCellValue(bd.doubleValue());
                        cell.setCellStyle(cellStyle);
                        //row.createCell(colIndex).setCellValue(bd.doubleValue());
                    } else {
                        HSSFCell cell = row.createCell(colIndex);
                        cell.setCellValue(record.get(columnNames.get(colIndex)).toString());
                        //   row.createCell(colIndex).setCellValue(record.get(columnNames.get(colIndex)).toString());
                    }
					/*row.createCell(colIndex).setCellValue(record.get(columnNames.get(colIndex)));*/
                }

                rowCounter++;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return workbook;

    }
}