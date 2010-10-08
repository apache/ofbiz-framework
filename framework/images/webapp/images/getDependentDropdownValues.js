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

// *** getDependentDropdownValues allows to dynamically populate a dependent drop-down on change on its parent drop-down, doesn't require any fixed naming convention 
// request      = request calling the service which retrieve the info from the DB, ex: getAssociatedStateList
// paramKey     = parameter value used in the called service 
// paramField   = parent drop-down field Id (mainId)
// targetField  = dependent drop-down field Id (dependentId)
// responseName = result returned by the service (using a standard json response, ie chaining json request)
// keyName      = keyName of the dependent drop-down  
// descName     = name of the dependent drop-down description
// selected     = optional name of a selected option
// callback     = optional javascript function called at end
// hide         = optional boolean argument, if true the dependent drop-down field (targetField) will be hidden when no options are available else only disabled. False by default.
// hideTitle    = optional boolean argument (hide must be set to true), if true the title of the dependent drop-down field (targetField) will be hidden when no options are available else only disabled. False by default.
// inputField   = optional name of an input field    
// 				  this is to handle a specific case where an input field is needed instead of a drop-down when no values are returned by the request
// 				  this will be maybe extended later to use an auto-completed drop-down or a lookup, instead of straight drop-down currently, when there are too much values to populate
// 				  this is e.g. currently used in the Product Price Rules screen


function getDependentDropdownValues(request, paramKey, paramField, targetField, responseName, keyName, descName, selected, callback, hide, hideTitle, inputField) {
	var params = new Array();
	params[paramKey] = $F(paramField);
	
    var optionList = [];
    new Ajax.Request(request, {
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
            if ((list.size() < 1) || ((list.size() == 1) && list[0].indexOf("_NA_") >=0)) {
            	Form.Element.disable(targetField);
            	if (hide) {
					if ($(targetField).visible()) {
						Effect.Fade(targetField, {duration: 2.5});
						if (hideTitle) Effect.Fade(targetField + '_title', {duration: 2.5});
					} else {
		                Effect.Appear(targetField, {duration: 0.0});
		                if (hideTitle) Effect.Appear(targetField + '_title', {duration: 0.0});	                    
						Effect.Fade(targetField, {duration: 2.5});
						if (hideTitle) Effect.Fade(targetField + '_title', {duration: 2.5});
		            }            	
				}
            } else {
            	Form.Element.enable(targetField);
            	if (hide) {
	                if (!$(targetField).visible()) {
	                    Effect.Appear(targetField, {duration: 0.0});
	                    if (hideTitle) Effect.Appear(targetField + '_title', {duration: 0.0});	                    
	                }
            	}
            }
            if (callback != null)
            	eval(callback);
        },
	    onComplete: function() {
            // this is to handle a specific case where an input field is needed instead of a drop-down when no values are returned by the request (else if allow-empty="true" is used autoComplete handle the case)
            // this could be extended later to use an autocompleted drop-down or a lookup, instead of drop-down currently, when there are too much values to populate
            // Another option is to use an input field with Id instead of a drop-down, see setPriceRulesCondEventJs.ftl and top of getAssociatedPriceRulesConds service
            if (!list && inputField) {
                $(targetField).hide();
                $(inputField).show();
            } else if (inputField) { 
                $(inputField).hide();
                $(targetField).show();
            }
            $(targetField).update(optionList);
		}        
    });
}

//*** calls any service already mounted as an event
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

//*** checkUomConversion returns true if an UomConversion exists 
function checkUomConversion(request, params) {
    data = getServiceResult(request, params);    
    return data['exist']; 
}
