/**
 * @class кнопка - DIV
 * Если выделение схлопнуто и находится внутри div'a - он удаляется
 * Новые div'ы создаются только из несхлопнутого выделения
 *
 * @param  elRTE  rte   объект-редактор
 * @param  String name  название кнопки 
 * 
 * @author:    Dmitry Levashov (dio) dio@std42.ru
 * copyright: Studio 42, http://www.std42.ru
 **/
(function($) {
elRTE.prototype.ui.prototype.buttons.div = function(rte, name) {
	this.constructor.prototype.constructor.call(this, rte, name);
	
	this.command = function() {
		var n, nodes;
		this.rte.history.add();

		if (this.rte.selection.collapsed()) {
			n = this.rte.dom.selfOrParent(this.rte.selection.getNode(), /^DIV$/);
			if (n) {
				$(n).replaceWith($(n).html());
			}
		} else {
			nodes = this.rte.selection.selected({wrap : 'all', tag : 'div'});
			nodes.length && this.rte.selection.select(nodes[0], nodes[nodes.length-1]);
		}
		this.rte.ui.update(true);
	}
	
	this.update = function() {
		if (this.rte.selection.collapsed()) {
			if (this.rte.dom.selfOrParent(this.rte.selection.getNode(), /^DIV$/)) {
				this.domElem.removeClass('disabled').addClass('active');
			} else {
				this.domElem.addClass('disabled active');
			}
		} else {
			this.domElem.removeClass('disabled active');
		}
	}
}
})(jQuery);
