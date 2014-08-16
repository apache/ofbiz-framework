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

<form id="addMultipleImagesForm" name="addMultipleImagesForm" method="post" action="<@ofbizUrl>addImageForProduct</@ofbizUrl>" enctype="multipart/form-data">
<br/>
<div class="label">${uiLabelMap.ProductProductId} <@htmlTemplate.lookupField name="productId" id="productId" formName="addMultipleImagesForm" fieldFormName="LookupProduct"/></div>
<br/>
<br/>
  <table>
    <tbody>
      <tr>
        <td>
            <select name="imageResize" >
                <#list productFeatures as productFeature>
                    <option value="${productFeature.abbrev!}">${productFeature.description!}</option>
                </#list>
                <option selected="" value="">Do not resize</option>
            </select>
        </td>
      </tr>
      <tr>
        <td><input type="file" size="20" name="additionalImageOne"/></td>
      </tr>
      <tr>
        <td><input type="file" size="20" name="additionalImageTwo"/></td>
      </tr>
      <tr>
        <td><input type="file" size="20" name="additionalImageThree"/></td>
      </tr>
      <tr>
        <td><input type="file" size="20" name="additionalImageFour"/></td>
      </tr>
      <tr>
        <td><input type="file" size="20" name="additionalImageFive"/></td>
      </tr>
      <tr>
        <td><input type="file" size="20" name="additionalImageSix"/></td>
      </tr>
      <tr>
        <td><input type="file" size="20" name="additionalImageSeven"/></td>
      </tr>
      <tr>
        <td><input type="file" size="20" name="additionalImageEight"/></td>
      </tr>
      <tr>
        <td><input type="file" size="20" name="additionalImageNine"/></td>
      </tr>
      <tr>
        <td><input type="file" size="20" name="additionalImageTen"/></td>
      </tr>
      <tr>
        <td><input type="submit" value='${uiLabelMap.CommonUpload}'/></td>
      </tr>
    </tbody>
  </table>
</form>
