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
var mx, my;
var ACTIVATED_LOOKUP = null;
var LOOKUP_DIV = null;
var INITIALLY_COLLAPSED = null;
var COLLAPSE_SEQUENCE_NUMBER = 1999;

var target = null;
var target2 = null;
var targetW = null;
var lookups = [];

function getViewNameWithSeparator(view_name) {
    var sep = "?";
    if (view_name.indexOf("?") >= 0) {
        sep = "&";
    }
    return view_name + sep;
}

function lookup_error(str_message) {
    var CommonErrorMessage2 = getJSONuiLabel("CommonUiLabels", "CommonErrorMessage2");
    showErrorAlert(CommonErrorMessage2, str_message);
}

function lookup_popup1(view_name, form_name, viewWidth, viewheight) {
    var obj_lookupwindow = window.open(getViewNameWithSeparator(view_name) + 'formName=' + form_name + '&presentation=' + this.presentation
            + '&id=' + this.id, '_blank', 'width=' + viewWidth + ',height=' + viewheight + ',scrollbars=yes,status=no,resizable=yes,top='
            + my + ',left=' + mx + ',dependent=yes,alwaysRaised=yes');
    obj_lookupwindow.opener = window;
    obj_lookupwindow.focus();
}

function lookup_popup2(view_name) {
    var argString = "";
    if (this.args != null) {
        if (this.args.length > 2) {
            var i;
            for (i = 2; i < this.args.length; i++) {
                argString += "&parm" + (i - 3) + "=" + this.args[i];
            }
        }
    }

    var obj_lookupwindow = window.open(getViewNameWithSeparator(view_name) + 'presentation=' + this.presentation + '&id=' + this.id
            + argString, '_blank', 'width=900,height=700,scrollbars=yes,status=no,resizable=yes,top=' + my + ',left=' + mx
            + ',dependent=yes,alwaysRaised=yes');
    obj_lookupwindow.opener = window;
    obj_lookupwindow.focus();
}

function fieldLookup1(obj_target, args, presentation) {
    this.args = args;
    this.presentation = presentation;
    // passing methods
    this.popup = lookup_popup1;
    this.popup2 = lookup_popup2;

    // validate input parameters
    if (!obj_target) {
        return lookup_error("Error calling the field lookup: no target control specified");
    }
    if (obj_target.value === null) {
        return lookup_error("Error calling the field lookup: parameter specified is not valid target control");
    }
    targetW = obj_target;
}

function fieldLookup2(obj_target, obj_target2, args, presentation) {
    this.args = args;
    this.presentation = presentation;
    // passing methods
    this.popup = lookup_popup1;
    this.popup2 = lookup_popup2;
    // validate input parameters
    if (!obj_target) {
        return lookup_error("Error calling the field lookup: no target control specified");
    }
    if (obj_target.value === null) {
        return lookup_error("Error calling the field lookup: parameter specified is not valid target control");
    }
    targetW = obj_target;
    // validate input parameters
    if (!obj_target2) {
        return lookup_error("Error calling the field lookup: no target2 control specified");
    }
    if (obj_target2.value === null) {
        return lookup_error("Error calling the field lookup: parameter specified is not valid target2 control");
    }
    target2 = obj_target2;
}

function call_fieldlookup3(target, target2, viewName, presentation) {
    var fieldLookup = new fieldLookup2(target, target2, arguments, presentation);
    fieldLookup.popup2(viewName);
}

function call_fieldlookup(target, viewName, formName, viewWidth, viewheight) {
    var fieldLookup = new fieldLookup1(target);
    if (!viewWidth) {
        viewWidth = 350;
    }
    if (!viewheight) {
        viewheight = 200;
    }
    fieldLookup.popup(viewName, formName, viewWidth, viewheight);
}

function call_fieldlookup2(target, viewName, presentation) {
    var fieldLookup = new fieldLookup1(target, arguments, presentation);
    fieldLookup.popup2(viewName);
}

function CollapsePanel(link, areaId) {
    var container, liElement;

    container = jQuery(areaId);
    liElement = jQuery(link).up('li');

    liElement.removeClassName('expanded');
    liElement.addClassName('collapsed');
    Effect.toggle(container, 'appear');
}

