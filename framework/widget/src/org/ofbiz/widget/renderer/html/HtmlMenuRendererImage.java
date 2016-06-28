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
package org.ofbiz.widget.renderer.html;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.widget.content.WidgetContentWorker;
import org.ofbiz.widget.model.ModelMenuItem;

/**
 * Widget Library - HTML Menu Renderer implementation
 */

public class HtmlMenuRendererImage extends HtmlMenuRenderer {

    protected HtmlMenuRendererImage() {}

    public HtmlMenuRendererImage(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }


    public String buildDivStr(ModelMenuItem menuItem, Map<String, Object> context) throws IOException {

        StringBuilder imgStr = new StringBuilder("<img src=\"");
        String contentId = menuItem.getAssociatedContentId(context);
        Delegator delegator = (Delegator)request.getAttribute("delegator");
        GenericValue webSitePublishPoint = null;
                //Debug.logInfo("in HtmlMenuRendererImage, contentId:" + contentId,"");
        try {
            if (WidgetContentWorker.getContentWorker() != null) {
                webSitePublishPoint = WidgetContentWorker.getContentWorker().getWebSitePublishPointExt(delegator, contentId, false);
            } else {
                Debug.logError("Not rendering image because can't get WebSitePublishPoint, not ContentWorker found.", module);
            }
        } catch (GenericEntityException e) {
                //Debug.logInfo("in HtmlMenuRendererImage, GEException:" + e.getMessage(),"");
            throw new RuntimeException(e.getMessage());
        }
        String medallionLogoStr = webSitePublishPoint.getString("medallionLogo");
        StringWriter buf = new StringWriter();
        appendContentUrl(buf, medallionLogoStr);
        imgStr.append(buf.toString());
                //Debug.logInfo("in HtmlMenuRendererImage, imgStr:" + imgStr,"");
        String cellWidth = menuItem.getCellWidth();
        imgStr.append("\"");
        if (UtilValidate.isNotEmpty(cellWidth))
            imgStr.append(" width=\"").append(cellWidth).append("\" ");

        imgStr.append(" border=\"0\" />");
        return imgStr.toString();
    }

}
