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
var CURRENT_LOOKUP = null;

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
var lookups =[];

function call_fieldlookup(target, viewName, formName, viewWidth, viewheight) {
    var fieldLookup = new fieldLookup1(target);
    if (! viewWidth) viewWidth = 350;
    if (! viewheight) viewheight = 200;
    fieldLookup.popup(viewName, formName, viewWidth, viewheight);
}
function call_fieldlookupLayer(target, viewName, lookupWidth, lookupHeight, lookupPosition, fadeBackground) {
    if (isEmpty(target) || isEmpty(viewName)) {
        return lookup_error("Lookup can't be created, one of these variables is missing: target=" + target + " viewName=" + viewName);
    }

    if (CURRENT_LOOKUP != null) {
        if (CURRENT_LOOKUP.parentTarget == target) {
            CURRENT_LOOKUP.closeLookup();
            return;
        }
    }
    var fieldLookupPopup = new FieldLookupPopup(target, viewName, lookupWidth, lookupHeight, lookupPosition, fadeBackground);
    fieldLookupPopup.showLookup();
    this.target = target;
}

function call_fieldlookupLayer3(target, target2, viewName, lookupWidth, lookupHeight, lookupPosition, fadeBackground) {
    if (isEmpty(target) || isEmpty(target2) || isEmpty(viewName)) {
        return lookup_error("Lookup can't be created, one of these variables is missing: target=" + target + " target2=" + target2 + " viewName=" + viewName);
    }

    if (CURRENT_LOOKUP != null) {
        if (CURRENT_LOOKUP.parentTarget == target) {
            CURRENT_LOOKUP.closeLookup();
            return;
        }
    }
    var fieldLookupPopup = new FieldLookupPopup(target, viewName, lookupWidth, lookupHeight, lookupPosition, fadeBackground);
    fieldLookupPopup.showLookup();
    this.target = target;
    this.target2 = target2;
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
    if (! obj_target)
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
    this.popup = lookup_popup1;
    this.popup2 = lookup_popup2;
    // validate input parameters
    if (! obj_target)
    return lookup_error("Error calling the field lookup: no target control specified");
    if (obj_target.value == null)
    return lookup_error("Error calling the field lookup: parameter specified is not valid target control");
    target = obj_target;
    // validate input parameters
    if (! obj_target2)
    return lookup_error("Error calling the field lookup: no target2 control specified");
    if (obj_target2.value == null)
    return lookup_error("Error calling the field lookup: parameter specified is not valid target2 control");
    target2 = obj_target2;
    
    
    // register in global collections
    //this.id = lookups.length;
    //lookups[this.id] = this;
}

function lookup_popup1(view_name, form_name, viewWidth, viewheight) {
    var obj_lookupwindow = window.open(view_name + '?formName=' + form_name + '&id=' + this.id, '_blank', 'width=' + viewWidth + ',height=' + viewheight + ',scrollbars=yes,status=no,resizable=yes,top=' + my + ',left=' + mx + ',dependent=yes,alwaysRaised=yes');
    obj_lookupwindow.opener = window;
    obj_lookupwindow.focus();
}
function lookup_popup2(view_name) {
    var argString = "";
    if (this.args != null) {
        if (this.args.length > 2) {
            for (var i = 2; i < this.args.length; i++) {
                argString += "&parm" + (i - 2) + "=" + this.args[i];
            }
        }
    }
    var sep = "?";
    if (view_name.indexOf("?") >= 0) {
        sep = "&";
    }
    var obj_lookupwindow = window.open(view_name + sep + 'id=' + this.id + argString, '_blank', 'width=700,height=550,scrollbars=yes,status=no,resizable=yes,top=' + my + ',left=' + mx + ',dependent=yes,alwaysRaised=yes');
    obj_lookupwindow.opener = window;
    obj_lookupwindow.focus();
}
function lookup_error(str_message) {
    alert(str_message);
    return null;
}

/*************************************
* Fieldlookup Class & Methods
*************************************/
//if ESC is pressed, remove draged portlet + hoverDiv
function key_event(evt) {
    if (evt.keyCode == 27) {
        //removes the hover div after the portlet is moved to another position
        if (CURRENT_LOOKUP != null) {
            lookupHide();
        }
    }
}