function initiallyCollapse() {
    if ((!LOOKUP_DIV) || (INITIALLY_COLLAPSED != "true")) {
        return;
    }

    var i, j, childEle, childElements, ul, slTitleBar, slTitleBars = LOOKUP_DIV.getElementsByClassName('screenlet-title-bar');
    for (i in slTitleBars) {
        slTitleBar = slTitleBars[i];
        ul = slTitleBar.firstChild;
        if ((typeof ul) != 'object') {
            continue;
        }

        childElements = ul.childNodes;
        for (j in childElements) {
            if (childElements[j].className === 'expanded' || childElements[j].className === 'collapsed') {
                break;
            }
        }

        childEle = childElements[j].firstChild;
        new CollapsePanel(childEle, 'lec' + COLLAPSE_SEQUENCE_NUMBER);
        break;
    }
}

function initiallyCollapseDelayed() {
    setTimeout("initiallyCollapse()", 400);
}

/*******************************************************************************
 * Lookup Object
 ******************************************************************************/
var Lookup = function(options) {
    var _newInputBoxId, _lookupId, _inputBox, _lookupContainer, _backgroundCloseClickEvent;

    options = {
        requestUrl : options.requestUrl || "",
        inputFieldId : options.inputFieldId || "",
        dialogTarget : options.dialogTarget || "",
        dialogOptionalTarget : options.dialogOptionalTarget || "",
        formName : options.formName || "",
        width : options.width,
        height : options.height,
        position : options.position,
        modal : options.modal || "true",
        ajaxUrl : options.ajaxUrl || "",
        showDescription : options.showDescription || "",
        presentation : options.presentation || "layer",
        defaultMinLength : options.defaultMinLength || "",
        defaultDelay : options.defaultDelay || "",
        args : options.args || ""
    }

    function _init() {
        _lookupId = GLOBAL_LOOKUP_REF.createNextKey();
        _modifyContainer();
        _createAjaxAutoComplete();

        _lookupContainer = jQuery("#" + _lookupId);
        var dialogOpts = _createDialogOptions(_lookupContainer);

        // init Dialog and register
        // create an object with all Lookup Informationes that are needed
        var dialogRef = _lookupContainer.dialog(dialogOpts);

        // setting up global variabels, for external access
        this.inputBoxId = _inputBox.id;
        this.lookupId = _lookupId;
        this.formName = options.formName;
        this.target = null;
        this.presentation = options.presentation;
        this.showDescription = (options.showDescription == "true") ? true : false;
        if (options.dialogOptionalTarget != null) {
            this.target2 = null;
        }
        this.prevLookup = null;
        this.dialogRef = dialogRef;

        // write external settings in global window manager
        GLOBAL_LOOKUP_REF.setReference(_lookupId, this);

        _addOpenEvent(dialogRef);
    }

    function _modifyContainer() {
        _inputBox = document.getElementById(options.inputFieldId);
        _newInputBoxId = _lookupId + "_" + options.inputFieldId;
        _inputBox.id = _newInputBoxId;
        var parent = _inputBox.parentNode;

        var link = document.createElement('A');
        link.href = "javascript:void(0);";
        link.id = _lookupId + "_button";

        parent.appendChild(link);

        var hiddenDiv = document.createElement("DIV");
        hiddenDiv.id = _lookupId;
        hiddenDiv.css = "{display: none;}";
        hiddenDiv.title = "";

        parent.appendChild(hiddenDiv);
    }

    function _createAjaxAutoComplete() {
        if (options.ajaxUrl != "") {
            // write the new input box id in the ajaxUrl Array
            options.ajaxUrl = options.ajaxUrl.replace(options.ajaxUrl.substring(0, options.ajaxUrl.indexOf(",")), _newInputBoxId);
            new ajaxAutoCompleter(options.ajaxUrl, (options.showDescription == "true") ? true : false, options.defaultMinLength, options.defaultDelay,
                    options.formName, options.args);
        }
    }

    function _createDialogOptions(_lookupContainer) {
        var positioning = _positioning();

        var dialogOpts = {
            modal : (options.modal == "true") ? true : false,
            bgiframe : true,
            autoOpen : false,
            height : parseInt(options.height),
            width : parseInt(options.width),
            position : positioning,
            draggable : true,
            resizeable : true,
            open : _dialogOpen,
            close : _dialogClose
        };

        return dialogOpts;
    }

    function _positioning() {
        var positioning = null;
        if (options.position == "topleft") {
            positioning = [ 'left', 'top' ];
        } else if (options.position == "topcenter") {
            positioning = [ 'center', 'top' ];
        } else if (options.position == "topright") {
            positioning = [ 'right', 'top' ];
        } else if (options.position == "center") {
            positioning = 'center';
        } else if (options.position == "left") {
            positioning = 'left';
        } else if (options.position == "right") {
            positioning = 'right';
        } else {
            positioning = [ 'left', 'top' ];
        }

        return positioning;
    }

    function _dialogOpen(event, ui) {
        waitSpinnerShow();
        _lookupContainer.empty();

        var queryArgs = "presentation=" + options.presentation;
        if (typeof options.args == "object" && jQuery.isArray(options.args)) {
            for ( var i = 0; i < options.args.length; i++) {
                queryArgs += "&parm" + i + "=" + jQuery(options.args[i]).val();
            }
        }

        _lookupChaining();
        _addCloseEventForClickingOnBackgroundLayer();

        // load lookup data from server
        jQuery.ajax({
            type : "POST",
            url : options.requestUrl,
            data : queryArgs,
            timeout : AJAX_REQUEST_TIMEOUT,
            cache : false,
            dataFilter : function(data, dataType) {
                waitSpinnerHide();
                return data;
            },

            success : function(data) {
                _lookupContainer.html(data);
                new ButtonModifier(_lookupId).modifyLookupLinks();
            },

            error : function(xhr, reason, exception) {
                if (exception != 'abort') {
                    alert("An error occurred while communicating with the server:\n\n\nreason=" + reason + "\n\nexception=" + exception);
                }
                location.reload(true);
            }
        });
    }

    function _lookupChaining() {
        /*
         * set up the window chaining if the ACTIVATED_LOOKUP var is set there
         * have to be more than one lookup, before registrating the new lookup
         * we store the id of the old lookup in the preLookup variable of the
         * new lookup object. I.e. lookup_1 calls lookup_8, the lookup_8 object
         * need a reference to lookup_1, this reference is set here
         */

        var prevLookup = null
        if (ACTIVATED_LOOKUP) {
            prevLookup = ACTIVATED_LOOKUP;
        }

        _activateLookup(_lookupId);

        GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).prevLookup = prevLookup;
    }

    function _activateLookup(newAl) {
        if (ACTIVATED_LOOKUP != newAl) {
            ACTIVATED_LOOKUP = newAl;
        }
    }

    function _addCloseEventForClickingOnBackgroundLayer() {
        _backgroundCloseClickEvent = function() {
            if (ACTIVATED_LOOKUP && ACTIVATED_LOOKUP == _lookupId) {
                GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).dialogRef.dialog("close");
            }
        }

        jQuery(".ui-widget-overlay").click(_backgroundCloseClickEvent);
    }

    function _dialogClose() {
        jQuery(".ui-widget-overlay").unbind("click", _backgroundCloseClickEvent)

        var prevLookup = null;
        if (ACTIVATED_LOOKUP) {
            prevLookup = GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).prevLookup;
        }

        if (prevLookup) {
            _activateLookup(prevLookup);
        } else {
            ACTIVATED_LOOKUP = null;
        }
    }

    function _addOpenEvent(dialogReference) {
        jQuery("#" + _lookupId + "_button").click(function() {
            dialogReference.dialog("open");

            GLOBAL_LOOKUP_REF.getReference(_lookupId).target = jQuery(options.dialogTarget);
            if (options.dialogOptionalTarget != null) {
                GLOBAL_LOOKUP_REF.getReference(_lookupId).target2 = jQuery(options.dialogOptionalTarget);
            }
        });

    }

    return {
        init : _init
    }
};

