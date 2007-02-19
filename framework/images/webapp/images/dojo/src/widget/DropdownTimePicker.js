/*
	Copyright (c) 2004-2006, The Dojo Foundation
	All Rights Reserved.

	Licensed under the Academic Free License version 2.1 or above OR the
	modified BSD license. For more information on Dojo licensing, see:

		http://dojotoolkit.org/community/licensing.shtml
*/

dojo.provide("dojo.widget.DropdownTimePicker");

dojo.require("dojo.widget.*");
dojo.require("dojo.widget.DropdownContainer");
dojo.require("dojo.widget.TimePicker");
dojo.require("dojo.event.*");
dojo.require("dojo.html.*");
dojo.require("dojo.date.format");
dojo.require("dojo.date.serialize");
dojo.require("dojo.i18n.common");
dojo.requireLocalization("dojo.widget", "DropdownTimePicker", null, "ROOT");

dojo.widget.defineWidget(
	"dojo.widget.DropdownTimePicker",
	dojo.widget.DropdownContainer,
	{
		/*
		summary: 
			A form input for entering times with a pop-up dojo.widget.TimePicker to aid in selection
		
			description: 
				This is TimePicker in a DropdownContainer, it supports all features of TimeePicker.
		
				It is possible to customize the user-visible formatting with either the formatLength or 
				displayFormat attributes.  The value sent to the server is typically a locale-independent 
				value in a hidden field as defined by the name attribute. RFC3339 representation is used 
				by default, but other options are available with saveFormat.
		
			usage: 
				var dtp = dojo.widget.createWidget("DropdownTimePicker", {},   
				dojo.byId("DropdownTimePickerNode")); 
		 	 
				<input dojoType="DropdownTimePicker">
		*/

		// iconURL: URL
		//	path of icon for button to display time picker widget
		iconURL: dojo.uri.dojoUri("src/widget/templates/images/timeIcon.gif"),
		
		// formatLength: String
		//	Type of formatting used for visual display, appropriate to locale (choice of long, short, medium or full)
		//	See dojo.date.format for details.
		formatLength: "short",

		// displayFormat: String
		//	A pattern used for the visual display of the formatted date.
		//	Setting this overrides the default locale-specific settings as well as the formatLength
		//	attribute.  See dojo.date.format for a reference which defines the formatting patterns.
		displayFormat: "",

		timeFormat: "", // deprecated, will be removed for 0.5

//FIXME: need saveFormat attribute support
		// saveFormat: String
		//	Formatting scheme used when submitting the form element.  This formatting is used in a hidden
		//  field (name) intended for server use, and is therefore typically locale-independent.
		//  By default, uses rfc3339 style date formatting (rfc)
		//	Use a pattern string like displayFormat or one of the following:
		//	rfc|iso|posix|unix
		saveFormat: "",

		// value: String|Date
		//	form value property in rfc3339 format. ex: 12:00
		value: "",

		// name: String
		// 	name of the form element, used to create a hidden field by this name for form element submission.
		name: "",

		postMixInProperties: function() {
			// summary: see dojo.widget.DomWidget
			dojo.widget.DropdownTimePicker.superclass.postMixInProperties.apply(this, arguments);
			var messages = dojo.i18n.getLocalization("dojo.widget", "DropdownTimePicker", this.lang);
			this.iconAlt = messages.selectTime;

			//FIXME: should this be if/else/else?
			if(typeof(this.value)=='string'&&this.value.toLowerCase()=='today'){
				this.value = new Date();
			}
			if(this.value && isNaN(this.value)){
				var orig = this.value;
				this.value = dojo.date.fromRfc3339(this.value);
				if(!this.value){
					var d = dojo.date.format(new Date(), { selector: "dateOnly", datePattern: "yyyy-MM-dd" });
					var c = orig.split(":");
					for(var i = 0; i < c.length; ++i){
						if(c[i].length == 1) c[i] = "0" + c[i];
					}
					orig = c.join(":");
					this.value = dojo.date.fromRfc3339(d + "T" + orig);
					dojo.deprecated("dojo.widget.DropdownTimePicker", "time attributes must be passed in Rfc3339 format", "0.5");
				}
			}
			if(this.value && !isNaN(this.value)){
				this.value = new Date(this.value);
			}
		},

		fillInTemplate: function(){
			// summary: see dojo.widget.DomWidget

			dojo.widget.DropdownTimePicker.superclass.fillInTemplate.apply(this, arguments);

			var value = "";
			if(this.value instanceof Date) {
				value = this.value;
			} else if(this.value) {
				var orig = this.value;
				var d = dojo.date.format(new Date(), { selector: "dateOnly", datePattern: "yyyy-MM-dd" });
				var c = orig.split(":");
				for(var i = 0; i < c.length; ++i){
					if(c[i].length == 1) c[i] = "0" + c[i];
				}
				orig = c.join(":");
				value = dojo.date.fromRfc3339(d + "T" + orig);
			}
			
			var tpArgs = { widgetContainerId: this.widgetId, lang: this.lang, value: value };
			this.timePicker = dojo.widget.createWidget("TimePicker", tpArgs, this.containerNode, "child");
			
			dojo.event.connect(this.timePicker, "onValueChanged", this, "_updateText");
			
			if(this.value){
				this._updateText();
			}
			this.containerNode.style.zIndex = this.zIndex;
			this.containerNode.explodeClassName = "timeContainer";
			this.valueNode.name = this.name;
		},
		
		getValue: function(){
			// summary: return current time in time-only portion of RFC 3339 format
			return this.valueNode.value; /*String*/
		},

		getTime: function(){
			// summary: return current time as a Date object
			return this.timePicker.storedTime; /*Date*/
		},

		setValue: function(/*Date|String*/rfcDate){
			//summary: set the current time from RFC 3339 formatted string or a date object, synonymous with setTime
			this.setTime(rfcDate);
		},

		setTime: function(/*Date|String*/dateObj){
			// summary: set the current time and update the UI
			var value = "";
			if(dateObj instanceof Date) {
				value = dateObj;
			} else if(this.value) {
				var orig = this.value;
				var d = dojo.date.format(new Date(), { selector: "dateOnly", datePattern: "yyyy-MM-dd" });
				var c = orig.split(":");
				for(var i = 0; i < c.length; ++i){
					if(c[i].length == 1) c[i] = "0" + c[i];
				}
				orig = c.join(":");
				value = dojo.date.fromRfc3339(d + "T" + orig);
			}

			this.timePicker.setTime(value);
			this._syncValueNode();
		},
	
		_updateText: function(){
			// summary: updates the <input> field according to the current value (ie, displays the formatted date)
			if(this.timePicker.selectedTime.anyTime){
				this.inputNode.value = "";
			}else if(this.timeFormat){
				dojo.deprecated("dojo.widget.DropdownTimePicker",
				"Must use displayFormat attribute instead of timeFormat.  See dojo.date.format for specification.", "0.5");
				this.inputNode.value = dojo.date.strftime(this.timePicker.time, this.timeFormat, this.lang);
			}else{
				this.inputNode.value = dojo.date.format(this.timePicker.time,
					{formatLength:this.formatLength, timePattern:this.displayFormat, selector:'timeOnly', locale:this.lang});
			}
			this._syncValueNode();
			this.onValueChanged(this.getTime());
			this.hideContainer();
		},

		onValueChanged: function(/*Date*/dateObj){
			// summary: triggered when this.value is changed
		},
		
		onInputChange: function(){
			// summary: callback when user manually types a time into the <input> field
			if(this.dateFormat){
				dojo.deprecated("dojo.widget.DropdownTimePicker",
				"Cannot parse user input.  Must use displayFormat attribute instead of dateFormat.  See dojo.date.format for specification.", "0.5");
			}else{
				var input = dojo.string.trim(this.inputNode.value);
				if(input){
					var inputTime = dojo.date.parse(input,
							{formatLength:this.formatLength, timePattern:this.displayFormat, selector:'timeOnly', locale:this.lang});			
					if(inputTime){
						this.setTime(inputTime);
					}
				} else {
					this.valueNode.value = input;
				}
			}
			// If the time entered didn't parse, reset to the old time.  KISS, for now.
			//TODO: usability?  should we provide more feedback somehow? an error notice?
			// seems redundant to do this if the parse failed, but at least until we have validation,
			// this will fix up the display of entries like 01/32/2006
			if(input){ this._updateText(); }
		},

		_syncValueNode: function(){
			var time = this.timePicker.time;
			var value;
			switch(this.saveFormat.toLowerCase()){
				case "rfc": case "iso": case "":
					value = dojo.date.toRfc3339(time, 'timeOnly');
					break;
				case "posix": case "unix":
					value = Number(time);
					break;
				default:
					value = dojo.date.format(time, {datePattern:this.saveFormat, selector:'timeOnly', locale:this.lang});
			}
			this.valueNode.value = value;
		},
		
		destroy: function(/*Boolean*/finalize){
			this.timePicker.destroy(finalize);
			dojo.widget.DropdownTimePicker.superclass.destroy.apply(this, arguments);
		}
	}
);
