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
package org.apache.ofbiz.widget.renderer;

import java.io.IOException;
import java.util.Map;

import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.widget.model.ModelScreen;
import org.apache.ofbiz.widget.model.ModelScreenWidget;

/**
 * Widget Library - Screen String Renderer interface.
 */
public interface ScreenStringRenderer {
    String getRendererName();
    void renderBegin(Appendable writer, Map<String, Object> context) throws IOException;
    void renderEnd(Appendable writer, Map<String, Object> context) throws IOException;
    void renderScreenBegin(Appendable writer, Map<String, Object> context, ModelScreen modelScreen) throws IOException;
    void renderScreenEnd(Appendable writer, Map<String, Object> context, ModelScreen modelScreen) throws IOException;
    void renderSectionBegin(Appendable writer, Map<String, Object> context, ModelScreenWidget.Section section) throws IOException;
    void renderSectionEnd(Appendable writer, Map<String, Object> context, ModelScreenWidget.Section section) throws IOException;
    void renderColumnContainer(Appendable writer, Map<String, Object> context, ModelScreenWidget.ColumnContainer columnContainer) throws IOException;
    void renderContainerBegin(Appendable writer, Map<String, Object> context, ModelScreenWidget.Container container) throws IOException;
    void renderContainerEnd(Appendable writer, Map<String, Object> context, ModelScreenWidget.Container container) throws IOException;
    void renderContentBegin(Appendable writer, Map<String, Object> context, ModelScreenWidget.Content content) throws IOException;
    void renderContentBody(Appendable writer, Map<String, Object> context, ModelScreenWidget.Content content) throws IOException;
    void renderContentEnd(Appendable writer, Map<String, Object> context, ModelScreenWidget.Content content) throws IOException;
    void renderSubContentBegin(Appendable writer, Map<String, Object> context, ModelScreenWidget.SubContent content) throws IOException;
    void renderSubContentBody(Appendable writer, Map<String, Object> context, ModelScreenWidget.SubContent content) throws IOException;
    void renderSubContentEnd(Appendable writer, Map<String, Object> context, ModelScreenWidget.SubContent content) throws IOException;

    void renderHorizontalSeparator(Appendable writer, Map<String, Object> context, ModelScreenWidget.HorizontalSeparator separator)
            throws IOException;
    void renderLabel(Appendable writer, Map<String, Object> context, ModelScreenWidget.Label label) throws IOException;
    void renderLink(Appendable writer, Map<String, Object> context, ModelScreenWidget.ScreenLink link) throws IOException;
    void renderImage(Appendable writer, Map<String, Object> context, ModelScreenWidget.ScreenImage image) throws IOException;

    void renderContentFrame(Appendable writer, Map<String, Object> context, ModelScreenWidget.Content content) throws IOException;
    void renderScreenletBegin(Appendable writer, Map<String, Object> context, boolean collapsed, ModelScreenWidget.Screenlet screenlet)
            throws IOException;
    void renderScreenletSubWidget(Appendable writer, Map<String, Object> context, ModelScreenWidget subWidget, ModelScreenWidget.Screenlet screenlet)
            throws GeneralException, IOException;
    void renderScreenletEnd(Appendable writer, Map<String, Object> context, ModelScreenWidget.Screenlet screenlet) throws IOException;

    void renderPortalPageBegin(Appendable writer, Map<String, Object> context, ModelScreenWidget.PortalPage portalPage)
            throws GeneralException, IOException;
    void renderPortalPageEnd(Appendable writer, Map<String, Object> context, ModelScreenWidget.PortalPage portalPage)
            throws GeneralException, IOException;
    void renderPortalPageColumnBegin(Appendable writer, Map<String, Object> context, ModelScreenWidget.PortalPage portalPage,
                                     GenericValue portalPageColumn) throws GeneralException, IOException;
    void renderPortalPageColumnEnd(Appendable writer, Map<String, Object> context, ModelScreenWidget.PortalPage portalPage,
                                   GenericValue portalPageColumn) throws GeneralException, IOException;
    void renderPortalPagePortletBegin(Appendable writer, Map<String, Object> context, ModelScreenWidget.PortalPage portalPage,
                                      GenericValue portalPortlet) throws GeneralException, IOException;
    void renderPortalPagePortletBody(Appendable writer, Map<String, Object> context, ModelScreenWidget.PortalPage portalPage,
                                     GenericValue portalPortlet) throws GeneralException, IOException;
    void renderPortalPagePortletEnd(Appendable writer, Map<String, Object> context, ModelScreenWidget.PortalPage portalPage,
                                    GenericValue portalPortlet) throws GeneralException, IOException;
}



