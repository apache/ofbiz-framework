// Copyright (c) 2005 Thomas Fakes (http://craz8.com)
// 
// This code is substantially based on code from script.aculo.us which has the 
// following copyright and permission notice
//
// Copyright (c) 2005 Thomas Fuchs (http://script.aculo.us, http://mir.aculo.us)
// 
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
// 
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

var Resizeable = Class.create();
Resizeable.prototype = {
  initialize: function(element) {
    var options = Object.extend({
      top: 6,
      bottom: 6,
      left: 6,
      right: 6,
      minHeight: 0,
      minWidth: 0,
      zindex: 1000,
      resize: null,
      duringresize: null
    }, arguments[1] || {});

    this.element      = $(element);
    this.handle 	  = this.element;

    Element.makePositioned(this.element); // fix IE    

    this.options      = options;

    this.active       = false;
    this.resizing     = false;   
    this.currentDirection = '';

    this.eventMouseDown = this.startResize.bindAsEventListener(this);
    this.eventMouseUp   = this.endResize.bindAsEventListener(this);
    this.eventMouseMove = this.update.bindAsEventListener(this);
    this.eventCursorCheck = this.cursor.bindAsEventListener(this);
    this.eventKeypress  = this.keyPress.bindAsEventListener(this);
    
    this.registerEvents();
  },
  destroy: function() {
    Event.stopObserving(this.handle, "mousedown", this.eventMouseDown);
    this.unregisterEvents();
  },
  registerEvents: function() {
    Event.observe(document, "mouseup", this.eventMouseUp);
    Event.observe(document, "mousemove", this.eventMouseMove);
    Event.observe(document, "keypress", this.eventKeypress);
    Event.observe(this.handle, "mousedown", this.eventMouseDown);
    Event.observe(this.element, "mousemove", this.eventCursorCheck);
  },
  unregisterEvents: function() {
    //if(!this.active) return;
    //Event.stopObserving(document, "mouseup", this.eventMouseUp);
    //Event.stopObserving(document, "mousemove", this.eventMouseMove);
    //Event.stopObserving(document, "mousemove", this.eventCursorCheck);
    //Event.stopObserving(document, "keypress", this.eventKeypress);
  },
  startResize: function(event) {
    if(Event.isLeftClick(event)) {
      
      // abort on form elements, fixes a Firefox issue
      var src = Event.element(event);
      if(src.tagName && (
        src.tagName=='INPUT' ||
        src.tagName=='SELECT' ||
        src.tagName=='BUTTON' ||
        src.tagName=='TEXTAREA')) return;

	  var dir = this.directions(event);
	  if (dir.length > 0) {      
	      this.active = true;
    	  var offsets = Position.cumulativeOffset(this.element);
	      this.startTop = offsets[1];
	      this.startLeft = offsets[0];
	      this.startWidth = parseInt(Element.getStyle(this.element, 'width'));
	      this.startHeight = parseInt(Element.getStyle(this.element, 'height'));
	      this.startX = event.clientX + document.body.scrollLeft + document.documentElement.scrollLeft;
	      this.startY = event.clientY + document.body.scrollTop + document.documentElement.scrollTop;
	      
	      this.currentDirection = dir;
	      Event.stop(event);
	  }
    }
  },
  finishResize: function(event, success) {
    // this.unregisterEvents();

    this.active = false;
    this.resizing = false;

    if(this.options.zindex)
      this.element.style.zIndex = this.originalZ;
      
    if (this.options.resize) {
    	this.options.resize(this.element);
    }
  },
  keyPress: function(event) {
    if(this.active) {
      if(event.keyCode==Event.KEY_ESC) {
        this.finishResize(event, false);
        Event.stop(event);
      }
    }
  },
  endResize: function(event) {
    if(this.active && this.resizing) {
      this.finishResize(event, true);
      Event.stop(event);
    }
    this.active = false;
    this.resizing = false;
  },
  draw: function(event) {
    var pointer = [Event.pointerX(event), Event.pointerY(event)];
    var style = this.element.style;
    if (this.currentDirection.indexOf('n') != -1) {
    	var pointerMoved = this.startY - pointer[1];
    	var margin = Element.getStyle(this.element, 'margin-top') || "0";
    	var newHeight = this.startHeight + pointerMoved;
    	if (newHeight > this.options.minHeight) {
    		style.height = newHeight + "px";
    		style.top = (this.startTop - pointerMoved - parseInt(margin)) + "px";
    	}
    }
    if (this.currentDirection.indexOf('w') != -1) {
    	var pointerMoved = this.startX - pointer[0];
    	var margin = Element.getStyle(this.element, 'margin-left') || "0";
    	var newWidth = this.startWidth + pointerMoved;
    	if (newWidth > this.options.minWidth) {
    		style.left = (this.startLeft - pointerMoved - parseInt(margin))  + "px";
    		style.width = newWidth + "px";
    	}
    }
    if (this.currentDirection.indexOf('s') != -1) {
    	var newHeight = this.startHeight + pointer[1] - this.startY;
    	if (newHeight > this.options.minHeight) {
    		style.height = newHeight + "px";
    	}
    }
    if (this.currentDirection.indexOf('e') != -1) {
    	var newWidth = this.startWidth + pointer[0] - this.startX;
    	if (newWidth > this.options.minWidth) {
    		style.width = newWidth + "px";
    	}
    }
    if(style.visibility=="hidden") style.visibility = ""; // fix gecko rendering
  },
  between: function(val, low, high) {
  	return (val >= low && val < high);
  },
  directions: function(event) {
    var pointer = [Event.pointerX(event), Event.pointerY(event)];
    var offsets = Position.cumulativeOffset(this.element);
    
	var cursor = '';
	if (this.between(pointer[1] - offsets[1], 0, this.options.top)) cursor += 'n';
	if (this.between((offsets[1] + this.element.offsetHeight) - pointer[1], 0, this.options.bottom)) cursor += 's';
	if (this.between(pointer[0] - offsets[0], 0, this.options.left)) cursor += 'w';
	if (this.between((offsets[0] + this.element.offsetWidth) - pointer[0], 0, this.options.right)) cursor += 'e';

	return cursor;
  },
  cursor: function(event) {
  	var cursor = this.directions(event);
	if (cursor.length > 0) {
		cursor += '-resize';
	} else {
		cursor = '';
	}
	this.element.style.cursor = cursor;		
  },
  update: function(event) {
   if(this.active) {
      if(!this.resizing) {
        var style = this.element.style;
        this.resizing = true;
        
        if(Element.getStyle(this.element,'position')=='') 
          style.position = "relative";
        
        if(this.options.zindex) {
          this.originalZ = parseInt(Element.getStyle(this.element,'z-index') || 0);
          style.zIndex = this.options.zindex;
        }
      }
      this.draw(event);
      if(this.options.duringresize) { this.options.duringresize(this.element); } 
      // fix AppleWebKit rendering
      if(navigator.appVersion.indexOf('AppleWebKit')>0) window.scrollBy(0,0); 
      Event.stop(event);
      return false;
   }
  }
}
