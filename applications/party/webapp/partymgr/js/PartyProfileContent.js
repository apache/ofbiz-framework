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

var progressBar;

document.observe('dom:loaded', function() {
  Event.observe('uploadPartyContent', 'submit', uploadPartyContent);
  Event.observe('uploadPartyContent', 'submit', getUploadProgressStatus);
  progressBar = new Control.ProgressBar('progress_bar');
});
function uploadPartyContent(event){
    var targetFrame = $('target_upload');
    if(!targetFrame){
        $('partyContent').insert("<iframe id='target_upload' name='target_upload' style='display: none' src=''> </iframe>");
    }
    $('uploadPartyContent').target="target_upload";
    Event.observe('target_upload', 'load', uploadCompleted);
    var errordiv = $('content-messages');
    if(errordiv){
        $('content-messages').remove();
    }
}

function uploadCompleted(event){
    var doc = getIframeDocument($('target_upload'));
    var errordiv = doc.getElementById('content-messages');
    //console.log(errordiv);
    if(errordiv){
        $('partyContent').insert(errordiv);
    }
    var partyContentListDiv = doc.getElementById('partyContentList');
    //console.log(partyContentListDiv);
    if(partyContentListDiv){
        $('partyContentList').update(partyContentListDiv.innerHTML);
    }
    if($('progressBarSavingMsg')){
        $('progressBarSavingMsg').remove();
    }
    progressBar.reset();
}

function getUploadProgressStatus(event){
    var i=0;
    new PeriodicalExecuter(function(event){
        new Ajax.Request('/partymgr/control/getFileUploadProgressStatus', {
            onSuccess: function(transport){
                var data = transport.responseText.evalJSON(true);
                if (data._ERROR_MESSAGE_LIST_ != undefined) {
                   //console.log(data._ERROR_MESSAGE_LIST_);
                   //alert(data._ERROR_MESSAGE_LIST_);
                }else if (data._ERROR_MESSAGE_ != undefined) {
                   //console.log(data._ERROR_MESSAGE_);
                   //alert(data._ERROR_MESSAGE_);
                }else {
                   //console.log(data.readPercent);
                   var readPercent = data.readPercent;
                   progressBar.setProgress(readPercent);
                   if(readPercent > 99){
                          $('uploadPartyContent').insert("<span id='progressBarSavingMsg' class='label'>Saving..</span>");
                       event.stop();
                   }

                }
            }});
        },1);
}

function getIframeDocument(frameElement) {
  var doc = null;
  if (frameElement.contentDocument) {
    doc = frameElement.contentDocument;
  } else if (frameElement.contentWindow) {
    doc = frameElement.contentWindow.document;
  } else if (frameElement.document) {
    doc = frameElement.document;
  } else {
    return null;
  }
  return doc;
}
