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

<#macro renderField text><#if text??>"${text}"</#if></#macro>

<#macro renderDisplayField type imageLocation idName description title class alert inPlaceEditorUrl="" inPlaceEditorParams="">
<@renderField description />
</#macro>
<#macro renderHyperlinkField></#macro>

<#macro renderTextField name className alert value textSize maxlength id event action disabled clientAutocomplete ajaxUrl ajaxEnabled mask tabindex readonly placeholder="" delegatorName="default"><@renderField value /></#macro>

<#macro renderTextareaField name className alert cols="" rows="" maxlength="" id="" readonly="" value="" visualEditorEnable="" buttons="" tabindex="" language="" disabled=false placeholder=""><@renderField value /></#macro>

<#macro renderDateTimeField name className alert title value size maxlength id shortDateInput timeDropdownParamName defaultDateTimeString localizedIconTitle timeDropdown timeHourName classString hour1 hour2 timeMinutesName minutes isTwelveHour ampmName amSelected pmSelected compositeType formName mask="" event="" action="" step="" timeValues="" tabindex="" disabled="" isXMLHttpRequest=""><@renderField value /></#macro>

<#macro renderDropDownField name className id formName explicitDescription options ajaxEnabled
        otherFieldName="" otherValue="" otherFieldSize=""
        alert="" conditionGroup="" tabindex="" multiple=false event="" size="" placeCurrentValueAsFirstOption=false
        currentValue="" allowEmpty=false dDFCurrent="" noCurrentSelectedKey="" disabled=false action="">
<@renderField explicitDescription />
</#macro>

<#macro renderTooltip tooltip tooltipStyle></#macro>
<#macro renderCheckField items className alert id currentValue name event action conditionGroup tabindex disabled allChecked=""></#macro>
<#macro renderRadioField items className alert currentValue noCurrentSelectedKey name event action conditionGroup tabindex disabled></#macro>

<#macro renderSubmitField buttonType className alert formName action imgSrc ajaxUrl id title="" name="" event="" confirmation="" containerId="" tabindex="" disabled=false closeOnSubmit="true"></#macro>
<#macro renderResetField className alert name title></#macro>

<#macro renderHiddenField name conditionGroup="" value="" id="" event="" action="" disabled=false></#macro>
<#macro renderIgnoredField></#macro>

<#macro renderFieldTitle style title id fieldHelpText="" for=""><@renderField title /></#macro>
<#macro renderEmptyFormDataMessage message></#macro>
<#macro renderSingleFormFieldTitle></#macro>

<#macro renderFormOpen linkUrl formType targetWindow containerId containerStyle autocomplete name viewIndexField viewSizeField viewIndex viewSize useRowSubmit focusFieldName hasRequiredField csrfNameValue></#macro>
<#macro renderFormClose></#macro>
<#macro renderMultiFormClose></#macro>

<#macro renderFormatListWrapperOpen formName style columnStyles></#macro>
<#macro renderFormatListWrapperClose formName></#macro>

<#macro renderFormatHeaderOpen></#macro>
<#macro renderFormatHeaderClose></#macro>
<#macro renderFormatHeaderRowOpen style></#macro>
<#macro renderFormatHeaderRowClose>

</#macro>
<#macro renderFormatHeaderRowCellOpen style positionSpan></#macro>
<#macro renderFormatHeaderRowCellClose></#macro>

<#macro renderFormatHeaderRowFormCellOpen style> </#macro>
<#macro renderFormatHeaderRowFormCellClose></#macro>
<#macro renderFormatHeaderRowFormCellTitleSeparator style isLast></#macro>

<#macro renderFormatItemRowOpen formName itemIndex altRowStyles evenRowStyle oddRowStyle></#macro>
<#macro renderFormatItemRowClose formName>

</#macro>
<#macro renderFormatItemRowCellOpen fieldName style positionSpan></#macro>
<#macro renderFormatItemRowCellClose fieldName></#macro>
<#macro renderFormatItemRowFormCellOpen style></#macro>
<#macro renderFormatItemRowFormCellClose></#macro>

<#macro renderFormatSingleWrapperOpen formName style></#macro>
<#macro renderFormatSingleWrapperClose formName>
</#macro>

