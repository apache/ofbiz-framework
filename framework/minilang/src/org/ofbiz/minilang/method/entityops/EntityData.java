/*
 * $Id: EntityData.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2004 The Open For Business Project - www.ofbiz.org
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
package org.ofbiz.minilang.method.entityops;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

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
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      3.1
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
            EntityDataAssert.assertData(dataUrl, delegator, messages);
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

