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

<#macro renderField text>
    <#if text?exists>
        ${text}<#lt/>
    </#if>
</#macro>

<#macro renderDisplayField type imageLocation idName description class alert inPlaceEditorUrl="" inPlaceEditorParams="">
    <#if type?has_content && type=="image">
        <img src="${imageLocation}"><#lt/>
    <#else>
        <#if inPlaceEditorUrl?has_content || class?has_content || alert=="true">
            <span <#if idName?has_content>id="${idName}"</#if> <@renderClass class alert />><#t/>
        </#if>
        
        <#if description?has_content>
            ${description?replace("\n", "<br />")}<#t/>
        <#else>
            &nbsp;<#t/>
        </#if>
        <#if inPlaceEditorUrl?has_content || class?has_content || alert=="true">
            </span><#lt/>
        </#if>
        <#if inPlaceEditorUrl?has_content && idName?has_content>
            <script language="JavaScript" type="text/javascript"><#lt/>
            ajaxInPlaceEditDisplayField('${idName}', '${inPlaceEditorUrl}', ${inPlaceEditorParams});<#lt/>
            </script><#lt/>
        </#if>
    </#if>
</#macro>
<#macro renderHyperlinkField></#macro>

<#macro renderTextField name className alert value textSize maxlength id event action disabled clientAutocomplete ajaxUrl ajaxEnabled>
    <input type="text" name="${name?default("")?html}"<#t/>
    <@renderClass className alert />
    <#if value?has_content> value="${value}"</#if><#rt/>
    <#if textSize?has_content> size="${textSize}"</#if><#rt/>
    <#if maxlength?has_content> maxlength="${maxlength}"</#if><#rt/>
    <#if disabled?has_content && disabled> disabled="disabled"</#if><#rt/>    
    <#if id?has_content> id="${id}"</#if><#rt/>
    <#if event?has_content && action?has_content> ${event}="${action}"</#if><#rt/>
    <#if clientAutocomplete?has_content && clientAutocomplete=="false"> autocomplete="off"</#if><#rt/>
    /><#t/>
    <#if ajaxEnabled?has_content && ajaxEnabled>
        <script language="JavaScript" type="text/javascript">ajaxAutoCompleter('${ajaxUrl}');</script><#lt/>
    </#if>
</#macro>

<#macro renderTextareaField name className alert cols rows id readonly value visualEdtiorEnalble buttons>
    <textarea name="${name}"<#t/>
    <@renderClass className alert />
    <#if cols?has_content> cols="${cols}"</#if><#rt/>
    <#if rows?has_content> rows="${rows}"</#if><#rt/>
    <#if id?has_content> id="${id}"</#if><#rt/>
    <#if readonly?has_content> ${readonly}</#if><#rt/>
    <#if maxlength?has_content> maxlength="${maxlength}"</#if><#rt/>
    ><#t/>
    <#if value?has_content>${value}</#if><#t/>
    </textarea><#lt/>
    <#if visualEdtiorEnalble?has_content>
        <script language="javascript" src="/images/htmledit/whizzywig.js" type="text/javascript"></script><#rt/>
        <script language="javascript" type="text/javascript"> buttonPath = "/images/htmledit/"; cssFile="/images/htmledit/simple.css"; makeWhizzyWig("${id?default("")}", "${buttons?default("")}")</script>
    </#if>
</#macro>

<#macro renderDateTimeField name className alert title value size maxlength id dateType shortDateInput timeDropdownParamName defaultDateTimeString localizedIconTitle timeDropdown timeHourName classString hour1 hour2 timeMinutesName minutes isTwelveHour ampmName amSelected pmSelected compositeType formName>
  <div class="view-calendar">
    <ul>
      <li><input type="text" name="${name}" <@renderClass className alert /><#rt/>
        <#if title?has_content> title="${title}"</#if>
        <#if value?has_content> value="${value}"</#if>
        <#if size?has_content> size="${size}"</#if><#rt/>
        <#if maxlength?has_content>  maxlength="${maxlength}"</#if>
        <#if id?has_content> id="${id}"</#if>/><#rt/>
      </li>
      <#if dateType!="time" >
        <li>
          <#if shortDateInput?exists && shortDateInput>
             <a href="javascript:call_cal_notime(document.<#rt/>
          <#else>
             <a href="javascript:call_cal(document.<#rt/>
          </#if>
          ${formName}.
          <#if timeDropdownParamName?has_content>${timeDropdownParamName}</#if>
          <#if defaultDateTimeString?has_content>,'${defaultDateTimeString}'</#if>);" 
          title="<#if localizedIconTitle?has_content>${localizedIconTitle}</#if>"><#rt/>
          </a><#rt/>
        </li>
      </#if>
      <#if timeDropdown?has_content && timeDropdown=="time-dropdown">
        <li>
          <select name="${timeHourName}" <#if classString?has_content>class="${classString}"</#if>><#rt/>
          <#if isTwelveHour>
            <#assign x=11>
            <#list 0..x as i>
              <option value="${i}"<#if hour1?has_content><#if i=hour1> selected="selected"</#if></#if>>${i}</option><#rt/>
            </#list>
          <#else>
            <#assign x=23>
            <#list 0..x as i>
              <option value="${i}"<#if hour2?has_content><#if i=hour2> selected="selected"</#if></#if>>${i}</option><#rt/>
            </#list>
          </#if>
          </select>:<select name="${timeMinutesName}" <#if classString?has_content>class="${classString}"</#if>><#rt/>
            <#assign x=59>
            <#list 0..x as i>
              <option value="${i}"<#if minutes?has_content><#if i=minutes> selected="selected"</#if></#if>>${i}</option><#rt/>
            </#list>
          </select>
        </li><#rt/>
        <#if isTwelveHour>
          <li>
            <select name="${ampmName}" <#if classString?has_content>class="${classString}"</#if>><#rt/>
              <option value="AM" <#if amSelected == "selected">selected="selected"</#if> >AM</option><#rt/>
              <option value="PM" <#if pmSelected == "selected">selected="selected"</#if>>PM</option><#rt/>
            </select>
          </li><#rt/>
        </#if>
      </#if>
    </ul>
    <input type="hidden" name="${compositeType}" value="Timestamp"/>
  </div>
