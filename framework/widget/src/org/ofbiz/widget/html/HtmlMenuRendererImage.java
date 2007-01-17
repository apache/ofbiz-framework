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
package org.ofbiz.widget.html;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.widget.WidgetContentWorker;
import org.ofbiz.widget.menu.ModelMenuItem;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;

/**
 * Widget Library - HTML Menu Renderer implementation
 */

public class HtmlMenuRendererImage extends HtmlMenuRenderer {

    protected HtmlMenuRendererImage() {}

    public HtmlMenuRendererImage(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }


    public String buildDivStr(ModelMenuItem menuItem, Map context) {

        String imgStr = "<img src=\"";
        String contentId = menuItem.getAssociatedContentId(context);
        GenericDelegator delegator = (GenericDelegator)request.getAttribute("delegator");
        GenericValue webSitePublishPoint = null;
                //Debug.logInfo("in HtmlMenuRendererImage, contentId:" + contentId,"");
        try {
            if (WidgetContentWorker.contentWorker != null) {
                webSitePublishPoint = WidgetContentWorker.contentWorker.getWebSitePublishPointExt(delegator, contentId, false);
            } else {
                Debug.logError("Not rendering image because can't get WebSitePublishPoint, not ContentWorker found.", module);
            }
        } catch(GenericEntityException e) {
                //Debug.logInfo("in HtmlMenuRendererImage, GEException:" + e.getMessage(),"");
            throw new RuntimeException(e.getMessage());
        }
        String medallionLogoStr = webSitePublishPoint.getString("medallionLogo");
        StringBuffer buf = new StringBuffer();
        appendContentUrl(buf, medallionLogoStr);
        imgStr += buf.toString();
                //Debug.logInfo("in HtmlMenuRendererImage, imgStr:" + imgStr,"");
        String cellWidth = menuItem.getCellWidth();
        imgStr += "\"";
        String widthStr = "";
        if (UtilValidate.isNotEmpty(cellWidth)) 
            widthStr = " width=\"" + cellWidth + "\" ";
        
        imgStr += widthStr;
        imgStr += " border=\"0\" />";
        return imgStr;
    }

}
