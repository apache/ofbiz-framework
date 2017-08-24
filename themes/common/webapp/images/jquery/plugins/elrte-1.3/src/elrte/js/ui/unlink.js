/**
 * @class button - remove link
 *
 * @param  elRTE  rte   объект-редактор
 * @param  String name  название кнопки
 *
 * @author:    Dmitry Levashov (dio) dio@std42.ru
 * @copyright: Studio 42, http://www.std42.ru 
 **/
(function($) {

	elRTE.prototype.ui.prototype.buttons.unlink = function(rte, name) {
		this.constructor.prototype.constructor.call(this, rte, name);

		this.command = function() {

			var n = this.rte.selection.getNode(), 
				l = this.rte.dom.selfOrParentLink(n);

			function isLink(n) { return n.nodeName == 'A' && n.href; }

			if (!l) {

				var sel = $.browser.msie ? this.rte.selection.selected() : this.rte.selection.selected({wrap : false});
				if (sel.length) {
					for (var i=0; i < sel.length; i++) {
						if (isLink(sel[i])) {
							l = sel[i];
							break;
						}
					};
					if (!l) {
						l = this.rte.dom.parent(sel[0], isLink) || this.rte.dom.parent(sel[sel.length-1], isLink);
					}
				}
			}

			if (l) {
				this.rte.history.add();
				this.rte.selection.select(l);
				this.rte.doc.execCommand('unlink', false, null);
				this.rte.ui.update(true);
			}
		
		}
	
		this.update = function() {
			var n = this.rte.selection.getNode();
			if (this.rte.dom.selfOrParentLink(n)) {
				this.domElem.removeClass('disabled').addClass('active');
			} else if (this.rte.dom.selectionHas(function(n) { return n.nodeName == 'A' && n.href; })) {
				this.domElem.removeClass('disabled').addClass('active');
			} else {
				this.domElem.addClass('disabled').removeClass('active');
			}
		}
	}

})(jQuery);

