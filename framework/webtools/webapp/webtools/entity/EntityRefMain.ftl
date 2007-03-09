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
<html>
    <head>
        <title>${uiLabelMap.WebtoolsEntityReference}</title>
        <style>
          .packagetext {font-family: Helvetica,sans-serif; font-size: 18pt; font-weight: bold; text-decoration: none; color: black;}
          .toptext {font-family: Helvetica,sans-serif; font-size: 16pt; font-weight: bold; text-decoration: none; color: black;}
          .titletext {font-family: Helvetica,sans-serif; font-size: 12pt; font-weight: bold; text-decoration: none; color: blue;}
          .headertext {font-family: Helvetica,sans-serif; font-size: 8pt; font-weight: bold; text-decoration: none; background-color: blue; color: white;}
          .enametext {font-family: Helvetica,sans-serif; font-size: 8pt; font-weight: bold; text-decoration: none; color: black;}
          .entitytext {font-family: Helvetica,sans-serif; font-size: 8pt; text-decoration: none; color: black;}
          .relationtext {font-family: Helvetica,sans-serif; font-size: 8pt; text-decoration: none; color: black;}
          A.rlinktext {font-family: Helvetica,sans-serif; font-size: 8pt; font-weight: bold; text-decoration: none; color: blue;}
          A.rlinktext:hover {color:red;}
        </style>
    </head>
    <body bgcolor="#FFFFFF">
        <div align="center">
        <div class='toptext'>${uiLabelMap.WebtoolsEntityReferenceChart}<br/>
            ${numberOfEntities} ${uiLabelMap.WebtoolsTotalEntities}
        </div>
        <#assign numberShowed = 0>
        <#list packagesList as package>
            <a name='${package.packageName}'></a><hr/>
            <div class='packagetext'>${package.packageName}</div><hr/>        
            <#list package.entitiesList as entity>
                <a name="${entity.entityName}"></a>
                <table width="95%" border="1" cellpadding='2' cellspacing='0'>
                    <tr bgcolor="#CCCCCC"> 
                        <td colspan="5"> 
                            <div align="center" class="titletext">${uiLabelMap.WebtoolsEntity}: ${entity.entityName} | ${uiLabelMap.WebtoolsTable}: ${entity.plainTableName?if_exists}</div>
                            <div align="center" class="entitytext"><b>${entity.title}</b>&nbsp;
                                <#if !forstatic>
                                    <#assign encodeURL = response.encodeURL(controlPath + "/FindGeneric?entityName=" + entity.entityName + "&find=true&VIEW_SIZE=50&VIEW_INDEX=0")>
                                    <a target='main' href="${encodeURL}">[${uiLabelMap.WebtoolsViewData}]</a>
                                </#if>
                            </div>
                            <#if entity.description?has_content && 
                                 !entity.description.equalsIgnoreCase("NONE") && 
                                !entity.description.equalsIgnoreCase("")>
                                   <div align="center" class="entitytext">${entity.description}</div>
                            </#if>
                        </td>
                    </tr>
                    <tr class='headertext'>
                        <td width="30%" align="center">${uiLabelMap.WebtoolsJavaName}</td>
                        <td width="30%" align="center">${uiLabelMap.WebtoolsDbName}</td>
                        <td width="10%" align="center">${uiLabelMap.WebtoolsFieldType}</td>
                        <td width="15%" align="center">${uiLabelMap.WebtoolsJavaType}</td>
                        <td width="15%" align="center" nowrap>${uiLabelMap.WebtoolsSqlType}</td>
                    </tr>
                    <#list entity.javaNameList as javaName>
                        <tr bgcolor="#EFFFFF">
                            <td><div align="left" class='enametext'>${javaName.name}</div></td>
                            <td><div align="left" class='entitytext'>${javaName.colName}</div></td>
                            <td><div align="left" class='entitytext'>${javaName.type}</div></td>
                            <#if javaName.javaType?has_content>
                                <td><div align="left" class='entitytext'>${javaName.javaType}</div></td>
                                <td><div align="left" class='entitytext'>${javaName.sqlType}</div></td>
                            <#else>
                                <td><div align="left" class='entitytext'>${uiLabelMap.WebtoolsNotFound}</div></td>
                                <td><div align="left" class='entitytext'>${uiLabelMap.WebtoolsNotFound}</div></td>
                            </#if>
                        </tr>
                    </#list>
                    <tr bgcolor="#FFCCCC">
                        <td colspan="5"><hr/></td>
                    </tr>
                    <tr class='headertext'> 
                        <td align="center">${uiLabelMap.WebtoolsRelation}</td>
                        <td align="center" colspan='4'>${uiLabelMap.WebtoolsRelationType}</td>                            
                    </tr>
                    <#if entity.relationsList?has_content>
                        <#list entity.relationsList as relation>
                            <tr bgcolor="#FEEEEE"> 
                                <td> 
                                    <div align="left" class='relationtext'>
                                        <b>${relation.title}</b><A href='#${relation.relEntity}' class='rlinktext'>${relation.relEntity}</A>
                                    </div>
                                    <#if relation.fkName?has_content>
                                        <div class='relationtext'>${uiLabelMap.WebtoolsFKName}: ${relation.fkName}</div>
                                    </#if>
                                </td>
                                <td width="60%" colspan='4'>
                                    <div align="left" class='relationtext'>
                                        ${relation.type}:
                                        <#if relation.length == 3>
                                            &nbsp;
                                        </#if>
                                        <#if relation.keysList?has_content>
                                        <#list relation.keysList as keyList>                                        
                                            <br/>&nbsp;&nbsp;${keyList.row})&nbsp;
                                              <#if keyList.fieldName == keyList.relFieldName>
                                                  ${keyList.fieldName}
                                              <#else>
                                                  ${keyList.fieldName} : ${keyList.relFieldName}
                                              </#if>
                                        </#list>
                                        </#if>
                                    </div>
                                </td>
                            </tr>        
                        </#list>
                    </#if>
                </table>
                <br/>        
                <#assign numberShowed = numberShowed + 1>                
            </#list>
        </#list>
        <div align="center">
            <br/><br/>
              ${uiLabelMap.WebtoolsEntityDisplayed}: ${numberShowed}
          <div>
    </body>
</html>
