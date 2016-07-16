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
<script language="JavaScript" type="text/javascript">
    function updateAndSaveLabel() {
        document.UpdateLabelForm.action="<@ofbizUrl>SaveLabelsToXmlFile</@ofbizUrl>";
        document.UpdateLabelForm.submit();
    }
</script>
<div class="screenlet-body">
    <form method="post" action="<@ofbizUrl>SaveLabelsToXmlFile</@ofbizUrl>" name="UpdateLabelForm">
        <table class="basic-table" cellspacing="3">
            <tr>
                <td colspan="2">&nbsp;</td>
            </tr>
            <tr>
                <td class="label">${uiLabelMap.WebtoolsLabelManagerKey}</td>
                <td>
                    <#if parameters.sourceKey??>
                        ${parameters.sourceKey}
                        <input type="hidden" name="key" value="${parameters.sourceKey}" />
                        <input type="hidden" name="update_label" value="Y" />
                    <#else>
                        <input type="text" name="key" size="70" />
                        <input type="hidden" name="update_label" value="N" />
                    </#if>
                </td>
            </tr>
            <tr>
                <td class="label">${uiLabelMap.WebtoolsLabelManagerKeyComment}</td>
                <td>
                    <input type="text" name="keyComment" size="70" value="${parameters.sourceKeyComment!}" />
                </td>
            </tr>
            <tr>
                <td class="label">${uiLabelMap.WebtoolsLabelManagerFileName}</td>
                <td>
                    <#if parameters.sourceFileName??>
                        ${parameters.sourceFileName}
                        <input type="hidden" name="fileName" value="${parameters.sourceFileName}" />
                    <#else>
                        <select name="fileName">
                            <#list filesFound as fileInfo>
                              <#assign fileName = fileInfo.file.getName()/>
                              <option <#if parameters.fileName?? && parameters.fileName == fileName>selected="selected"</#if> value="${fileName}">${fileName}</option>
                            </#list>
                        </select>
                    </#if>
                </td>
            </tr>
            <tr>
                <td colspan="2" align="center">
                    <input type="submit" value="${uiLabelMap.CommonBack}"/>
                    <#if parameters.sourceKey??>
                        <input type="submit" value="${uiLabelMap.WebtoolsLabelManagerRemove}" name="removeLabel"/>
                        <input type="submit" value="${uiLabelMap.CommonUpdate}" name="confirm" onclick="javascript:updateAndSaveLabel()"/>
                    <#else>
                        <input type="submit" value="${uiLabelMap.CommonAdd}" name="confirm"/>
                        <input type="submit" value="${uiLabelMap.WebtoolsLabelManagerUpdateAndSave}" name="confirm" onclick="javascript:updateAndSaveLabel()"/>
                    </#if>
                </td>
            </tr>
            <#list localesFound as localeFound>
                <#assign labelValue = "">
                <#assign labelComment = "">
                <#if parameters.sourceKey??>
                    <#assign value = (label.getLabelValue(localeFound))!>
                    <#if value?has_content>
                        <#assign labelValue = value.getLabelValue()>
                        <#assign labelComment = value.getLabelComment()>
                    </#if>
                </#if>
                <#assign showLocale = true>
                <#if parameters.labelLocaleName?? && parameters.labelLocaleName != "" && parameters.labelLocaleName != localeFound>
                    <#assign showLocale = false>
                </#if>
                <#if showLocale == true>
                    <tr>
                        <#assign locale = Static["org.apache.ofbiz.base.util.UtilMisc"].parseLocale(localeFound)!/>
                        <#if locale?? && locale?has_content>
                            <#assign langAttr = localeFound.toString()?replace("_", "-")>
                            <#assign langDir = "ltr">
                            <#if "ar.iw"?contains(langAttr?substring(0, 2))>
                                <#assign langDir = "rtl">
                            </#if>
                            <td lang="${langAttr}" dir="${langDir}" class="label">
                                ${locale.getDisplayName(locale)}
                            </td>
                        <#else>
                            <td class="label">${localeFound}</td>
                        </#if>
                        <td>
                            <input type="hidden" name="localeNames" value="${localeFound}" />
                            <input type="text" name="localeValues" size="70" value="${labelValue!}" />
                            <input type="text" name="localeComments" size="70" value="${labelComment!}" />
                        </td>
                    </tr>
                </#if>
            </#list>
            <tr>
                <td colspan="2" align="center">
                    <input type="submit" value="${uiLabelMap.CommonBack}"/>
                    <#if parameters.sourceKey??>
                        <input type="submit" value="${uiLabelMap.WebtoolsLabelManagerRemove}" name="removeLabel"/>
                        <input type="submit" value="${uiLabelMap.CommonUpdate}" name="confirm" onclick="javascript:updateAndSaveLabel()"/>
                    <#else>
                        <input type="submit" value="${uiLabelMap.CommonAdd}" name="confirm"/>
                        <input type="submit" value="${uiLabelMap.WebtoolsLabelManagerUpdateAndSave}" name="confirm" onclick="javascript:updateAndSaveLabel()"/>
                    </#if>
                </td>
            </tr>
        </table>
    </form>
</div>
