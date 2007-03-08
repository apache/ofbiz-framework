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
package org.ofbiz.minilang.method.entityops;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javolution.util.FastList;

import org.ofbiz.base.location.FlexibleLocation;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.util.EntityDataAssert;
import org.ofbiz.entity.util.EntitySaxReader;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Uses the delegator to find entity values by a primary key
 */
public class EntityData extends MethodOperation {
    
    public static final String module = EntityData.class.getName();
    
    protected FlexibleStringExpander locationExdr;
    protected FlexibleStringExpander delegatorNameExdr;
    protected FlexibleStringExpander timeoutExdr;
    protected ContextAccessor errorListAcsr;
    protected String mode;

    public EntityData(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        locationExdr = new FlexibleStringExpander(element.getAttribute("location"));
        delegatorNameExdr = new FlexibleStringExpander(element.getAttribute("delegator-name"));
        timeoutExdr = new FlexibleStringExpander(element.getAttribute("timeout"));
        errorListAcsr = new ContextAccessor(element.getAttribute("error-list-name"), "error_list");
        
        mode = element.getAttribute("mode");
        if (UtilValidate.isEmpty(mode)) {
            mode = "load";
        }
    }

    public boolean exec(MethodContext methodContext) {
        List messages = (List) errorListAcsr.get(methodContext);
        if (messages == null) {
            messages = FastList.newInstance();
            errorListAcsr.put(methodContext, messages);
        }

        String location = this.locationExdr.expandString(methodContext.getEnvMap());
        String delegatorName = this.delegatorNameExdr.expandString(methodContext.getEnvMap());

        GenericDelegator delegator = methodContext.getDelegator();
        if (delegatorName != null && delegatorName.length() > 0) {
            delegator = GenericDelegator.getGenericDelegator(delegatorName);
        }

        URL dataUrl = null;
        try {
            dataUrl = FlexibleLocation.resolveLocation(location, methodContext.getLoader());
        } catch (MalformedURLException e) {
            messages.add("Could not find Entity Data document in resource: " + location + "; error was: " + e.toString());
        }
        if (dataUrl == null) {
            messages.add("Could not find Entity Data document in resource: " + location);
        }
        
        String timeout = this.timeoutExdr.expandString(methodContext.getEnvMap());
        int txTimeout = -1;
        if (UtilValidate.isNotEmpty(timeout)) {
            try {
                txTimeout = Integer.parseInt(timeout);
            } catch (NumberFormatException e) {
                Debug.logWarning("Timeout not formatted properly in entity-data operation, defaulting to container default", module);
            }
        }

        if ("assert".equals(mode)) {
            // load the XML file, read in one element at a time and check it against the database
            try {
                EntityDataAssert.assertData(dataUrl, delegator, messages);
            } catch (Exception e) {
                String xmlError = "Error checking/asserting XML Resource \"" + dataUrl.toExternalForm() + "\"; Error was: " + e.getMessage();
                //Debug.logError(e, xmlError, module);
                messages.add(xmlError);
            }
        } else {
            // again, default to load
            try {
                EntitySaxReader reader = null;
                if (txTimeout > 0) {
                    reader = new EntitySaxReader(delegator, txTimeout);
                } else {
                    reader = new EntitySaxReader(delegator);
                }
                long rowsChanged = reader.parse(dataUrl);
            } catch (Exception e) {
                String xmlError = "Error loading XML Resource \"" + dataUrl.toExternalForm() + "\"; Error was: " + e.getMessage();
                messages.add(xmlError);
                Debug.logError(e, xmlError, module);
            }
        }
        return true;
    }

    public String rawString() {
        // TODO: something more than the empty tag
        return "<entity-data/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}

