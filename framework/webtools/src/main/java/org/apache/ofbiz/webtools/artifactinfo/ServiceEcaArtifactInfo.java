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
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.ofbiz.base.location.FlexibleLocation;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.service.eca.ServiceEcaAction;
import org.apache.ofbiz.service.eca.ServiceEcaRule;

/**
 * The type Service eca artifact info.
 */
public class ServiceEcaArtifactInfo extends ArtifactInfoBase {
    private ServiceEcaRule serviceEcaRule;
    private String displayPrefix = null;
    private int displaySuffixNum = 0;

    private Set<ServiceArtifactInfo> servicesCalledByThisServiceEca = new ConcurrentSkipListSet<>();

    /**
     * Instantiates a new Service eca artifact info.
     * @param serviceEcaRule the service eca rule
     * @param aif            the aif
     * @throws GeneralException the general exception
     */
    public ServiceEcaArtifactInfo(ServiceEcaRule serviceEcaRule, ArtifactInfoFactory aif) throws GeneralException {
        super(aif);
        this.serviceEcaRule = serviceEcaRule;
    }

    /**
     * This must be called after creation from the ArtifactInfoFactory after this class has been put into the global Map in order
     * to avoid recursive initialization
     * @throws GeneralException the general exception
     */
    public void populateAll() throws GeneralException {
        // populate the services called Set
        for (ServiceEcaAction ecaAction: serviceEcaRule.getEcaActionList()) {
            servicesCalledByThisServiceEca.add(getAif().getServiceArtifactInfo(ecaAction.getServiceName()));
            addToSortedSetInMap(this, getAif().getAllServiceEcaInfosReferringToServiceName(), ecaAction.getServiceName());
        }
    }

    @Override
    public String getDisplayName() {
        return this.getDisplayPrefixedName();
    }

    @Override
    public String getDisplayType() {
        return "Service ECA";
    }

    @Override
    public String getType() {
        return ArtifactInfoFactory.SERVICE_ECA_INFO_TYPE_ID;
    }

    @Override
    public String getUniqueId() {
        return this.serviceEcaRule.toString();
    }

    @Override
    public URL getLocationURL() throws MalformedURLException {
        return FlexibleLocation.resolveLocation(serviceEcaRule.getDefinitionLocation());
    }

    /**
     * Gets service eca rule.
     * @return the service eca rule
     */
    public ServiceEcaRule getServiceEcaRule() {
        return this.serviceEcaRule;
    }

    /**
     * Sets display prefix.
     * @param displayPrefix the display prefix
     */
    public void setDisplayPrefix(String displayPrefix) {
        this.displayPrefix = displayPrefix;
    }

    /**
     * Sets display suffix num.
     * @param displaySuffixNum the display suffix num
     */
    public void setDisplaySuffixNum(int displaySuffixNum) {
        this.displaySuffixNum = displaySuffixNum;
    }

    /**
     * Gets display prefixed name.
     * @return the display prefixed name
     */
    public String getDisplayPrefixedName() {
        return (this.displayPrefix != null ? this.displayPrefix : "") + this.serviceEcaRule.getServiceName() + "_"
                + this.serviceEcaRule.getEventName() + "_" + displaySuffixNum;
    }

    /**
     * Gets services called by service eca actions.
     * @return the services called by service eca actions
     */
    public Set<ServiceArtifactInfo> getServicesCalledByServiceEcaActions() {
        return this.servicesCalledByThisServiceEca;
    }

    /**
     * Gets services triggering service eca.
     * @return the services triggering service eca
     */
    public Set<ServiceArtifactInfo> getServicesTriggeringServiceEca() {
        return getAif().getAllServiceInfosReferringToServiceEcaRule().get(this.serviceEcaRule);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ServiceEcaArtifactInfo) {
            ServiceEcaArtifactInfo that = (ServiceEcaArtifactInfo) obj;
            return this.serviceEcaRule.equals(that.serviceEcaRule);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
