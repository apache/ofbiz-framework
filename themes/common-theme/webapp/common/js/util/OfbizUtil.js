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

//Define global variable to store last auto-completer request object (jqXHR).
var LAST_AUTOCOMP_REF = null;

//default ajax request timeout in milliseconds
var AJAX_REQUEST_TIMEOUT = 5000;

// Add observers on DOM ready.
$(document).ready(function() {
    //initializing UI combobox dropdown by overriding its methods.
    ajaxAutoCompleteDropDown();
    // bindObservers will add observer on passed html section when DOM is ready.
    bindObservers("body");
});

/* bindObservers function contains the code of adding observers and it can be called for specific section as well
   when we need to add observer on section which is updated by Ajax.
   Example: bindObservers("sectionSelector");
   sectionSelector can be Id, Class and Element name.
*/
function bindObservers(bind_element) {

    // Adding observer for checkboxes for select all action.
    jQuery(bind_element).on("click", "[type=checkbox]", function() {
        var action_checkbox = jQuery(this),
            parent_action = action_checkbox.is(".selectAll") ? action_checkbox : action_checkbox.getForm().getFormFields().filter(".selectAll");
        if (parent_action.length !== 0) {
            addSelectAllObserver(action_checkbox);
        }
    });

    // If parent checkbox is already checked on DOM ready then check its child checkboxes also.
    if (jQuery(".selectAll").is(":checked")) {
        jQuery(".selectAll").removeAttr("checked").trigger("click");
    }

    //Set default pattern for new plugin jQuery-Mask-Plugin same as Masked-Input-Plugin.
    jQuery.jMaskGlobals = {
        translation: {
            '9': {pattern: /[0-9*]/},
            '*': {pattern: /[a-zA-Z0-9]/},
            'a': {pattern: /[a-zA-Z]/}
        }
    };
    jQuery(bind_element).find("[data-mask]").each(function(){
        var element = jQuery(this);
        var mask = element.data('mask');
        element.mask(mask);
    });
    jQuery(bind_element).find('.autoCompleteDropDown').each(function(){
        jQuery(this).combobox();
    });
    jQuery(bind_element).find('[data-other-field-name]').each(function(){
        var element = jQuery(this);
        var otherFieldName = element.data("other-field-name");
        var otherFieldValue = element.data("other-field-value");
        var otherFieldSize = element.data("other-field-size");
        var disabled = true;
        if(other_choice(this))
            disabled = false;
        var $input = jQuery("<input>", {type: "text", name: otherFieldName})
            .attr("size", otherFieldSize)
            .val(otherFieldValue)
            .on("focus", function(e){
                check_choice(element);
            })
            .css('visibility', 'hidden');
            $input.prop("disabled", disabled);
        $input.insertAfter(element.closest(".ui-widget"));
        element.on("change", function(e) {
            process_choice(element[0], $input);
        })
    });
    jQuery(bind_element).find(".visual-editor").each(function(){
        var element = jQuery(this);
        var toolbar = element.data('toolbar');
        var language = element.data('language');
        var opts = {
            cssClass : 'el-rte',
            lang     : language,
            toolbar  : toolbar,
            doctype  : '<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">', //'<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN">',
            cssfiles : ['/images/jquery/plugins/elrte-1.3/css/elrte-inner.css']
        }
        element.elrte(opts);
    });
    jQuery(bind_element).find(".ajaxAutoCompleter").each(function(){
        var element = jQuery(this);
        var ajaxUrl = element.data("ajax-url");
        var showDescription = element.data("show-description");
        var defaultMinLength = element.data("default-minlength");
        var defaultDelay = element.data("default-delay");
        ajaxAutoCompleter(ajaxUrl, showDescription, defaultMinLength, defaultDelay);
    });
    jQuery(bind_element).find("[data-inplace-editor-url]").each(function(){
        var element = jQuery(this);
        var id =  element.attr("id");
        var url = element.data("inplace-editor-url");
        var params = element.data("inplace-editor-params");
        ajaxInPlaceEditDisplayField(id, url, (new Function("return " + params + ";")()));
    });
    jQuery(bind_element).on("click", "[data-dialog-url]", function(){
        var element = jQuery(this);
        var url = element.data("dialog-url");
        var title = element.data("dialog-title");
        var width = element.data("dialog-width");
        var height = element.data("dialog-height");
        var params = element.data("dialog-params");
        var dialogContainer = jQuery('<div/>');
        dialogContainer.dialog({
            autoOpen: false,
            title: title,
            height: height,
            width: width,
            modal: true,
            closeOnEscape: true,
            open: function() {
                jQuery.ajax({
                    url: url,
                    type: "POST",
                    data: params,
                    success: function(data) {
                        dialogContainer.html(data);
                        bindObservers(dialogContainer);
                    }
                });
            }
        });
        dialogContainer.dialog("open");
    });
    jQuery(bind_element).on("click", "[data-confirm-message]", function(e){
        var element = jQuery(this);
        var confirmMessage = element.data("confirm-message");
        if (!confirm(confirmMessage)) {
            e.preventDefault();
        }
    });
    jQuery(bind_element).find("[data-lookup-presentation]").each(function(){
        var element = jQuery(this);
        var form = element.form();
        var formName = form.attr("name");
        if (!formName) {
            console.log("Developer: For lookups to work you must provide a form name!");
            return;
        }
        var presentation = element.data("lookup-presentation");
        var ajaxEnabled = element.data("lookup-ajax-enabled");
        var ajaxUrl = element.data("lookup-ajax-url");
        var showDescription = element.data("lookup-show-description");

        var defaultMinLength = element.data("lookup-default-minlength");
        var defaultDelay = element.data("lookup-default-delay");
        var descriptionFieldName = element.data("lookup-description-field");

        if (presentation && presentation === "window" && descriptionFieldName) {
            var descriptionField = form.find("input[name=" + descriptionFieldName+"]").get(0);
            var fieldFormname = element.data("lookup-field-formname");
            var lookupArgs = element.data("lookup-args");
            var args = [];
            if (lookupArgs){
                jQuery.each(lookupArgs.split(', '), function (index, value) {
                    var argValue = form.find("input[name=" + value + "]").get(0).value;
                    args.push(argValue);
                });
            }

            var argList = [this, descriptionField, fieldFormname, presentation];
            argList = argList.concat(args);
            var $a = jQuery("<a/>").on("click", function () {
                call_fieldlookup3.apply(null, argList);
            });
            element.parent().append($a);

        } else if (presentation && presentation === "window"){
            var fieldFormname = element.data("lookup-field-formname");
            var lookupArgs = element.data("lookup-args");
            var args = [];
            if (lookupArgs){
                jQuery.each(lookupArgs.split(', '), function (index, value) {
                    var argValue = form.find("input[name=" + value + "]").get(0).value;
                    args.push(argValue);
                });
            }

            var argList = [this, fieldFormname, presentation];
            argList = argList.concat(args);
            var $a = jQuery("<a/>").on("click", function () {
                call_fieldlookup2.apply(null, argList);
            });
            element.parent(".field-lookup").append($a);
        } else {
            var lookupOptionalTarget = element.data("lookup-optional-target");
            var dialogOptionalTarget = undefined;
            if (lookupOptionalTarget)
                dialogOptionalTarget = form.find("input[name=" + element.data("lookup-optional-target")+"]").get(0);
            var lookupArgs = element.data("lookup-args");
            var args = [];
            if (lookupArgs){
                jQuery.each(lookupArgs.split(', '), function (index, value) {
                    var argElement = form.find("input[name=" + value + "]").get(0);
                    args.push(argElement);
                });
            }

            var options = {
                requestUrl : element.data("lookup-request-url"),
                inputFieldId : this.id,
                dialogTarget : this,
                dialogOptionalTarget : dialogOptionalTarget,
                formName : formName,
                width : element.data("lookup-width"),
                height : element.data("lookup-height"),
                position : element.data("lookup-position"),
                modal : element.data("lookup-modal"),
                ajaxUrl : ajaxUrl,
                showDescription : showDescription,
                presentation : presentation,
                defaultMinLength : defaultMinLength,
                defaultDelay : defaultDelay,
                args : args
            };
            new Lookup(options).init();
        }
        element.siblings(".clearField").on("click", function (){
            element.val("");
            jQuery('#' + element.attr('id') + '_lookupDescription').html('');
        });

        if (ajaxEnabled && presentation && presentation == "window"){
            ajaxAutoCompleter(ajaxUrl, showDescription, defaultMinLength, defaultDelay);
        }
    });
    jQuery(bind_element).find("[data-focus-field]").each(function() {
        var element = jQuery(this);
        var focusField = element.data("focus-field");
        element.find("[name=" + focusField + "]").focus();
    });
    jQuery(bind_element).find(".requireValidation").each(function(){
        var element = jQuery(this);
        element.validate();
    });

    jQuery(bind_element).find(".date-time-picker").each(function(){
        var element = jQuery(this);
        var id = element.attr("id");
        var element_i18n = jQuery("#" + id + "_i18n");
        var shortDate = element.data("shortdate");
        //If language specific lib is found, use date / time converter else just copy the value fields
        if (Date.CultureInfo != undefined) {
            var initDate = element.val();
            if (initDate != "") {
                var dateFormat;
                var ofbizTime;
                if (shortDate) {
                    dateFormat = Date.CultureInfo.formatPatterns.shortDate;
                    ofbizTime = "yyyy-MM-dd";
                } else {
                    dateFormat = Date.CultureInfo.formatPatterns.shortDate + " " + Date.CultureInfo.formatPatterns.longTime;
                    ofbizTime = "yyyy-MM-dd HH:mm:ss"
                }
                // The JS date parser doesn't understand the dot before ms in the date/time string. The ms here should be always 000
                if (initDate.indexOf('.') != -1) {
                    initDate = initDate.substring(0, initDate.indexOf('.'));
                }
                element.val(initDate);
                var dateObj = Date.parseExact(initDate, ofbizTime);
                var formatedObj = dateObj.toString(dateFormat);
                element_i18n.val(formatedObj);
            }

            element.change(function() {
                var value = element.val();
                var dateFormat;
                var ofbizTime;
                if (shortDate) {
                    dateFormat = Date.CultureInfo.formatPatterns.shortDate;
                    ofbizTime = "yyyy-MM-dd";
                } else {
                    dateFormat = Date.CultureInfo.formatPatterns.shortDate + " " + Date.CultureInfo.formatPatterns.longTime;
                    ofbizTime = "yyyy-MM-dd HH:mm:ss"
                }
                var newValue = ""
                if (value != "") {
                    var dateObj = Date.parseExact(value, ofbizTime);
                    newValue = dateObj.toString(dateFormat);
                }
                element_i18n.val(newValue);
            });

            element_i18n.change(function() {
                var value = element_i18n.val();
                var dateFormat;
                var ofbizTime;
                if (shortDate) {
                    dateFormat = Date.CultureInfo.formatPatterns.shortDate;
                    ofbizTime = "yyyy-MM-dd";
                } else {
                    dateFormat = Date.CultureInfo.formatPatterns.shortDate + " " + Date.CultureInfo.formatPatterns.longTime;
                    ofbizTime = "yyyy-MM-dd HH:mm:ss"
                }
                var newValue = "";
                var dateObj = Date.parseExact(this.value, dateFormat);
                if (value != "" && dateObj !== null) {
                    newValue = dateObj.toString(ofbizTime);
                } else { // invalid input
                    element_i18n.val("");
                }
                element.val(newValue);
            });
        } else {
            //fallback if no language specific js date file is found
            element.change(function() {
                element_i18n.val(this.value);
            });
            element_i18n.change(function() {
                element.val(this.value);
            });
        }
        if (shortDate) {
            element.datepicker({
                showWeek: true,
                showOn: 'button',
                buttonImage: '',
                buttonText: '',
                buttonImageOnly: false,
                dateFormat: 'yy-mm-dd'
            })
        } else {
            element.datetimepicker({
                showSecond: true,
                // showMillisec: true,
                timeFormat: 'HH:mm:ss',
                stepHour: 1,
                stepMinute: 1,
                stepSecond: 1,
                showOn: 'button',
                buttonImage: '',
                buttonText: '',
                buttonImageOnly: false,
                dateFormat: 'yy-mm-dd'
            })
        }
    });
    jQuery(bind_element).on("click", ".fieldgroup  li.collapsed, .fieldgroup  li.expanded", function(e){
        var element = jQuery(this);
        var collapsibleAreaId =  element.data("collapsible-area-id");
        var expandToolTip =  element.data("expand-tooltip");
        var collapseToolTip =  element.data("collapse-tooltip");
        toggleCollapsiblePanel(element, collapsibleAreaId, expandToolTip, collapseToolTip);
    });
}

