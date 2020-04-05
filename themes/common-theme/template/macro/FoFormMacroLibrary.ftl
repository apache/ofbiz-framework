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
<#macro getFoStyle style>
    <#local foStyles = {
        "listtitlestyle":"font-weight=\"bold\" text-align=\"center\" border=\"solid black\" padding=\"2pt\"",
        "tabletext":"border-left=\"solid black\" border-right=\"solid black\" padding-left=\"2pt\" padding-top=\"2pt\"",
        "tabletextright":"border-left=\"solid black\" border-right=\"solid black\" padding-left=\"2pt\" padding-top=\"2pt\" text-align=\"right\"",
        "tableheadverysmall":"column-width=\"0.3in\"",
        "tableheadsmall":"column-width=\"0.5in\"",
        "tableheadmedium":"column-width=\"1.5in\"",
        "tableheadwide":"column-width=\"3in\"",
        "tableheadhuge":"column-width=\"5in\"",
        "head1":"font-size=\"12\" font-weight=\"bold\"",
        "head2":"font-weight=\"bold\"",
        "head3":"font-weight=\"bold\" font-style=\"italic\"",
        "h1":"font-size=\"12\" font-weight=\"bold\"",
        "h2":"font-weight=\"bold\"",
        "h3":"font-weight=\"bold\" font-style=\"italic\"",
        "alternate-row" : "background-color=\"lightgray\"",
        "error":"color=\"red\""}/>
    <#list style?split(' ') as styleItem>
        <#local foStyle = foStyles[styleItem]?default("")/>
        ${foStyle?default("")}
    </#list>
</#macro>

<#escape x as x?xml>

<#macro makeBlock style="" text=""><fo:block<#if style?has_content> <@getFoStyle style/></#if>><#if text??>${text}</#if></fo:block></#macro>

<#macro renderField text=""><#if text??>${text}</#if></#macro>

<#macro renderDisplayField type imageLocation idName description title class alert inPlaceEditorUrl="" inPlaceEditorParams="">
<@makeBlock class description />
</#macro>
<#macro renderHyperlinkField><!--hyper--></#macro>

<#macro renderTextField name className alert value textSize maxlength id event action disabled clientAutocomplete ajaxUrl ajaxEnabled mask tabindex readonly placeholder="" delegatorName="default"><@makeBlock className value /></#macro>

<#macro renderTextareaField name className alert cols rows maxlength id readonly value visualEditorEnable language buttons tabindex language=""><@makeBlock className value /></#macro>

<#macro renderDateTimeField name className alert title value size maxlength step timeValues id event action dateType shortDateInput timeDropdownParamName defaultDateTimeString localizedIconTitle timeDropdown timeHourName classString hour1 hour2 timeMinutesName minutes isTwelveHour ampmName amSelected pmSelected compositeType formName mask="" event="" action="" step="" timeValues="" tabindex=""><@makeBlock className value /></#macro>

<#macro renderDropDownField name className alert id multiple formName otherFieldName event action size explicitDescription allowEmpty options fieldName otherFieldName otherValue otherFieldSize dDFCurrent ajaxEnabled noCurrentSelectedKey ajaxOptions frequency minChars choices autoSelect partialSearch partialChars ignoreCase fullSearch conditionGroup tabindex firstInList="" currentValue="">
<#if currentValue?has_content && firstInList?has_content>
<@makeBlock "" explicitDescription />
<#else>
<#list options as item>
<@makeBlock "" item.description />
</#list>
</#if>
</#macro>

<#macro renderCheckField items className alert id allChecked currentValue name event action conditionGroup tabindex disabled><@makeBlock "" "" /></#macro>
<#macro renderRadioField items className alert currentValue noCurrentSelectedKey name event action conditionGroup tabindex><@makeBlock "" "" /></#macro>

<#macro renderSubmitField buttonType className alert formName title name event action imgSrc confirmation containerId ajaxUrl tabindex><@makeBlock "" "" /></#macro>
<#macro renderResetField className alert name title><@makeBlock "" "" /></#macro>

<#macro renderHiddenField name conditionGroup value id event action><!--hidden--></#macro>
<#macro renderIgnoredField><!--ignore--></#macro>

