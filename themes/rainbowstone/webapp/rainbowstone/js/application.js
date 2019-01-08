/***********************************************
APACHE OPEN FOR BUSINESS
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
***********************************************/

/*********************
JQuery Columns
*********************/
var j = 1;
 
(function(jQuery) {
	jQuery.fn.columns = function(options) {
	
	var defaults = {			
		colNumber: 2,
		direction: 'vertical'
	};
			
	this.each(function() {
		
		var obj = jQuery(this);
		var settings = jQuery.extend(defaults, options);
		var totalListElements = jQuery(this).children('li').size();
		var baseColItems = Math.ceil(totalListElements / settings.colNumber);
		var listClass = jQuery(this).attr('class');
		
		for (i=1;i<=settings.colNumber;i++) {	
			if(i==1){
				jQuery(this).addClass('listCol1').wrap('<div class="listContainer'+j+'"></div>');
			} 
			else if(jQuery(this).is('ul')) {
				jQuery(this).parents('.listContainer'+j).append('<ul class="listCol'+i+'"></ul>');
			} 
			else {
				jQuery(this).parents('.listContainer'+j).append('<ol class="listCol'+i+'"></ol>');
			}
				jQuery('.listContainer'+j+' > ul,.listContainer'+j+' > ol').addClass(listClass);
		}
		
		var listItem = 0;
		var k = 1;
		var l = 0;	
		
		if(settings.direction == 'vertical') {			
			jQuery(this).children('li').each(function() {
				listItem = listItem+1;
				if (listItem > baseColItems*(settings.colNumber-1) ) {
					jQuery(this).parents('.listContainer'+j).find('.listCol'+settings.colNumber).append(this);
				} 
				else {
					if(listItem<=(baseColItems*k)) {
						jQuery(this).parents('.listContainer'+j).find('.listCol'+k).append(this);
					} 
					else {
						jQuery(this).parents('.listContainer'+j).find('.listCol'+(k+1)).append(this);
						k = k+1;
					}
				}
			});
			
			jQuery('.listContainer'+j).find('ol,ul').each(function(){
				if(jQuery(this).children().size() == 0) {
				jQuery(this).remove();
				}
			});	
			
		}
		
		else {
			jQuery(this).children('li').each(function(){
				l = l+1;
				if(l <= settings.colNumber) {
					jQuery(this).parents('.listContainer'+j).find('.listCol'+l).append(this);
				} 
				else {
					l = 1;
					jQuery(this).parents('.listContainer'+j).find('.listCol'+l).append(this);
				}				
			});
		}
		
		jQuery('.listContainer'+j).find('ol:last,ul:last').addClass('last');
		j = j+1;
		
	});
    };
})(jQuery);

/*********************
JQuery Formalize
*********************/
var FORMALIZE = (function($, window, document, undefined) {
	var PLACEHOLDER_SUPPORTED = 'placeholder' in document.createElement('input');
	var AUTOFOCUS_SUPPORTED = 'autofocus' in document.createElement('input');
	var WEBKIT = 'webkitAppearance' in document.createElement('select').style;
	var IE6 = !!($.browser.msie && parseInt($.browser.version, 10) === 6);
	var IE7 = !!($.browser.msie && parseInt($.browser.version, 10) === 7);
	return {
		go: function() {
			for (var i in FORMALIZE.init) {
				FORMALIZE.init[i]();
			}
		},
		init: {
			detect_webkit: function() {			
				if (!WEBKIT) {
					return;
				}
				$('html').addClass('is_webkit');
			},
			full_input_size: function() {
				if (!IE7 || !$('textarea, input.input_full').length) {
					return;
				}
				$('textarea, input.input_full').wrap('<span class="input_full_wrap"></span>');
			},
			ie6_skin_inputs: function() {
				if (!IE6 || !$('input, select, textarea').length) {
					return;
				}
				var button_regex = /button|submit|reset/;
				var type_regex = /date|datetime|datetime-local|email|month|number|password|range|search|tel|text|time|url|week/;
				$('input').each(function() {
					var el = $(this);
					if (this.getAttribute('type').match(button_regex)) {
						el.addClass('ie6_button');
						if (this.disabled) {
							el.addClass('ie6_button_disabled');
						}
					}
					else if (this.getAttribute('type').match(type_regex)) {
						el.addClass('ie6_input');
						if (this.disabled) {
							el.addClass('ie6_input_disabled');
						}
					}
				});
				$('textarea, select').each(function() {
					if (this.disabled) {
						$(this).addClass('ie6_input_disabled');
					}
				});
			},
			placeholder: function() {
				if (PLACEHOLDER_SUPPORTED || !$(':input[placeholder]').length) {
					return;
				}
				$(':input[placeholder]').each(function() {
					var el = $(this);
					var text = el.attr('placeholder');
					function add_placeholder() {
						if (!el.val() || el.val() === text) {
							el.val(text).addClass('placeholder_text');
						}
					}
					add_placeholder();
					el.focus(function() {
						if (el.val() === text) {
							el.val('').removeClass('placeholder_text');;
						}
					}).blur(function() {
						add_placeholder();
					});
					el.closest('form').submit(function() {
						if (el.val() === text) {
							el.val('');
						}
					}).on('reset', function() {
						setTimeout(add_placeholder, 50);
					});
				});
			},
			autofocus: function() {
				if (AUTOFOCUS_SUPPORTED || !$(':input[autofocus]').length) {
					return;
				}
				$(':input[autofocus]:visible:first').select();
			}
		}
	};
})(jQuery, this, this.document);

jQuery(document).ready(function() {
	FORMALIZE.go();
});

