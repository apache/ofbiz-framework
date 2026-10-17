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
import org.apache.ofbiz.widget.model.ModelForm;
import org.apache.ofbiz.widget.model.ModelGrid;
import org.apache.ofbiz.widget.model.ModelSingleForm;
import org.xml.sax.SAXException;

/**
 *
 */
public class FormWidgetArtifactInfo extends ArtifactInfoBase {
    private static final String MODULE = FormWidgetArtifactInfo.class.getName();

    private ModelForm modelForm;

    private String formName;
    private String formLocation;

    private Set<EntityArtifactInfo> entitiesUsedInThisForm = new TreeSet<>();
    private Set<ServiceArtifactInfo> servicesUsedInThisForm = new TreeSet<>();
    private FormWidgetArtifactInfo formThisFormExtends = null;
    private Set<ControllerRequestArtifactInfo> requestsLinkedToInForm = new TreeSet<>();
    private Set<ControllerRequestArtifactInfo> requestsTargetedByInForm = new TreeSet<>();

    public FormWidgetArtifactInfo(String formName, String formLocation, ArtifactInfoFactory aif) throws GeneralException {
        super(aif);
        this.formName = formName;
        this.formLocation = formLocation;
        try {
            this.modelForm = aif.getModelForm(formName, formLocation);
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new GeneralException(e);
        }
    }

    /** note this is mean to be called after the object is created and added to the ArtifactInfoFactory.allFormInfos in
     * ArtifactInfoFactory.getFormWidgetArtifactInfo */
    public void populateAll() throws GeneralException {
        ArtifactInfoContext infoContext = new ArtifactInfoContext();
        ArtifactInfoGatherer infoGatherer = new ArtifactInfoGatherer(infoContext);
        try {
            if (this.modelForm instanceof ModelSingleForm) {
                infoGatherer.visit((ModelSingleForm) this.modelForm);
            } else {
                infoGatherer.visit((ModelGrid) this.modelForm);
            }
        } catch (Exception e) {
            throw new GeneralException(e);
        }
        populateEntitiesFromNameSet(infoContext.getEntityNames());
        populateServicesFromNameSet(infoContext.getServiceNames());
        this.populateFormExtended();
        this.populateLinkedRequests(infoContext.getRequestLocations());
        this.populateTargetedRequests(infoContext.getTargetLocations());
    }

    /**
     * Populate form extended.
     * @throws GeneralException the general exception
     */
    protected void populateFormExtended() throws GeneralException {
        // populate formThisFormExtends and the reverse-associate cache in the aif
        if (this.modelForm.getParentFormName() != null) {
            String formName = this.modelForm.getParentFormLocation() + "#" + this.modelForm.getParentFormName();
            if (formName.contains("${")) {
                return;
            }

            try {
                getAif().getModelForm(formName);
            } catch (Exception e) {
                Debug.logWarning("Form [" + formName + "] reference in form [" + this.formName + "] in resource [" + this.formLocation
                        + "] does not exist!", MODULE);
                return;
            }

            // the forward reference
            this.formThisFormExtends = getAif().getFormWidgetArtifactInfo(formName);
            // the reverse reference
            UtilMisc.addToSortedSetInMap(this, getAif().getAllFormInfosExtendingForm(), formName);
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
            if (!getAif().getEntityModelReader().getEntityNames().contains(entityName)) {
                Debug.logWarning("Entity [" + entityName + "] reference in form [" + this.formName + "] in resource [" + this.formLocation
                        + "] does not exist!", MODULE);
                continue;
            }

            // the forward reference
            this.entitiesUsedInThisForm.add(getAif().getEntityArtifactInfo(entityName));
            // the reverse reference
            UtilMisc.addToSortedSetInMap(this, getAif().getAllFormInfosReferringToEntityName(), entityName);
        }
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
                Debug.logWarning("Service [" + serviceName + "] reference in form [" + this.formName + "] in resource [" + this.formLocation
                        + "] does not exist!", MODULE);
                continue;
            }

