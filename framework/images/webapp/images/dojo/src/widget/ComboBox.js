/*
	Copyright (c) 2004-2006, The Dojo Foundation
	All Rights Reserved.

	Licensed under the Academic Free License version 2.1 or above OR the
	modified BSD license. For more information on Dojo licensing, see:

		http://dojotoolkit.org/community/licensing.shtml
*/

dojo.provide("dojo.widget.ComboBox");

dojo.require("dojo.widget.*");
dojo.require("dojo.event.*");
dojo.require("dojo.io.*");
dojo.require("dojo.html.*");
dojo.require("dojo.string");
dojo.require("dojo.widget.html.stabile");
dojo.require("dojo.widget.PopupContainer");

dojo.declare(
	"dojo.widget.incrementalComboBoxDataProvider",
	null,
	function(options){
		// summary:
		//		Reference implementation / interface for Combobox incremental data provider.
		//		This class takes a search string and returns values that match
		//		that search string.  The filtering of values (to find values matching given
		//		search string) is done on the server.
		//
		// options:
		//		Structure containing {dataUrl: "foo.js?search={searchString}"} or similar data.
		//		dataUrl is a URL that is passed the search string a returns a JSON structure
		//		showing the matching values, like [ ["Alabama","AL"], ["Alaska","AK"], ["American Samoa","AS"] ]

		this.searchUrl = options.dataUrl;

		// TODO: cache doesn't work
		this._cache = {};

		this._inFlight = false;
		this._lastRequest = null;

		// allowCache: Boolean
		//	Setting to use/not use cache for previously seen values
		//	TODO: caching doesn't work.
		//	TODO: read the setting for this value from the widget parameters
		this.allowCache = false;
	},
	{
		_addToCache: function(/*String*/ keyword, /*Array*/ data){
			if(this.allowCache){
				this._cache[keyword] = data;
			}
		},

		startSearch: function(/*String*/ searchStr, /*Function*/ callback){
			// summary:
			//		Start the search for patterns that match searchStr, and call
			//		specified callback functions with the results
			// searchStr:
			//		The characters the user has typed into the <input>.
			// callback:
			//		This function will be called with the result, as an
			//		array of label/value pairs (the value is used for the Select widget).  Example:
			//		[ ["Alabama","AL"], ["Alaska","AK"], ["American Samoa","AS"] ]

			if(this._inFlight){
				// FIXME: implement backoff!
			}
			var tss = encodeURIComponent(searchStr);
			var realUrl = dojo.string.substituteParams(this.searchUrl, {"searchString": tss});
			var _this = this;
			var request = this._lastRequest = dojo.io.bind({
				url: realUrl,
				method: "get",
				mimetype: "text/json",
				load: function(type, data, evt){
					_this._inFlight = false;
					if(!dojo.lang.isArray(data)){
						var arrData = [];
						for(var key in data){
							arrData.push([data[key], key]);
						}
						data = arrData;
					}
					_this._addToCache(searchStr, data);
					if (request == _this._lastRequest){
						callback(data);
					}
				}
			});
			this._inFlight = true;
		}
	}
);

