package org.ofbiz.ebay;

import java.util.Locale;
import java.util.Map;
import javolution.util.FastMap;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import com.ebay.sdk.ApiException;
import com.ebay.sdk.SdkException;
import com.ebay.sdk.SdkSoapException;
import com.ebay.sdk.call.AddSellingManagerInventoryFolderCall;
import com.ebay.sdk.call.AddSellingManagerProductCall;
import com.ebay.sdk.call.GetSellingManagerInventoryCall;
import com.ebay.sdk.call.GetSellingManagerInventoryFolderCall;
import com.ebay.sdk.call.ReviseSellingManagerProductCall;
import com.ebay.soap.eBLBaseComponents.AddSellingManagerInventoryFolderRequestType;
import com.ebay.soap.eBLBaseComponents.AddSellingManagerInventoryFolderResponseType;
import com.ebay.soap.eBLBaseComponents.AddSellingManagerProductRequestType;
import com.ebay.soap.eBLBaseComponents.AddSellingManagerProductResponseType;
import com.ebay.soap.eBLBaseComponents.GetSellingManagerInventoryFolderRequestType;
import com.ebay.soap.eBLBaseComponents.GetSellingManagerInventoryFolderResponseType;
import com.ebay.soap.eBLBaseComponents.GetSellingManagerInventoryRequestType;
import com.ebay.soap.eBLBaseComponents.GetSellingManagerInventoryResponseType;
import com.ebay.soap.eBLBaseComponents.ReviseSellingManagerProductRequestType;
import com.ebay.soap.eBLBaseComponents.ReviseSellingManagerProductResponseType;
import com.ebay.soap.eBLBaseComponents.SellingManagerFolderDetailsType;
import com.ebay.soap.eBLBaseComponents.SellingManagerProductDetailsType;
import com.ebay.soap.eBLBaseComponents.SellingManagerProductType;

public class EbayStoreInventoryServices {
	private static final String resource = "EbayUiLabels";
	private static final String module = EbayStoreInventoryServices.class.getName();
	private static final String defaultFolderName = "OFBizProducts";
	private static String folderId = null;
	public EbayStoreInventoryServices() {
		// TODO Auto-generated constructor stub
	}
	/*update inventory on ebay site*/
	public static Map<String,Object> updateEbayStoreInventory(DispatchContext dctx, Map<String,Object> context){
		Map<String,Object> result = FastMap.newInstance();
		Locale locale = (Locale) context.get("locale");
		Delegator delegator = dctx.getDelegator();
		GetSellingManagerInventoryRequestType invenReq = null;
		GetSellingManagerInventoryResponseType invenResp = null;
		boolean checkProd = false;
		boolean status = false;
		try {
			if(context.get("productStoreId") == null || context.get("productId") == null || context.get("folderId") == null){
				result  = ServiceUtil.returnError("The process was required productStoreId, productId and ebay inventory folderId.");
				result.put("productStoreId", context.get("productStoreId"));
				result.put("facilityId", context.get("facilityId"));
				result.put("folderId", context.get("folderId"));
				return result;
			}

			String productId = (String)context.get("productId");
			String folderId = (String)context.get("folderId");
			// start upload/update products which selected  to an ebay inventory 
			if(folderId != null){
				GetSellingManagerInventoryCall invenCall = new GetSellingManagerInventoryCall(EbayHelper.getApiContext((String)context.get("productStoreId"), locale, delegator));
				invenReq = new GetSellingManagerInventoryRequestType();
				invenResp = (GetSellingManagerInventoryResponseType) invenCall.execute(invenReq);
				if(invenResp != null && "SUCCESS".equals(invenResp.getAck().toString())){
					GenericValue ebayProductStoreInventory = delegator.findByPrimaryKey("EbayProductStoreInventory", UtilMisc.toMap("productId", productId,"facilityId",context.get("facilityId"),"productStoreId",context.get("productStoreId")));

					SellingManagerProductType[]  sellingManagerProductTypeList = invenResp.getSellingManagerProduct();
					for(SellingManagerProductType sellingManagerProductType : sellingManagerProductTypeList){
						SellingManagerProductDetailsType sellingManagerProductDetailsType = sellingManagerProductType.getSellingManagerProductDetails();
						if(String.valueOf(sellingManagerProductDetailsType.getFolderID()).equals(folderId) && String.valueOf(sellingManagerProductDetailsType.getProductID()).equals(String.valueOf(ebayProductStoreInventory.getLong("ebayProductId"))) && String.valueOf(sellingManagerProductDetailsType.getCustomLabel()).equals(productId)){
							checkProd = true;
							break;
						}
					}
				}

				// checkProduct is true then update detail  but is false do create new one.
				if(checkProd == true){
					status = updateProductInEbayInventoryFolder(dctx,context);
				}else {
					status = createNewProductInEbayInventoryFolder(dctx,context);
				}
				if(status == true){
					Debug.logInfo("Done to updated product ".concat(context.get("productId").toString()), module);
					result = ServiceUtil.returnSuccess("Updated ebay store inventory in folder "+context.get("folderId")+" success..");
				}else{
					result = ServiceUtil.returnError("Fail to update ebay store inventory.");
				}
			}
		}catch (ApiException e) {
			result = ServiceUtil.returnFailure(e.getMessage());
		} catch (SdkSoapException e) {
			result = ServiceUtil.returnFailure(e.getMessage());
		} catch (SdkException e) {
			result = ServiceUtil.returnFailure(e.getMessage());
		} catch (GenericEntityException e) {
			result = ServiceUtil.returnFailure(e.getMessage());
		} 
		result.put("productStoreId", context.get("productStoreId"));
		result.put("facilityId", context.get("facilityId"));
		result.put("folderId", context.get("folderId"));
		return result;
	}