</#macro>

<#macro renderDropDownField name className alert id multiple formName otherFieldName event action size firstInList currentValue explicitDescription allowEmpty options fieldName otherFieldName otherValue otherFieldSize dDFCurrent ajaxEnabled noCurrentSelectedKey ajaxOptions frequency minChars choices autoSelect partialSearch partialChars ignoreCase fullSearch>
<#if ajaxEnabled><input type="text"<#else><select</#if> name="${name?default("")}<#rt/>
<#if ajaxEnabled>
_description"<#if id?has_content> id="${id}_description"</#if><#if currentValue?has_content> value="${explicitDescription}"</#if>/><#rt/>
<input type="hidden" name="${name}"<#if id?has_content> id="${id}"</#if><#if currentValue?has_content> value="${currentValue}"</#if>/><#rt/>
<script language="JavaScript" type="text/javascript">var data = {${ajaxOptions}};ajaxAutoCompleteDropDown('<#if id?has_content>${id}_description</#if>','${id}',data,{autoSelect:${autoSelect},frequency:${frequency},minChars:${minChars},choices:${choices},partialSearch:${partialSearch},partialChars:${partialChars},ignoreCase:${ignoreCase},fullSearch:${fullSearch}});</script>
<#else>
" <@renderClass className alert /><#if id?has_content> id="${id}"</#if><#if multiple?has_content> multiple="multiple"</#if><#if otherFieldSize gt 0> onchange="process_choice(this,document.${formName}.${otherFieldName})"</#if><#if event?has_content> ${event}="${action}"</#if><#if size?has_content> size="${size}"</#if>>
<#if firstInList?has_content && currentValue?has_content && !multiple?has_content>
 <option selected="selected" value="${currentValue}">${explicitDescription}</option><#rt/>
 <option value="${currentValue}">---</option><#rt/>
</#if>
<#if allowEmpty?has_content || !options?has_content>
<option value="">&nbsp;</option>
</#if>
<#list options as item>
<#if multiple?has_content>
 <option<#if currentValue?has_content && item.selected?has_content> selected="${item.selected}" <#elseif !currentValue?has_content && noCurrentSelectedKey?has_content && noCurrentSelectedKey == item.key> selected="selected" </#if> value="${item.key}">${item.description}</option><#rt/>
<#else>
 <option<#if currentValue?has_content && currentValue == item.key && dDFCurrent?has_content && "selected" == dDFCurrent> selected="selected"<#elseif !currentValue?has_content && noCurrentSelectedKey?has_content && noCurrentSelectedKey == item.key> selected="selected"</#if> value="${item.key}">${item.description}</option><#rt/>
</#if>
</#list>
</select>
<#if otherFieldName?has_content>
<noscript><input type='text' name='${otherFieldName}' /></noscript>
<script type='text/javascript' language='JavaScript'><!--
disa = ' disabled';
if(other_choice(document.${formName}.${fieldName}))
    disa = '';
document.write("<input type='text' name='${otherFieldName}' value='${otherValue?js_string}' size='${otherFieldSize}'"+disa+" onfocus='check_choice(document.${formName}.${fieldName})' />");
if(disa && document.styleSheets)
   document.${formName}.${fieldName}.style.visibility  = 'hidden';
//--></script>
</#if>
</#if>
</#macro>

