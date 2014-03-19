// Copyright 1994-2006, Actuate Software Corp., All rights reserved.

Mask = Class.create();

Mask.prototype = {

	zIndexStack: [],
	zIndexBase: 200, //Lowest zIndex for mask
	zIndexCurrent: null,
	opacity: 0, //Default opacity is zero
	_baseElements : null,

	initialize: function(useIframe, opacity)
	{
		this.zIndexCurrent = this.zIndexBase;
		
		if(useIframe)
		{
			this.__useIFrame = true;
			this.__mask = document.createElement( 'iframe' );
			// Workaround for IE https secure warning
			this.__mask.src = "birt/pages/common/blank.html";
		}
		else
		{
			this.__useIFrame = false;
			this.__mask = document.createElement( 'div' );
		}

		this.__mask.id = "Mask";
		//default opacity to zero
		if(opacity)
		{
			debug("setting opacity to : " + opacity);
			this.opacity = opacity;
		}
		document.body.appendChild( this.__mask );
		this.__mask.style.position = 'absolute';
		this.__mask.style.top = '0px';
		this.__mask.style.left = '0px';
		var width = BirtPosition.viewportWidth();
		this.__mask.style.width = width + 'px';
		var height = BirtPosition.viewportHeight();
		this.__mask.style.height = height + 'px';
		this.__mask.style.zIndex = '200';
		this.__mask.style.backgroundColor = '#0044ff';
		this.__mask.style.filter = 'alpha( opacity=' + ( this.opacity * 100 ) + ')';
		this.__mask.style.opacity = this.opacity;
		this.__mask.scrolling = 'no';
		this.__mask.marginHeight = '0px';
		this.__mask.marginWidth = '0px';
		this.__mask.style.display = 'none';
		// Support low version Mozilla/NS
		this.__mask.style.MozOpacity = 0;
		
		if(useIframe)
		{
			this.__useIFrame = true;
			this.__progressBarMask = document.createElement( 'iframe' );
			// Workaround for IE https secure warning
			this.__progressBarMask.src = "birt/pages/common/blank.html";
		}
		else
		{
			this.__useIFrame = false;
			this.__progressBarMask = document.createElement( 'div' );
		}
	
		document.body.appendChild( this.__progressBarMask );
		this.__progressBarMask.style.position = 'absolute';
		this.__progressBarMask.style.top = '0px';
		this.__progressBarMask.style.left = '0px';
		var width = BirtPosition.viewportWidth();
		this.__progressBarMask.style.width = width + 'px';
		var height = BirtPosition.viewportHeight();
		this.__progressBarMask.style.height = height + 'px';
		this.__progressBarMask.style.zIndex = '200';
		this.__progressBarMask.style.backgroundColor = '#ff0000';
		this.__progressBarMask.style.filter = 'alpha( opacity=' + ( this.opacity * 100 ) + ')';
		this.__progressBarMask.style.opacity = this.opacity;
		this.__progressBarMask.scrolling = 'no';
		this.__progressBarMask.marginHeight = '0px';
		this.__progressBarMask.marginWidth = '0px';
		this.__progressBarMask.style.display = 'none';
		this.__progressBarMask.style.cursor = "move"; //cursor is set to a different value than desired so that change is trigged in IE
		
		this.__eh_resize_closure = this.__eh_resize.bindAsEventListener( this );
		Event.observe( window, 'resize', this.__eh_resize_closure, false );
	},
	
	/**
	 * Sets the element DOM objects present on the main window.
	 * They will be disabled when the mask is floating on the top
	 * of them.
	 * @param baseElements array of base elements
	 */
	setBaseElements : function( baseElements )
	{
		this._baseElements = baseElements;
	},
	
	/*
	If mask is not shown, shows mask, otherwise, moves mask zIndex above last returned zIndex.
	@returns zIndex for element to place directly above mask
	*/
	show: function()
	{
		if(this.zIndexStack.length == 0)
		{
			this._updateElements(this._baseElements, false);
			
			Element.show( this.__mask );
		}
		this.__mask.style.zIndex = this.zIndexCurrent;
		this.zIndexStack.push(this.zIndexCurrent);
		this.zIndexCurrent++;
		var dialogZIndex = this.zIndexCurrent;
		this.zIndexCurrent++;
		
		return dialogZIndex;
	},
	
	/*
	Resizes masks to current screen width (not including scrollbars)
	*/
	__eh_resize: function()
	{
		var width = BirtPosition.viewportWidth();
		var height = BirtPosition.viewportHeight();
		
		this.__mask.style.width = width + 'px';
		this.__mask.style.height = height + 'px';
		this.__progressBarMask.style.width = width + 'px';
		this.__progressBarMask.style.height = height + 'px';
	},
	
	/*
	Shows progress bar mask above the highest regular item zIndex.
	*/
	showProgressBarMask: function()
	{		
		this.__progressBarMask.style.cursor = "wait";
		
		Element.show( this.__progressBarMask );

		this.__progressBarMask.style.zIndex = this.zIndexCurrent;
		
		return (this.zIndexCurrent + 1);
		
	},
	
	hide: function()
	{
		if(this.zIndexStack.length == 1)
		{
			this._updateElements(this._baseElements, true);
			
			Element.hide( this.__mask );
			this.zIndexStack.pop();
			this.zIndexCurrent = this.zIndexBase;
		}
		else
		{
			this.zIndexCurrent = this.zIndexStack.pop();
			this.__mask.style.zIndex = this.zIndexStack[this.zIndexStack.length -1];
		}
	},
	
	hideProgressBarMask: function()
	{	
		this.__progressBarMask.style.cursor = "move";
			
		Element.hide( this.__progressBarMask );
	},
	
	isIFrame: function()
	{
		return this.__useIFrame;
	},
	
	_updateElements : function(elements, enable)
	{
		if ( !elements )
		{
			return;
		}
		
		var func = enable?birtUtility.restoreTabIndexes:birtUtility.disableTabIndexes;
		for ( var i = 0; i < elements.length; i++ )
		{
			func.call(birtUtility, $(elements[i]));
		}
	}

}
