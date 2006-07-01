/*
 * $Id: GenericEngineFactory.java 5462 2005-08-05 18:35:48Z jonesde $
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

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.ofbiz.base.config.GenericConfigException;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ServiceDispatcher;
import org.ofbiz.service.config.ServiceConfigUtil;
import org.ofbiz.base.util.UtilXml;
import org.w3c.dom.Element;

/**
 * Generic Engine Factory
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
 */
public class GenericEngineFactory {

    protected ServiceDispatcher dispatcher = null;
    protected Map engines = null;
    
    public GenericEngineFactory(ServiceDispatcher dispatcher) {
        this.dispatcher = dispatcher;
        engines = new HashMap();
    }

    /** 
     * Gets the GenericEngine instance that corresponds to given the name
     *@param engineName Name of the engine
     *@return GenericEngine that corresponds to the engineName
     */
    public GenericEngine getGenericEngine(String engineName) throws GenericServiceException {        
        Element rootElement = null;

        try {
            rootElement = ServiceConfigUtil.getXmlRootElement();
        } catch (GenericConfigException e) {
            throw new GenericServiceException("Error getting Service Engine XML root element", e);
        }
        Element engineElement = UtilXml.firstChildElement(rootElement, "engine", "name", engineName);

        if (engineElement == null) {
            throw new GenericServiceException("Cannot find an engine definition for the engine name [" + engineName + "] in the serviceengine.xml file");
        }

        String className = engineElement.getAttribute("class");

        GenericEngine engine = (GenericEngine) engines.get(engineName);

        if (engine == null) {
            synchronized (GenericEngineFactory.class) {
                engine = (GenericEngine) engines.get(engineName);
                if (engine == null) {
                    Class[] paramTypes = new Class[] { ServiceDispatcher.class };
                    Object[] params = new Object[] { dispatcher };

                    try {
                        ClassLoader loader = Thread.currentThread().getContextClassLoader();
                        Class c = loader.loadClass(className);
                        Constructor cn = c.getConstructor(paramTypes);
                        engine = (GenericEngine) cn.newInstance(params);
                    } catch (Exception e) {
                        throw new GenericServiceException(e.getMessage(), e);
                    }
                    if (engine != null) {
                        engines.put(engineName, engine);
                    }
                }
            }
        }

        return engine;
    }
}

