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
package org.apache.ofbiz.webtools.artifactinfo;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilObject;
import org.apache.ofbiz.base.util.UtilURL;
import org.apache.ofbiz.webapp.control.ConfigXMLReader;

/**
 *
 */
public class ControllerRequestArtifactInfo extends ArtifactInfoBase {
    public static final String module = ControllerRequestArtifactInfo.class.getName();

    protected URL controllerXmlUrl;
    protected String requestUri;

    protected ConfigXMLReader.RequestMap requestInfoMap;

    protected ServiceArtifactInfo serviceCalledByRequestEvent = null;
    protected Set<ControllerRequestArtifactInfo> requestsThatAreResponsesToThisRequest = new TreeSet<ControllerRequestArtifactInfo>();
    protected Set<ControllerViewArtifactInfo> viewsThatAreResponsesToThisRequest = new TreeSet<ControllerViewArtifactInfo>();

    public ControllerRequestArtifactInfo(URL controllerXmlUrl, String requestUri, ArtifactInfoFactory aif) throws GeneralException {
        super(aif);
        this.controllerXmlUrl = controllerXmlUrl;
        this.requestUri = requestUri;

        this.requestInfoMap = aif.getControllerRequestMap(controllerXmlUrl, requestUri);

        if (this.requestInfoMap == null) {
            throw new GeneralException("Controller request with name [" + requestUri + "] is not defined in controller file [" + controllerXmlUrl + "].");
        }
    }

    /** note this is mean to be called after the object is created and added to the ArtifactInfoFactory.allControllerRequestInfos in ArtifactInfoFactory.getControllerRequestArtifactInfo */
    public void populateAll() throws GeneralException {
        // populate serviceCalledByRequestEvent, requestsThatAreResponsesToThisRequest, viewsThatAreResponsesToThisRequest and related reverse maps

        if (this.requestInfoMap.event != null && this.requestInfoMap.event.type != null && (this.requestInfoMap.event.type.indexOf("service") >= 0)) {
            String serviceName = this.requestInfoMap.event.invoke;
            this.serviceCalledByRequestEvent = this.aif.getServiceArtifactInfo(serviceName);
            if (this.serviceCalledByRequestEvent != null) {
                // add the reverse association
                UtilMisc.addToSortedSetInMap(this, aif.allRequestInfosReferringToServiceName, this.serviceCalledByRequestEvent.getUniqueId());
            }
        }

        Map<String, ConfigXMLReader.RequestResponse> requestResponseMap = UtilGenerics.checkMap(this.requestInfoMap.requestResponseMap);
        for (ConfigXMLReader.RequestResponse response: requestResponseMap.values()) {
            if ("view".equals(response.type)) {
                String viewUri = response.value;
                if (viewUri.startsWith("/")) {
                    viewUri = viewUri.substring(1);
                }
                try {
                    ControllerViewArtifactInfo artInfo = this.aif.getControllerViewArtifactInfo(controllerXmlUrl, viewUri);
                    this.viewsThatAreResponsesToThisRequest.add(artInfo);
                    // add the reverse association
                    UtilMisc.addToSortedSetInMap(this, this.aif.allRequestInfosReferringToView, artInfo.getUniqueId());
                } catch (GeneralException e) {
                    Debug.logWarning(e.toString(), module);
                }
            } else if (response.type.equals("request")) {
                String otherRequestUri = response.value;
                if (otherRequestUri.startsWith("/")) {
                    otherRequestUri = otherRequestUri.substring(1);
                }
                try {
                    ControllerRequestArtifactInfo artInfo = this.aif.getControllerRequestArtifactInfo(controllerXmlUrl, otherRequestUri);
                    this.requestsThatAreResponsesToThisRequest.add(artInfo);
                    UtilMisc.addToSortedSetInMap(this, this.aif.allRequestInfosReferringToRequest, artInfo.getUniqueId());
                } catch (GeneralException e) {
                    Debug.logWarning(e.toString(), module);
                }
            } else if (response.type.equals("request-redirect")) {
                String otherRequestUri = response.value;
                ControllerRequestArtifactInfo artInfo = this.aif.getControllerRequestArtifactInfo(controllerXmlUrl, otherRequestUri);
                this.requestsThatAreResponsesToThisRequest.add(artInfo);
                UtilMisc.addToSortedSetInMap(this, this.aif.allRequestInfosReferringToRequest, artInfo.getUniqueId());
            } else if (response.type.equals("request-redirect-noparam")) {
                String otherRequestUri = response.value;
                ControllerRequestArtifactInfo artInfo = this.aif.getControllerRequestArtifactInfo(controllerXmlUrl, otherRequestUri);
                this.requestsThatAreResponsesToThisRequest.add(artInfo);
                UtilMisc.addToSortedSetInMap(this, this.aif.allRequestInfosReferringToRequest, artInfo.getUniqueId());
            }
        }
    }

    public URL getControllerXmlUrl() {
        return this.controllerXmlUrl;
    }

    public String getRequestUri() {
        return this.requestUri;
    }

    @Override
    public String getDisplayName() {
        String location = UtilURL.getOfbizHomeRelativeLocation(this.controllerXmlUrl);
        if (location.endsWith("/WEB-INF/controller.xml")) {
            location = location.substring(0, location.length() - 23);
        }
        return this.requestUri + " (" + location + ")";
    }

    @Override
    public String getDisplayType() {
        return "Controller Request";
    }

    @Override
    public String getType() {
        return ArtifactInfoFactory.ControllerRequestInfoTypeId;
    }

    @Override
    public String getUniqueId() {
        return this.controllerXmlUrl.toExternalForm() + "#" + this.requestUri;
    }

    @Override
    public URL getLocationURL() throws MalformedURLException {
        return this.controllerXmlUrl;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ControllerRequestArtifactInfo) {
            ControllerRequestArtifactInfo that = (ControllerRequestArtifactInfo) obj;
            return UtilObject.equalsHelper(this.controllerXmlUrl, that.controllerXmlUrl) && UtilObject.equalsHelper(this.requestUri, that.requestUri);
        } else {
            return false;
        }
    }

    /** Get the Services that are called by this Request */
    public ServiceArtifactInfo getServiceCalledByRequestEvent() {
        return serviceCalledByRequestEvent;
    }

    public Set<FormWidgetArtifactInfo> getFormInfosReferringToRequest() {
        return this.aif.allFormInfosReferringToRequest.get(this.getUniqueId());
    }

    public Set<FormWidgetArtifactInfo> getFormInfosTargetingRequest() {
        return this.aif.allFormInfosTargetingRequest.get(this.getUniqueId());
    }

    public Set<ScreenWidgetArtifactInfo> getScreenInfosReferringToRequest() {
        return this.aif.allScreenInfosReferringToRequest.get(this.getUniqueId());
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
