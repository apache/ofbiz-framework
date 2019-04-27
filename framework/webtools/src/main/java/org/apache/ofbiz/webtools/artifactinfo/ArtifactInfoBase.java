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

/**
 *
 */
public abstract class ArtifactInfoBase implements Comparable<ArtifactInfoBase> {

    protected ArtifactInfoFactory aif;
    private String fullName = null;

    public ArtifactInfoBase(ArtifactInfoFactory aif) {
        this.aif = aif;
    }

    @Override
    public int compareTo(ArtifactInfoBase that) {
        if (that == null) {
            return -1;
        }
        return this.toString().compareTo(that.toString());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        try {
            ArtifactInfoBase that = (ArtifactInfoBase) obj;
            return this.toString().equals(that.toString());
        } catch (Exception e) {
            return false;
        }
    }

    abstract public String getDisplayName();

    abstract public String getDisplayType();

    abstract public URL getLocationURL() throws MalformedURLException;

    abstract public String getType();

    abstract public String getUniqueId();

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {
        if (this.fullName == null) {
            this.fullName = this.getDisplayType().concat(":").concat(this.getDisplayName());
        }
        return this.fullName;
    }
}
