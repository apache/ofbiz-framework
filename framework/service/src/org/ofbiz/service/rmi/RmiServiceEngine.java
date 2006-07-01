/*
 * $Id: RmiServiceEngine.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
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
package org.ofbiz.service.rmi;

import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Map;

import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceDispatcher;
import org.ofbiz.service.engine.GenericAsyncEngine;

/**
 * RmiServiceEngine.java
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      3.0
 */
public class RmiServiceEngine extends GenericAsyncEngine {

    public RmiServiceEngine(ServiceDispatcher dispatcher) {
        super(dispatcher);
    }

    public Map runSync(String localName, ModelService modelService, Map context) throws GenericServiceException {
        return run(modelService, context);
    }

    public void runSyncIgnore(String localName, ModelService modelService, Map context) throws GenericServiceException {
        run(modelService, context);
    }

    protected Map run(ModelService service, Map context) throws GenericServiceException {
        // locate the remote dispatcher
        RemoteDispatcher rd = null;
        try {
            rd = (RemoteDispatcher) Naming.lookup(this.getLocation(service));
        } catch (NotBoundException e) {
            throw new GenericServiceException("RemoteDispatcher not bound to : " + service.location, e);
        } catch (java.net.MalformedURLException e) {
            throw new GenericServiceException("Invalid format for location");
        } catch (RemoteException e) {
            throw new GenericServiceException("RMI Error", e);
        }

        Map result = null;
        if (rd != null) {
            try {
                result = rd.runSync(service.invoke, context);
            } catch (RemoteException e) {
                throw new GenericServiceException("RMI Invocation Error", e);
            }
        } else {
            throw new GenericServiceException("RemoteDispatcher came back as null");
        }

        if (result == null) {
            throw new GenericServiceException("Null result returned");
        }

        return result;
    }
}
