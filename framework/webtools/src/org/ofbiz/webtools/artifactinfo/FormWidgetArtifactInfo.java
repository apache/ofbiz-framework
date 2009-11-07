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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.ParserConfigurationException;

import org.ofbiz.base.location.FlexibleLocation;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilURL;
import org.ofbiz.widget.form.ModelForm;
import org.xml.sax.SAXException;

/**
 *
 */
public class FormWidgetArtifactInfo extends ArtifactInfoBase {
    public static final String module = FormWidgetArtifactInfo.class.getName();

    protected ModelForm modelForm;

    protected String formName;
    protected String formLocation;

    protected Set<EntityArtifactInfo> entitiesUsedInThisForm = new TreeSet<EntityArtifactInfo>();
    protected Set<ServiceArtifactInfo> servicesUsedInThisForm = new TreeSet<ServiceArtifactInfo>();
    protected FormWidgetArtifactInfo formThisFormExtends = null;
    protected Set<ControllerRequestArtifactInfo> requestsLinkedToInForm = new TreeSet<ControllerRequestArtifactInfo>();
    protected Set<ControllerRequestArtifactInfo> requestsTargetedByInForm = new TreeSet<ControllerRequestArtifactInfo>();

    public FormWidgetArtifactInfo(String formName, String formLocation, ArtifactInfoFactory aif) throws GeneralException {
        super(aif);
        this.formName = formName;
        this.formLocation = formLocation;
        try {
            this.modelForm = aif.getModelForm(formName, formLocation);
        } catch (ParserConfigurationException e) {
            throw new GeneralException(e);
        } catch (SAXException e) {
            throw new GeneralException(e);
        } catch (IOException e) {
            throw new GeneralException(e);
        }
    }

    /** note this is mean to be called after the object is created and added to the ArtifactInfoFactory.allFormInfos in ArtifactInfoFactory.getFormWidgetArtifactInfo */
    public void populateAll() throws GeneralException {
        // populate entitiesUsedInThisForm, servicesUsedInThisForm, formThisFormExtends (and reverse in aif.allFormInfosExtendingForm)
        this.populateUsedEntities();
        this.populateUsedServices();
        this.populateFormExtended();
        this.populateLinkedRequests();
        this.populateTargetedRequests();
    }

    protected void populateFormExtended() throws GeneralException {
        // populate formThisFormExtends and the reverse-associate cache in the aif
        if (this.modelForm.getParentFormName() != null) {
            String formName = this.modelForm.getParentFormLocation() + "#" + this.modelForm.getParentFormName();
            if (formName.contains("${")) {
                return;
            }

            try {
                aif.getModelForm(formName);
            } catch (Exception e) {
                Debug.logWarning("Form [" + formName + "] reference in form [" + this.formName + "] in resource [" + this.formLocation + "] does not exist!", module);
                return;
            }

            // the forward reference
            this.formThisFormExtends = aif.getFormWidgetArtifactInfo(formName);
            // the reverse reference
            UtilMisc.addToSortedSetInMap(this, aif.allFormInfosExtendingForm, formName);
        }
    }
    protected void populateUsedEntities() throws GeneralException {
        // populate entitiesUsedInThisForm and for each the reverse-associate cache in the aif
        Set<String> allEntityNameSet = this.modelForm.getAllEntityNamesUsed();
        populateEntitiesFromNameSet(allEntityNameSet);
    }
    protected void populateEntitiesFromNameSet(Set<String> allEntityNameSet) throws GeneralException {
        for (String entityName: allEntityNameSet) {
            if (entityName.contains("${")) {
                continue;
            }
            if (!aif.getEntityModelReader().getEntityNames().contains(entityName)) {
                Debug.logWarning("Entity [" + entityName + "] reference in form [" + this.formName + "] in resource [" + this.formLocation + "] does not exist!", module);
                continue;
            }

            // the forward reference
            this.entitiesUsedInThisForm.add(aif.getEntityArtifactInfo(entityName));
            // the reverse reference
            UtilMisc.addToSortedSetInMap(this, aif.allFormInfosReferringToEntityName, entityName);
        }
    }
    protected void populateUsedServices() throws GeneralException {
        // populate servicesUsedInThisForm and for each the reverse-associate cache in the aif
        Set<String> allServiceNameSet = this.modelForm.getAllServiceNamesUsed();
        populateServicesFromNameSet(allServiceNameSet);
    }
    protected void populateServicesFromNameSet(Set<String> allServiceNameSet) throws GeneralException {
        for (String serviceName: allServiceNameSet) {
            if (serviceName.contains("${")) {
                continue;
            }
            try {
                aif.getModelService(serviceName);
            } catch (GeneralException e) {
                Debug.logWarning("Service [" + serviceName + "] reference in form [" + this.formName + "] in resource [" + this.formLocation + "] does not exist!", module);
                continue;
            }

            // the forward reference
            this.servicesUsedInThisForm.add(aif.getServiceArtifactInfo(serviceName));
            // the reverse reference
            UtilMisc.addToSortedSetInMap(this, aif.allFormInfosReferringToServiceName, serviceName);
        }
    }

