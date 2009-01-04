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
package org.ofbiz.webtools.labelmanager;

import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilValidate;

public class LabelInfo {
    
    public static final String module = LabelInfo.class.getName();
    
    protected String labelKey = "";
    protected String fileName = "";
    protected String componentName = "";
    protected Map<String, String> labelValues = FastMap.newInstance();
    
    public LabelInfo(String labelKey, String fileName, String componentName, String localeStr, String labelValue) throws GeneralException {
        this.labelKey = labelKey;
        this.fileName = fileName;
        this.componentName = componentName;
        setLabelValue(localeStr, labelValue, false);
    }
    
    public String getLabelKey() {
        return labelKey;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public String getComponentName() {
        return componentName;
    }
    
    public String getLabelValue(String localeStr) {
       return (String)labelValues.get(localeStr);
    }
    
    public int getLabelValueSize() {
       return labelValues.size();    
    }

    public boolean setLabelValue(String localeStr, String labelValue, boolean update) {
        String localeFound = getLabelValue(localeStr);
        boolean isDuplicatedLocales = false;
        
        if (UtilValidate.isEmpty(localeFound)) {
            if (UtilValidate.isNotEmpty(labelValue)) {
                labelValues.put(localeStr, labelValue);
            }
        } else {
            if (update) {
                if (UtilValidate.isNotEmpty(labelValue)) {
                    labelValues.put(localeStr, labelValue);
                } else {
                    labelValues.remove(localeStr);
                }
            } else {
                Debug.logWarning("Already found locale " + localeStr + " for label " + labelKey + " into the file " + fileName, module);
                isDuplicatedLocales = true;
            }
        }
        return isDuplicatedLocales;
    }
}