	/* add new product and quantity to ebay inventory */
	public static boolean createNewProductInEbayInventoryFolder(DispatchContext dctx, Map<String,Object> context){
		Locale locale = (Locale) context.get("locale");
		Delegator delegator = dctx.getDelegator();
		AddSellingManagerProductRequestType productReq = null;
		AddSellingManagerProductResponseType productResp = null;
		boolean flag = false;

		try {
			if(context.get("productStoreId") != null && context.get("productId")!=null && context.get("folderId")!=null){
				String productId = (String)context.get("productId");
				String folderId = (String)context.get("folderId");
				AddSellingManagerProductCall productCall = new AddSellingManagerProductCall(EbayHelper.getApiContext((String)context.get("productStoreId"), locale, delegator));
				productReq = new AddSellingManagerProductRequestType();
				productReq.setFolderID(new Long(folderId));
				SellingManagerProductDetailsType  sellingManagerProductDetailsType = new SellingManagerProductDetailsType();
				GenericValue ebayProductStoreInventory = delegator.findByPrimaryKey("EbayProductStoreInventory", UtilMisc.toMap("productId", productId,"facilityId",context.get("facilityId"),"productStoreId",context.get("productStoreId")));

				sellingManagerProductDetailsType.setProductName((delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId))).getString("internalName"));
				//Must keep productId in SKU NUMBER because ebay allow productId field only long value. 
				sellingManagerProductDetailsType.setCustomLabel(productId);
				if(ebayProductStoreInventory!=null) sellingManagerProductDetailsType.setQuantityAvailable(ebayProductStoreInventory.getBigDecimal("availableToPromiseListing").intValue());
				
				productReq.setSellingManagerProductDetails(sellingManagerProductDetailsType);
				productResp = (AddSellingManagerProductResponseType) productCall.execute(productReq);
				if(productResp != null && "SUCCESS".equals(productResp.getAck().toString())){
					flag = true;
					ebayProductStoreInventory.put("ebayProductId", productResp.getSellingManagerProductDetails().getProductID());
					ebayProductStoreInventory.put("folderId", folderId);
					ebayProductStoreInventory.store();
				}else{
					Debug.logError("Fail to  create inventory product ".concat(productId).concat("in productStore ").concat(context.get("productStoreId").toString()).concat(" message from ebay : ").concat(productResp.getMessage()), module);
				}
			}
		} catch (ApiException e) {
			Debug.logError(e.getMessage(), module);
		} catch (SdkSoapException e) {
			Debug.logError(e.getMessage(), module);
		} catch (SdkException e) {
			Debug.logError(e.getMessage(), module);
		} catch (GenericEntityException e) {
			Debug.logError(e.getMessage(), module);
		}
		return flag;
	}
	
	/* update product and quantity to ebay inventory */
	public static boolean updateProductInEbayInventoryFolder(DispatchContext dctx, Map<String,Object> context){
		Locale locale = (Locale) context.get("locale");
		Delegator delegator = dctx.getDelegator();
		ReviseSellingManagerProductRequestType req = null;
		ReviseSellingManagerProductResponseType resp = null;
		boolean flag = false;

		try {
			if(context.get("productStoreId") != null && context.get("productId")!=null && context.get("folderId")!=null){
				String productId = (String)context.get("productId");
				String folderId = (String)context.get("folderId");
				ReviseSellingManagerProductCall call = new ReviseSellingManagerProductCall(EbayHelper.getApiContext((String)context.get("productStoreId"), locale, delegator));
				req = new ReviseSellingManagerProductRequestType();
				SellingManagerProductDetailsType  sellingManagerProductDetailsType = new SellingManagerProductDetailsType();
				GenericValue ebayProductStoreInventory = delegator.findByPrimaryKey("EbayProductStoreInventory", UtilMisc.toMap("productId", productId,"facilityId",context.get("facilityId"),"productStoreId",context.get("productStoreId")));
				Long ebayProductId = null;
				if(ebayProductStoreInventory !=null && ebayProductStoreInventory.getLong("ebayProductId")==null){
					Debug.logError("Can not update product "+productId+" has no ebay product Id in EbayProductStoreInventory. ", module);
					return flag;
				}
				if(ebayProductStoreInventory!=null && ebayProductStoreInventory.getLong("ebayProductId")!=null){
					ebayProductId = ebayProductStoreInventory.getLong("ebayProductId");
				}
				sellingManagerProductDetailsType.setProductID(ebayProductId);

				sellingManagerProductDetailsType.setProductName((delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId))).getString("internalName"));
				//Must keep productId in SKU NUMBER because ebay allow productId field only long value. 
				sellingManagerProductDetailsType.setCustomLabel(productId);
				if(ebayProductStoreInventory!=null) sellingManagerProductDetailsType.setQuantityAvailable(ebayProductStoreInventory.getBigDecimal("availableToPromiseListing").intValue());
				
				req.setSellingManagerProductDetails(sellingManagerProductDetailsType);
				resp = (ReviseSellingManagerProductResponseType) call.execute(req);
				if(resp != null && "SUCCESS".equals(resp.getAck().toString())){
					flag = true;
					ebayProductStoreInventory.put("ebayProductId", ebayProductId);
					ebayProductStoreInventory.put("folderId", folderId);
					ebayProductStoreInventory.store();
				}else{
					Debug.logError("Fail to  update inventory product ".concat(productId).concat("in productStore ").concat(context.get("productStoreId").toString()).concat(" message from ebay : ").concat(resp.getMessage()), module);
				}
			}
		} catch (ApiException e) {
			Debug.logError(e.getMessage(), module);
		} catch (SdkSoapException e) {
			Debug.logError(e.getMessage(), module);
		} catch (SdkException e) {
			Debug.logError(e.getMessage(), module);
		} catch (GenericEntityException e) {
			Debug.logError(e.getMessage(), module);
		}
		return flag;
	}

	public static Map<String,Object> getFolderInEbayStoreInventory(DispatchContext dctx, Map<String,Object> context){
		Map<String,Object> result = FastMap.newInstance();
		Locale locale = (Locale) context.get("locale");
		Delegator delegator = dctx.getDelegator();
		GetSellingManagerInventoryFolderRequestType req = null;
		GetSellingManagerInventoryFolderResponseType resp = null;
		boolean flag = false; 

		try {
			if(context.get("productStoreId") != null){
				GetSellingManagerInventoryFolderCall  call = new GetSellingManagerInventoryFolderCall(EbayHelper.getApiContext((String)context.get("productStoreId"), locale, delegator));
				req = new GetSellingManagerInventoryFolderRequestType();
				resp = (GetSellingManagerInventoryFolderResponseType) call.execute(req);
				if(resp != null && "SUCCESS".equals(resp.getAck().toString())){
					SellingManagerFolderDetailsType sellingManagerFolderDetailsType = resp.getFolder();
					if(sellingManagerFolderDetailsType!=null){
						SellingManagerFolderDetailsType[] SellingManagerFolderDetailsTypeList = sellingManagerFolderDetailsType.getChildFolder();
						for(SellingManagerFolderDetailsType sellingManagerFolderDetails : SellingManagerFolderDetailsTypeList){
							Debug.logInfo("ebay inventory folders name ".concat(sellingManagerFolderDetails.getFolderName()), module);
							if(sellingManagerFolderDetails.getFolderName().equals(defaultFolderName)){
								folderId = String.valueOf(sellingManagerFolderDetails.getFolderID());
								flag = true;
								break;
							}
						}
					}
					if(flag == false){
						folderId = createNewFolderInEbayStoreInventory(dctx,context);
					}
				}
				result = ServiceUtil.returnSuccess("load ebay store folderId "+folderId+" success..");
			}
		} catch (ApiException e) {
			result = ServiceUtil.returnFailure(e.getMessage());
		} catch (SdkSoapException e) {
			result = ServiceUtil.returnFailure(e.getMessage());
		} catch (SdkException e) {
			result = ServiceUtil.returnFailure(e.getMessage());
		}
		if(result.get("responseMessage")!=null && result.get("responseMessage").equals("fail")) folderId = null;
		result.put("folderId", folderId);
		Debug.logInfo("service return result "+ result, module);
		return result;
	}
	
	/*create new folder for export product into inventory.*/
	public static String createNewFolderInEbayStoreInventory(DispatchContext dctx, Map<String,Object> context){
		Locale locale = (Locale) context.get("locale");
		Delegator delegator = dctx.getDelegator();
		AddSellingManagerInventoryFolderRequestType req = null;
		AddSellingManagerInventoryFolderResponseType resp = null;

		try {
			if(context.get("productStoreId") != null){
				AddSellingManagerInventoryFolderCall call = new AddSellingManagerInventoryFolderCall(EbayHelper.getApiContext((String)context.get("productStoreId"), locale, delegator));
				req = new AddSellingManagerInventoryFolderRequestType();
				req.setFolderName(defaultFolderName);//req.setComment(value);//req.setParentFolderID(value)
				resp = (AddSellingManagerInventoryFolderResponseType) call.execute(req);
				if(resp != null && "SUCCESS".equals(resp.getAck().toString())) {
					folderId = String.valueOf(resp.getFolderID());
				}else {
					Debug.logError("The problem with create new folder on ebay site.", module);
					return folderId;
				}
			}
		} catch (ApiException e) {
			Debug.logError(e.getMessage(), module);
		} catch (SdkSoapException e) {
			Debug.logError(e.getMessage(), module);
		} catch (SdkException e) {
			Debug.logError(e.getMessage(), module);
		}
		return folderId;
	}
}
