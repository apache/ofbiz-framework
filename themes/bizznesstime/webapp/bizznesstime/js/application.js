/***********************************************
APACHE OFBIZ
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
    if(jQuery('.contentarea')){
        jQuery('.contentarea').each(function(elm) {
            jQuery(this).addClass('clearfix');
        });
    }
}

//GLOBAL FUNCTION FOR APP DROP-DROWN SECTIONS
OFBIZ.initExpansion = function() {
    if(jQuery('.contracted')){
        jQuery('.contracted').each(function(elm) {
            jQuery(this).next().css({'display': 'none'});
        });    
    }
    if(jQuery('.expanded, .contracted')) {
        jQuery('.expanded, .contracted').each(function(elm) {
            var menu = jQuery(this);
            menu.click( function() {
                menu.next().toggle( function () {
                       if(menu.next().is(':visible')) {
                            menu.removeClass('contracted');
                            menu.addClass('expanded');
                        } else {
                            menu.removeClass('expanded');
                            menu.addClass('contracted');
                        }
                    });
                });
            });
    };
}

OFBIZ.hideExpanded = function() {
    if(jQuery('.expanded').length) {
        jQuery('.expanded').each(function(elm) {
               var menu = jQuery(this);
               if(menu.next().is(':visible')) {
                   menu.removeClass('expanded');
                   menu.addClass('contracted');
                   menu.next().fadeOut('fast');
               }
            });
    };
}

/*****************************************************
FORM FOCUS AND BLUR
javascript solution to make form focus style
consistent cross browser form platforms that
don't support input:focus (cough, cough, IE, cough)
*****************************************************/
jQuery('document').ready(function () {
    var fields = jQuery("input, textarea");
    for (var i = 0; i < fields.length; i++) {
     if (!fields[i].style.visibility == 'hidden') {
      fields[i].focus (function() {this.className += ' focused';});
      fields[i].blur (function() {this.className = this.className.replace('focused', '');});
     }
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
      appendTo = appendTo || jQuery('body')[0];
      humanMsg.msgOpacity = 0.5;
      if (msgOpacity !== undefined) {
        humanMsg.msgOpacity = parseFloat(msgOpacity);
      }
      /*var myTemplate = new Template(
        '<div id="#{msgID}" class="humanMsg" style="display:none;">'+
          '<div></div>'+
         '</div> ');
      var show = {msgID: humanMsg.msgID};*/
      jQuery('<div id="' + humanMsg.msgID + '" class="humanMsg" style="display:none;"><div></div></div>').appendTo(appendTo);
    },
    displayMsg: function(msg) {
      if (msg === '') {
        return;
      }
      clearTimeout(humanMsg.t2);
      // Inject message
      var msgElement = jQuery("#" + humanMsg.msgID);;
      msgElement.find('div:first').html(msg);
      msgElement.fadeIn('slow');
      msgElement.click(humanMsg.removeMsg);
    },
    // Remove message if mouse is moved or key is pressed
    bindEvents: function() {
      jQuery(document).mousemove(humanMsg.removeMsg)
              .click(humanMsg.removeMsg)
              .keypress(humanMsg.removeMsg);
    },
    // Unbind mouse & keyboard
    removeMsg: function() {
      jQuery(document).unbind('mousemove', humanMsg.removeMsg)
              .unbind('click', humanMsg.removeMsg)
              .unbind('keypress', humanMsg.removeMsg);
      jQuery("#" + humanMsg.msgID).fadeOut('slow');
    }
};

/**************************************************
MODAL WINDOWS
displays preferences (language, theme, timezone)
in modal window instead of popup/new window
**************************************************/
ModalWindow = function(){};

ModalWindow.openModalWindow = function(url, dialogId, width, height) {
    
    // if element doesn't exists create otherwise just open
    if (!document.getElementById('dialog-container_' + dialogId)) {
        var modalContainer = jQuery("<div id='dialog-container_" + dialogId + "' style='display:none'></div>");
        jQuery('body').append(modalContainer);
        modalContainer = jQuery('#dialog-container_' + dialogId)
        modalContainer.dialog({
            modal: true,
            zIndex: 10000,
            height: height,
            width: width,
            open: function() {
                    modalContainer.append(jQuery("<img id='dialog-ajax-spinner' src='/bizznesstime/images/ajax-loader.gif'/>"));
                    modalContainer.load(url, function () {jQuery("#dialog-ajax-spinner").remove();});
                    // make sure the other possible opened dialogs will be closed
                    ModalWindow.closeOtherModalWindows(dialogId);
                    // close the open menu
                    OFBIZ.hideExpanded();
                }
        });
    } else {
        jQuery('#dialog-container_' + dialogId).dialog('open');
    }
}

ModalWindow.closeOtherModalWindows = function(currentOpen) {
    if (currentOpen == 'listLanguage') {
        jQuery('#dialog-container_listTheme').dialog('close');
        jQuery('#dialog-container_listTimezone').dialog('close');
    } else if (currentOpen == 'listTheme') {
        jQuery('#dialog-container_listTimezone').dialog('close');
        jQuery('#dialog-container_listLanguage').dialog('close');
    } else if (currentOpen == 'listTimezone') {
        jQuery('#dialog-container_listTheme').dialog('close');
        jQuery('#dialog-container_listLanguage').dialog('close');
    }
}

//LOAD MODAL PREFERENCE WINDOWS
jQuery('document').ready (function () {
    jQuery("#language").click (function(e) {
        var url = this.href;
        this.href = "javascript:void(0);"
        ModalWindow.openModalWindow(url, 'listLanguage', 340, 400);    
    });
    jQuery("#theme").click (function(e) {
        var url = this.href;
        this.href = "javascript:void(0);"
            ModalWindow.openModalWindow(url, 'listTheme', 600, 400);  
    });
    jQuery("#timezone").click (function(e) {
        var url = this.href;
        this.href = "javascript:void(0);"
            ModalWindow.openModalWindow(url, 'listTimezone', 430, 400);  
    });
    jQuery("#company").click (function(e) {
        var url = this.href;
        this.href = "javascript:void(0);"
            ModalWindow.openModalWindow(url, 'listSetCompanies', 430, 400);  
    });
});

/**************************************************
LOAD 'EM UP
**************************************************/
//LOAD GLOBAL APP FUNCTIONS
jQuery('document').ready(function () {
    OFBIZ.clearFix();
    OFBIZ.initExpansion();
    humanMsg.setup();
});
