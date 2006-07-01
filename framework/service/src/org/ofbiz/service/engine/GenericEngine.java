/*
 * $Id: GenericEngine.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
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
package org.ofbiz.service.engine;

import java.util.Map;

import org.ofbiz.service.GenericRequester;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ModelService;

/**
 * Generic Engine Interface
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a> 
 * @version    $Rev$
 * @since      2.0
 */
public interface GenericEngine {

    public static final int ASYNC_MODE = 22;
    public static final int SYNC_MODE = 21;

    /**
     * Run the service synchronously and return the result.
     * 
     * @param localName Name of the LocalDispatcher.
     * @param modelService Service model object.
     * @param context Map of name, value pairs composing the context.
     * @return Map of name, value pairs composing the result.
     * @throws GenericServiceException
     */
    public Map runSync(String localName, ModelService modelService, Map context) throws GenericServiceException;

    /**
     * Run the service synchronously and IGNORE the result.
     * 
     * @param localName Name of the LocalDispatcher.
     * @param modelService Service model object.
     * @param context Map of name, value pairs composing the context.
     * @throws GenericServiceException
     */
    public void runSyncIgnore(String localName, ModelService modelService, Map context) throws GenericServiceException;

    /**
     * Run the service asynchronously, passing an instance of GenericRequester that will receive the result.
     * 
     * @param localName Name of the LocalDispatcher.
     * @param modelService Service model object.
     * @param context Map of name, value pairs composing the context.
     * @param requester Object implementing GenericRequester interface which will receive the result.
     * @param persist True for store/run; False for run.
     * @throws GenericServiceException
     */
    public void runAsync(String localName, ModelService modelService, Map context, GenericRequester requester, boolean persist)
        throws GenericServiceException;

    /**
     * Run the service asynchronously and IGNORE the result.
     * 
     * @param localName Name of the LocalDispatcher.
     * @param modelService Service model object.
     * @param context Map of name, value pairs composing the context.
     * @param persist True for store/run; False for run.
     * @throws GenericServiceException
     */
    public void runAsync(String localName, ModelService modelService, Map context, boolean persist) throws GenericServiceException;

    /**
     * Send the service callbacks
     * @param modelService Service model object
     * @param context Map of name, value pairs composing the context 
     * @param cbObj Object to return to callback (Throwable or Map)
     * @param mode Service mode (sync or async)
     * @throws GenericServiceException
     */
    public void sendCallbacks(ModelService modelService, Map context, Object cbObj, int mode) throws GenericServiceException;
}