            // the forward reference
            this.servicesUsedInThisForm.add(getAif().getServiceArtifactInfo(serviceName));
            // the reverse reference
            UtilMisc.addToSortedSetInMap(this, getAif().getAllFormInfosReferringToServiceName(), serviceName);
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
                this.requestsLinkedToInForm.add(getAif().getControllerRequestArtifactInfo(UtilURL.fromUrlString(controllerXmlUrl), requestUri));
                // the reverse reference
                UtilMisc.addToSortedSetInMap(this, getAif().getAllFormInfosReferringToRequest(), requestUniqueId);
            }
        }
    }

    /**
     * Populate targeted requests.
     * @param allRequestUniqueId the all request unique id
     * @throws GeneralException the general exception
     */
    protected void populateTargetedRequests(Set<String> allRequestUniqueId) throws GeneralException {

        for (String requestUniqueId: allRequestUniqueId) {
            if (requestUniqueId.contains("${")) {
                continue;
            }

            if (requestUniqueId.indexOf("#") > -1) {
                String controllerXmlUrl = requestUniqueId.substring(0, requestUniqueId.indexOf("#"));
                String requestUri = requestUniqueId.substring(requestUniqueId.indexOf("#") + 1);
                // the forward reference
                this.requestsTargetedByInForm.add(getAif().getControllerRequestArtifactInfo(UtilURL.fromUrlString(controllerXmlUrl), requestUri));
                // the reverse reference
                UtilMisc.addToSortedSetInMap(this, getAif().getAllFormInfosTargetingRequest(), requestUniqueId);
            }
        }
    }

    @Override
    public String getDisplayName() {
        // remove the component:// from the location
        return this.formName + " (" + this.formLocation.substring(12) + ")";
    }

    @Override
    public String getDisplayType() {
        return "Form Widget";
    }

    @Override
    public String getType() {
        return ArtifactInfoFactory.FORM_WIDGET_INFO_TYPE_ID;
    }

    @Override
    public String getUniqueId() {
        return this.formLocation + "#" + this.formName;
    }

    @Override
    public URL getLocationURL() throws MalformedURLException {
        return FlexibleLocation.resolveLocation(formLocation);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FormWidgetArtifactInfo) {
            return (this.modelForm.getName().equals(((FormWidgetArtifactInfo) obj).modelForm.getName())
                    && this.modelForm.getFormLocation().equals(((FormWidgetArtifactInfo) obj).modelForm.getFormLocation()));
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * Gets entities used in form.
     * @return the entities used in form
     */
    public Set<EntityArtifactInfo> getEntitiesUsedInForm() {
        return this.entitiesUsedInThisForm;
    }

    /**
     * Gets services used in form.
     * @return the services used in form
     */
    public Set<ServiceArtifactInfo> getServicesUsedInForm() {
        return this.servicesUsedInThisForm;
    }

    /**
     * Gets form this form extends.
     * @return the form this form extends
     */
    public FormWidgetArtifactInfo getFormThisFormExtends() {
        return this.formThisFormExtends;
    }

    /**
     * Gets forms extending this form.
     * @return the forms extending this form
     */
    public Set<FormWidgetArtifactInfo> getFormsExtendingThisForm() {
        return this.getAif().getAllFormInfosExtendingForm().get(this.getUniqueId());
    }

    /**
     * Gets screens including this form.
     * @return the screens including this form
     */
    public Set<ScreenWidgetArtifactInfo> getScreensIncludingThisForm() {
        return this.getAif().getAllScreenInfosReferringToForm().get(this.getUniqueId());
    }

    /**
     * Gets requests linked to in form.
     * @return the requests linked to in form
     */
    public Set<ControllerRequestArtifactInfo> getRequestsLinkedToInForm() {
        return this.requestsLinkedToInForm;
    }

    /**
     * Gets requests targeted by form.
     * @return the requests targeted by form
     */
    public Set<ControllerRequestArtifactInfo> getRequestsTargetedByForm() {
        return this.requestsTargetedByInForm;
    }
}
