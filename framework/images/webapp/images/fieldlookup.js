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
var NS4 = (navigator.appName.indexOf("Netscape") >= 0 && ! document.getElementById)? true: false;
var IE4 = (document.all && ! document.getElementById)? true: false;
var IE5 = (document.getElementById && document.all)? true: false;
var NS6 = (document.getElementById && navigator.appName.indexOf("Netscape") >= 0)? true: false;
var mx, my;
var ACTIVATED_LOOKUP = null;
var LOOKUP_DIV = null;
var INITIALLY_COLLAPSED = null;
var SHOW_DESCRIPTION = false;


function moveobj(evt) {
    if (NS4 || NS6) {
        mx = evt.screenX;
        my = evt.screenY;
    } else if (IE5 || IE4) {
        mx = event.screenX;
        my = event.screenY;
    }
}

var target = null;
var target2 = null;
var targetW = null;
var lookups =[];

function call_fieldlookup(target, viewName, formName, viewWidth, viewheight) {
    var fieldLookup = new fieldLookup1(target);
    if (! viewWidth) viewWidth = 350;
    if (! viewheight) viewheight = 200;
    fieldLookup.popup(viewName, formName, viewWidth, viewheight);
}
function call_fieldlookupLayer(target, viewName, lookupWidth, lookupHeight, lookupPosition, fadeBackground, initiallyCollapsed) {
    if (isEmpty(target) || isEmpty(viewName)) {
        return lookup_error("Lookup can't be created, one of these variables is missing: target=" + target + " viewName=" + viewName);
    }

    var fieldLookupPopup = new FieldLookupPopup(target, viewName, lookupWidth, lookupHeight, lookupPosition, fadeBackground, initiallyCollapsed, arguments);
    fieldLookupPopup.showLookup();
    this.target = target;
}

function call_fieldlookupLayer3(target, target2, viewName, lookupWidth, lookupHeight, lookupPosition, fadeBackground, initiallyCollapsed) {
    if (isEmpty(target) || isEmpty(target2) || isEmpty(viewName)) {
        return lookup_error("Lookup can't be created, one of these variables is missing: target=" + target + " target2=" + target2 + " viewName=" + viewName);
    }

    var fieldLookupPopup = new FieldLookupPopup(target, viewName, lookupWidth, lookupHeight, lookupPosition, fadeBackground, initiallyCollapsed, arguments);
    fieldLookupPopup.showLookup();
    this.target = target;
    this.target2 = target2;
}

function call_fieldlookup2(target, viewName, presentation) {
    var fieldLookup = new fieldLookup1(target, arguments, presentation);
    fieldLookup.popup2(viewName);
}

function call_fieldlookup3(target, target2, viewName, presentation) {
    var fieldLookup = new fieldLookup2(target, target2, arguments, presentation);
    fieldLookup.popup2(viewName);
}

function fieldLookup1(obj_target, args, presentation) {
    this.args = args;
    this.presentation = presentation;
    // passing methods
    this.popup = lookup_popup1;
    this.popup2 = lookup_popup2;
    
    // validate input parameters
    if (! obj_target) return lookup_error("Error calling the field lookup: no target control specified");
    if (obj_target.value == null) return lookup_error("Error calling the field lookup: parameter specified is not valid target control");
    targetW = obj_target;
}

function fieldLookup2(obj_target, obj_target2, args, presentation) {
    this.args = args;
    this.presentation = presentation;
    // passing methods
    this.popup = lookup_popup1;
    this.popup2 = lookup_popup2;
    // validate input parameters
    if (! obj_target) return lookup_error("Error calling the field lookup: no target control specified");
    if (obj_target.value == null) return lookup_error("Error calling the field lookup: parameter specified is not valid target control");
    targetW = obj_target;
    // validate input parameters
    if (! obj_target2) return lookup_error("Error calling the field lookup: no target2 control specified");
    if (obj_target2.value == null) return lookup_error("Error calling the field lookup: parameter specified is not valid target2 control");
    target2 = obj_target2;
    
}

