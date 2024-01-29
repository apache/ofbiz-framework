package com.fidelissd.zcp.xcommon.models.excel;

public enum ExcelColumnType {
    NUMBER(3), CURRENCY(8), TEXT(0), DATE(2);

    Integer excelCellStyleCode;

    ExcelColumnType(Integer styleCode) {
        this.excelCellStyleCode = styleCode;
    }

}
