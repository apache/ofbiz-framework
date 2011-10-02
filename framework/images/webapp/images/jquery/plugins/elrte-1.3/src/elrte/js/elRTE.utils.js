/*
 * Misc utils for elRTE
 *
 * @param Object rte - editor
 * @todo Подумать, что из этого реально нужно и навести порядок. Возможно часть перенести в ellib
 *
 * @author:    Dmitry Levashov (dio) dio@std42.ru
 * Copyright: Studio 42, http://www.std42.ru
 */
(function($) {
elRTE.prototype.utils = function(rte) {
	this.rte     = rte;
	this.url     = null;
	// domo arigato, Steave, http://blog.stevenlevithan.com/archives/parseuri
	this.reg     = /^(?:(?![^:@]+:[^:@\/]*@)([^:\/?#.]+):)?(?:\/\/)?((?:(([^:@]*)(?::([^:@]*))?)?@)?([^:\/?#]*)(?::(\d*))?)(((\/(?:[^?#](?![^?#\/]*\.[^?#\/.]+(?:[?#]|$)))*\/?)?([^?#\/]*))(?:\?([^#]*))?(?:#(.*))?)/;
	this.baseURL = '';
	this.path    = '';
	/**
	 * entities map
	 **/
	this.entities = {'&' : '&amp;', '"' : '&quot;', '<' : '&lt;', '>' : '&gt;'};
	/**
	 * entities regexp
	 **/
	this.entitiesRegExp = /[<>&\"]/g;
	/**
	 * media info
	 **/
	this.media = [{ 
			type     : 'application/x-shockwave-flash', 
			classid  : ['clsid:d27cdb6e-ae6d-11cf-96b8-444553540000'], 
			codebase : 'http://download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab#version=6,0,40,0' 
		}, { 
			type     : 'application/x-director', 
			classid  : ['clsid:166b1bca-3f9c-11cf-8075-444553540000'], 
			codebase : 'http://download.macromedia.com/pub/shockwave/cabs/director/sw.cab#version=8,5,1,0' 
		}, { 
			type     : 'application/x-mplayer2', 
			classid  : ['clsid:6bf52a52-394a-11d3-b153-00c04f79faa6', 'clsid:22d6f312-b0f6-11d0-94ab-0080c74c7e95', 'clsid:05589fa1-c356-11ce-bf01-00aa0055595a'], 
			codebase : 'http://activex.microsoft.com/activex/controls/mplayer/en/nsmp2inf.cab#Version=5,1,52,701' 
		}, { 
			type     : 'video/quicktime', 
			classid  : ['clsid:02bf25d5-8c17-4b23-bc80-d3488abddc6b'], 
			codebase : 'http://www.apple.com/qtactivex/qtplugin.cab#version=6,0,2,0' 
		}, { 
			type     : 'audio/x-pn-realaudio-plugin', 
			classid  : ['clsid:cfcdaa03-8be4-11cf-b84b-0020afbbccfa'], 
			codebase : 'http://download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab#version=6,0,40,0'
		}];
	
	// rgb color regexp
	this.rgbRegExp = /\s*rgb\s*?\(\s*?([0-9]+)\s*?,\s*?([0-9]+)\s*?,\s*?([0-9]+)\s*?\)\s*/i;
	// regexp to detect color in border/background properties
	this.colorsRegExp = /aqua|black|blue|fuchsia|gray|green|lime|maroon|navy|olive|orange|purple|red|silver|teal|white|yellow|rgb\s*\([^\)]+\)/i;
	// web safe colors
	this.colors = {
		aqua    : '#00ffff',
		black   : '#000000',
		blue    : '#0000ff',
		fuchsia : '#ff00ff',
		gray    : '#808080',
		green   : '#008000',
		lime    : '#00ff00',
		maroon  : '#800000',
		navy    : '#000080',
		olive   : '#808000',
		orange  : '#ffa500',
		purple  : '#800080',
		red     : '#ff0000',
		silver  : '#c0c0c0',
		teal    : '#008080',
		white   : '#fffffff',
		yellow  : '#ffff00'
	}
	
	var self     = this;
	
	this.rgb2hex = function(str) {
		return this.color2Hex(''+str)
	}
	
	this.toPixels = function(num) {
		var m = num.match(/([0-9]+\.?[0-9]*)\s*(px|pt|em|%)/);
		if (m) {
			num  = m[1];
			unit = m[2];
		} 
		if (num[0] == '.') {
			num = '0'+num;
		}
		num = parseFloat(num);

		if (isNaN(num)) {
			return '';
		}
		var base = parseInt($(document.body).css('font-size')) || 16;
		switch (unit) {
			case 'em': return parseInt(num*base);
			case 'pt': return parseInt(num*base/12);
			case '%' : return parseInt(num*base/100);
		}
		return num;
	}
	
	// TODO: add parse rel path ../../etc
	this.absoluteURL = function(url) {
		!this.url && this._url();
		url = $.trim(url);
		if (!url) {
			return '';
		}
		// ссылки на якоря не переводим в абс
		if (url[0] == '#') {
			return url;
		}
		var u = this.parseURL(url);

		if (!u.host && !u.path && !u.anchor) {
			//this.rte.log('Invalid URL: '+url)
			return '';
		}
		if (!this.rte.options.absoluteURLs) { 
			return url;
		}
		if (u.protocol) {
			//this.rte.log('url already absolute: '+url);
			return url;
		}
		if (u.host && (u.host.indexOf('.')!=-1 || u.host == 'localhost')) {
			//this.rte.log('no protocol');
			return this.url.protocol+'://'+url;
		}
		if (url[0] == '/') {
			url = this.baseURL+url;
		} else {
			if (url.indexOf('./') == 0) {
				url = url.substring(2);
			}
			url = this.baseURL+this.path+url;
		}
		return url;
	}
	
	this.parseURL = function(url) {
		var u   = url.match(this.reg);
		var ret = {};
		$.each(["source","protocol","authority","userInfo","user","password","host","port","relative","path","directory","file","query","anchor"], function(i) {
			ret[this] = u[i];
		});
		if (!ret.host.match(/[a-z0-9]/i)) {
			ret.host = '';
		}
		return ret;
	}
	
	this.trimEventCallback = function(c) {
		c = c ? c.toString() : '';
		return $.trim(c.replace(/\r*\n/mg, '').replace(/^function\s*on[a-z]+\s*\(\s*event\s*\)\s*\{(.+)\}$/igm, '$1'));
	}
	
	this._url = function() {
		this.url     = this.parseURL(window.location.href);
		this.baseURL = this.url.protocol+'://'+(this.url.userInfo ?  parts.userInfo+'@' : '')+this.url.host+(this.url.port ? ':'+this.url.port : '');
		this.path    = !this.url.file ? this.url.path : this.url.path.substring(0, this.url.path.length - this.url.file.length);
	}


	/**
	 * Create object (map) from array
	 *
	 * @param   Array
	 * @return  Object
	 **/
	this.makeObject = function(o) {
		var m = {};
		$.each(o, function(i, e) {
			m[e] = e;
		});
		return m;
	}

	/**
	 * Encode entities in string
	 *
	 * @param   String
	 * @return  String
	 **/
	this.encode = function(s) {
		var e = this.entities;
		return (''+s).replace(this.entitiesRegExp, function(c) {
			return e[c];
		});
	}
	
	/**
	 * Decode entities in string
	 *
	 * @param   String
	 * @return  String
	 **/
	this.decode = function(s) {
		return $('<div/>').html(s||'').text();
	}
	
	/**
	 * Parse style string into object
	 *
	 * @param   String
	 * @return  Object
	 **/
	this.parseStyle = function(s) {
		var st = {}, a = this.rte.options.allowBrowsersSpecStyles, t, n, v, p;
		
		if (typeof(s) == 'string' && s.length) {
			
			$.each(s.replace(/&quot;/gi, "'").split(';'), function(i, str) {
				if ((p = str.indexOf(':')) !== -1) {
					n = $.trim(str.substr(0, p));
					v = $.trim(str.substr(p+1))
					if (n == 'color' || n == 'background-color') {
						v = v.toLowerCase();
					}
					if (n && v && (a || n.substring(0, 1) != '-')) {
						st[n] = v;
					}
				}
			});
		}
		return st;
	}
	
	
	/**
	 * Compact some style properties and convert colors in hex
	 *
	 * @param   Object
	 * @return  Object
	 **/
	this.compactStyle = function(s) {
		var self = this;

		if (s.border == 'medium none') {
			delete s.border;
		}
		
		$.each(s, function(n, v) {
			if (/color$/i.test(n)) {
				s[n] = self.color2Hex(v);
			} else if (/^(border|background)$/i.test(n)) {
				s[n] = v.replace(self.colorsRegExp, function(m) {
					return self.color2Hex(m);
				});
			}
		});
		
		if (s['border-width']) {
			s.border = s['border-width']+' '+(s['border-style']||'solid')+' '+(s['border-color']||'#000');
			delete s['border-width'];
			delete s['border-style'];
			delete s['border-color'];
		}
		
		if (s['background-image']) {
			s.background = (s['background-color']+' ')||''+s['background-image']+' '+s['background-position']||'0 0'+' '+s['background-repeat']||'repeat';
			delete s['background-image'];
			delete['background-image'];
			delete['background-position'];
			delete['background-repeat'];
		}
		
		if (s['margin-top'] && s['margin-right'] && s['margin-bottom'] && s['margin-left']) {
			s.margin = s['margin-top']+' '+s['margin-right']+' '+s['margin-bottom']+' '+s['margin-left'];
			delete s['margin-top'];
			delete s['margin-right'];
			delete s['margin-bottom'];
			delete s['margin-left'];
		}
		
		if (s['padding-top'] && s['padding-right'] && s['padding-bottom'] && s['padding-left']) {
			s.padding = s['padding-top']+' '+s['padding-right']+' '+s['padding-bottom']+' '+s['padding-left'];
			delete s['padding-top'];
			delete s['padding-right'];
			delete s['padding-bottom'];
			delete s['padding-left'];
		}
		
		if (s['list-style-type'] || s['list-style-position'] || s['list-style-image']) {
			s['list-style'] = $.trim(s['list-style-type']||' '+s['list-style-position']||''+s['list-style-image']||'');
			delete s['list-style-type'];
			delete s['list-style-position'];
			delete s['list-style-image'];
		}
		
		return s;
	}
	
	/**
	 * Serialize style object into string
	 *
	 * @param   Object  style map
	 * @param   Boolean flag - compact style?
	 * @return  String
	 **/
	this.serializeStyle = function(o, c) {
		var s = [];
		// c=true
		$.each(c ? this.compactStyle(o) : o, function(n, v) {
			v && s.push(n+':'+v);
		});
		return s.join(';');
	}

	/**
	 * Parse class string into object
	 *
	 * @param   String
	 * @return  Object
	 **/
	this.parseClass = function(c) {
		c = $.trim(c); 
		// this.rte.log(c)
		return c.length ? this.makeObject(c.split(/\s+/)) : {};
		return c.length ? c.split(/\s+/) : [];
	}

	/**
	 * Serialize class object into string
	 *
	 * @param   Object
	 * @return  String
	 **/
	this.serializeClass = function(c) {
		// return c.join(' ')
		var s = [];
		// this.rte.log(c)
		var rte = this.rte
		$.each(c, function(n) {
			s.push(n);
			// rte.log(typeof(n))
		});
		return s.join(' ');
	}
	
	/**
	 * Return required media type info
	 *
	 * @param   String  mimetype
	 * @param   String  classid
	 * @return  Object
	 **/
	this.mediaInfo = function(t, c) {
		var l = this.media.length;
		
		while (l--) {
			if (t === this.media[l].type || (c && $.inArray(c, this.media[l].classid) != -1)) {
				return this.media[l];
			}
		}
	}

	/**
	 * Return color hex value
	 *
	 * @param   String   color name or rgb
	 * @return  String
	 **/
	this.color2Hex = function(c) {
		var m;
		
		c = c||'';
		
		if (c.indexOf('#') === 0) {
			return c;
		}
		
		
		function hex(s) {
			s = parseInt(s).toString(16);
			return s.length > 1 ? s : '0' + s; 
		};
		
		if (this.colors[c]) {
			return this.colors[c];
		}
		if ((m = c.match(this.rgbRegExp))) {
			return '#'+hex(m[1])+hex(m[2])+hex(m[3]);
		}
		return '';
	}
	

	
	
}

})(jQuery);