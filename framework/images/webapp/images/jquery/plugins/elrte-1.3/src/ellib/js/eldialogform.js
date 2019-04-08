/**
 * @class elDialogForm
 * Wraper for jquery.ui.dialog and jquery.ui.tabs
 *  Create form in dialog. You can decorate it as you wish - with tabs or/and tables
 *
 * Usage:
 *   var d = new elDialogForm(opts)
 *   d.append(['Field name: ', $('<input type="text" name="f1" />')])
 *		.separator()
 *		.append(['Another field name: ', $('<input type="text" name="f2" />')])
 *      .open()
 * will create dialog with pair text field separated by horizontal rule
 * Calling append() with 2 additional arguments ( d.append([..], null, true)) 
 *  - will create table in dialog and put text inputs and labels in table cells
 *
 * Dialog with tabs:
 *   var d = new elDialogForm(opts)
 *   d.tab('first', 'First tab label)
 * 	  .tab('second', 'Second tab label)
 *    .append(['Field name: ', $('<input type="text" name="f1" />')], 'first', true)  - add label and input to first tab in table (table will create automagicaly)
 *    .append(['Field name 2: ', $('<input type="text" name="f2" />')], 'second', true)  - same in secon tab
 *
 * Options:
 *   class     - css class for dialog
 *   submit    - form submit event callback. Accept 2 args - event and this object
 *   ajaxForm  - arguments for ajaxForm, if needed (dont forget include jquery.form.js)
 *   tabs      - arguments for ui.tabs
 *   dialog    - arguments for ui.dialog
 *   name      - hidden text field in wich selected value will saved
 *
 * Notice!
 * When close dialog, it will destroing insead of dialog('close'). Reason - strange bug with tabs in dialog on secondary opening. 
 *
 * @author:    Dmitry Levashov (dio) dio@std42.ru
 *
 **/