/* SelectAll: This utility can be used when we need to use parent and child box combination over any page. Here is the list of tasks it will do:
   1. Check/ Uncheck child checkboxes when parent checkbox checked.
   2. Check/ Uncheck parent checkbox when child checkboxes checked.
*/

// addSelectAllObserver: This function will add observers to checkboxes which belongs to select all functionality.
function addSelectAllObserver(action_checkbox) {
    var form_fields = jQuery(action_checkbox).getForm().getFormFields(),
        all_child = form_fields.filter(":checkbox:not(:disabled):not(.selectAll)"),
        select_child = all_child.filter(".selectAllChild").size() > 0 ? all_child.filter(".selectAllChild") : all_child,
        parent_checkbox = form_fields.filter(".selectAll"),
        is_parent = action_checkbox.is(".selectAll");

    if (is_parent) {
        // Check/ Uncheck child checkboxes when parent checked.
        jQuery(select_child).attr("checked", function() {
            return parent_checkbox.is(":checked");
        });
    } else {
        // Check/ Uncheck parent checkbox when child checkboxes checked.
        if (select_child.size() > 0) {
            var all_checked = true;

            select_child.each(function() {
                if (all_checked) {
                    all_checked = all_checked && jQuery(this).is(":checked");
                }
            });

            // Below code is for checking or unchecking the parent checkbox if all its dependent child checkbox is checked.
            if (all_checked) {
                parent_checkbox.attr("checked", "checked");
            } else {
                parent_checkbox.removeAttr("checked");
            }
        }
    }
}

