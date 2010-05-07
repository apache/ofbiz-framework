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

    var fieldLookupPopup = new FieldLookupPopup(target, viewName, lookupWidth, lookupHeight, lookupPosition, fadeBackground);
    fieldLookupPopup.showLookup();
    this.target = target;
}

function call_fieldlookupLayer3(target, target2, viewName, lookupWidth, lookupHeight, lookupPosition, fadeBackground) {
    if (isEmpty(target) || isEmpty(target2) || isEmpty(viewName)) {
        return lookup_error("Lookup can't be created, one of these variables is missing: target=" + target + " target2=" + target2 + " viewName=" + viewName);
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

var FieldLookupCounter = Class.create({
    initialize: function () {
        this.refArr = {};
    },
    
    setReference: function (key, ref) {
        //if key doesn't exist in the array and
        var bool = true;
        for (itm in this.refArr) {
            if (itm == key) {
                bool = false;
                break;
            }
        }

        if (bool) {
            this.refArr[key] = ref;
            return this.refArr[key];
        }
        
        return null;
    },
    
    getReference: function (key) {
        // when key does not exist return null?
        return this.refArr[key] != null ? this.refArr[key] : null;
    },
    
    createNextKey: function () {
        return this.countFields() + "_lookupId";
    },
    
    countFields: function () {
        var count = 0;
        for (itm in this.refArr) {
            count++;
        }
        
        return count;
    },
    
    removeReference: function (key) {
        // deletes the Array entry (doesn't effect the array length)
        delete this.refArr[key];
        
        // if all lookups closed, kill the referenze
        if (this.countFields() == 0) {
            ACTIVATED_LOOKUP = null;
        }
    }
    
});
var GLOBAL_LOOKUP_REF = new FieldLookupCounter;

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
        Event.observe(document, "keypress", this.key_event = this.key_event.bindAsEventListener(this));
        Event.observe(document, "mousedown", this.close_on_click = this.close_on_click.bindAsEventListener(this));
    },

    showLookup: function () {
        if (this.divRef != null) {
            Effect.Appear(this.divRef, {duration: 0.3});
        }
    },

    closeLookup: function () {
        if (this.divRef != null) {
            Effect.Fade(this.divRef, {duration: 0.3});
        }
        // removes the layer after fading
        window.setTimeout('' + this.removeLayer() + '', 400);
        
        //identify the next lookup
        lastLookup = GLOBAL_LOOKUP_REF.getReference((GLOBAL_LOOKUP_REF.countFields() - 1) + "_lookupId");
        if (lastLookup != null) {
            identifyLookup(lastLookup.globalRef);
        }
    },

    createElement: function () {
        var parent = this.parentTarget;
        var that = this;
        
        //set global reference
        this.globalRef = GLOBAL_LOOKUP_REF.createNextKey();
        if (GLOBAL_LOOKUP_REF.setReference(this.globalRef, this) == null){
            alert("Sorry this Reference: " + this.globalRef + " still exist. Can't create lookup");
        }
        
        //creates the lookup dom element
        var lookupDiv = new Element('DIV', {
            id: this.globalRef + "_fieldLookup",
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
            onClick: "GLOBAL_LOOKUP_REF.getReference('" + this.globalRef + "').closeLookup()"
        });
        closeButtonImg.onclick = function () {
            hideLookup();
        };
        
        closeButton.appendChild(closeButtonImg);
        headerDiv.appendChild(closeButton);
        lookupDiv.appendChild(headerDiv);
        
        //get the lookup from an anjax request
        this.contentRef = this.loadContent(lookupDiv);
        
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
        
        identifyLookup(this.globalRef);
    },

    close_on_click: function (evt) {
        if (! Event.element(evt).descendantOf(this.divRef)) {
            this.closeLookup(this);
        } else {
            identifyLookup(this.globalRef);
        }
    },

    key_event: function (evt) {
        if (evt.keyCode == 27) {
            //removes the hover div after the portlet is moved to another position
            this.closeLookup(this);
        }
    },

    loadContent: function (lookupDiv) {
        var lookupCont = new Element('DIV', {
            id: "fieldLookupContent"
        });

        new Ajax.Request(this.viewName, {
            parameters: { presentation : "layer" },
            onSuccess: function (transport) {
                var lookupContent = transport.responseText;
                lookupDiv.appendChild(lookupCont);
                lookupCont.insert({
                    bottom: "" + lookupContent + ""
                });

                //modify the submit button
                modifySubmitButton(lookupDiv);
            }
        });
        
        return lookupCont
    },

    createFadedBackground: function (){
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
            if (ACTIVATED_LOOKUP != null) {
                obj = GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP);
                obj.divRef.appendChild(lookupDiv);
                lookupLeft = 0;
                lookupTop = 0;
            }
            else {        
                bdy.appendChild(lookupDiv);
                var dimensions = lookupDiv.getDimensions();
                lookupLeft = (bdy.offsetWidth / 2) - (dimensions.width / 2);
                var scrollOffY = document.viewport.getScrollOffsets().top;
                var winHeight = document.viewport.getHeight();
                lookupTop = (scrollOffY + winHeight / 2) - (dimensions.height / 2);
            }
            
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
        Element.stopObserving(document, "keypress", this.key_event);
        Element.stopObserving(document, "mousedown", this.close_on_click);
        this.divRef.parentNode.removeChild(this.divRef);
        //remove the faded Background if exists
        var fb = $('fadedBackground')
        if (fb != null){
            fb.parentNode.removeChild(fb);
        }
        this.target = null;
        GLOBAL_LOOKUP_REF.removeReference(this.globalRef);
    },

    getPageSize: function () {
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

function identifyLookup (newAl) {
    if (ACTIVATED_LOOKUP != newAl) { 
        ACTIVATED_LOOKUP = newAl;
    }
}

function hideLookup() {
    obj = GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP);
    obj.closeLookup();
}

