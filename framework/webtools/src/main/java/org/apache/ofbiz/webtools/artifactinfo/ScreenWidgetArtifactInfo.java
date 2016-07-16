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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.ofbiz.base.location.FlexibleLocation;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilURL;
import org.apache.ofbiz.widget.artifact.ArtifactInfoContext;
import org.apache.ofbiz.widget.artifact.ArtifactInfoGatherer;
import org.apache.ofbiz.widget.model.ModelScreen;
import org.xml.sax.SAXException;

/**
 *
 */
public class ScreenWidgetArtifactInfo extends ArtifactInfoBase {
    public static final String module = ScreenWidgetArtifactInfo.class.getName();

    protected ModelScreen modelScreen;

    protected String screenName;
    protected String screenLocation;

    protected Set<EntityArtifactInfo> entitiesUsedInThisScreen = new TreeSet<EntityArtifactInfo>();
    protected Set<ServiceArtifactInfo> servicesUsedInThisScreen = new TreeSet<ServiceArtifactInfo>();
    protected Set<FormWidgetArtifactInfo> formsIncludedInThisScreen = new TreeSet<FormWidgetArtifactInfo>();
    protected Set<ControllerRequestArtifactInfo> requestsLinkedToInScreen = new TreeSet<ControllerRequestArtifactInfo>();

    public ScreenWidgetArtifactInfo(String screenName, String screenLocation, ArtifactInfoFactory aif) throws GeneralException {
        super(aif);
        this.screenName = screenName;
        this.screenLocation = screenLocation;
        try {
            this.modelScreen = aif.getModelScreen(screenName, screenLocation);
        } catch (IllegalArgumentException e) {
            throw new GeneralException(e);
        } catch (ParserConfigurationException e) {
            throw new GeneralException(e);
        } catch (SAXException e) {
            throw new GeneralException(e);
        } catch (IOException e) {
            throw new GeneralException(e);
        }

    }

    public void populateAll() throws GeneralException {
        ArtifactInfoContext infoContext = new ArtifactInfoContext();
        ArtifactInfoGatherer infoGatherer = new ArtifactInfoGatherer(infoContext);
        try {
            infoGatherer.visit(this.modelScreen);
        } catch (Exception e) {
            throw new GeneralException(e);
        }
        populateServicesFromNameSet(infoContext.getServiceNames());
        populateEntitiesFromNameSet(infoContext.getEntityNames());
        populateFormsFromNameSet(infoContext.getFormLocations());
        populateLinkedRequests(infoContext.getRequestLocations());
    }

    protected void populateServicesFromNameSet(Set<String> allServiceNameSet) throws GeneralException {
        for (String serviceName: allServiceNameSet) {
            if (serviceName.contains("${")) {
                continue;
            }
            try {
                aif.getModelService(serviceName);
            } catch (GeneralException e) {
                Debug.logWarning("Service [" + serviceName + "] reference in screen [" + this.screenName + "] in resource [" + this.screenLocation + "] does not exist!", module);
                continue;
            }

            // the forward reference
            this.servicesUsedInThisScreen.add(aif.getServiceArtifactInfo(serviceName));
            // the reverse reference
            UtilMisc.addToSortedSetInMap(this, aif.allScreenInfosReferringToServiceName, serviceName);
        }
    }

    protected void populateEntitiesFromNameSet(Set<String> allEntityNameSet) throws GeneralException {
        for (String entityName: allEntityNameSet) {
            if (entityName.contains("${")) {
                continue;
            }
            // attempt to convert relation names to entity names
            entityName = aif.getEntityModelReader().validateEntityName(entityName);
            if (entityName == null) {
                Debug.logWarning("Entity [" + entityName + "] reference in screen [" + this.screenName + "] in resource [" + this.screenLocation + "] does not exist!", module);
                continue;
            }

            // the forward reference
            this.entitiesUsedInThisScreen.add(aif.getEntityArtifactInfo(entityName));
            // the reverse reference
            UtilMisc.addToSortedSetInMap(this, aif.allScreenInfosReferringToEntityName, entityName);
        }
    }

    protected void populateFormsFromNameSet(Set<String> allFormNameSet) throws GeneralException {
        for (String formName: allFormNameSet) {
            if (formName.contains("${")) {
                continue;
            }

            try {
                aif.getModelForm(formName);
            } catch (Exception e) {
                Debug.logWarning("Form [" + formName + "] reference in screen [" + this.screenName + "] in resource [" + this.screenLocation + "] does not exist!", module);
                continue;
            }

            // the forward reference
            this.formsIncludedInThisScreen.add(aif.getFormWidgetArtifactInfo(formName));
            // the reverse reference
            UtilMisc.addToSortedSetInMap(this, aif.allScreenInfosReferringToForm, formName);
        }
    }

    protected void populateLinkedRequests(Set<String> allRequestUniqueId) throws GeneralException{

        for (String requestUniqueId: allRequestUniqueId) {
            if (requestUniqueId.contains("${")) {
                continue;
            }

            if (requestUniqueId.indexOf("#") > -1) {
                String controllerXmlUrl = requestUniqueId.substring(0, requestUniqueId.indexOf("#"));
                String requestUri = requestUniqueId.substring(requestUniqueId.indexOf("#") + 1);
                // the forward reference
                this.requestsLinkedToInScreen.add(aif.getControllerRequestArtifactInfo(UtilURL.fromUrlString(controllerXmlUrl), requestUri));
                // the reverse reference
                UtilMisc.addToSortedSetInMap(this, aif.allScreenInfosReferringToRequest, requestUniqueId);
            }
        }
    }

    @Override
    public String getDisplayName() {
        // remove the component:// from the location
        return this.screenName + " (" + this.screenLocation.substring(12) + ")";
    }

    @Override
    public String getDisplayType() {
        return "Screen Widget";
    }

    @Override
    public String getType() {
        return ArtifactInfoFactory.ScreenWidgetInfoTypeId;
    }

    @Override
    public String getUniqueId() {
        return this.screenLocation + "#" + this.screenName;
    }

    @Override
    public URL getLocationURL() throws MalformedURLException {
        return FlexibleLocation.resolveLocation(this.screenLocation, null);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ScreenWidgetArtifactInfo) {
            return (this.modelScreen.getName().equals(((ScreenWidgetArtifactInfo) obj).modelScreen.getName()) &&
                    this.modelScreen.getSourceLocation().equals(((ScreenWidgetArtifactInfo) obj).modelScreen.getSourceLocation()));
        } else {
            return false;
        }
    }

    public Set<ControllerViewArtifactInfo> getViewsReferringToScreen() {
        return this.aif.allViewInfosReferringToScreen.get(this.getUniqueId());
    }

    public Set<EntityArtifactInfo> getEntitiesUsedInScreen() {
        return this.entitiesUsedInThisScreen;
    }

    public Set<ServiceArtifactInfo> getServicesUsedInScreen() {
        return this.servicesUsedInThisScreen;
    }

    public Set<FormWidgetArtifactInfo> getFormsIncludedInScreen() {
        return this.formsIncludedInThisScreen;
    }

    public Set<ScreenWidgetArtifactInfo> getScreensIncludedInScreen() {
        // TODO: implement this
        return new HashSet<ScreenWidgetArtifactInfo>();
    }

    public Set<ScreenWidgetArtifactInfo> getScreensIncludingThisScreen() {
        return this.aif.allScreenInfosReferringToScreen.get(this.getUniqueId());
    }

    public Set<ControllerRequestArtifactInfo> getRequestsLinkedToInScreen() {
        return this.requestsLinkedToInScreen;
    }
}