// getFormFields: This utility function return all form fields (inside and outside form)
jQuery.fn.getFormFields = function() {
    var id = jQuery(this).attr("id");
    if (id === undefined) {
        return jQuery(this).find(":input");
    } else {
        return jQuery.merge(jQuery(this).find(":input"), jQuery(":input[form=" + id + "]"));
    }
}

// getForm: This utility function return form of the field.
jQuery.fn.getForm = function() {
    var form_id = jQuery(this).attr("form");
    // Get closest form if no form id specified else get the form using id.
    if (form_id === undefined) {
        return jQuery(this).closest("form");
    } else {
        return jQuery("#" + form_id);
    }
}

function removeSelectedDefault() {
    removeSelected("selectAllForm");
}
function removeSelected(formName) {
    var cform = document[formName];
    cform.removeSelected.value = true;
    cform.submit();
}

// highlight the selected row(s)

function highlightRow(e,rowId){
    var currentClassName = document.getElementById(rowId).className;
    if (e.checked) {
        if (currentClassName == '' ) {
            document.getElementById(rowId).className = 'selected';
        } else if (currentClassName == 'alternate-row') {
            document.getElementById(rowId).className = 'alternate-rowSelected';
        }
    } else {
        if (currentClassName == 'selected') {
            document.getElementById(rowId).className = '';
        } else if (currentClassName == 'alternate-rowSelected') {
            document.getElementById(rowId).className = 'alternate-row';
        }
    }
}

function highlightAllRows(e, halfRowId, formName){
    var cform = document[formName];
    var len = cform.elements.length;
    for (var i = 0; i < len; i++) {
        var element = cform.elements[i];
        if (element.name.substring(0, 10) == "_rowSubmit") {
            highlightRow(e, halfRowId+element.name.substring(13));
        }
    }
}

// popup windows functions

function popUp(url, name, height, width) {
    popupWindow = window.open(url, name, 'location=no,scrollbars,width=' + width + ',height=' + height);
}
function popUpSmall(url, name) {
    popUp(url, name, '300', '450');
}

// Post a form from a pop up using the parent window
function doPostViaParent(formName) {
    var theForm = document[formName];
    var newForm = theForm.cloneNode(true);
    var hiddenDiv = document.createElement('div');
    hiddenDiv.style.visibility = 'hidden';
    hiddenDiv.appendChild(newForm);
    window.opener.document.body.appendChild(hiddenDiv);
    newForm.submit();
    window.opener.focus();
}
// From a child window, navigate the parent window to the supplied url
function doGetViaParent(url) {
    window.opener.location = url;
    window.opener.focus();
}

// hidden div functions

function getStyleObject(objectId) {
    if (document.getElementById && document.getElementById(objectId)) {
        return document.getElementById(objectId).style;
    } else if (document.all && document.all(objectId)) {
        return document.all(objectId).style;
    } else if (document.layers && document.layers[objectId]) {
        return document.layers[objectId];
    } else {
        return false;
    }
}
function changeObjectVisibility(objectId, newVisibility) {
    var styleObject = getStyleObject(objectId);
    if (styleObject) {
        styleObject.visibility = newVisibility;
        return true;
    } else {
        return false;
    }
}

// To use this in a link use a URL like this: javascript:confirmActionLink('You want to delete this example?', 'deleteExample?exampleId=${exampleId}')
function confirmActionLink(msg, newLocation) {
    if (msg == null) {
        msg = "Are you sure you want to do this?";
    }
    var agree = confirm(msg);
    if (agree) {
        if (newLocation != null) location.replace(newLocation);
    }
}

// To use this in a link use a URL like this: javascript:confirmActionFormLink('You want to update this example?', 'updateExample')
function confirmActionFormLink(msg, formName) {
    if (msg == null) {
        msg = "Are you sure you want to do this?";
    }
    var agree = confirm(msg);
    if (agree) {
        if (formName != null) document.forms[formName].submit();
    }
}

