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

import java.io.IOException;
import java.util.Map;

import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.widget.model.ModelScreen;
import org.apache.ofbiz.widget.model.ModelScreenWidget;
import org.apache.ofbiz.widget.renderer.ScreenStringRenderer;

/**
 * No-op {@code ScreenStringRenderer}: lets {@code ScreenRenderer} walk a screen's
 * container/section/label tree without error while an xlsx export is in progress.
 * None of this screen-level chrome is part of the tabular data export - only the
 * form/grid content reached via {@code XlsxFormRenderer} matters.
 */
public class XlsxScreenStringRenderer implements ScreenStringRenderer {

    @Override
    public String getRendererName() {
        return "XlsxScreenStringRenderer";
    }

    @Override
    public void renderBegin(Appendable writer, Map<String, Object> context) throws IOException {
    }

    @Override
    public void renderEnd(Appendable writer, Map<String, Object> context) throws IOException {
    }

    @Override
    public void renderScreenBegin(Appendable writer, Map<String, Object> context, ModelScreen modelScreen) throws IOException {
    }

    @Override
    public void renderScreenEnd(Appendable writer, Map<String, Object> context, ModelScreen modelScreen) throws IOException {
    }

    @Override
    public void renderSectionBegin(Appendable writer, Map<String, Object> context, ModelScreenWidget.Section section) throws IOException {
    }

    @Override
    public void renderSectionEnd(Appendable writer, Map<String, Object> context, ModelScreenWidget.Section section) throws IOException {
    }

    @Override
    public void renderColumnContainer(Appendable writer, Map<String, Object> context, ModelScreenWidget.ColumnContainer columnContainer)
            throws IOException {
    }

    @Override
    public void renderContainerBegin(Appendable writer, Map<String, Object> context, ModelScreenWidget.Container container) throws IOException {
    }

    @Override
    public void renderContainerEnd(Appendable writer, Map<String, Object> context, ModelScreenWidget.Container container) throws IOException {
    }

    @Override
    public void renderContentBegin(Appendable writer, Map<String, Object> context, ModelScreenWidget.Content content) throws IOException {
    }

    @Override
    public void renderContentBody(Appendable writer, Map<String, Object> context, ModelScreenWidget.Content content) throws IOException {
    }

    @Override
    public void renderContentEnd(Appendable writer, Map<String, Object> context, ModelScreenWidget.Content content) throws IOException {
    }

    @Override
    public void renderSubContentBegin(Appendable writer, Map<String, Object> context, ModelScreenWidget.SubContent content) throws IOException {
    }

    @Override
    public void renderSubContentBody(Appendable writer, Map<String, Object> context, ModelScreenWidget.SubContent content) throws IOException {
    }

    @Override
    public void renderSubContentEnd(Appendable writer, Map<String, Object> context, ModelScreenWidget.SubContent content) throws IOException {
    }

    @Override
    public void renderHorizontalSeparator(Appendable writer, Map<String, Object> context, ModelScreenWidget.HorizontalSeparator separator)
            throws IOException {
    }

    @Override
    public void renderLabel(Appendable writer, Map<String, Object> context, ModelScreenWidget.Label label) throws IOException {
    }

    @Override
    public void renderLink(Appendable writer, Map<String, Object> context, ModelScreenWidget.ScreenLink link) throws IOException {
    }

    @Override
    public void renderImage(Appendable writer, Map<String, Object> context, ModelScreenWidget.ScreenImage image) throws IOException {
    }

    @Override
    public void renderContentFrame(Appendable writer, Map<String, Object> context, ModelScreenWidget.Content content) throws IOException {
    }

    @Override
    public void renderScreenletBegin(Appendable writer, Map<String, Object> context, boolean collapsed, ModelScreenWidget.Screenlet screenlet)
            throws IOException {
    }

    @Override
    public void renderScreenletSubWidget(Appendable writer, Map<String, Object> context, ModelScreenWidget subWidget,
            ModelScreenWidget.Screenlet screenlet) throws GeneralException, IOException {
    }

    @Override
    public void renderScreenletEnd(Appendable writer, Map<String, Object> context, ModelScreenWidget.Screenlet screenlet) throws IOException {
    }

    @Override
    public void renderPortalPageBegin(Appendable writer, Map<String, Object> context, ModelScreenWidget.PortalPage portalPage)
            throws GeneralException, IOException {
    }

    @Override
    public void renderPortalPageEnd(Appendable writer, Map<String, Object> context, ModelScreenWidget.PortalPage portalPage)
            throws GeneralException, IOException {
    }

    @Override
    public void renderPortalPageColumnBegin(Appendable writer, Map<String, Object> context, ModelScreenWidget.PortalPage portalPage,
                                     GenericValue portalPageColumn) throws GeneralException, IOException {
    }

    @Override
    public void renderPortalPageColumnEnd(Appendable writer, Map<String, Object> context, ModelScreenWidget.PortalPage portalPage,
                                   GenericValue portalPageColumn) throws GeneralException, IOException {
    }

    @Override
    public void renderPortalPagePortletBegin(Appendable writer, Map<String, Object> context, ModelScreenWidget.PortalPage portalPage,
                                      GenericValue portalPortlet) throws GeneralException, IOException {
    }

    @Override
    public void renderPortalPagePortletBody(Appendable writer, Map<String, Object> context, ModelScreenWidget.PortalPage portalPage,
                                     GenericValue portalPortlet) throws GeneralException, IOException {
    }

    @Override
    public void renderPortalPagePortletEnd(Appendable writer, Map<String, Object> context, ModelScreenWidget.PortalPage portalPage,
                                    GenericValue portalPortlet) throws GeneralException, IOException {
    }
}