<#macro renderCheckField items className alert allChecked currentValue name event action>
<#list items as item>
<input type="checkbox" <@renderClass className alert /><#rt/>
<#if allChecked?has_content && allChecked> checked="checked" <#elseif allChecked?has_content && !allChecked><#elseif currentValue?has_content && currentValue==item.value> checked="checked"</#if> name="${name?default("")?html}" value="${item.value?default("")?html}"<#if event?has_content> ${event}="${action}"</#if>/><#rt/>
${item.description?default("")}
</#list>
</#macro>
<#macro renderRadioField items className alert currentValue noCurrentSelectedKey name event action>
<#list items as item>
<div <@renderClass className alert />><#rt/>
<input type="radio"<#if currentValue?has_content><#if currentValue==item.key> checked="checked"</#if><#elseif noCurrentSelectedKey?has_content && noCurrentSelectedKey == item.key> checked="checked"</#if> name="${name?default("")?html}" value="${item.key?default("")?html}"<#if event?has_content> ${event}="${action}"</#if>/><#rt/>
${item.description}</div>
</#list>
</#macro>

<#macro renderSubmitField buttonType className alert formName title name event action imgSrc confirmation containerId ajaxUrl>
<#if buttonType=="text-link">
 <a <@renderClass className alert /> href="javascript:document.${formName}.submit()" <#if confirmation?has_content>onclick="return confirm('${confirmation?js_string}');"</#if>><#if title?has_content>${title}</#if> </a>
<#elseif buttonType=="image">
 <input type="image" src="${imgSrc}" <@renderClass className alert /><#if name?has_content> name="${name}"</#if><#if title?has_content> alt="${title}"</#if><#if event?has_content> ${event}="${action}"</#if> <#if confirmation?has_content>onclick="return confirm('${confirmation?js_string}');"</#if>/>
<#else>
<input type="<#if containerId?has_content>button<#else>submit</#if>" <@renderClass className alert /><#if name?exists> name="${name}"</#if><#if title?has_content> value="${title}"</#if><#if event?has_content> ${event}="${action}"</#if><#if containerId?has_content> onclick="<#if confirmation?has_content>if (confirm('${confirmation?js_string}')) </#if>ajaxSubmitFormUpdateAreas('${containerId}', '${ajaxUrl}')"<#else><#if confirmation?has_content> onclick="return confirm('${confirmation?js_string}');"</#if></#if>/></#if>
</#macro>

<#macro renderResetField className alert name title>
    <input type="reset" <@renderClass className alert /> name="${name}"<#if title?has_content> value="${title}"</#if>/>
</#macro>

<#macro renderHiddenField name value id event action>
    <input type="hidden" name="${name}"<#if value?has_content> value="${value}"</#if><#if id?has_content> id="${id}"</#if><#if event?has_content && action?has_content> ${event}="${action}"</#if>/>
</#macro>

<#macro renderIgnoredField></#macro>

<#macro renderFieldTitle style title>
    <#if style?has_content>
        <span class="${style}"><#t/>
    </#if>
    ${title}<#t/>
    <#if style?has_content>
        </span><#lt/>
    </#if>
</#macro>

<#macro renderSingleFormFieldTitle></#macro>

<#macro renderFormOpen linkUrl formType targetWindow containerId containerStyle autocomplete name useRowSubmit>

    <form method="post" action="${linkUrl}"<#if formType=="upload"> enctype="multipart/form-data"</#if><#if targetWindow?has_content> target="${targetWindow}"</#if><#if containerId?has_content> id="${containerId}"</#if> class=<#if containerStyle?has_content>"${containerStyle}"<#else>"basic-form"</#if> onsubmit="javascript:submitFormDisableSubmits(this)"<#if autocomplete?has_content> autocomplete="${autocomplete}"</#if> name="${name}"><#lt/>
    <#if useRowSubmit?has_content && useRowSubmit>
        <input type="hidden" name="_useRowSubmit" value="Y"/>
    </#if>
</#macro>
<#macro renderFormClose focusFieldName formName>
    </form><#lt/>
    <#if focusFieldName?has_content>
        <script language="JavaScript" type="text/javascript">document.${formName}.${focusFieldName}.focus();</script><#lt/>
    </#if>
</#macro>
<#macro renderMultiFormClose></#macro>

<#macro renderFormatListWrapperOpen formName style columnStyles>
    <table cellspacing="0" class="<#if style?has_content>${style}<#else>basic-table form-widget-table dark-grid</#if>"><#lt/>
</#macro>

<#macro renderFormatListWrapperClose formName>
    </table><#lt/>
</#macro>

<#macro renderFormatHeaderRowOpen style>
    <tr class="<#if style?has_content>${style}<#else>header-row</#if>">
</#macro>
<#macro renderFormatHeaderRowClose>
    </tr>
</#macro>
<#macro renderFormatHeaderRowCellOpen style positionSpan>
    <td <#if positionSpan?has_content && positionSpan gt 1 >colspan="${positionSpan}"</#if><#if style?has_content>class="${style}"</#if>>
