/***********************************************
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

function lookupOrders(click) {
    orderIdValue = document.lookuporder.orderId.value;
    if (orderIdValue.length > 1) {
        document.lookuporder.action = "<@ofbizUrl>orderview</@ofbizUrl>";
        document.lookuporder.method = "get";
    } else {
        document.lookuporder.action = "<@ofbizUrl>searchorders</@ofbizUrl>";
    }

    if (click) {
        document.lookuporder.submit();
    }
    return true;
}
function toggleOrderId(master) {
    var form = document.massOrderChangeForm;
    var orders = form.elements.length;
    for (var i = 0; i < orders; i++) {
        var element = form.elements[i];
        if ("orderIdList" == element.name) {
            element.checked = master.checked;
        }
    }
    toggleOrderIdList();
}
function setServiceName(selection) {
    document.massOrderChangeForm.action = selection.value;
}
function runAction() {
    var form = document.massOrderChangeForm;
    form.submit();
}

function toggleOrderIdList() {
    var form = document.massOrderChangeForm;
    var orders = form.elements.length;
    var isAllSelected = true;
    var isSingle = true;
    for (var i = 0; i < orders; i++) {
        var element = form.elements[i];
        if ("orderIdList" == element.name) {
            if (element.checked) {
                isSingle = false;
            } else {
                isAllSelected = false;
            }
        }
    }
    if (isAllSelected) {
        jQuery('#checkAllOrders').attr('checked', true);
    } else {
        jQuery('#checkAllOrders').attr('checked', false);
    }
    jQuery('#checkAllOrders').attr("checked", isAllSelected);
    if (!isSingle && jQuery('#serviceName').val() != "") {
        jQuery('#submitButton').removeAttr("disabled");
    } else {
        jQuery('#submitButton').attr('disabled', true);
    }
}

function paginateOrderList(viewSize, viewIndex, hideFields) {
    document.paginationForm.viewSize.value = viewSize;
    document.paginationForm.viewIndex.value = viewIndex;
    document.paginationForm.hideFields.value = hideFields;
    document.paginationForm.submit();
}