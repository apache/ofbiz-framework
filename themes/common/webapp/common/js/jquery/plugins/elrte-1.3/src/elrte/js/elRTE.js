/*
 * elRTE - WSWING editor for web
 *
 * Usage:
 * var opts = {
 *	.... // see elRTE.options.js
 * }
 * var editor = new elRTE($('#my-id').get(0), opts)
 * or
 * $('#my-id').elrte(opts)
 *
 * $('#my-id) may be textarea or any DOM Element with text
 *
 * @author:    Dmitry Levashov (dio) dio@std42.ru
 * Copyright: Studio 42, http://www.std42.ru
 */
(function($) {

elRTE = function(target, opts) {
	if (!target || !target.nodeName) {
		return alert('elRTE: argument "target" is not DOM Element');
	}
	var self       = this, html;
	this.version   = '1.3';
	this.build     = '2011-06-23';
	this.options   = $.extend(true, {}, this.options, opts);
	this.browser   = $.browser;
	this.target    = $(target);
	
	this.lang      = (''+this.options.lang);
	this._i18n     = new eli18n({textdomain : 'rte', messages : { rte : this.i18Messages[this.lang] || {}} });
	this.rtl       = !!(/^(ar|fa|he)$/.test(this.lang) && this.i18Messages[this.lang]);
	
	if (this.rtl) {
		this.options.cssClass += ' el-rte-rtl';
	}
	this.toolbar   = $('<div class="toolbar"/>');
	this.iframe    = document.createElement('iframe');
	this.iframe.setAttribute('frameborder', 0); // fixes IE border

	// this.source    = $('<textarea />').hide();
	this.workzone  = $('<div class="workzone"/>').append(this.iframe).append(this.source);
	this.statusbar = $('<div class="statusbar"/>');
	this.tabsbar   = $('<div class="tabsbar"/>');
	this.editor    = $('<div class="'+this.options.cssClass+'" />').append(this.toolbar).append(this.workzone).append(this.statusbar).append(this.tabsbar);
	
	this.doc       = null;
	this.$doc      = null;
	this.window    = null;
	
	this.utils     = new this.utils(this);
	this.dom       = new this.dom(this);
	this.filter    = new this.filter(this)
	
	/**
	 * Sync iframes/textareas height with workzone height 
	 *
	 * @return void
	 */
	this.updateHeight = function() {
		self.workzone.add(self.iframe).add(self.source).height(self.workzone.height());
	}
	
	/**
	 * Turn editor resizable on/off if allowed
	 *
	 * @param  Boolean 
	 * @return void
	 **/
	this.resizable = function(r) {
		var self = this;
		if (this.options.resizable && $.fn.resizable) {
			if (r) {
				this.editor.resizable({handles : 'se', alsoResize : this.workzone, minWidth :300, minHeight : 200 }).bind('resize', self.updateHeight);
			} else {
				this.editor.resizable('destroy').unbind('resize', self.updateHeight);
			}
		}
	}
	
	/* attach editor to document */
	this.editor.insertAfter(target);
	/* init editor textarea */
	var content = '';
	if (target.nodeName == 'TEXTAREA') {
		this.source = this.target;
		this.source.insertAfter(this.iframe).hide();
		content = this.target.val();
	} else {
		this.source = $('<textarea />').insertAfter(this.iframe).hide();
		content = this.target.hide().html();
	}
	this.source.attr('name', this.target.attr('name')||this.target.attr('id'));
	content = $.trim(content);
	if (!content) {
		content = ' ';
	}

	/* add tabs */
	if (this.options.allowSource) {
		this.tabsbar.append('<div class="tab editor rounded-bottom-7 active">'+self.i18n('Editor')+'</div><div class="tab source rounded-bottom-7">'+self.i18n('Source')+'</div><div class="clearfix" style="clear:both"/>')
			.children('.tab').click(function(e) {
				if (!$(this).hasClass('active')) {
					self.tabsbar.children('.tab').toggleClass('active');
					self.workzone.children().toggle();

					if ($(this).hasClass('editor')) {
						self.updateEditor();
						self.window.focus();
						self.ui.update(true);
					} else {
						self.updateSource();
						self.source.focus();
						if ($.browser.msie) {
							// @todo
						} else {
							self.source[0].setSelectionRange(0, 0);
						}
						self.ui.disable();
						self.statusbar.empty();
						
					}
				}
				
			});
	}
	
	this.window = this.iframe.contentWindow;
	this.doc    = this.iframe.contentWindow.document;
	this.$doc   = $(this.doc);
	
	/* put content into iframe */
	html = '<html xmlns="http://www.w3.org/1999/xhtml"><head><meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />';
	$.each(self.options.cssfiles, function() {
		html += '<link rel="stylesheet" type="text/css" href="'+this+'" />';
	});
	this.doc.open();
	var s = this.filter.wysiwyg(content),
		cl = this.rtl ? ' class="el-rte-rtl"' : '';
	this.doc.write(self.options.doctype+html+'</head><body'+cl+'>'+(s)+'</body></html>');
	this.doc.close();
	
	/* make iframe editable */
	if ($.browser.msie) {
		this.doc.body.contentEditable = true;
	} else {
		try { this.doc.designMode = "on"; } 
		catch(e) { }
		this.doc.execCommand('styleWithCSS', false, this.options.styleWithCSS);
	}
	
	if (this.options.height>0) {
		this.workzone.height(this.options.height);
	}
	if (this.options.width>0) {
		this.editor.width(this.options.width);
	}
	
	this.updateHeight();
	this.resizable(true);
	this.window.focus();
	
	this.history = new this.history(this);
	
	/* init selection object */
	this.selection = new this.selection(this);
	/* init buttons */
	this.ui = new this.ui(this);
	
	
	/* bind updateSource to parent form submit */
	this.target.parents('form').bind('submit.elfinder', function(e) {
		self.source.parents('form').find('[name="el-select"]').remove()
		self.beforeSave();
	});
	
	// on tab press - insert \t and prevent move focus
	this.source.bind('keydown', function(e) {
		if (e.keyCode == 9) {
			e.preventDefault();
				
			if ($.browser.msie) {
				var r = document.selection.createRange();
				r.text = "\t"+r.text;
				this.focus();
			} else {
				var before = this.value.substr(0, this.selectionStart),
					after = this.value.substr(this.selectionEnd);
				this.value = before+"\t"+after;
				this.setSelectionRange(before.length+1, before.length+1);
			}
		}
	});
	
	$(this.doc.body).bind('dragend', function(e) {
		setTimeout(function() {
			try {
				self.window.focus();
				var bm = self.selection.getBookmark();
				self.selection.moveToBookmark(bm);
				self.ui.update();
			} catch(e) { }
			
			
		}, 200);
		
	});
	
	this.typing = false;
	this.lastKey = null;
	/* update buttons on click and keyup */
	this.$doc.bind('mouseup', function() {
		self.typing = false;
		self.lastKey = null;
		self.ui.update();
	})
	.bind('keyup', function(e) {
		if ((e.keyCode >= 8 && e.keyCode <= 13) || (e.keyCode>=32 && e.keyCode<= 40) || e.keyCode == 46 || (e.keyCode >=96 && e.keyCode <= 111)) {
			self.ui.update();
		}
	})
	.bind('keydown', function(e) {
		if ((e.metaKey || e.ctrlKey) && e.keyCode == 65) {
			self.ui.update();
		} else if (e.keyCode == 13) {
			var n = self.selection.getNode();
			// self.log(n)
			if (self.dom.selfOrParent(n, /^PRE$/)) {
				self.selection.insertNode(self.doc.createTextNode("\r\n"));
				return false;
			} else if ($.browser.safari && e.shiftKey) {
				self.selection.insertNode(self.doc.createElement('br'))
				return false;
			}
		}

		if ((e.keyCode>=48 && e.keyCode <=57) || e.keyCode==61 || e.keyCode == 109 || (e.keyCode>=65 && e.keyCode<=90) || e.keyCode==188 ||e.keyCode==190 || e.keyCode==191 || (e.keyCode>=219 && e.keyCode<=222)) {
			if (!self.typing) {
				self.history.add(true);
			}
			self.typing = true;
			self.lastKey = null;
		} else if (e.keyCode == 8 || e.keyCode == 46 || e.keyCode == 32 || e.keyCode == 13) {
			if (e.keyCode != self.lastKey) {
				self.history.add(true);
			}
			self.lastKey = e.keyCode;
			self.typing = false;
		}
		
		if (e.keyCode == 32 && $.browser.opera) {
			self.selection.insertNode(self.doc.createTextNode(" "));
			return false
		}
	})
	.bind('paste', function(e) {
		if (!self.options.allowPaste) {
			// paste denied 
			e.stopPropagation();
			e.preventDefault();
		} else {
			var n = $(self.dom.create('div'))[0],
				r = self.doc.createTextNode('_');
			self.history.add(true);
			self.typing = true;
			self.lastKey = null;
			n.appendChild(r);
			self.selection.deleteContents().insertNode(n);
			self.selection.select(r);
			setTimeout(function() {
				if (n.parentNode) {
					// clean sandbox content
					$(n).html(self.filter.proccess('paste', $(n).html()));
					r = n.lastChild;
					self.dom.unwrap(n);
					if (r) {
						self.selection.select(r);
						self.selection.collapse(false);
					}
				} else {
					// smth wrong - clean all doc
					n.parentNode && n.parentNode.removeChild(n);
					self.val(self.filter.proccess('paste', self.filter.wysiwyg2wysiwyg($(self.doc.body).html())));
					self.selection.select(self.doc.body.firstChild);
					self.selection.collapse(true);
				}
				$(self.doc.body).mouseup(); // to activate history buutons
			}, 15);
		}
	});
	
	if ($.browser.msie) {
		this.$doc.bind('keyup', function(e) {
			if (e.keyCode == 86 && (e.metaKey||e.ctrlKey)) {
				self.history.add(true);
				self.typing = true;
				self.lastKey = null;
				self.selection.saveIERange();
				self.val(self.filter.proccess('paste', self.filter.wysiwyg2wysiwyg($(self.doc.body).html())));
				self.selection.restoreIERange();
				$(self.doc.body).mouseup();
				this.ui.update();
			}
		});
	}
	
	if ($.browser.safari) {
		this.$doc.bind('click', function(e) {
			$(self.doc.body).find('.elrte-webkit-hl').removeClass('elrte-webkit-hl');
			if (e.target.nodeName == 'IMG') {
				$(e.target).addClass('elrte-webkit-hl');
			}
		}).bind('keyup', function(e) {
			$(self.doc.body).find('.elrte-webkit-hl').removeClass('elrte-webkit-hl');
		})
	}
	
	this.window.focus();
	
	this.destroy = function() {
		this.updateSource();
		this.target.is('textarea')
			? this.target.val($.trim(this.source.val()))
			: this.target.html($.trim(this.source.val()));
		this.editor.remove();
		this.target.show().parents('form').unbind('submit.elfinder');
	}
	
}

/**
 * Return message translated to selected language
 *
 * @param  string  msg  message text in english
 * @return string
 **/
elRTE.prototype.i18n = function(msg) {
	return this._i18n.translate(msg);
}



/**
 * Display editor
 *
 * @return void
 **/
elRTE.prototype.open = function() {
	this.editor.show();
}

/**
 * Hide editor and display elements on wich editor was created
 *
 * @return void
 **/
elRTE.prototype.close = function() {
	this.editor.hide();
}

elRTE.prototype.updateEditor = function() {
	this.val(this.source.val());
}

elRTE.prototype.updateSource = function() {
	this.source.val(this.filter.source($(this.doc.body).html()));
}

/**
 * Return edited text
 *
 * @return String
 **/
elRTE.prototype.val = function(v) {
	if (typeof(v) == 'string') {
		v = ''+v;
		if (this.source.is(':visible')) {
			this.source.val(this.filter.source2source(v));
		} else {
			if ($.browser.msie) {
				this.doc.body.innerHTML = '<br />'+this.filter.wysiwyg(v);
				this.doc.body.removeChild(this.doc.body.firstChild);
			} else {
				this.doc.body.innerHTML = this.filter.wysiwyg(v);
			}
			
		}
	} else {
		if (this.source.is(':visible')) {
			return this.filter.source2source(this.source.val()).trim();
		} else {
			return this.filter.source($(this.doc.body).html()).trim();
		}
	}
}

elRTE.prototype.beforeSave = function() {
	this.source.val($.trim(this.val())||'');
}

/**
 * Submit form
 *
 * @return void
 **/
elRTE.prototype.save = function() {
	this.beforeSave();
	this.editor.parents('form').submit();
}

elRTE.prototype.log = function(msg) {
	if (window.console && window.console.log) {
		window.console.log(msg);
	}
        
}

elRTE.prototype.i18Messages = {};

$.fn.elrte = function(o, v) { 
	var cmd = typeof(o) == 'string' ? o : '', ret;
	
	this.each(function() {
		if (!this.elrte) {
			this.elrte = new elRTE(this, typeof(o) == 'object' ? o : {});
		}
		switch (cmd) {
			case 'open':
			case 'show':
				this.elrte.open();
				break;
			case 'close':
			case 'hide':
				this.elrte.close();
				break;
			case 'updateSource':
				this.elrte.updateSource();
				break;
			case 'destroy':
				this.elrte.destroy();
		}
	});
	
	if (cmd == 'val') {
		if (!this.length) {
			return '';
		} else if (this.length == 1) {
			return v ? this[0].elrte.val(v) : this[0].elrte.val();
		} else {
			ret = {}
			this.each(function() {
				ret[this.elrte.source.attr('name')] = this.elrte.val();
			});
			return ret;
		}
	}
	return this;
}

})(jQuery);
