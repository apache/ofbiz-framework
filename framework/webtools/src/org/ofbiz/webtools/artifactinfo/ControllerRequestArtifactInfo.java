/*
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
 */
package org.ofbiz.webtools.artifactinfo;

import java.net.URL;
import java.util.Map;
import java.util.Set;

import javolution.util.FastSet;

import org.ofbiz.base.util.UtilObject;

/**
 *
 */
public class ControllerRequestArtifactInfo extends ArtifactInfoBase {
    
    protected URL controllerXmlUrl;
    protected String requestUri;
    
    protected Map<String, String> requestInfoMap;
    
    protected Set<ServiceArtifactInfo> servicesCalledByRequest = FastSet.newInstance();
    protected Set<ControllerRequestArtifactInfo> requestsThatAreResponsesToThisRequest = FastSet.newInstance();
    protected Set<ControllerViewArtifactInfo> viewsThatAreResponsesToThisRequest = FastSet.newInstance();
    
    public ControllerRequestArtifactInfo(URL controllerXmlUrl, String requestUri, ArtifactInfoFactory aif) {
        super(aif);
        this.controllerXmlUrl = controllerXmlUrl;
        this.requestUri = requestUri;
        
        this.requestInfoMap = aif.getControllerRequestInfoMap(controllerXmlUrl, requestUri);
        
        // TODO populate servicesCalledByRequest, requestsThatAreResponsesToThisRequest, viewsThatAreResponsesToThisRequest
        
        // TODO populate reverse Set for getRequestsThatThisRequestIsResponsTo, View.getRequestsThatThisViewIsResponseTo
    }
    
    public URL getControllerXmlUrl() {
        return this.controllerXmlUrl;
    }
    
    public String getRequestUri() {
        return this.requestUri;
    }
    
    public String getUniqueId() {
        return this.controllerXmlUrl.toExternalForm() + "#" + this.requestUri;
    }
    
    public boolean equals(Object obj) {
        if (obj instanceof ControllerRequestArtifactInfo) {
            ControllerRequestArtifactInfo that = (ControllerRequestArtifactInfo) obj;
            return UtilObject.equalsHelper(this.controllerXmlUrl, that.controllerXmlUrl) &&
                UtilObject.equalsHelper(this.requestUri, that.requestUri);
        } else {
            return false;
        }
    }
    
    /** Get the Services that are called by this Request */
    public Set<ServiceArtifactInfo> getServicesCalledByRequest() {
        return servicesCalledByRequest;
    }
    
    public Set<ControllerRequestArtifactInfo> getRequestsThatAreResponsesToThisRequest() {
        return this.requestsThatAreResponsesToThisRequest;
    }
    
    public Set<ControllerRequestArtifactInfo> getRequestsThatThisRequestIsResponsTo() {
        return this.aif.allRequestInfosReferringToRequest.get(this.getUniqueId());
    }
    
    public Set<ControllerViewArtifactInfo> getViewsThatAreResponsesToThisRequest() {
        return this.viewsThatAreResponsesToThisRequest;
    }
}
