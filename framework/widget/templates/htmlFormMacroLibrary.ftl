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

<#macro renderField text><#if text?exists>${text}</#if></#macro>

<#macro renderDisplayField idName description class alert>
<#if class?has_content || alert=="true"><span class="<#if class?has_content>${class}<#if alert=="true"> alert</#if>"</#if>><#rt/></#if>
<#if description?has_content>${description}<#else>&nbsp;</#if><#if class?has_content || alert=="true"></span></#if>
</#macro>
<#macro renderHyperlinkField></#macro>

<#macro renderTextField name className alert value textSize maxlength id event action clientAutocomplete ajaxUrl ajaxEnabled>
<input type="text" name="${name?default("")?html}"<#rt/>
<@renderClass className alert />
<#if value?has_content> value="${value}"</#if><#if textSize?has_content> size="${textSize}"</#if><#if maxlength?has_content> maxlength="${maxlength}"</#if><#rt/>
<#if id?has_content> id="${id}"</#if><#if event?has_content && action?has_content>${event}="${action}"</#if><#if clientAutocomplete?has_content && clientAutocomplete=="false"> autocomplete="off"</#if>/><#rt/>
<#if ajaxEnabled?has_content && ajaxEnabled>
<script language="JavaScript" type="text/javascript">ajaxAutoCompleter('${ajaxUrl}');</script>
</#if>
</#macro>

