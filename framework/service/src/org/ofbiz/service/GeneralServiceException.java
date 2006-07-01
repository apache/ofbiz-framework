/*
 * $Id: GeneralServiceException.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2004 The Open For Business Project - www.ofbiz.org
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
package org.ofbiz.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;

/**
 * General Service Exception - base Exception for in-Service Errors
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      3.2
 */
public class GeneralServiceException extends org.ofbiz.base.util.GeneralException {

    protected List errorMsgList = null;
    protected Map errorMsgMap = null;
    protected Map nestedServiceResult = null;

    public GeneralServiceException() {
        super();
    }

    public GeneralServiceException(String str) {
        super(str);
    }

    public GeneralServiceException(String str, Throwable nested) {
        super(str, nested);
    }

    public GeneralServiceException(Throwable nested) {
        super(nested);
    }

    public GeneralServiceException(String str, List errorMsgList, Map errorMsgMap, Map nestedServiceResult, Throwable nested) {
        super(str, nested);
        this.errorMsgList = errorMsgList;
        this.errorMsgMap = errorMsgMap;
        this.nestedServiceResult = nestedServiceResult;
    }

    public Map returnError(String module) {
        String errMsg = this.getMessage() == null ? "Error in Service" : this.getMessage();
        if (this.getNested() != null) {
            Debug.logError(this.getNested(), errMsg, module);
        }
        return ServiceUtil.returnError(errMsg, this.errorMsgList, this.errorMsgMap, this.nestedServiceResult);
    }

    public void addErrorMessages(List errMsgs) {
        if (this.errorMsgList == null) {
            this.errorMsgList = new LinkedList();
        }
        this.errorMsgList.addAll(errMsgs);
    }
}