/*******************************************************************************
 * Lookup Counter Object
 ******************************************************************************/
var FieldLookupCounter = function() {
    this.refArr = {};

    this.setReference = function(key, ref) {
        // if key doesn't exist in the array and
        var itm;
        for (itm in this.refArr) {
            if (itm == key) {
                prefix = key.substring(0, key.indexOf("_"));
                key = prefix + "_" + key;
                this.refArr["" + key + ""] = ref;
                return this.refArr[key];
            }
        }
        this.refArr["" + key + ""] = ref;
        return this.refArr[key];
    };

    this.getReference = function(key) {
        // when key does not exist return null?
        return this.refArr[key] != null ? this.refArr[key] : null;
    };

    this.getLastReference = function() {
        return (this.countFields() - 1) + "_lookupId";
    }

    this.createNextKey = function() {
        return this.countFields() + "_lookupId";
    };

    this.countFields = function() {
        var count = 0;
        jQuery.each(this.refArr, function(itm) {
            count++;
        });
        return count;
    };

    this.removeReference = function(key) {
        // deletes the Array entry (doesn't effect the array length)
        delete this.refArr[key];
    };

};
var GLOBAL_LOOKUP_REF = new FieldLookupCounter();

/*******************************************************************************
 * Button Modifier Object
 ******************************************************************************/