function lookup_popup1(view_name, form_name, viewWidth, viewheight) {
    var obj_lookupwindow = window.open(getViewNameWithSeparator(view_name) + 'formName=' + form_name + '&presentation=' + this.presentation + '&id=' + this.id, '_blank', 'width=' + viewWidth + ',height=' + viewheight + ',scrollbars=yes,status=no,resizable=yes,top=' + my + ',left=' + mx + ',dependent=yes,alwaysRaised=yes');
    obj_lookupwindow.opener = window;
    obj_lookupwindow.focus();
}
function lookup_popup2(view_name) {
    var argString = "";
    if (this.args != null) {
        if (this.args.length > 2) {
            for (var i = 2; i < this.args.length; i++) {
                argString += "&parm" + (i - 3) + "=" + this.args[i];
            }
        }
    }

    var obj_lookupwindow = window.open(getViewNameWithSeparator(view_name) + 'presentation=' + this.presentation + '&id=' + this.id + argString, '_blank', 'width=900,height=700,scrollbars=yes,status=no,resizable=yes,top=' + my + ',left=' + mx + ',dependent=yes,alwaysRaised=yes');
    obj_lookupwindow.opener = window;
    obj_lookupwindow.focus();
}
function lookup_error(str_message) {
    var CommonErrorMessage2 = getJSONuiLabel("CommonUiLabels", "CommonErrorMessage2");
    showErrorAlert(CommonErrorMessage2, str_message);
    return null;
}

function getViewNameWithSeparator(view_name) {
    var sep = "?";
    if (view_name.indexOf("?") >= 0) {
        sep = "&";
    }
    return view_name + sep;
}

function initiallyCollapse() {
    if ((!LOOKUP_DIV) || (INITIALLY_COLLAPSED != "true")) return;
    var slTitleBars = LOOKUP_DIV.getElementsByClassName('screenlet-title-bar');
    for (i in slTitleBars) {
        var slTitleBar = slTitleBars[i];
        var ul = slTitleBar.firstChild;
        if ((typeof ul) != 'object') continue;

        var childElements = ul.childNodes;
        for (j in childElements) {
            if (childElements[j].className == 'expanded' || childElements[j].className == 'collapsed') {
                break;
            }
        }        
        var childEle = childElements[j].firstChild;
        CollapsePanel(childEle, 'lec' + COLLAPSE);
        break;
    }
}

function CollapsePanel(link, areaId){
    var container = $(areaId);
    var liElement = $(link).up('li');
    liElement.removeClassName('expanded');
    liElement.addClassName('collapsed');
    Effect.toggle(container, 'appear');
}

function initiallyCollapseDelayed() {
    setTimeout("initiallyCollapse()", 400);
}


