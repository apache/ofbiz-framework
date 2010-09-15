/***********************************************
APACHE OPEN FOR BUSINESS
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
***********************************************/

//GLOBAL NAMESPACE
var OFBIZ = window.OFBIZ || {};

/**************************************************
LAYOUT AND NAVIGATION
builds main-nav/preferences dropdowns and 
adds functionality to style layout elements
**************************************************/
//ADD CLEARFIX STYLING TO MAIN CONTENT AREA
OFBIZ.clearFix = function(){
    if($$('.contentarea')){
        $$('.contentarea').each(function(elm) {
            $(elm).addClassName('clearfix');
        });
    }
}

//GLOBAL FUNCTION FOR APP DROP-DROWN SECTIONS
OFBIZ.initExpansion = function() {
    if($$('.contracted')){
        $$('.contracted').each(function(elm) {
            $(elm).next().style.display = 'none';
        });    
    }
    if($$('.expanded, .contracted')) {
        $$('.expanded, .contracted').each(function(elm) {
            $(elm).onclick = function() {
                Effect.toggle($(elm).next(),'blind',{
                    duration : .2,
                    afterFinish: function() {
                        if($(elm).next().visible()) {
                            $(elm).removeClassName('contracted');
                            $(elm).addClassName('expanded');
                        } else {
                            $(elm).removeClassName('expanded');
                            $(elm).addClassName('contracted');
                        }
                    }
                });
            }
        });
    }
}


OFBIZ.Effect = Object.extend({}, Effect);
Object.extend(Element,{
    findElement: function(element, tagName) {
        var element = $(element);
        while (element.parentNode && (!element.tagName || 
            (element.tagName.toUpperCase() != tagName.toUpperCase())))
            element = element.parentNode;
            return $(element);
    }
});

/*****************************************************
FORM FOCUS AND BLUR
javascript solution to make form focus style
consistent cross browser form platforms that
don't support input:focus (cough, cough, IE, cough)
*****************************************************/
document.observe('dom:loaded', function() {
    var fields = $$("input, textarea");
    for (var i = 0; i < fields.length; i++) {
      fields[i].onfocus = function() {this.className += ' focused';}
      fields[i].onblur = function() {this.className = this.className.replace('focused', '');}
    }
});


/********************************************************
HUMANIZED MESSAGES
displays success and error messages in modal window.
message automatically fades on keypress, click mousemove
(ala growl/system tray notices)
********************************************************/
//SUCCESS AND ERROR MESSAGE NOTIFICATIONS
var humanMsg = {
    setup: function(appendTo, msgOpacity) {
      humanMsg.msgID = 'humanMsg';
      appendTo = appendTo || $$('body')[0];
      humanMsg.msgOpacity = 0.5;
      if (msgOpacity !== undefined) {
        humanMsg.msgOpacity = parseFloat(msgOpacity);
      }
      var myTemplate = new Template(
        '<div id="#{msgID}" class="humanMsg" style="display:none;">'+
          '<div></div>'+
         '</div> ');
      var show = {msgID: humanMsg.msgID};
      appendTo.insert(myTemplate.evaluate(show));
    },
    displayMsg: function(msg) {
      if (msg === '') {
        return;
      }
      clearTimeout(humanMsg.t2);
      // Inject message
      var msgElement = $(humanMsg.msgID);;
      msgElement.down('div').update(msg);
      msgElement.appear({ duration: 0.2 });
      //humanMsg.t1 = setTimeout("humanMsg.bindEvents()", 700)
      //humanMsg.t2 = setTimeout("humanMsg.removeMsg()", 5000)
      msgElement.observe('click', humanMsg.removeMsg);
    },
    // Remove message if mouse is moved or key is pressed
    bindEvents: function() {
      document.observe('mousemove', humanMsg.removeMsg)
              .observe('click', humanMsg.removeMsg)
              .observe('keypress', humanMsg.removeMsg);
    },
    // Unbind mouse & keyboard
    removeMsg: function() {
      document.stopObserving('mousemove', humanMsg.removeMsg)
              .stopObserving('click', humanMsg.removeMsg)
              .stopObserving('keypress', humanMsg.removeMsg);
      $(humanMsg.msgID).fade({ duration: 0.5, delay:0.5 });
    }
};

/**************************************************
MODAL WINDOWS
displays preferences (language, theme, timezone)
in modal window instead of popup/new window
**************************************************/
//PREFERENCES POPUPS
ModalWindow = Class.create({
    initialize: function(){
        this.cont = "";
        this.overlay = "";
        this.win = "";
        this.container = new Element('div', {id:'modal-container'});
        var container = this.container;
        $(document.body).insert({bottom:container});
    },
    show: function(element, overlay){
        this.close();
        this.cont = element;
        if(overlay) this.overlay = this.container.appendChild(new Element('div', {'class':'modal-overlay'}));
        this.win = this.container.appendChild(new Element('div', {'class':'modal-window'}));
        this.win.insert({bottom:this.cont});
    },
    close: function(e){
        if(e) e.stop();
        this.container.childElements().invoke('remove');
    }
});

//LOAD MODAL PREFERENCE WINDOWS
document.observe('dom:loaded', function() {
    get = new ModalWindow();
    $("language").observe('click',function(e) {
      var locale = new Element('div', {id:'modal-contents'}).update("<img src='/bizznesstime/images/ajax-loader.gif'/>Updating Languages, please wait...");
      var localeUpdate = new Ajax.Updater('modal-contents', this.rel, {method: 'get'});
      get.show(locale,true);
    });
    $("theme").observe('click',function(e) {
          var look = new Element('div', {id:'modal-contents'}).update("<img src='/bizznesstime/images/ajax-loader.gif'/>Updating Themes, please wait...");
          var lookUpdate = new Ajax.Updater('modal-contents', this.rel, {method: 'get'});
          get.show(look,true);
    });
    $("timezone").observe('click',function(e) {
          var time = new Element('div', {id:'modal-contents'}).update("<img src='/bizznesstime/images/ajax-loader.gif'/>Updating Timezones, please wait...");
          var timeUpdate = new Ajax.Updater('modal-contents', this.rel, {method: 'get'});
          get.show(time,true);
    });
});

/**************************************************
LOAD 'EM UP
**************************************************/
//LOAD GLOBAL APP FUNCTIONS
document.observe('dom:loaded', function(){
    OFBIZ.clearFix();
    OFBIZ.initExpansion();
    humanMsg.setup();
});



