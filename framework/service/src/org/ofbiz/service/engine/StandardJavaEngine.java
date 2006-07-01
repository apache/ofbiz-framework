/*
 * $Id: StandardJavaEngine.java 5462 2005-08-05 18:35:48Z jonesde $
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceDispatcher;
import org.ofbiz.base.util.Debug;

/**
 * Standard Java Static Method Service Engine
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
 */
public final class StandardJavaEngine extends GenericAsyncEngine {

    public static final String module = StandardJavaEngine.class.getName();

    public StandardJavaEngine(ServiceDispatcher dispatcher) {
        super(dispatcher);
    }
 
    /**
     * @see org.ofbiz.service.engine.GenericEngine#runSyncIgnore(java.lang.String, org.ofbiz.service.ModelService, java.util.Map)
     */
    public void runSyncIgnore(String localName, ModelService modelService, Map context) throws GenericServiceException {
        Map result = runSync(localName, modelService, context);
    }

    /**
     * @see org.ofbiz.service.engine.GenericEngine#runSync(java.lang.String, org.ofbiz.service.ModelService, java.util.Map)
     */
    public Map runSync(String localName, ModelService modelService, Map context) throws GenericServiceException {
        Object result = serviceInvoker(localName, modelService, context);

        if (result == null || !(result instanceof Map))
            throw new GenericServiceException("Service did not return expected result");
        return (Map) result;
    }

    // Invoke the static java method service.
    private Object serviceInvoker(String localName, ModelService modelService, Map context) throws GenericServiceException {
        // static java service methods should be: public Map methodName(DispatchContext dctx, Map context)
        DispatchContext dctx = dispatcher.getLocalContext(localName);

        if (modelService == null)
            Debug.logError("ERROR: Null Model Service.", module);
        if (dctx == null)
            Debug.logError("ERROR: Null DispatchContext.", module);
        if (context == null)
            Debug.logError("ERROR: Null Service Context.", module);

        Class[] paramTypes = new Class[] {DispatchContext.class, Map.class};
        Object[] params = new Object[] {dctx, context};
        Object result = null;

        // check the package and method names
        if (modelService.location == null || modelService.invoke == null)
            throw new GenericServiceException("Cannot locate service to invoke (location or invoke name missing)");

        // get the classloader to use
        ClassLoader cl = null;

        if (dctx == null)
            cl = this.getClass().getClassLoader();
        else
            cl = dctx.getClassLoader();

        try {
            Class c = cl.loadClass(this.getLocation(modelService));
            Method m = c.getMethod(modelService.invoke, paramTypes);
            result = m.invoke(null, params);
        } catch (ClassNotFoundException cnfe) {
            throw new GenericServiceException("Cannot find service location", cnfe);
        } catch (NoSuchMethodException nsme) {
            throw new GenericServiceException("Service method does not exist", nsme);
        } catch (SecurityException se) {
            throw new GenericServiceException("Access denied", se);
        } catch (IllegalAccessException iae) {
            throw new GenericServiceException("Method not accessible", iae);
        } catch (IllegalArgumentException iarge) {
            throw new GenericServiceException("Invalid parameter match", iarge);
        } catch (InvocationTargetException ite) {
            throw new GenericServiceException("Service target threw an unexpected exception", ite.getTargetException());
        } catch (NullPointerException npe) {
            throw new GenericServiceException("Specified object is null", npe);
        } catch (ExceptionInInitializerError eie) {
            throw new GenericServiceException("Initialization failed", eie);
        } catch (Throwable th) {
            throw new GenericServiceException("Error or nknown exception", th);
        }

        return result;
    }
}