function elDialogForm(o) {
	var self = this;
	
	var defaults = {
		'class'   : 'el-dialogform',
		submit    : function(e, d) { d.close(); },
		form      : { action : window.location.href,	method : 'post'	},
		ajaxForm  : null,
		validate  : null,
		spinner   : 'Loading',
		tabs      : { active: 0, selected : 0 },
		tabPrefix : 'el-df-tab-',
		dialog    : {
			title     : 'dialog',
			autoOpen  : false,
			modal     : true,
			resizable : false,
			closeOnEscape : true,
			buttons  : {
				Cancel : function() { self.close(); },
				Ok     : function() { self.form.trigger('submit'); }
			}
		}
	};

	this.opts = jQuery.extend(true, {}, defaults, o);
	
	this.opts.dialog.close = function() { 
		self.close(); 
	}

	// this.opts.dialog.autoOpen = true;
	if (this.opts.rtl) {
		this.opts['class'] += ' el-dialogform-rtl';
	}
	
	if (o && o.dialog && o.dialog.buttons && typeof(o.dialog.buttons) == 'object') {
		this.opts.dialog.buttons = o.dialog.buttons;
	}

	this.ul     = null;
	this.tabs   = {};
	this._table = null;

	this.dialog = jQuery('<div />').addClass(this.opts['class']).dialog(this.opts.dialog);

	this.message = jQuery('<div class="el-dialogform-message rounded-5" />').hide().appendTo(this.dialog);
	this.error   = jQuery('<div class="el-dialogform-error rounded-5" />').hide().appendTo(this.dialog);
	this.spinner = jQuery('<div class="spinner" />').hide().appendTo(this.dialog);
	this.content = jQuery('<div class="el-dialogform-content" />').appendTo(this.dialog)
	this.form   = jQuery('<form />').attr(this.opts.form).appendTo(this.content);

	if (this.opts.submit) {
		this.form.bind('submit', function(e) { self.opts.submit(e, self) })
	}
	if (this.opts.ajaxForm && jQuery.fn.ajaxForm) {
		this.form.ajaxForm(this.opts.ajaxForm);
	}
	if (this.opts.validate) {
		this.form.validate(this.opts.validate);
	}
	
	this.option = function(name, value) {
		return this.dialog.dialog('option', name, value)
	}
	
	this.showError = function(msg, hideContent) {
		this.hideMessage();
		this.hideSpinner();
		this.error.html(msg).show();
		hideContent && this.content.hide();
		return this;
	}
	
	this.hideError= function() {
		this.error.text('').hide();
		this.content.show();
		return this;		
	}
	
	this.showSpinner = function(txt) {
		this.error.hide();
		this.message.hide();
		this.content.hide();
		this.spinner.text(txt||this.opts.spinner).show();
		this.option('buttons', {});
		return this;		
	}
	
	this.hideSpinner = function() {
		this.content.show();
		this.spinner.hide();
		return this;		
	}
	
	this.showMessage = function(txt, hideContent) {
		this.hideError();
		this.hideSpinner();
		this.message.html(txt||'').show();
		hideContent && this.content.hide();
		return this;
	}
	
	this.hideMessage = function() {
		this.message.hide();
		this.content.show();
		return this;		
	}
	
	/**
	 * Create new tab
	 * @param string id    - tab id
	 * @param string title - tab name
	 * @return elDialogForm	
	**/
	this.tab = function(id, title) {
		id = this.opts.tabPrefix+id;
		
		if (!this.ul) {
			this.ul = jQuery('<ul />').prependTo(this.form);
		}
		jQuery('<li />').append(jQuery('<a />').attr('href', '#'+id).html(title)).appendTo(this.ul);
		this.tabs[id] = {tab : jQuery('<div />').attr('id', id).addClass('tab').appendTo(this.form), table : null};
		return this;
	}
	
	/**
	 * Create new table
	 * @param string id  tab id, if set - table will create in tab, otherwise - in dialog
	 * @return elDialogForm	
	**/
	this.table = function(id) {
		id = id && id.indexOf(this.opts.tabPrefix) == -1 ? this.opts.tabPrefix+id : id;
		if (id && this.tabs && this.tabs[id]) {
			this.tabs[id].table = jQuery('<table />').appendTo(this.tabs[id].tab);
		} else {
			this._table = jQuery('<table />').appendTo(this.form); 
		}
		return this;
	}
	
	/**
	 * Append html, dom nodes or jQuery objects to dialog or tab
	 * @param array|object|string  data object(s) to append to dialog
	 * @param string               tid  tab id, if adding to tab
	 * @param bool                 t    if true - data will added in table (creating automagicaly)
	 * @return elDialogForm	
	**/
	this.append = function(data, tid, t) {
		tid = tid ? 'el-df-tab-'+tid : '';

		if (!data) {
			return this;
		}
		
		if (tid && this.tabs[tid]) {
			if (t) {
				!this.tabs[tid].table && this.table(tid);
				var tr = jQuery('<tr />').appendTo(this.tabs[tid].table);
				if (!jQuery.isArray(data)) {
					tr.append(jQuery('<td />').append(data));
				} else {
					for (var i=0; i < data.length; i++) {
						tr.append(jQuery('<td />').append(data[i]));
					};
				}
			} else {
				if (!jQuery.isArray(data)) {
					this.tabs[tid].tab.append(data)
				} else {
					for (var i=0; i < data.length; i++) {
						this.tabs[tid].tab.append(data[i]);
					};
				}
			}
			
		} else {
			if (!t) {
				if (!jQuery.isArray(data)) {
					this.form.append(data);
				} else {
					for (var i=0; i < data.length; i++) {
						this.form.append(data[i]);
					};
				}
			} else {
				if (!this._table) {
					this.table();
				}
				var tr = jQuery('<tr />').appendTo(this._table);
				if (!jQuery.isArray(data)) {
					tr.append(jQuery('<td />').append(data));
				} else {
					for (var i=0; i < data.length; i++) {
						tr.append(jQuery('<td />').append(data[i]));
					};
				}
			}
		}
		return this;
	}
	
	/**
	 * Append separator (div class="separator") to dialog or tab
	 * @param  string tid  tab id, if adding to tab
	 * @return elDialogForm	
	**/
	this.separator = function(tid) {
		tid = 'el-df-tab-'+tid;
		if (this.tabs && this.tabs[tid]) {
			this.tabs[tid].tab.append(jQuery('<div />').addClass('separator'));
			this.tabs[tid].table && this.table(tid);
		} else {
			this.form.append(jQuery('<div />').addClass('separator'));
		}
		return this;
	}
	
	/**
	 * Open dialog window
	 * @return elDialogForm	
	**/
	this.open = function() {
		var self = this;
		
		this.ul && this.form.tabs(this.opts.tabs);

		setTimeout(function() {
			self.dialog.find(':text')
				.keydown(function(e) {
					if (e.keyCode == 13) {
						e.preventDefault()
						self.form.submit();
					}
				})
				.filter(':first')[0].focus()
		}, 200);

		this.dialog.dialog('open');

		return this;
	}
	
	/**
	 * Close dialog window and destroy content
	 * @return void	
	**/
	this.close = function() {
		if (typeof(this.opts.close) == 'function') {
			this.opts.close();
		}
		this.dialog.dialog('destroy')//.remove();
	}
	
}

