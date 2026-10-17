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
 * The type Screen widget artifact info.
 */
public class ScreenWidgetArtifactInfo extends ArtifactInfoBase {
    private static final String MODULE = ScreenWidgetArtifactInfo.class.getName();

    private ModelScreen modelScreen;

    private String screenName;
    private String screenLocation;

    private Set<EntityArtifactInfo> entitiesUsedInThisScreen = new TreeSet<>();
    private Set<ServiceArtifactInfo> servicesUsedInThisScreen = new TreeSet<>();
    private Set<FormWidgetArtifactInfo> formsIncludedInThisScreen = new TreeSet<>();
    private Set<ControllerRequestArtifactInfo> requestsLinkedToInScreen = new TreeSet<>();

    public ScreenWidgetArtifactInfo(String screenName, String screenLocation, ArtifactInfoFactory aif) throws GeneralException {
        super(aif);
        this.screenName = screenName;
        this.screenLocation = screenLocation;
        try {
            this.modelScreen = aif.getModelScreen(screenName, screenLocation);
        } catch (IllegalArgumentException | IOException | SAXException | ParserConfigurationException e) {
            throw new GeneralException(e);
        }

    }

    /**
     * Populate all.
     * @throws GeneralException the general exception
     */
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

    /**
     * Populate services from name set.
     * @param allServiceNameSet the all service name set
     * @throws GeneralException the general exception
     */
    protected void populateServicesFromNameSet(Set<String> allServiceNameSet) throws GeneralException {
        for (String serviceName: allServiceNameSet) {
            if (serviceName.contains("${")) {
                continue;
            }
            try {
                getAif().getModelService(serviceName);
            } catch (GeneralException e) {
                Debug.logWarning("Service [" + serviceName + "] reference in screen [" + this.screenName + "] in resource [" + this.screenLocation
                        + "] does not exist!", MODULE);
                continue;
            }

            // the forward reference
            this.servicesUsedInThisScreen.add(getAif().getServiceArtifactInfo(serviceName));
            // the reverse reference
            UtilMisc.addToSortedSetInMap(this, getAif().getAllScreenInfosReferringToServiceName(), serviceName);
        }
    }

    /**
     * Populate entities from name set.
     * @param allEntityNameSet the all entity name set
     * @throws GeneralException the general exception
     */
    protected void populateEntitiesFromNameSet(Set<String> allEntityNameSet) throws GeneralException {
        for (String entityName: allEntityNameSet) {
            if (entityName.contains("${")) {
                continue;
            }
            // attempt to convert relation names to entity names
            entityName = getAif().getEntityModelReader().validateEntityName(entityName);
            if (entityName == null) {
                Debug.logWarning("Entity [" + entityName + "] reference in screen [" + this.screenName + "] in resource [" + this.screenLocation
                        + "] does not exist!", MODULE);
                continue;
            }

            // the forward reference
            this.entitiesUsedInThisScreen.add(getAif().getEntityArtifactInfo(entityName));
            // the reverse reference
            UtilMisc.addToSortedSetInMap(this, getAif().getAllScreenInfosReferringToEntityName(), entityName);
        }
    }

    /**
     * Populate forms from name set.
     * @param allFormNameSet the all form name set
     * @throws GeneralException the general exception
     */
    protected void populateFormsFromNameSet(Set<String> allFormNameSet) throws GeneralException {
        for (String formName: allFormNameSet) {
            if (formName.contains("${")) {
                continue;
            }

            try {
                getAif().getModelForm(formName);
            } catch (Exception e) {
                Debug.logWarning("Form [" + formName + "] reference in screen [" + this.screenName + "] in resource [" + this.screenLocation
                        + "] does not exist!", MODULE);
                continue;
            }

            // the forward reference
            this.formsIncludedInThisScreen.add(getAif().getFormWidgetArtifactInfo(formName));
            // the reverse reference
            UtilMisc.addToSortedSetInMap(this, getAif().getAllScreenInfosReferringToForm(), formName);
        }
    }

    /**
     * Populate linked requests.
     * @param allRequestUniqueId the all request unique id
     * @throws GeneralException the general exception
     */
    protected void populateLinkedRequests(Set<String> allRequestUniqueId) throws GeneralException {

        for (String requestUniqueId: allRequestUniqueId) {
            if (requestUniqueId.contains("${")) {
                continue;
            }

            if (requestUniqueId.indexOf("#") > -1) {
                String controllerXmlUrl = requestUniqueId.substring(0, requestUniqueId.indexOf("#"));
                String requestUri = requestUniqueId.substring(requestUniqueId.indexOf("#") + 1);
                // the forward reference
                this.requestsLinkedToInScreen.add(getAif().getControllerRequestArtifactInfo(UtilURL.fromUrlString(controllerXmlUrl), requestUri));
                // the reverse reference
                UtilMisc.addToSortedSetInMap(this, getAif().getAllScreenInfosReferringToRequest(), requestUniqueId);
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
        return ArtifactInfoFactory.SCREEN_WIDGET_INFO_TYPE_ID;
    }

    @Override
    public String getUniqueId() {
        return this.screenLocation + "#" + this.screenName;
    }

    @Override
    public URL getLocationURL() throws MalformedURLException {
        return FlexibleLocation.resolveLocation(screenLocation);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ScreenWidgetArtifactInfo) {
            return (this.modelScreen.getName().equals(((ScreenWidgetArtifactInfo) obj).modelScreen.getName())
                    && this.modelScreen.getSourceLocation().equals(((ScreenWidgetArtifactInfo) obj).modelScreen.getSourceLocation()));
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * Gets views referring to screen.
     *
     * @return the views referring to screen
     */
    public Set<ControllerViewArtifactInfo> getViewsReferringToScreen() {
        return this.getAif().getAllViewInfosReferringToScreen().get(this.getUniqueId());
    }

    /**
     * Gets entities used in screen.
     * @return the entities used in screen
     */
    public Set<EntityArtifactInfo> getEntitiesUsedInScreen() {
        return this.entitiesUsedInThisScreen;
    }

    /**
     * Gets services used in screen.
     * @return the services used in screen
     */
    public Set<ServiceArtifactInfo> getServicesUsedInScreen() {
        return this.servicesUsedInThisScreen;
    }

    /**
     * Gets forms included in screen.
     * @return the forms included in screen
     */
    public Set<FormWidgetArtifactInfo> getFormsIncludedInScreen() {
        return this.formsIncludedInThisScreen;
    }

    /**
     * Gets screens included in screen.
     * @return the screens included in screen
     */
    public Set<ScreenWidgetArtifactInfo> getScreensIncludedInScreen() {
        // TODO: implement this
        return new HashSet<>();
    }

    /**
     * Gets screens including this screen.
     * @return the screens including this screen
     */
    public Set<ScreenWidgetArtifactInfo> getScreensIncludingThisScreen() {
        return this.getAif().getAllScreenInfosReferringToScreen().get(this.getUniqueId());
    }

    /**
     * Gets requests linked to in screen.
     * @return the requests linked to in screen
     */
    public Set<ControllerRequestArtifactInfo> getRequestsLinkedToInScreen() {
        return this.requestsLinkedToInScreen;
    }
}
