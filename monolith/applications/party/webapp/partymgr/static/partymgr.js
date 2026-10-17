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

function lookupParty(url) {
    partyIdValue = document.lookupparty.partyId.value;
    userLoginIdValue = document.lookupparty.userLoginId.value;
    if (partyIdValue.length > 0 || userLoginIdValue.length > 0) {
        document.lookupparty.action = url;
    }
    return true;
}

function refreshInfo() {
    document.lookupparty.lookupFlag.value = "N";
    document.lookupparty.hideFields.value = "N";
    document.lookupparty.submit();
}

function collapseFindPartyOptions(currentOption) {
    jQuery('.fieldgroup').each(function() {
        var titleBar = jQuery(this).children('.fieldgroup-title-bar'), body = jQuery(this).children('.fieldgroup-body');
        if (titleBar.children().length > 0 && body.is(':visible') != false && body.attr('id') != currentOption) {
            toggleCollapsiblePanel(titleBar.find('a'), body.attr('id'), 'Expand', 'Collapse');
        }
    });
}

