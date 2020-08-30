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
* */

/**
 *
 */
var uiLabelJsonObject = null;
jQuery(document).ready(function() {
    var labelObject = {
            "CommonUiLabels" : ["CommonUpload", "CommonSave", "CommonCompleted"]
          };
    getJSONuiLabels(labelObject, function(result){
    	uiLabelJsonObjects = result.responseJSON;
    });
    jQuery("#progress_bar").progressbar({value: 0});
});

function uploadPartyContent(event){
    jQuery("#progress_bar").progressbar("option", "value", 0);
    var targetFrame = jQuery('#target_upload');
    var infodiv = jQuery('#content-messages');
    if(infodiv.length < 1){
        jQuery('<div id="content-messages"></div>').insertAfter(jQuery("#partyContentList"));
    }
    if (targetFrame.length < 1){
        jQuery('#partyContent').append("<iframe id='target_upload' name='target_upload' style='display: none' src=''> </iframe>");
    }
    jQuery('#uploadPartyContent').attr("target", "target_upload");

    var labelField = jQuery("#progressBarSavingMsg");
    if (labelField.length) {
        labelField.remove();
    }
}

function uploadCompleted(){
    var iframePartyContentList = jQuery("#target_upload").contents().find("#partyContentList").html();

    // update partyContentList - copy the Data from the iFrame partyContentList
    // to the page partyContentList
    jQuery("#partyContentList").html(iframePartyContentList);

    jQuery('#progressBarSavingMsg').html(uiLabelJsonObjects.CommonUiLabels[2]);
    // reset progressbar
    jQuery("#progress_bar").progressbar("option", "value", 0);

    // remove iFrame
    jQuery("#target_upload").remove();
    return;
}

function checkIframeStatus() {
    var iframePartyContentList = null;
    // if the new partyContentList isn't created wait a few ms and call the
    // method again
    jQuery.fjTimer({
        interval: 500,
        repeat: true,
        tick: function(counter, timerId) {
            iframePartyContentList = jQuery("#target_upload").contents().find("#partyContentList");
            if (iframePartyContentList != null && iframePartyContentList.length > 0) {
                timerId.stop();
                uploadCompleted();
            }
        }
    });
    return;
}

function getUploadProgressStatus(event){
    importLibrary(["/common/js/jquery/plugins/fjTimer/jquerytimer-min.js"], function(){
        jQuery('#uploadPartyContent').append("<span id='progressBarSavingMsg' class='label'>" + uiLabelJsonObjects.CommonUiLabels[0] + "...</span>");
        var i=0;
        jQuery.fjTimer({
            interval: 1000,
            repeat: true,
            tick: function(counter, timerId) {
                var timerId = timerId;
                jQuery.ajax({
                    url: 'getFileUploadProgressStatus',
                    dataType: 'json',
                    success: function(data) {
                        if (data._ERROR_MESSAGE_LIST_ != undefined) {
                            jQuery('#content-messages').html(data._ERROR_MESSAGE_LIST_);
                            timerId.stop();
                         } else if (data._ERROR_MESSAGE_ != undefined) {
                             jQuery('#content-messages').html(data._ERROR_MESSAGE_);
                            timerId.stop();
                         } else {
                            var readPercent = data.readPercent;
                            jQuery("#progress_bar").progressbar("option", "value", readPercent);
                            jQuery('#progressBarSavingMsg').html(uiLabelJsonObjects.CommonUiLabels[0] + "... (" + readPercent + "%)");
                            if(readPercent > 99){
                                jQuery('#progressBarSavingMsg').html(uiLabelJsonObjects.CommonUiLabels[1] + "...");
                                // stop the fjTimer
                                timerId.stop();
                                // call the upload complete method to do final stuff
                                checkIframeStatus();
                            }
                         }
                    },
                    error: function() {
                         timerId.stop();
                    }
                });
            }
        });
    });
}
