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
//GLOBAL NAMESPACE
var OFBIZ = window.OFBIZ || {};

//SUBNAV HEADING
OFBIZ.buildSubnav = function(){
	if($$('.tab-bar')){
		$$('.tab-bar').each(function(elm) {
			$(elm).insert( {
				before :"<h4 id='nav'></h4>"
			});
			$(elm).insert( {
				top :"<h5>navigate application</h5>"
			});
			$(elm).insert( {
				bottom :"<div><a href='#' class='close-tab'>close</a></div>"
			});
		});
		//OFBIZ.initExpansion();
	}
}

/* Round 2 - Sliding Left Bar - Coming Soon
//QUICK NAV/SEARCH FOR LEFT SCREENLET
OFBIZ.buildSearch = function(){
	if($$('.left')){
		$$('.left').each(function(elm) {
			$(elm).insert( {
				before :"<div id='sideBar'>"
			});
			$(elm).insert( {
				after :"<a href='#' id='sideBarTab'><img src='/smoothfeather/images/slide-button-left.gif' alt='sideBar' title='sideBar' /></a></div>"
			});
		});
		//OFBIZ.initExpansion();
	}
}

var isExtended = 0;
OFBIZ.slideSideBar = function(){
	new Effect.toggle('sideBarContents', 'blind', {scaleX: 'true', scaleY: 'true;', scaleContent: false});
	if(isExtended==0){
		$('sideBarTab').childNodes[0].src = $('sideBarTab').childNodes[0].src.replace(/(\.[^.]+)$/, '-active$1');
		new Effect.Fade('sideBarContents',{ duration:1.0, from:0.0, to:1.0 });
		isExtended++;
	}
	else{
		$('sideBarTab').childNodes[0].src = $('sideBarTab').childNodes[0].src.replace(/-active(\.[^.]+)$/, '$1');
		new Effect.Fade('sideBarContents',{ duration:1.0, from:1.0, to:0.0 });
		isExtended=0;
	}
}

OFBIZ.initSideBar = function(){
	Event.observe('sideBarTab', 'click', OFBIZ.slideSideBar, true);
}

Event.observe(window, 'load', OFBIZ.initSideBar, true);
*/

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
							OFBIZ.Cookie.set($(elm).next().id,'expanded',360);
						} else {
							$(elm).removeClassName('expanded');
							$(elm).addClassName('contracted');
							OFBIZ.Cookie.set($(elm).next().id,'contracted',360);
						}
					}
				});
			}
		});
	}
}

OFBIZ.Cookie = {
	set: function(name, value, daysToExpire) {
		var expire = '';
		if (daysToExpire != undefined) {
			var d = new Date();
			d.setTime(d.getTime() + (86400000 * parseFloat(daysToExpire)));
			expire = '; expires=' + d.toGMTString();
		}
		return (document.cookie = escape(name) + '=' + escape(value || '') + expire +'; path=/;');
	},
	get: function(name) {
		var cookie = document.cookie.match(new RegExp('(^|;)\\s*' + escape(name) + '=([^;\\s]*)'));
		return (cookie ? unescape(cookie[2]) : null);
	},
	erase: function(name) {
		var cookie = OFBIZ.Cookie.get(name) || true;
		OFBIZ.Cookie.set(name, '', -1);
		return cookie;
	},
	accept: function() {
		if (typeof navigator.cookieEnabled == 'boolean') {
			return navigator.cookieEnabled;
		}
		OFBIZ.Cookie.set('_test', '1');
		return (OFBIZ.Cookie.erase('_test') === '1');
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
	  humanMsg.t1 = setTimeout("humanMsg.bindEvents()", 700)
	  humanMsg.t2 = setTimeout("humanMsg.removeMsg()", 5000)
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
	  $(humanMsg.msgID).fade({ duration: 0.5 });
	}
};

//LOAD MODAL PREFERENCE WINDOWS
Event.observe(window, 'load', function() {
	get = new ModalWindow();
	$("language").observe('click',function(e) {
	  var locale = new Element('div', {id:'modal-contents'}).update("Updating Languages, please wait...");
	  var localeUpdate = new Ajax.Updater('modal-contents', this.rel, {method: 'get'});
	  get.show(locale,true);
	});
	$("theme").observe('click',function(e) {
		  var look = new Element('div', {id:'modal-contents'}).update("Updating Themes, please wait...");
		  var lookUpdate = new Ajax.Updater('modal-contents', this.rel, {method: 'get'});
		  get.show(look,true);
	});
	$("timezone").observe('click',function(e) {
		  var time = new Element('div', {id:'modal-contents'}).update("Updating Timezones, please wait...");
		  var timeUpdate = new Ajax.Updater('modal-contents', this.rel, {method: 'get'});
		  get.show(time,true);
	});
});

//LOAD GLOBAL APP FUNCTIONS
document.observe('dom:loaded', function(){
	OFBIZ.initExpansion();
	OFBIZ.buildSubnav();
	OFBIZ.buildSearch();
	humanMsg.setup();
});

