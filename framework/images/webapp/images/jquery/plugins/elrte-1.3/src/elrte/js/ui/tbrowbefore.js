/**
 * @class меню - Новый ряд в таблице
 *
 * @param  elRTE  rte   объект-редактор
 * @param  String name  название кнопки 
 **/
elRTE.prototype.ui.prototype.buttons.tbrowbefore = function(rte, name) {
	this.constructor.prototype.constructor.call(this, rte, name);
	
	this.command = function() {
		var n  = this.rte.selection.getNode();
		var c  = this.rte.dom.selfOrParent(n, /^(TD|TH)$/);
		var r  = this.rte.dom.selfOrParent(c, /^TR$/);
		var mx = this.rte.dom.tableMatrix(this.rte.dom.selfOrParent(c, /^TABLE$/));

		if (c && r && mx) {
			this.rte.history.add();
			var before = this.name == 'tbrowbefore';
			var ro     = $(r).prevAll('tr').length;
			var cnt    = 0;
			var mdf    = [];
			
			function _find(x, y) {
				while (y>0) {
					y--;
					if (mx[y] && mx[y][x] && mx[y][x].nodeName) {
						return mx[y][x];
					}
				}
			}
			
			for (var i=0; i<mx[ro].length; i++) {
				if (mx[ro][i] && mx[ro][i].nodeName) {
					var cell    = $(mx[ro][i]);
					var colspan = parseInt(cell.attr('colspan')||1);
					if (parseInt(cell.attr('rowspan')||1) > 1) {
						if (before) {
							cnt += colspan;
						} else {
							mdf.push(cell);
						}
					} else {
						cnt += colspan;
					}
				} else if (mx[ro][i] == '-') {
					cell = _find(i, ro);
					cell && mdf.push($(cell));
				}
			}
			var row = $(this.rte.dom.create('tr'));
			for (var i=0; i<cnt; i++) {
				row.append('<td>&nbsp;</td>');
			}
			if (before) {
				row.insertBefore(r);
			} else {
				row.insertAfter(r);
			}
			$.each(mdf, function() {
				$(this).attr('rowspan', parseInt($(this).attr('rowspan')||1)+1);
			});
			this.rte.ui.update();
		}
	}
	
	this.update = function() {
		if (this.rte.dom.selfOrParent(this.rte.selection.getNode(), /^TR$/)) {
			this.domElem.removeClass('disabled');
		} else {
			this.domElem.addClass('disabled');
		}
	}
}

elRTE.prototype.ui.prototype.buttons.tbrowafter = elRTE.prototype.ui.prototype.buttons.tbrowbefore;
