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


<#-- Main Heading -->
<table width='100%' cellpadding='0' cellspacing='0' border='0'>
  <tr>
    <td>
      <h1>${contentId!}
      </h1>
    </td>
    <td align="right">
    </td>
  </tr>
</table>
<br />


<#if currentValue?has_content>
    <@renderTextData content=currentValue textData=textData! />
</#if>

<br />
<table border="0" width='100%' cellspacing='0' cellpadding='0' class='boxoutside'>
  <tr>
    <td width='100%'>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxtop'>
        <tr>
          <td valign="middle">
            <div class="boxhead">&nbsp; Links </div>
          </td>
        </tr>
      </table>
    </td>
  </tr>
  <tr>
    <td width='100%'>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxbottom'>
        <tr>
          <td>
            <form mode="post" name="publishsite" action="<@ofbizUrl>linkContentToPubPt</@ofbizUrl>">
              <input type="hidden" name="contentId" value="${contentId}"/>
              <table width="100%" border="0" cellpadding="1">
                    <#assign rowCount = 0 />
                    <#assign rootForumId=rootForumId />
                    <@publishContent forumId=rootForumId contentId=contentId />
                    <#assign rootForumId2=rootForumId2 />
                    <@publishContent forumId=rootForumId2 contentId=contentId />
                    <tr>
                      <td colspan="1">
                          <input type="submit" name="submitBtn" value="Publish"/>
                      </td>
                    </tr>
              </table>
              <input type="hidden" name="_rowCount" value="${rowCount}"/>
            </form>
          </td>
        </tr>

      </table>
    </td>
  </tr>
</table>

<br />
<table border="0" width='100%' cellspacing='0' cellpadding='0' class='boxoutside'>
  <tr>
    <td width='100%'>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxtop'>
        <tr>
          <td valign="middle">
            <div class="boxhead">&nbsp; Features </div>
          </td>
        </tr>
      </table>
    </td>
  </tr>
  <tr>
    <td width='100%'>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxbottom'>
        <tr>
          <td>
            <form mode="post" name="updatefeatures" action="<@ofbizUrl>updateFeatures</@ofbizUrl>">
              <input type="hidden" name="contentId" value="${contentId}"/>
              <table width="100%" border="0" cellpadding="1">
                       <tr>
                          <td class="">Product Feature</td>
                          <td class="">Has Feature</td>
                    <#assign rowCount = 0 />
                    <#list featureList as feature>
                       <#assign checked=""/>
                       <#if feature.action?has_content && feature.action == "Y">
                           <#assign checked="checked='checked'"/>
                       </#if>
                       <tr>
                          <td class="">[${feature.productFeatureId}] - ${feature.description}</td>
                          <td class=""><input type="checkbox" name="action_o_${rowCount}" value="Y" ${checked}/></td>
                          <input type="hidden" name="fieldName0_o_${rowCount}" value="productFeatureId"/>
                          <input type="hidden" name="fieldValue0_o_${rowCount}" value="${feature.productFeatureId}"/>
                          <input type="hidden" name="fieldName1_o_${rowCount}" value="dataResourceId"/>
                          <input type="hidden" name="fieldValue1_o_${rowCount}" value="${feature.dataResourceId}"/>
                          <input type="hidden" name="entityName_o_${rowCount}" value="ProductFeatureDataResource"/>
                          <input type="hidden" name="pkFieldCount_o_${rowCount}" value="2"/>
                       </tr>
                       <#assign rowCount=rowCount + 1/>
                    </#list>
                    <tr>
                      <td valign="middle">
                        <div class="boxhead">
                          <@htmlTemplate.lookupField formName="updatefeatures" name="fieldValue0_o_${rowCount}" id="fieldValue0_o_${rowCount}" fieldFormName="LookupProductFeature"/>
                        </div>
                      </td>
                          <input type="hidden" name="fieldName0_o_${rowCount}" value="productFeatureId"/>
                          <input type="hidden" name="fieldValue0_o_${rowCount}" value=""/>
                          <input type="hidden" name="fieldName1_o_${rowCount}" value="dataResourceId"/>
                          <input type="hidden" name="fieldValue1_o_${rowCount}" value="${dataResourceId}"/>
                          <input type="hidden" name="entityName_o_${rowCount}" value="ProductFeatureDataResource"/>
                          <input type="hidden" name="pkFieldCount_o_${rowCount}" value="2"/>
                          <#assign rowCount=rowCount + 1/>
                    </tr>
                    <tr>
                      <td colspan="1">
                          <input type="submit" name="submitBtn" value="${uiLabelMap.CommonUpdate}"/>
                      </td>
                    </tr>
              </table>
              <input type="hidden" name="_rowCount" value="${rowCount}"/>
            </form>
          </td>
        </tr>

      </table>
    </td>
  </tr>