//global expand/col button var
var COLLAPSE = 1999;
function getNextCollapseSeq() {
	COLLAPSE++;
	return COLLAPSE;
}

//modify epande/ collapse button
function modifyCollapseable(lookupDiv){
	if (!lookupDiv) {
		return;
	}
	
    var slTitleBars = lookupDiv.getElementsByClassName('screenlet-title-bar');
    for (i in slTitleBars) {
    	var slTitleBar = slTitleBars[i];
    	
    	var ul = slTitleBar.firstChild;

    	if ((typeof ul) != 'object') {
    		continue;
    	}

    	var childElements = ul.childNodes;
    	for (j in childElements) {
    		if (childElements[j].className == 'expanded' ||childElements[j].className == 'collapsed') {
    			break;
    		}
    	}
    	
    	getNextCollapseSeq();
    	var childEle = childElements[j].firstChild;
    	childEle.onclick = function () {
    		toggleScreenlet(childEle, 'lec' + COLLAPSE, 'true', 'Expand', 'Collapse');
    	};
    	slTitleBar.next('div').setAttribute('id', 'lec' + COLLAPSE);
    	
    } 
}

function modifySubmitButton (lookupDiv) {
	/* changes form/submit behavior for Lookup Layer */
    if (lookupDiv) {
        modifyCollapseable(lookupDiv);
    	
        //find the lookup form
        var forms = lookupDiv.getElementsByTagName('form');
        var lookupForm = null;
        for (var i = 0; i < forms.length; i++) {
            if (! isEmpty(forms[i].getAttribute('id'))) {
                lookupForm = $(forms[i].getAttribute('id'));
            }
        }
        
        if (lookupForm == null) {
            return;
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
                    id: 'lookupSubmitButton'
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
                // modifay nav-pager
                var navPager = null;
                navPager = lookupDiv.getElementsByClassName("nav-pager");
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
                                        select[0].setAttribute("onchange", "lookupPaginationAjaxRequest(" + ocSub + ", '" + lookupForm.getAttribute('id') +"')");
                                    }
                                }
                                catch (ex) {
                                }
                            }
                        }
                    }
                }
                // modify links in result table
                var resultTable = null;
                if (navPager.length > 0) {
                    resultTable =  navPager[0].next('table');
                } else {
                    resultTable= $('search-results').firstDescendant();
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
                                    cellElement.href = "javascript:lookupAjaxRequest('" + liSub + "')";
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
 * Createan ajax Request
 */
function lookupAjaxRequest(request) {
    lookupDiv = (GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).divRef);
    lookupContent = (GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).contentRef);
    
    // get request arguments
    var arg = request.substring(request.indexOf('?')+1,(request.length));

    new Ajax.Request(request, {
        method: 'post',
        parameters: arg, requestHeaders: {
            Accept: 'application/json'
        },
        onSuccess: function (transport) {
            var formRequest = transport.responseText;
            lookupContent.remove();
            var lookupCont = new Element('DIV', {
                id: "fieldLookupContent"
            });
            lookupDiv.appendChild(lookupCont);

            lookupCont.insert({
                bottom: "" + formRequest + ""
            });
            GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).contentRef = lookupCont;
            modifySubmitButton(lookupDiv);
        }
    });
}