</#macro>
<#macro renderFormatHeaderRowCellClose>
    </td>
</#macro>

<#macro renderFormatHeaderRowFormCellOpen style>
    <td <#if style?has_content>class="${style}"</#if>>
</#macro>
<#macro renderFormatHeaderRowFormCellClose>
    </td>
</#macro>
<#macro renderFormatHeaderRowFormCellTitleSeparator style isLast>
<#if style?has_content><span class="${style}"></#if> - <#if style?has_content></span></#if>
</#macro>

<#macro renderFormatItemRowOpen formName itemIndex altRowStyles evenRowStyle oddRowStyle>
    <tr <#if itemIndex?has_content><#if itemIndex%2==0><#if evenRowStyle?has_content>class="${evenRowStyle}<#if altRowStyles?has_content> ${altRowStyles}</#if>"<#elseif altRowStyles?has_content>class="${altRowStyles}"</#if><#else><#if oddRowStyle?has_content>class="${oddRowStyle}<#if altRowStyles?has_content> ${altRowStyles}</#if>"<#elseif altRowStyles?has_content>class="${altRowStyles}"</#if></#if></#if> >
</#macro>
<#macro renderFormatItemRowClose formName>
    </tr>
</#macro>
<#macro renderFormatItemRowCellOpen fieldName style positionSpan>
    <td <#if positionSpan?has_content && positionSpan gt 1>colspan="${positionSpan}"</#if><#if style?has_content>class="${style}"</#if>>
</#macro>
<#macro renderFormatItemRowCellClose fieldName>
    </td>
</#macro>
<#macro renderFormatItemRowFormCellOpen style>
    <td<#if style?has_content> class="${style}"</#if>>
</#macro>
<#macro renderFormatItemRowFormCellClose>
    </td>
</#macro>

<#macro renderFormatSingleWrapperOpen formName style>
    <table cellspacing="0" <#if style?has_content>class="${style}"</#if>>
</#macro>
<#macro renderFormatSingleWrapperClose formName>
    </table>
</#macro>

<#macro renderFormatFieldRowOpen>
    <tr>
</#macro>
<#macro renderFormatFieldRowClose>
    </tr>
</#macro>
<#macro renderFormatFieldRowTitleCellOpen style>
    <td class="<#if style?has_content>${style}<#else>label</#if>">
</#macro>
<#macro renderFormatFieldRowTitleCellClose>
    </td>
</#macro>
<#macro renderFormatFieldRowSpacerCell></#macro>
<#macro renderFormatFieldRowWidgetCellOpen positionSpan style>
    <td<#if positionSpan?has_content && positionSpan gt 0> colspan="${1+positionSpan*3}"</#if><#if style?has_content> class="${style}"</#if>>
</#macro>
<#macro renderFormatFieldRowWidgetCellClose>
    </td>
</#macro>

<#--
    Initial work to convert table based layout for "single" form to divs.
<#macro renderFormatSingleWrapperOpen style> <div <#if style?has_content>class="${style}"</#if> ></#macro>
<#macro renderFormatSingleWrapperClose> </div></#macro>

<#macro renderFormatFieldRowOpen>  <div></#macro>
<#macro renderFormatFieldRowClose>  </div></#macro>
<#macro renderFormatFieldRowTitleCellOpen style>   <div class="<#if style?has_content>${style}<#else>label</#if>"></#macro>
<#macro renderFormatFieldRowTitleCellClose></div></#macro>
<#macro renderFormatFieldRowSpacerCell></#macro>
<#macro renderFormatFieldRowWidgetCellOpen positionSpan style>   <div<#if positionSpan?has_content && positionSpan gt 0> colspan="${1+positionSpan*3}"</#if><#if style?has_content> class="${style}"</#if>></#macro>
<#macro renderFormatFieldRowWidgetCellClose></div></#macro>

-->


<#macro renderFormatEmptySpace>&nbsp;</#macro>

<#macro renderTextFindField name value defaultOption opEquals opBeginsWith opContains opIsEmpty opNotEqual className alert size maxlength autocomplete titleStyle hideIgnoreCase ignCase ignoreCase>
<#if opEquals?has_content>
<select <#if name?has_content>name="${name}_op"</#if>    class="selectBox"><#rt/>
<option value="equals"<#if defaultOption=="equals"> selected="selected"</#if>>${opEquals}</option><#rt/>
<option value="like"<#if defaultOption=="like"> selected="selected"</#if>>${opBeginsWith}</option><#rt/>
<option value="contains"<#if defaultOption=="contains"> selected="selected"</#if>>${opContains}</option><#rt/>
<option value="empty"<#rt/><#if defaultOption=="empty"> selected="selected"</#if>>${opIsEmpty}</option><#rt/>
<option value="notEqual"<#if defaultOption=="notEqual"> selected="selected"</#if>>${opNotEqual}</option><#rt/>
</select>
<#else>
<input type="hidden" name=<#if name?has_content> "${name}_op"</#if>    value="${defaultOption}"/><#rt/>
</#if>
 <input type="text" <@renderClass className alert /> name="${name}"<#if value?has_content> value="${value}"</#if><#if size?has_content> size="${size}"</#if><#if maxlength?has_content> maxlength="${maxlength}"</#if><#if autocomplete?has_content> autocomplete="off"</#if>/><#rt/>