/*************************************
* Fieldlookup Class & Methods
*************************************/
function ConstructLookup(requestUrl, inputFieldId, dialogTarget, dialogOptionalTarget, formName, width, height, position, modal, ajaxUrl, showDescription, presentation, defaultMinLength, defaultDelay, args) {
    // add the presentation attribute to the request url to let the request know which decorator should be loaded
    if(!presentation) {
      var presentation = "layer"
    }
    
    // create Link Element with unique Key
    var lookupId = GLOBAL_LOOKUP_REF.createNextKey();
    var inputBox = document.getElementById(inputFieldId);
    newInputBoxId = lookupId + "_" + inputFieldId;
    inputBox.id = newInputBoxId;
    var parent = inputBox.parentNode;

    var link = document.createElement('A');
    link.href = "javascript:void(0);";
    link.id = lookupId + "_button";

    parent.appendChild(link);

    var hiddenDiv = document.createElement("DIV");
    hiddenDiv.id = lookupId;
    hiddenDiv.css = "{display: none;}";
    hiddenDiv.title = "";

    parent.appendChild(hiddenDiv);

    // createAjax autocomplete
    if (ajaxUrl != "" && showDescription != "") {
         SHOW_DESCRIPTION = showDescription;
        //write the new input box id in the ajaxUrl Array
        ajaxUrl = ajaxUrl.replace(ajaxUrl.substring(0, ajaxUrl.indexOf(",")), newInputBoxId);
        new ajaxAutoCompleter(ajaxUrl, showDescription, defaultMinLength, defaultDelay, formName);
    }
    
    var positioning = null;
    if (position == "topleft") {
        positioning = ['left', 'top'];
    } else if (position == "topcenter") {
        positioning = ['center', 'top'];
    } else if (position == "topright") {
        positioning = ['right', 'top'];
    } else if (position == "center") {
        positioning = 'center';
    } else if (position == "left") {
        positioning = 'left';
    } else if (position == "right") {
        positioning = 'right';
    } else {
        positioning = ['left', 'top'];
    }

    var lookupFormAction = null;
    function lookup_onKeyEnter(event) {
        if (event.which == 13) {
            lookupFormAjaxRequest(lookupFormAction, "form_" + lookupId);
            return false;
        }
    }
    
    // Lookup Configuration
    var dialogOpts = {
        modal: (modal == "true") ? true : false,
        bgiframe: true,
        autoOpen: false,
        height: (height != "") ? parseInt(height) : 500,
        width: (width != "") ? parseInt(width) : 620,
        position: positioning,
        draggable: true,
        resizeable: true,
        open: function(event,ui) {
            waitSpinnerShow();
            jQuery("#" + lookupId).empty();

            var queryArgs = "presentation=" + presentation;
            if (typeof args == "object" && jQuery.isArray(args)) {
                for (var i = 0; i < args.length; i++) {
                    queryArgs += "&parm" + i + "=" + jQuery(args[i]).val();
                }
            }

            jQuery.ajax({
                type: "post",
                url: requestUrl,
                data: queryArgs,
                timeout: AJAX_REQUEST_TIMEOUT,
                cache: false,
                dataFilter: function(data, dataType) {
                    waitSpinnerHide();
                    return data;
                },
                success: function(data) {
                    jQuery("#" + lookupId).html(data);
                    
                    lookupFormAction = jQuery("#" + lookupId + " form:first").attr("action");
                    modifySubmitButton(lookupId);
                    jQuery("#" + lookupId).bind("keypress", lookup_onKeyEnter);
                    // set up the window chaining
                    // if the ACTIVATED_LOOKUP var is set there have to be more than one lookup,
                    // before registrating the new lookup we store the id of the old lookup in the
                    // preLookup variable of the new lookup object. I.e. lookup_1 calls lookup_8, the lookup_8
                    // object need a reference to lookup_1, this reference is set here
                    var prevLookup = null
                    if (ACTIVATED_LOOKUP) {
                        prevLookup = GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).lookupId;
                    }
                    identifyLookup(lookupId);
                    
                    if (prevLookup) {
                        GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).prevLookup = prevLookup;
                    }
                },
                error: function(xhr, reason, exception) {
                    if(exception != 'abort') {
                        alert("An error occurred while communicating with the server:\n\n\nreason=" + reason + "\n\nexception=" + exception);
                    }
                    location.reload(true);
                },
            });
        },
        close: function() {
            jQuery("#" + lookupId).unbind("keypress", lookup_onKeyEnter);
            
            waitSpinnerHide();

            //when the window is closed the prev Lookup get the focus (if exists)
            if (ACTIVATED_LOOKUP) {
                var prevLookup = GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).prevLookup;
            }
            if (prevLookup) {
                identifyLookup(prevLookup);
            } else {
                ACTIVATED_LOOKUP = null;
            }
        }
    };
    
    // init Dialog and register
    // create an object with all Lookup Informationes that are needed
    var dialogRef = jQuery("#" + lookupId).dialog(dialogOpts);
    
    //setting up global variabels, for external access
    this.inputBoxId = inputBox.id;
    this.lookupId = lookupId;
    this.formName = formName;
    this.target = null;
    this.presentation = presentation;
    if (dialogOptionalTarget != null) {
        this.target2 = null;
    }
    this.prevLookup = null;
    this.dialogRef = dialogRef;
    //write external settings in global window manager
    GLOBAL_LOOKUP_REF.setReference(lookupId, this);
    
    // bind click Event to Dialog button
    jQuery("#" + lookupId + "_button").click(
        function (){
            jQuery("#" + lookupId).dialog("open");
            jQuery("#" + lookupId).dialog(dialogOpts);
            GLOBAL_LOOKUP_REF.getReference(lookupId).target = jQuery(dialogTarget);
            if (dialogOptionalTarget != null) {
                //the target2 have to be set here, because the input field is not created before
                GLOBAL_LOOKUP_REF.getReference(lookupId).target2 = jQuery(dialogOptionalTarget);
            }
            return false;
        }
    );

    // close the dialog when clicking outside the dialog area
    jQuery(".ui-widget-overlay").live("click", function() {  
        if(!ACTIVATED_LOOKUP || lookupId==ACTIVATED_LOOKUP){
            jQuery("#" + lookupId).dialog("close");
        }
    });
}

