/*
 * $Id: ServiceMcaAction.java 5462 2005-08-05 18:35:48Z jonesde $
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
package org.ofbiz.service.mail;

import java.util.Map;

import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericValue;

import org.w3c.dom.Element;

/**
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      3.3
 */
public class ServiceMcaAction implements java.io.Serializable {

    public static final String module = ServiceMcaAction.class.getName();

    protected String serviceName = null;
    protected String serviceMode = null;
    protected String runAsUser = null;
    protected boolean persist = false;

    protected ServiceMcaAction() { }

    protected ServiceMcaAction(Element action) {
        this.serviceName = action.getAttribute("service");
        this.serviceMode = action.getAttribute("mode");
        this.runAsUser = action.getAttribute("runAsUser");
        this.persist = "true".equals(action.getAttribute("persist"));
    }

    public boolean runAction(LocalDispatcher dispatcher, MimeMessageWrapper messageWrapper, GenericValue userLogin) throws GenericServiceException {
        Map serviceContext = UtilMisc.toMap("messageWrapper", messageWrapper, "userLogin", userLogin);
        serviceContext.put("userLogin", ServiceUtil.getUserLogin(dispatcher.getDispatchContext(), serviceContext, runAsUser));

        if (serviceMode.equals("sync")) {
            Map result = dispatcher.runSync(serviceName, serviceContext);
            if (ServiceUtil.isError(result)) {
                Debug.logError(ServiceUtil.getErrorMessage(result), module);
                return false;
            } else {
                return true;
            }
        } else if (serviceMode.equals("async")) {
            dispatcher.runAsync(serviceName, serviceContext, persist);
            return true;
        } else {
            Debug.logError("Invalid service mode [" + serviceMode + "] unable to invoke MCA action (" + serviceName + ").", module);
            return false;
        }
    }
}
