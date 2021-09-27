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



function setLookDescription(textFieldId, description, params, formName, showDescription) {
    if (description) {
        var start = description.lastIndexOf(' [');
        if (start != -1) {
            description = description.substring(0, start);

            // This sets a (possibly hidden) dependent field if a description-field-name is provided
            var dependentField = params.substring(params.indexOf("searchValueFieldName"));
            dependentField = jQuery("#" + formName + "_" + dependentField.substring(dependentField.indexOf("=") + 1));
            var dependentFieldValue = description.substring(0, description.lastIndexOf(' '))
            if (dependentField.length) {
                dependentField.val(dependentFieldValue);
                dependentField.trigger("change"); // let the 'hidden' field know its been changed
            }
        }
        var lookupWrapperEl = jQuery("#" + textFieldId).closest('.field-lookup');
        if (lookupWrapperEl.length) {
            if (start == -1 && showDescription) {
                start = description.indexOf(' ');
                if (start != -1 && description.indexOf('<script type="text/javascript">') == -1) {
                    description = description.substring(start);
                }
            }
            tooltipElement = jQuery("#" + textFieldId + '_lookupDescription');
            var descriptionElement = "<p>" + description + "</p>"
            if (tooltipElement.length) {
                tooltipElement.html(descriptionElement)
            } else {
                tooltipElement = jQuery("<span id='" + textFieldId + "_lookupDescription' class='tooltip'>" + descriptionElement + "</span>");
            }
            lookupWrapperEl.append(tooltipElement);
        }
    }
}
