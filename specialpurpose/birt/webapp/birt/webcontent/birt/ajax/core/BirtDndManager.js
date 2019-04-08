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

BirtDndManager = function( ) { 

	//An item is dragging
this.isDragging = false;

};

BirtDndManager.prototype = {

	__dropTargetManager: null, //PRIVATE - extending classes must use accessor method
	
	__mousemoveHandlerFunction: null,
	
	__dropHandlerFunction: null,
		
	currentDropTarget: null, //Element that is the current valid drop target
	
	currentDragElement: null, //Element that is currently being dragged
	
	
	/**
	Extending classes should use this method to set the drop target manager if
	custom drop behavior is desired
	@param {DropTargetManager}
	*/
	setDropTargetManager: function( dTargetManager)
	{
		this.__dropTargetManager = dTargetManager;
	},
		
	/**
	Drop targets call this method to register as accepting "dragType" items
	*/		
	addAssociation: function(dragType, targetHtmlId, target)
	{
		if(this.__dropTargetManager == null)
		{
			throw new WRError("BirtDndManager", "DropTargetManager not set");
		}
		this.__dropTargetManager.addAssociation(dragType, targetHtmlId, target);
	},
	
	/**
	Delete an association when the corresponding html element is removed
	@param dragType
	@param dropTargetId id attribute of html element to remove
	*/
	deleteAssociation: function(dragType, htmlId)
	{
		if(this.__dropTargetManager == null)
		{
			throw new WRError("BirtDndManager", "DropTargetManager not set");
		}
		this.__dropTargetManager.deleteAssociation(dragType, htmlId);
	},
	
	/**
	startDrag redefines itself the first time it is called based on the desired type of drag drop behavior.<b>
	*/ 
	startDrag: function(element, event, dragType)
	{
			//If there is no drop target manager, define startDrag as default
		if(this.__dropTargetManager == null)
		{
			this.startDrag = this.__getDefaultStartDrag();			
		}
		else
		{
			this.startDrag = this.__getCustomStartDrag();
		}
		this.startDrag(element, event, dragType);
	},
	
	/**
	Default start drag function handles elements that drag but do not have drop targets
	@returns {function} default start drag function
	*/
	__getDefaultStartDrag: function()
	{
		return function(element, event, dragType)
		{
			debug("default startDrag");
			if(!this.isDragging)
			{
				this.eventMouseout = this.elementMouseout.bindAsEventListener(this);
			
				this.__mousemoveHandlerFunction = this.__moveElement.bindAsEventListener(this);
				this.__dropElementFunction = this.__dropElement.bindAsEventListener(this);
			
				var zIndex = this.__activateDragMask();
				this.__elementSetUp(element, event, zIndex);
				this.__startDragObservers(this.__mousemoveHandlerFunction, this.__dropElementFunction);
			}
			this.isDragging = true;
		}
	},
	
	/**
	Custom start drag function handles elements that have drop targets
	@returns {function} custome start drag function
	*/
	__getCustomStartDrag: function()
	{	
		return function(element, event, dragType)
		{
			debug("custom startDrag");
			if(!this.isDragging)
			{
				
				if(dragType == null) //There are no drop targets
				{
					this.__mousemoveHandlerFunction = this.__moveElement.bindAsEventListener(this);
					this.__dropElementFunction = this.__dropElement.bindAsEventListener(this);
				}
				else
				{
					this.__dropTargetManager.setUpForDrag(dragType);
					this.__mousemoveHandlerFunction = this.__moveElementWithTarget.bindAsEventListener(this);
					this.__dropElementFunction = this.__dropElementWithTarget.bindAsEventListener(this);				
				}		
				this.eventMouseout = this.elementMouseout.bindAsEventListener(this);
				
				var zIndex = this.__activateDragMask();
				this.__elementSetUp(element, event, zIndex);
				this.__startDragObservers(this.__mousemoveHandlerFunction, this.__dropElementFunction);
			}
			this.isDragging = true;
		}
	},
	
	__activateDragMask: function()
	{
		return Mask.show();
	},
	
	__deactivateDragMask: function()
	{
		Mask.hide();
	},

	/**
	Set up an element to be dragged
	*/
	__elementSetUp: function(elem, event, zIndex)
	{	
		//TODO if removing selections is kept, move to utility funciton
		//remove any existing selections
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
	
		this.currentDragElement = elem;
		this.currentDragElement.oldZIndex = this.currentDragElement.style.zIndex;
		this.currentDragElement.style.zIndex = zIndex;
		this.currentDropTarget = null;
			//Location of mouse
		var mouseX = Event.pointerX(event);
		var mouseY = Event.pointerY(event);
			//Distance from the edge of the element to edge of browser
		var offsets = Position.cumulativeOffset(elem);
			//Distance from the edge of the element of mouse
		elem.offsetX =  (mouseX - offsets[0]);
		elem.offsetY =  (mouseY - offsets[1]);
			
			//Used for revert effect
		elem.originalTop = parseInt(elem.style.top || '0');
		elem.originalLeft = parseInt(elem.style.left || '0');		 
	},
	
	/**
	Start drag event observing
	*/
	__startDragObservers: function(mousemoveHandlerFunction, dropHandlerFunction)
	{	
		Event.observe(document, "mousemove", mousemoveHandlerFunction, false);
		Event.observe(document, "mouseup", dropHandlerFunction, false);
	},	
	
	/**
	Move element
	*/
	__moveElement: function(event)
	{
		var xPos = Event.pointerX(event);
		var yPos = Event.pointerY(event);
		
		this.__moveCurrentDragElement(xPos, yPos);				
	},
		
	/**
	Move element and use DropTargetManager to check if it can be dropped
	*/
	__moveElementWithTarget: function(event)
	{	
		var xPos = Event.pointerX(event);
		var yPos = Event.pointerY(event);
		
		this.__moveCurrentDragElement(xPos, yPos);
		
		var oldDropTarget = this.currentDropTarget; //is the state changing?
		var dropTarget = this.__dropTargetManager.getDropTarget(xPos, yPos);
		
			//Change indicator to drop allowed
		if(dropTarget)
		{
			this.currentDropTarget = dropTarget;				
			if(!oldDropTarget) 
			{
				var nodes = this.currentDragElement.childNodes;
				this.toggleDropIndicator(true, nodes);
			}
		}
		else //Change indicator to drop forbidden
		{
			this.currentDropTarget = null;
			if(oldDropTarget)
			{
				var nodes = this.currentDragElement.childNodes;
				this.toggleDropIndicator(false, nodes);
			}
		}			
	},
	
	__moveCurrentDragElement: function(x, y)
	{
		var offsets = Position.cumulativeOffset(this.currentDragElement);
	    	//subtract the element's current left, top coordinates from the offsets
	    offsets[0] -= parseInt(this.currentDragElement.style.left || '0');
	    offsets[1] -= parseInt(this.currentDragElement.style.top || '0');
	        
	    var style = this.currentDragElement.style;
	     
	    	//take current mouse position, subtract difference in drag object position, s
	    style.left = (x - offsets[0] - this.currentDragElement.offsetX) + "px";
	    style.top  = (y - offsets[1] - this.currentDragElement.offsetY) + "px";   
	},
	
	/**
	Check if the mouse button is no longer pressed, if not, call
	eventDropped. (IE specific)<b>
	*/ 
	__mouseStillDown: function(event)
	{
		/**
		Check that the mouse button is still down
		(used to detect roll off the screen in IE)
		*/
		//debug("check mouse down");
			//TODO refine this Check that element is really moving	
		if(!event.which && event.button == "0")
		{
			this.eventDropped(event);
			return true;
		}
		return false;
	},
	
		
	/**
	Handle mouseout of drag element
	*/
	elementMouseout: function(event)
	{
		//debug("mouseout");
		var target = Event.element( event );
		if(target.tagName.toLowerCase() == "iframe")
		{
			Event.stop(event);
			this.eventDropped(event);
		}
		
	},
	
	__dropElement: function(event)
	{
		this.isDragging = false;

	    Event.stopObserving(document, "mousemove", this.__mousemoveHandlerFunction);
	    Event.stopObserving(document, "mouseup", this.__dropElementFunction);
	    Event.stopObserving(document, "mouseout", this.eventMouseout);
	    this.__deactivateDragMask();
		this.currentDragElement.style.zIndex = this.currentDragElement.oldZIndex;
	},
	
	__dropElementWithTarget: function(event)
	{
		this.__dropElement(event);
	    
	    this.__dropTargetManager.dropElementWithTarget(event, this.currentDragElement, this.currentDropTarget);
	},
	
     
     /**
     Toggle drop indicator to show if item can be dropped
     @param accept {boolean} Indicates if item is accepted
     @param nodes {Array} Potential imageHolder nodes //TODO once indicator is decided on can be made more efficient
     */
     toggleDropIndicator: function(accept, nodes)
     {     	
		for(var j = 0 ; j < nodes.length ; j++)
		{
			if(nodes[j].imageHolder)
			{
				accept ? nodes[j].childNodes[0].style.display = "none" : nodes[j].childNodes[0].style.display = "block";
				accept ? nodes[j].childNodes[1].style.display = "block" : nodes[j].childNodes[1].style.display = "none";
				break;
			}
		}
     }
       
}