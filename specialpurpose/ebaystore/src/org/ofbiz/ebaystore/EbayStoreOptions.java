package org.ofbiz.ebaystore;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.entity.Delegator;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.webapp.event.EventHandlerException;

import com.ebay.sdk.ApiException;
import com.ebay.sdk.SdkException;
import com.ebay.sdk.SdkSoapException;
import com.ebay.sdk.call.GetStoreOptionsCall;
import com.ebay.soap.eBLBaseComponents.GetStoreOptionsRequestType;
import com.ebay.soap.eBLBaseComponents.GetStoreOptionsResponseType;
import com.ebay.soap.eBLBaseComponents.StoreColorSchemeType;
import com.ebay.soap.eBLBaseComponents.StoreColorType;
import com.ebay.soap.eBLBaseComponents.StoreFontFaceCodeType;
import com.ebay.soap.eBLBaseComponents.StoreFontSizeCodeType;
import com.ebay.soap.eBLBaseComponents.StoreFontType;
import com.ebay.soap.eBLBaseComponents.StoreThemeArrayType;
import com.ebay.soap.eBLBaseComponents.StoreThemeType;

import net.sf.json.JSONObject;

public class EbayStoreOptions {
	
	public static String  retrieveThemeColorSchemeByThemeId(HttpServletRequest request,HttpServletResponse response){
		   
		   Locale locale = UtilHttp.getLocale(request);
		   Delegator delegator = (Delegator) request.getAttribute("delegator");
	       GetStoreOptionsRequestType req = null;
	       GetStoreOptionsResponseType resp  = null;
	       StoreThemeArrayType returnedBasicThemeArray = null;
	       
	       try {
	    	   Map paramMap = UtilHttp.getCombinedMap(request);
	    	   if(paramMap.get("productStoreId") != null){
	    		   String themeId = (String)paramMap.get("themeId");
	    		   
	    		   GetStoreOptionsCall  call = new GetStoreOptionsCall(EbayStoreHelper.getApiContext((String)paramMap.get("productStoreId"), locale, delegator));
	    		   req = new GetStoreOptionsRequestType();

	    		   resp = (GetStoreOptionsResponseType) call.execute(req);
	    		   if(resp != null && "SUCCESS".equals(resp.getAck().toString())){
	    			   
	    			   returnedBasicThemeArray = resp.getBasicThemeArray();
	    			   StoreThemeType[] storeBasicTheme = returnedBasicThemeArray.getTheme();
	    			   
	    			   int i=0;
	    			   String colorSchemeId = themeId.substring(themeId.indexOf("-")+1);
	    			   themeId = themeId.substring(0,themeId.indexOf("-"));
	    			  
	    			   Map<String,Object> storeColorSchemeMap = FastMap.newInstance();
	    			   while(i<storeBasicTheme.length){
	    				   
	    				   StoreThemeType storeThemeType = (StoreThemeType)storeBasicTheme[i];
	    				   if(themeId.equals(storeThemeType.getThemeID().toString())){
	    					   StoreColorSchemeType colorSchemeType = storeThemeType.getColorScheme();
	    					   if(colorSchemeType!=null){
	    						   if(colorSchemeId.equals(colorSchemeType.getColorSchemeID().toString())){
	    							   StoreColorType storeColor = colorSchemeType.getColor();
	    							   storeColorSchemeMap.put("storeColorAccent",storeColor.getAccent());
	    							   storeColorSchemeMap.put("storeColorPrimary",storeColor.getPrimary());
	    							   storeColorSchemeMap.put("storeColorSecondary",storeColor.getSecondary());
	    							   
	    							   // get font,size and color 
	    							   StoreFontType storeFontType = colorSchemeType.getFont();
	    							   storeColorSchemeMap.put("storeFontTypeNameFaceColor",storeFontType.getNameColor());
	    		    				   storeColorSchemeMap.put("storeFontTypeFontFaceValue",storeFontType.getNameFace().value());
	    		    				   storeColorSchemeMap.put("storeFontTypeSizeFaceValue",storeFontType.getNameSize().value());
	    		    				   
	    		    				   storeColorSchemeMap.put("storeFontTypeTitleColor",storeFontType.getTitleColor());
	    		    				   storeColorSchemeMap.put("storeFontTypeFontTitleValue",storeFontType.getTitleFace().value());
	    		    				   storeColorSchemeMap.put("storeFontSizeTitleValue",storeFontType.getTitleSize().value());
	    		    				   
	    		    				   storeColorSchemeMap.put("storeFontTypeDescColor",storeFontType.getDescColor());
	    		    				   storeColorSchemeMap.put("storeFontTypeFontDescValue",storeFontType.getDescFace().value());
	    		    				   storeColorSchemeMap.put("storeDescSizeValue",storeFontType.getDescSize().value());
	    		    				   toJsonObject(storeColorSchemeMap,response);
	    		    				   
	    							   break;
	    						   }
	    					   }
	    				   }
	    				   i++;
	    			   }
	    		   }
	    	   }
	       }catch (ApiException e) {
	    	   e.printStackTrace();
	    	   return "error";
	       } catch (SdkSoapException e) {
	    	   e.printStackTrace();
	    	   return "error";
	       } catch (SdkException e) {
	    	   e.printStackTrace();
	    	   return "error";
	       } catch (EventHandlerException e) {
	    	   e.printStackTrace();
	    	   return "error";
	       }
	       
	       return "success";
	   }
	
	 public static void toJsonObject(Map<String,Object> attrMap, HttpServletResponse response) throws EventHandlerException{
	   	 JSONObject json = JSONObject.fromObject(attrMap);
	        String jsonStr = json.toString();
	        if (jsonStr == null) {
	            throw new EventHandlerException("JSON Object was empty; fatal error!");
	        }
	        // set the X-JSON content type
	        response.setContentType("application/json");
	        // jsonStr.length is not reliable for unicode characters
	        try {
	            response.setContentLength(jsonStr.getBytes("UTF8").length);
	        } catch (UnsupportedEncodingException e) {
	            throw new EventHandlerException("Problems with Json encoding", e);
	        }
	        // return the JSON String
	        Writer out;
	        try {
	            out = response.getWriter();
	            out.write(jsonStr);
	            out.flush();
	        } catch (IOException e) {
	            throw new EventHandlerException("Unable to get response writer", e);
	        }
	   }

}