<#if titleStyle?has_content><span class="${titleStyle}" ><#rt/></#if>
<#if hideIgnoreCase>
 <input type="hidden" name="${name}_ic" value=<#if ignCase>"Y"<#else> ""</#if>/><#rt/>
 <#else>
 <input type="checkbox" name="${name}_ic" value="Y" <#if ignCase> checked="checked"</#if> /> ${ignoreCase}<#rt/>
</#if><#if titleStyle?has_content></span></#if>
</#macro>

<#macro renderDateFindField className alert name localizedInputTitle value size maxlength dateType formName defaultDateTimeString imgSrc localizedIconTitle titleStyle defaultOptionFrom defaultOptionThru opEquals opSameDay opGreaterThanFromDayStart opGreaterThan opGreaterThan opLessThan opUpToDay opUpThruDay opIsEmpty>
<div class="view-calendar"><ul>
<li><input type="text" <@renderClass className alert /><#if name?has_content> name="${name?html}_fld0_value"</#if><#if localizedInputTitle?has_content> title="${localizedInputTitle}"</#if><#if value?has_content> value="${value}"</#if><#if size?has_content> size="${size}"</#if><#if maxlength?has_content> maxlength="${maxlength}"</#if>/></li><#rt/>
<#if dateType != "time">
<li>
<#if dateType == "date">
<a href="javascript:call_cal_notime(document.<#rt/>
<#else>
<a href="javascript:call_cal(document.<#rt/>
</#if>
<#if formName?has_content>${formName}.</#if><#if name?has_content>${name}_fld0_value,</#if>'<#if defaultDateTimeString?has_content>${defaultDateTimeString}</#if>');" title="${localizedIconTitle}"><#rt/>
</a><#rt/>
</li>
</#if>
<li>
<#if titleStyle?has_content>
<span class="${titleStyle}"><#rt/>
</#if>
<select<#if name?has_content> name="${name}_fld0_op"</#if> class="selectBox"><#rt/>
<option value="equals"<#if defaultOptionFrom=="equals"> selected="selected"</#if>>${opEquals}</option><#rt/>
<option value="sameDay"<#if defaultOptionFrom=="sameDay"> selected="selected"</#if>>${opSameDay}</option><#rt/>
<option value="greaterThanFromDayStart"<#if defaultOptionFrom=="greaterThanFromDayStart"> selected="selected"</#if>>${opGreaterThanFromDayStart}</option><#rt/>
<option value="greaterThan"<#if defaultOptionFrom=="greaterThan"> selected="selected"</#if>>${opGreaterThan}</option><#rt/>
</select><#rt/>
<#if titleStyle?has_content>
 </span><#rt/>
</#if>
</li></ul>
<ul><#rt/>
<li><input type="text" <@renderClass className alert /><#if name?has_content> name="${name}_fld1_value"</#if><#if localizedInputTitle?exists> title="${localizedInputTitle?html}"</#if><#if value2?has_content> value="${value2}"</#if><#if size?has_content> size="${size}"</#if><#if maxlength?has_content> maxlength="${maxlength}"</#if>/></li><#rt/>
<#if dateType != "time">
<li>
<#if dateType == "date">
<a href="javascript:call_cal_notime(document.<#rt/>
<#else>
<a href="javascript:call_cal(document.<#rt/>
</#if>
<#if formName?has_content>${formName}.</#if><#if name?has_content>${name}_fld1_value,'</#if><#if defaultDateTimeString?has_content>${defaultDateTimeString}</#if>');" title="${localizedIconTitle}"><#rt/>
</a><#rt/>
</#if>
</li>
<li>
<#if titleStyle?has_content>
 <span class="${titleStyle}"><#rt/>
</#if>
<select name=<#if name?has_content>"${name}_fld1_op"</#if> class="selectBox"><#rt/>
<option value="opLessThan"<#if defaultOptionThru=="opLessThan"> selected="selected"</#if>>${opLessThan}</option><#rt/>
<option value="upToDay"<#if defaultOptionThru=="upToDay"> selected="selected"</#if>>${opUpToDay}</option><#rt/>
<option value="upThruDay"<#if defaultOptionThru=="upThruDay"> selected="selected"</#if>>${opUpThruDay}</option><#rt/>
<option value="empty"<#if defaultOptionFrom=="empty"> selected="selected"</#if>>${opIsEmpty}</option><#rt/>
</select><#rt/>
<#if titleStyle?has_content>
</span>
</#if>
</li></ul></div>
</#macro>