<#macro renderFormatFieldRowOpen></#macro>
<#macro renderFormatFieldRowClose>
</#macro>
<#macro renderFormatFieldRowTitleCellOpen style> </#macro>
<#macro renderFormatFieldRowTitleCellClose></#macro>
<#macro renderFormatFieldRowSpacerCell></#macro>
<#macro renderFormatFieldRowWidgetCellOpen positionSpan style></#macro>
<#macro renderFormatFieldRowWidgetCellClose></#macro>

<#macro renderFormatEmptySpace>&nbsp;</#macro>

<#macro renderTextFindField name value defaultOption opEquals opBeginsWith opContains opIsEmpty opNotEqual className alert size maxlength autocomplete titleStyle hideIgnoreCase ignCase ignoreCase conditionGroup tabindex><@renderField value /></#macro>

<#macro renderDateFindField id name formName defaultOptionFrom defaultOptionThru opEquals opSameDay opGreaterThanFromDayStart opGreaterThan opGreaterThan opLessThan opUpToDay opUpThruDay opIsEmpty className="" alert=false imgSrc="" value="" isTimeType=false isDateType=false conditionGroup="" localizedInputTitle="" value2="" size="" maxlength="" titleStyle="" tabindex="" disabled=false><@renderField value /></#macro>

<#macro renderRangeFindField className alert name value size maxlength autocomplete titleStyle defaultOptionFrom opEquals opGreaterThan opGreaterThanEquals opLessThan opLessThanEquals value2 defaultOptionThru conditionGroup tabindex>
<@renderField value />
</#macro>

<#macro renderLookupField name formName fieldFormName conditionGroup className="" alert="false" value="" size="" maxlength="" id="" event="" action="" readonly=false autocomplete="" descriptionFieldName="" targetParameterIter="" imgSrc="" ajaxUrl="" ajaxEnabled=javaScriptEnabled presentation="layer" width="" height="" position="" fadeBackground="true" clearText="" showDescription="" initiallyCollapsed="" lastViewName="main" tabindex="" delegatorName="default">><@renderField value /></#macro>
<#macro renderNextPrev paginateStyle paginateFirstStyle viewIndex highIndex listSize viewSize ajaxEnabled javaScriptEnabled ajaxFirstUrl firstUrl paginateFirstLabel paginatePreviousStyle ajaxPreviousUrl previousUrl paginatePreviousLabel pageLabel ajaxSelectUrl selectUrl ajaxSelectSizeUrl selectSizeUrl commonDisplaying paginateNextStyle ajaxNextUrl nextUrl paginateNextLabel paginateLastStyle ajaxLastUrl lastUrl paginateLastLabel paginateViewSizeLabel></#macro>
<#macro renderFileField className alert name value size maxlength autocomplete tabindex><@renderField value /></#macro>
<#macro renderPasswordField className alert name value size maxlength id autocomplete tabindex></#macro>
<#macro renderImageField value description alternate style event action></#macro>
<#macro renderBanner style leftStyle rightStyle leftText text rightText></#macro>
<#macro renderContainerField id className></#macro>
<#macro renderFieldGroupOpen style id title collapsed collapsibleAreaId collapsible expandToolTip collapseToolTip></#macro>
<#macro renderFieldGroupClose style id title></#macro>

<#macro renderHyperlinkTitle name title showSelectAll="N"></#macro>
<#macro renderSortField style title linkUrl ajaxEnabled tooltip=""><@renderFieldTitle style title /></#macro>
<#macro formatBoundaryComment boundaryType widgetType widgetName></#macro>
<#macro renderAsterisks requiredField requiredStyle>*</#macro>
<#macro makeHiddenFormLinkForm actionUrl name parameters targetWindow></#macro>
<#macro makeHiddenFormLinkAnchor linkStyle hiddenFormName event action imgSrc description confirmation><@renderField description /></#macro>
<#macro makeHyperlinkString linkStyle hiddenFormName event action imgSrc title targetParameters alternate linkUrl targetWindow description confirmation uniqueItemName="" height="" width="" id=""><@renderField description /></#macro>