var ButtonModifier = function(lookupDiv) {

    function _modifyLookupLinks() {
        if (!lookupDiv) {
            return;
        }

        _modifyCollapseable();

        _modifySubmitButton();

        _modifyPagination();

        _modifyResultTable();
    }

    function _modifyCollapseable() {

        var slTitleBars = jQuery("#" + lookupDiv + " .screenlet-title-bar");

        jQuery.each(slTitleBars, function(index) {
            var slTitleBar = slTitleBars[index];
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

            _getNextCollapseSeq();
            var childEle = childElements[j].firstChild;

            childEle.setAttribute('onclick', "javascript:toggleScreenlet(this, 'lec" + COLLAPSE_SEQUENCE_NUMBER
                    + "', 'true', 'Expand', 'Collapse');");
            childEle.href = "javascript:void(0);"
            jQuery(slTitleBar).next('div').attr('id', 'lec' + COLLAPSE_SEQUENCE_NUMBER);

        });
    }

    function _getNextCollapseSeq() {
        COLLAPSE_SEQUENCE_NUMBER++;
        return COLLAPSE_SEQUENCE_NUMBER;
    }

    function _modifySubmitButton() {
        var lookupForm = jQuery("#" + lookupDiv + " form:first");

        // set new form name and id
        var oldFormName = lookupForm.attr("name");
        lookupForm.attr("name", "form_" + lookupDiv);
        lookupForm.attr("id", "form_" + lookupDiv);
        lookupForm = jQuery("#form_" + lookupDiv);

        // set new links for lookups
        var newLookups = jQuery("#" + lookupDiv + " .field-lookup");

        var formAction = lookupForm.attr("action");

        // remove the form action
        lookupForm.attr("action", "");
        var input = jQuery("#" + lookupDiv + " input[type=submit]").css({
            display : "block"
        });

        // remove the original input button and replace with a new one
        var txt = input.attr("value");
        (input.parent()).append(jQuery("<button/>", {
            id : "lookupSubmitButton",
            href : "javascript:void(0);",
            click : function() {
                lookupFormAjaxRequest(formAction, lookupForm.attr("id"));
                return false;
            },
            text : txt
        }));

        input.remove();
    }

    function _modifyPagination() {
        // modify nav-pager
        var navPagers = jQuery("#" + lookupDiv + " .nav-pager a");
        jQuery.each(navPagers, function(navPager) {
            var onClickEvent = navPagers[navPager].onclick;
            navPagers[navPager].onclick = function(){
                this.setAttribute("data-lookupajax", "true");
                onClickEvent.apply(this);
            }
        });

        var navPagersSelect = jQuery("#" + lookupDiv + " .nav-pager select");
        jQuery.each(navPagersSelect, function(navPager) {
            var onChangeEvent = navPagersSelect[navPager].onchange;
            navPagersSelect[navPager].onchange = function(){
                this.setAttribute("data-lookupajax", "true");
                onChangeEvent.apply(this);
            }
        });
    }

    function _modifyResultTable() {
        var resultTable = jQuery("#" + lookupDiv + " #search-results table:first tbody");
        var tableChilds = resultTable.children();

        jQuery.each(tableChilds, function(tableChild) {
            var childElements = jQuery(tableChilds[tableChild]);
            var tableRows = childElements.children();

            jQuery.each(tableRows, function(cell) {
                var cellChilds = jQuery(tableRows[cell]).children();

                jQuery.each(cellChilds, function(child) {
                    if (cellChilds[child].tagName == "A") {
                        var link = cellChilds[child].href;
                        if (link.indexOf("javascript:set_") != -1) {
                            cellChilds[child].href = link;
                        } else {
                            var liSub = link.substring(link.lastIndexOf('/') + 1, (link.length));
                            cellChilds[child].href = "javascript:lookupAjaxRequest('" + liSub + "&presentation=layer')";
                        }
                    }
                });

            });

        });
    }

    return {
        modifyLookupLinks : _modifyLookupLinks
    }
}

