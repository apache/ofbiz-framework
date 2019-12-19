/*** fjTimer
 *   Version: 1.3  http://www.foxjunior.eu/ 
 *
 *   Licensed under the MIT license:
 *   http://www.opensource.org/licenses/mit-license.php
**/

/******************************
Simple jQuery based timer. Simple usage: 

jQuery.fjTimer({
	interval: 1000,
	repeat: 5,
	tick: function(counter, timerId) {
		alert("tick:" + counter);
	}
});
properties are:
* interval - ticker interval in milliseconds, default 10
* repeat - number of repeat times or boolean if reapeat forever or in case false not repeat at all
* tick - ticker function itself with parameter of counter and timerId paramerer if you want to clear interval yourself

fjTimerEach method can be used with arrays only. No objects!
jQuery.fjTimerEach({
	interval: 1000,
	tick: function(counter, object) {
		alert("tick:" + counter + " - " + object);
	},
	array: ["me", "myself", "I"]
});
Usage is similar to fjTimer with some exceptions:
* tick comes with parameters index and object in array.
* step property - if you want to walk through array with bigger steps than 1
* array property - this is simple array element.

fjFunctionQueue idea is that we have alot of functions going on. Sometimes browsers(mostly IE) hangs if alot of dom is changed or something is going on. So I wrote simple queue.
Simple usage, just add function to queue:
jQuery.fjFunctionQueue(function() {alert("a")});

Configuration
jQuery.fjFunctionQueue({ interval: 1, onStart: function(){ alert("start")}, onComplete: function(){alert("complete!"}, autoStart: false, tick: function(index, func) {func();}, });

properties are:
* interval - ticker interval in milliseconds. Default 1;
* onStart function executed when queue starts;.
* onComplete function executed when queue is complete (including added functions);
* autoStart property, if queue should started automatically. Default true;
* tick function is executed before each queue item execution. Parameters are index and executed function itself. When exteding it func method must be called manually.
******************************/
jQuery.extend({
	fjFunctionQueue: function(funcToQue) {
		if (funcToQue == null) {
			if (jQuery.fjFunctionQueue.queue != null && jQuery.fjFunctionQueue.queue.queue.length > 0) {
				if (jQuery.fjFunctionQueue.queue.running) {
					jQuery.fjTimer({
						interval: jQuery.fjFunctionQueue.queue.properties.interval,
						tick: function(counter, timer) {
							var func = jQuery.fjFunctionQueue.queue.queue.shift();
							try {
								jQuery.fjFunctionQueue.queue.properties.onTick(jQuery.fjFunctionQueue.queue.index, func);
								jQuery.fjFunctionQueue.queue.index++;
							} catch (e) {
								jQuery.fjFunctionQueue();
								throw e;
							}
							if (jQuery.fjFunctionQueue.queue.queue.length > 0) {
								jQuery.fjFunctionQueue();
							} else {
								jQuery.fjFunctionQueue.queue.running = false;
								jQuery.fjFunctionQueue.queue.index = 0;
								jQuery.fjFunctionQueue.queue.properties.onComplete();
							}
						}
					});
				} else {
					jQuery.fjFunctionQueue.queue.running = true;
					jQuery.fjFunctionQueue();
				}
			}
		} else {
			if (jQuery.fjFunctionQueue.queue == null) {
				jQuery.fjFunctionQueue.queue = {index: 0, running: false, queue:[], properties: {interval: 1, onComplete: function(){}, onStart: function(){}, autoStart: true, onTick: function(counter, func) {func();}}};
			}
			var isEmptyArray = jQuery.fjFunctionQueue.queue.queue.length == 0;
			if (jQuery.isFunction(funcToQue)) {
				jQuery.fjFunctionQueue.queue.queue.push(funcToQue);
			} else if (jQuery.isArray(funcToQue)) {
				for(var i = 0; i < funcToQue.length; i++) {
					jQuery.fjFunctionQueue.queue.queue.push(funcToQue[i]);
				}
			} else {
				jQuery.fjFunctionQueue.queue.properties = jQuery.extend(jQuery.fjFunctionQueue.queue.properties, funcToQue);
			}
			if (isEmptyArray && jQuery.fjFunctionQueue.queue.queue.length > 0 && !jQuery.fjFunctionQueue.queue.running && jQuery.fjFunctionQueue.queue.properties.autoStart) {
				jQuery.fjFunctionQueue.queue.running = true;
				jQuery.fjFunctionQueue.queue.properties.onStart();
				jQuery.fjFunctionQueue.queue.running = false;
				jQuery.fjFunctionQueue();
			}
		}
	},
	fjTimer : function(properties) {
	    properties = jQuery.extend({interval: 10, tick: function(){}, repeat: false, random :false, onComplete: function(){}, step: 1}, properties);
	    var counter = 0;
	    var timer = new function() {
	    	this.timerId = null;
	    	this.stop = function() {
	    		clearInterval(this.timerId);
	    	}
	    }
	    timer.timerId = setInterval(function() {
	    	try {
	    		properties.tick(counter, timer);
	    		counter+=properties.step;
	    	} catch (e) {
	    		alert(e);
	    	}
	    	if (properties.repeat !== true && ((properties.repeat * properties.step) <= counter || properties.repeat === false)) {
	    		timer.stop();
	    		properties.onComplete();
	    	}
	    }, properties.interval);
	},
	fjTimerEach: function(properties) {
		var ___array = properties.array;
		var ___callback = properties.tick;
		properties.repeat = ___array.length;
		if (properties.step != null) {
			properties.repeat = Math.ceil(___array.length / parseInt(properties.step, 10));
		}
		properties.tick = function(counter, timer) {
			___callback(counter, ___array[counter]);
		}
		jQuery.fjTimer(properties);
	}
});
