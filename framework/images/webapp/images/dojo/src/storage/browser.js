/*
	Copyright (c) 2004-2006, The Dojo Foundation
	All Rights Reserved.

	Licensed under the Academic Free License version 2.1 or above OR the
	modified BSD license. For more information on Dojo licensing, see:

		http://dojotoolkit.org/community/licensing.shtml
*/

dojo.provide("dojo.storage.browser");

dojo.require("dojo.storage");
dojo.require("dojo.flash");
dojo.require("dojo.json");
dojo.require("dojo.uri.*");



dojo.storage.browser.WhatWGStorageProvider = function(){
	// summary:
	//		Storage provider that uses WHAT Working Group features in Firefox 2 
	//		to achieve permanent storage.
	// description: 
	//		The WHAT WG storage API is documented at 
	//		http://www.whatwg.org/specs/web-apps/current-work/#scs-client-side
	//
	//		You can disable this storage provider with the following djConfig
	//		variable:
	//		var djConfig = { disableWhatWGStorage: true };
	//		
	//		Authors of this storage provider-	
	//			JB Boisseau, jb.boisseau@eutech-ssii.com
	//			Brad Neuberg, bkn3@columbia.edu 
}

dojo.inherits(dojo.storage.browser.WhatWGStorageProvider, dojo.storage);

// instance methods and properties
dojo.lang.extend(dojo.storage.browser.WhatWGStorageProvider, {
	namespace: "default",
	initialized: false,
	
	_domain: null,
	_available: null,
	_statusHandler: null,
	
	initialize: function(){
		if(djConfig["disableWhatWGStorage"] == true){
			return;
		}
		
		// get current domain
		this._domain = location.hostname;
		
		// indicate that this storage provider is now loaded
		this.initialized = true;
		dojo.storage.manager.loaded();	
	},
	
	isAvailable: function(){
		try{
			var myStorage = globalStorage[location.hostname];
		}catch(e){
			this._available = false;
			return this._available;
		}
		
		this._available = true;	
		return this._available;
	},

	put: function(key, value, resultsHandler){
		if(this.isValidKey(key) == false){
			dojo.raise("Invalid key given: " + key);
		}			
		
		this._statusHandler = resultsHandler;
		
		// serialize the value;
		// handle strings differently so they have better performance
		if(dojo.lang.isString(value)){
			value = "string:" + value;
		}else{
			value = dojo.json.serialize(value);
		}
		
		// register for successful storage events
		window.addEventListener("storage", function(evt){
			// indicate we succeeded
			resultsHandler.call(null, dojo.storage.SUCCESS, key);
		}, false);
		
		// try to store the value	
		try{
			var myStorage = globalStorage[this._domain];
			myStorage.setItem(key,value);
		}catch(e){
			// indicate we failed
			this._statusHandler.call(null, dojo.storage.FAILED, 
									key, e.toString());
		}
	},

	get: function(key){
		if(this.isValidKey(key) == false){
			dojo.raise("Invalid key given: " + key);
		}
		
		var myStorage = globalStorage[this._domain];
		
		var results = myStorage.getItem(key);

		if(results == null){
			return null;
		}
	
		results = results.value;
		
		// destringify the content back into a 
		// real JavaScript object;
		// handle strings differently so they have better performance
		if(!dojo.lang.isUndefined(results) && results != null 
			 && /^string:/.test(results)){
			results = results.substring("string:".length);
		}else{
			results = dojo.json.evalJson(results);
		}
		
		return results;
	},

	getKeys: function(){
		var myStorage = globalStorage[this._domain];
		var keysArray = new Array();
		for(i=0; i<myStorage.length;i++){
			keysArray[i] = myStorage.key(i);
		}
		
		return keysArray;
	},

	clear: function(){
		var myStorage = globalStorage[this._domain];
		var keys = new Array();
		for(var i = 0; i < myStorage.length; i++){
			keys[keys.length] = myStorage.key(i);
		}
		
		for(var i = 0; i < keys.length; i++){
			myStorage.removeItem(keys[i]);
		}
	},
	
	remove: function(key){
		var myStorage = globalStorage[this._domain];
		myStorage.removeItem(key);
	},
	
	isPermanent: function(){
		return true;
	},

	getMaximumSize: function(){
		return dojo.storage.SIZE_NO_LIMIT;
	},

	hasSettingsUI: function(){
		return false;
	},
	
	showSettingsUI: function(){
		dojo.raise(this.getType() + " does not support a storage settings user-interface");
	},
	
	hideSettingsUI: function(){
		dojo.raise(this.getType() + " does not support a storage settings user-interface");
	},
	
	getType: function(){
		return "dojo.storage.browser.WhatWGStorageProvider";
	}
});