// ===== Ajax Functions - based on jQuery.js ===== //

/** Update an area (HTML container element).
  * @param areaId The id of the HTML container to update
  * @param target The URL to call to update the HTML container
  * @param targetParams The URL parameters
*/

function ajaxUpdateArea(areaId, target, targetParams) {
    if (areaId == "window") {
        targetUrl = target + "?" + targetParams.replace('?','');
        window.location.assign(targetUrl);
        return;
    }
    waitSpinnerShow();
    jQuery.ajax({
        url: target,
        type: "POST",
        data: targetParams,
        success: function(data) {
            jQuery("#" + areaId).html(data);
            waitSpinnerHide();
        },
        error: function(data) {waitSpinnerHide()}
    });
}

/** Update multiple areas (HTML container elements).
  * @param areaCsvString The area CSV string. The CSV string is a flat array in the
  * form of: areaId, target, target parameters [, areaId, target, target parameters...].
*/
function ajaxUpdateAreas(areaCsvString) {
    /*split all parameters separate by comma, the regExp manage areaId,target,param1=a&param2={b,c,d}&param3=e as three parameters*/
    var regExpArea = /,(?=(?:[^{}]*{[^{}]*})*[^{}]*$)/g; 
    var areaArray = areaCsvString.split(regExpArea);
    var numAreas = parseInt(areaArray.length / 3);
    for (var i = 0; i < numAreas * 3; i = i + 3) {
        var areaId = areaArray[i];
        var target = areaArray[i + 1];
        var targetParams = areaArray[i + 2];
        // Remove the ? and the anchor flag from the parameters
        // not nice but works
        targetParams = targetParams.replace('#','');
        targetParams = targetParams.replace('?','');
        ajaxUpdateArea(areaId, target, targetParams);
    }
}

/** Update an area (HTML container element) periodically.
  * @param areaId The id of the HTML container to update
  * @param target The URL to call to update the HTML container
  * @param targetParams The URL parameters
  * @param interval The update interval, in seconds.
*/
function ajaxUpdateAreaPeriodic(areaId, target, targetParams, interval) {
    var intervalMillis = interval * 1000;
    jQuery.fjTimer({
        interval: intervalMillis,
        repeat: true,
        tick: function(container, timerId){
            jQuery.ajax({
                url: target,
                type: "POST",
                data: targetParams,
                success: function(data) {
                    jQuery("#" + areaId).html(data);
                    waitSpinnerHide();
                },
                error: function(data) {waitSpinnerHide()}
            });

        }
    });
}

/** Submit request, update multiple areas (HTML container elements).
  * @param target The URL to call to update the HTML container
  * @param targetParams The URL parameters
  * @param areaCsvString The area CSV string. The CSV string is a flat array in the
  * form of: areaId, target, target parameters [, areaId, target, target parameters...].
*/
function ajaxSubmitRequestUpdateAreas(target, targetParams, areaCsvString) {
    updateFunction = function(transport) {
        ajaxUpdateAreas(areaCsvString);
    }
    jQuery.ajax({
        url: target,
        type: "POST",
        data: targetParams,
        success: updateFunction()
    });
}

/** Submit form, update an area (HTML container element).
  * @param form The form element
  * @param areaId The id of the HTML container to update
  * @param submitUrl The URL to call to update the HTML container
*/
function submitFormInBackground(form, areaId, submitUrl) {
    submitFormDisableSubmits(form);
    updateFunction = function() {
        jQuery("#" + areaId).load(submitUrl);
    }
    jQuery.ajax({
        url: jQuery(form).attr("action"),
        data: jQuery(form).serialize(),
        success: updateFunction()
    });
}

/** Submit form, update multiple areas (HTML container elements).
 * @param form The form element
 * @param areaCsvString The area CSV string. The CSV string is a flat array in the
 * form of: areaId, target, target parameters [, areaId, target, target parameters...].
*/
function ajaxSubmitFormUpdateAreas(form, areaCsvString) {
   waitSpinnerShow();
   hideErrorContainer = function() {
       jQuery('#content-messages').html('');
       jQuery('#content-messages').removeClass('errorMessage').fadeIn('fast');
   }
   updateFunction = function(data) {
       if (data._ERROR_MESSAGE_LIST_ != undefined || data._ERROR_MESSAGE_ != undefined) {
           if (!jQuery('#content-messages').length) {
              //add this div just after app-navigation
              if(jQuery('#content-main-section')){
                  jQuery('#content-main-section' ).before('<div id="content-messages" onclick="hideErrorContainer()"></div>');
              }
           }
           jQuery('#content-messages').addClass('errorMessage');
          if (data._ERROR_MESSAGE_LIST_ != undefined && data._ERROR_MESSAGE_ != undefined) {
              jQuery('#content-messages' ).html(data._ERROR_MESSAGE_LIST_ + " " + data._ERROR_MESSAGE_);
          } else if (data._ERROR_MESSAGE_LIST_ != undefined) {
              jQuery('#content-messages' ).html(data._ERROR_MESSAGE_LIST_);
          } else {
              jQuery('#content-messages' ).html(data._ERROR_MESSAGE_);
          }
          showjGrowl();
       } else {
           if (jQuery('#content-messages').length) {
               jQuery('#content-messages').html('');
               jQuery('#content-messages').removeClass('errorMessage').fadeIn("fast");
           }
           ajaxUpdateAreas(areaCsvString);
       }
       waitSpinnerHide();
   }

   jQuery.ajax({
       type: "POST",
       url: jQuery("#" + form).attr("action"),
       data: jQuery("#" + form).serialize(),
       success: function(data) {
               updateFunction(data);
       }
   });
}

/** Enable auto-completion for text elements, with a possible span of tooltip class showing description.
 * @param areaCsvString The area CSV string. The CSV string is a flat array in the
 * form of: areaId, target, target parameters [, areaId, target, target parameters...].
*/

