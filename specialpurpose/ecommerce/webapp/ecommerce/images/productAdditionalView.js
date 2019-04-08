/*
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
*/

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
        var mainImage = document.getElementById('detailImage');
        mainImage.src = document.getElementById('originalImage').value;
        return false;
    },
    showImage: function() {
        var mainImage = document.getElementById('detailImage');
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
jQuery(document).ready(imgView.init);
