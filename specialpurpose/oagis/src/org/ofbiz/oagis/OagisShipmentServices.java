package org.ofbiz.oagis;

/**
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
**/
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.sql.Timestamp;

import org.w3c.dom.Document;

import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.base.util.*;
import org.ofbiz.base.util.collections.MapStack;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;

import org.ofbiz.widget.fo.FoFormRenderer;
import org.ofbiz.widget.screen.ScreenRenderer;
import org.ofbiz.widget.html.HtmlScreenRenderer;

public class OagisShipmentServices {
    
    public static final String module = OagisShipmentServices.class.getName();

    protected static final HtmlScreenRenderer htmlScreenRenderer = new HtmlScreenRenderer();
    protected static final FoFormRenderer foFormRenderer = new FoFormRenderer();
    
    public static Map showShipment(DispatchContext ctx, Map context) {
        InputStream in = (InputStream) context.get("inputStream");
        OutputStream out = (OutputStream) context.get("outputStream");
        try{
            Document doc = UtilXml.readXmlDocument(in, true, "ShowShipment");
        }catch (Exception e){
            Debug.logError(e, module);
        }
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
        writer.println("Service not Implemented");
        writer.flush();
        Map result = ServiceUtil.returnError("Service not Implemented");
        return result;
        
    }
    
    public static void writeScreenToOutputStream(OutputStream out, String bodyScreenUri, MapStack parameters){

        Writer writer = new OutputStreamWriter(out);
        ScreenRenderer screens = new ScreenRenderer(writer, parameters, new HtmlScreenRenderer());
        try {
            screens.render(bodyScreenUri);
        } catch (Exception e) {
            Debug.logError(e, "Error rendering [text/xml]: ", module);
        }

    }
    
    
    public static Map exportMsgFromScreen(DispatchContext dctx, Map serviceContext) {

        String bodyScreenUri = (String) serviceContext.remove("bodyScreenUri");
        Map bodyParameters = (Map) serviceContext.remove("bodyParameters");

        MapStack screenContext = MapStack.create();
        Writer bodyWriter = new StringWriter();
        ScreenRenderer screens = new ScreenRenderer(bodyWriter, screenContext, htmlScreenRenderer);
        if (bodyParameters != null) {
            screens.populateContextForService(dctx, bodyParameters);
            screenContext.putAll(bodyParameters);
        }
        //screenContext.putAll(serviceContext);
        //screens.getContext().put("formStringRenderer", foFormRenderer);
        try {
            screens.render(bodyScreenUri);
        } catch (Exception e) {
            String errMsg = "Error rendering [text/xml]: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        Map result = ServiceUtil.returnSuccess();
        Debug.logInfo(bodyWriter.toString(), module);
        result.put("body", bodyWriter.toString());
        return result;
    }

    public static Map processShipment(DispatchContext ctx, Map context) {
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericDelegator delegator = ctx.getDelegator();
        String orderId = (String) context.get("orderId");
        String shipmentId = (String) context.get("shipmentId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        GenericValue orderHeader = null;
        try {
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        Map csResult = null;
        Map psmMap = new HashMap();
        GenericValue orderItemShipGroup = null;
        GenericValue productStore =null;
        String orderStatusId = null;
        if (orderHeader != null) {
            orderStatusId = orderHeader.getString("statusId");
            if (orderStatusId.equals("ORDER_APPROVED")) {
                try {
                    orderItemShipGroup = EntityUtil.getFirst(delegator.findByAnd("OrderItemShipGroup", UtilMisc.toMap("orderId", orderId), UtilMisc.toList("shipGroupSeqId")));
                    String productStoreId = orderHeader.getString("productStoreId"); 
                    productStore = delegator.findByPrimaryKey("ProductStore", UtilMisc.toMap("productStoreId", productStoreId));
                    String originFacilityId = productStore.getString("inventoryFacilityId");
                    String statusId = "SHIPMENT_INPUT";
                    
                    csResult= dispatcher.runSync("createShipment", UtilMisc.toMap("primaryOrderId", orderId,"primaryShipGroupSeqId",orderItemShipGroup.get("shipGroupSeqId") ,"statusId", statusId ,"originFacilityId", originFacilityId ,"userLogin", userLogin));
                    shipmentId = (String) csResult.get("shipmentId");

                    List orderItems = new ArrayList();
                    Map orderItemCtx = new HashMap();
                    orderItems = delegator.findByAnd("OrderItem", UtilMisc.toMap("orderId", orderId));
                    Iterator oiIter = orderItems.iterator();
                    while (oiIter.hasNext()) {
                        GenericValue orderItem = (GenericValue) oiIter.next();
                        
                        orderItemCtx.put("orderId", orderItem.get("orderId"));
                        orderItemCtx.put("orderItemSeqId", orderItem.get("orderItemSeqId"));
                        orderItemCtx.put("shipmentId", shipmentId);
                        orderItemCtx.put("quantity", orderItem.get("quantity"));
                        orderItemCtx.put("userLogin", userLogin);
                         
                        dispatcher.runSync("addOrderShipmentToShipment", orderItemCtx);
                    }
                    String logicalId = UtilProperties.getPropertyValue("oagis.properties", "CNTROLAREA.SENDER.LOGICALID");
                    String authId = UtilProperties.getPropertyValue("oagis.properties", "CNTROLAREA.SENDER.AUTHID");
                    String referenceId = delegator.getNextSeqId("OagisMessageInfo");
                    Timestamp timestamp = null;
                    timestamp = UtilDateTime.nowTimestamp();

                    psmMap.put("logicalId", logicalId);
                    psmMap.put("authId", authId);
                    psmMap.put("referenceId", referenceId);
                    psmMap.put("sentDate", timestamp);
                    psmMap.put("shipmentId", shipmentId);
                    psmMap.put("userLogin", userLogin);
                    
                    // send the process shipment message
                    dispatcher.runSync("sendProcessShipmentMsg", psmMap);                    
                } catch (Exception e) {
                    Debug.logError("Error in processing" + e.getMessage(), module);
                }
            }
        }
        psmMap.put("component", "INVENTORY");
        psmMap.put("task", "SHIPREQUES"); // Actual value of task is "SHIPREQUES" which is more than 10 char 
        psmMap.put("outgoingMessage", "Y");
        psmMap.put("confirmation", "1");
        psmMap.put("bsrVerb", "PROCESS");
        psmMap.put("bsrNoun", "SHIPMENT");
        psmMap.put("bsrRevision", "001");
        psmMap.put("processingStatusId", orderStatusId);        
        psmMap.put("orderId", orderId);        
        try {
            dispatcher.runSync("createOagisMessageInfo", psmMap);
        } catch (Exception e) {
            return ServiceUtil.returnError("error in creating message info" + e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }
}
