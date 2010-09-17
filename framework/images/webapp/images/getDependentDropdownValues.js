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
// inputField   = optional name of an input field to use instead of a dropdown (this will be extended later to use an of autocompleted dropdown, instead of dropdown or a lookup, when there are too much values to populate)   
// hide         = optional argument, if true the dependend dropdown field (targetField) will be hidden when no options are available else only disabled. False by default.
function getDependentDropdownValues(request, paramKey, paramField, targetField, responseName, keyName, descName, selected, callback, hide) {
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
            // this is to handle a specific case where an input field is needed, uses inputField for the field name
            if (!list) {
				$(targetField).hide();
				$(targetField).insert({after: new Element('input', {name : inputField, id : targetField + '_input', size : 3})}); 
            	return;
            } else { 
            	if ($(targetField + '_input')) { 
            		$(targetField + '_input').remove();            		
					$(targetField).show();
            	}
            }
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
            	Form.Element.disable(targetField);
            	if (hide) {
					if ($(targetField).visible()) {
						Effect.Fade(targetField, {duration: 1.5});
					}
				}
            } else {
            	Form.Element.enable(targetField);
            	if (hide) {
	                if (!$(targetField).visible()) {
	                    Effect.Appear(targetField, {duration: 0.0});
	                }
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