function FieldLookupCounter() {
    this.refArr = {};

    this.setReference = function (key, ref) {
        //if key doesn't exist in the array and
        for (itm in this.refArr) {
            if (itm == key) {
                prefix = key.substring(0, key.indexOf("_"));
                key = prefix + "_" + key;
                this.refArr[""+ key + ""] = ref;
                return this.refArr[key];
            }
        }
        this.refArr[""+ key + ""] = ref;
        return this.refArr[key];
    };
    
    this.getReference = function (key) {
        // when key does not exist return null?
        return this.refArr[key] != null ? this.refArr[key] : null;
    };
    
    this.getLastReference = function () {
        return (this.countFields() -1) + "_lookupId";
    }
    
    this.createNextKey = function () {
        return this.countFields() + "_lookupId";       
    };
    
    this.countFields = function () {
        var count = 0;
        jQuery.each(this.refArr, function (itm) {count++;});
        return count;
    };
    
    this.removeReference = function (key) {
        // deletes the Array entry (doesn't effect the array length)
        delete this.refArr[key];
    };
    
};
var GLOBAL_LOOKUP_REF = new FieldLookupCounter;

/**
* returns true if a String is empty
* @param value - String value
* @return
*/
function isEmpty(value) {
    if (value == null || value == "") {
        return true;
    }
    return false;
}

function identifyLookup (newAl) {
    if (ACTIVATED_LOOKUP != newAl) { 
        ACTIVATED_LOOKUP = newAl;
    }
}

//global expand/col button var
var COLLAPSE = 1999;
function getNextCollapseSeq() {
    COLLAPSE++;
    return COLLAPSE;
}

//modify expande/collapse button
function modifyCollapseable(lookupDiv){
    if (!lookupDiv) {
        return;
    }
    var slTitleBars = jQuery("#" + lookupDiv + " .screenlet-title-bar");
    //jQuery("#" + lookupDiv + " li.expanded");

    jQuery.each(slTitleBars, function(i) {
        var slTitleBar = slTitleBars[i];
        var ul = slTitleBar.firstChild;
        if ((typeof ul) != 'object') {
            return true;
        }
        var childElements = ul.childNodes;

        for (j in childElements) {
            if (childElements[j].className == 'expanded' || childElements[j].className == 'collapsed') {
                break;
            }
        }

        getNextCollapseSeq();
        var childEle = childElements[j].firstChild;

        childEle.setAttribute('onclick', "javascript:toggleScreenlet(this, 'lec" + COLLAPSE +"', 'true', 'Expand', 'Collapse');");
        childEle.href = "javascript:void(0);"
        jQuery(slTitleBar).next('div').attr('id', 'lec' + COLLAPSE);

    });
}

