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
package org.apache.ofbiz.order.order;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.MimeConstants;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.collections.MapStack;
import org.apache.ofbiz.content.data.DataResourceWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.webapp.view.ApacheFopWorker;
import org.apache.ofbiz.widget.renderer.ScreenRenderer;
import org.apache.ofbiz.widget.renderer.ScreenStringRenderer;
import org.apache.ofbiz.widget.renderer.VisualTheme;
import org.apache.ofbiz.widget.renderer.fo.FoFormRenderer;
import org.apache.ofbiz.widget.renderer.macro.MacroScreenRenderer;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.xml.sax.SAXException;

import freemarker.template.TemplateException;

/**
 * Order Events
 */
public class OrderEvents {

    private static final String MODULE = OrderEvents.class.getName();
    private static final String ORDER_PDF_SCREEN = "component://order/widget/ordermgr/OrderPrintScreens.xml#OrderPDF";

    /**
     * Streams one combined PDF for all selected orders to the browser, used by the
     * Find Orders "View PDF" mass action.
     */
    public static String viewOrdersPdf(HttpServletRequest request, HttpServletResponse response) {
        String[] orderIds = request.getParameterValues("orderIdList");
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Security security = (Security) request.getAttribute("security");
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        Locale locale = UtilHttp.getLocale(request);
        VisualTheme visualTheme = UtilHttp.getVisualTheme(request);

        try {
            ScreenStringRenderer foScreenRenderer = new MacroScreenRenderer(visualTheme.getModelTheme().getType("screenfop"),
                    visualTheme.getModelTheme().getScreenRendererLocation("screenfop"));
            PDFMergerUtility merger = new PDFMergerUtility();
            int orderCount = 0;
            for (String orderId : orderIds != null ? orderIds : new String[0]) {
                if (UtilValidate.isEmpty(orderId)) {
                    continue;
                }
                Writer writer = new StringWriter();
                ScreenRenderer screens = new ScreenRenderer(writer, MapStack.create(), foScreenRenderer);
                screens.populateBasicContext(UtilMisc.toMap("orderId", (Object) orderId), delegator, dispatcher, security, locale, userLogin);
                screens.getContext().put("formStringRenderer", new FoFormRenderer());
                screens.render(ORDER_PDF_SCREEN);

                ByteArrayOutputStream orderPdf = new ByteArrayOutputStream();
                Fop fop = ApacheFopWorker.createFopInstance(orderPdf, MimeConstants.MIME_PDF);
                ApacheFopWorker.transform(new StreamSource(new StringReader(writer.toString())), null, fop);
                merger.addSource(new RandomAccessReadBuffer(orderPdf.toByteArray()));
                orderCount++;
            }
            if (orderCount == 0) {
                request.setAttribute("_ERROR_MESSAGE_", "No orders selected.");
                return "error";
            }
            ByteArrayOutputStream mergedPdf = new ByteArrayOutputStream();
            merger.setDestinationStream(mergedPdf);
            merger.mergeDocuments(org.apache.pdfbox.io.IOUtils.createMemoryOnlyStreamCache());

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "inline; filename=\"orders.pdf\"");
            response.setContentLength(mergedPdf.size());
            mergedPdf.writeTo(response.getOutputStream());
        } catch (GeneralException | IOException | SAXException | ParserConfigurationException | TemplateException e) {
            String errMsg = "Error rendering orders PDF: " + e.toString();
            Debug.logError(e, errMsg, MODULE);
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }
        return "success";
    }

    public static String downloadDigitalProduct(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        ServletContext application = session.getServletContext();
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        String dataResourceId = request.getParameter("dataResourceId");

        try {
            // has the userLogin.partyId ordered a product with DIGITAL_DOWNLOAD content associated for the given dataResourceId?
            GenericValue orderRoleAndProductContentInfo = EntityQuery.use(delegator).from("OrderRoleAndProductContentInfo")
                    .where("partyId", userLogin.get("partyId"),
                            "dataResourceId", dataResourceId,
                            "productContentTypeId", "DIGITAL_DOWNLOAD",
                            "statusId", "ITEM_COMPLETED")
                    .queryFirst();

            if (orderRoleAndProductContentInfo == null) {
                request.setAttribute("_ERROR_MESSAGE_", "No record of purchase for digital download found (dataResourceId=["
                        + dataResourceId + "]).");
                return "error";
            }


            // TODO: check validity based on ProductContent fields: useCountLimit, useTime/useTimeUomId

            if (orderRoleAndProductContentInfo.getString("mimeTypeId") != null) {
                response.setContentType(orderRoleAndProductContentInfo.getString("mimeTypeId"));
            }
            OutputStream os = response.getOutputStream();
            GenericValue dataResource = EntityQuery.use(delegator).from("DataResource").where("dataResourceId", dataResourceId).cache().queryOne();
            Map<String, Object> resourceData = DataResourceWorker.getDataResourceStream(dataResource, "", application.getInitParameter("webSiteId"),
                    UtilHttp.getLocale(request), application.getRealPath("/"), false);
            os.write(IOUtils.toByteArray((InputStream) resourceData.get("stream")));
            os.flush();
        } catch (GeneralException | IOException e) {
            String errMsg = "Error downloading digital product content: " + e.toString();
            Debug.logError(e, errMsg, MODULE);
            request.setAttribute("_ERROR_MESSAGE_", errMsg);
            return "error";
        }

        return "success";
    }

    public static String cancelSelectedOrderItems(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        HttpSession session = request.getSession();
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        Locale locale = UtilHttp.getLocale(request);

        Map<String, Object> resultMap;
        String orderId = request.getParameter("orderId");
        String[] selectedItems = request.getParameterValues("selectedItem");

        if (selectedItems != null) {
            for (String selectedItem : selectedItems) {
                String[] orderItemSeqIdAndOrderItemShipGrpId = selectedItem.split(":");
                String orderItemSeqId = orderItemSeqIdAndOrderItemShipGrpId[0];
                String shipGroupSeqId = orderItemSeqIdAndOrderItemShipGrpId[1];
                BigDecimal cancelQuantity = new BigDecimal(request.getParameter("iqm_" + orderItemSeqId + ":" + shipGroupSeqId));
                Map<String, Object> contextMap = new HashMap<>();
                contextMap.put("orderId", orderId);
                contextMap.put("orderItemSeqId", orderItemSeqId);
                contextMap.put("shipGroupSeqId", shipGroupSeqId);
                contextMap.put("cancelQuantity", cancelQuantity);
                contextMap.put("userLogin", userLogin);
                contextMap.put("locale", locale);
                try {
                    resultMap = dispatcher.runSync("cancelOrderItem", contextMap);
                    if (ServiceUtil.isError(resultMap)) {
                        String errorMessage = ServiceUtil.getErrorMessage(resultMap);
                        request.setAttribute("_ERROR_MESSAGE_", errorMessage);
                        Debug.logError(errorMessage, MODULE);
                        return "error";
                    }
                } catch (GenericServiceException e) {
                    Debug.logError(e, MODULE);
                    request.setAttribute("_ERROR_MESSAGE_", e.getMessage());
                    return "error";
                }
            }
            return "success";
        }
        request.setAttribute("_ERROR_MESSAGE_", "No order item selected. Please select an order item to cancel");
        return "error";
    }
}
