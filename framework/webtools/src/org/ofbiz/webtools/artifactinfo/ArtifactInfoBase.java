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

import java.net.MalformedURLException;
import java.net.URL;



/**
 *
 */
public abstract class ArtifactInfoBase implements Comparable<ArtifactInfoBase> {
    protected ArtifactInfoFactory aif;

    public ArtifactInfoBase(ArtifactInfoFactory aif) {
        this.aif = aif;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ArtifactInfoBase) {
            return this.equals(obj);
        } else {
            return false;
        }
    }

    public int compareTo(ArtifactInfoBase that) {
        if (that == null) return -1;
        String thisName = this.getDisplayType() + ":" + this.getDisplayName();
        String thatName = that.getDisplayType() + ":" + that.getDisplayName();
        return thisName.compareTo(thatName);
    }

    abstract public String getDisplayName();
    abstract public String getDisplayType();
    abstract public String getType();
    abstract public String getUniqueId();
    abstract public URL getLocationURL() throws MalformedURLException;


    //public static List<ArtifactInfoBase> sortArtifactInfoSetByDisplayName(Set<ArtifactInfoBase> artifactInfoSet) {
        //SortedMap<String, ArtifactInfoBase> sortedMap = FastMap.newInstance();
    //}
}
