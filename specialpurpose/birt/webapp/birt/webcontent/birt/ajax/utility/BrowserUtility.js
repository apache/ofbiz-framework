// Copyright 1994-2008, Actuate Software Corp., All rights reserved.

BrowserUtility = Class.create();

BrowserUtility.prototype = {
	
	initialize: function()
	{
		this.isIE = this.__isIE();
		if ( this.isIE )
		{
			this.isIE6 = false;
			this.isIE7 = false;
			this.isIE8 = false;
			if (document.documentMode) {
				if (document.documentMode >= 8) {
					this.isIE8 = true;
				} else if (document.documentMode >= 7) {
					this.isIE7 = true;
				} else {
					this.isIE6 = true;
				}
			}
			else if ( window.XMLHttpRequest )
			{
				this.isIE7 = true;
			}
			else
			{
				this.isIE6 = true;
			}
		}

		this.isMozilla = this.__isMozilla();
		this.isFirefox = this.__isFirefox();
		this.isGecko = this.__isGecko();
		this.isSafari = this.__isSafari();
		this.isKHTML = this.__isKHTML();
		this.isOpera = this.__isOpera();
		
		if ( this.isFirefox )
		{
			var firefoxVersion = this._getAgentVersion("Firefox");
			if ( firefoxVersion && firefoxVersion.length > 0 )
			{
				if ( firefoxVersion[0] == 2 )
				{
					this.isFirefox2 = true;
				}
				else if ( firefoxVersion[0] == 3 )
				{
					this.isFirefox3 = true;
				}				
			}
		}
	},
	
	_getAgentVersion : function( agentName )
	{
		var re = new RegExp(agentName + "\/([^\s])", "i");
		var agentVersion = re.exec( navigator.userAgent );
		if ( agentVersion && agentVersion[1] )
		{
			return this._getVersionComponents( agentVersion[1] );
		}
		else
		{
			return null;
		}
	},
	
	_getVersionComponents : function( versionString )
	{
		if ( !versionString )
		{
			return null;
		}
		return versionString.split(".");
	},
		
	__isSafari: function()
	{
		return navigator.appVersion.match(/Safari/) != null;		
	},

	__isKHTML: function()
	{
		return navigator.appVersion.match(/KHTML/) != null;		
	},

	__isOpera: function()
	{
		return navigator.appName.match(/Opera/) != null;
	},
	
	__isIE: function()
	{
		var userAgent = navigator.userAgent.toLowerCase();
		var useIFrame;
		if(userAgent.indexOf('msie') > -1)
		{
			//Internet Explorer
			return true;
			
		}
		else 
		{
			return false;
		}
	},
	
	__isMozilla : function()
	{
		var userAgent = navigator.userAgent.toLowerCase();
		return (userAgent.indexOf('mozilla') > -1);
	},
	
	__isFirefox : function()
	{
		var userAgent = navigator.userAgent.toLowerCase();
		return (userAgent.indexOf('firefox') > -1);
	},

	__isGecko : function()
	{
		var userAgent = navigator.userAgent.toLowerCase();
		return (userAgent.indexOf('gecko') > -1);
	},
	
	useIFrame: function()
	{
		return this.isIE;
	},
	
	_getScrollBarWidth : function(container, viewportWidth, viewportHeight)
	{
		var defaultWidth = 20;
		
		if (this.scrollBarWidth) {
			return this.scrollBarWidth;
		}
		else if (container != null && viewportWidth > 0 && viewportHeight < container.offsetHeight)
		{
			var oldWidth = container.style.width;
			var oldHeight = container.style.height;
			var oldOverflowX = container.style.overflowX;
			var oldOverflowY = container.style.overflowY;
			var oldPosition = container.style.position;
			
			// Sets report container's styles to calculate scroll bar's width.
			container.style.width = "";
			container.style.height = "";
			container.style.overflowX = "scroll";
			container.style.overflowY = "hidden";
			container.style.position = "relative";

			this.scrollBarWidth = viewportWidth - container.offsetWidth;
			if (this.scrollBarWidth <= 0){
				this.scrollBarWidth = defaultWidth;
			}

			// Restors report container's old styles.
			container.style.width = oldWidth;
			container.style.height = oldHeight;
			container.style.overflowX = oldOverflowX;
			container.style.overflowY = oldOverflowY;
			container.style.position = oldPosition;
			return this.scrollBarWidth;
		}
		return defaultWidth;
	}
}
