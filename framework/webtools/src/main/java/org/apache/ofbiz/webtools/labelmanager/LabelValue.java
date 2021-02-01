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

public class LabelValue {

    private static final String MODULE = LabelValue.class.getName();

    private String labelValue = "";
    private String labelComment = "";

    public LabelValue(String labelValue, String labelComment) {
        this.labelValue = labelValue;
        this.labelComment = labelComment;
    }

    /**
     * Gets label value.
     * @return the label value
     */
    public String getLabelValue() {
        return labelValue;
    }

    /**
     * Gets label comment.
     * @return the label comment
     */
    public String getLabelComment() {
        return labelComment;
    }

    /**
     * Sets label value.
     * @param labelValue the label value
     */
    public void setLabelValue(String labelValue) {
        this.labelValue = labelValue;
    }

    /**
     * Sets label comment.
     * @param labelComment the label comment
     */
    public void setLabelComment(String labelComment) {
        this.labelComment = labelComment;
    }
}
