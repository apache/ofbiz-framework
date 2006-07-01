/*
 * $Id: StatusServices.java 5719 2005-09-13 01:57:32Z jonesde $
 *
 * Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.ofbiz.common.status;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;

/**
 * StatusServices
 *
 * @author     <a href="mailto:johan@ibibi.com">Johan Isacsson</a>
 * @version    $Rev$
 * @since      2.1
 */
public class StatusServices {
    
    public static final String module = StatusServices.class.getName();
    
    public static Map getStatusItems(DispatchContext ctx, Map context) {
        GenericDelegator delegator = (GenericDelegator) ctx.getDelegator();
        List statusTypes = (List) context.get("statusTypeIds");
        if (statusTypes == null || statusTypes.size() == 0) {
            return ServiceUtil.returnError("Parameter statusTypeIds can not be null and must contain at least one element");
        }
        
        Iterator i = statusTypes.iterator();
        List statusItems = new LinkedList();
        while (i.hasNext()) {
            String statusTypeId = (String) i.next();
            try {
                Collection myStatusItems = delegator.findByAndCache("StatusItem", UtilMisc.toMap("statusTypeId", statusTypeId), UtilMisc.toList("sequenceId"));
                statusItems.addAll(myStatusItems);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        }        
        Map ret = new HashMap();
        ret.put("statusItems",statusItems);
        return ret;
    }

    public static Map getStatusValidChangeToDetails(DispatchContext ctx, Map context) {
        GenericDelegator delegator = (GenericDelegator) ctx.getDelegator();
        List statusValidChangeToDetails = null;
        String statusId = (String) context.get("statusId");
        try {
            statusValidChangeToDetails = delegator.findByAndCache("StatusValidChangeToDetail", UtilMisc.toMap("statusId", statusId), UtilMisc.toList("sequenceId"));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        Map ret = ServiceUtil.returnSuccess();
        if (statusValidChangeToDetails != null) {
            ret.put("statusValidChangeToDetails", statusValidChangeToDetails);
        }
        return ret;        
    }
}
