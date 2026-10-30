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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 *
 */
public abstract class ArtifactInfoBase implements Comparable<ArtifactInfoBase> {

    private final ArtifactInfoFactory aif;

    /**
     * Gets aif.
     * @return the aif
     */
    public ArtifactInfoFactory getAif() {
        return aif;
    }

    private String fullName = null;

    public ArtifactInfoBase(ArtifactInfoFactory aif) {
        this.aif = aif;
    }

    /**
     * Adds an artifact to the sorted set kept under a key of a reverse-reference map.
     * The artifact graph is built from several threads at once, so the set is created atomically and is thread-safe.
     * @param element the artifact to add
     * @param theMap the reverse-reference map
     * @param setKey the key of the set
     */
    static <K, V> void addToSortedSetInMap(V element, Map<K, Set<V>> theMap, K setKey) {
        theMap.computeIfAbsent(setKey, k -> new ConcurrentSkipListSet<>()).add(element);
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

    public abstract String getDisplayName();

    public abstract String getDisplayType();

    public abstract URL getLocationURL() throws MalformedURLException;

    public abstract String getType();

    public abstract String getUniqueId();

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
