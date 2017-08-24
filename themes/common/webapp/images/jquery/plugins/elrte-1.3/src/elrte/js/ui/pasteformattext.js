/**
 * @class button - insert formatted text (open dialog window)
 *
 * @param  elRTE  rte   объект-редактор
 * @param  String name  название кнопки 
 *
 * @author:    Dmitry Levashov (dio) dio@std42.ru
 * @copyright: Studio 42, http://www.std42.ru
 **/
(function($) {
elRTE.prototype.ui.prototype.buttons.pasteformattext = function(rte, name) {
	this.constructor.prototype.constructor.call(this, rte, name);
	this.iframe = $(document.createElement('iframe')).addClass('el-rte-paste-input');
	this.doc    = null;
	var self    = this;
	
	this.command = function() {
		this.rte.selection.saveIERange();
		var self = this,
			opts = {
			submit : function(e, d) {
				e.stopPropagation();
				e.preventDefault();
				self.paste();
				d.close();
			},
			dialog : {
				width : 500,
				title : this.rte.i18n('Paste formatted text')
			}
		},
		d = new elDialogForm(opts);
		d.append(this.iframe).open();
		this.doc = this.iframe.get(0).contentWindow.document;
		html = this.rte.options.doctype
			+'<html xmlns="http://www.w3.org/1999/xhtml"><head><meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />';
		html += '</head><body> <br /> </body></html>';	
		
		this.doc.open();
		this.doc.write(html);
		this.doc.close();

		if (!this.rte.browser.msie) {
			try { this.doc.designMode = "on"; } 
			catch(e) { }
		} else {
			this.doc.body.contentEditable = true;
		}
		setTimeout(function() { self.iframe[0].contentWindow.focus(); }, 50);
	}
	
	this.paste = function() {
		$(this.doc.body).find('[class]').removeAttr('class');
		var html = $.trim($(this.doc.body).html());
		if (html) {
			this.rte.history.add();
			this.rte.selection.restoreIERange();
			this.rte.selection.insertHtml(this.rte.filter.wysiwyg2wysiwyg(this.rte.filter.proccess('paste', html)));
			this.rte.ui.update(true);
		}
	}

	this.update = function() {
		this.domElem.removeClass('disabled');
	}
}
})(jQuery);

