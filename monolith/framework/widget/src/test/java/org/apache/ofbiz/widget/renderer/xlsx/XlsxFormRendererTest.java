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
package org.apache.ofbiz.widget.renderer.xlsx;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.ofbiz.widget.model.ModelForm;
import org.apache.ofbiz.widget.model.ModelFormField;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

final class XlsxFormRendererTest {

    private final Map<String, Object> context = new HashMap<>();
    private final XSSFWorkbook workbook = new XSSFWorkbook();
    private final XlsxFormRenderer renderer = new XlsxFormRenderer(workbook);

    @Test
    void writesHeaderRowThenPlainTextItemRow() {
        ModelForm modelForm = mock(ModelForm.class);
        when(modelForm.getName()).thenReturn("TestForm");

        ModelFormField titleField = mock(ModelFormField.class, RETURNS_DEEP_STUBS);
        when(titleField.getTitle(context)).thenReturn("Product Name");

        ModelFormField.TextField textField = mock(ModelFormField.TextField.class, RETURNS_DEEP_STUBS);
        when(textField.getType()).thenReturn("");
        when(textField.getModelFormField().getEntry(context)).thenReturn("Widget");

        renderer.renderFormatListWrapperOpen(null, context, modelForm);
        renderer.renderFormatHeaderRowOpen(null, context, modelForm);
        renderer.renderFormatHeaderRowCellOpen(null, context, modelForm, titleField, 1);
        renderer.renderFieldTitle(null, context, titleField);
        renderer.renderFormatHeaderRowCellClose(null, context, modelForm, titleField);
        renderer.renderFormatHeaderRowClose(null, context, modelForm);

        renderer.renderFormatItemRowOpen(null, context, modelForm);
        renderer.renderFormatItemRowCellOpen(null, context, modelForm, titleField, 1);
        renderer.renderTextField(null, context, textField);
        renderer.renderFormatItemRowCellClose(null, context, modelForm, titleField);
        renderer.renderFormatItemRowClose(null, context, modelForm);
        renderer.renderFormatListWrapperClose(null, context, modelForm);

        Sheet sheet = renderer.getSheet();
        assertThat(sheet.getSheetName(), equalTo("TestForm"));
        assertThat(sheet.getRow(0).getCell(0).getStringCellValue(), equalTo("Product Name"));
        assertThat(sheet.getRow(1).getCell(0).getStringCellValue(), equalTo("Widget"));
    }

    @Test
    void writesCurrencyDisplayFieldAsNumericCell() {
        ModelForm modelForm = mock(ModelForm.class);
        when(modelForm.getName()).thenReturn("TestForm");

        ModelFormField.DisplayField priceField = mock(ModelFormField.DisplayField.class, RETURNS_DEEP_STUBS);
        when(priceField.getType()).thenReturn("currency");
        when(priceField.getModelFormField().getEntry(context)).thenReturn("19.99");
        when(priceField.getDescription(context)).thenReturn("$19.99");

        renderer.renderFormatListWrapperOpen(null, context, modelForm);
        renderer.renderFormatItemRowOpen(null, context, modelForm);
        renderer.renderFormatItemRowCellOpen(null, context, modelForm, priceField.getModelFormField(), 1);
        renderer.renderDisplayField(null, context, priceField);
        renderer.renderFormatItemRowCellClose(null, context, modelForm, priceField.getModelFormField());

        Cell cell = renderer.getSheet().getRow(0).getCell(0);
        assertThat(cell.getCellType(), equalTo(CellType.NUMERIC));
        assertThat(cell.getNumericCellValue(), equalTo(19.99));
    }

    @Test
    void fallsBackToStringWhenCurrencyValueIsUnparseable() {
        ModelForm modelForm = mock(ModelForm.class);
        when(modelForm.getName()).thenReturn("TestForm");

        ModelFormField.DisplayField priceField = mock(ModelFormField.DisplayField.class, RETURNS_DEEP_STUBS);
        when(priceField.getType()).thenReturn("currency");
        when(priceField.getModelFormField().getEntry(context)).thenReturn("");
        when(priceField.getDescription(context)).thenReturn("N/A");

        renderer.renderFormatListWrapperOpen(null, context, modelForm);
        renderer.renderFormatItemRowOpen(null, context, modelForm);
        renderer.renderFormatItemRowCellOpen(null, context, modelForm, priceField.getModelFormField(), 1);
        renderer.renderDisplayField(null, context, priceField);
        renderer.renderFormatItemRowCellClose(null, context, modelForm, priceField.getModelFormField());

        Cell cell = renderer.getSheet().getRow(0).getCell(0);
        assertThat(cell.getCellType(), equalTo(CellType.STRING));
        assertThat(cell.getStringCellValue(), equalTo("N/A"));
    }

    @Test
    void writesDateDisplayFieldAsRealDateCell() {
        ModelForm modelForm = mock(ModelForm.class);
        when(modelForm.getName()).thenReturn("TestForm");

        ModelFormField.DisplayField shipDateField = mock(ModelFormField.DisplayField.class, RETURNS_DEEP_STUBS);
        when(shipDateField.getType()).thenReturn("date");
        when(shipDateField.getModelFormField().getEntry(context)).thenReturn("2026-01-15");
        when(shipDateField.getDescription(context)).thenReturn("01/15/2026");

        renderer.renderFormatListWrapperOpen(null, context, modelForm);
        renderer.renderFormatItemRowOpen(null, context, modelForm);
        renderer.renderFormatItemRowCellOpen(null, context, modelForm, shipDateField.getModelFormField(), 1);
        renderer.renderDisplayField(null, context, shipDateField);
        renderer.renderFormatItemRowCellClose(null, context, modelForm, shipDateField.getModelFormField());

        Cell cell = renderer.getSheet().getRow(0).getCell(0);
        assertThat(DateUtil.isCellDateFormatted(cell), equalTo(true));
        assertThat(cell.getDateCellValue(), equalTo(Date.valueOf("2026-01-15")));
    }