<#macro renderTextareaField name className alert cols rows id readonly value visualEdtiorEnalble buttons>
<textarea name="${name}" <@renderClass className alert /><#if cols?has_content> cols="${cols}"</#if><#if rows?has_content> rows="${rows}"</#if><#if id?has_content> id="${id}"</#if><#if readonly?has_content> ${readonly}</#if>><#rt/>
<#if value?has_content> ${value}</#if><#if maxlength?has_content> maxlength="${maxlength}"</#if></textarea><#rt/>
<#if visualEdtiorEnalble?has_content>
 <script language="javascript" src="/images/htmledit/whizzywig.js" type="text/javascript"></script><#rt/>
 <script language="javascript" type="text/javascript"> buttonPath = "/images/htmledit/"; cssFile="/images/htmledit/simple.css";makeWhizzyWig("<#rt/>
<#if id?has_content> "${id}"</#if>","<#if buttons?has_content> "${buttons}"</#if>")</script>
</#if>
</#macro>

<#macro renderDateTimeField name className alert title value size maxlength id dateType shortDateInput timeDropdownParamName defaultDateTimeString calGif localizedIconTitle timeDropdown timeHourName classString hour1 hour2 timeMinutesName minutes isTwelveHour ampmName amSelected pmSelected compositeType formName>
<input type="text" name="${name}" <@renderClass className alert /><#rt/>
<#if title?has_content> title="${title}"</#if><#if value?has_content> value="${value}"</#if><#if size?has_content> size="${size}"</#if><#rt/>
<#if maxlength?has_content>  maxlength="${maxlength}"</#if><#if id?has_content> id="${id}"</#if>/><#rt/>
<#if dateType!="time" >
<#if shortDateInput?exists && shortDateInput>
 <a href="javascript:call_cal_notime(document.<#rt/>
<#else>
 <a href="javascript:call_cal(document.<#rt/>
</#if>
${formName}.<#if timeDropdownParamName?has_content>${timeDropdownParamName},'</#if><#if defaultDateTimeString?has_content>${defaultDateTimeString},');"></#if><#rt/>
<#if calGif?has_content><img src="${calGif}" width="16" height="16" border="0" alt="<#if localizedIconTitle?has_content>${localizedIconTitle}</#if>" title="<#if localizedIconTitle?has_content>${localizedIconTitle}</#if>"/><#rt/></#if>
</a><#rt/>
</#if>
<#if timeDropdown?has_content && timeDropdown=="time-dropdown">
 <select name="${timeHourName}" <#if classString?has_content>class="${classString}"</#if>><#rt/>
 <#if isTwelveHour>
<#assign x=11>
<#list 1..x as i>
<option value="${i}" <#if hour1?has_content><#if i=hour1>selected</#if></#if>>${i}</option><#rt/>
</#list>  
<#else>
<#assign x=23>
<#list 1..x as i>
<option value="${i}"<#if hour2?has_content><#if i=hour2> selected</#if></#if>>${i}</option><#rt/>
</#list> 
</#if>
</select>:<select name="${timeMinutesName}" <#if classString?has_content>class="${classString}"</#if>><#rt/>
<#assign x=59>
<#list 1..x as i>
<option value="${i}"<#if minutes?has_content><#if i=minutes> selected</#if></#if>>${i}</option><#rt/>
</#list> 
</select><#rt/>
<#if isTwelveHour>
 <select name="${ampmName}" <#if classString?has_content>class="${classString}"</#if>><#rt/>
 <option value="AM" ${amSelected}>AM</option><#rt/>
 <option value="PM" ${pmSelected}>PM</option><#rt/>
 </select><#rt/>
</#if>
<input type="hidden" name="${compositeType}" value="Timestamp"/>
</#if>
</#macro>

<#macro renderDropDownField name className alert id multiple formName otherFieldName event action size firstInList currentValue explicitDescription allowEmpty options fieldName otherFieldName otherValue otherFieldSize>
<select name="${name?default("")}" <@renderClass className alert /><#if id?has_content> id="${id}"</#if><#if multiple?has_content> multiple="multiple"</#if><#if otherFieldSize?has_content> onchange="process_choice(this,document.${formName}.${otherFieldName})"</#if><#if event?has_content> ${event}="${action}"</#if><#if size?has_content> size="${size}"</#if>>
<#if firstInList?has_content && currentValue?has_content>
 <option selected="selected" value="${currentValue}">${explicitDescription}</option><#rt/>
 <option value="${currentValue}">---</option><#rt/>
</#if>
<#if allowEmpty?has_content>
<option value="">&nbsp;</option>
</#if>
<#list options as item>   
<option<#if currentValue?has_content && currentValue == item.key && dDFCurrent?has_content && "selected" == dDFCurrent> selected="selected"<#elseif !currentValue?has_content && noCurrentSelectedKey?has_content && noCurrentSelectedKey == item.key> selected="selected"</#if> value="${item.key}">${item.description}</option><#rt/>
</#list>	
</select>
<#if otherFieldName?has_content>
<noscript><input type='text' name='${otherFieldName}' /></noscript>
<script type='text/javascript' language='JavaScript'><!--
disa = ' disabled';
if(other_choice(document.${formName}.${fieldName}))
	disa = '';
document.write("<input type='text' name='${otherFieldName}' value='${otherValue}' size='${otherFieldSize}'"+disa+" onfocus='check_choice(document.${formName}.${fieldName})' />");
if(disa && document.styleSheets)
   document.${formName}.${fieldName}.style.visibility  = 'hidden';
//--></script>
</#if>
</#macro>

<#macro renderCheckField items className alert allChecked currentValue name event action>
<#list items as item>   
<input type="checkbox" <@renderClass className alert /><#rt/>
<#if allChecked?has_content && allChecked> checked="checked" <#elseif allChecked?has_content && !allChecked><#elseif currentValue?has_content && currentValue==item.key> checked="checked"</#if> name="${name?default("")?html}" value="${item.key?default("")?html}"<#if event?has_content> ${event}="${action}"</#if>/><#rt/>
${item.description}
</#list>
</#macro>
<#macro renderRadioField items className alert currentValue noCurrentSelectedKey name event ation>
<#list items as item>   
<div <@renderClass className alert />><#rt/>
<input type="radio"<#if currentValue?has_content><#if rp.currentValue==item.key> checked="checked"</#if><#elseif noCurrentSelectedKey?has_content && noCurrentSelectedKey == item.key> checked="checked"</#if> name="${name?default("")?html}" value="${item.key?default("")?html}"<#if event?has_content> ${event}="${action}"</#if>/><#rt/>
${item.description}</div>
</#list>
</#macro>

<#macro renderSubmitField buttonType className alert formName title name event action imgSrc>
<#if buttonType=="text-link">
 <a <@renderClass className alert /> href="javascript:document.${formName}.submit()"><#if title?has_content> title="${title}"</#if> </a>
<#elseif buttonType=="image">
 <input type="image" src="${imgSrc}" <#if className?has_content> class="${className}</#if><#if alert?has_content> ${alert}</#if>"<#if name?has_content> name="${name}"</#if><#if title?has_content> alt="${title}"</#if><#if event?has_content> ${event}="${action}"</#if> />
<#else>
<input type="submit" <#if className?has_content> class="${className}</#if><#if alert?has_content> ${alert}</#if>"<#if name?exists> name="${name}"</#if><#if title?has_content> value="${title}"</#if><#if event?has_content> ${event}="${action}"</#if> /></#if>
</#macro>
<#macro renderResetField className alert name title><input type="reset" <@renderClass className alert /> name="${name}"<#if title?has_content> value="${title}"</#if>/></#macro>

<#macro renderHiddenField name value><input type="hidden" name="${name}"<#if value?has_content> value="${value}"</#if>/></#macro>
<#macro renderIgnoredField></#macro>

<#macro renderFieldTitle style title><#if style?has_content><span class="${style}></#if>${title}<#if style?has_content></span></#if></#macro>
<#macro renderSingleFormFieldTitle></#macro>
    
<#macro renderFormOpen linkUrl formType targetWindow containerId containerStyle autocomplete name useRowSubmit><form method="post" action="${linkUrl}"<#if formType=="upload"> enctype="multipart/form-data"</#if><#if targetWindow?has_content> target="${targetWindow}"</#if><#if containerId?has_content> id="${containerId}"</#if> class=<#if containerStyle?has_content>"${containerStyle}"<#else>"basic-form"</#if> onSubmit="javascript:submitFormDisableSubmits(this)"<#if autocomplete?has_content> autocomplete="${autocomplete}"</#if> name="${name}" <#if useRowSubmit?has_content && useRowSubmit><input type="hidden" name="_useRowSubmit" value="Y"/></#if>></#macro>
<#macro renderFormClose focusFieldName formName></form><#if focusFieldName?has_content><script language="JavaScript" type="text/javascript">document.${formName}.${focusFieldName}.focus();</script></#if></#macro>
<#macro renderMultiFormClose></#macro>
    
<#macro renderFormatListWrapperOpen style>  <table cellspacing="0" class="<#if style?has_content>${style}<#else>basic-table form-widget-table dark-grid</#if>" > </#macro>
<#macro renderFormatListWrapperClose> </table></#macro>

<#macro renderFormatHeaderRowOpen style>  <tr class="<#if style?has_content>${style}<#else>header-row</#if>"></#macro>
<#macro renderFormatHeaderRowClose>  </tr></#macro>
<#macro renderFormatHeaderRowCellOpen style positionSpan>  <td <#if positionSpan?has_content && positionSpan gt 1 >colspan="${positionSpan}"</#if><#if style?has_content>class="${style}"</#if></#macro>
<#macro renderFormatHeaderRowCellClose></td></#macro>

<#macro renderFormatHeaderRowFormCellOpen style>   <td <#if style?has_content>class="${style}"</#if>></#macro>
<#macro renderFormatHeaderRowFormCellClose></td></#macro>
<#macro renderFormatHeaderRowFormCellTitleSeparator style isLast><#if style?has_content><sapn class="${style}"></#if> - <#if style?has_content></span></#if></#macro>
    
<#macro renderFormatItemRowOpen itemIndex altRowStyles evenRowStyle oddRowStyle> <tr <#if itemIndex?has_content><#if itemIndex%2==0><#if evenRowStyle?has_content>class="${evenRowStyle}<#if altRowStyles?has_content> ${altRowStyles}</#if>"<#elseif altRowStyles?has_content>class="${altRowStyles}"</#if><#else><#if oddRowStyle?has_content>class="${oddRowStyle}<#if altRowStyles?has_content> ${altRowStyles}</#if>"<#elseif altRowStyles?has_content>class="${altRowStyles}"</#if></#if></#if> ></#macro>
<#macro renderFormatItemRowClose>  </tr></#macro>
<#macro renderFormatItemRowCellOpen style positionSpan>  <td <#if positionSpan?has_content && positionSpan >1>colspan="${positionSpan}"</#if><#if style?has_content>class="${style}"</#if></#macro>
<#macro renderFormatItemRowCellClose>  </td></#macro>
<#macro renderFormatItemRowFormCellOpen style>   <td<#if style?has_content> class="${style}"</#if>></#macro>
<#macro renderFormatItemRowFormCellClose></td></#macro>

<#macro renderFormatSingleWrapperOpen style> <table cellspacing="0" <#if style?has_content>class="${style}"</#if> ></#macro>
<#macro renderFormatSingleWrapperClose> </table></#macro>

<#macro renderFormatFieldRowOpen>  <tr></#macro>
<#macro renderFormatFieldRowClose>  </tr></#macro>
<#macro renderFormatFieldRowTitleCellOpen style>   <td class="<#if style?has_content>${style}<#else>label</#if>"></#macro>
<#macro renderFormatFieldRowTitleCellClose></td></#macro>
<#macro renderFormatFieldRowSpacerCell></#macro>
<#macro renderFormatFieldRowWidgetCellOpen positionSpan style>   <td<#if positionSpan?has_content && positionSpan gt 0> colspan="${1+positionSpan*3}"</#if><#if style?has_content> class="${style}"</#if>></#macro>
<#macro renderFormatFieldRowWidgetCellClose></td></#macro>

<#macro renderFormatEmptySpace>&nbsp;</#macro>

<#macro renderTextFindField></#macro>
<#macro renderDateFindField></#macro>
<#macro renderRangeFindField></#macro>
<#macro renderLookupField></#macro>
<#macro renderFileField></#macro>
<#macro renderPasswordField></#macro>
<#macro renderImageField></#macro>
<#macro renderBanner></#macro>
<#macro renderFieldGroupOpen></#macro>
<#macro renderFieldGroupClose></#macro>

<#macro renderHyperlinkTitle name title><#if title?has_content>${title}<br/></#if><input type="checkbox" name="selectAll" value="Y" onclick="javascript:toggleAll(this, '${name}');"/></#macro>
<#macro renderSortField style title linkUrl ajaxEnabled><a<#if style?has_content> class="${style}"</#if> href="<#if ajaxEnabled?has_content && ajaxEnabled>javascript:ajaxUpdateAreas('${linkUrl}')<#else>${linkUrl}</#if>">${title}</a></#macro>
<#macro formatBoundaryComment boundaryType widgetType widgetName><!-- ${boundaryType}  ${widgetType}  ${widgetName} --></#macro>

<#macro renderTooltip tooltip tooltipStyle><#if tooltip?has_content><span class="<#if tooltipStyle?exists>${tooltipStyle}<#else>tooltip</#if>">${tooltip}</span><#rt/></#if></#macro>
<#macro renderClass className alert><#if className?has_content>class="${className}</#if><#if alert?exists> ${alert}</#if><#if className?exists>"<#rt/></#if></#macro>
<#macro renderAsterisks requiredField requiredStyle><#if requiredField=="true"><#if requiredStyle?has_content>*</#if></#if></#macro>