//checks if a lookup can be closed if the mouse is clicking beside the lookup layer
function close_on_click(evt) {
    if (! Event.element(evt).descendantOf(CURRENT_LOOKUP.divRef)) {
        lookupHide();
    }
}

/**
* Class to create a lookup layer
* target - target where the value should be passed
* viewName - view name which will be opened
* lookupWidth - layer width i.e. 500px, 27% ... [default: 700px]
* lookupHeight - layer height i.e. 500px, 27% ... [default: 550px]
* position - normal (under the target field), center (layer is centered) [default: normal] -- !work still in process
*/
var FieldLookupPopup = Class.create({
    initialize: function (target, viewName, lookupWidth, lookupHeight, position, fadeBackground) {

        //removes a existing Lookup
        if (CURRENT_LOOKUP != null) {
            CURRENT_LOOKUP.removeLayer();
        }
        //fade the background if the flag is set
        if (fadeBackground != "false") {
            this.createFadedBackground();
        }
        //set dimension isn't set, set default parameters
        if (isEmpty(lookupWidth)) {
            lookupWidth = '700px';
        }
        this.lookupWidth = lookupWidth;

        if (isEmpty(lookupHeight)) {
            lookupHeight = '550px';
        }
        this.lookupHeight = lookupHeight;

        //set the parent target to create later the lookup as child of this element
        this.parentTarget = target;
        this.viewName = viewName;
        this.position = position;

        this.createElement();

        //set observe events for mouse and keypress
        Event.observe(document, "keypress", key_event);
        Event.observe(document, "mousedown", close_on_click);

        //set global reference
        CURRENT_LOOKUP = this;
    },
    
    showLookup: function () {
        if ($('fieldLookup') != null) {
            Effect.Appear('fieldLookup', {duration: 0.3});
        }
    },
    
    closeLookup: function () {
        if ($('fieldLookup') != null) {
            Effect.Fade('fieldLookup', {duration: 0.3});
        }
        // removes the layer after fading
        window.setTimeout('CURRENT_LOOKUP.removeLayer()', 400);
    },
    
    createElement: function () {
        var parent = this.parentTarget;
        
        //creates the lookup dom element
        var lookupDiv = new Element('DIV', {
            id: "fieldLookup",
            style: "width: " + this.lookupWidth + "; height: " + this.lookupHeight + ""
        });
        lookupDiv.setAttribute("class", "fieldLookup");
        lookupDiv.setAttribute("className", "fieldLookup");
        //IE7 Hack
        
        //creates lookupHeader
        var headerDiv = new Element('DIV', {
            id: "fieldLookupHeader"
        });
        headerDiv.setAttribute("class", "fieldLookupHeader");
        headerDiv.setAttribute("className", "fieldLookupHeader");
        // IE 7 Hack
        
        //create close Button
        var closeButton = new Element('SPAN', {
        });
        var closeButtonImg = new Element('DIV', {
            onClick: "lookupHide()"
        });
        closeButtonImg.onclick = function () {
            lookupHide();
        };
        
        closeButton.appendChild(closeButtonImg);
        headerDiv.appendChild(closeButton);
        lookupDiv.appendChild(headerDiv);
        
        //get the lookup from an anjax request
        this.loadContent(lookupDiv);
        
        lookupDiv.style.display = "none";
        //creates the div as child of the form element (parent --> input field; parentNode --> form)
        this.pn = parent.parentNode;

        //set the layer position
        this.setPosition(lookupDiv);

        this.divRef = lookupDiv;

        //make layer draggable
        this.makeDraggable(lookupDiv);

        //make the window resiable
        this.makeResizeable(lookupDiv);
    },

    loadContent: function (lookupDiv) {
        new Ajax.Request(this.viewName, {
            onSuccess: function (transport) {
                var lookupContent = transport.responseText;
                lookupDiv.insert({
                    bottom: "<div id='fieldLookupContent'>" + lookupContent + "</div>"
                });
                modifySubmitButton();
            }
        });
    },
    
    createFadedBackground: function(){
        //remove the faded Background if exists
        var fb = $('fadedBackground')
        if (fb != null) {
            fb.parentNode.removeChild(fb);
        }

        var pageSize = this.getPageSize();
        var fadedBackground = new Element ('DIV', {
            id: "fadedBackground",
            style: "width: " + pageSize[0] + "px; height: " + pageSize[1] + "px;"
            });

        document.body.appendChild(fadedBackground);
    },
    
    setPosition: function (lookupDiv) {
        //set layer position
        var bdy = document.body;
        if (this.position == "center") {
            bdy.appendChild(lookupDiv);
            var dimensions = lookupDiv.getDimensions();
            lookupLeft = (bdy.offsetWidth / 2) - (dimensions.width / 2);
            var scrollOffY = document.viewport.getScrollOffsets().top;
            var winHeight = document.viewport.getHeight();
            lookupTop = (scrollOffY + winHeight / 2) - (dimensions.height / 2);
            lookupDiv.style.left = lookupLeft + "px";
            lookupDiv.style.top = lookupTop + "px";
        } else if (this.position == "right") {
            bdy.appendChild(lookupDiv);
            var dimensions = lookupDiv.getDimensions();
            lookupLeft = (bdy.offsetWidth) - (dimensions.width + 5);
            var scrollOffY = document.viewport.getScrollOffsets().top;
            var winHeight = document.viewport.getHeight();
            lookupTop = (scrollOffY + winHeight / 2) - (dimensions.height / 2);
            lookupDiv.style.left = lookupLeft + "px";
            lookupDiv.style.top = lookupTop + "px";
        } else if (this.position == "left") {
            bdy.appendChild(lookupDiv);
            var dimensions = lookupDiv.getDimensions();
            lookupLeft = 5;
            var scrollOffY = document.viewport.getScrollOffsets().top;
            var winHeight = document.viewport.getHeight();
            lookupTop = (scrollOffY + winHeight / 2) - (dimensions.height / 2);
            lookupDiv.style.left = lookupLeft + "px";
            lookupDiv.style.top = lookupTop + "px";
        } else if (this.position == "topright") {
            bdy.appendChild(lookupDiv);
            var dimensions = lookupDiv.getDimensions();
            lookupLeft = (bdy.offsetWidth) - (dimensions.width + 5);
            var scrollOffY = document.viewport.getScrollOffsets().top;
            lookupTop = 5 + scrollOffY;
            lookupDiv.style.left = lookupLeft + "px";
            lookupDiv.style.top = lookupTop + "px";
        } else if (this.position == "topleft") {
            bdy.appendChild(lookupDiv);
            var dimensions = lookupDiv.getDimensions();
            lookupLeft = 5;
            var scrollOffY = document.viewport.getScrollOffsets().top;
            lookupTop = 5 + scrollOffY;
            lookupDiv.style.left = lookupLeft + "px";
            lookupDiv.style.top = lookupTop + "px";
        } else if (this.position == "topcenter") {
            bdy.appendChild(lookupDiv);
            var dimensions = lookupDiv.getDimensions();
            lookupLeft = (bdy.offsetWidth / 2) - (dimensions.width / 2);
            var scrollOffY = document.viewport.getScrollOffsets().top;
            lookupTop = 5 + scrollOffY;
            lookupDiv.style.left = lookupLeft + "px";
            lookupDiv.style.top = lookupTop + "px";
        } else {
            //for 'normal', empty etc.
            if (this.pn != null) {
                // IE Fix
                this.pn.appendChild(lookupDiv);
            }
        }
    },

    makeDraggable: function (lookupDiv) {
        this.loopupDrag = new Draggable(lookupDiv, {
            handle: 'fieldLookupHeader', revert: false, ghosting: false
        });
    },

    makeResizeable: function (lookupDiv) {
        new Resizeable(lookupDiv, {
            top: 0,
            left: 50,
            resize: function (el) {
                $('fieldLookupContent').setStyle({
                    width: "100%",
                    height: "90%"
                });
            }
        });
    },
    
    removeLayer: function () {
        this.loopupDrag.destroy();
        Element.stopObserving(document, "keypress");
        Element.stopObserving(document, "mousedown");
        CURRENT_LOOKUP.divRef.parentNode.removeChild(CURRENT_LOOKUP.divRef);
        //remove the faded Background if exists
        var fb = $('fadedBackground')
        if (fb != null){
            fb.parentNode.removeChild(fb);
        }
        CURRENT_LOOKUP = null;
        this.target = null;
    },
    
    getPageSize: function() {

        var xScroll, yScroll;

        if (window.innerHeight && window.scrollMaxY) {
            xScroll = window.innerWidth + window.scrollMaxX;
            yScroll = window.innerHeight + window.scrollMaxY;
        } else if (document.body.scrollHeight > document.body.offsetHeight){ // all but Explorer Mac
            xScroll = document.body.scrollWidth;
            yScroll = document.body.scrollHeight;
        } else { // Explorer Mac...would also work in Explorer 6 Strict, Mozilla and Safari
            xScroll = document.body.offsetWidth;
            yScroll = document.body.offsetHeight;
        }

        var windowWidth, windowHeight;

        if (self.innerHeight) {    // all except Explorer
            if (document.documentElement.clientWidth){
                windowWidth = document.documentElement.clientWidth;
            } else {
                windowWidth = self.innerWidth;
            }
            windowHeight = self.innerHeight;
        } else if (document.documentElement && document.documentElement.clientHeight) { // Explorer 6 Strict Mode
            windowWidth = document.documentElement.clientWidth;
            windowHeight = document.documentElement.clientHeight;
        } else if (document.body) { // other Explorers
            windowWidth = document.body.clientWidth;
            windowHeight = document.body.clientHeight;
        }

        // for small pages with total height less then height of the viewport
        if (yScroll < windowHeight){
            pageHeight = windowHeight;
        } else {
            pageHeight = yScroll;
        }

        // for small pages with total width less then width of the viewport
        if (xScroll < windowWidth){
            pageWidth = xScroll;
        } else {
            pageWidth = windowWidth;
        }

        return [pageWidth,pageHeight];
    }
});

