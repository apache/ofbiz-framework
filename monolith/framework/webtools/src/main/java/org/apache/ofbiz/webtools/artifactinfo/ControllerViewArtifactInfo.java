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
import java.util.Objects;
import java.util.Set;

import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilURL;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.webapp.control.ConfigXMLReader;

/**
 *
 */
public class ControllerViewArtifactInfo extends ArtifactInfoBase {
    private static final String MODULE = ControllerViewArtifactInfo.class.getName();

    private URL controllerXmlUrl;
    private String viewUri;
    private ConfigXMLReader.ViewMap viewInfoMap;
    private ScreenWidgetArtifactInfo screenCalledByThisView = null;

    public ControllerViewArtifactInfo(URL controllerXmlUrl, String viewUri, ArtifactInfoFactory aif) throws GeneralException {
        super(aif);
        this.controllerXmlUrl = controllerXmlUrl;
        this.viewUri = viewUri;

        this.viewInfoMap = aif.getControllerViewMap(controllerXmlUrl, viewUri);

        if (this.viewInfoMap == null) {
            throw new GeneralException("Could not find Controller View [" + viewUri + "] at URL [" + controllerXmlUrl.toExternalForm() + "]");
        }

        if (this.viewInfoMap == null) {
            throw new GeneralException("Controller view with name [" + viewUri + "] is not defined in controller file [" + controllerXmlUrl + "].");
        }
        // populate screenCalledByThisView and reverse in aif.allViewInfosReferringToScreen
        if ("screen".equals(this.viewInfoMap.getType()) || "screenfop".equals(this.viewInfoMap.getType())
                || "screentext".equals(this.viewInfoMap.getType()) || "screenxml".equals(this.viewInfoMap.getType())) {
            String fullScreenName = this.viewInfoMap.getPage();
            if (UtilValidate.isNotEmpty(fullScreenName)) {
                int poundIndex = fullScreenName.indexOf('#');
                this.screenCalledByThisView = this.getAif().getScreenWidgetArtifactInfo(fullScreenName.substring(poundIndex + 1),
                        fullScreenName.substring(0, poundIndex));
                if (this.screenCalledByThisView != null) {
                    // add the reverse association
                    UtilMisc.addToSortedSetInMap(this, aif.getAllViewInfosReferringToScreen(), this.screenCalledByThisView.getUniqueId());
                }
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
     * Gets view uri.
     * @return the view uri
     */
    public String getViewUri() {
        return this.viewUri;
    }

    @Override
    public String getDisplayName() {
        String location = UtilURL.getOfbizHomeRelativeLocation(this.controllerXmlUrl);
        if (location.endsWith("/WEB-INF/controller.xml")) {
            location = location.substring(0, location.length() - 23);
        }
        return this.viewUri + " (" + location + ")";
    }

    @Override
    public String getDisplayType() {
        return "Controller View";
    }

    @Override
    public String getType() {
        return ArtifactInfoFactory.CONTROLLER_VIEW_INFO_TYPE_ID;
    }

    @Override
    public String getUniqueId() {
        return this.controllerXmlUrl.toExternalForm() + "#" + this.viewUri;
    }

    @Override
    public URL getLocationURL() throws MalformedURLException {
        return this.controllerXmlUrl;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ControllerViewArtifactInfo) {
            ControllerViewArtifactInfo that = (ControllerViewArtifactInfo) obj;
            return Objects.equals(this.controllerXmlUrl, that.controllerXmlUrl)
                    && Objects.equals(this.viewUri, that.viewUri);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * Gets requests that this view is response to.
     * @return the requests that this view is response to
     */
    public Set<ControllerRequestArtifactInfo> getRequestsThatThisViewIsResponseTo() {
        return this.getAif().getAllRequestInfosReferringToView().get(this.getUniqueId());
    }

    /**
     * Gets screen called by this view.
     * @return the screen called by this view
     */
    public ScreenWidgetArtifactInfo getScreenCalledByThisView() {
        return screenCalledByThisView;
    }
}
