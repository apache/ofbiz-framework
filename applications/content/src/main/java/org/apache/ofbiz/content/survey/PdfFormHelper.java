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
package org.apache.ofbiz.content.survey;

import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDButton;
import org.apache.pdfbox.pdmodel.interactive.form.PDChoice;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDTerminalField;

/**
 * Reads and fills the AcroForm fields of a PDF document for the survey services.
 */
final class PdfFormHelper {

    private static final String MODULE = PdfFormHelper.class.getName();

    private final PDDocument pdDocument;
    /** For each widget annotation of the document, its page number (from 1) and its index in the page's annotations. */
    private final Map<COSDictionary, int[]> widgetPlaces = new IdentityHashMap<>();

    /**
     * @param pdDocument the PDF document, which must stay open while this helper is used
     * @throws IOException if the annotations of a page can't be read
     */
    PdfFormHelper(PDDocument pdDocument) throws IOException {
        this.pdDocument = pdDocument;
        int pageNumber = 0;
        for (PDPage page : pdDocument.getPages()) {
            pageNumber++;
            List<PDAnnotation> annotations = page.getAnnotations();
            for (int index = 0; index < annotations.size(); index++) {
                if (annotations.get(index) instanceof PDAnnotationWidget) {
                    widgetPlaces.putIfAbsent(annotations.get(index).getCOSObject(), new int[] {pageNumber, index});
                }
            }
        }
    }

    /**
     * @return the fields of the document's form that are shown on a page, in the order of the form's field tree
     */
    List<PDTerminalField> getFormFields() {
        List<PDTerminalField> formFields = new ArrayList<>();
        PDAcroForm acroForm = pdDocument.getDocumentCatalog().getAcroForm(null);
        if (acroForm != null) {
            for (PDField field : acroForm.getFieldTree()) {
                if (field instanceof PDTerminalField && getPlacedWidget((PDTerminalField) field) != null) {
                    formFields.add((PDTerminalField) field);
                }
            }
        }
        return formFields;
    }

    /**
     * @return the first widget of the field that is shown on a page, null if there is none
     */
    PDAnnotationWidget getPlacedWidget(PDTerminalField field) {
        for (PDAnnotationWidget widget : field.getWidgets()) {
            if (widgetPlaces.containsKey(widget.getCOSObject())) {
                return widget;
            }
        }
        return null;
    }

    /**
     * @return the page number (from 1) of a widget shown on a page, and its index in the page's annotations
     */
    int[] getPlace(PDAnnotationWidget widget) {
        return widgetPlaces.get(widget.getCOSObject());
    }

    /**
     * @return the field value as a string. A choice field gives its selected option, an empty string when nothing is
     *         selected, and its selected options in brackets when several are.
     */
    String getValue(PDTerminalField field) {
        if (field instanceof PDChoice) {
            List<String> values = ((PDChoice) field).getValue();
            if (values.size() <= 1) {
                return values.isEmpty() ? "" : values.get(0);
            }
        }
        return field.getValueAsString();
    }

    /**
     * Sets the field value. A value that the field does not accept, because it is not one of its options, is still
     * stored in a choice field, and turns a check box or radio button off.
     * @throws IOException if the field appearance can't be updated
     */
    void setValue(PDTerminalField field, String value) throws IOException {
        try {
            field.setValue(value);
        } catch (IllegalArgumentException | UnsupportedOperationException e) {
            Debug.logWarning("Value [" + value + "] is not valid for the PDF form field [" + field.getFullyQualifiedName() + "]: "
                    + e.getMessage(), MODULE);
            if (field instanceof PDChoice) {
                field.getCOSObject().setString(COSName.V, value);
            } else if (field instanceof PDButton && !((PDButton) field).isPushButton()) {
                field.setValue(COSName.Off.getName());
            }
        }
    }

    /**
     * Turns the form fields into page content, keeping only the printable ones, as the survey services always did.
     * @throws IOException if the fields can't be flattened
     */
    void flatten() throws IOException {
        PDAcroForm acroForm = pdDocument.getDocumentCatalog().getAcroForm(null);
        if (acroForm == null) {
            return;
        }
        for (PDField field : acroForm.getFieldTree()) {
            for (PDAnnotationWidget widget : field.getWidgets()) {
                if (!widget.isPrinted()) {
                    widget.setHidden(true);
                }
            }
        }
        acroForm.flatten();
    }
}
