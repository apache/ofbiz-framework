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
package org.apache.ofbiz.webtools.labelmanager;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;

public class LabelInfo {

    private static final String MODULE = LabelInfo.class.getName();

    private String labelKey = "";
    private String labelKeyComment = "";
    private String fileName = "";
    private Map<String, LabelValue> labelValues = new LinkedHashMap<>();

    public LabelInfo(String labelKey, String labelKeyComment, String fileName, String localeStr, String labelValue, String labelComment) {
        this.labelKey = labelKey;
        this.labelKeyComment = labelKeyComment;
        this.fileName = fileName;
        setLabelValue(localeStr, labelValue, labelComment, false);
    }

    /**
     * Gets label key.
     * @return the label key
     */
    public String getLabelKey() {
        return labelKey;
    }

    /**
     * Gets label key comment.
     * @return the label key comment
     */
    public String getLabelKeyComment() {
        return labelKeyComment;
    }

    /**
     * Sets label key comment.
     * @param labelKeyComment the label key comment
     */
    public void setLabelKeyComment(String labelKeyComment) {
        this.labelKeyComment = labelKeyComment;
    }

    /**
     * Gets file name.
     * @return the file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Gets label value.
     * @param localeStr the locale str
     * @return the label value
     */
    public LabelValue getLabelValue(String localeStr) {
        return labelValues.get(localeStr);
    }

    /**
     * Gets label value size.
     * @return the label value size
     */
    public int getLabelValueSize() {
        return labelValues.size();
    }

    /**
     * Sets label value.
     * @param localeStr the locale str
     * @param labelValue the label value
     * @param labelComment the label comment
     * @param update the update
     * @return the label value
     */
    public boolean setLabelValue(String localeStr, String labelValue, String labelComment, boolean update) {
        LabelValue localeFound = getLabelValue(localeStr);
        boolean isDuplicatedLocales = false;

        if (UtilValidate.isEmpty(localeFound)) {
            if (UtilValidate.isNotEmpty(labelValue)) {
                localeFound = new LabelValue(labelValue, labelComment);
                labelValues.put(localeStr, localeFound);
            }
        } else {
            if (update) {
                if (UtilValidate.isNotEmpty(labelValue)) {
                    localeFound.setLabelValue(labelValue);
                    localeFound.setLabelComment(labelComment);
                    labelValues.put(localeStr, localeFound);
                } else {
                    labelValues.remove(localeStr);
                }
            } else {
                if (Debug.warningOn()) {
                    Debug.logWarning("Already found locale " + localeStr + " for label " + labelKey + " into the file " + fileName, MODULE);
                }
                isDuplicatedLocales = true;
            }
        }
        return isDuplicatedLocales;
    }
}
