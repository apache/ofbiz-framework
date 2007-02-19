/*
	Copyright (c) 2004-2006, The Dojo Foundation
	All Rights Reserved.

	Licensed under the Academic Free License version 2.1 or above OR the
	modified BSD license. For more information on Dojo licensing, see:

		http://dojotoolkit.org/community/licensing.shtml
*/

dojo.provide("dojo.widget.Wizard");

dojo.require("dojo.widget.*");
dojo.require("dojo.widget.LayoutContainer");
dojo.require("dojo.widget.ContentPane");
dojo.require("dojo.event.*");
dojo.require("dojo.html.style");

// TODO: base this on PageContainer
dojo.widget.defineWidget(
	"dojo.widget.WizardContainer",
	dojo.widget.LayoutContainer,
{
	// summary
	//		A set of panels that display sequentially, typically notating a step-by-step
	//		procedure like an install
	
	templatePath: dojo.uri.dojoUri("src/widget/templates/Wizard.html"),
	templateCssPath: dojo.uri.dojoUri("src/widget/templates/Wizard.css"),

	// selected: DomNode
	//		Currently selected panel.  (Read-only)
	selected: null,

	// nextButtonLabel: String
	//		Label for the "Next" button.
	nextButtonLabel: "next",

	// previousButtonLabel: String
	//		Label for the "Previous" button.
	previousButtonLabel: "previous",

	// cancelButtonLabel: String
	//		Label for the "Cancel" button.
	cancelButtonLabel: "cancel",

	// doneButtonLabel: String
	//		Label for the "Done" button.
	doneButtonLabel: "done",

	// cancelButtonLabel: FunctionName
	//		Name of function to call if user presses cancel button.
	//		Cancel button is not displayed if function is not specified.
	cancelFunction: "",

	// hideDisabledButtons: Boolean
	//		If true, disabled buttons are hidden; otherwise, they are assigned the
	//		"WizardButtonDisabled" CSS class
	hideDisabledButtons: false,

	fillInTemplate: function(args, frag){
		dojo.event.connect(this.nextButton, "onclick", this, "_onNextButtonClick");
		dojo.event.connect(this.previousButton, "onclick", this, "_onPreviousButtonClick");
		if (this.cancelFunction){
			dojo.event.connect(this.cancelButton, "onclick", this.cancelFunction);
		}else{
			this.cancelButton.style.display = "none";
		}
		dojo.event.connect(this.doneButton, "onclick", this, "done");
		this.nextButton.value = this.nextButtonLabel;
		this.previousButton.value = this.previousButtonLabel;
		this.cancelButton.value = this.cancelButtonLabel;
		this.doneButton.value = this.doneButtonLabel;
	},

	_checkButtons: function(){
		var lastStep = !this.hasNextPanel();
		this.nextButton.disabled = lastStep;
		this._setButtonClass(this.nextButton);
		if(this.selected.doneFunction){
			this.doneButton.style.display = "";
			// hide the next button if this is the last one and we have a done function
			if(lastStep){
				this.nextButton.style.display = "none";
			}
		}else{
			this.doneButton.style.display = "none";
		}
		this.previousButton.disabled = ((!this.hasPreviousPanel()) || (!this.selected.canGoBack));
		this._setButtonClass(this.previousButton);
	},

	_setButtonClass: function(button){
		if(!this.hideDisabledButtons){
			button.style.display = "";
			dojo.html.setClass(button, button.disabled ? "WizardButtonDisabled" : "WizardButton");
		}else{
			button.style.display = button.disabled ? "none" : "";
		}
	},

	registerChild: function(panel, insertionIndex){
		dojo.widget.WizardContainer.superclass.registerChild.call(this, panel, insertionIndex);
		this.wizardPanelContainerNode.appendChild(panel.domNode);
		panel.hide();

		if(!this.selected){
			this.onSelected(panel);
		}
		this._checkButtons();
	},

	onSelected: function(/*WizardPanel*/ panel){
		// summary: Callback when new panel is selected..  Deselect old panel and select new one
		if(this.selected ){
			if (this.selected._checkPass()) {
				this.selected.hide();
			} else {
				return;
			}
		}
		panel.show();
		this.selected = panel;
	},

	getPanels: function() {
		// summary: returns array of WizardPane children
		return this.getChildrenOfType("WizardPane", false);		// WizardPane[]
	},

	selectedIndex: function() {
		// summary: Returns index (into this.children[]) for currently selected child.
		if (this.selected) {
			return dojo.lang.indexOf(this.getPanels(), this.selected);	// Integer
		}
		return -1;
	},

	_onNextButtonClick: function() {
		// summary: callback when next button is clicked
		var selectedIndex = this.selectedIndex();
		if ( selectedIndex > -1 ) {
			var childPanels = this.getPanels();
			if (childPanels[selectedIndex + 1]) {
				this.onSelected(childPanels[selectedIndex + 1]);
			}
		}
		this._checkButtons();
	},

	_onPreviousButtonClick: function() {
		// summary: callback when previous button is clicked
		var selectedIndex = this.selectedIndex();
		if ( selectedIndex > -1 ) {
			var childPanels = this.getPanels();
			if (childPanels[selectedIndex - 1]) {
				this.onSelected(childPanels[selectedIndex - 1]);
			}
		}
		this._checkButtons();
	},

	hasNextPanel: function() {
		// summary: Returns true if there's a another panel after the current panel
		var selectedIndex = this.selectedIndex();
		return (selectedIndex < (this.getPanels().length - 1));
	},

	hasPreviousPanel: function() {
		// summary: Returns true if there's a panel before the current panel
		var selectedIndex = this.selectedIndex();
		return (selectedIndex > 0);
	},

	done: function() {
		// summary: Finish the wizard's operation
		this.selected.done();
	}
});

dojo.widget.defineWidget(
	"dojo.widget.WizardPane",
	dojo.widget.ContentPane,
{
	// summary
	//		a panel in a WizardContainer

	// canGoBack: Boolean
	//		If true, then can move back to a previous panel (by clicking the "Previous" button)
	canGoBack: true,

	// passFunction: String
	//		Name of function that checks if it's OK to advance to the next panel.
	//		If it's not OK (for example, mandatory field hasn't been entered), then
	//		returns an error message (String) explaining the reason.
	passFunction: "",
	
	// doneFunction: String
	//		Name of function that is run if you press the "Done" button from this panel
	doneFunction: "",

	postMixInProperties: function(args, frag) {
		if (this.passFunction) {
			this.passFunction = dj_global[this.passFunction];
		}
		if (this.doneFunction) {
			this.doneFunction = dj_global[this.doneFunction];
		}
		dojo.widget.WizardPane.superclass.postMixInProperties.apply(this, arguments);
	},

	_checkPass: function() {
		// summary:
		//		Called when the user presses the "next" button.
		//		Calls passFunction to see if it's OK to advance to next panel, and
		//		if it isn't, then display error.
		//		Returns true to advance, false to not advance.
		if (this.passFunction && dojo.lang.isFunction(this.passFunction)) {
			var failMessage = this.passFunction();
			if (failMessage) {
				alert(failMessage);
				return false;
			}
		}
		return true;
	},

	done: function() {
		if (this.doneFunction && dojo.lang.isFunction(this.doneFunction)) {
			this.doneFunction();
		}
	}
});
