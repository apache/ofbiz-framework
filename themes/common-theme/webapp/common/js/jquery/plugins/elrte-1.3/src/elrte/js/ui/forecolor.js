/**
 * @class color pallete for text color and background
 *
 * @param  elRTE  rte   объект-редактор
 * @param  String name  название кнопки 
 *
 * @author:    Dmitry Levashov (dio) dio@std42.ru
 * @copyright: Studio 42, http://www.std42.ru
 **/
(function($) {
elRTE.prototype.ui.prototype.buttons.forecolor = function(rte, name) {
	var self = this;
	this.constructor.prototype.constructor.call(this, rte, name);
	var opts = {
		'class' : '',
		palettePosition : 'outer',
		color   : this.defaultColor,
		update  : function(c) { self.indicator.css('background-color', c); },
		change  : function(c) { self.set(c) }
	}
	
	this.defaultColor = this.name == 'forecolor' ? '#000000' : '#ffffff';
	this.picker       = this.domElem.elColorPicker(opts);
	this.indicator    = $('<div />').addClass('color-indicator').prependTo(this.domElem);
	
	this.command = function() {
	}
	
	this.set = function(c) {
		if (!this.rte.selection.collapsed()) {
			this.rte.history.add();
			var nodes = this.rte.selection.selected({collapse : false, wrap : 'text'}),
				css   = this.name == 'forecolor' ? 'color' : 'background-color';			
			$.each(nodes, function() {
				if (/^(THEAD|TBODY|TFOOT|TR)$/.test(this.nodeName)) {
					$(this).find('td,th').each(function() {
						$(this).css(css, c).find('*').css(css, '');
					})
				} else {
					$(this).css(css, c).find('*').css(css, '');
				}
			});
			this.rte.ui.update(true);
		}
	}
	
	this.update = function() {
		this.domElem.removeClass('disabled');
		var n = this.rte.selection.getNode();
		this.picker.val(this.rte.utils.rgb2hex($(n.nodeType != 1 ? n.parentNode : n).css(this.name == 'forecolor' ? 'color' : 'background-color'))||this.defaultColor)
	}
}

elRTE.prototype.ui.prototype.buttons.hilitecolor = elRTE.prototype.ui.prototype.buttons.forecolor;

})(jQuery);