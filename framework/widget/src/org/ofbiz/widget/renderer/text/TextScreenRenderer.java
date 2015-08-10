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
package org.ofbiz.widget.renderer.text;

import java.io.IOException;
import java.util.Map;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.widget.model.ModelScreenWidget;
import org.ofbiz.widget.model.ModelScreenWidget.ColumnContainer;
import org.ofbiz.widget.renderer.ScreenStringRenderer;

/**
 * Widget Library - Text Screen Renderer implementation
 * @deprecated Use MacroScreenRenderer.
 */
public class TextScreenRenderer implements ScreenStringRenderer {

    public static final String module = TextScreenRenderer.class.getName();

    public TextScreenRenderer() {}

    public String getRendererName() {
        return "text";
    }

    public void renderScreenBegin(Appendable writer, Map<String, Object> context) throws IOException {
    }

    public void renderScreenEnd(Appendable writer, Map<String, Object> context) throws IOException {
    }

    public void renderSectionBegin(Appendable writer, Map<String, Object> context, ModelScreenWidget.Section section) throws IOException {
        // do nothing, this is just a place holder container for HTML
    }
    public void renderSectionEnd(Appendable writer, Map<String, Object> context, ModelScreenWidget.Section section) throws IOException {
        // do nothing, this is just a place holder container for HTML
    }

    public void renderContainerBegin(Appendable writer, Map<String, Object> context, ModelScreenWidget.Container container) throws IOException {
    }
    public void renderContainerEnd(Appendable writer, Map<String, Object> context, ModelScreenWidget.Container container) throws IOException {
        appendWhitespace(writer);
    }

    public void renderLabel(Appendable writer, Map<String, Object> context, ModelScreenWidget.Label label) throws IOException {
        String labelText = label.getText(context);
        if (UtilValidate.isEmpty(labelText)) {
            // nothing to render
            return;
        }
        writer.append(labelText);
        appendWhitespace(writer);
    }

    public void renderHorizontalSeparator(Appendable writer, Map<String, Object> context, ModelScreenWidget.HorizontalSeparator separator) throws IOException {
        // TODO: not implemented
    }

    public void renderLink(Appendable writer, Map<String, Object> context, ModelScreenWidget.ScreenLink link) throws IOException {
        // TODO: not implemented
    }

    public void renderImage(Appendable writer, Map<String, Object> context, ModelScreenWidget.ScreenImage image) throws IOException {
        // TODO: not implemented
    }

    public void renderContentBegin(Appendable writer, Map<String, Object> context, ModelScreenWidget.Content content) throws IOException {
        // TODO: not implemented
    }

    public void renderContentBody(Appendable writer, Map<String, Object> context, ModelScreenWidget.Content content) throws IOException {
        // TODO: not implemented
    }

    public void renderContentEnd(Appendable writer, Map<String, Object> context, ModelScreenWidget.Content content) throws IOException {
        // TODO: not implemented
    }

    public void renderContentFrame(Appendable writer, Map<String, Object> context, ModelScreenWidget.Content content) throws IOException {
        // TODO: not implemented
    }

    public void renderSubContentBegin(Appendable writer, Map<String, Object> context, ModelScreenWidget.SubContent content) throws IOException {
        // TODO: not implemented
    }

    public void renderSubContentBody(Appendable writer, Map<String, Object> context, ModelScreenWidget.SubContent content) throws IOException {
        // TODO: not implemented
    }

    public void renderSubContentEnd(Appendable writer, Map<String, Object> context, ModelScreenWidget.SubContent content) throws IOException {
        // TODO: not implemented
    }

    public void appendWhitespace(Appendable writer) throws IOException {
        // appending line ends for now, but this could be replaced with a simple space or something
        writer.append("\r\n");
    }
    public void renderScreenletBegin(Appendable writer, Map<String, Object> context, boolean collapsed, ModelScreenWidget.Screenlet screenlet) throws IOException {
        // TODO: not implemented
    }
    public void renderScreenletSubWidget(Appendable writer, Map<String, Object> context, ModelScreenWidget subWidget, ModelScreenWidget.Screenlet screenlet) throws GeneralException {
        // TODO: not implemented
    }
    public void renderScreenletEnd(Appendable writer, Map<String, Object> context, ModelScreenWidget.Screenlet screenlet) throws IOException {
        // TODO: not implemented
    }
    public void renderPortalPageBegin(Appendable writer, Map<String, Object> context, ModelScreenWidget.PortalPage portalPage) throws GeneralException, IOException {
        // TODO: not implemented
    }
    public void renderPortalPageEnd(Appendable writer, Map<String, Object> context, ModelScreenWidget.PortalPage portalPage) throws GeneralException, IOException {
        // TODO: not implemented
    }
    public void renderPortalPageColumnBegin(Appendable writer, Map<String, Object> context, ModelScreenWidget.PortalPage portalPage, GenericValue portalPageColumn) throws GeneralException, IOException {
        // TODO: not implemented
    }
    public void renderPortalPageColumnEnd(Appendable writer, Map<String, Object> context, ModelScreenWidget.PortalPage portalPage, GenericValue portalPageColumn) throws GeneralException, IOException {
        // TODO: not implemented
    }
    public void renderPortalPagePortletBegin(Appendable writer, Map<String, Object> context, ModelScreenWidget.PortalPage portalPage, GenericValue portalPortlet) throws GeneralException, IOException {
        // TODO: not implemented
    }
    public void renderPortalPagePortletEnd(Appendable writer, Map<String, Object> context, ModelScreenWidget.PortalPage portalPage, GenericValue portalPortlet) throws GeneralException, IOException {
        // TODO: not implemented
    }
    public void renderPortalPagePortletBody(Appendable writer, Map<String, Object> context, ModelScreenWidget.PortalPage portalPage, GenericValue portalPortlet) throws GeneralException, IOException {
        // TODO: not implemented
    }

    @Override
    public void renderColumnContainer(Appendable writer, Map<String, Object> context, ColumnContainer columnContainer) throws IOException {
        // TODO: not implemented
    }
}
