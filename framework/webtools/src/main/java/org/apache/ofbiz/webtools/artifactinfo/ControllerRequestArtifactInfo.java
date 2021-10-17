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
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilURL;
import org.apache.ofbiz.webapp.control.ConfigXMLReader;

/**
 * The type Controller request artifact info.
 */
public class ControllerRequestArtifactInfo extends ArtifactInfoBase {
    private static final String MODULE = ControllerRequestArtifactInfo.class.getName();

    private URL controllerXmlUrl;
    private String requestUri;
    private ConfigXMLReader.RequestMap requestInfoMap;
    private ServiceArtifactInfo serviceCalledByRequestEvent = null;
    private Set<ControllerRequestArtifactInfo> requestsThatAreResponsesToThisRequest = new TreeSet<>();
    private Set<ControllerViewArtifactInfo> viewsThatAreResponsesToThisRequest = new TreeSet<>();

    public ControllerRequestArtifactInfo(URL controllerXmlUrl, String requestUri, ArtifactInfoFactory aif) throws GeneralException {
        super(aif);
        this.controllerXmlUrl = controllerXmlUrl;
        this.requestUri = requestUri;

        this.requestInfoMap = aif.getControllerRequestMap(controllerXmlUrl, requestUri);

        if (this.requestInfoMap == null) {
            throw new GeneralException("Controller request with name [" + requestUri + "] is not defined in controller file ["
                    + controllerXmlUrl + "].");
        }
    }

    /** note this is mean to be called after the object is created and added to the ArtifactInfoFactory.allControllerRequestInfos in
     * ArtifactInfoFactory.getControllerRequestArtifactInfo */
    public void populateAll() throws GeneralException {
        // populate serviceCalledByRequestEvent, requestsThatAreResponsesToThisRequest, viewsThatAreResponsesToThisRequest and related reverse maps

        if (this.requestInfoMap.getEvent() != null && this.requestInfoMap.getEvent().getType() != null
                && (this.requestInfoMap.getEvent().getType().indexOf("service") >= 0)) {
            String serviceName = this.requestInfoMap.getEvent().getInvoke();
            this.serviceCalledByRequestEvent = this.getAif().getServiceArtifactInfo(serviceName);
            if (this.serviceCalledByRequestEvent != null) {
                // add the reverse association
                UtilMisc.addToSortedSetInMap(this, getAif().getAllRequestInfosReferringToServiceName(),
                        this.serviceCalledByRequestEvent.getUniqueId());
            }
        }

        Map<String, ConfigXMLReader.RequestResponse> requestResponseMap = UtilGenerics.cast(this.requestInfoMap.getRequestResponseMap());
        for (ConfigXMLReader.RequestResponse response: requestResponseMap.values()) {
            if ("view".equals(response.getType())) {
                String viewUri = response.getValue();
                if (viewUri.startsWith("/")) {
                    viewUri = viewUri.substring(1);
                }
                try {
                    ControllerViewArtifactInfo artInfo = this.getAif().getControllerViewArtifactInfo(controllerXmlUrl, viewUri);
                    this.viewsThatAreResponsesToThisRequest.add(artInfo);
                    // add the reverse association
                    UtilMisc.addToSortedSetInMap(this, this.getAif().getAllRequestInfosReferringToView(), artInfo.getUniqueId());
                } catch (GeneralException e) {
                    Debug.logWarning(e.toString(), MODULE);
                }
            } else if ("request".equals(response.getType())) {
                String otherRequestUri = response.getValue();
                if (otherRequestUri.startsWith("/")) {
                    otherRequestUri = otherRequestUri.substring(1);
                }
                try {
                    ControllerRequestArtifactInfo artInfo = this.getAif().getControllerRequestArtifactInfo(controllerXmlUrl, otherRequestUri);
                    this.requestsThatAreResponsesToThisRequest.add(artInfo);
                    UtilMisc.addToSortedSetInMap(this, this.getAif().getAllRequestInfosReferringToRequest(), artInfo.getUniqueId());
                } catch (GeneralException e) {
                    Debug.logWarning(e.toString(), MODULE);
                }
            } else if ("request-redirect".equals(response.getType())) {
                String otherRequestUri = response.getValue();
                ControllerRequestArtifactInfo artInfo = this.getAif().getControllerRequestArtifactInfo(controllerXmlUrl, otherRequestUri);
                this.requestsThatAreResponsesToThisRequest.add(artInfo);
                UtilMisc.addToSortedSetInMap(this, this.getAif().getAllRequestInfosReferringToRequest(), artInfo.getUniqueId());
            } else if ("request-redirect-noparam".equals(response.getType())) {
                String otherRequestUri = response.getValue();
                ControllerRequestArtifactInfo artInfo = this.getAif().getControllerRequestArtifactInfo(controllerXmlUrl, otherRequestUri);
                this.requestsThatAreResponsesToThisRequest.add(artInfo);
                UtilMisc.addToSortedSetInMap(this, this.getAif().getAllRequestInfosReferringToRequest(), artInfo.getUniqueId());
            }
        }
    }

    /**
     * Gets controller xml url.
     * @return the controller xml url
     */
    public URL getControllerXmlUrl() {
        return this.controllerXmlUrl;
    }

    /**
     * Gets request uri.
     * @return the request uri
     */
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
        return ArtifactInfoFactory.CONTROLLER_REQ_INFO_TYPE_ID;
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
            return Objects.equals(this.controllerXmlUrl, that.controllerXmlUrl) && Objects.equals(this.requestUri, that.requestUri);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /** Get the Services that are called by this Request */
    public ServiceArtifactInfo getServiceCalledByRequestEvent() {
        return serviceCalledByRequestEvent;
    }

    /**
     * Gets form infos referring to request.
     * @return the form infos referring to request
     */
    public Set<FormWidgetArtifactInfo> getFormInfosReferringToRequest() {
        return this.getAif().getAllFormInfosReferringToRequest().get(this.getUniqueId());
    }

    /**
     * Gets form infos targeting request.
     * @return the form infos targeting request
     */
    public Set<FormWidgetArtifactInfo> getFormInfosTargetingRequest() {
        return this.getAif().getAllFormInfosTargetingRequest().get(this.getUniqueId());
    }

    /**
     * Gets screen infos referring to request.
     * @return the screen infos referring to request
     */
    public Set<ScreenWidgetArtifactInfo> getScreenInfosReferringToRequest() {
        return this.getAif().getAllScreenInfosReferringToRequest().get(this.getUniqueId());
    }

    /**
     * Gets requests that are responses to this request.
     * @return the requests that are responses to this request
     */
    public Set<ControllerRequestArtifactInfo> getRequestsThatAreResponsesToThisRequest() {
        return this.requestsThatAreResponsesToThisRequest;
    }

    /**
     * Gets requests that this request is respons to.
     * @return the requests that this request is respons to
     */
    public Set<ControllerRequestArtifactInfo> getRequestsThatThisRequestIsResponsTo() {
        return this.getAif().getAllRequestInfosReferringToRequest().get(this.getUniqueId());
    }

    /**
     * Gets views that are responses to this request.
     * @return the views that are responses to this request
     */
    public Set<ControllerViewArtifactInfo> getViewsThatAreResponsesToThisRequest() {
        return this.viewsThatAreResponsesToThisRequest;
    }
}
