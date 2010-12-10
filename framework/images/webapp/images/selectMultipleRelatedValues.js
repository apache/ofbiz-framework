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

// *** selectMultipleRelatedValues:  Select multi related values 
// request      = request calling the service which retrieve the info from the DB, ex: getAssociatedStateList
// paramKey     = parameter value used in the called service 
// paramField   = parent dropdown field Id
// targetField  = dependend dropdown field Id
// type         = type of values to retrieve
// typeValue    = value of type to retrieve
// responseName = result returned by the service (using a standard json response, ie chaining json request)

function selectMultipleRelatedValues(request, paramKey, paramField, targetField, type, typeValue, responseName) {
    data = [ { name: paramKey, value: jQuery('#' + paramField).val()}, { name: type, value: typeValue} ];  // get requested value from parent dropdown field 
    list = jQuery.post(request, data, function(result) {
        selectedOptions = result[responseName];
        jQuery("#" + targetField).val(selectedOptions).click().change(); // .change() for asmselect, .click() specifically for IE8 
    }, 'json');
}
