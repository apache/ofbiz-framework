/*
 * $Id: EntityEcaHandler.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
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
package org.ofbiz.entity.eca;

import java.util.*;
import org.ofbiz.entity.*;

/**
 * EntityEcaHandler interface
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.1
 */
public interface EntityEcaHandler {
    
    public static final String EV_VALIDATE = "validate";
    public static final String EV_RUN = "run";
    public static final String EV_RETURN = "return";
    public static final String EV_CACHE_CLEAR = "cache-clear";
    public static final String EV_CACHE_CHECK = "cache-check";
    public static final String EV_CACHE_PUT = "cache-put";
    
    public static final String OP_CREATE = "create";
    public static final String OP_STORE = "store";
    public static final String OP_REMOVE = "remove";
    public static final String OP_FIND = "find";
    

    public void setDelegator(GenericDelegator delegator);

    public Map getEntityEventMap(String entityName);

    public void evalRules(String currentOperation, Map eventMap, String event, GenericEntity value, boolean isError) throws GenericEntityException;
}