function modifySubmitButton (lookupDiv) {
    /* changes form/submit behavior for Lookup Layer */
    if (lookupDiv) {
        modifyCollapseable(lookupDiv);

        //find the lookup form and input button
        var lookupForm = jQuery("#" + lookupDiv + " form:first");

        //set new form name and id
        oldFormName = lookupForm.attr("name");
        lookupForm.attr("name", "form_" + lookupDiv);
        lookupForm.attr("id", "form_" + lookupDiv);
        lookupForm = jQuery("#form_" + lookupDiv);
        //set new links for lookups
        var newLookups = jQuery("#" + lookupDiv + " .field-lookup");

        var formAction = lookupForm.attr("action");
        // remove the form action
        lookupForm.attr("action", "");
        var input = jQuery("#" + lookupDiv + " input[type=submit]").css({display: "block"});

        // remove the original input button and replace with a new one

        var txt = input.attr("value");
        (input.parent()).append(jQuery("<button/>", {
            id: "lookupSubmitButton",
            href: "javascript:void(0);",
            click: function () {
                lookupFormAjaxRequest(formAction, lookupForm.attr("id"));
                return false;
            },
            text: txt
        }));

        input.remove();
        //modify nav-pager
        var navPagers = jQuery("#" + lookupDiv + " .nav-pager a");
        jQuery.each(navPagers, function(navPager) {
            jQuery(navPagers[navPager]).attr("href", "javascript:lookupPaginationAjaxRequest('" + encodeURI(jQuery(navPagers[navPager]).attr("href")) + "','link')");
        });

        var navPagersSelect = jQuery("#" + lookupDiv + " .nav-pager select");
        jQuery.each(navPagersSelect, function(navPager) {
            // that's quite weird maybe someone have a better idea ... that's where the magic happens
            try {
                  var oc = jQuery(navPagersSelect[navPager]).attr("onchange");
                  if((typeof oc) == "function"){ // IE6/7 Fix
                    oc = oc.toString();
                    var ocSub = oc.substring((oc.indexOf('=') + 3),(oc.length - 4));
                    // define search pattern we must seperate between IE and Other Browser
                    var searchPattern = /" \+ this.value \+ "/g;
                    var searchPattern_IE = /'\+this.value\+'/g;
                    var searchPattern2 = /" \+ this.valu/g;
                    var searchPattern2_IE = /'\+this.valu/g;

                    if (searchPattern.test(ocSub)) {
                        var viewSize = navPagersSelect[navPager].value;
                        var spl = ocSub.split(searchPattern);
                        navPagersSelect[navPager].onchange = function () {
                            lookupPaginationAjaxRequest(spl[0] + this.value + spl[1],'select');
                        };
                    } else if (searchPattern_IE.test(ocSub)) {
                        var viewSize = navPagersSelect[navPager].value;
                        var spl = ocSub.split(searchPattern_IE);
                        navPagersSelect[navPager].onchange = function () {
                            lookupPaginationAjaxRequest("/" + spl[0] + this.value + spl[1],'select');
                        };
                    } else if (searchPattern2.test(ocSub)) {
                        ocSub = ocSub.replace(searchPattern2, "");
                        if (searchPattern.test(ocSub)) {
                            ocSub.replace(searchPattern, viewSize);
                        }
                        navPagersSelect[navPager].onchange = function () {
                            lookupPaginationAjaxRequest(ocSub + this.value,'select');
                        };
                    } else if (searchPattern2_IE.test(ocSub)) {
                        ocSub = ocSub.replace(searchPattern2_IE, "");
                        if (searchPattern_IE.test(ocSub)) {
                            ocSub.replace(searchPattern_IE, viewSize);
                        }
                        navPagersSelect[navPager].onchange = function () {
                            lookupPaginationAjaxRequest("/" + ocSub + this.value,'select');
                        };
                    }
                } else {
                    var ocSub = oc.substring((oc.indexOf('=') + 1),(oc.length - 1));
                    navPagersSelect[navPager].setAttribute("onchange", "lookupPaginationAjaxRequest(" + ocSub + ",'')");
                }

                if (resultTable == null) {
                    return;
                }
                resultTable = resultTable.childElements()[0];
                var resultElements = resultTable.childElements();
                for (i in resultElements) {
                    var childElements = resultElements[i].childElements();
                    if (childElements.size() == 1) {
                        continue;
                    }
                    for (k = 1; k < childElements.size(); k++) {
                        var cell = childElements[k];
                        var cellChild = null;
                        cellChild = cell.childElements();
                        if (cellChild.size() > 0) {
                            for (l in cellChild) {
                                var cellElement = cellChild[l];
                                if (cellElement.tagName == 'A') {
                                    var link = cellElement.href;
                                    var liSub = link.substring(link.lastIndexOf('/')+1,(link.length));
                                    if (liSub.indexOf("javascript:set_") != -1) {
                                        cellElement.href = link;
                                    } else {
                                        cellElement.href = "javascript:lookupAjaxRequest('" + liSub + "&presentation=layer')";
                                    }
                                }
                            }
                        }
                    }
                }
            }
            catch (ex) {
            }
        });
        // modify links in result table ...
        var resultTable= jQuery("#" + lookupDiv + " #search-results table:first tbody");
        var tableChildren = resultTable.children();
        jQuery.each(tableChildren, function(tableChild){
            var childElements = jQuery(tableChildren[tableChild]);
            var tableRow = childElements.children();
            jQuery.each(tableRow, function(cell){
                var cellChild = null;
                cellChild = jQuery(tableRow[cell]).children();
                jQuery.each(cellChild, function (child) {
                    if (cellChild[child].tagName == "A"){
                        var link = cellChild[child].href;
                        var liSub = link.substring(link.lastIndexOf('/')+1,(link.length));
                        if (liSub.indexOf("javascript:set_") != -1) {
                            cellChild[child].href = link;
                        } else {
                            cellChild[child].href = "javascript:lookupAjaxRequest('" + liSub + "&presentation=layer')";
                        }
                    }
                });

            });

        });
    }
}
/**
 * Create an ajax Request
 */
