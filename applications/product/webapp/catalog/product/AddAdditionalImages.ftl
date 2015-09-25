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
<#if product?has_content>
  <#assign productAdditionalImage1 = (Static["org.ofbiz.product.product.ProductContentWrapper"].getProductContentAsText(product, "ADDITIONAL_IMAGE_1", locale, dispatcher, "url"))! />
  <#assign productAdditionalImage2 = (Static["org.ofbiz.product.product.ProductContentWrapper"].getProductContentAsText(product, "ADDITIONAL_IMAGE_2", locale, dispatcher, "url"))! />
  <#assign productAdditionalImage3 = (Static["org.ofbiz.product.product.ProductContentWrapper"].getProductContentAsText(product, "ADDITIONAL_IMAGE_3", locale, dispatcher, "url"))! />
  <#assign productAdditionalImage4 = (Static["org.ofbiz.product.product.ProductContentWrapper"].getProductContentAsText(product, "ADDITIONAL_IMAGE_4", locale, dispatcher, "url"))! />
</#if>
<form id="addAdditionalImagesForm" method="post" action="<@ofbizUrl>addAdditionalImagesForProduct</@ofbizUrl>" enctype="multipart/form-data">
  <input id="additionalImageProductId" type="hidden" name="productId" value="${productId!}" />
  <table>
    <tbody>
      <tr>
        <td><#if productAdditionalImage1?has_content><a href="javascript:void(0);" swapDetail="<@ofbizContentUrl>${productAdditionalImage1}</@ofbizContentUrl>"><img src="<@ofbizContentUrl>${productAdditionalImage1}</@ofbizContentUrl>" class="cssImgSmall" alt="" /></a></#if></td>
        <td><input id="additionalImageOne" type="file" size="20" name="additionalImageOne" /></td>
      </tr>
      <tr>
        <td><#if productAdditionalImage2?has_content><a href="javascript:void(0);" swapDetail="<@ofbizContentUrl>${productAdditionalImage2}</@ofbizContentUrl>" ><img src="<@ofbizContentUrl>${productAdditionalImage2}</@ofbizContentUrl>" class="cssImgSmall" alt="" /></a></#if></td>
        <td><input type="file" size="20" name="additionalImageTwo" /></td>
      </tr>
      <tr>
        <td><#if productAdditionalImage3?has_content><a href="javascript:void(0);" swapDetail="<@ofbizContentUrl>${productAdditionalImage3}</@ofbizContentUrl>"><img src="<@ofbizContentUrl>${productAdditionalImage3}</@ofbizContentUrl>" class="cssImgSmall" alt="" /></a></#if></td>
        <td><input type="file" size="20" name="additionalImageThree" /></td>
      </tr>
      <tr>
        <td><#if productAdditionalImage4?has_content><a href="javascript:void(0);" swapDetail="<@ofbizContentUrl>${productAdditionalImage4}</@ofbizContentUrl>"><img src="<@ofbizContentUrl>${productAdditionalImage4}</@ofbizContentUrl>" class="cssImgSmall" alt="" /></a></#if></td>
        <td><input type="file" size="20" name="additionalImageFour" /></td>
      </tr>
      <tr>
        <td></td>
        <td><input type="submit" value='${uiLabelMap.CommonUpload}' /></td>
      </tr>
    </tbody>
  </table>
  <div class="right" style='margin-top:-250px;'>
    <a href="javascript:void(0);"><img id="detailImage" name="mainImage" vspace="5" hspace="5" width="150" height="150" style='margin-left:50px' src="" alt="" /></a>
    <input type="hidden" id="originalImage" name="originalImage" />
  </div>
</form>
