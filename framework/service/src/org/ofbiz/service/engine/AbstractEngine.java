/*
 * $Id: AbstractEngine.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2004-2005 The Open For Business Project - www.ofbiz.org
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
import java.util.List;
import java.util.Iterator;

import javolution.util.FastMap;

import org.ofbiz.service.ServiceDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.GenericServiceCallback;
import org.ofbiz.service.config.ServiceConfigUtil;
import org.ofbiz.base.config.GenericConfigException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilXml;

import org.w3c.dom.Element;

/**
 * Abstract Service Engine
 * 
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      3.1
 */
public abstract class AbstractEngine implements GenericEngine {

    public static final String module = AbstractEngine.class.getName();
    protected static Map locationMap = null;

    protected ServiceDispatcher dispatcher = null;

    protected AbstractEngine(ServiceDispatcher dispatcher) {
        this.dispatcher = dispatcher;
        initLocations();
    }

    // creates the location alias map
    protected synchronized void initLocations() {
        if (locationMap == null) {
            locationMap = FastMap.newInstance();

            Element root = null;
            try {
                root = ServiceConfigUtil.getXmlRootElement();
            } catch (GenericConfigException e) {
                Debug.logError(e, module);
            }

            if (root != null) {
                List locationElements = UtilXml.childElementList(root, "service-location");
                if (locationElements != null) {
                    Iterator i = locationElements.iterator();
                    while (i.hasNext()) {
                        Element e = (Element) i.next();
                        locationMap.put(e.getAttribute("name"), e.getAttribute("location"));
                    }
                }
            }
            Debug.logInfo("Loaded Service Locations : " + locationMap, module);
        }
    }

    // uses the lookup map to determin if the location has been aliased in serviceconfig.xml
    protected String getLocation(ModelService model) {
        if (locationMap.containsKey(model.location)) {
            return (String) locationMap.get(model.location);
        } else {
            return model.location;
        }
    }

    /**
     * @see org.ofbiz.service.engine.GenericEngine#sendCallbacks(org.ofbiz.service.ModelService, java.util.Map, java.lang.Object, int)
     */
    public void sendCallbacks(ModelService model, Map context, Object cbObj, int mode) throws GenericServiceException {
        List callbacks = dispatcher.getCallbacks(model.name);
        if (callbacks != null) {
            Iterator i = callbacks.iterator();
            while (i.hasNext()) {
                GenericServiceCallback gsc = (GenericServiceCallback) i.next();
                if (gsc.isEnabled()) {
                    if (cbObj == null) {
                        gsc.receiveEvent(context);
                    } else if (cbObj instanceof Throwable) {
                        gsc.receiveEvent(context, (Throwable) cbObj);
                    } else if (cbObj instanceof Map) {
                        gsc.receiveEvent(context, (Map) cbObj);
                    } else {
                        throw new GenericServiceException("Callback object is not Throwable or Map");
                    }
                } else {
                    i.remove();
                }
            }
        }
    }
}
