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
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import javolution.util.FastSet;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.widget.form.ModelForm;
import org.xml.sax.SAXException;

/**
 *
 */
public class FormWidgetArtifactInfo extends ArtifactInfoBase {
    
    protected ModelForm modelForm;
    
    protected String formName;
    protected String formLocation;
    
    protected Set<EntityArtifactInfo> entitiesUsedInThisForm = FastSet.newInstance();
    protected Set<ServiceArtifactInfo> servicesUsedInThisForm = FastSet.newInstance();
    protected FormWidgetArtifactInfo formThisFormExtends = null;
    
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
    public void populateAll() {
        // TODO: populate entitiesUsedInThisForm, servicesUsedInThisForm, formThisFormExtends (and reverse in aif.allFormInfosExtendingForm)
    }
    
    public String getDisplayName() {
        return this.getUniqueId();
    }
    
    public String getDisplayType() {
        return "Form Widget";
    }
    
    public String getType() {
        return ArtifactInfoFactory.FormWidgetInfoTypeId;
    }
    
    public String getUniqueId() {
        return this.formLocation + "#" + this.formName;
    }
    
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
}