function lookupHide() {
    if (CURRENT_LOOKUP != null) {
        CURRENT_LOOKUP.closeLookup();
    }
}

/**
* returns true if a Stirng is empty
* @param value - String value
* @return
*/
function isEmpty(value) {
    if (value == null || value == "") {
        return true;
    }
    return false;
}

/**
* Modify the Submit Button for the new lookup layer
* @return
*/
function modifySubmitButton() {
    /* changes form/submit behavior for Lookup Layer */
    if ($('fieldLookup')) {
        //find the lookup form
        var forms = $('fieldLookup').getElementsByTagName('form');
        for (var i = 0; i < forms.length; i++) {
            if (! isEmpty(forms[i].getAttribute('id'))) {
                var lookupForm = $(forms[i].getAttribute('id'));
            }
        }
        
        //diable the form action
        var formAction = lookupForm.getAttribute('action');
        lookupForm.setAttribute('action', '');
        
        //modify the form submit button
        for (var i = 0; i < lookupForm.length; i++) {
            var ele = lookupForm.elements[i];
            if ((ele.getAttribute('type')) == "submit") {
                ele.style.display = "none";
                var txt = ele.value;
                var submit = new Element('A', {
                });
                submit.onclick = function () {
                    lookupFormAjaxRequest(formAction, lookupForm.getAttribute('id'));
                    return false;
                };
                submit.setAttribute("class", "smallSubmit");
                  submit.setAttribute("className", "smallSubmit"); // IE 7 Hack
                submit.href = "";
                
                var textNode = document.createTextNode(txt);
                submit.appendChild(textNode);
                ele.parentNode.appendChild(submit);
                Event.observe(document, "keypress", function (evt) {
                    if (Event.KEY_RETURN == evt.keyCode) {
                        lookupFormAjaxRequest(formAction, lookupForm.getAttribute('id'));
                    }
                });
                ele.parentNode.removeChild(ele);
                //modifay nav-pager
                var navPager = $('fieldLookup').getElementsByClassName("nav-pager");
                if (navPager.length > 0) {
                    
                    for (var j = 0; j < navPager.length; j++) {
                        var eleChild = navPager[j].getElementsByTagName("ul")[0].getElementsByTagName("li");
                        for (var k = 0; k < eleChild.length; k++) {
                            var link = eleChild[k].getElementsByTagName("A");
                            var select = eleChild[k].getElementsByTagName("SELECT");
                            
                            if (link.length > 0) {
                                link[0].href = "javascript:lookupPaginationAjaxRequest('" + link[0].href + "', '" + lookupForm.getAttribute('id') + "', 'link')";
                            } else if (select.length > 0) {
                                try {
                                    var oc = select[0].getAttribute("onchange");
                                      if((typeof oc) == "function"){ // IE6/7 Fix
                                        oc = oc.toString();
                                        var ocSub = oc.substring((oc.indexOf('=') + 2),(oc.length - 4));
                                        var searchPattern = /'\+this.value\+'/g;
                                        var searchPattern2 = /'\+this.valu/g;
                                        
                                        if (searchPattern.test(ocSub)) {
                                            var viewSize = select[0].value;
                                            var spl = ocSub.split(searchPattern);
                                            select[0].onchange = function () {
                                                lookupPaginationAjaxRequest(spl[0] + this.value + spl[1], lookupForm.getAttribute('id'), 'select');
                                            };
                                        } else if (searchPattern2.test(ocSub)) {
                                            ocSub = ocSub.replace(searchPattern2, "");
                                            if (searchPattern.test(ocSub)) {
                                                ocSub.replace(searchPattern, viewSize);
                                            }
                                            select[0].onchange = function () {
                                                lookupPaginationAjaxRequest(ocSub + this.value, lookupForm.getAttribute('id'), 'select');
                                            };
                                        }
                                    } else {
                                        var ocSub = oc.substring((oc.indexOf('=') + 1),(oc.length - 1));
                                        select[0].setAttribute("onchange", "lookupPaginationAjaxRequest(" + ocSub + ", '" + lookupForm.getAttribute('id') + "')");
                                    }
                                }
                                catch (ex) {
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
* Create an ajax request to get the search results
* @param formAction - action target
* @param form - formId
* @return
*/
function lookupFormAjaxRequest(formAction, form) {
    new Ajax.Request(formAction, {
        method: 'post',
        parameters: $(form).serialize(), requestHeaders: {
            Accept: 'application/json'
        },
        onSuccess: function (transport) {
            var formRequest = transport.responseText;
            $('fieldLookupContent').remove();
            $('fieldLookup').insert({
                bottom: "<div id='fieldLookupContent'>" + formRequest + "</div>"
            });
            modifySubmitButton();
        }
    });
}

function lookupPaginationAjaxRequest(navAction, form, type) {
    if (type == 'link') {
        navAction = navAction.substring(0, navAction.length - 1);
    }
    new Ajax.Request(navAction, {
        method: 'get',
        requestHeaders: {
            Accept: 'application/json'
        },
        onSuccess: function (transport) {
            var formRequest = transport.responseText;
            $('fieldLookupContent').remove();
            $('fieldLookup').insert({
                bottom: "<div id='fieldLookupContent'>" + formRequest + "</div>"
            });
            modifySubmitButton();
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

var bkColor = "yellow";
function setSourceColor(src) {
    if (src != null)
    src.style.backgroundColor = bkColor;
}
// function passing selected value to calling window
function set_value(value) {
    if (! obj_caller) return;
    setSourceColor(obj_caller.target);
    obj_caller.target.value = value;
    if (obj_caller.target.onchange != null) {    
       obj_caller.target.onchange();                       
   }
    closeLookup();
}
// function passing selected value to calling window
function set_values(value, value2) {
    set_value(value);
    if (! obj_caller.target2) return;
    if (obj_caller.target2 == null) return;
    setSourceColor(obj_caller.target2);
    obj_caller.target2.value = value2;
    if (obj_caller.target2.onchange != null) {     
        obj_caller.target2.onchange();                    
    }
}
function set_multivalues(value) {
    obj_caller.target.value = value;
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
    if (window.opener != null) {
        window.close();
    } else {
        parent.lookupHide();
    }
}