</table>

<#macro renderTextData content textData >
    <#assign contentId=content.contentId!/>
<table border="0" width='100%' cellspacing='0' cellpadding='0' class='boxoutside'>
  <tr>
    <td width='100%'>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxtop'>
        <tr>
          <td valign="middle">
            <div class="boxhead">&nbsp;</div>
          </td>
          <td valign="middle" align="right">
            <a href="<@ofbizUrl>EditAddContent?contentId=${content.contentId!}&amp;contentIdTo=${content.caContentIdTo!}&amp;contentAssocTypeId=${content.caContentAssocTypeId!}&amp;fromDate=${content.caFromDate!}&amp;mapKey=${content.caMapKey!}</@ofbizUrl>" class="submenutextright">Update</a>
          </td>
        </tr>
      </table>
    </td>
  </tr>
  <tr>
    <td width='100%'>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxbottom'>
        <tr>
          <td>
  <table width="100%" border="0" cellpadding="0" cellspacing='0'>
    <tr><td align="right" nowrap="nowrap"><div class='tabletext'><b>Content Name</b></div></td><td>&nbsp;</td><td><div class='tabletext'>${content.contentName!}</div></td></tr>
    <tr><td align="right" nowrap="nowrap"><div class='tabletext'><b>Description</b></div></td><td>&nbsp;</td><td><div class='tabletext'>${content.description!}<div></td></tr>
  </table>
          </td>
        </tr>
      </table>
    </td>
  </tr>
  <tr>
    <td width='100%'>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxbottom'>
        <tr>
          <td>
  <table width="100%" border="0" cellpadding="0" cellspacing='0'>
    <tr><td align="right" nowrap="nowrap"><div class='tabletext'><b></b></div></td><td>&nbsp;</td><td><div class='tabletext'>
<#-- ${textData!} -->
<@renderContentAsText subContentId=content.contentId  editRequestName="/EditAddContent"/>
<div></td></tr>
  </table>
          </td>
        </tr>
      </table>
    </td>
  </tr>
</table>
</#macro>

<#macro publishContent forumId contentId formAction="/updatePublishLinksMulti"  indentIndex=0 catTrail=[]>

<#local thisContentId=catTrail[indentIndex]!/>

<#assign viewIdx = "" />
<#if requestParameters.viewIndex?has_content>
<#assign viewIdx = requestParameters.viewIndex!?number />
</#if>
<#assign viewSz = "" />
<#if requestParameters.viewSize?has_content>
<#assign viewSz = requestParameters.viewSize!?number />
</#if>

<#local indent = "">
<#local thisCatTrailCsv = "" />
<#local listUpper = (indentIndex - 1) />
<#if catTrail?size < listUpper >
    <#local listUpper = (catTrail?size - 1)>
</#if>
<#if 0 < listUpper >
  <#list 0..listUpper as idx>
      <#if thisCatTrailCsv?has_content>
          <#local thisCatTrailCsv = thisCatTrailCsv + ","/>
      </#if>
      <#local thisCatTrailCsv = thisCatTrailCsv + catTrail[idx]>
  </#list>
</#if>
<#if 0 < indentIndex >
  <#list 0..(indentIndex - 1) as idx>
      <#local indent = indent + "&nbsp;&nbsp;&nbsp;&nbsp;">
  </#list>
</#if>


<@loopSubContent contentId=forumId viewIndex=viewIdx viewSize=viewSz contentAssocTypeId="SUBSITE" returnAfterPickWhen="1==1";>
    <#local isPublished = "" />
    <#assign contentAssocViewFrom=Static["org.apache.ofbiz.content.content.ContentWorker"].getContentAssocViewFrom(delegator, subContentId, contentId, "PUBLISH_LINK", null, null)! />
    <#if contentAssocViewFrom?has_content>
        <#local isPublished = "checked='checked'" />
    </#if>
       <tr>
         <td >
            ${indent}
            <#local plusMinus="-"/>
            ${plusMinus} ${content.contentName!}
         </td >
         <td  >
            <input type="checkbox" name="publish_o_${rowCount}" value="Y" ${isPublished}/>
         </td >
            <input type="hidden" name="contentIdTo_o_${rowCount}" value="${subContentId}" />
            <input type="hidden" name="contentId_o_${rowCount}" value="${contentId}" />
            <input type="hidden" name="contentAssocTypeId_o_${rowCount}" value="PUBLISH_LINK" />
            <input type="hidden" name="statusId_o_${rowCount}" value="CTNT_IN_PROGRESS" />
       </tr>
       <#assign rowCount = rowCount + 1 />
       <@publishContent forumId=subContentId contentId=contentId indentIndex=(indentIndex + 1)/>
</@loopSubContent>

</#macro>
