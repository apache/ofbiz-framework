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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Map;

import org.apache.ofbiz.widget.model.ModelForm;
import org.apache.ofbiz.widget.model.ModelFormField;
import org.apache.ofbiz.widget.renderer.FormStringRenderer;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;

/**
 * Writes POI spreadsheet cells directly from widget form/grid rendering callbacks,
 * in place of the macro-call text a {@code FormStringRenderer} normally produces.
 * Used by {@code ScreenXlsxViewHandler} to build a real binary .xlsx workbook.
 */
public final class XlsxFormRenderer implements FormStringRenderer {

    private final Workbook workbook;
    private final CellStyle numericStyle;
    private final CellStyle dateStyle;
    private final CellStyle dateTimeStyle;

    private Sheet sheet;
    private Row currentRow;
    private Cell currentCell;
    private int rowIndex;
    private int colIndex;

    public XlsxFormRenderer(Workbook workbook) {
        this.workbook = workbook;
        CreationHelper creationHelper = workbook.getCreationHelper();

        this.numericStyle = workbook.createCellStyle();
        this.numericStyle.setDataFormat(creationHelper.createDataFormat().getFormat("#,##0.00"));

        this.dateStyle = workbook.createCellStyle();
        this.dateStyle.setDataFormat(creationHelper.createDataFormat().getFormat("m/d/yyyy"));

        this.dateTimeStyle = workbook.createCellStyle();
        this.dateTimeStyle.setDataFormat(creationHelper.createDataFormat().getFormat("m/d/yyyy h:mm"));
    }

    Sheet getSheet() {
        return sheet;
    }