function lookupAjaxRequest(request) {
    // get request arguments
    var arg = request.substring(request.indexOf('?')+1,(request.length));
    request = request.substring(0, request.indexOf('?'));
    lookupId = GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).lookupId;
    jQuery("#" + lookupId).load(request, arg, function(data) {
        modifySubmitButton(lookupId);
    });
}

/**
* Create an ajax request to get the search results
* @param formAction - action target
* @param form - formId
* @return
*/
function lookupFormAjaxRequest(formAction, form) {
    lookupId = GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).lookupId;
    var data = jQuery("#" + form).serialize();
    data = data + "&presentation=" + GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).presentation;
    /*jQuery("#" + lookupId).load(formAction, data, function(data) {
        modifySubmitButton(lookupId);
    });*/
    var screenletTitleBar= jQuery("#"+lookupId+" .screenlet-title-bar :visible:first");
    jQuery.ajax({
      url: formAction,
      data: data,
      beforeSend: function(jqXHR, settings) {
        //Here we append the spinner to the lookup screenlet and it will shown till the ajax request is processed.
        var indicator = screenletTitleBar.find("span.indicator");
        //Check that if spinner is already in execution then don't add new spinner
        if (indicator.length == 0) {
          jQuery("<span class='indicator'><img src='/images/ajax-loader.gif' alt='' /></span>").appendTo(screenletTitleBar);
        }
      },
      success: function(result) {
        if (result.search(/loginform/) != -1) {
          window.location.href = window.location.href;
          return;
        }
        // Here we are removing the spinner.
        var indicator = screenletTitleBar.find("span.indicator");
        if (indicator != undefined) {
          jQuery("span.indicator").remove();
        }
        jQuery("#" + lookupId).html(result);
        modifySubmitButton(lookupId);
      }
    });
}

function lookupPaginationAjaxRequest(navAction, type) {
    lookupDiv = (GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).divRef);
    lookupContent = (GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).contentRef);

    lookupId = GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).lookupId;
    /*jQuery("#" + lookupId).load(navAction, function(data) {
        modifySubmitButton(lookupId);
    });*/
    var screenletTitleBar= jQuery("#"+lookupId+" .screenlet-title-bar :visible:first");
    jQuery.ajax({
      url: navAction,
      beforeSend: function(jqXHR, settings) {
        //Here we append the spinner to the lookup screenlet and it will shown till the ajax request is processed.
        var indicator = screenletTitleBar.find("span.indicator");
        //Check that if spinner is already in execution then don't add new spinner
        if (indicator.length == 0) {
          jQuery("<span class='indicator'><img src='/images/ajax-loader.gif' alt='' /></span>").appendTo(screenletTitleBar);
        }
      },
      success: function(result) {
        if (result.search(/loginform/) != -1) {
          window.location.href = window.location.href;
          return;
        }
        // Here we are removing the spinner.
        var indicator = screenletTitleBar.find("span.indicator");
        if (indicator != undefined) {
          jQuery("span.indicator").remove();
        }
        jQuery("#" + lookupId).html(result);
        modifySubmitButton(lookupId);
      }
    });
}

/*******************************************************************************************************
* This code inserts the value lookedup by a popup window back into the associated form element
*******************************************************************************************************/
var re_id = new RegExp('id=(\\d+)');
var num_id = (re_id.exec(String(window.location))? new Number(RegExp.$1): 0);
var obj_caller = (window.opener? window.opener.lookups[num_id]: null);
if (obj_caller == null && window.opener != null) {
    obj_caller = window.opener;
} else if (obj_caller == null && window.opener == null) {
    obj_caller = parent;
}