<#macro renderFieldTitle style title id fieldHelpText="" for=""><fo:block <@getFoStyle style/>>${title?default("")?replace("&nbsp;", " ")}</fo:block></#macro>
<#macro renderEmptyFormDataMessage message></#macro>
<#macro renderSingleFormFieldTitle><!--title form--></#macro>

<#macro renderFormOpen linkUrl formType targetWindow containerId containerStyle autocomplete name viewIndexField viewSizeField viewIndex viewSize useRowSubmit focusFieldName hasRequiredField></#macro>
<#macro renderFormClose></#macro>
<#macro renderMultiFormClose></#macro>

<#macro renderFormatListWrapperOpen formName style columnStyles><fo:table table-layout="fixed" border="solid black" <@getFoStyle style/>><#list columnStyles as columnStyle><fo:table-column<#if columnStyle?has_content> <@getFoStyle columnStyle/></#if>/></#list></#macro>
<#macro renderFormatListWrapperClose formName><fo:table-row><fo:table-cell><fo:block/></fo:table-cell></fo:table-row></fo:table-body></fo:table></#macro>

<#macro renderFormatHeaderOpen><fo:table-header></#macro>
<#macro renderFormatHeaderClose></fo:table-header><fo:table-body></#macro>

<#macro renderFormatHeaderRowOpen style><fo:table-row></#macro>
<#macro renderFormatHeaderRowClose></fo:table-row>
<#-- FIXME: this is an hack to avoid FOP rendering errors for empty lists (fo:table-body cannot be null) -->
<fo:table-row><fo:table-cell><fo:block/></fo:table-cell></fo:table-row>
</#macro>
<#macro renderFormatHeaderRowCellOpen style="" positionSpan=""><fo:table-cell <#if positionSpan?has_content && positionSpan gt 1 >number-columns-spanned="${positionSpan}"</#if><#if style?has_content><@getFoStyle "${style}"/><#else><@getFoStyle "listtitlestyle"/></#if>><fo:block></#macro>
<#macro renderFormatHeaderRowCellClose></fo:block></fo:table-cell></#macro>

<#macro renderFormatHeaderRowFormCellOpen style></#macro>
<#macro renderFormatHeaderRowFormCellClose></#macro>
<#macro renderFormatHeaderRowFormCellTitleSeparator style isLast></#macro>

<#macro renderFormatItemRowOpen formName itemIndex altRowStyles evenRowStyle oddRowStyle><fo:table-row <@getFoStyle "${altRowStyles}"/>></#macro>
<#macro renderFormatItemRowClose formName></fo:table-row></#macro>
<#macro renderFormatItemRowCellOpen fieldName style="" positionSpan=""><fo:table-cell <#if positionSpan?has_content && positionSpan gt 1 >number-columns-spanned="${positionSpan}"</#if><#if style?has_content><@getFoStyle style/><#else><@getFoStyle "tabletext"/></#if>><fo:block></#macro>
<#macro renderFormatItemRowCellClose fieldName></fo:block></fo:table-cell></#macro>
<#macro renderFormatItemRowFormCellOpen style></#macro>
<#macro renderFormatItemRowFormCellClose></#macro>

<#macro renderFormatSingleWrapperOpen formName style><fo:table><fo:table-column column-width="1.75in"/><fo:table-column column-width="1.75in"/><fo:table-column column-width="1.75in"/><fo:table-column column-width="1.75in"/><fo:table-body></#macro>
<#macro renderFormatSingleWrapperClose formName></fo:table-body></fo:table></#macro>

<#macro renderFormatFieldRowOpen><fo:table-row></#macro>
<#macro renderFormatFieldRowClose></fo:table-row></#macro>
<#macro renderFormatFieldRowTitleCellOpen style><fo:table-cell font-weight="bold" text-align="right" padding="3pt"><fo:block></#macro>
<#macro renderFormatFieldRowTitleCellClose></fo:block></fo:table-cell></#macro>
<#macro renderFormatFieldRowSpacerCell></#macro>
<#macro renderFormatFieldRowWidgetCellOpen positionSpan="" style=""><fo:table-cell text-align="left" padding="2pt" padding-left="5pt" <#if positionSpan?has_content && positionSpan gt 1 >number-columns-spanned="${positionSpan}"</#if>><fo:block></#macro>
<#macro renderFormatFieldRowWidgetCellClose></fo:block></fo:table-cell></#macro>

