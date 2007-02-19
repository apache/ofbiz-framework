/*
	Copyright (c) 2004-2006, The Dojo Foundation
	All Rights Reserved.

	Licensed under the Academic Free License version 2.1 or above OR the
	modified BSD license. For more information on Dojo licensing, see:

		http://dojotoolkit.org/community/licensing.shtml
*/

dojo.provide("dojo.widget.Select");

dojo.require("dojo.widget.ComboBox");
dojo.require("dojo.widget.*");
dojo.require("dojo.widget.html.stabile");

dojo.widget.defineWidget(
	"dojo.widget.Select",
	dojo.widget.ComboBox,
	{
		/*
		 * summary
		 *	Enhanced version of HTML's <select> tag.
		 *
		 *	Similar features:
		 *	  - There is a drop down list of possible values.
		 *    - You can only enter a value from the drop down list.  (You can't enter an arbitrary value.)
		 *    - The value submitted with the form is the hidden value (ex: CA),
		 *      not the displayed value a.k.a. label (ex: California)
		 *
		 *	Enhancements over plain HTML version:
		 *    - If you type in some text then it will filter down the list of possible values in the drop down list.
		 *    - List can be specified either as a static list or via a javascript function (that can get the list from a server)
		 */

		//	This value should not be changed by the user
		forceValidOption: true,

		setValue: function(value) {
			// summary
			//	Sets the value of the combobox.
			//	TODO: this doesn't work correctly when a URL is specified, because we can't
			//	set the label automatically (based on the specified value)
			this.comboBoxValue.value = value;
			dojo.widget.html.stabile.setState(this.widgetId, this.getState(), true);
			this.onValueChanged(value);
		},

		setLabel: function(value){
			// summary
			//	FIXME, not sure what to do here!
			//	Users shouldn't call this function; they should be calling setValue() instead
			this.comboBoxSelectionValue.value = value;
			if (this.textInputNode.value != value) { // prevent mucking up of selection
				this.textInputNode.value = value;
			}
		},	  

		getLabel: function(){
			// summary: returns current label
			return this.comboBoxSelectionValue.value;	// String
		},

		getState: function() {
			// summary: returns current value and label
			return {
				value: this.getValue(),
				label: this.getLabel()
			};	// Object
		},

		onKeyUp: function(/*Event*/ evt){
			// summary: internal function
			this.setLabel(this.textInputNode.value);
		},

		setState: function(/*Object*/ state) {
			// summary: internal function to set both value and label
			this.setValue(state.value);
			this.setLabel(state.label);
		},

		setAllValues: function(/*String*/ value1, /*String*/ value2){
			// summary: internal function to set both value and label
			this.setLabel(value1);
			this.setValue(value2);
		}
	}
);
