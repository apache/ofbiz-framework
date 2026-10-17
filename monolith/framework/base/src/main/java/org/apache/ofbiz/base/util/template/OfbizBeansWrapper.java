/*******************************************************************************
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
 *******************************************************************************/
package org.apache.ofbiz.base.util.template;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.MapModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.Version;

/**
 * The {@link BeansWrapper} that exposes Java objects to OFBiz FreeMarker templates.
 *
 * <p>It behaves like {@code BeansWrapper} in every respect but one: for {@link Map} values it
 * enumerates only the map's own keys. FreeMarker's stock {@link MapModel} reports the union of the
 * map's keys and the bean property names of the map object, so {@code ?keys}, {@code ?values},
 * {@code ?size} and {@code <#list aMap as key, value>} all see accessors such as {@code getClass} or
 * {@code entrySet} mixed in with the real entries (OFBIZ-13164).
 *
 * <p>Only key enumeration changes. Member lookup still falls back to the bean model, so templates
 * can keep calling methods on maps — including on {@code GenericValue}, which implements {@code Map}.
 */
public class OfbizBeansWrapper extends BeansWrapper {

    public OfbizBeansWrapper(Version version) {
        super(version);
    }

    @Override
    public TemplateModel wrap(Object object) throws TemplateModelException {
        // A TemplateModel is left to the superclass, which passes it through untouched even when it
        // also happens to be a Map.
        if (object instanceof Map && !(object instanceof TemplateModel)) {
            return new MapEntryKeysModel((Map<?, ?>) object, this);
        }
        return super.wrap(object);
    }

    /**
     * A {@link MapModel} that enumerates the map's own keys rather than the union of those keys and
     * the bean property names of the map object.
     */
    private static final class MapEntryKeysModel extends MapModel {
        private final Map<?, ?> map;

        MapEntryKeysModel(Map<?, ?> map, BeansWrapper wrapper) {
            super(map, wrapper);
            this.map = map;
        }

        @Override
        protected Set<Object> keySet() {
            // A modifiable copy: MapModel's own implementation adds to the set it gets from BeanModel,
            // while maps such as GenericEntity and MapContext return an unmodifiable key set. Copying
            // also preserves the map's iteration order.
            return new LinkedHashSet<>(map.keySet());
        }
    }
}