<#macro renderRangeFindField className alert name value size maxlength autocomplete titleStyle defaultOptionFrom opEquals opGreaterThan opGreaterThanEquals opLessThan opLessThanEquals value2 defaultOptionThru>
<input type="text" <@renderClass className alert /> <#if name?has_content>name="${name}_fld0_value"</#if><#if value?has_content> value="${value}"</#if><#if size?has_content> size="${size}"</#if><#if maxlength?has_content> maxlength="${maxlength}"</#if><#if autocomplete?has_content> autocomplete="off"</#if>/><#rt/>
<#if titleStyle?has_content>
 <span class="${titleStyle}" ><#rt/>
</#if>
<select <#if name?has_content>name="${name}_fld0_op"</#if> class="selectBox"><#rt/>
<option value="equals"<#if defaultOptionFrom=="equals"> selected="selected"</#if>>${opEquals}</option><#rt/>
<option value="greaterThan"<#if defaultOptionFrom=="greaterThan"> selected="selected"</#if>>${opGreaterThan}</option><#rt/>
<option value="greaterThanEqualTo"<#if defaultOptionFrom=="greaterThanEqualTo"> selected="selected"</#if>>${opGreaterThanEquals}</option><#rt/>
</select><#rt/>
<#if titleStyle?has_content>
</span><#rt/>
</#if>
<br /><#rt/>
<input type="text" <@renderClass className alert /><#if name?has_content> name="${name}_fld1_value"</#if><#if value2?has_content> value="${value2}"</#if><#if size?has_content> size="${size}"</#if><#if maxlength?has_content> maxlength="${maxlength}"</#if><#if autocomplete?has_content> autocomplete="off"</#if>/><#rt/>
<#if titleStyle?has_content>
 <span class="${titleStyle}" ><#rt/>
</#if>
<select name=<#if name?has_content>"${name}_fld1_op"</#if> class="selectBox"><#rt/>
<option value="lessThan"<#if defaultOptionThru=="lessThan"> selected="selected"</#if>>${opLessThan?html}</option><#rt/>
<option value="lessThanEqualTo"<#if defaultOptionThru=="lessThanEqualTo"> selected="selected"</#if>>${opLessThanEquals?html}</option><#rt/>
</select><#rt/>
<#if titleStyle?has_content>
 </span>
</#if>
</#macro>

<#macro renderLookupField className alert name value size maxlength id event action disabled autocomplete descriptionFieldName formName fieldFormName targetParameterIter imgSrc ajaxUrl ajaxEnabled presentation width height position fadeBackground clearText>
<div class="field-lookup"><ul>
<#if size?has_content && size=="0"><li><input type="hidden" <#if name?has_content> name="${name}"/></#if></li><#else><li><input type="text" <@renderClass className alert /><#if name?has_content> name="${name}"</#if><#if value?has_content> value="${value}"</#if><#if size?has_content> size="${size}"</#if><#if maxlength?has_content> maxlength="${maxlength}"</#if><#if id?has_content> id="${id}"</#if><#rt/><#if disabled?has_content && disabled> disabled="disabled"</#if><#rt/><#if event?has_content && action?has_content> ${event}="${action}"</#if><#rt/><#if autocomplete?has_content> autocomplete="off"</#if>/><#rt/></li></#if>
<li><#if presentation?has_content && descriptionFieldName?has_content && presentation == "layer">
 <a href="javascript:call_fieldlookupLayer3(document.${formName?html}.${name?html}, document.${formName?html}.${descriptionFieldName},'${fieldFormName}','${width}','${height}','${position}', '${fadeBackground}'<#rt/>
<#elseif presentation?has_content && presentation == "layer">
 <a href="javascript:call_fieldlookupLayer(document.${formName?html}.${name?html},'${fieldFormName}','${width}','${height}','${position}', '${fadeBackground}'<#rt/>
<#elseif descriptionFieldName?has_content>
 <a href="javascript:call_fieldlookup3(document.${formName?html}.${name?html}, document.${formName?html}.${descriptionFieldName},'${fieldFormName}'<#rt/>
<#else>
 <a href="javascript:call_fieldlookup2(document.${formName}.${name},'${fieldFormName}'<#rt/>
</#if>
<#if targetParameterIter?has_content>
 <#list targetParameterIter as item>
  ,document.${formName}.${item}.value<#rt>
 </#list>
</#if>
);">
<#if ajaxEnabled?has_content && ajaxEnabled><span id="${id}_indicator" style="display: none" class="indicator"><img src="" alt=""/></span></#if>
</a></li><#rt>
<#if disabled?has_content && disabled><li><a id="${id}_clear" style="background:none;margin-left:-6px;margin-right:15px;" class="clearField" href="javascript:void();" onclick="javascript:document.${formName}.${name}.value='';<#if descriptionFieldName?has_content>document.${formName}.${descriptionFieldName}.value='';</#if>">${clearText}</a></li></#if>
</ul></div>
<#if ajaxEnabled?has_content && ajaxEnabled>
    <script language="JavaScript" type="text/javascript">ajaxAutoCompleter('${ajaxUrl}');</script><#t/>
