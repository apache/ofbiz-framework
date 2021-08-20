/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

function showHideUserPref() {
    var userPref = document.getElementById("user-details");

    if(userPref.style.display == "none") {
        userPref.style.display = "flex";
    }
    else {
        userPref.style.display = "none";
    }
}

function selectOrgaOK(orgaName){
    var selectOrga = document.getElementById("orga"+orgaName);
    var currentModal = document.getElementById("selectOrga");
    selectOrga.click();
    currentModal.style.visibility = "hidden";
}

document.addEventListener("DOMContentLoaded", function() {
    let tooltips = document.querySelectorAll('.tooltip'),
        checkboxs = document.querySelectorAll('input[type=checkbox]'),
        radios = document.querySelectorAll('input[type=radio]'),
        lefts = document.querySelectorAll('.lefthalf');

    tooltips.forEach(tooltip => {
        tooltip.classList.add('hidden');

        let ParentTooltip = tooltip.parentNode;
        ParentTooltip.classList.add('has-tooltip');

        let infoTooltips = ParentTooltip.querySelectorAll('i');

        infoTooltips.forEach(infoTooltip => {
            infoTooltip.classList.remove("hidden");
            infoTooltip.addEventListener("mouseenter", function( event ) {
                tooltip.classList.remove('hidden');
            });
            infoTooltip.addEventListener("mouseout", function( event ) {
                tooltip.classList.add('hidden');
            })
        })
    });

    checkboxs.forEach(checkbox => {
        checkbox.parentNode.classList.add('has-checkbox');
    });
    radios.forEach(radio => {
        radio.parentNode.classList.add('has-radio');
    }); 
    lefts.forEach(left => {
        left.parentNode.classList.add('screenlet-flex');
    });
});