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
<#assign docLangAttr = locale.toString()?replace("_", "-")>
<#assign langDir = "ltr">
<#if "ar.iw"?contains(docLangAttr?substring(0, 2))>
    <#assign langDir = "rtl">
</#if>
<#assign forstatic = false/>
<#if "true" == (parameters.forstatic)?default("false")>
  <#assign forstatic = true/>
</#if>
<html lang="${docLangAttr}" dir="${langDir}" xmlns="http://www.w3.org/1999/xhtml">
    <head>
        <title>${uiLabelMap.WebtoolsEntityReference}</title>
        <style type="text/css">
           body, textarea, input, select {font-family: Helvetica, sans-serif; background-color: #ffffff;}
          .packagetext {font-size: 18pt; font-weight: bold; text-align: center}
          .toptext {font-size: 16pt; font-weight: bold; text-align: center}
          .titletext {font-size: 12pt; font-weight: bold; color: blue;}
          .headertext {font-size: 8pt; font-weight: bold; background-color: blue; color: white;}
          .enametext {font-size: 8pt; font-weight: bold;}
          .entityheader {font-size: 8pt; font-weight: bold; background-color: #cccccc; text-align: center}
          .entityheader a {font-weight: normal;}
          .entityheader a:hover {color:red;}
          .entitytext {font-size: 8pt; background-color: #efffff;}
          .relationtext {font-size: 8pt; background-color: #feeeee;}
          .relationtext a {font-weight: bold; color: blue;}
          .relationtext a:hover {color:red;}
          .pktext {color:red;}
          .descriptiontext {font-size: 8pt;}
        </style>
    </head>
    <body>
        <div class='toptext'>${uiLabelMap.WebtoolsEntityReferenceChart}<br />
            ${numberOfEntities} ${uiLabelMap.WebtoolsTotalEntities}
        </div>
        <#assign numberShowed = 0>
        <#list packagesList as package>
            <hr /><div id='${package.packageName}' class='packagetext'>${package.packageName}</div><hr />
            <#list package.entitiesList as entity>
                <table width="95%" border="1" cellpadding='2' cellspacing='0'>
                    <tr class='entityheader'>
                        <td colspan="5">
                            <div id='${entity.entityName}' class="titletext">
                                ${uiLabelMap.WebtoolsEntity}: ${entity.entityName}
                                <#if entity.plainTableName?has_content>  | ${uiLabelMap.WebtoolsTable}: ${entity.plainTableName}</#if>
                            </div>
                            <div>${entity.title}&nbsp;
                                <#if !forstatic>
                                    <a target='main' href="<@ofbizUrl>entity/find/${entity.entityName}?noConditionFind=Y</@ofbizUrl>">[${uiLabelMap.WebtoolsViewData}]</a>
                                </#if>
                                <#if !forstatic>
                                    <a target='main' href="<@ofbizUrl>ArtifactInfo?name=${entity.entityName}&amp;type=entity</@ofbizUrl>">[${uiLabelMap.WebtoolsArtifactInfo}]</a>
                                </#if>
                            </div>
                            <#if entity.description?has_content &&
                                 !entity.description.equalsIgnoreCase("NONE") &&
                                !entity.description.equalsIgnoreCase("")>
                                   <div>${entity.description}</div>
                            </#if>
                            <#if entity.location?has_content>
                                <div class='description'>${entity.location}</div>
                            </#if>
                        </td>
                    </tr>
                    <tr class='headertext'>
                        <th width="30%">${uiLabelMap.WebtoolsJavaName}</th>
                        <th width="30%">${uiLabelMap.WebtoolsDbName}</th>
                        <th width="10%">${uiLabelMap.WebtoolsFieldType}</th>
                        <th width="15%">${uiLabelMap.WebtoolsJavaType}</th>
                        <th width="15%" nowrap="nowrap">${uiLabelMap.WebtoolsSqlType}</th>
                    </tr>
                    <#list entity.javaNameList as javaName>
                        <tr class='entitytext'>
                            <td>
                              <div class='enametext<#if javaName.isPk> pktext</#if>'>${javaName.name}</div>
                              <#if javaName.description?has_content>
                                <div class="descriptiontext">${javaName.description}</div>
                              </#if>
                              <#if javaName.encrypted>
                                <div class="descriptiontext">[${uiLabelMap.WebtoolsEncrypted}]</div>
                              </#if>
                            </td>
                            <td>${javaName.colName}</td>
                            <td>${javaName.type}</td>
                            <#if javaName.javaType?has_content>
                                <td>${javaName.javaType}</td>
                                <td>${javaName.sqlType}</td>
                            <#else>
                                <td>${uiLabelMap.WebtoolsNotFound}</td>
                                <td>${uiLabelMap.WebtoolsNotFound}</td>
                            </#if>
                        </tr>
                    </#list>
                    <#if entity.relationsList?has_content>
                        <tr class='entityheader'>
                            <td colspan="5"><hr /></td>
                        </tr>
                        <tr class='headertext'>
                            <th>${uiLabelMap.WebtoolsRelation}</th>
                            <th colspan='4'>${uiLabelMap.WebtoolsRelationType}</th>
                        </tr>
                        <#list entity.relationsList as relation>
                            <tr class='relationtext'>
                                <td>
                                    <#if relation.title?has_content><b>${relation.title}</b> </#if><a href='#${relation.relEntity}'>${relation.relEntity}</a>
                                    <#if relation.fkName?has_content>
                                        <br />${uiLabelMap.WebtoolsFKName}: ${relation.fkName}
                                    </#if>
                                    <#if relation.description?has_content>
                                        <br /><span class='descriptiontext'>${relation.description}</span>
                                    </#if>
                                </td>
                                <td width="60%" colspan='4'>
                                    ${relation.type}:
                                    <#if relation.length == 3>
                                        &nbsp;
                                    </#if>
                                    <#if relation.keysList?has_content>
                                        <#list relation.keysList as keyList>
                                            <br />&nbsp;&nbsp;${keyList.row})&nbsp;
                                              <#if keyList.fieldName == keyList.relFieldName>
                                                  ${keyList.fieldName}
                                              <#else>
                                                  ${keyList.fieldName} : ${keyList.relFieldName}
                                              </#if>
                                        </#list>
                                    </#if>
                                </td>
                            </tr>
                        </#list>
                    </#if>
                    <#if entity.indexList?has_content>
                        <tr class='entityheader'>
                            <td colspan="5"><hr /></td>
                        </tr>
                        <tr class='headertext'>
                            <th>${uiLabelMap.WebtoolsIndexName}</th>
                            <th colspan='4'>${uiLabelMap.WebtoolsIndexFieldList}</th>
                        </tr>
                        <#list entity.indexList as index>
                            <tr class='relationtext'>
                                <td>${index.name}</td>
                                <td width="60%" colspan='4'>
                                    <#list index.fieldNameList as fieldName>
                                        ${fieldName}
                                    </#list>
                                </td>
                            </tr>
                        </#list>
                    </#if>
                </table>
                <br />
                <#assign numberShowed = numberShowed + 1>
            </#list>
        </#list>
        <div align="center">
            <br /><br />
              ${uiLabelMap.WebtoolsEntityDisplayed}: ${numberShowed}
        </div>
    </body>
</html>
