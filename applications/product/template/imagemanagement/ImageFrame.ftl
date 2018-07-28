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
<script type="application/javascript">
    jQuery.noConflict();
    var host = document.location.host;
    jQuery(document).ready(function() {
        var productId = jQuery('#ImageFrames_productId').val();
        var imageName = jQuery('#ImageFrames_imageName').val();
    });
    jQuery(window).load(function() {
        var width = jQuery('td.image-src img').width();
        var height = jQuery('td.image-src img').height();
        jQuery('td.image-src img').css("width", 200);
        var dimension = width + " x " + height + " pixels";
        jQuery('td.dimension').text(dimension);
        
        var widthFrame = jQuery('td.image-fr img').width();
        var heightFrame = jQuery('td.image-fr img').height();
        jQuery('td.image-fr img').css("width", 200);
        var dimensionFrame = widthFrame + " x " + heightFrame + " pixels";
        jQuery('td.frameDimension').text(dimensionFrame);
    });
    function setTargetWindows(target) {
        if ((target == "upload") || (target == "choose")) {
            jQuery('#ImageFrames').attr("target", "_self");
        } else {
            jQuery('#ImageFrames').attr("target", target);
        }

        if (target == "upload") {
            document.ImageFrames.action = "uploadFrame?productId=" + jQuery('#ImageFrames_productId').val() + "&contentId=" + jQuery('#ImageFrames_contentId').val() + "&dataResourceId="+ jQuery('#ImageFrames_dataResourceId').val() + "&frameExistDataResourceId="+ jQuery('#ImageFrames_frameDataResourceId').val() + "&frameExistContentId="+ jQuery('#ImageFrames_frameContentId').val();
        } else if (target == "choose") {
            document.ImageFrames.action = "chooseFrameImage?productId=" + jQuery('#ImageFrames_productId').val() + "&contentId=" + jQuery('#ImageFrames_contentId').val() + "&dataResourceId="+ jQuery('#ImageFrames_dataResourceId').val() + "&frameExistDataResourceId="+ jQuery('#ImageFrames_frameDataResourceId').val() + "&frameExistContentId="+ jQuery('#ImageFrames_frameContentId').val() + "&frameContentId="+jQuery('#0_lookupId_ImageFrames_imageFrameContentId').val();
        } else if (target == "new") {
            document.ImageFrames.action = 'previewFrameImage';
        } else {
            document.ImageFrames.action = 'createImageFrame';
        }

        if ((target != "upload") && (target != "choose")) {
            var width = jQuery('#ImageFrames_imageWidth').val();
            var hieght = jQuery('#ImageFrames_imageHeight').val();
            if ((width == "") || (hieght == "")) {
                jQuery('#ImageFrames').attr("target", "_self");
            }
        }
    }
    function setUploadTarget(target) {
        jQuery('#ImageFrames').attr("target", target);
    }
    function deletePreviewFrameImage() {
        jQuery.post("deleteFrameImage");
    }
</script>