dojo.declare(
	"dojo.widget.basicComboBoxDataProvider",
	null,
	function(/*Object*/ options, /*DomNode*/ node){
		// summary:
		//		Reference implementation / interface for Combobox data provider.
		//		This class takes a search string and returns values that match
		//		that search string.    All possible values for the combobox are downloaded
		//		on initialization, and then startSearch() runs locally,
		//		merely filting that downloaded list, to find values matching search string
		//
		//		NOTE: this data provider is designed as a naive reference
		//		implementation, and as such it is written more for readability than
		//		speed. A deployable data provider would implement lookups, search
		//		caching (and invalidation), and a significantly less naive data
		//		structure for storage of items.
		//
		//	options: Object
		//		Options object.  Example:
		//		{
		//			dataUrl: String (URL to query to get list of possible drop down values),
		//			setAllValues: Function (callback for setting initially selected value)
		//		}
		//		The return format for dataURL is (for example)
		//			[ ["Alabama","AL"], ["Alaska","AK"], ["American Samoa","AS"] ... ]
		//
		// node:
		//		Pointer to the domNode in the original markup.
		//		This is needed in the case when the list of values is embedded
		//		in the html like <select> <option>Alabama</option> <option>Arkansas</option> ...
		//		rather than specified as a URL.

		// _data: Array
		//		List of every possible value for the drop down list
		//		startSearch() simply searches this array and returns matching values.
		this._data = [];

		// searchLimit: Integer
		//		Maximum number of results to return.
		//		TODO: need to read this value from the widget parameters
		this.searchLimit = 30;

		// searchType: String
		//		Defines what values match the search string; see searchType parameter
		//		of ComboBox for details
		//		TODO: need to read this value from the widget parameters; the setting in ComboBox is being ignored.
		this.searchType = "STARTSTRING";

		// caseSensitive: Boolean
		//		Should search be case sensitive?
		//		TODO: this should be a parameter to combobox?
		this.caseSensitive = false;

		if(!dj_undef("dataUrl", options) && !dojo.string.isBlank(options.dataUrl)){
			this._getData(options.dataUrl);
		}else{
			// check to see if we can populate the list from <option> elements
			if((node)&&(node.nodeName.toLowerCase() == "select")){
				// NOTE: we're not handling <optgroup> here yet
				var opts = node.getElementsByTagName("option");
				var ol = opts.length;
				var data = [];
				for(var x=0; x<ol; x++){
					var text = opts[x].textContent || opts[x].innerText || opts[x].innerHTML;
					var keyValArr = [String(text), String(opts[x].value)];
					data.push(keyValArr);
					if(opts[x].selected){
						options.setAllValues(keyValArr[0], keyValArr[1]);
					}
				}
				this.setData(data);
			}
		}
	},
	{
		_getData: function(/*String*/ url){
			dojo.io.bind({
				url: url,
				load: dojo.lang.hitch(this, function(type, data, evt){
					if(!dojo.lang.isArray(data)){
						var arrData = [];
						for(var key in data){
							arrData.push([data[key], key]);
						}
						data = arrData;
					}
					this.setData(data);
				}),
				mimetype: "text/json"
			});
		},

		startSearch: function(/*String*/ searchStr, /*Function*/ callback){
			// summary:
			//		Start the search for patterns that match searchStr.
			// searchStr:
			//		The characters the user has typed into the <input>.
			// callback:
			//		This function will be called with the result, as an
			//		array of label/value pairs (the value is used for the Select widget).  Example:
			//		[ ["Alabama","AL"], ["Alaska","AK"], ["American Samoa","AS"] ]

			// FIXME: need to add timeout handling here!!
			this._performSearch(searchStr, callback);
		},

		_performSearch: function(/*String*/ searchStr, /*Function*/ callback){
			//
			//	NOTE: this search is LINEAR, which means that it exhibits perhaps
			//	the worst possible speed characteristics of any search type. It's
			//	written this way to outline the responsibilities and interfaces for
			//	a search.
			//
			var st = this.searchType;
			// FIXME: this is just an example search, which means that we implement
			// only a linear search without any of the attendant (useful!) optimizations
			var ret = [];
			if(!this.caseSensitive){
				searchStr = searchStr.toLowerCase();
			}
			for(var x=0; x<this._data.length; x++){
				if((this.searchLimit > 0)&&(ret.length >= this.searchLimit)){
					break;
				}
				// FIXME: we should avoid copies if possible!
				var dataLabel = new String((!this.caseSensitive) ? this._data[x][0].toLowerCase() : this._data[x][0]);
				if(dataLabel.length < searchStr.length){
					// this won't ever be a good search, will it? What if we start
					// to support regex search?
					continue;
				}

				if(st == "STARTSTRING"){
					if(searchStr == dataLabel.substr(0, searchStr.length)){
						ret.push(this._data[x]);
					}
				}else if(st == "SUBSTRING"){
					// this one is a gimmie
					if(dataLabel.indexOf(searchStr) >= 0){
						ret.push(this._data[x]);
					}
				}else if(st == "STARTWORD"){
					// do a substring search and then attempt to determine if the
					// preceeding char was the beginning of the string or a
					// whitespace char.
					var idx = dataLabel.indexOf(searchStr);
					if(idx == 0){
						// implicit match
						ret.push(this._data[x]);
					}
					if(idx <= 0){
						// if we didn't match or implicily matched, march onward
						continue;
					}
					// otherwise, we have to go figure out if the match was at the
					// start of a word...
					// this code is taken almost directy from nWidgets
					var matches = false;
					while(idx!=-1){
						// make sure the match either starts whole string, or
						// follows a space, or follows some punctuation
						if(" ,/(".indexOf(dataLabel.charAt(idx-1)) != -1){
							// FIXME: what about tab chars?
							matches = true; break;
						}
						idx = dataLabel.indexOf(searchStr, idx+1);
					}
					if(!matches){
						continue;
					}else{
						ret.push(this._data[x]);
					}
				}
			}
			callback(ret);
		},

		setData: function(/*Array*/ pdata){
			// summary: set (or reset) the data and initialize lookup structures
			this._data = pdata;
		}
	}
);

