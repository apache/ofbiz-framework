package com.simbaquartz.xapi.services.product;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import com.fidelissd.zcp.xcommon.collections.FastList;
import com.fidelissd.zcp.xcommon.collections.FastMap;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

public class ProductImageServices {

    /**
     * Service to fetch images for given product
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> getProductImages(DispatchContext dctx, Map<String, ? extends Object> context) {
        String productId = (String) context.get("productId");
        String imageType = (String) context.get("productContentTypeId");
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = ServiceUtil.returnSuccess();
        List<Map<String, Object>> allContentDetails = FastList.newInstance();

        try {
            List<GenericValue> productContents = EntityQuery.use(delegator).from("ProductContent").where("productId", productId, "productContentTypeId", imageType).queryList();
            if (UtilValidate.isNotEmpty(productContents)) {
                for (GenericValue productContent : productContents) {
                    Map<String, Object> imageDetails = FastMap.newInstance();
                    GenericValue content = productContent.getRelatedOne("Content", true);
                    String isPrimary = (String) productContent.get("isPrimaryImage");
                    String contentId = (String) content.get("contentId");
                    GenericValue dataResource = content.getRelatedOne("DataResource", true);
                    String dataResourceId = (String) dataResource.get("dataResourceId");
                    String source = (String) dataResource.get("objectInfo");
                    if (UtilValidate.isNotEmpty(isPrimary)) {
                        imageDetails.put("isPrimary", isPrimary);
                    } else {
                        imageDetails.put("isPrimary", "N");
                    }
                    imageDetails.put("contentId", contentId);
                    imageDetails.put("dataResourceId", dataResourceId);
                    imageDetails.put("productId", productId);
                    imageDetails.put("source", source);
                    allContentDetails.add(imageDetails);
                }
            }
            result.put("imagesList", allContentDetails);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static Map<String, Object> getFinalProductImages(DispatchContext dctx, Map<String, ? extends Object> context) {
        String productIdFrom = (String) context.get("productIdFrom");
        String productIdTo = (String) context.get("productIdTo");
        String productContentTypeId = (String) context.get("productContentTypeId");
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = ServiceUtil.returnSuccess();
        List<Map<String, Object>> allContentDetails = FastList.newInstance();

        try {
            List<GenericValue> productContents = EntityQuery.use(delegator).from("KitProductContent").where("productIdFrom", productIdFrom, "productIdTo", productIdTo, "productContentTypeId",productContentTypeId).queryList();
            if (UtilValidate.isNotEmpty(productContents)) {
                for (GenericValue productContent : productContents) {
                    Map<String, Object> imageDetails = FastMap.newInstance();
                    GenericValue content = productContent.getRelatedOne("Content", true);
                    String contentId = (String) content.get("contentId");
                    String isPrimary = (String) productContent.get("isPrimaryImage");
                    GenericValue dataResource = content.getRelatedOne("DataResource", true);
                    String dataResourceId = (String) dataResource.get("dataResourceId");
                    String source = (String) dataResource.get("objectInfo");
                    if (UtilValidate.isNotEmpty(isPrimary)) {
                        imageDetails.put("isPrimary", isPrimary);
                    } else {
                        imageDetails.put("isPrimary", "N");
                    }
                    imageDetails.put("contentId", contentId);
                    imageDetails.put("dataResourceId", dataResourceId);
                    imageDetails.put("productIdFrom", productIdFrom);
                    imageDetails.put("productIdTo", productIdTo);
                    imageDetails.put("source", source);
                    allContentDetails.add(imageDetails);
                }
            }
            result.put("imagesList", allContentDetails);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static Map<String, Object> getProductImage(DispatchContext dctx, Map<String, ? extends Object> context) {
        String productId = (String) context.get("productId");
        String contentId = (String) context.get("contentId");
        String imageType = (String) context.get("productContentTypeId");
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Map<String, Object> imageDetails = FastMap.newInstance();

        try {
            GenericValue productContent = EntityQuery.use(delegator).from("ProductContent").where("productId", productId, "contentId", contentId, "productContentTypeId", imageType).queryFirst();
            if (UtilValidate.isNotEmpty(productContent)) {
                GenericValue content = productContent.getRelatedOne("Content", true);
                GenericValue dataResource = content.getRelatedOne("DataResource", true);
                String dataResourceId = (String) dataResource.get("dataResourceId");
                String source = (String) dataResource.get("objectInfo");
                imageDetails.put("contentId", contentId);
                imageDetails.put("dataResourceId", dataResourceId);
                imageDetails.put("productId", productId);
                imageDetails.put("source", source);
                result.put("image", imageDetails);
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static Map<String, Object> getProductPrimaryImage(DispatchContext dctx, Map<String, ? extends Object> context) {
        String productId = (String) context.get("productId");
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Map<String, Object> imageDetails = FastMap.newInstance();

        try {
            GenericValue productContent = EntityQuery.use(delegator).from("ProductContent").where("productId", productId, "productContentTypeId", "IMAGE", "isPrimaryImage", "Y").queryFirst();
            if (UtilValidate.isNotEmpty(productContent)) {
                GenericValue content = productContent.getRelatedOne("Content", true);
                GenericValue dataResource = content.getRelatedOne("DataResource", true);
                String dataResourceId = (String) dataResource.get("dataResourceId");
                String source = (String) dataResource.get("objectInfo");
                imageDetails.put("contentId", productContent.getString("contentId"));
                imageDetails.put("dataResourceId", dataResourceId);
                imageDetails.put("productId", productId);
                imageDetails.put("source", source);
                result.put("image", imageDetails);
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static Map<String, Object> getProductVideos(DispatchContext dctx, Map<String, ? extends Object> context) {
        String productId = (String) context.get("productId");
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = ServiceUtil.returnSuccess();
        List<Map<String, Object>> allContentDetails = FastList.newInstance();

        try {
            List<GenericValue> productContents = EntityQuery.use(delegator).from("ProductContent").where("productId", productId, "productContentTypeId", "VIDEO").queryList();
            if (UtilValidate.isNotEmpty(productContents)) {
                for (GenericValue productContent : productContents) {
                    Map<String, Object> videoDetails = FastMap.newInstance();
                    GenericValue content = productContent.getRelatedOne("Content", true);
                    String contentId = (String) content.get("contentId");
                    GenericValue dataResource = content.getRelatedOne("DataResource", true);
                    String dataResourceId = (String) dataResource.get("dataResourceId");
                    String source = (String) dataResource.get("objectInfo");
                    videoDetails.put("contentId", contentId);
                    videoDetails.put("dataResourceId", dataResourceId);
                    videoDetails.put("productId", productId);
                    videoDetails.put("source", source);
                    allContentDetails.add(videoDetails);
                }
            }
            result.put("videosList", allContentDetails);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static Map<String, Object> getProductVideoById(DispatchContext dctx, Map<String, ? extends Object> context) {
        String productId = (String) context.get("productId");
        String contentId = (String) context.get("contentId");
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Map<String, Object> videoDetails = FastMap.newInstance();

        try {
            GenericValue productContent = EntityQuery.use(delegator).from("ProductContent").where("productId", productId, "contentId", contentId, "productContentTypeId", "VIDEO").queryFirst();
            if (UtilValidate.isNotEmpty(productContent)) {
                GenericValue content = productContent.getRelatedOne("Content", true);
                GenericValue dataResource = content.getRelatedOne("DataResource", true);
                String dataResourceId = (String) dataResource.get("dataResourceId");
                String source = (String) dataResource.get("objectInfo");
                videoDetails.put("contentId", contentId);
                videoDetails.put("dataResourceId", dataResourceId);
                videoDetails.put("productId", productId);
                videoDetails.put("source", source);
                result.put("video", videoDetails);
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static Map<String, Object> getFinalProductVideo(DispatchContext dctx, Map<String, ? extends Object> context) {
        String productIdFrom = (String) context.get("productIdFrom");
        String productIdTo = (String) context.get("productIdTo");
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = ServiceUtil.returnSuccess();
        List<Map<String, Object>> allContentDetails = FastList.newInstance();

        try {
            List<GenericValue> productContents = EntityQuery.use(delegator).from("KitProductContent").where("productIdFrom", productIdFrom, "productIdTo", productIdTo, "productContentTypeId", "VIDEO").queryList();
            if (UtilValidate.isNotEmpty(productContents)) {
                for (GenericValue productContent : productContents) {
                    Map<String, Object> videoDetails = FastMap.newInstance();
                    GenericValue content = productContent.getRelatedOne("Content", true);
                    String contentId = (String) content.get("contentId");
                    GenericValue dataResource = content.getRelatedOne("DataResource", true);
                    String dataResourceId = (String) dataResource.get("dataResourceId");
                    String source = (String) dataResource.get("objectInfo");
                    videoDetails.put("contentId", contentId);
                    videoDetails.put("dataResourceId", dataResourceId);
                    videoDetails.put("productIdFrom", productIdFrom);
                    videoDetails.put("productIdTo", productIdTo);
                    videoDetails.put("source", source);
                    allContentDetails.add(videoDetails);
                }
            }
            result.put("videosList", allContentDetails);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static Map<String,Object> activateProduct(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        String productId = (String) context.get("productId");

        try{
            GenericValue product = EntityQuery.use(delegator).from("Product").where("productId",productId).queryOne();
            if(UtilValidate.isNotEmpty(product)){
                product.set("salesDiscontinuationDate", null);
                delegator.store(product);
            } else {
                String error = "No product found:" + productId;
                return ServiceUtil.returnError(error);
            }
        }
        catch(Exception e){
            return ServiceUtil.returnError(e.getMessage());
        }
        return serviceResult;
    }
    public static Map<String,Object> deactivateProduct(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        String productId = (String) context.get("productId");

        try{
            GenericValue product = EntityQuery.use(delegator).from("Product").where("productId",productId).queryOne();
            if(UtilValidate.isNotEmpty(product)){
                Timestamp salesDiscontinuationDate = UtilDateTime.nowTimestamp();
                product.set("salesDiscontinuationDate", salesDiscontinuationDate);
                delegator.store(product);
            } else {
                String error = "No product found:" + productId;
                return ServiceUtil.returnError(error);
            }
        }
        catch(Exception e){
            return ServiceUtil.returnError(e.getMessage());
        }
        return serviceResult;
    }

    public static Map<String, Object> addPrimaryProductImage(DispatchContext dctx, Map<String, ? extends Object> context) {
        String contentId = (String) context.get("contentId");
        String productId = (String) context.get("productId");
        String isPrimaryImage = (String) context.get("isPrimaryImage");
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = ServiceUtil.returnSuccess();

        try {
            List<GenericValue> productContents = EntityQuery.use(delegator).from("ProductContent").where("productId", productId).queryList();
            if (UtilValidate.isNotEmpty(productContents)) {
                for (GenericValue productContent : productContents) {
                    String imageContentId = (String) productContent.get("contentId");
                    if (UtilValidate.isNotEmpty(imageContentId)) {
                        if (imageContentId.equals(contentId)) {
                            productContent.set("isPrimaryImage", isPrimaryImage);
                            delegator.store(productContent);
                        } else {
                            productContent.set("isPrimaryImage", "N");
                            delegator.store(productContent);
                        }
                    }
                }
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static Map<String, Object> addKitPrimaryProductImage(DispatchContext dctx, Map<String, ? extends Object> context) {
        String contentId = (String) context.get("contentId");
        String productIdFrom = (String) context.get("productIdFrom");
        String productIdTo = (String) context.get("productIdTo");
        String isPrimaryImage = (String) context.get("isPrimaryImage");
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = ServiceUtil.returnSuccess();

        try {
            List<GenericValue> productContents = EntityQuery.use(delegator).from("KitProductContent").where("productIdFrom", productIdFrom, "productIdTo", productIdTo).queryList();
            if (UtilValidate.isNotEmpty(productContents)) {
                for (GenericValue productContent : productContents) {
                    String imageContentId = (String) productContent.get("contentId");
                    if (UtilValidate.isNotEmpty(imageContentId)) {
                        if (imageContentId.equals(contentId)) {
                            productContent.set("isPrimaryImage", isPrimaryImage);
                            delegator.store(productContent);
                        } else {
                            productContent.set("isPrimaryImage", "N");
                            delegator.store(productContent);
                        }
                    }
                }
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        return result;
    }

    public static Map<String, Object> getOrderList(DispatchContext ctx, Map<String, Object> context) {
        Delegator delegator = ctx.getDelegator();
        Integer viewSize = (Integer) context.get("viewSize");
        Integer startIndex = (Integer) context.get("startIndex");

        Map<String, Object> result = ServiceUtil.returnSuccess();
        List<Map> finalResult = FastList.newInstance();
        if (startIndex == null) {
            startIndex = 0;
        }

        int highIndex = (startIndex + 1) * viewSize;

        EntityQuery eq = EntityQuery.use(delegator).from("OrderHeader").where("statusId","ORDER_CREATED").orderBy("orderId").maxRows(highIndex).cursorScrollInsensitive();
        try (EntityListIterator pli = eq.queryIterator()) {
            List<GenericValue> orderList = pli.getPartialList(startIndex, viewSize);
            for (GenericValue order : orderList) {
                Map<String, Object> orderDetails = FastMap.newInstance();
                if(UtilValidate.isNotEmpty(order)) {
                    orderDetails.put("orderId", order.getString("orderId"));
                    orderDetails.put("createdBy", order.getString("createdBy"));
                    orderDetails.put("grandTotal", order.getString("grandTotal"));
                    orderDetails.put("orderTypeId", order.getString("orderTypeId"));
                    orderDetails.put("orderDate", order.getString("orderDate"));
                }
                finalResult.add(orderDetails);
            }
            int count = 0;
            count = (int) eq.queryCount();
            result.put("totalNumberOfRecords",count);
        } catch (GenericEntityException e) {
            e.printStackTrace();
            return ServiceUtil.returnError("Unable to fetch orders");
        }
        result.put("ordersList", finalResult);
        return result;
    }

}
