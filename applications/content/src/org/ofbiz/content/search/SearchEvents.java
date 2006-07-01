/*
 * $Id: SearchEvents.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ofbiz.content.search;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;



/**
 * SearchEvents Class
 * 
 * @author <a href="mailto:byersa@automationgroups.com">Al Byers</a> Hacked from Lucene demo file
 * @version $Rev$
 * @since 3.1
 * 
 *  
 */
public class SearchEvents {

    public static final String module = SearchEvents.class.getName();
	
    public static String indexTree(HttpServletRequest request, HttpServletResponse response) {

        Map result;
        Map serviceInMap = new HashMap();
        HttpSession session = request.getSession();
        GenericValue userLogin = (GenericValue)session.getAttribute("userLogin");
        serviceInMap.put("userLogin", userLogin);
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Map paramMap = UtilHttp.getParameterMap(request);
        String siteId = (String)paramMap.get("contentId");
        serviceInMap.put("contentId", siteId);
        try {
            result = dispatcher.runSync("indexTree", serviceInMap);
        } catch (GenericServiceException e) {
            String errorMsg = "Error calling the indexTree service." + e.toString();
            Debug.logError(e, errorMsg, module);
            request.setAttribute("_ERROR_MESSAGE_", errorMsg + e.toString());
            return "error";
        }
        String errMsg = ServiceUtil.getErrorMessage(result);
        if (Debug.infoOn()) Debug.logInfo("errMsg:" + errMsg, module);
        if (Debug.infoOn()) Debug.logInfo("result:" + result, module);
        if (UtilValidate.isEmpty(errMsg)) {
            List badIndexList = (List)result.get("badIndexList");
            if (Debug.infoOn()) Debug.logInfo("badIndexList:" + badIndexList, module);
            String badIndexMsg = StringUtil.join(badIndexList, "\n") + badIndexList.size() + " entities not indexed";
            Integer goodIndexCount = (Integer)result.get("goodIndexCount");
            String goodIndexMsg = goodIndexCount + " entities indexed.";
            if (Debug.infoOn()) Debug.logInfo("goodIndexCount:" + goodIndexCount, module);
            ServiceUtil.setMessages(request, badIndexMsg, goodIndexMsg, null);
            return "success";
        } else {
            ServiceUtil.setMessages(request, errMsg, null, null);
            return "error";
        }
    }
}
