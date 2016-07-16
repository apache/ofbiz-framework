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

    public static final String module = LabelValue.class.getName();

    protected String labelValue = "";
    protected String labelComment = "";

    public LabelValue(String labelValue, String labelComment) {
        this.labelValue = labelValue;
        this.labelComment = labelComment;
    }

    public String getLabelValue() {
        return labelValue;
    }

    public String getLabelComment() {
        return labelComment;
    }

    public void setLabelValue(String labelValue) {
        this.labelValue = labelValue;
    }

    public void setLabelComment(String labelComment) {
        this.labelComment = labelComment;
    }
}
