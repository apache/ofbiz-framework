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
function updateHotKeys() {
    var item = 0;
    var shortcutList = "";
    jQuery.each(WebPosHotkeys.hotkeys, function(i, arr) {
        shortcutList = shortcutList + '<li id="shortcut' + (item + 1) + '" class="notSelectedShortcut">';
        shortcutList = shortcutList + '<a href="javascript:executeAction(' + item + ')">';
        shortcutList = shortcutList + arr[1].toUpperCase() + '<br/>';
        shortcutList = shortcutList + arr[4] + '</a></li>';
        item = item + 1;
    });
    jQuery('#posShortcut').html(shortcutList);
    showSelectedShortcut();
}

function executeAction(item) {
    hideOverlayDiv();
    var key = WebPosHotkeys.hotkeys[item];
    var action = key[3];
    eval(action);
}

function showSelectedShortcut() {
    jQuery('li.notSelectedShortcut').each(function(idx) {
        var id = jQuery(this).attr('id');
        if (id != '') {
            id = '#' + id;
            jQuery(id).mouseover(function(event) {
                jQuery(id).addClass("selectedShortcut");
                jQuery(id).removeClass("notSelectedShortcut");
            });
            jQuery(id).mouseout(function(event) {
                jQuery(id).addClass("notSelectedShortcut");
                jQuery(id).removeClass("selectedShortcut");
            });
        }
    });
}