dojo.widget.defineWidget(
	"dojo.widget.ComboBox",
	dojo.widget.HtmlWidget,
	{
		// summary:
		//		Auto-completing text box, and base class for Select widget.
		//
		//		The drop down box's values are populated from an class called
		//		a data provider, which returns a list of values based on the characters
		//		that the user has typed into the input box.
		//
		//		Some of the options to the ComboBox are actually arguments to the data
		//		provider.

		// forceValidOption: Boolean
		//		If true, only allow selection of strings in drop down list.
		//		If false, user can select a value from the drop down, or just type in
		//		any random value.
		forceValidOption: false,

		// searchType: String
		//		Argument to data provider.
		//		Specifies rule for matching typed in string w/list of available auto-completions.
		//			startString - look for auto-completions that start w/the specified string.
		//			subString - look for auto-completions containing the typed in string.
		//			startWord - look for auto-completions where any word starts w/the typed in string.
		searchType: "stringstart",

		// dataProvider: Object
		//		(Read only) reference to data provider object created for this combobox
		//		according to "dataProviderClass" argument.
		dataProvider: null,

		// autoComplete: Boolean
		//		If you type in a partial string, and then tab out of the <input> box,
		//		automatically copy the first entry displayed in the drop down list to
		//		the <input> field
		autoComplete: true,

		// searchDelay: Integer
		//		Delay in milliseconds between when user types something and we start
		//		searching based on that value
		searchDelay: 100,

		// dataUrl: String
		//		URL argument passed to data provider object (class name specified in "dataProviderClass")
		//		An example of the URL format for the default data provider is
		//		"remoteComboBoxData.js?search=%{searchString}"
		dataUrl: "",

		// fadeTime: Integer
		//		Milliseconds duration of fadeout for drop down box
		fadeTime: 200,

		// maxListLength: Integer
		//		 Limits list to X visible rows, scroll on rest
		maxListLength: 8,

		// mode: String
		//		Mode must be specified unless dataProviderClass is specified.
		//		"local" to inline search string, "remote" for JSON-returning live search
		//		or "html" for dumber live search.
		mode: "local",

		// selectedResult: Array
		//		(Read only) array specifying the value/label that the user selected
		selectedResult: null,

		// dataProviderClass: String
		//		Name of data provider class (code that maps a search string to a list of values)
		//		The class must match the interface demonstrated by dojo.widget.incrementalComboBoxDataProvider
		dataProviderClass: "",

		// buttonSrc: URI
		//		URI for the down arrow icon to the right of the input box.
		buttonSrc: dojo.uri.dojoUri("src/widget/templates/images/combo_box_arrow.png"),

		// dropdownToggle: String
		//		Animation effect for showing/displaying drop down box
		dropdownToggle: "fade",

		templatePath: dojo.uri.dojoUri("src/widget/templates/ComboBox.html"),
		templateCssPath: dojo.uri.dojoUri("src/widget/templates/ComboBox.css"),

		setValue: function(/*String*/ value){
			// summary: Sets the value of the combobox
			this.comboBoxValue.value = value;
			if (this.textInputNode.value != value){ // prevent mucking up of selection
				this.textInputNode.value = value;
				// only change state and value if a new value is set
				dojo.widget.html.stabile.setState(this.widgetId, this.getState(), true);
				this.onValueChanged(value);
			}
		},

		onValueChanged: function(/*String*/ value){
			// summary: callback when value changes, for user to attach to
		},

		getValue: function(){
			// summary: Rerturns combo box value
			return this.comboBoxValue.value;
		},

		getState: function(){
			// summary:
			//	Used for saving state of ComboBox when navigates to a new
			//	page, in case they then hit the browser's "Back" button.
			return {value: this.getValue()};
		},

		setState: function(/*Object*/ state){
			// summary:
			//	Used for restoring state of ComboBox when has navigated to a new
			//	page but then hits browser's "Back" button.
			this.setValue(state.value);
		},

		enable:function(){
			this.disabled=false;
			this.textInputNode.removeAttribute("disabled");
		},

		disable: function(){
			this.disabled = true;
			this.textInputNode.setAttribute("disabled",true);
		},

		_getCaretPos: function(/*DomNode*/ element){
			// khtml 3.5.2 has selection* methods as does webkit nightlies from 2005-06-22
			if(dojo.lang.isNumber(element.selectionStart)){
				// FIXME: this is totally borked on Moz < 1.3. Any recourse?
				return element.selectionStart;
			}else if(dojo.render.html.ie){
				// in the case of a mouse click in a popup being handled,
				// then the document.selection is not the textarea, but the popup
				// var r = document.selection.createRange();
				// hack to get IE 6 to play nice. What a POS browser.
				var tr = document.selection.createRange().duplicate();
				var ntr = element.createTextRange();
				tr.move("character",0);
				ntr.move("character",0);
				try {
					// If control doesnt have focus, you get an exception.
					// Seems to happen on reverse-tab, but can also happen on tab (seems to be a race condition - only happens sometimes).
					// There appears to be no workaround for this - googled for quite a while.
					ntr.setEndPoint("EndToEnd", tr);
					return String(ntr.text).replace(/\r/g,"").length;
				} catch (e){
					return 0; // If focus has shifted, 0 is fine for caret pos.
				}

			}
		},

		_setCaretPos: function(/*DomNode*/ element, /*Number*/ location){
			location = parseInt(location);
			this._setSelectedRange(element, location, location);
		},

		_setSelectedRange: function(/*DomNode*/ element, /*Number*/ start, /*Number*/ end){
			if(!end){ end = element.value.length; }  // NOTE: Strange - should be able to put caret at start of text?
			// Mozilla
			// parts borrowed from http://www.faqts.com/knowledge_base/view.phtml/aid/13562/fid/130
			if(element.setSelectionRange){
				element.focus();
				element.setSelectionRange(start, end);
			}else if(element.createTextRange){ // IE
				var range = element.createTextRange();
				with(range){
					collapse(true);
					moveEnd('character', end);
					moveStart('character', start);
					select();
				}
			}else{ //otherwise try the event-creation hack (our own invention)
				// do we need these?
				element.value = element.value;
				element.blur();
				element.focus();
				// figure out how far back to go
				var dist = parseInt(element.value.length)-end;
				var tchar = String.fromCharCode(37);
				var tcc = tchar.charCodeAt(0);
				for(var x = 0; x < dist; x++){
					var te = document.createEvent("KeyEvents");
					te.initKeyEvent("keypress", true, true, null, false, false, false, false, tcc, tcc);
					element.dispatchEvent(te);
				}
			}
		},

		_handleKeyEvents: function(/*Event*/ evt){
			// summary: handles keyboard events
			if(evt.ctrlKey || evt.altKey || !evt.key){ return; }

			// reset these
			this._prev_key_backspace = false;
			this._prev_key_esc = false;

			var k = dojo.event.browser.keys;
			var doSearch = true;

			switch(evt.key){
	 			case k.KEY_DOWN_ARROW:
					if(!this.popupWidget.isShowingNow){
						this._startSearchFromInput();
					}
					this._highlightNextOption();
					dojo.event.browser.stopEvent(evt);
					return;
				case k.KEY_UP_ARROW:
					this._highlightPrevOption();
					dojo.event.browser.stopEvent(evt);
					return;
				case k.KEY_TAB:
					// using linux alike tab for autocomplete
					if(!this.autoComplete && this.popupWidget.isShowingNow && this._highlighted_option){
						dojo.event.browser.stopEvent(evt);
						this._selectOption({ 'target': this._highlighted_option, 'noHide': false});

						// put caret last
						this._setSelectedRange(this.textInputNode, this.textInputNode.value.length, null);
					}else{
						this._selectOption();
						return;
					}
					break;
				case k.KEY_ENTER:
					// prevent submitting form if we press enter with list open
					if(this.popupWidget.isShowingNow){
						dojo.event.browser.stopEvent(evt);
					}
					if(this.autoComplete){
						this._selectOption();
						return;
					}
					// fallthrough
				case " ":
					if(this.popupWidget.isShowingNow && this._highlighted_option){
						dojo.event.browser.stopEvent(evt);
						this._selectOption();
						this._hideResultList();
						return;
					}
					break;
				case k.KEY_ESCAPE:
					this._hideResultList();
					this._prev_key_esc = true;
					return;
				case k.KEY_BACKSPACE:
					this._prev_key_backspace = true;
					if(!this.textInputNode.value.length){
						this.setAllValues("", "");
						this._hideResultList();
						doSearch = false;
					}
					break;
				case k.KEY_RIGHT_ARROW: // fall through
				case k.KEY_LEFT_ARROW: // fall through
					doSearch = false;
					break;
				default:// non char keys (F1-F12 etc..)  shouldn't open list
					if(evt.charCode==0){
						doSearch = false;
					}
			}

			if(this.searchTimer){
				clearTimeout(this.searchTimer);
			}
			if(doSearch){
				// if we have gotten this far we dont want to keep our highlight
				this._blurOptionNode();

				// need to wait a tad before start search so that the event bubbles through DOM and we have value visible
				this.searchTimer = setTimeout(dojo.lang.hitch(this, this._startSearchFromInput), this.searchDelay);
			}
		},

		compositionEnd: function(/*Event*/ evt){
			// summary: When inputting characters using an input method, such as Asian
			// languages, it will generate this event instead of onKeyDown event
			evt.key = evt.keyCode;
			this._handleKeyEvents(evt);
		},

		onKeyUp: function(/*Event*/ evt){
			// summary: callback on key up event
			this.setValue(this.textInputNode.value);
		},

		setSelectedValue: function(/*String*/ value){
			// summary:
			//		This sets a hidden value associated w/the displayed value.
			//		The hidden value (and this function) shouldn't be used; if
			//		you need a hidden value then use Select widget instead of ComboBox.
			// TODO: remove?
			// FIXME, not sure what to do here!
			this.comboBoxSelectionValue.value = value;
		},

		setAllValues: function(/*String*/ value1, /*String*/ value2){
			// summary:
			//		This sets the displayed value and hidden value.
			//		The hidden value (and this function) shouldn't be used; if
			//		you need a hidden value then use Select widget instead of ComboBox.
			this.setSelectedValue(value2);
			this.setValue(value1);
		},

		_focusOptionNode: function(/*DomNode*/ node){
			// summary: does the actual highlight
			if(this._highlighted_option != node){
				this._blurOptionNode();
				this._highlighted_option = node;
				dojo.html.addClass(this._highlighted_option, "dojoComboBoxItemHighlight");
			}
		},

		_blurOptionNode: function(){
			// sumary: removes highlight on highlighted
			if(this._highlighted_option){
				dojo.html.removeClass(this._highlighted_option, "dojoComboBoxItemHighlight");
				this._highlighted_option = null;
			}
		},

		_highlightNextOption: function(){
			if((!this._highlighted_option) || !this._highlighted_option.parentNode){
				this._focusOptionNode(this.optionsListNode.firstChild);
			}else if(this._highlighted_option.nextSibling){
				this._focusOptionNode(this._highlighted_option.nextSibling);
			}
			dojo.html.scrollIntoView(this._highlighted_option);
		},

		_highlightPrevOption: function(){
			if(this._highlighted_option && this._highlighted_option.previousSibling){
				this._focusOptionNode(this._highlighted_option.previousSibling);
			}else{
				this._highlighted_option = null;
				this._hideResultList();
				return;
			}
			dojo.html.scrollIntoView(this._highlighted_option);
		},

		_itemMouseOver: function(/*Event*/ evt){
			if (evt.target === this.optionsListNode){ return; }
			this._focusOptionNode(evt.target);
			dojo.html.addClass(this._highlighted_option, "dojoComboBoxItemHighlight");
		},

		_itemMouseOut: function(/*Event*/ evt){
			if (evt.target === this.optionsListNode){ return; }
			this._blurOptionNode();
		},

		onResize: function(){
			// summary: this function is called when the input area has changed size
			var inputSize = dojo.html.getContentBox(this.textInputNode);
			if( inputSize.height <= 0 ){
				// need more time to calculate size
				dojo.lang.setTimeout(this, "onResize", 100);
				return;
			}
			var buttonSize = { width: inputSize.height, height: inputSize.height};
			dojo.html.setContentBox(this.downArrowNode, buttonSize);
		},

		fillInTemplate: function(/*Object*/ args, /*Object*/ frag){
			// there's some browser specific CSS in ComboBox.css
			dojo.html.applyBrowserClass(this.domNode);

			var source = this.getFragNodeRef(frag);
			if (! this.name && source.name){ this.name = source.name; }
			this.comboBoxValue.name = this.name;
			this.comboBoxSelectionValue.name = this.name+"_selected";

			/* different nodes get different parts of the style */
			dojo.html.copyStyle(this.domNode, source);
			dojo.html.copyStyle(this.textInputNode, source);
			dojo.html.copyStyle(this.downArrowNode, source);
			with (this.downArrowNode.style){ // calculate these later
				width = "0px";
				height = "0px";
			}

			// Use specified data provider class; if no class is specified
			// then use comboboxDataProvider or incrmentalComboBoxDataProvider
			// depending on setting of mode
			var dpClass;
			if(this.dataProviderClass){
				if(typeof this.dataProviderClass == "string"){
					dpClass = dojo.evalObjPath(this.dataProviderClass)
				}else{
					dpClass = this.dataProviderClass;
				}
			}else{
				if(this.mode == "remote"){
					dpClass = dojo.widget.incrementalComboBoxDataProvider;
				}else{
					dpClass = dojo.widget.basicComboBoxDataProvider;
				}
			}
			this.dataProvider = new dpClass(this, this.getFragNodeRef(frag));

			this.popupWidget = new dojo.widget.createWidget("PopupContainer",
				{toggle: this.dropdownToggle, toggleDuration: this.toggleDuration});
			dojo.event.connect(this, 'destroy', this.popupWidget, 'destroy');
			this.optionsListNode = this.popupWidget.domNode;
			this.domNode.appendChild(this.optionsListNode);
			dojo.html.addClass(this.optionsListNode, 'dojoComboBoxOptions');
			dojo.event.connect(this.optionsListNode, 'onclick', this, '_selectOption');
			dojo.event.connect(this.optionsListNode, 'onmouseover', this, '_onMouseOver');
			dojo.event.connect(this.optionsListNode, 'onmouseout', this, '_onMouseOut');

			// TODO: why does onmouseover and onmouseout connect to two separate handlers???
			dojo.event.connect(this.optionsListNode, "onmouseover", this, "_itemMouseOver");
			dojo.event.connect(this.optionsListNode, "onmouseout", this, "_itemMouseOut");
		},

		_openResultList: function(/*Array*/ results){
			if (this.disabled){
				return;
			}
			this._clearResultList();
			if(!results.length){
				this._hideResultList();
			}

			if(	(this.autoComplete)&&
				(results.length)&&
				(!this._prev_key_backspace)&&
				(this.textInputNode.value.length > 0)){
				var cpos = this._getCaretPos(this.textInputNode);
				// only try to extend if we added the last character at the end of the input
				if((cpos+1) > this.textInputNode.value.length){
					// only add to input node as we would overwrite Capitalisation of chars
					this.textInputNode.value += results[0][0].substr(cpos);
					// build a new range that has the distance from the earlier
					// caret position to the end of the first string selected
					this._setSelectedRange(this.textInputNode, cpos, this.textInputNode.value.length);
				}
			}

			var even = true;
			while(results.length){
				var tr = results.shift();
				if(tr){
					var td = document.createElement("div");
					td.appendChild(document.createTextNode(tr[0]));
					td.setAttribute("resultName", tr[0]);
					td.setAttribute("resultValue", tr[1]);
					td.className = "dojoComboBoxItem "+((even) ? "dojoComboBoxItemEven" : "dojoComboBoxItemOdd");
					even = (!even);
					this.optionsListNode.appendChild(td);
				}
			}

			// show our list (only if we have content, else nothing)
			this._showResultList();
		},

		_onFocusInput: function(){
			this._hasFocus = true;
		},

		_onBlurInput: function(){
			this._hasFocus = false;
			this._handleBlurTimer(true, 500);
		},

		_handleBlurTimer: function(/*Boolean*/clear, /*Number*/ millisec){
			// summary: collect all blur timers issues here
			if(this.blurTimer && (clear || millisec)){
				clearTimeout(this.blurTimer);
			}
			if(millisec){ // we ignore that zero is false and never sets as that never happens in this widget
				this.blurTimer = dojo.lang.setTimeout(this, "_checkBlurred", millisec);
			}
		},

		_onMouseOver: function(/*Event*/ evt){
			// summary: needed in IE and Safari as inputTextNode loses focus when scrolling optionslist
			if(!this._mouseover_list){
				this._handleBlurTimer(true, 0);
				this._mouseover_list = true;
			}
		},

		_onMouseOut:function(/*Event*/ evt){
			// summary: needed in IE and Safari as inputTextNode loses focus when scrolling optionslist
			var relTarget = evt.relatedTarget;
			try { // fixes #1807
				if(!relTarget || relTarget.parentNode != this.optionsListNode){
					this._mouseover_list = false;
					this._handleBlurTimer(true, 100);
					this._tryFocus();
				}
			}catch(e){}
		},

		_isInputEqualToResult: function(/*String*/ result){
			var input = this.textInputNode.value;
			if(!this.dataProvider.caseSensitive){
				input = input.toLowerCase();
				result = result.toLowerCase();
			}
			return (input == result);
		},

		_isValidOption: function(){
			var tgt = dojo.html.firstElement(this.optionsListNode);
			var isValidOption = false;
			while(!isValidOption && tgt){
				if(this._isInputEqualToResult(tgt.getAttribute("resultName"))){
					isValidOption = true;
				}else{
					tgt = dojo.html.nextElement(tgt);
				}
			}
			return isValidOption;
		},

		_checkBlurred: function(){
			if(!this._hasFocus && !this._mouseover_list){
				this._hideResultList();
				// clear the list if the user empties field and moves away.
				if(!this.textInputNode.value.length){
					this.setAllValues("", "");
					return;
				}

				var isValidOption = this._isValidOption();
				// enforce selection from option list
				if(this.forceValidOption && !isValidOption){
					this.setAllValues("", "");
					return;
				}
				if(!isValidOption){// clear
					this.setSelectedValue("");
				}
			}
		},

		_selectOption: function(/*Event*/ evt){
			var tgt = null;
			if(!evt){
				evt = { target: this._highlighted_option };
			}

			if(!dojo.html.isDescendantOf(evt.target, this.optionsListNode)){
				// handle autocompletion where the the user has hit ENTER or TAB

				// if the input is empty do nothing
				if(!this.textInputNode.value.length){
					return;
				}
				tgt = dojo.html.firstElement(this.optionsListNode);

				// user has input value not in option list
				if(!tgt || !this._isInputEqualToResult(tgt.getAttribute("resultName"))){
					return;
				}
				// otherwise the user has accepted the autocompleted value
			}else{
				tgt = evt.target;
			}

			while((tgt.nodeType!=1)||(!tgt.getAttribute("resultName"))){
				tgt = tgt.parentNode;
				if(tgt === dojo.body()){
					return false;
				}
			}

			this.selectedResult = [tgt.getAttribute("resultName"), tgt.getAttribute("resultValue")];
			this.setAllValues(tgt.getAttribute("resultName"), tgt.getAttribute("resultValue"));
			if(!evt.noHide){
				this._hideResultList();
				this._setSelectedRange(this.textInputNode, 0, null);
			}
			this._tryFocus();
		},

		_clearResultList: function(){
			if(this.optionsListNode.innerHTML){
				this.optionsListNode.innerHTML = "";  // browser natively knows how to collect this memory
			}
		},

		_hideResultList: function(){
			this.popupWidget.close();
		},

		_showResultList: function(){
			// Our dear friend IE doesnt take max-height so we need to calculate that on our own every time
			var childs = this.optionsListNode.childNodes;
			if(childs.length){
				var visibleCount = Math.min(childs.length,this.maxListLength);

				with(this.optionsListNode.style)
				{
					display = "";
					if(visibleCount == childs.length){
						//no scrollbar is required, so unset height to let browser calcuate it,
						//as in css, overflow is already set to auto
						height = "";
					}else{
						//show it first to get the correct dojo.style.getOuterHeight(childs[0])
						//FIXME: shall we cache the height of the item?
						height = visibleCount * dojo.html.getMarginBox(childs[0]).height +"px";
					}
					width = (dojo.html.getMarginBox(this.domNode).width-2)+"px";
				}
				this.popupWidget.open(this.domNode, this, this.downArrowNode);
			}else{
				this._hideResultList();
			}
		},

		handleArrowClick: function(){
			// summary: callback when arrow is clicked
			this._handleBlurTimer(true, 0);
			this._tryFocus();
			if(this.popupWidget.isShowingNow){
				this._hideResultList();
			}else{
				// forces full population of results, if they click
				// on the arrow it means they want to see more options
				this._startSearch("");
			}
		},

		_tryFocus: function(){
			try {
				this.textInputNode.focus();
			} catch (e){
				// element isn't focusable if disabled, or not visible etc - not easy to test for.
	 		};
		},

		_startSearchFromInput: function(){
			this._startSearch(this.textInputNode.value);
		},

		_startSearch: function(/*String*/ key){
			this.dataProvider.startSearch(key, dojo.lang.hitch(this, "_openResultList"));
		},

		postCreate: function(){
			this.onResize();

			// TODO: add these attach events to template
			dojo.event.connect(this.textInputNode, "onblur", this, "_onBlurInput");
			dojo.event.connect(this.textInputNode, "onfocus", this, "_onFocusInput");

			if (this.disabled){
				this.disable();
			}
			var s = dojo.widget.html.stabile.getState(this.widgetId);
			if (s){
				this.setState(s);
			}
		}
	}
);