    protected void populateLinkedRequests() throws GeneralException{
        Set<String> allRequestUniqueId = this.modelForm.getLinkedRequestsLocationAndUri();

        for (String requestUniqueId: allRequestUniqueId) {
            if (requestUniqueId.contains("${")) {
                continue;
            }

            if (requestUniqueId.indexOf("#") > -1) {
                String controllerXmlUrl = requestUniqueId.substring(0, requestUniqueId.indexOf("#"));
                String requestUri = requestUniqueId.substring(requestUniqueId.indexOf("#") + 1);
                // the forward reference
                this.requestsLinkedToInForm.add(aif.getControllerRequestArtifactInfo(UtilURL.fromUrlString(controllerXmlUrl), requestUri));
                // the reverse reference
                UtilMisc.addToSortedSetInMap(this, aif.allFormInfosReferringToRequest, requestUniqueId);
            }
        }
    }
    protected void populateTargetedRequests() throws GeneralException{
        Set<String> allRequestUniqueId = this.modelForm.getTargetedRequestsLocationAndUri();

        for (String requestUniqueId: allRequestUniqueId) {
            if (requestUniqueId.contains("${")) {
                continue;
            }

            if (requestUniqueId.indexOf("#") > -1) {
                String controllerXmlUrl = requestUniqueId.substring(0, requestUniqueId.indexOf("#"));
                String requestUri = requestUniqueId.substring(requestUniqueId.indexOf("#") + 1);
                // the forward reference
                this.requestsTargetedByInForm.add(aif.getControllerRequestArtifactInfo(UtilURL.fromUrlString(controllerXmlUrl), requestUri));
                // the reverse reference
                UtilMisc.addToSortedSetInMap(this, aif.allFormInfosTargetingRequest, requestUniqueId);
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
        return ArtifactInfoFactory.FormWidgetInfoTypeId;
    }

    @Override
    public String getUniqueId() {
        return this.formLocation + "#" + this.formName;
    }

    @Override
    public URL getLocationURL() throws MalformedURLException {
        return FlexibleLocation.resolveLocation(this.formLocation, null);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FormWidgetArtifactInfo) {
            return (this.modelForm.getName().equals(((FormWidgetArtifactInfo) obj).modelForm.getName()) &&
                    this.modelForm.getFormLocation().equals(((FormWidgetArtifactInfo) obj).modelForm.getFormLocation()));
        } else {
            return false;
        }
    }

    public Set<EntityArtifactInfo> getEntitiesUsedInForm() {
        return this.entitiesUsedInThisForm;
    }

    public Set<ServiceArtifactInfo> getServicesUsedInForm() {
        return this.servicesUsedInThisForm;
    }

    public FormWidgetArtifactInfo getFormThisFormExtends() {
        return this.formThisFormExtends;
    }

    public Set<FormWidgetArtifactInfo> getFormsExtendingThisForm() {
        return this.aif.allFormInfosExtendingForm.get(this.getUniqueId());
    }

    public Set<ScreenWidgetArtifactInfo> getScreensIncludingThisForm() {
        return this.aif.allScreenInfosReferringToForm.get(this.getUniqueId());
    }

    public Set<ControllerRequestArtifactInfo> getRequestsLinkedToInForm() {
        return this.requestsLinkedToInForm;
    }

    public Set<ControllerRequestArtifactInfo> getRequestsTargetedByForm() {
        return this.requestsTargetedByInForm;
    }
}
