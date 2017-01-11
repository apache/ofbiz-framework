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
<div class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.ParseSamplePricat}</li>
    </ul>
    <br class="clear"/>
  </div>
  <div class="screenlet-body">
  <form method="post" enctype="multipart/form-data" action="<@ofbizUrl>pricatUpload</@ofbizUrl>">
    <input type="hidden" name="action" value="store_excel" />
    <input type="file" size="60" name="filename"/><br />
    ${uiLabelMap.ExcelTemplateType}:
    <select name="excelTemplateType" id="excelTemplateType">
      <option value="sample_pricat" checked>${uiLabelMap.SamplePricatType}</option>
      <option value="ofbiz_pricat">${uiLabelMap.OFBizPricatType}</option>
    </select>
    <div class="button-bar"><input type="submit" value="${uiLabelMap.UploadPricat}"/></div>
  </form>
  </div>
</div>
<div class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.SamplePricatTemplate}</li>
    </ul>
    <br class="clear"/>
  </div>
  <div class="screenlet-body">
    <label id="downloadTemplate">
      <a href="<@ofbizUrl>downloadExcelTemplate?templateType=pricatExcelTemplate</@ofbizUrl>" target="_BLANK" class="buttontext">${uiLabelMap.DownloadPricatTemplate}</a>
    </label>
    <br class="clear"/>
  </div>
</div>
