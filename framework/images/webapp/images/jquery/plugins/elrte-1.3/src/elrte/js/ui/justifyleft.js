/**
 * @class button - justify text
 *
 * @param  elRTE  rte   объект-редактор
 * @param  String name  название кнопки 
 *
 *
 * @author:    Dmitry Levashov (dio) dio@std42.ru
 * @copyright: Studio 42, http://www.std42.ru
 **/
(function($) {
elRTE.prototype.ui.prototype.buttons.justifyleft = function(rte, name) {
	this.constructor.prototype.constructor.call(this, rte, name);
	this.align = this.name == 'justifyfull' ? 'justify' : this.name.replace('justify', '');

	this.command = function() {
		var s = this.rte.selection.selected({collapsed:true, blocks : true, tag : 'div'}),
			l = s.length;
		l && this.rte.history.add();
		while (l--) {
			this.rte.dom.filter(s[l], 'textNodes') && $(s[l]).css('text-align', this.align);
		}
		this.rte.ui.update();
	}
	
	this.update = function() {
		var s = this.rte.selection.getNode(), 
			n = s.nodeName == 'BODY' ? s : this.rte.dom.selfOrParent(s, 'textNodes')||(s.parentNode && s.parentNode.nodeName == 'BODY' ? s.parentNode : null);
		if (n) {
			this.domElem.removeClass('disabled').toggleClass('active', $(n).css('text-align') == this.align);
		} else {
			this.domElem.addClass('disabled');
		}
	}
	
}

elRTE.prototype.ui.prototype.buttons.justifycenter = elRTE.prototype.ui.prototype.buttons.justifyleft;
elRTE.prototype.ui.prototype.buttons.justifyright  = elRTE.prototype.ui.prototype.buttons.justifyleft;
elRTE.prototype.ui.prototype.buttons.justifyfull   = elRTE.prototype.ui.prototype.buttons.justifyleft;

})(jQuery);
