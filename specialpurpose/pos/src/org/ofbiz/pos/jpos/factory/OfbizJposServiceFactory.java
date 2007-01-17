/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.ofbiz.pos.jpos.factory;

import java.util.HashMap;
import java.util.Map;

import jpos.JposConst;
import jpos.JposException;
import jpos.config.JposEntry;
import jpos.loader.JposServiceInstance;
import jpos.loader.JposServiceInstanceFactory;

import org.ofbiz.base.util.ObjectType;
import org.ofbiz.pos.jpos.service.BaseService;


public class OfbizJposServiceFactory extends Object implements JposServiceInstanceFactory {

    public static final String module = OfbizJposServiceFactory.class.getName();
    private static Map serviceMap = new HashMap();

    public JposServiceInstance createInstance(String logicalName, JposEntry entry) throws JposException {
        // check to see if we have a service class property
        if (!entry.hasPropertyWithName(JposEntry.SERVICE_CLASS_PROP_NAME)) {
            throw new JposException(JposConst.JPOS_E_NOSERVICE, "serviceClass property not found!");
        }

        String className = (String) entry.getPropertyValue(JposEntry.SERVICE_CLASS_PROP_NAME);
        BaseService service = (BaseService) serviceMap.get(className);

        if (service != null) {
            service.setEntry(entry);
        } else {
            try {
                Object obj = ObjectType.getInstance(className);
                if (obj == null) {
                    throw new JposException(JposConst.JPOS_E_NOEXIST, "unable to locate serviceClass");
                }                

                if (!(obj instanceof JposServiceInstance)) {
                    throw new JposException(JposConst.JPOS_E_NOSERVICE, "serviceClass is not an instance of JposServiceInstance");
                } else if (!(obj instanceof BaseService)) {
                    throw new JposException(JposConst.JPOS_E_NOSERVICE, "serviceClass is not an instance of BaseKybService");
                } else {
                    service = (BaseService) obj;
                    service.setEntry(entry);
                    serviceMap.put(className, service);
                }
            } catch (Exception e) {
                throw new JposException(JposConst.JPOS_E_NOSERVICE, "Error creating the service instance [" + className + "]", e);
            }
        }

        return service;
    }
}
