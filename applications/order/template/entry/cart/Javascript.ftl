<#--
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
-->
<script language="JavaScript" type="text/javascript">
function removeSelected() {
    var cform = document.cartform;
    cform.removeSelected.value = true;
    cform.submit();
}
function addToList() {
    var cform = document.cartform;
    cform.action = "<@ofbizUrl>addBulkToShoppingList</@ofbizUrl>";
    cform.submit();
}
function gwAll(e) {
    var cform = document.cartform;
    var len = cform.elements.length;
    var selectedValue = e.value;
    if ("" == selectedValue) {
        return;
    }

    var cartSize = ${shoppingCartSize};
    var passed = 0;
    for (var i = 0; i < len; i++) {
        var element = cform.elements[i];
        var ename = element.name;
        var sname = ename.substring(0,16);
        if ("option^GIFT_WRAP" == sname) {
            var options = element.options;
            var olen = options.length;
            var matching = -1;
            for (var x = 0; x < olen; x++) {
                var thisValue = element.options[x].value;
                if (thisValue == selectedValue) {
                    element.selectedIndex = x;
                    passed++;
                }
            }
        }
    }
    if (cartSize > passed && selectedValue != "NO^") {
        showErrorAlert("${uiLabelMap.CommonErrorMessage2}","${uiLabelMap.OrderSelectedGiftNotAvailableForAll}");
    }
    cform.submit();
}
function quicklookup_popup(element) {
    target = element;  // note: global var target comes from fieldlookup.js
    var searchTerm = element.value;
    var obj_lookupwindow = window.open('LookupProduct?productId_op=like&amp;productId_ic=Y&amp;productId=' + searchTerm,'FieldLookup', 'width=700,height=550,scrollbars=yes,status=no,resizable=yes,top='+my+',left='+mx+',dependent=yes,alwaysRaised=yes');
    obj_lookupwindow.opener = window;
    obj_lookupwindow.focus();
}
function quicklookup(element) {
    <#if "PURCHASE_ORDER" == shoppingCart.getOrderType()>
    window.location='<@ofbizUrl>LookupBulkAddSupplierProducts</@ofbizUrl>?productId='+element.value;
    <#else>
    window.location='<@ofbizUrl>LookupBulkAddProducts</@ofbizUrl>?productId='+element.value;
    </#if>
}
</script>