function setSourceColor(src) {
    if (target && target != null) {
        src.css("background-color", "yellow");
    }
}
// function passing selected value to calling window, using only in the TimeDuration case
function set_duration_value (value) {
    if(GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP)){
        obj_caller.target = GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).target;
    }
    else {
        obj_caller.target = jQuery(obj_caller.targetW);
    }    
    var target = obj_caller.target;

    write_value(value, target);
    closeLookup();
}
// function passing selected value to calling window
function set_value (value) {
    if(GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP)){
        obj_caller.target = GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).target;
    }
    else {
        obj_caller.target = jQuery(obj_caller.targetW);
    }
    
    var target = obj_caller.target;
    write_value(value, target);

    closeLookup();
}
// function passing selected value to calling window
function set_values (value, value2) {
    if(GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP)){
        obj_caller.target = GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).target;
        obj_caller.target2 = GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).target2;
    }
    else {
        obj_caller.target = jQuery(obj_caller.targetW);        
    }
    var target = obj_caller.target;
    var target2 = obj_caller.target2;
    write_value(value, target);
    write_value(value2, target2)
    if (SHOW_DESCRIPTION) setLookDescription(target.attr("id"), value + " " + value2, "", "", SHOW_DESCRIPTION);
    
    closeLookup();
}

function write_value (value, target) {
    if (target && target != null) {
        setSourceColor(target);
        target.val(value);
        target.trigger("lookup:changed");
    }    
}

function set_multivalues(value) {
    obj_caller.target.value = value;
    field = jQuery("#" + target.attr('id')); // TODO: Not tested (should be ok)we need to fix 1st the window lookup (see OFBIZ-3933)
    field.trigger("lookup:changed"); //  If we decide to keep it (only used in Example, though it's needed also for Themes and Languages but not the same way)
    if (field.change != null) {
        field.click().change()
    }

    var thisForm = obj_caller.target.form;
    var evalString = "";
    
    if (arguments.length > 2) {
        for (var i = 1; i < arguments.length; i = i + 2) {
            evalString = "setSourceColor(thisForm." + arguments[i] + ")";
            eval(evalString);
            evalString = "thisForm." + arguments[i] + ".value='" + arguments[i + 1] + "'";
            eval(evalString);
        }
    }
    closeLookup();
}

//close the window after passing the value
function closeLookup() {
    if (window.opener != null && GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP) == null) {
        window.close();
    } else {
        obj = GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).lookupId;
        jQuery("#" + obj).dialog("close");
    }
}

//load description for lookup fields 
var lookupDescriptionLoaded = function(fieldId, url, params, formName) {
  this.init(fieldId, url, params, formName);
}
lookupDescriptionLoaded.prototype.init = function (fieldId, url, params, formName) {
  this.fieldId = fieldId;
  this.url = url;
  this.params = params;
  this.formName = formName;
}
lookupDescriptionLoaded.prototype.update = function (){
  var tooltipElement = jQuery("#" + this.fieldId + '_lookupDescription');
  if (tooltipElement.length) {//first remove current description
    tooltipElement.remove();
  }
  //actual server call
  var fieldName = this.params.substring(this.params.indexOf("searchValueFieldName"));
  fieldName = fieldName.substring(fieldName.indexOf("=") + 1);
  if (jQuery("input[name=" + fieldName + "]").val()) {
  var fieldSerialized = jQuery("input[name=" + fieldName + "]", jQuery("form[name=" + this.formName + "]")).serialize();
  this.allParams = this.params + '&' + fieldSerialized + '&' + 'searchType=EQUALS';
  _fieldId = this.fieldId;
  
  jQuery.ajax({
    url: this.url,
    type: "POST",
    data: this.allParams,
    async: false,
    success: function(result){
      // This would be far more reliable if we were removing the widget boundaries in LookupDecorator using widgetVerbose in context :/
      if (result.split("ajaxAutocompleteOptions.ftl -->")[1]) {
        setLookDescription(_fieldId, result.split("ajaxAutocompleteOptions.ftl -->")[1].trim().split("<!--")[0].trim(), "", "");
      }
    }
  });
  }
}
    
if(typeof String.prototype.trim !== 'function') { // Needed because IE8 does not implement trim yet
  String.prototype.trim = function() {
    return this.replace(/^\s+|\s+$/g, ''); 
  }
}