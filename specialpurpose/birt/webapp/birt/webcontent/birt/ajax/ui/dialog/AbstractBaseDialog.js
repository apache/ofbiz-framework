/******************************************************************************
 *	Copyright (c) 2004 Actuate Corporation and others.
 *	All rights reserved. This program and the accompanying materials 
 *	are made available under the terms of the Eclipse Public License v1.0
 *	which accompanies this distribution, and is available at
 *		http://www.eclipse.org/legal/epl-v10.html
 *	
 *	Contributors:
 *		Actuate Corporation - Initial implementation.
 *****************************************************************************/
 
/**
 *	Dialog base class
 */
AbstractBaseDialog = function(){};

AbstractBaseDialog.prototype =
{		
	contentHolderWidth: 500, //TODO - move to display constants? Default width in pixels
	visible: null, //Is the dialog currently visible		
	__operationCancelled: false,
	__allowSelection: false,
	 
	/**
	 Initialize dialog base
	 */
	__initBase: function(htmlId, contentWidth)
	{
		this.__instance = $(htmlId);
		this.htmlId = htmlId;
		this.visible = false;
		
		//references will be set for okay button so it can be enabled/disabled
		this.okButton = null;
		this.okButtonLeft = null;
		this.okButtonRight = null;
		
		//Instance is given a location within screen to avoid
		//extra scroll bar creation
		this.__instance.style.top = '0px';
		this.__instance.style.left = '0px';
		
		//Sizing
		this.contentHolderName = htmlId + "dialogContentContainer";
		if(contentWidth)
		{
			this.contentHolderWidth = parseInt(contentWidth);
		}	
		
		this.__neh_resize_closure = this.__neh_resize.bindAsEventListener( this );
		
		// Initialize event handler closures	
		this.__neh_okay_closure = this.__neh_okay.bind(this);
		this.__neh_cancel_closure = this.__neh_cancel.bind(this);
		this.mousedown_closure = this.__neh_mousedown.bindAsEventListener(this);
		this.mouseup_closure = this.__neh_mouseup.bindAsEventListener(this);
		this.drag_closure = this.__neh_drag.bindAsEventListener(this);
		this.disposeSelection_closure = this.__neh_disposeSelection.bindAsEventListener(this);
		this.enableSelection_closure = this.__neh_enableSelection.bindAsEventListener(this);
		
	    this.__beh_cancelOperation_closure = this.__beh_cancelOperation.bindAsEventListener( this );
		
		birtEventDispatcher.registerEventHandler( birtEvent.__E_CANCEL_TASK, this.htmlId, this.__beh_cancelOperation_closure );
		
		this.__operationCancelled = false;
		
		// Initialize shared events	
		this.__base_installEventHandlers(htmlId);	
	},
	
	/**
	Install event handlers shared across all dialogs.
	Buttons (close, cancel, ok), move dialog (drag and drop), screen resize.
	*/
	__base_installEventHandlers : function( id )
	{
		//Initialize iframe
		this.__iframe = $(id + "iframe");
		
		// Close button
		var closeBtn = $(id + "dialogCloseBtn");
		Event.observe( closeBtn, 'click', this.__neh_cancel_closure, false );
		Event.observe( closeBtn, 'mousedown', this.__neh_stopEvent.bindAsEventListener(this), false );
		
		// OK and Cancel buttons		
		this.okBtn = $(id + "okButton");
		var cancelBtn = $(id + "cancelButton");
		
		this.okBtnLeft = $(id + "okButtonLeft"); //left part of background image
		this.okBtnRight = $(id + "okButtonRight"); //right part of background image
		
		//set OK button to enabled as default
		this.okBtn.className = "dialogBtnBarButtonEnabled";
		if ( this.okBtnLeft )
		{
			this.okBtnLeft.className = "dialogBtnBarButtonLeftBackgroundEnabled";
		}
		if ( this.okBtnRight )
		{
			this.okBtnRight.className = "dialogBtnBarButtonRightBackgroundEnabled";
		}
		
		Event.observe( this.okBtn, 'click', this.__neh_okay_closure , false );
		//Cancel		
		Event.observe( cancelBtn, 'click', this.__neh_cancel_closure , false );
			
		//Drag and Drop
		this.dragBarName = id + "dialogTitleBar";
		var dragArea = $(this.dragBarName);	
		Event.observe(dragArea, 'mousedown', this.mousedown_closure, false);
		
		//work around for IE, enable selection for dialog text controls
		var oInputs = this.__instance.getElementsByTagName( 'INPUT' );
		for ( var i = 0; i < oInputs.length ; i++ )
		{
			if(oInputs[i].type != 'button')
			{
				this.__enableSelection( oInputs[i] );
			}
		}
		
		var oTextAreas = this.__instance.getElementsByTagName( 'TEXTAREA' );
		for ( var i = 0; i < oTextAreas.length ; i++ )
		{
			this.__enableSelection( oTextAreas[i] );
		}		
	},	
	
	/**
	 *	Binding data to the dialog UI.
	 *
	 *	@data, data DOM tree (schema TBD)
	 *	@return, void
	 */
	__cb_bind : function( data )
	{
		this.__bind( data );
			
		this.__l_show( );
	},
	
	/**
	ABSTRACT - must be implemented by extending class
	Gets xml data before dialog is shown
	*/
	__bind: function(data)
	{
	
	},
	
	/**
	Trigger dialog from client (bypasses bind step)
	*/
	showDialog: function()
	{
		this.__l_show( );
	},
	
	/**
	 *	
	 *
	 *	@event, incoming browser native event
	 *	@return, void
	 */
	__l_show : function( )
	{
		// reset cancelled flag
		this.__operationCancelled = false;
		this.__preShow();
			
				//check if the dialog is already shown
		if(!this.visible)
		{
			var zIndex = Mask.show(); 
			debug("showing at zIndex " + zIndex);
			this.__instance.style.zIndex = zIndex;
					
			Element.show( this.__instance );
			this.visible = true;
			
			//workaround for Mozilla bug https://bugzilla.mozilla.org/show_bug.cgi?id=167801
			if(BrowserUtility.useIFrame())
			{
				//show iframe under dialog
				Element.show( this.__iframe );
			}
			
			this.__setWidth();
				
			BirtPosition.center( this.__instance );
			
			// workaround for IE7 in rtl mode
			if ( BrowserUtility.isIE7 && rtl )
			{
				// force refreshing the DIV elements,
				// else their positioning might become brokem after opening
				// the same dialog box twice...
				var titleContainer = $(this.htmlId + "dialogTitleBar"); 
				if ( titleContainer )
				{
					titleContainer.style.direction = "rtl";
					var elements = titleContainer.getElementsByTagName("div");
					for ( var i = 0; i < elements.length; i++ )
					{
						var el = elements[i];
						el.style.display = "none";
						el.style.display = "block";
					}
				}
			}
			
			Event.observe( window, 'resize', this.__neh_resize_closure, false );
			Event.observe( document, 'mouseup', this.disposeSelection_closure, false );			
		}
		
		this.__postShow();	
	},
	

	/**
	Called right before element is shown
	*/
	__preShow: function()
	{
		//implementation is left to extending class
	},
	
	/**
	Called after element is shown
	*/
	__postShow: function()
	{
		//implementation is left to extending class
	},
	
	/**
	 *	Handle native event 'click'.
	 *
	 *	@event, incoming browser native event
	 *	@return, void
	 */
	__l_hide : function( )
	{
		this.__preHide();
		Event.stopObserving( window, 'resize', this.__neh_resize_closure, false );
		Event.stopObserving( document, 'mouseup', this.disposeSelection_closure, false );
		Element.hide( this.__instance, this.__iframe );
		this.visible = false;
		Mask.hide();
	},
		
	/**
	Called before element is hidden
	*/
	__preHide: function()
	{
		//implementation is left to extending class
	},
	
	/**
	Enables/disabled OK button
	@param boolean enabled
	*/
	__setOKButtonState: function(enabled)
	{
		if(enabled)
		{
			if(this.okBtn.className == "dialogBtnBarButtonDisabled")
			{
				this.okBtn.className = "dialogBtnBarButtonEnabled";
				if ( this.okBtnLeft )
				{
					this.okBtnLeft.className = "dialogBtnBarButtonLeftBackgroundEnabled";
				}
				if ( this.okBtnRight )
				{
					this.okBtnRight.className = "dialogBtnBarButtonRightBackgroundEnabled";
				}
				Event.observe( this.okBtn, 'click', this.__neh_okay_closure , false );
			}
		}
		else
		{
			this.okBtn.className = "dialogBtnBarButtonDisabled";
			if ( this.okBtnLeft )
			{
				this.okBtnLeft.className = "dialogBtnBarButtonLeftBackgroundDisabled";
			}
			if ( this.okBtnRight )
			{
				this.okBtnRight.className = "dialogBtnBarButtonRightBackgroundDisabled";
			}
			Event.stopObserving( this.okBtn, 'click', this.__neh_okay_closure , false );
		}
	},
	
	/**
	Stop event
	*/
	__neh_stopEvent: function(event)
	{
		Event.stop(event);
	},
	
	/**
	Handle mouse down
	*/
	__neh_mousedown: function(event)
	{
		debug("AbstractBaseDialog __neh_mousedown");
		
		//Event.stop(event);
		var target = Event.element( event );
		
		Event.observe( target, 'mouseup', this.mouseup_closure , false );
		Event.observe( target, 'mousemove', this.drag_closure , false );
	},
	
	/**
	Handle mouse up
	*/
	__neh_mouseup: function(event)
	{
		var target = Event.element( event );

		Event.stopObserving( target, 'mouseup',  this.mouseup_closure , false );
		Event.stopObserving( target, 'mousemove', this.drag_closure , false );
	},
	
	/**
	Handle mousemove 
	*/
	__neh_drag: function(event)
	{
		debug("Mouse move");
		Event.stop( event );

		var target = Event.element( event );
		Event.stopObserving( target, 'mouseup',  this.mouseup_closure , false );
		Event.stopObserving( target, 'mousemove', this.drag_closure , false );
					
		DragDrop.startDrag(this.__instance, event, null);
	},
	
	/**
	* Handle cancel selection
	*/
	__neh_disposeSelection: function(event)
	{
		if ( !this.__allowSelection )
		{
			if(document.selection)
			{
				document.selection.empty();
			}
			else if(window.getSelection)
			{
				var selection = window.getSelection();
				if(selection)
				{
					selection.removeAllRanges();
				}
			}
		}
	},

	/**
	 *	Handle enable selection for dialog controls.
	 *
	 *	@obj, incoming target object
	 *	@return, void
	 */	
	__enableSelection: function( obj )
	{
		Event.observe( obj, 'select', this.enableSelection_closure , false );
		Event.observe( obj, 'selectstart', this.enableSelection_closure , false );
		Event.observe( obj, 'mouseup', this.enableSelection_closure , false );
	},

	/**
	 *	Handle enable selection event.
	 *
	 *	@event, incoming browser native event
	 *	@return, void
	 */	
	__neh_enableSelection: function( event )
	{
		event.cancelBubble = true;
	},
		
	/**
	 *	Handle native event 'resize'.
	 *
	 *	@event, incoming browser native event
	 *	@return, void
	 */
	__neh_resize : function( event )
	{
		BirtPosition.center( this.__instance );
	},
	
	__neh_cancel: function()
	{
		this.__l_hide( );
	},
	
	__neh_okay: function()
	{
		this.__okPress( );
	},

	/**
	 ABSTRACT - Handle clicking on ok.
	*/
	__okPress: function( )
	{
		//ABSTRACT - needs to be implemented by extending class
	},

	//TODO change so called once
	__setWidth: function()
	{	
		// In Mozilla 1.4 or lower version, if don't set overflow as "auto",then
		// clientWidth/clientHeight always return zero. The display is incorrect.
		// So add the following section.
		if ( this.__instance.clientWidth <= 0)
		{
			this.__instance.style.overflow = "auto";
		}
		
		var contentHolder = $(this.contentHolderName);
		var innerWidth = contentHolder.offsetWidth;
		var outerWidth = this.__instance.clientWidth;
		var difference = outerWidth - innerWidth;			
		contentHolder.style.width = this.contentHolderWidth + 'px';
		var newOuterWidth = contentHolder.offsetWidth + difference;
		this.__instance.style.width = newOuterWidth + 'px';
			
		this.__iframe.style.width = this.__instance.offsetWidth + 'px';
		this.__iframe.style.height = this.__instance.offsetHeight + 'px';
		
		//move iframe to true top, left
		//assumes that top/bottom left/right borders are same width
		if(this.__iframe.clientWidth > 0)
		{
			this.__iframe.style.top = (this.__instance.clientHeight - this.__instance.offsetHeight)/2 + 'px';
			this.__iframe.style.left = (this.__instance.clientWidth - this.__instance.offsetWidth)/2 + 'px';
		}
	},
	
	/**
	 * Shows or hide the title bar.
	 * @param visible visibility flag
	 */
	__setTitleBarVisibile : function(visible)
	{
		// Hide dialog title bar if embedded in designer.
		var titleBar = $( this.htmlId + 'dialogTitleBar' );
		titleBar.style.display = visible?'inline':'none';			
	},
	
	/**
	@returns html id attribute of associated html element for this dialog
	*/
	getHtmlId: function()
	{
		return this.htmlId;
	},
	
	/**
	 * This event handler is called whenever an operation has been cancelled.
	 * If the dialog box is visible, sets the cancelled flag to true.
	 */
	__beh_cancelOperation : function()
	{
		if ( this.visible )
		{
			this.__operationCancelled = true;
		}
	}
	
}