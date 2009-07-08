imgView = {
    init: function() {
        if (document.getElementById) {
            allAnchors = document.getElementsByTagName('a');
            if (allAnchors.length) {
                for (var i = 0; i < allAnchors.length; i++) {
                    if (allAnchors[i].getAttributeNode('swapDetail') && allAnchors[i].getAttributeNode('swapDetail').value != '') {
                        allAnchors[i].onmouseover = imgView.showImage;
                        allAnchors[i].onmouseout = imgView.showDetailImage;
                    }
                }
            }
        }
    },
    showDetailImage: function() { 
        var mainImage = $('detailImage');
        mainImage.src = $F('originalImage');
        return false;
    },
    showImage: function() {
        var mainImage = $('detailImage');
        mainImage.src = this.getAttributeNode('swapDetail').value;
        return false;
    },
    addEvent: function(element, eventType, doFunction, useCapture) {
        if (element.addEventListener) {
            element.addEventListener(eventType, doFunction, useCapture);
            return true;
        }else if (element.attachEvent) {
              var r = element.attachEvent('on' + eventType, doFunction);
              return r;
        }else {
             element['on' + eventType] = doFunction;
        }
    }
}
Event.observe(window, 'load', imgView.init, false);
