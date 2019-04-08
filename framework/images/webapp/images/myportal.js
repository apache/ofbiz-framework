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

//Drag & Drop Functions for myPortal

//global Var for controlling hover Boxes
"use strict";
var SORTABLE_COLUMN_LIST = null;
var IS_UPDATED = false;
var DESTINATION_COLUMN_ID = null;

//init KeyListener
jQuery(document).ready( function() {
    // initializ the d_n_d jQuery functions
    jQuery(SORTABLE_COLUMN_LIST).sortable({
        connectWith: ".connectedSortable",
        handle: ".portlet-config, .screenlet-title-bar",
        tolerance: "pointer",
        dropOnEmpty: true,
        cursor: "move",
        revert: true,
        placeholder: "ui-state-highlight",
        forcePlaceholderSize: true,
        update: function(event, ui) {
                    IS_UPDATED = true;
                    DESTINATION_COLUMN_ID = jQuery(this).attr("id");
                },
        stop: function(event, ui) {
                    preparePortletBackgroundOrderChange(jQuery(SORTABLE_COLUMN_LIST).sortable("toArray", {connected: true}), jQuery(ui.item).attr("id"), DESTINATION_COLUMN_ID);
                    // reset the flags
                    IS_UPDATED = false;
                    DESTINATION_COLUMN_ID = null;
                }
    });
});

function preparePortletBackgroundOrderChange(serializedDate, dragedItemId, destinationColumnId) {
    if (!IS_UPDATED) {
        return;
    } 
    
    // split the portal column id
    destinationColumnId = destinationColumnId.split("_")[1];
    
    // make clean array and remove all fields with empty values
    var dataArray = []
    jQuery.each(serializedDate, function(index, value) {
        if (value.length) {
            dataArray.push(value);
        }
    });

    // find the new position of the moved element in the array
    var beforeItem = null;
    var afterItem = null;
    var currentItem = null;
    
    jQuery.each(dataArray, function(index, value) {
        if (dragedItemId == value) {
            // create object for the item before the current dropped object if not undefined
            var dataArrayValue = dataArray[index-1];
            if (dataArrayValue != undefined) {
                beforeItem = jQuery("#" + dataArrayValue);
            }
            
            // create object for the item after the current dropped object if not undefined                         
            dataArrayValue = dataArray[index+1];
            if (dataArrayValue != undefined) {                          
                afterItem = jQuery("#" + dataArrayValue);
            }
            
            // create object for the current dropped object
            currentItem = jQuery("#" + value);
            
            // break the jQuery.each loop
            return false;
        } 
    });
    
    // check if the before or after Item is still in the new column to get a reference Object in this column
    var nextObjectToDroppedItem = null;
    
    // check if the item is moved to another column
    if (destinationColumnId != null && destinationColumnId != currentItem.attr("columnseqid") ) {
        //mode can be "BEFORE" (for adding the item before the nextObjectToDroppedItem), "AFTER" (for adding the item after the nextObjectToDroppedItem) or "NEW" (when the item is the first one in the list and should be added to the top)
        var mode = null;
        if ((beforeItem == null || destinationColumnId != beforeItem.attr("columnseqid")) && (afterItem == null || destinationColumnId != afterItem.attr("columnseqid"))) {
            // the moved object entered an empty list
            mode = "NEW";
        } else if (beforeItem != null && destinationColumnId == beforeItem.attr("columnseqid")) {
            // the moved object entered in a new list and should be moved after this beforeItem
            nextObjectToDroppedItem = beforeItem;
            mode = "AFTER";
        } else if (afterItem != null && destinationColumnId == afterItem.attr("columnseqid")) {
            // the moved object entered in a new list and should be moved before this beforeItem
            nextObjectToDroppedItem = afterItem;
            mode = "BEFORE";
        }
        
    } else {
        // if the item is moved in the same column get the before and/or after element
        if (beforeItem.attr("id") != null) {
            mode = "AFTER";
            nextObjectToDroppedItem = beforeItem;
        } else if (afterItem.attr("id") != null){
            mode = "BEFORE";
            nextObjectToDroppedItem = afterItem;
        }
    }
    
    // call the update service
    updatePortletOrder(currentItem, nextObjectToDroppedItem, mode, destinationColumnId);
    
    // change the html attributes after the move
    currentItem.attr({"columnseqid": destinationColumnId});
    

}

function updatePortletOrder(currentItem, nextObjectToDroppedItem, mode, destinationColumn) {
    onStartRequest();
    // create a JSON request object with the needed information
    var requestData = {
            mode: mode,
            destinationColumn: destinationColumn,
            o_portalPageId: currentItem.attr("portalpageid"),
            o_portalPortletId: currentItem.attr("portalportletid"),
            o_portletSeqId: currentItem.attr("portletseqid"),
            d_portalPageId: (nextObjectToDroppedItem != null) ? nextObjectToDroppedItem.attr("portalpageid") : null,
            d_portalPortletId: (nextObjectToDroppedItem != null) ? nextObjectToDroppedItem.attr("portalportletid") : null,
            d_portletSeqId: (nextObjectToDroppedItem != null) ? nextObjectToDroppedItem.attr("portletseqid") : null
    };
    
    jQuery.ajax({
        url: "/myportal/control/updatePortalPagePortletSeqAjax",
        data: requestData,
        type: "POST",
    }).success( function(data){ onCompleteRequest(); });

}

//removes the loading image
function onCompleteRequest() {
    var loading = document.getElementById("loading");
    if(loading != null){
        //IE Fix (IE treats DOM objects and Javascript objects separately, and you can't extend the DOM objects using Object.prototype)
        loading.parentNode.removeChild(loading);
    }
}

//safely get height of whole document
function getDocHeight() {
    var D = document;
    return Math.max(
        Math.max(D.body.scrollHeight, D.documentElement.scrollHeight),
        Math.max(D.body.offsetHeight, D.documentElement.offsetHeight),
        Math.max(D.body.clientHeight, D.documentElement.clientHeight)
    );
}

//displays the loading image
function onStartRequest() {    
    var p = document.createElement("div");
    p.setAttribute("id", "loading");    
    p.setAttribute("style", "height: " + getDocHeight() + "px;" )
    
    var img = document.createElement("img");
    img.setAttribute("src", "/images/loader.gif");
    img.setAttribute("id", "loaderImg");

    p.appendChild(img);
    
    var container = document.getElementById("content-main-section");
    container.appendChild(p);
}
