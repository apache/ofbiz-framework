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

// *** getDependentDropdownValues allows to dynamically populate a dependent dropdown on change on its parent dropdown, doesn't require any fixed naming convention 
// request      = request calling the service which retrieve the info from the DB, ex: getAssociatedStateList
// paramKey     = parameter value used in the called service 
// paramField   = parent dropdown field Id (mainId)
// targetField  = dependend dropdown field Id (dependentId)
// responseName = result returned by the service (using a standard json response, ie chaining json request)
// keyName      = keyName of the dependent dropdown  
// descName     = name of the dependent dropdown description
// selected     = optional name of a selected option
// callback     = optional javascript function called at end
function getDependentDropdownValues(request, paramKey, paramField, targetField, responseName, keyName, descName, selected, callback) {
	// parameters
	var params = new Array();
	params[paramKey] = $F(paramField);			
	
    var optionList = [];
    var requestToSend = request;
    new Ajax.Request(requestToSend, {
        asynchronous: false,
        parameters: params,
        onSuccess: function(transport) {
            var data = transport.responseText.evalJSON(true);                     
            list = data[responseName];
            list.each(function(value) {
            	if (typeof value == 'string') {            	
	                values = value.split(': ');
	                if (values[1].indexOf(selected) >=0) {
	                    optionList.push("<option selected='selected' value = "+values[1]+" >"+values[0]+"</option>");
	                } else {
	                    optionList.push("<option value = "+values[1]+" >"+values[0]+"</option>");
	                }
            	} else {
            		if (value[keyName] == selected) {
            			optionList.push("<option selected='selected' value = " + value[keyName] +" >"+ value[descName] + "</option>");
            		} else {
            			optionList.push("<option value = " + value[keyName] + " >" + value[descName] + "</option>");
            		}
            	}
            });
            $(targetField).update(optionList);
            if ((list.size() < 1) || ((list.size() == 1) && list[0].indexOf("_NA_") >=0)) {                
                if ($(targetField).visible()) {
                    Effect.Fade(targetField, {duration: 1.5});
                }
            } else {
                if (!$(targetField).visible()) {
                    Effect.Appear(targetField, {duration: 0.0});
                }
            }
            if (callback != null)
            	eval(callback);
        }
    });
}

// calls any service already mounted as an event
function getServiceResult(request, params) {
	var data;
	new Ajax.Request(request, {
        asynchronous: false,
        parameters: params,
        onSuccess: function(transport) {
			data = transport.responseText.evalJSON(true);  			
		}
	});
	return data;
}
