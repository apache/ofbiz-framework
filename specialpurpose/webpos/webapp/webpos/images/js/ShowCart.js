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
function selectCartItem() {
    jQuery('tr.pos-cart-even').each(function(idx) {
        var id = jQuery(this).attr('id');
        if (id != '') {
            id = '#' + id;
            jQuery(id).bind('click', function(event) {
                showCartItemSelected(id);
            });
        }
    });
    jQuery('tr.pos-cart-odd').each(function(idx) {
        var id = jQuery(this).attr('id');
        if (id != '') {
            id = '#' + id;
            jQuery(id).bind('click', function(event) {
                showCartItemSelected(id);
            });
        }
    });
}

function showCartItemSelected(itemId) {
    var lineIndex = itemId.substring(9, 10);
    updateCartItemSelected(lineIndex, 'Y');
    jQuery('#selectedItem').val(lineIndex);
}

function keyUp() {
    var selectedItem = parseInt(jQuery('#selectedItem').value);
    var cartSize = parseInt(jQuery('#cartSize').value);
    cartSize = cartSize - 1;
    if (selectedItem > 0) {
        jQuery('#selectedItem').val(selectedItem - 1);
        updateCartItemSelected(jQuery('#selectedItem').value, 'Y');
    }
    return false;
}

function keyDown() {
    var selectedItem = parseInt(jQuery('#selectedItem').value);
    var cartSize = parseInt(jQuery('#cartSize').value);
    cartSize = cartSize - 1;
    if (selectedItem < cartSize) {
        jQuery('#selectedItem').val(selectedItem + 1);
        updateCartItemSelected(jQuery('#selectedItem').value, 'Y');
    }
    return false;
}