/*******************************************************************************
 * Ajax Request Helper
 ******************************************************************************/
function lookupAjaxRequest(request) {
    // get request arguments
    var arg = request.substring(request.indexOf('?') + 1, (request.length));
    request = request.substring(0, request.indexOf('?'));
    lookupId = GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).lookupId;
    jQuery("#" + lookupId).load(request, arg, function(data) {
        new ButtonModifier(lookupId).modifyLookupLinks();
    });
}

function lookupFormAjaxRequest(formAction, form) {
    var lookupId = GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).lookupId;
    var data = jQuery("#" + form).serialize();
    data = data + "&presentation=" + GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).presentation;

    var screenletTitleBar = jQuery("#" + lookupId + " .screenlet-title-bar :visible:first");
    jQuery.ajax({
        url : formAction,
        type: "POST",
        data : data,
        beforeSend : function(jqXHR, settings) {
            // Here we append the spinner to the lookup screenlet and it will
            // shown till the ajax request is processed.
            var indicator = screenletTitleBar.find("span.indicator");
            // Check that if spinner is already in execution then don't add new
            // spinner
            if (indicator.length == 0) {
                jQuery("<span class='indicator'><img src='/images/ajax-loader.gif' alt='' /></span>").appendTo(screenletTitleBar);
            }
        },
        success : function(result) {
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
            new ButtonModifier(lookupId).modifyLookupLinks();
        }
    });
}

function lookupPaginationAjaxRequest(navAction, type) {
    var lookupDiv = (GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).divRef);
    var lookupContent = (GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).contentRef);

    var lookupId = GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).lookupId;
    var screenletTitleBar = jQuery("#" + lookupId + " .screenlet-title-bar :visible:first");

    jQuery.ajax({
        url : navAction.substring(0, navAction.indexOf("?")),
        type : "POST",
        data : navAction.substring(navAction.indexOf("?")+1, navAction.length),
        beforeSend : function(jqXHR, settings) {
            // Here we append the spinner to the lookup screenlet and it will
            // shown till the ajax request is processed.
            var indicator = screenletTitleBar.find("span.indicator");
            // Check that if spinner is already in execution then don't add new
            // spinner
            if (indicator.length == 0) {
                jQuery("<span class='indicator'><img src='/images/ajax-loader.gif' alt='' /></span>").appendTo(screenletTitleBar);
            }
        },
        success : function(result) {
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
            new ButtonModifier(lookupId).modifyLookupLinks();
        }
    });
}

/*******************************************************************************
 * This code inserts the value lookedup by a popup window back into the
 * associated form element
 ******************************************************************************/
var re_id = new RegExp('id=(\\d+)');
var num_id = (re_id.exec(String(window.location)) ? new Number(RegExp.$1) : 0);
var obj_caller;
try {
    obj_caller = (window.opener && window.opener.lookups? window.opener.lookups[num_id]: null);
    if (obj_caller == null && window.opener != null) {
        obj_caller = window.opener;
    } else if (obj_caller == null && window.opener == null) {
        obj_caller = parent;
    }
}
catch (err) {
    obj_caller = parent;
    if (console) {
        console.log(err);
    }
}