function ajaxAutoCompleter(areaCsvString, showDescription, defaultMinLength, defaultDelay, formName) {
    ajaxAutoCompleter(areaCsvString, showDescription, defaultMinLength, defaultDelay, formName, null);
}
function ajaxAutoCompleter(areaCsvString, showDescription, defaultMinLength, defaultDelay, formName, args) {
    var areaArray = areaCsvString.replace(/&amp;/g, '&').split(",");
    var numAreas = parseInt(areaArray.length / 3);

    for (var i = 0; i < numAreas * 3; i = i + 3) {
        var initUrl = areaArray[i + 1];
        if (initUrl.indexOf("?") > -1)
            var url = initUrl + "&" + areaArray[i + 2];
        else
            var url = initUrl + "?" + areaArray[i + 2];
        var div = areaArray[i];
        // create a separated div where the result JSON Opbject will be placed
        if ((jQuery("#" + div + "_auto")).length < 1) {
            jQuery("<div id='" + div + "_auto'></div>").insertBefore("#" + areaArray[i]);
        }

        jQuery("#" + div).autocomplete({
            minLength: defaultMinLength,
            delay: defaultDelay,
            source: function(request, response){
                var queryArgs = {"term": request.term};
                if (typeof args == "object" && jQuery.isArray(args)) {
                     for (var i = 0; i < args.length; i++) {
                         queryArgs["parm" + i] = jQuery(args[i]).val();
                     }
                }
                jQuery.ajax({
                    url: url,
                    type: "post",
                    data: queryArgs,
                    beforeSend: function (jqXHR, settings) {
                        //If LAST_AUTOCOMP_REF is not null means an existing ajax auto-completer request is in progress, so need to abort them to prevent inconsistent behavior of autocompleter
                        if (LAST_AUTOCOMP_REF != null && LAST_AUTOCOMP_REF.readyState != 4) {
                            var oldRef = LAST_AUTOCOMP_REF;
                            oldRef.abort();
                            //Here we are aborting the LAST_AUTOCOMP_REF so need to call the response method so that auto-completer pending request count handle in proper way
                            response( [] );
                        }
                        LAST_AUTOCOMP_REF= jqXHR;
                    },
                    success: function(data) {
                        // reset the autocomp field
                        autocomp = undefined;

                        jQuery("#" + div + "_auto").html(data);

                        if (typeof autocomp != 'undefined') {
                            jQuery.each(autocomp, function(index, item){
                                item.label = jQuery("<div>").html(item.label).text();
                            })
                            // autocomp is the JSON Object which will be used for the autocomplete box
                            response(autocomp);
                        }
                    },
                    error: function(xhr, reason, exception) {
                        if(exception != 'abort') {
                            alert("An error occurred while communicating with the server:\n\n\nreason=" + reason + "\n\nexception=" + exception);
                        }
                    }
                });
            },
            select: function(event, ui){
                //jQuery("#" + areaArray[0]).html(ui.item);
                jQuery("#" + areaArray[0]).val(ui.item.value); // setting a text field
                if (showDescription && (ui.item.value != undefined && ui.item.value != '')) {
                    setLookDescription(areaArray[0], ui.item.label, areaArray[2], formName, showDescription)
                }
            }
        });
        if (showDescription) {
            var lookupDescriptionLoader = new lookupDescriptionLoaded(areaArray[i], areaArray[i + 1], areaArray[i + 2], formName);
            lookupDescriptionLoader.update();
            jQuery("#" + areaArray[i]).on('change lookup:changed', function(){
                lookupDescriptionLoader.update();
            });
        }
    }
}

function setLookDescription(textFieldId, description, params, formName, showDescription){
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
                var start = description.indexOf(' ');
                if (start != -1 && description.indexOf('<script type="application/javascript">') == -1) {
                    description = description.substring(start);
                }
            }
            tooltipElement = jQuery("#" + textFieldId + '_lookupDescription')
            if (!tooltipElement.length) {
                tooltipElement = jQuery("<span id='" + textFieldId + "_lookupDescription' class='tooltip'></span>");
            }
            tooltipElement.html(description);
            lookupWrapperEl.append(tooltipElement);
        }
    }
}

/** Enable auto-completion for drop-down elements.*/

function ajaxAutoCompleteDropDown() {
    jQuery.widget( "ui.combobox", {
        _create: function() {
            var self = this;
            var select = this.element.hide(),
                selected = select.children( ":selected" ),
                value = selected.val() ? selected.text() : "";
            var input = jQuery( "<input>" )
                .insertAfter( select )
                .val( value )
                .autocomplete({
                    delay: 0,
                    minLength: 0,
                    source: function( request, response ) {
                        var matcher = new RegExp( jQuery.ui.autocomplete.escapeRegex(request.term), "i" );
                        response( select.children( "option" ).map(function() {
                            var text = jQuery( this ).text();
                            if ( this.value && ( !request.term || matcher.test(text) ) )
                                return {
                                    label: text.replace(
                                        new RegExp(
                                            "(?![^&;]+;)(?!<[^<>]*)(" +
                                            jQuery.ui.autocomplete.escapeRegex(request.term) +
                                            ")(?![^<>]*>)(?![^&;]+;)", "gi"
                                        ), "<b>$1</b>" ),
                                    value: text,
                                    option: this
                                };
                        }) );
                    },
                    select: function( event, ui ) {
                        ui.item.option.selected = true;
                        //select.val( ui.item.option.value );
                        self._trigger( "selected", event, {
                            item: ui.item.option
                        });
                    },
                    change: function( event, ui ) {
                        var element = jQuery(this);
                        if (element.data('other-field-name') != undefined) {
                            var otherField = (element.form()).find("input[name=" + element.data('other-field-name') + "]");
                            if (otherField != undefined) {
                                process_choice(element, jQuery(otherField));
                            }
                        }
                        if ( !ui.item ) {
                            var matcher = new RegExp( "^" + jQuery.ui.autocomplete.escapeRegex( jQuery(this).val() ) + "$", "i" ),
                                valid = false;
                            select.children( "option" ).each(function() {
                                if ( this.value.match( matcher ) ) {
                                    this.selected = valid = true;
                                    return false;
                                }
                            });
                            if ( !valid ) {
                                // remove invalid value, as it didn't match anything
                                jQuery( this ).val( "" );
                                select.val( "" );
                                return false;
                            }
                        }
                    }
                })
                //.addClass( "ui-widget ui-widget-content ui-corner-left" );

            input.data( "ui-autocomplete" )._renderItem = function( ul, item ) {
                return jQuery( "<li></li>" )
                    .data( "item.autocomplete", item )
                    .append( "<a>" + item.label + "</a>" )
                    .appendTo( ul );
            };

            jQuery( "<a>&nbsp;</a>" )
                .attr( "tabIndex", -1 )
                .attr( "title", "Show All Items" )
                .insertAfter( input )
                .button({
                    icons: {
                        primary: "ui-icon-triangle-1-s"
                    },
                    text: false
                })
                .removeClass( "ui-corner-all" )
                .addClass( "ui-corner-right ui-button-icon" )
                .click(function() {
                    // close if already visible
                    if ( input.autocomplete( "widget" ).is( ":visible" ) ) {
                        input.autocomplete( "close" );
                        return;
                    }

                    // pass empty string as value to search for, displaying all results
                    input.autocomplete( "search", "" );
                    input.focus();
                });
        }
    });

}