dojo.storage.browser.FlashStorageProvider = function(){
	// summary: Storage provider that uses features in Flash to achieve permanent storage
	// description:
	//		Authors of this storage provider-
	//			Brad Neuberg, bkn3@columbia.edu	
}

dojo.inherits(dojo.storage.browser.FlashStorageProvider, dojo.storage);

// instance methods and properties
dojo.lang.extend(dojo.storage.browser.FlashStorageProvider, {
	namespace: "default",
	initialized: false,
	_available: null,
	_statusHandler: null,
	
	initialize: function(){
		if(djConfig["disableFlashStorage"] == true){
			return;
		}
		
		// initialize our Flash
		var loadedListener = function(){
			dojo.storage._flashLoaded();
		}
		dojo.flash.addLoadedListener(loadedListener);
		var swfloc6 = dojo.uri.dojoUri("Storage_version6.swf").toString();
		var swfloc8 = dojo.uri.dojoUri("Storage_version8.swf").toString();
		dojo.flash.setSwf({flash6: swfloc6, flash8: swfloc8, visible: false});
	},
	
	isAvailable: function(){
		if(djConfig["disableFlashStorage"] == true){
			this._available = false;
		}else{
			this._available = true;
		}
		
		return this._available;
	},

	put: function(key, value, resultsHandler){
		if(this.isValidKey(key) == false){
			dojo.raise("Invalid key given: " + key);
		}
			
		this._statusHandler = resultsHandler;
		
		// serialize the value;
		// handle strings differently so they have better performance
		if(dojo.lang.isString(value)){
			value = "string:" + value;
		}else{
			value = dojo.json.serialize(value);
		}
		
		dojo.flash.comm.put(key, value, this.namespace);
	},

	get: function(key){
		if(this.isValidKey(key) == false){
			dojo.raise("Invalid key given: " + key);
		}
		
		var results = dojo.flash.comm.get(key, this.namespace);

		if(results == ""){
			return null;
		}
    
		// destringify the content back into a 
		// real JavaScript object;
		// handle strings differently so they have better performance
		if(!dojo.lang.isUndefined(results) && results != null 
			 && /^string:/.test(results)){
			results = results.substring("string:".length);
		}else{
			results = dojo.json.evalJson(results);
		}
    
		return results;
	},

	getKeys: function(){
		var results = dojo.flash.comm.getKeys(this.namespace);
		
		if(results == ""){
			return [];
		}

		// the results are returned comma seperated; split them
		return results.split(",");
	},

	clear: function(){
		dojo.flash.comm.clear(this.namespace);
	},
	
	remove: function(key){
		// summary: 
		//		Note- This one method is not implemented on the
		// 		FlashStorageProvider yet
		
		dojo.unimplemented("dojo.storage.browser.FlashStorageProvider.remove");
	},
	
	isPermanent: function(){
		return true;
	},

	getMaximumSize: function(){
		return dojo.storage.SIZE_NO_LIMIT;
	},

	hasSettingsUI: function(){
		return true;
	},

	showSettingsUI: function(){
		dojo.flash.comm.showSettings();
		dojo.flash.obj.setVisible(true);
		dojo.flash.obj.center();
	},

	hideSettingsUI: function(){
		// hide the dialog
		dojo.flash.obj.setVisible(false);
		
		// call anyone who wants to know the dialog is
		// now hidden
		if(dojo.storage.onHideSettingsUI != null &&
			!dojo.lang.isUndefined(dojo.storage.onHideSettingsUI)){
			dojo.storage.onHideSettingsUI.call(null);	
		}
	},
	
	getType: function(){
		return "dojo.storage.browser.FlashStorageProvider";
	},
	
	/** Called when the Flash is finished loading. */
	_flashLoaded: function(){
		this._initialized = true;

		// indicate that this storage provider is now loaded
		dojo.storage.manager.loaded();
	},
	
	//	Called if the storage system needs to tell us about the status
	//	of a put() request. 
	_onStatus: function(statusResult, key){
		var ds = dojo.storage;
		var dfo = dojo.flash.obj;
		
		if(statusResult == ds.PENDING){
			dfo.center();
			dfo.setVisible(true);
		}else{
			dfo.setVisible(false);
		}
		
		if((!dj_undef("_statusHandler", ds))&&(ds._statusHandler != null)){
			ds._statusHandler.call(null, statusResult, key);		
		}
	}
});

// register the existence of our storage providers
dojo.storage.manager.register("dojo.storage.browser.WhatWGStorageProvider",
								new dojo.storage.browser.WhatWGStorageProvider());
dojo.storage.manager.register("dojo.storage.browser.FlashStorageProvider",
								new dojo.storage.browser.FlashStorageProvider());

// now that we are loaded and registered tell the storage manager to initialize
// itself
dojo.storage.manager.initialize();
