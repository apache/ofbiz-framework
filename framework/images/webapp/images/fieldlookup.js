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

// ================= FIELD LOOKUP METHODS ============================
var NS4 = (navigator.appName.indexOf("Netscape")>=0 && !document.getElementById)? true : false;
var IE4 = (document.all && !document.getElementById)? true : false;
var IE5 = (document.getElementById && document.all)? true : false;
var NS6 = (document.getElementById && navigator.appName.indexOf("Netscape")>=0 )? true: false;
var mx, my;

function moveobj(evt) {
    if (NS4 || NS6) {
    mx=evt.screenX;
    	my=evt.screenY;
    } else if (IE5 || IE4) {
    	mx=event.screenX;
    	my=event.screenY;
    }
}

var target = null;
var target2 = null;
var lookups = [];

function call_fieldlookup(target, viewName, formName,viewWidth,viewheight) {
    var fieldLookup = new fieldLookup1(target);
    if (! viewWidth) viewWidth = 350;
    if (! viewheight) viewheight = 200;
    fieldLookup.popup(viewName, formName, viewWidth, viewheight);
}
function call_fieldlookup2(target, viewName) {
    var fieldLookup = new fieldLookup1(target, arguments);
    fieldLookup.popup2(viewName);
}
function call_fieldlookup3(target, target2, viewName) {
    var fieldLookup = new fieldLookup2(target, target2);
    fieldLookup.popup2(viewName);
}

function fieldLookup1(obj_target, args) {
    this.args = args;
	// passing methods
	this.popup = lookup_popup1;
	this.popup2 = lookup_popup2;

	// validate input parameters
	if (!obj_target)
		return lookup_error("Error calling the field lookup: no target control specified");
	if (obj_target.value == null)
		return lookup_error("Error calling the field lookup: parameter specified is not valid target control");
    //this.target = obj_target;
    target = obj_target;

	// register in global collections
	//this.id = lookups.length;
	//lookups[this.id] = this;
}
function fieldLookup2(obj_target, obj_target2) {
    // passing methods
    this.popup    = lookup_popup1;
    this.popup2    = lookup_popup2;
    // validate input parameters
    if (!obj_target)
        return lookup_error("Error calling the field lookup: no target control specified");
    if (obj_target.value == null)
        return lookup_error("Error calling the field lookup: parameter specified is not valid target control");
    target = obj_target;
    // validate input parameters
    if (!obj_target2)
        return lookup_error("Error calling the field lookup: no target2 control specified");
    if (obj_target2.value == null)
        return lookup_error("Error calling the field lookup: parameter specified is not valid target2 control");
    target2 = obj_target2;


    // register in global collections
    //this.id = lookups.length;
    //lookups[this.id] = this;
}

function lookup_popup1 (view_name, form_name, viewWidth, viewheight) {
	var obj_lookupwindow = window.open(view_name + '?formName=' + form_name + '&id=' + this.id,'_blank', 'width='+viewWidth+',height='+viewheight+',scrollbars=yes,status=no,resizable=yes,top='+my+',left='+mx+',dependent=yes,alwaysRaised=yes');
	obj_lookupwindow.opener = window;
	obj_lookupwindow.focus();
}
function lookup_popup2 (view_name) {
    var argString = "";
    if (this.args != null) {
	    if (this.args.length > 2) {
	        for(var i=2; i < this.args.length; i++) {
	            argString += "&parm" + (i-2) + "=" + this.args[i];
	        }
	    }
    }
    var sep = "?";
    if (view_name.indexOf("?") >= 0) {
        sep = "&";
    }
	var obj_lookupwindow = window.open(view_name + sep + 'id=' + this.id + argString,'_blank', 'width=700,height=550,scrollbars=yes,status=no,resizable=yes,top='+my+',left='+mx+',dependent=yes,alwaysRaised=yes');
	obj_lookupwindow.opener = window;
	obj_lookupwindow.focus();
}
function lookup_error (str_message) {
	alert (str_message);
	return null;
}


