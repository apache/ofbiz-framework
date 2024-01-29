package com.simbaquartz.xapi.connect.utils;

import com.simbaquartz.xapi.connect.StringUtil;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilValidate;

import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by Joban on 7/5/17.
 */
public class XapiUtil {
    private static final String RFC_3339_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";

    public static String formatTimestamp(Timestamp dateTimeToFormat){
        return UtilDateTime.toDateString(new Date(dateTimeToFormat.getTime()), RFC_3339_DATE_TIME_FORMAT);
    }

    /**
     * Returns uploaded file name extracted from MultivaluedMap.
     * @param header
     * @return
     */
    public static String getFileName(MultivaluedMap<String, String> header) {

        String[] contentDisposition = header.getFirst("Content-Disposition").split(";");

        for (String filename : contentDisposition) {
            if ((filename.trim().startsWith("filename"))) {

                String[] name = filename.split("=");

                String finalFileName = name[1].trim().replaceAll("\"", "");
                return finalFileName;
            }
        }
        return "unknown";
    }

    public static List<Map<String, Object>> parseExcelSheet(InputStream inputStream) throws  IOException, InvalidFormatException {

        List<Map<String, Object>> records =  new LinkedList<Map<String,Object>>();

        // Creating a Workbook from an Excel file (.xls or .xlsx)
        Workbook wb = WorkbookFactory.create(inputStream);

        Sheet sheet = wb.getSheetAt(0);

        Iterator<Row> iterator = sheet.iterator();
        List<String> sheetHeaders = new LinkedList<String>();
        boolean isFirstRow = true;
        while (iterator.hasNext()) {
            boolean isRowEmpty = true;
            Row nextRow = iterator.next();
            Iterator<Cell> cellIterator = nextRow.cellIterator();
            Map<String, Object> data = new HashMap<String, Object>();
            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                cell.setCellType(CellType.STRING);
                if (isFirstRow) {
                    sheetHeaders.add(StringUtil.toCamelCase(cell.getStringCellValue()));
                } else {
                    String cellValue = cell.getStringCellValue();
                    if (UtilValidate.isEmpty(cellValue)) {
                        cellValue = "";
                    }
                    if(isRowEmpty && UtilValidate.isNotEmpty(cellValue)) {
                        isRowEmpty = false;
                    }
                    data.put(sheetHeaders.get(cell.getColumnIndex()), cellValue);
                }
            }

            if (!isFirstRow) {
                for(String header : sheetHeaders) {
                    if(!data.keySet().contains(header)) {
                        data.put(header, "");
                    }
                }
                if (!isRowEmpty) {
                    records.add(data);
                }
            }
            isFirstRow = false;
        }

        return records;
    }

}