    @Test
    void writesDateTimeFieldAsRealDateCell() {
        ModelForm modelForm = mock(ModelForm.class);
        when(modelForm.getName()).thenReturn("TestForm");

        ModelFormField.DateTimeField orderDateField = mock(ModelFormField.DateTimeField.class, RETURNS_DEEP_STUBS);
        when(orderDateField.isDateType()).thenReturn(false);
        when(orderDateField.isTimeType()).thenReturn(false);
        when(orderDateField.getModelFormField().getEntry(context)).thenReturn("2026-01-15 10:30:00.0");

        renderer.renderFormatListWrapperOpen(null, context, modelForm);
        renderer.renderFormatItemRowOpen(null, context, modelForm);
        renderer.renderFormatItemRowCellOpen(null, context, modelForm, orderDateField.getModelFormField(), 1);
        renderer.renderDateTimeField(null, context, orderDateField);
        renderer.renderFormatItemRowCellClose(null, context, modelForm, orderDateField.getModelFormField());

        Cell cell = renderer.getSheet().getRow(0).getCell(0);
        assertThat(DateUtil.isCellDateFormatted(cell), equalTo(true));
        assertThat(cell.getDateCellValue(), equalTo(java.sql.Timestamp.valueOf("2026-01-15 10:30:00.0")));
    }

    @Test
    void writesDropDownFieldRawValueAsString() {
        ModelForm modelForm = mock(ModelForm.class);
        when(modelForm.getName()).thenReturn("TestForm");

        ModelFormField.DropDownField statusField = mock(ModelFormField.DropDownField.class, RETURNS_DEEP_STUBS);
        when(statusField.getModelFormField().getEntry(context)).thenReturn("ORDER_APPROVED");

        renderer.renderFormatListWrapperOpen(null, context, modelForm);
        renderer.renderFormatItemRowOpen(null, context, modelForm);
        renderer.renderFormatItemRowCellOpen(null, context, modelForm, statusField.getModelFormField(), 1);
        renderer.renderDropDownField(null, context, statusField);
        renderer.renderFormatItemRowCellClose(null, context, modelForm, statusField.getModelFormField());

        Cell cell = renderer.getSheet().getRow(0).getCell(0);
        assertThat(cell.getStringCellValue(), equalTo("ORDER_APPROVED"));
    }

    @Test
    void writesSingleFormAsTitleValueRows() {
        ModelForm modelForm = mock(ModelForm.class);
        when(modelForm.getName()).thenReturn("DetailForm");

        ModelFormField nameField = mock(ModelFormField.class, RETURNS_DEEP_STUBS);
        when(nameField.getTitle(context)).thenReturn("Order Id");

        ModelFormField.TextField valueField = mock(ModelFormField.TextField.class, RETURNS_DEEP_STUBS);
        when(valueField.getType()).thenReturn("");
        when(valueField.getModelFormField().getEntry(context)).thenReturn("WS10000");

        renderer.renderFormatSingleWrapperOpen(null, context, modelForm);
        renderer.renderFormatFieldRowOpen(null, context, modelForm);
        renderer.renderFormatFieldRowTitleCellOpen(null, context, nameField);
        renderer.renderSingleFormFieldTitle(null, context, nameField);
        renderer.renderFormatFieldRowTitleCellClose(null, context, nameField);
        renderer.renderFormatFieldRowWidgetCellOpen(null, context, nameField, 1, 1, null);
        renderer.renderTextField(null, context, valueField);
        renderer.renderFormatFieldRowWidgetCellClose(null, context, nameField, 1, 1, null);
        renderer.renderFormatFieldRowClose(null, context, modelForm);

        Sheet sheet = renderer.getSheet();
        assertThat(sheet.getSheetName(), equalTo("DetailForm"));
        assertThat(sheet.getRow(0).getCell(0).getStringCellValue(), equalTo("Order Id"));
        assertThat(sheet.getRow(0).getCell(1).getStringCellValue(), equalTo("WS10000"));
    }

    @Test
    void writesBareHeaderTitlesWithoutExplicitCellOpenIntoDistinctColumns() {
        ModelForm modelForm = mock(ModelForm.class);
        when(modelForm.getName()).thenReturn("TestForm");

        ModelFormField firstField = mock(ModelFormField.class, RETURNS_DEEP_STUBS);
        when(firstField.getTitle(context)).thenReturn("Product Name");

        ModelFormField secondField = mock(ModelFormField.class, RETURNS_DEEP_STUBS);
        when(secondField.getTitle(context)).thenReturn("Quantity");

        renderer.renderFormatListWrapperOpen(null, context, modelForm);
        renderer.renderFormatHeaderRowOpen(null, context, modelForm);
        // No renderFormatHeaderRowCellOpen calls: modelForm.getSeparateColumns() and
        // modelFormField.getSeparateColumn() are both false, the default case, so FormRenderer
        // invokes renderFieldTitle directly for each field with no preceding cell-open.
        renderer.renderFieldTitle(null, context, firstField);
        renderer.renderFieldTitle(null, context, secondField);
        renderer.renderFormatHeaderRowClose(null, context, modelForm);

        Sheet sheet = renderer.getSheet();
        assertThat(sheet.getRow(0).getCell(0).getStringCellValue(), equalTo("Product Name"));
        assertThat(sheet.getRow(0).getCell(1).getStringCellValue(), equalTo("Quantity"));
    }
}