</#if>
</#macro>

<#macro renderNextPrev paginateStyle paginateFirstStyle viewIndex highIndex listSize viewSize ajaxEnabled javaScriptEnabled ajaxFirstUrl firstUrl paginateFirstLabel paginatePreviousStyle ajaxPreviousUrl previousUrl paginatePreviousLabel pageLabel ajaxSelectUrl selectUrl ajaxSelectSizeUrl selectSizeUrl commonDisplaying paginateNextStyle ajaxNextUrl nextUrl paginateNextLabel paginateLastStyle ajaxLastUrl lastUrl paginateLastLabel paginateViewSizeLabel>
<#if listSize gt viewSize>
<div class="${paginateStyle}">&nbsp; <ul>
<li class="${paginateFirstStyle}<#if viewIndex gt 0>"><a href="<#if ajaxEnabled>javascript:ajaxUpdateAreas('${ajaxFirstUrl}')<#else>${firstUrl}</#if>">${paginateFirstLabel}</a><#else>-disabled"><span>${paginateFirstLabel}</span></#if></li>
<li class="${paginatePreviousStyle}<#if viewIndex gt 0>"><a href="<#if ajaxEnabled>javascript:ajaxUpdateAreas('${ajaxPreviousUrl}')<#else>${previousUrl}</#if>">${paginatePreviousLabel}</a><#else>-disabled"><span>${paginatePreviousLabel}</span></#if></li>
<#if listSize gt 0 && javaScriptEnabled><li>${pageLabel}<select name="page" size="1" onchange="<#if ajaxEnabled>javascript:ajaxUpdateAreas('${ajaxSelectUrl}')<#else>location.href='${selectUrl}'+this.value;</#if>"><#rt/>
<#assign x=listSize/viewSize?floor>
<#if listSize gt (viewIndex*viewSize)><#assign x=x+1></#if>
<#list 1..x as i>
<#if i == (viewIndex+1)><option selected="selected" value="<#else><option value="</#if>${i-1}">${i}</option>
</#list>
</select></li></#if>
<li class="${paginateNextStyle}<#if highIndex lt listSize>"><a href="<#if ajaxEnabled>javascript:ajaxUpdateAreas('${ajaxNextUrl}')<#else>${nextUrl}</#if>">${paginateNextLabel}</a><#else>-disabled"><span>${paginateNextLabel}</span></#if></li>
<li class="${paginateLastStyle}<#if highIndex lt listSize>"><a href="<#if ajaxEnabled>javascript:ajaxUpdateAreas('${ajaxLastUrl}')<#else>${lastUrl}</#if>">${paginateLastLabel}</a><#else>-disabled"><span>${paginateLastLabel}</span></#if></li>
<#if javaScriptEnabled><li class="nav-pagesize"><select name="pageSize" size="1" onchange="<#if ajaxEnabled>javascript:ajaxUpdateAreas('${ajaxSelectSizeUrl}')<#else>location.href='${selectSizeUrl}';</#if>"><#rt/>
<#assign availPageSizes = [20, 30, 50, 100, 200]>
<#list availPageSizes as ps>
  <option <#if viewSize == ps>selected="selected" </#if> value="${ps}">${ps}</option>
</#list>
</select> ${paginateViewSizeLabel}</li></#if>
<li class="nav-displaying">${commonDisplaying}</li>
</ul></div><br />
</#if>
</#macro>

<#macro renderFileField className alert name value size maxlength autocomplete><input type="file" <@renderClass className alert /><#if name?has_content> name="${name}"</#if><#if value?has_content> value="${value}"</#if><#if size?has_content> size="${size}"</#if><#if maxlength?has_content> maxlength="${maxlength}"</#if><#if autocomplete?has_content> autocomplete="off"</#if>/><#rt/></#macro>
<#macro renderPasswordField className alert name value size maxlength id autocomplete><input type="password" <@renderClass className alert /><#if name?has_content> name="${name}"</#if><#if value?has_content> value="${value}"</#if><#if size?has_content> size="${size}"</#if><#if maxlength?has_content> maxlength="${maxlength}"</#if><#if id?has_content> id="${id}"</#if><#if autocomplete?has_content> autocomplete="off"</#if>/></#macro>
<#macro renderImageField value description alternate border width height event action><img<#if value?has_content> src="${value}"</#if><#if description?has_content> title="${description}"</#if><#if alternate?has_content> alt="${alternate}"</#if><#if border?has_content> border="${border}"</#if><#if width?has_content> width="${width}"</#if><#if height?has_content> height="${height}"</#if><#if event?has_content> ${rp.event?html}="${action}" </#if>/></#macro>

