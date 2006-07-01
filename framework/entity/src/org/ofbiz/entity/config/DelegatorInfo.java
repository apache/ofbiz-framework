/*
 * $Id: DelegatorInfo.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
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
package org.ofbiz.entity.config;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.w3c.dom.Element;

/**
 * Misc. utility method for dealing with the entityengine.xml file
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a> 
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a> 
 * @version    $Rev$
 * @since      2.0
 */
public class DelegatorInfo extends NamedInfo {

    public String entityModelReader;
    public String entityGroupReader;
    public String entityEcaReader;
    public boolean useEntityEca;
    public String entityEcaHandlerClassName;
    public boolean useDistributedCacheClear;
    public String distributedCacheClearClassName;
    public String distributedCacheClearUserLoginId;
    public String sequencedIdPrefix;
    public Map groupMap = new HashMap();

    public DelegatorInfo(Element element) {
        super(element);
        this.entityModelReader = element.getAttribute("entity-model-reader");
        this.entityGroupReader = element.getAttribute("entity-group-reader");
        this.entityEcaReader = element.getAttribute("entity-eca-reader");

        // this defaults to true, ie anything but false is true
        this.useEntityEca = !"false".equalsIgnoreCase(element.getAttribute("entity-eca-enabled"));
        this.entityEcaHandlerClassName = element.getAttribute("entity-eca-handler-class-name");

        // this defaults to false, ie anything but true is false
        this.useDistributedCacheClear = "true".equalsIgnoreCase(element.getAttribute("distributed-cache-clear-enabled"));
        this.distributedCacheClearClassName = element.getAttribute("distributed-cache-clear-class-name");
        if (UtilValidate.isEmpty(this.distributedCacheClearClassName)) this.distributedCacheClearClassName = "org.ofbiz.entityext.cache.EntityCacheServices";
        
        this.distributedCacheClearUserLoginId = element.getAttribute("distributed-cache-clear-user-login-id");
        if (UtilValidate.isEmpty(this.distributedCacheClearUserLoginId)) this.distributedCacheClearUserLoginId= "admin";

        this.sequencedIdPrefix = element.getAttribute("sequenced-id-prefix");
        
        List groupMapList = UtilXml.childElementList(element, "group-map");
        Iterator groupMapIter = groupMapList.iterator();

        while (groupMapIter.hasNext()) {
            Element groupMapElement = (Element) groupMapIter.next();
            groupMap.put(groupMapElement.getAttribute("group-name"), groupMapElement.getAttribute("datasource-name"));
        }
    }
}
