/*
 *
 * Copyright 2001-2006 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.ofbiz.manufacturing.techdata;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.manufacturing.mrp.MrpServices;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;


/**
 * Method to retrieve some manufacturing Product Information
 *
 * @author     <a href="mailto:olivier.heintz@nereide.biz">Olivier Heintz</a>
 * @author     <a href="mailto:nicolas@librenberry.net">Nicolas MALIN</a>
 * @author     <a href=mailto:tgrauss@free.fr">Thierry GRAUSS</a>
 */
public class ProductHelper {
    
    public static final String module = ProductHelper.class.getName();
    public static final String resource = "ManufacturingUiLabels";
    
    
    /**
     * Get the routing object for a product.
     * @param product : the product for which the method return the routing
     * @param quantity : the quantity to build
     * @param evenDate : the date used to filter the active routing
     * @return GenericValue routing : the routing object
     **/
    // DEPRECATED METHOD
    public static GenericValue getRouting(GenericValue product, double quantity, Timestamp eventDate, LocalDispatcher dispatcher){
        Debug.logInfo("getRouting called", module);
        
        //Looks for the routing associated with the product
        List listRouting = null;
        try{
            listRouting = product.getRelated("WorkEffortGoodStandard", UtilMisc.toMap("workEffortGoodStdTypeId", "ROU_PROD_TEMPLATE"),UtilMisc.toList("estimatedQuantity DESC"));
            if (listRouting.size()>0) listRouting = EntityUtil.filterByDate(listRouting,eventDate);
        } catch (GenericEntityException e) {
            Debug.logError(e,"Error : product.getRelated routing... productId="+product.getString("productId"), module);
            return null;
        }
        if (listRouting == null || listRouting.size()==0) {
            // if the product is a configuration, probably the routing is linked to the virtual product
            Map serviceResponse = null;
            try {
                serviceResponse = dispatcher.runSync("getManufacturingComponents", UtilMisc.toMap("productId", product.getString("productId"), "quantity", new Double(quantity)));
            } catch (GenericServiceException e) {
                Debug.logError("Error : getManufacturingComponents for productId ="+product.getString("productId")+"--"+e.getMessage(), module);
                return null;
            }
            String routingId = (String)serviceResponse.get("workEffortId");
            try{
                return product.getDelegator().findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", routingId));
            } catch (GenericEntityException e) {
                return null;
            }
        }
        Iterator listRoutingIter = listRouting.iterator();
        boolean found = false;
        GenericValue routingProduct = null;
        //Looks to determine which routing has a valid quantity
        while (listRoutingIter.hasNext() && !found) {
            routingProduct = (GenericValue) listRoutingIter.next();
            if (routingProduct.getDouble("estimatedQuantity")==null ||
            routingProduct.getDouble("estimatedQuantity").doubleValue() < quantity) found = true;
        }
        try{
            return routingProduct.getRelatedOneCache("WorkEffort");
        } catch (GenericEntityException e) {
            Debug.logError(e,"Error : routingProduct.getRelated routing... workEffortId="+routingProduct.getString("workEffortId")+" productId="+routingProduct.getString("productId"), module);
            return null;
        }
    }

    /**
     * Calcul the ATP Date of a list d'objet inventoryEventPlan
     * @param List The list of inventoryEventPlan that transmit to ftl
     * @return List of Double represant ATPDate
     */
    public static List getVariationProduct(List inventoryList, LocalDispatcher dispatcher){
        
        Debug.logInfo("coucou de la methode", module);
        ArrayList inventoryProductList;
        Map inventoryProductMap = new TreeMap();
        Map cumulativeAtpByEventMap = new TreeMap();
        GenericValue inventoryTmp;
        boolean firstOfList = true;
        
        //regroupement des inventorys en fonction de l'article
        Iterator iter = inventoryList.iterator();
        while( iter.hasNext() ){
            inventoryTmp = (GenericValue) iter.next();
            inventoryProductList = (ArrayList)inventoryProductMap.get( inventoryTmp.getString("productId") );
            if( inventoryProductList == null ){
                inventoryProductList = new ArrayList();
                inventoryProductMap.put( inventoryTmp.getString("productId"), inventoryProductList);
            }
            inventoryProductList.add( inventoryTmp );
        }
        
        // iteration on the product found
        ArrayList keys = new ArrayList( inventoryProductMap.keySet() );
        for (Iterator iterMap = keys.iterator(); iterMap.hasNext(); ){
            String productId = (String) iterMap.next();
            inventoryProductList = (ArrayList)  inventoryProductMap.get(productId);
            double productAtp = 0;
            for (iter = inventoryProductList.iterator();iter.hasNext();){
                //Acumulate all the InventoryEventPlanned.quantity
                inventoryTmp = (GenericValue)iter.next();
                if (firstOfList){
                    //Intinial ATP equal to the current product QOH
                    try {
                        GenericValue product = inventoryTmp.getRelatedOneCache("Product");
                        productAtp =  MrpServices.findProductMrpQoh(product, dispatcher);
                    } catch (Exception e) {
                        Debug.logError("Error : getRelatedOneCache Produc with productId="+inventoryTmp.getString("productId")+"--"+e.getMessage(), module);
                        return null;
                    }
                    firstOfList = false;
                }
                Double doubleTmp = (Double)inventoryTmp.getDouble("eventQuantity");
                productAtp += doubleTmp.doubleValue();
                cumulativeAtpByEventMap.put( inventoryTmp, new Double(productAtp) );
            }
            firstOfList = true;
        }
        
        //construct the return list
        List eventPlannedAndCumulativeAtp = new LinkedList();
        iter = inventoryList.iterator();
        while( iter.hasNext() ){
            inventoryTmp = (GenericValue)iter.next();
            Double productAtp = (Double) cumulativeAtpByEventMap.get(inventoryTmp);
            eventPlannedAndCumulativeAtp.add( productAtp );
        }
        return eventPlannedAndCumulativeAtp;
        
    }
}