<#macro renderBanner style leftStyle rightStyle leftText text rightText>
<table width="100%">  <tr><#rt/>
<#if leftText?has_content><td align="left"><#if leftStyle?has_content><div class="${leftStyle}"></#if>${leftText}<#if leftStyle?has_content></div></#if></td><#rt/></#if>
<#if text?has_content><td align="center"><#if style?has_content><div class="${style}"></#if>${text}<#if style?has_content></div></#if></td><#rt/></#if>
<#if rightText?has_content><td align="right"><#if rightStyle?has_content><div class="${rightStyle}"></#if>${rightText}<#if rightStyle?has_content></div></#if></td><#rt/></#if>
</tr> </table>
</#macro>

<#macro renderContainerField id><div id="${id?if_exists}"/></#macro>

<#macro renderFieldGroupOpen style id title collapsed collapsibleAreaId collapsible expandToolTip collapseToolTip>
<#if style?has_content || id?has_content || title?has_content><div class="fieldgroup<#if style?has_content> ${style}</#if>"<#if id?has_content> id="${id}"</#if>>
<div class="fieldgroup-title-bar">
 <#if collapsible><ul><li class="<#if collapsed>collapsed"><a onclick="javascript:toggleCollapsiblePanel(this, '${collapsibleAreaId}', '${expandToolTip}', '${collapseToolTip}');">
   <#else>expanded"><a onclick="javascript:toggleCollapsiblePanel(this, '${collapsibleAreaId}', '${expandToolTip}', '${collapseToolTip}');"></#if>
     &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<#if title?has_content>${title}</#if></a></li></ul>
 <#else><#if title?has_content>${title}</#if></#if><#rt/>
</div>
<div id="${collapsibleAreaId}" class="fieldgroup-body" <#if collapsed && collapsible> style="display: none;"</#if>>
</#if>
</#macro>

<#macro renderFieldGroupClose style id title><#if style?has_content || id?has_content || title?has_content></div></div></#if></#macro>

<#macro renderHyperlinkTitle name title showSelectAll="N"><#if title?has_content>${title}<br /></#if><#if showSelectAll="Y"><input type="checkbox" name="selectAll" value="Y" onclick="javascript:toggleAll(this, '${name}');"/></#if></#macro>
<#macro renderSortField style title linkUrl ajaxEnabled><a<#if style?has_content> class="${style}"</#if> href="<#if ajaxEnabled?has_content && ajaxEnabled>javascript:ajaxUpdateAreas('${linkUrl}')<#else>${linkUrl}</#if>">${title}</a></#macro>
<#macro formatBoundaryComment boundaryType widgetType widgetName><!-- ${boundaryType}  ${widgetType}  ${widgetName} --></#macro>

<#macro renderTooltip tooltip tooltipStyle><#if tooltip?has_content><span class="<#if tooltipStyle?has_content>${tooltipStyle}<#else>tooltip</#if>">${tooltip}</span><#rt/></#if></#macro>

<#macro renderClass className="" alert="">
<#if className?has_content || (alert?has_content && alert=="true")> class="${className}<#if alert?has_content && alert=="true"> alert</#if>" </#if>
</#macro>

<#macro renderAsterisks requiredField requiredStyle>
<#if requiredField=="true"><#if !requiredStyle?has_content>*</#if></#if>
</#macro>

<#macro makeHiddenFormLinkForm actionUrl name parameters targetWindow><form method="post" action="${actionUrl}" <#if targetWindow?has_content>target="${targetWindow}"</#if> onsubmit="javascript:submitFormDisableSubmits(this)" name="${name}"><#list parameters as parameter><input name="${parameter.name}" value="${parameter.value}" type="hidden"/></#list></form></#macro>
<#macro makeHiddenFormLinkAnchor linkStyle hiddenFormName event action imgSrc description confirmation><a <#if linkStyle?has_content>class="${linkStyle}"</#if> href="javascript:document.${hiddenFormName}.submit()"<#if action?has_content && event?has_content> ${event}="${action}"</#if><#if confirmation?has_content> onclick="return confirm('${confirmation?js_string}')"</#if>><#if imgSrc?has_content><img src="${imgSrc}"/></#if>${description}</a></#macro>
<#macro makeHyperlinkString linkStyle hiddenFormName event action imgSrc title alternate linkUrl targetWindow description confirmation><a <#if linkStyle?has_content>class="${linkStyle}"</#if> href="${linkUrl}"<#if targetWindow?has_content> target="${targetWindow}"</#if><#if action?has_content && event?has_content> ${event}="${action}"</#if><#if confirmation?has_content> onclick="return confirm('${confirmation?js_string}')"</#if>><#if imgSrc?has_content><img src="${imgSrc}" alt="${alternate}" title="${title}"/></#if>${description}</a></#macro>
