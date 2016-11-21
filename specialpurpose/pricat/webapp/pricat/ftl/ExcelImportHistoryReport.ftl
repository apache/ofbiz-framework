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
<div class="row">	
	<div class="col-sm-12 m-b-xs text-left p-xs">
		<label >${uiLabelMap.ExcelImportHistoryList}</label>
		<span class="tooltip pad-left30 top">${uiLabelMap.OnlyYourOwnImportHistoryDisplayed}</span>
	</div> 
</div>

<div id="loadBody" style="display:none">
</div>
<table id="productlist" name="productlist" class="table table-striped ms-table-primary">
	<thead>
		<th></th>
		<th>${uiLabelMap.SerialNumber}</th>
		<th>${uiLabelMap.Filename}</th>
		<th>${uiLabelMap.FromDate}</th>
		<th>${uiLabelMap.ThruDate}</th>
		<th>${uiLabelMap.ImportStatus}</th>
		<th>${uiLabelMap.ThruReasonId}</th>
		<th>${uiLabelMap.Actions}</th>
	</thead>
	<tbody>
		<#if (data?has_content)>
			<#list data as historyEntry>
				<tr name="${historyEntry.sequenceNum!}">
					<td>
					<#if historyEntry.statusId?exists && historyEntry.statusId == 'EXCEL_IMPORTING'>
						<img src="/erptheme/images/blue_anime.gif" alt="${uiLabelMap.EXCE_IMPORTING}" tooltip="${uiLabelMap.EXCE_IMPORTING}">
					<#elseif historyEntry.statusId?exists && historyEntry.statusId == 'EXCEL_IMPORTED' && historyEntry.thruReasonId?exists && historyEntry.thruReasonId?has_content>
						<#if historyEntry.thruReasonId == 'EXCEL_IMPORT_SUCCESS'>
						<button id="excel-import-status" data-tooltip="${uiLabelMap.get(historyEntry.thruReasonId)}"><i class="icon-ok-sign success-color">${uiLabelMap.ReasonOK}</i></button>
						<#elseif historyEntry.thruReasonId == 'EXCEL_IMPORT_STOPPED'>
						<button id="excel-import-status" data-tooltip="${uiLabelMap.get(historyEntry.thruReasonId)}"><i class="icon-remove-sign stopped-color">${uiLabelMap.ReasonStopped}</i></button>
						<#elseif historyEntry.thruReasonId == 'EXCEL_IMPORT_ERROR'>
						<button id="excel-import-status" data-tooltip="${uiLabelMap.get(historyEntry.thruReasonId)}"><i class="icon-exclamation-sign error-color"></i>${uiLabelMap.ReasonError}</button>
						<#elseif historyEntry.thruReasonId == 'EXCEL_IMPORT_QUEST'>
						<button id="excel-import-status" data-tooltip="${uiLabelMap.get(historyEntry.thruReasonId)}"><i class="icon-question-sign question-color"></i>${uiLabelMap.ReasonWarning}</button>
						</#if>
					</#if>
					</td>
					<td>${historyEntry.sequenceNum!}</td>
  					<td style="text-align:right;">${historyEntry.fileName!}</td>
					<td><#if historyEntry.fromDate?exists && historyEntry.fromDate?has_content>${historyEntry.fromDate?string("yyyy-MM-dd HH:mm:ss")}</#if></td>
					<td><#if historyEntry.thruDate?exists && historyEntry.thruDate?has_content>${historyEntry.thruDate?string("yyyy-MM-dd HH:mm:ss")}</#if></td>
					<td><#if historyEntry.statusId?exists && historyEntry.statusId?has_content>${uiLabelMap.get(historyEntry.statusId)}</#if></td>
					<td><#if historyEntry.statusId?exists && historyEntry.statusId == "EXCEL_IMPORTED" && historyEntry.thruReasonId?exists && historyEntry.thruReasonId?has_content>${uiLabelMap.get(historyEntry.thruReasonId)}</#if></td>
					<td>
						<#assign buttons = 0 />
						<#if historyEntry.logFileName?exists && historyEntry.logFileName?has_content>
							<#if Static["org.apache.ofbiz.base.util.FileUtil"].getFile(historyEntry.logFileName).exists()>
                    		<button id="excel-import-log" type="button" onclick="viewExcelImportLog(${historyEntry.sequenceNum});" data-tooltip="${uiLabelMap.ViewExcelImportLogContent}">
					    		<i class="icon-comments icon-blue">${uiLabelMap.ViewPricatLog}</i>
				    		</button>
				    		<#assign buttons = buttons + 1 />
					        </#if>
                        </#if>
						<#if buttons == 0>
                    		<#--- <button id="excel-import-empty" type="button"></button> -->
				    		<#assign buttons = buttons + 1 />
						</#if>
						<#if historyEntry.logFileName?exists && historyEntry.logFileName?has_content>
							<#if Static["org.apache.ofbiz.pricat.AbstractPricatParser"].isCommentedExcelExists(request, historyEntry.sequenceNum)>
                    		<button id="excel-import-download" type="button" onclick="downloadCommentedExcel(${historyEntry.sequenceNum});" data-tooltip="${uiLabelMap.DownloadCommentedExcel}">
					    		<i class="icon-download icon-blue">${uiLabelMap.DownloadCommentedPricat}</i>
				    		</button>
				    		<#assign buttons = buttons + 1 />
					        </#if>
                        </#if>
						<#if buttons == 1>
                    		<#--- <button id="excel-import-empty" type="button"></button> -->
						</#if>
                    </td>
				</tr>
			</#list>
		<#else>
			<tr>
				<td colspan="10" style="text-align:center;vertical-align:middle;height:60px;">
					${uiLabelMap.ExcelImportTipNoData}
				</td>
			</tr>
		</#if>
	</tbody>
</table>
<script type="text/javascript" language="JavaScript">
	function viewExcelImportLog(sequenceNum) {
        document.location = "<@ofbizUrl>viewExcelImportLog</@ofbizUrl>?sequenceNum=" + sequenceNum;
    }

	function downloadCommentedExcel(sequenceNum) {
        document.location = "<@ofbizUrl>downloadCommentedExcel</@ofbizUrl>?sequenceNum=" + sequenceNum;
    }
</script>