function setSourceColor(src) {
    if (src && src != null) {
        src.effect("highlight", {}, 3000);
    }
}

// function passing selected value to calling window, using only in the
// TimeDuration case
function set_duration_value(value) {
    if (GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP)) {
        obj_caller.target = GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).target;
    } else {
        obj_caller.target = jQuery(obj_caller.targetW);
    }
    var target = obj_caller.target;

    write_value(value, target);
    closeLookup();
}
// function passing selected value to calling window
function set_value(value) {
    if (GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP)) {
        obj_caller.target = GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).target;
    } else {
        obj_caller.target = jQuery(obj_caller.targetW);
    }

    var target = obj_caller.target;
    write_value(value, target);

    closeLookup();
}
// function passing selected value to calling window
function set_values(value, value2) {
    if (GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP)) {
        obj_caller.target = GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).target;
        obj_caller.target2 = GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).target2;
    } else {
        obj_caller.target = jQuery(obj_caller.targetW);
    }
    var target = obj_caller.target;
    var target2 = obj_caller.target2;
    write_value(value, target);
    write_value(value2, target2)
    var showDescription = GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP) ? GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).showDescription : false;
    if (showDescription) {
        setLookDescription(target.attr("id"), value + " " + value2, "", "", showDescription);
    }

    closeLookup();
}

function write_value(value, target) {
    if (target && target != null) {
        setSourceColor(target);
        target.val(value);
        target.trigger("lookup:changed");
    }
}

function set_multivalues(value) {
    obj_caller.target.value = value;
    field = jQuery("#" + target.attr('id'));
    field.trigger("lookup:changed");
    /*
     * If we decide to keep it (only used in Example, though it's needed also
     * for Themes and Languages but not the same way)
     */
    if (field.change != null) {
        field.click().change()
    }

    var thisForm = obj_caller.target.form;
    var evalString = "";

    if (arguments.length > 2) {
        for ( var i = 1; i < arguments.length; i = i + 2) {
            evalString = "setSourceColor(thisForm." + arguments[i] + ")";
            eval(evalString);
            evalString = "thisForm." + arguments[i] + ".value='" + arguments[i + 1] + "'";
            eval(evalString);
        }
    }
    closeLookup();
}

// close the window after passing the value
function closeLookup() {
    if (window.opener != null && GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP) == null) {
        window.close();
    } else {
        var lookupId = GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).lookupId;
        jQuery("#" + lookupId).dialog("close");
    }
}

/*******************************************************************************
 * Lookup Description Helper
 ******************************************************************************/
// load description for lookup fields
var lookupDescriptionLoaded = function(fieldId, url, params, formName) {
    this.init(fieldId, url, params, formName);
}
lookupDescriptionLoaded.prototype.init = function(fieldId, url, params, formName) {
    this.fieldId = fieldId;
    this.url = url;
    this.params = params;
    this.formName = formName;
}
lookupDescriptionLoaded.prototype.update = function() {
    var tooltipElement = jQuery("#" + this.fieldId + '_lookupDescription');
    if (tooltipElement.length) {// first remove current description
        tooltipElement.remove();
    }

    var indexOf = this.params.indexOf("searchValueFieldName");
    if (indexOf == -1) {
        return;
    }

    // actual server call
    var fieldName = this.params.substring(indexOf);
    fieldName = fieldName.substring(fieldName.indexOf("=") + 1);
    fieldObj = jQuery("input[name=" + fieldName + "]", jQuery("form[name=" + this.formName + "]"));
    if (fieldObj.val()) {
        var fieldSerialized = fieldObj.serialize();
        this.allParams = this.params + '&' + fieldSerialized + '&' + 'searchType=EQUALS';
        var _fieldId = this.fieldId;

        jQuery.ajax({
            url : this.url,
            type : "POST",
            data : this.allParams,
            success : function(result) {
                setLookDescription(_fieldId, result.trim(), "", "");
            }
        });
    }
}

// Needed because IE8 does not implement trim yet
if (typeof String.prototype.trim !== 'function') {
    String.prototype.trim = function() {
        return this.replace(/^\s+|\s+$/g, '');
    }
}