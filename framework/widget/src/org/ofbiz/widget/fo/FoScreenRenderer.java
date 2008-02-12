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
package org.ofbiz.widget.fo;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.widget.html.HtmlWidgetRenderer;
import org.ofbiz.widget.screen.ModelScreenWidget;
import org.ofbiz.widget.screen.ScreenStringRenderer;

/**
 * Widget Library - HTML Form Renderer implementation
 */
public class FoScreenRenderer extends HtmlWidgetRenderer implements ScreenStringRenderer {

    public static final String module = FoScreenRenderer.class.getName();

    public FoScreenRenderer() {}

    // This is a util method to get the style from a property file
    public static String getFoStyle(String styleName) {
        String value = UtilProperties.getPropertyValue("fo-styles.properties", styleName);
        if (value.equals(styleName)) {
            return "";
        }
        return value;
    }

    public void renderSectionBegin(Writer writer, Map context, ModelScreenWidget.Section section) throws IOException {
        renderBeginningBoundaryComment(writer, section.isMainSection?"Screen":"Section Widget", section);
    }
    public void renderSectionEnd(Writer writer, Map context, ModelScreenWidget.Section section) throws IOException {
        renderEndingBoundaryComment(writer, section.isMainSection?"Screen":"Section Widget", section);
    }

    public void renderContainerBegin(Writer writer, Map context, ModelScreenWidget.Container container) throws IOException {
        writer.write("<fo:block");

        String style = container.getStyle(context);
        if (UtilValidate.isNotEmpty(style)) {
            writer.append(" ");
            writer.append(FoScreenRenderer.getFoStyle(style));
        }
        writer.write(">");
        appendWhitespace(writer);
    }
    public void renderContainerEnd(Writer writer, Map context, ModelScreenWidget.Container container) throws IOException {
        writer.write("</fo:block>");
        appendWhitespace(writer);
    }

    public void renderLabel(Writer writer, Map context, ModelScreenWidget.Label label) throws IOException {
        String labelText = label.getText(context);
        if (UtilValidate.isEmpty(labelText)) {
            // nothing to render
            return;
        }
        // open tag
        String style = label.getStyle(context);
        if (UtilValidate.isNotEmpty(style)) {
            writer.write("<fo:inline ");
            writer.append(FoScreenRenderer.getFoStyle(style));
            writer.write(">");
            // the text
            writer.write(labelText);
            // close tag
            writer.write("</fo:inline>");
        } else {
            writer.write(labelText);
        }
        appendWhitespace(writer);
    }

    public void renderLink(Writer writer, Map context, ModelScreenWidget.Link link) throws IOException {
        // TODO: not implemented
    }

    public void renderImage(Writer writer, Map context, ModelScreenWidget.Image image) throws IOException {
        // TODO: not implemented
    }

    public void renderContentBegin(Writer writer, Map context, ModelScreenWidget.Content content) throws IOException {
        // TODO: not implemented
    }

    public void renderContentBody(Writer writer, Map context, ModelScreenWidget.Content content) throws IOException {
        // TODO: not implemented
    }

    public void renderContentEnd(Writer writer, Map context, ModelScreenWidget.Content content) throws IOException {
        // TODO: not implemented
    }

    public void renderContentFrame(Writer writer, Map context, ModelScreenWidget.Content content) throws IOException {
        // TODO: not implemented
    }

    public void renderSubContentBegin(Writer writer, Map context, ModelScreenWidget.SubContent content) throws IOException {
        // TODO: not implemented
    }

    public void renderSubContentBody(Writer writer, Map context, ModelScreenWidget.SubContent content) throws IOException {
        // TODO: not implemented
    }

    public void renderSubContentEnd(Writer writer, Map context, ModelScreenWidget.SubContent content) throws IOException {
        // TODO: not implemented
    }

    public void renderScreenletBegin(Writer writer, Map context, boolean collapsed, ModelScreenWidget.Screenlet screenlet) throws IOException {
        // TODO: not implemented
    }

    public void renderScreenletSubWidget(Writer writer, Map context, ModelScreenWidget subWidget, ModelScreenWidget.Screenlet screenlet) throws GeneralException {
        // TODO: not implemented
    }

    public void renderScreenletEnd(Writer writer, Map context, ModelScreenWidget.Screenlet screenlet) throws IOException {
        // TODO: not implemented
    }
}