/** Toggle area visibility on/off.
 * @param link The <a> element calling this function
 * @param areaId The id of the HTML container to toggle
 * @param expandTxt Localized 'Expand' text
 * @param collapseTxt Localized 'Collapse' text
*/
function toggleCollapsiblePanel(link, areaId, expandTxt, collapseTxt){
   var container = jQuery("#" + areaId);
   var liElement = jQuery(link).is("li") ? jQuery(link) : jQuery(link).parents('li:first');
    if (liElement) {
      if (container.is(':visible')) {
        liElement.removeClass('expanded');
        liElement.addClass('collapsed');
        link.title = expandTxt;
      } else {
        liElement.removeClass('collapsed');
        liElement.addClass('expanded');
        link.title = collapseTxt;
      }
    }
   container.animate({opacity: 'toggle', height: 'toggle'}, "slow");
}

/** Toggle screenlet visibility on/off.
 * @param link The <a> element calling this function
 * @param areaId The id of the HTML container to toggle
 * @param expandTxt Localized 'Expand' text
 * @param collapseTxt Localized 'Collapse' text
*/
function toggleScreenlet(link, areaId, saveCollapsed, expandTxt, collapseTxt){
   toggleCollapsiblePanel(link, areaId, expandTxt, collapseTxt);
   var screenlet = jQuery(link).parents('div:eq(1)').attr('id');
   var title = jQuery(link).attr('title');
   if(title == expandTxt){
       var currentParam = screenlet + "_collapsed=false";
       var newParam = screenlet + "_collapsed=true";
       if(saveCollapsed=='true'){
           setUserLayoutPreferences('GLOBAL_PREFERENCES',screenlet+"_collapsed",'true');
       }
   } else {
       var currentParam = screenlet + "_collapsed=true";
       var newParam = screenlet + "_collapsed=false";
       if(saveCollapsed=='true'){
           setUserLayoutPreferences('GLOBAL_PREFERENCES',screenlet+"_collapsed",'false');
       }
   }
   var paginationMenus = jQuery('div.nav-pager');
   jQuery.each(paginationMenus, function(index, menu) {
       if (menu) {
           var childElements = jQuery(menu).find('a');
           for (var i = 0; i < childElements.length; i++) {
               if (childElements[i].href.indexOf("http") == 0) {
                   childElements[i].href = replaceQueryParam(childElements[i].href, currentParam, newParam);
               }
           }
           childElements = jQuery(menu).find('select');
           for (i = 0; i < childElements.length; i++) {
             //FIXME: Not able to understand the purpose of below line, as href is not valid attribute of select element.
             //if (childElements[i].href.indexOf("location.href") >= 0) {
               if (childElements[i].value.indexOf("location.href") >= 0) {
                   Element.extend(childElements[i]);
                   childElements[i].writeAttribute("onchange", replaceQueryParam(childElements[i].readAttribute("onchange"), currentParam, newParam));
               }
           }
       }
   });
}

/** In Place Editor for display elements
  * @param element The id of the display field
  * @param url The request to be called to update the display field
  * @param options Options to be passed to Ajax.InPlaceEditor
  * https://cwiki.apache.org/confluence/display/OFBIZ/Enhancing+Display+Widget+to+use+Ajax.InPlaceEditor
*/

