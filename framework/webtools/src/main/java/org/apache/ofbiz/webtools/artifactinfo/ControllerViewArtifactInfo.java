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
    public static final String module = ControllerViewArtifactInfo.class.getName();

    protected URL controllerXmlUrl;
    protected String viewUri;

    protected ConfigXMLReader.ViewMap viewInfoMap;

    protected ScreenWidgetArtifactInfo screenCalledByThisView = null;

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
        if ("screen".equals(this.viewInfoMap.type) || "screenfop".equals(this.viewInfoMap.type) ||
                "screentext".equals(this.viewInfoMap.type) || "screenxml".equals(this.viewInfoMap.type)) {
            String fullScreenName = this.viewInfoMap.page;
            if (UtilValidate.isNotEmpty(fullScreenName)) {
                int poundIndex = fullScreenName.indexOf('#');
                this.screenCalledByThisView = this.aif.getScreenWidgetArtifactInfo(fullScreenName.substring(poundIndex+1), fullScreenName.substring(0, poundIndex));
                if (this.screenCalledByThisView != null) {
                    // add the reverse association
                    UtilMisc.addToSortedSetInMap(this, aif.allViewInfosReferringToScreen, this.screenCalledByThisView.getUniqueId());
                }
            }
        }
    }

    public URL getControllerXmlUrl() {
        return this.controllerXmlUrl;
    }

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
        return ArtifactInfoFactory.ControllerViewInfoTypeId;
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
            return Objects.equals(this.controllerXmlUrl, that.controllerXmlUrl) &&
                Objects.equals(this.viewUri, that.viewUri);
        } else {
            return false;
        }
    }

    public Set<ControllerRequestArtifactInfo> getRequestsThatThisViewIsResponseTo() {
        return this.aif.allRequestInfosReferringToView.get(this.getUniqueId());
    }

    public ScreenWidgetArtifactInfo getScreenCalledByThisView() {
        return screenCalledByThisView;
    }
}