    @Override
    public void renderFormatListWrapperOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) {
        this.sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName(modelForm.getName()));
        this.rowIndex = 0;
        this.colIndex = 0;
    }

    @Override
    public void renderFormatListWrapperClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) {
    }

    @Override
    public void renderFormatHeaderOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) {
    }

    @Override
    public void renderFormatHeaderClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) {
    }

    @Override
    public void renderFormatHeaderRowOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) {
        this.currentRow = sheet.createRow(rowIndex++);
        this.colIndex = 0;
    }

    @Override
    public void renderFormatHeaderRowClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) {
    }

    @Override
    public void renderFormatHeaderRowCellOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm,
            ModelFormField modelFormField, int positionSpan) {
        this.currentCell = currentRow.createCell(colIndex);
    }

    @Override
    public void renderFormatHeaderRowCellClose(Appendable writer, Map<String, Object> context, ModelForm modelForm,
            ModelFormField modelFormField) {
        this.currentCell = null;
        this.colIndex++;
    }

    @Override
    public void renderFormatHeaderRowFormCellOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) {
    }

    @Override
    public void renderFormatHeaderRowFormCellClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) {
    }

    @Override
    public void renderFormatHeaderRowFormCellTitleSeparator(Appendable writer, Map<String, Object> context, ModelForm modelForm,
            ModelFormField modelFormField, boolean isLast) {
    }

    @Override
    public void renderFieldTitle(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) {
        Cell cell = obtainCell();
        cell.setCellValue(modelFormField.getTitle(context));
    }

    @Override
    public void renderSingleFormFieldTitle(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) {
        renderFieldTitle(writer, context, modelFormField);
    }

    @Override
    public void renderFormatItemRowOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) {
        this.currentRow = sheet.createRow(rowIndex++);
        this.colIndex = 0;
    }

    @Override
    public void renderFormatItemRowClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) {
    }

    @Override
    public void renderFormatItemRowCellOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm,
            ModelFormField modelFormField, int positionSpan) {
        this.currentCell = currentRow.createCell(colIndex);
    }

    @Override
    public void renderFormatItemRowCellClose(Appendable writer, Map<String, Object> context, ModelForm modelForm,
            ModelFormField modelFormField) {
        this.currentCell = null;
        this.colIndex++;
    }

    @Override
    public void renderFormatItemRowFormCellOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) {
    }

    @Override
    public void renderFormatItemRowFormCellClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) {
    }

    @Override
    public void renderFormatSingleWrapperOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) {
        this.sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName(modelForm.getName()));
        this.rowIndex = 0;
        this.colIndex = 0;
    }

    @Override
    public void renderFormatSingleWrapperClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) {
    }

    @Override
    public void renderFormatFieldRowOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) {
        this.currentRow = sheet.createRow(rowIndex++);
        this.colIndex = 0;
    }

    @Override
    public void renderFormatFieldRowClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) {
    }

    @Override
    public void renderFormatFieldRowTitleCellOpen(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) {
        this.currentCell = currentRow.createCell(colIndex);
    }

    @Override
    public void renderFormatFieldRowTitleCellClose(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) {
        this.currentCell = null;
        this.colIndex++;
    }

    @Override
    public void renderFormatFieldRowSpacerCell(Appendable writer, Map<String, Object> context, ModelFormField modelFormField) {
    }

    @Override
    public void renderFormatFieldRowWidgetCellOpen(Appendable writer, Map<String, Object> context, ModelFormField modelFormField,
            int positions, int positionSpan, Integer nextPositionInRow) {
        this.currentCell = currentRow.createCell(colIndex);
    }

    @Override
    public void renderFormatFieldRowWidgetCellClose(Appendable writer, Map<String, Object> context, ModelFormField modelFormField,
            int positions, int positionSpan, Integer nextPositionInRow) {
        this.currentCell = null;
        this.colIndex++;
    }

    @Override
    public void renderFormatEmptySpace(Appendable writer, Map<String, Object> context, ModelForm modelForm) {
    }

    @Override
    public void renderDisplayField(Appendable writer, Map<String, Object> context, ModelFormField.DisplayField displayField) {
        String type = displayField.getType();
        String rawValue = displayField.getModelFormField().getEntry(context);
        String displayValue = displayField.getDescription(context);
        if ("currency".equals(type) || "accounting-number".equals(type)) {
            writeNumericOrFallback(rawValue, displayValue);
        } else if ("date".equals(type)) {
            writeDateOrFallback(rawValue, displayValue, true);
        } else if ("date-time".equals(type)) {
            writeDateOrFallback(rawValue, displayValue, false);
        } else {
            writeString(displayValue);
        }
    }

    @Override
    public void renderTextField(Appendable writer, Map<String, Object> context, ModelFormField.TextField textField) {
        String type = textField.getType();
        String rawValue = textField.getModelFormField().getEntry(context);
        if ("currency".equals(type) || "accounting-number".equals(type)) {
            writeNumericOrFallback(rawValue, rawValue);
        } else {
            writeString(rawValue);
        }
    }

    @Override
    public void renderDateTimeField(Appendable writer, Map<String, Object> context, ModelFormField.DateTimeField dateTimeField) {
        String rawValue = dateTimeField.getModelFormField().getEntry(context);
        writeDateOrFallback(rawValue, rawValue, dateTimeField.isDateType());
    }

    private void writeString(String value) {
        Cell cell = obtainCell();
        cell.setCellValue(value == null ? "" : value);
    }

    private void writeNumericOrFallback(String rawValue, String displayValue) {
        BigDecimal numeric = parseNumeric(rawValue);
        if (numeric != null) {
            Cell cell = obtainCell();
            cell.setCellValue(numeric.doubleValue());
            cell.setCellStyle(numericStyle);
        } else {
            writeString(displayValue);
        }
    }

    private void writeDateOrFallback(String rawValue, String displayValue, boolean isDateType) {
        java.util.Date parsed = parseDateTime(rawValue, isDateType);
        if (parsed != null) {
            Cell cell = obtainCell();
            cell.setCellValue(parsed);
            cell.setCellStyle(isDateType ? dateStyle : dateTimeStyle);
        } else {
            writeString(displayValue);
        }
    }

    /**
     * Returns the cell that a title/value write should land in. When an explicit
     * {@code renderFormat*CellOpen} call already created {@link #currentCell}, that same cell is
     * returned unchanged. Otherwise (a field rendered without a separate column, the default when
     * {@code separate-columns}/{@code separate-column} is not set) a cell is created at the current
     * column and the column index is advanced, so subsequent bare fields land in the next column.
     */
    private Cell obtainCell() {
        if (currentCell == null) {
            return currentRow.createCell(colIndex++);
        }
        return currentCell;
    }

    private static BigDecimal parseNumeric(String rawValue) {
        if (rawValue == null || rawValue.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(rawValue.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static java.util.Date parseDateTime(String rawValue, boolean isDateType) {
        if (rawValue == null || rawValue.isEmpty()) {
            return null;
        }
        try {
            return isDateType ? java.sql.Date.valueOf(rawValue.trim()) : Timestamp.valueOf(rawValue.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public void renderDropDownField(Appendable writer, Map<String, Object> context, ModelFormField.DropDownField dropDownField) {
        writeString(dropDownField.getModelFormField().getEntry(context));
    }

    @Override
    public void renderCheckField(Appendable writer, Map<String, Object> context, ModelFormField.CheckField checkField) {
        writeString(checkField.getModelFormField().getEntry(context));
    }

    @Override
    public void renderRadioField(Appendable writer, Map<String, Object> context, ModelFormField.RadioField radioField) {
        writeString(radioField.getModelFormField().getEntry(context));
    }

    @Override
    public void renderHiddenField(Appendable writer, Map<String, Object> context, ModelFormField modelFormField, String value) {
    }

    @Override
    public void renderHiddenField(Appendable writer, Map<String, Object> context, ModelFormField.HiddenField hiddenField) {
    }

    @Override
    public void renderIgnoredField(Appendable writer, Map<String, Object> context, ModelFormField.IgnoredField ignoredField) {
    }

    @Override
    public void renderSubmitField(Appendable writer, Map<String, Object> context, ModelFormField.SubmitField submitField) {
    }

    @Override
    public void renderResetField(Appendable writer, Map<String, Object> context, ModelFormField.ResetField resetField) {
    }

    @Override
    public void renderFormOpen(Appendable writer, Map<String, Object> context, ModelForm modelForm) {
    }

    @Override
    public void renderFormClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) {
    }

    @Override
    public void renderMultiFormClose(Appendable writer, Map<String, Object> context, ModelForm modelForm) {
    }

    // Everything below has no place in a tabular data export (styling, hyperlinks, lookups,
    // find-fields, banners, tooltips, field groups, images) - matches the CSV/XLS FTL macro
    // libraries' own no-op behavior for the same methods.

    @Override
    public void renderHyperlinkField(Appendable writer, Map<String, Object> context, ModelFormField.HyperlinkField hyperlinkField) {
    }

    @Override
    public void renderMenuField(Appendable writer, Map<String, Object> context, ModelFormField.MenuField menuField) {
    }

    @Override
    public void renderTextareaField(Appendable writer, Map<String, Object> context, ModelFormField.TextareaField textareaField) {
    }

    @Override
    public void renderTextFindField(Appendable writer, Map<String, Object> context, ModelFormField.TextFindField textField) {
    }

    @Override
    public void renderDateFindField(Appendable writer, Map<String, Object> context, ModelFormField.DateFindField textField) {
    }

    @Override
    public void renderRangeFindField(Appendable writer, Map<String, Object> context, ModelFormField.RangeFindField textField) {
    }

    @Override
    public void renderDateRangePickerField(Appendable writer, Map<String, Object> context,
            ModelFormField.DateRangePickerField textField) {
    }

    @Override
    public void renderLookupField(Appendable writer, Map<String, Object> context, ModelFormField.LookupField textField) {
    }

    @Override
    public void renderFileField(Appendable writer, Map<String, Object> context, ModelFormField.FileField textField) {
    }

    @Override
    public void renderPasswordField(Appendable writer, Map<String, Object> context, ModelFormField.PasswordField textField) {
    }

    @Override
    public void renderImageField(Appendable writer, Map<String, Object> context, ModelFormField.ImageField textField) {
    }

    @Override
    public void renderBanner(Appendable writer, Map<String, Object> context, ModelForm.Banner banner) {
    }

    @Override
    public void renderContainerFindField(Appendable writer, Map<String, Object> context, ModelFormField.ContainerField containerField) {
    }

    @Override
    public void renderFieldGroupOpen(Appendable writer, Map<String, Object> context, ModelForm.FieldGroup fieldGroup) {
    }

    @Override
    public void renderFieldGroupClose(Appendable writer, Map<String, Object> context, ModelForm.FieldGroup fieldGroup) {
    }

    @Override
    public void renderEmptyFormDataMessage(Appendable writer, Map<String, Object> context, ModelForm modelForm) {
    }
}