<#macro renderFormatEmptySpace> <@makeBlock "" " " /><!--space--></#macro>

<#macro renderTextFindField name value defaultOption opEquals opBeginsWith opContains opIsEmpty opNotEqual className alert size maxlength autocomplete titleStyle hideIgnoreCase ignCase ignoreCase conditionGroup tabindex><@makeBlock className value/></#macro>

<#macro renderDateFindField className alert id name localizedInputTitle value value2 size maxlength dateType formName defaultDateTimeString imgSrc localizedIconTitle titleStyle defaultOptionFrom defaultOptionThru opEquals opSameDay opGreaterThanFromDayStart opGreaterThan opGreaterThan opLessThan opUpToDay opUpThruDay opIsEmpty conditionGroup tabindex>
<@makeBlock className value />
</#macro>
<#macro renderRangeFindField className alert name value size maxlength autocomplete titleStyle defaultOptionFrom opEquals opGreaterThan opGreaterThanEquals opLessThan opLessThanEquals value2 defaultOptionThru conditionGroup tabindex>
<@makeBlock className value />
</#macro>

<#macro renderLookupField name formName fieldFormName conditionGroup className="" alert="false" value="" size="" maxlength="" id="" event="" action="" readonly=false autocomplete="" descriptionFieldName="" targetParameterIter="" imgSrc="" ajaxUrl="" ajaxEnabled=javaScriptEnabled presentation="layer" width="" height="" position="" fadeBackground="true" clearText="" showDescription="" initiallyCollapsed="" lastViewName="main" tabindex="" delegatorName="default"></#macro>
<#macro renderNextPrev paginateStyle paginateFirstStyle viewIndex highIndex listSize viewSize ajaxEnabled javaScriptEnabled ajaxFirstUrl firstUrl paginateFirstLabel paginatePreviousStyle ajaxPreviousUrl previousUrl paginatePreviousLabel pageLabel ajaxSelectUrl selectUrl ajaxSelectSizeUrl selectSizeUrl commonDisplaying paginateNextStyle ajaxNextUrl nextUrl paginateNextLabel paginateLastStyle ajaxLastUrl lastUrl paginateLastLabel paginateViewSizeLabel></#macro>
<#macro renderFileField className alert name value size maxlength autocomplete tabindex><@makeBlock className value /></#macro>
<#macro renderPasswordField className alert name value size maxlength id autocomplete tabindex><@makeBlock className "" /></#macro>
<#macro renderImageField value description alternate style event action><@makeBlock "" "" /></#macro>
<#macro renderBanner style leftStyle rightStyle leftText text rightText><@makeBlock "" "" /></#macro>
<#macro renderContainerField id className><@makeBlock className "" /></#macro>
<#macro renderFieldGroupOpen style id title collapsed collapsibleAreaId collapsible expandToolTip collapseToolTip></#macro>
<#macro renderFieldGroupClose style id title></#macro>

<#macro renderHyperlinkTitle name title showSelectAll="N"></#macro>
<#macro renderSortField style title linkUrl ajaxEnabled tooltip=""><@renderFieldTitle style title ""/></#macro>
<#macro formatBoundaryComment boundaryType widgetType widgetName></#macro>
<#macro makeHiddenFormLinkForm actionUrl name parameters targetWindow></#macro>
<#macro makeHiddenFormLinkAnchor linkStyle hiddenFormName event action imgSrc description><@renderField description /></#macro>
<#macro makeHyperlinkString linkStyle hiddenFormName event action imgSrc title targetParameters alternate linkUrl targetWindow description confirmation uniqueItemName="" height="" width="" id=""><@makeBlock linkStyle description /></#macro>
<#macro renderTooltip tooltip tooltipStyle></#macro>
<#macro renderAsterisks requiredField requiredStyle></#macro>
</#escape>
