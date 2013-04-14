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

package org.ofbiz.entity;


import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.StringUtil;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.MapModel;
import freemarker.ext.beans.StringModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;


/**
 * Generic Entity Value Object - Handles persistence for any defined entity.
 * WARNING: This object is experimental!
 *
 */
@SuppressWarnings("serial")
public class GenericValueHtmlWrapper extends GenericValue {

    /** Creates new GenericValueHtmlWrapper from existing GenericValue */
    public static GenericValueHtmlWrapper create(GenericValue value) {
        GenericValueHtmlWrapper newValue = new GenericValueHtmlWrapper();
        try {
            newValue.init(value);
        } catch (RuntimeException e) {
            Debug.logError(e, "Error in init for clone of value: " + value, module);
            throw e;
        }
        return newValue;
    }

    /* NOTE: this is NOT used because there are certain FTL files that call services and things, and this messes those up, so only overriding the Map.get(Object) method to get use of this as a Map
     * Override the basic get method, which all other get methods call so we only need to do this one (though most important for the Map.get(Object) and the getString() methods
    public Object get(String name) {
        Object value = super.get(name);
        if (value instanceof String) {
            return StringUtil.htmlEncoder.encode((String) value);
        } else {
            return value;
        }
    }*/

    @Override
    public Object get(Object name) {
        Object value = super.get(name);
        if (value instanceof String) {
            return StringUtil.htmlEncoder.encode((String) value);
        } else {
            return value;
        }
    }

    // another experimental object, this one specifically for FTL
    public static class GenericValueHtmlWrapperForFtl extends MapModel {
        public GenericValueHtmlWrapperForFtl(GenericValue gv, BeansWrapper wrapper) {
            super(gv, wrapper);
        }

        @Override
        public TemplateModel get(String key) {
            TemplateModel tm = null;
            try {
                tm = super.get(key);
            } catch (TemplateModelException e) {
                Debug.logError(e, "Error getting Map with key [" + key + "]: " + e.toString(), module);
            }
            if (tm instanceof StringModel) {
                String original = ((StringModel) tm).getAsString();
                if (original != null) {
                    String encoded = StringUtil.htmlEncoder.encode(original);
                    if (!original.equals(encoded)) {
                        return new StringModel(encoded, this.wrapper);
                    }
                }
            }
            return tm;
        }
    }
}
