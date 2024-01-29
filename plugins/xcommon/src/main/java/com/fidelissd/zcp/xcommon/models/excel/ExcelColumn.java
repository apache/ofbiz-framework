package com.fidelissd.zcp.xcommon.models.excel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExcelColumn {
    private String name;
    @Builder.Default
    private ExcelColumnType excelColumnType = ExcelColumnType.TEXT;
}