function ajaxInPlaceEditDisplayField(element, url, options) {
    var jElement = jQuery("#" + element);
    jElement.mouseover(function() {
        jQuery(this).css('background-color', 'rgb(255, 255, 153)');
    });

    jElement.mouseout(function() {
        jQuery(this).css('background-color', 'transparent');
    });

    jElement.editable(function(value, settings){
        // removes all line breaks from the value param, because the parseJSON Function can't work with line breaks
        value = value.replace(/\n/g, " ");
        value = value.replace(/\"/g,"&quot;");

        var resultField = jQuery.parseJSON('{"' + settings.name + '":"' + value + '"}');
        // merge both parameter objects together
        jQuery.extend(settings.submitdata, resultField);
        jQuery.ajax({
            type : settings.method,
            url : url,
            data : settings.submitdata,
            success : function(data) {
                // adding the new value to the field and make the modified field 'blink' a little bit to show the user that somethink have changed
                jElement.text(value).fadeOut(500).fadeIn(500).fadeOut(500).fadeIn(500).css('background-color', 'transparent');
            }
        });
    }, options);
}

// ===== End of Ajax Functions ===== //

function replaceQueryParam(queryString, currentParam, newParam) {
    var result = queryString.replace(currentParam, newParam);
    if (result.indexOf(newParam) < 0) {
        if (result.indexOf("?") < 0) {
            result = result + "?" + newParam;
        } else if (result.endsWith("#")) {
            result = result.replace("#", "&" + newParam + "#");
        } else if (result.endsWith(";")) {
            result = result.replace(";", " + '&" + newParam + "';");
        } else {
            result = result + "&" + newParam;
        }
    }
    return result;
}

function submitFormDisableSubmits(form) {
    for (var i=0;i<form.length;i++) {
        var formel = form.elements[i];
        if (formel.type == "submit") {
            submitFormDisableButton(formel);
            var formName = form.name;
            var formelName = formel.name;
            var timeoutString = "submitFormEnableButtonByName('" + formName + "', '" + formelName + "')";
            var t = setTimeout(timeoutString, 1500);
        }
    }
}

function showjGrowl(showAllLabel, collapseLabel, hideAllLabel, jGrowlPosition, jGrowlWidth, jGrowlHeight, jGrowlSpeed) {

    var contentMessages = jQuery("#content-messages");
    if (contentMessages.length) {
        jQuery("#content-messages").hide();
        var errMessage = jQuery("#content-messages").html();
        var classEvent = "";
        var classList = jQuery("#content-messages").attr('class').split(/\s+/);
        var stickyValue = false;
        jQuery(classList).each(function(index) {
            var localClass = classList[index];
            if(localClass == "eventMessage" || localClass == "errorMessage" ){
                classEvent = localClass + "JGrowl";
            }
        });
        if (classEvent == "errorMessageJGrowl") {
            stickyValue = true;
        }

        if (errMessage == null || errMessage == "" || errMessage == undefined ) {
            // No Error Message Information is set, Error Msg Box can't be created
            return;
        }
        $.jGrowl.defaults.closerTemplate = '<div class="closeAllJGrowl">'+hideAllLabel+'</div>';
        if (jGrowlPosition !== null && jGrowlPosition !== undefined) $.jGrowl.defaults.position = jGrowlPosition;
        $.jGrowl(errMessage, { theme: classEvent, sticky: stickyValue,
            beforeOpen: function(e,m,o){
                if (jGrowlWidth !== null && jGrowlWidth !== undefined) $(e).width( jGrowlWidth+'px' );
                if (jGrowlHeight !== null  && jGrowlHeight !== undefined) $(e).height( jGrowlHeight+'px' );
            },
            afterOpen: function(e,m) {
                jQuery(".jGrowl-message").readmore({
                    moreLink: '<a href="#" style="display: block; width: auto; padding: 0px;text-align: right; margin-top: 10px; color: #ffffff; font-size: 0.8em">'+showAllLabel+'</a>',
                    lessLink: '<a href="#" style="display: block; width: auto; padding: 0px;text-align: right; margin-top: 10px; color: #ffffff; font-size: 0.8em">'+collapseLabel+'</a>',

                    maxHeight: 75
                });
            },
            speed:jGrowlSpeed
        });
        contentMessages.remove();
    }
}


// prevents doubleposts for <submit> inputs of type "button" or "image"
function submitFormDisableButton(button) {
    if (button.form.action != null && button.form.action.length) {
        button.disabled = true;
    }
    button.className = button.className + " disabled";
    button.value = button.value + "*";
}

function submitFormEnableButtonByName(formName, buttonName) {
    var form = document[formName];
    var button = form.elements[buttonName];
    submitFormEnableButton(button);
}
function submitFormEnableButton(button) {
    button.disabled = false;
    button.className = button.className.substring(0, button.className.length - " disabled".length);
    button.value = button.value.substring(0, button.value.length - 1);
}

/**
 * Expands or collapses all groups of one portlet
 *
 * @param bool <code>true</code> to expand, <code>false</code> otherwise
 * @param portalPortletId The id of the portlet
 */
function expandAllP(bool, portalPortletId) {
    jQuery('#scrlt_'+portalPortletId+' .fieldgroup').each(function() {
        var titleBar = jQuery(this).children('.fieldgroup-title-bar'), body = jQuery(this).children('.fieldgroup-body');
        if (titleBar.children().length > 0 && body.is(':visible') != bool) {
            toggleCollapsiblePanel(titleBar.find('a'), body.attr('id'), 'expand', 'collapse');
        }
    });
}

/**
 * Expands or collapses all groups of the page
 *
 * @param bool <code>true</code> to expand, <code>false</code> otherwise
 */
function expandAll(bool) {
    jQuery('.fieldgroup').each(function() {
        var titleBar = jQuery(this).children('.fieldgroup-title-bar'), body = jQuery(this).children('.fieldgroup-body');
        if (titleBar.children().length > 0 && body.is(':visible') != bool) {
            toggleCollapsiblePanel(titleBar.find('li.collapsed, li.expanded'), body.attr('id'), 'expand', 'collapse');
        }
    });
}

//calls ajax request for storing user layout preferences
function setUserLayoutPreferences(userPrefGroupTypeId, userPrefTypeId, userPrefValue){
    jQuery.ajax({
        url:'ajaxSetUserPreference',
        type: "POST",
        data: ({userPrefGroupTypeId: userPrefGroupTypeId, userPrefTypeId: userPrefTypeId, userPrefValue: userPrefValue}),
        success: function(data) {}
    });
}

function waitSpinnerShow() {
    jSpinner = jQuery("#wait-spinner");
    if (!jSpinner.length) return

    bdy = document.body;
    lookupLeft = (bdy.offsetWidth / 2) - (jSpinner.width() / 2);
    scrollOffY = jQuery(window).scrollTop();
    winHeight = jQuery(window).height();
    lookupTop = (scrollOffY + winHeight / 2) - (jSpinner.height() / 2);

    jSpinner.css("display", "block");
    jSpinner.css("left", lookupLeft + "px");
    jSpinner.css("top", lookupTop + "px");
    jSpinner.show();
}

function waitSpinnerHide() {
    jQuery("#wait-spinner").hide()
}

/**
 * Reads the requiered uiLabels from the uiLabelXml Files
 * @param requiredLabels JSON Object {resource : [label1, label2 ...], resource2 : [label1, label2, ...]}
 * @return JSON Object
 */
function getJSONuiLabels(requiredLabels, callback) {
    var requiredLabelsStr = JSON.stringify(requiredLabels)

    if (requiredLabels != null && requiredLabels != "") {
        jQuery.ajax({
            url: "getJSONuiLabelArray",
            type: "POST",
            data: {"requiredLabels" : requiredLabelsStr},
            complete: function(data) {
                callback(data);
            }
        });
    }
}
/**
 * Read the requiered uiLabel from the uiLabelXml Resource
 * @param uiResource String
 * @param errUiLabel String
 * @returns String with Label
 */
function getJSONuiLabel(uiResource, errUiLabel) {
    requiredLabel = {};
    requiredLabel[uiResource] = errUiLabel;

    var returnVal = "";
    var requiredLabelStr = JSON.stringify(requiredLabel)

    if (requiredLabel != null && requiredLabel != "") {
        jQuery.ajax({
            url: "getJSONuiLabel",
            type: "POST",
            data: {"requiredLabel" : requiredLabelStr},
            success: function(data) {
                returnVal = data;
            }
        });
    }
    return returnVal[arguments[0]];
}

/**
 * Opens an alert alert box with an i18n error message
 * @param errBoxTitleResource String - Can be empty
 * @param errBoxTitleLabel String - Can be empty
 * @param uiResource String - Required
 * @param errUiLabel String - Required
 */
function showErrorAlertLoadUiLabel(errBoxTitleResource, errBoxTitleLabel, uiResource, errUiLabel) {
    if (uiResource == null || uiResource == "" || uiResource == undefined || errUiLabel == null || errUiLabel == "" || errUiLabel == undefined) {
        // No Label Information are set, Error Msg Box can't be created
        return;
    }

    var labels = {};
    var useTitle = false;
    // title can only be set when the resource and the label are set
    if (errBoxTitleResource != null && errBoxTitleResource != "" && errBoxTitleLabel != null && errBoxTitleLabel != "") {
        // create the JSON Object
        if (errBoxTitleResource == uiResource) {
            labels[errBoxTitleResource] = [errBoxTitleLabel, errUiLabel];
        } else {
            labels[errBoxTitleResource] = [errBoxTitleLabel];
            labels[uiResource] = [errUiLabel];
        }
        useTitle = true;
    } else {
        labels[uiResource] = [errUiLabel];
    }
    // request the labels
    getJSONuiLabels(labels, function(result){
        labels = result.responseJSON;
    });

    var errMsgBox = jQuery("#contentarea").after(jQuery("<div id='errorAlertBox'></div>"));

    if (errMsgBox.length) {
        errMsgBox.dialog({
            modal: true,
            title: function() {
                if (useTitle) {
                    return labels[errBoxTitleResource][0]
                } else {
                    return ""
                }
            },
            open : function() {
                var positionInArray = 0;
                if (errBoxTitleResource == uiResource) {
                    positionInArray = 1;
                }
                errMsgBox.html(labels[uiResource][positionInArray]);
            },
            buttons: {
                Ok: function() {
                    errMsgBox.remove();
                    jQuery( this ).dialog( "close" );
                }
            }
        });
    } else {
      alert(labels[uiResource][0]);
    }
}

/**
 * Opens an alert alert box. This function is for a direct call from the ftl files where you can direcetly resolve youre labels
 * @param errBoxTitle String - Can be empty
 * @param errMessage String - Required - i18n Error Message
 */
function showErrorAlert(errBoxTitle, errMessage) {
    if (errMessage == null || errMessage == "" || errMessage == undefined ) {
        // No Error Message Information is set, Error Msg Box can't be created
        return;
    }

    var errMsgBox = jQuery("#contentarea").after(jQuery("<div id='errorAlertBox'>" + errMessage + "</div>"));

    if (errMsgBox.length) {
        errMsgBox.dialog({
            modal: true,
            title: errBoxTitle,
            buttons: {
                Ok: function() {
                    errMsgBox.remove();
                    jQuery( this ).dialog( "close" );
                }
            }
        });
    }
}

/**
 * Submit the pagination request
 * @param obj The DOM object of pagination anchor or select element
 * @param url The pagination URL
 */
function submitPagination(obj, url) {
    if (obj.getAttribute("data-lookupajax") == "true" && typeof window.lookupPaginationAjaxRequest == "function") {
        lookupPaginationAjaxRequest(url, (obj.tagName == "SELECT" ? "select" : "link"));
        return false;
    }
    if (url.length > 2000) {
        var request = url.substring(0, url.indexOf("?"));
        var params = url.substring(url.indexOf("?")+1, url.length);
        var paramsArray = params.split("&");
        var form = document.createElement("form");
        form.setAttribute("method", "post");
        form.setAttribute("action", request);
        for (var i = 0; i < paramsArray.length; i ++) {
            var param = paramsArray[i];
            if (param!= "" && param.indexOf("=") > 0) {
                var keyValue = param.split("=");
                var hiddenField = document.createElement("input");
                hiddenField.setAttribute("type", "hidden");
                hiddenField.setAttribute("name", keyValue[0]);
                hiddenField.setAttribute("value", keyValue[1]);
                form.appendChild(hiddenField);
            }
        }
        document.body.appendChild(form);
        form.submit();
        return false;
    } else {
        if (obj.tagName == "SELECT" || obj.tagName == "INPUT") {
            location.href = url;
            return false;
        } else {
            obj.href = url;
            return true;
        }
    }
}
function loadJWT() {
    var JwtToken = "";
    jQuery.ajax({
        url: "loadJWT",
        type: "POST",
        async: false,
        dataType: "text",
        success: function(response) {
            JwtToken = response;
        },
        error: function(textStatus, errorThrown){
            alert('Failure, errorThrown: ' + errorThrown);
        }
    });
    return JwtToken;
}

function sendJWT(targetUrl) {
    var redirectUrl = targetUrl;
    var jwtToken = loadJWT(); 
    if (jwtToken != null && jwtToken != "") {
        jQuery.ajax({
            url: targetUrl,
            async: false,
            type: 'POST',
            xhrFields: {withCredentials: true},
            headers: {"Authorization" : "Bearer " + jwtToken},
            success: function(){
                window.location.assign(redirectUrl);
            }
        });
    }
}