/**
* Create an ajax request to get the search results
* @param formAction - action target
* @param form - formId
* @return
*/
function lookupFormAjaxRequest(formAction, form) {
	lookupDiv = (GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).divRef);
	lookupContent = (GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).contentRef);
	
	new Ajax.Request(formAction, {
        method: 'post',
        parameters: $(form).serialize(), requestHeaders: {
            Accept: 'application/json'
        },
        onSuccess: function (transport) {
            var formRequest = transport.responseText;
            lookupContent.remove();
            var lookupCont = new Element('DIV', {
                id: "fieldLookupContent"
            });
            lookupDiv.appendChild(lookupCont);

            lookupCont.insert({
                bottom: "" + formRequest + ""
            });
            GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).contentRef = lookupCont;
            modifySubmitButton(lookupDiv);
        }
    });
}

function lookupPaginationAjaxRequest(navAction, form, type) {
	lookupDiv = (GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).divRef);
	lookupContent = (GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).contentRef);

	if (type == 'link') {
        navAction = navAction.substring(0, navAction.length - 1);
    }
    navAction = navAction + "&presentation=layer";
    new Ajax.Request(navAction, {
        method: 'get',
        requestHeaders: {
            Accept: 'application/json'
        },
        onSuccess: function (transport) {
            var formRequest = transport.responseText;
            lookupContent.remove();
            var lookupCont = new Element('DIV', {
                id: "fieldLookupContent"
            });
            lookupDiv.appendChild(lookupCont);

            lookupCont.insert({
                bottom: "" + formRequest + ""
            });
            GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).contentRef = lookupCont;
            modifySubmitButton(lookupDiv);
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
function set_value (value) {
    obj_caller.target = $(GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).parentTarget.id);
    var target = obj_caller.target;
    
    write_value(value, target);
    
    closeLookup();
}
// function passing selected value to calling window
function set_values (value, value2) {
    obj_caller.target = $(GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP).parentTarget.id);
    var target = obj_caller.target;
    var target2 = obj_caller.target2;
    write_value(value, target);
    write_value(value2, target2)
    
    closeLookup();
}

function write_value (value, target) {
    if (! target) return;
    if (target == null) return;
    
    setSourceColor(target);
    target.value = value;
    target.fire("lookup:changed");
    if (target.onchange != null) {     
        target.onchange();                    
    }
}
function set_multivalues(value) {
    obj_caller.target.value = value;
    obj_caller.target.fire("lookup:changed");
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
        obj = GLOBAL_LOOKUP_REF.getReference(ACTIVATED_LOOKUP);
        obj.closeLookup();
    }
}

//load description for lookup fields 
var lookupDescriptionLoaded = Class.create({
    initialize: function(fieldId, url, params) {
        this.fieldId = fieldId;
        this.url = url;
        this.params = params;
        this.updateLookup();
        $(fieldId).observe('change', this.updateLookup.bind(this));
        $(fieldId).observe('lookup:changed', this.updateLookup.bind(this));
    },

    updateLookup: function() {
        var tooltipElement = $(this.fieldId + '_lookupDescription');
        if (tooltipElement) {//first remove current description
            tooltipElement.remove();
        }
        if (!$F(this.fieldId)) {
            return;
        }
        //actual server call
        var allParams = this.params + '&' + $(this.fieldId).serialize() + '&' + 'searchType=EQUALS'
        new Ajax.Request(this.url,{parameters: allParams, onSuccess: this.updateFunction.bind(this)});
    }, 
    
    updateFunction: function(transport) {
        var wrapperElement = new Element('div').insert(transport.responseText);
        if('UL'!= wrapperElement.firstDescendant().tagName || (wrapperElement.firstDescendant().childElements().length != 1)) {    
            //alert(transport.responseText); response is error or more than one entries are found
            return;
        }
        Element.cleanWhitespace(wrapperElement);
        Element.cleanWhitespace(wrapperElement.down());
        setLookDescription(this.fieldId, wrapperElement.firstDescendant().firstDescendant().textContent);
    }            
});