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
<html lang="${docLangAttr}" dir="${langDir}" xmlns="http://www.w3.org/1999/xhtml">
    <head>
        <title>${uiLabelMap.WebtoolsEntityReference}</title>
        <style>
           body, textarea, input, select {font-family: Helvetica, sans-serif; background-color: #ffffff; text-decoration: none;}
          .section-header {font-size: 10pt; font-weight: bold; color: #000000; padding-bottom: 10;}
          .listtext {font-size: 10pt; font-weight: bold; color: blue;}
          .listtext a {text-decoration: none;}
          .listtext a:hover {color:red; text-decoration: underline;}
        </style>
    </head>
    <body>
        <div class='listtext'>
            <#if !forstatic>
                <#assign encodeURLMain = response.encodeURL(controlPath + "/main")> 
                <#assign encodeURLRefMain = response.encodeURL(controlPath + "/view/entityref_main")> 
                <#assign encodeURLCheckDb = response.encodeURL(controlPath + "/view/checkdb")> 
                <#assign encodeURLModelInduceFromDb = response.encodeURL(controlPath + "/view/ModelInduceFromDb")> 
                <a href="${encodeURLMain}" target='main'>${uiLabelMap.WebtoolsPopupWebToolsMain}</A><BR>
                <a href="${encodeURLRefMain}" target="entityFrame">${uiLabelMap.WebtoolsEntityReferenceMainPage}</A><BR>
                <a href="${encodeURLCheckDb}" target="entityFrame">${uiLabelMap.WebtoolsCheckUpdateDatabase}</A>
                <hr/>
                <#--
                <#assign encodeURLModelWriter = response.encodeURL(controlPath + "/ModelWriter")> 
                <#assign encodeURLModelWriterSaveFile = response.encodeURL(controlPath + "/ModelWriter?savetofile=true")> 
                <#assign encodeURLModelGroupWriter = response.encodeURL(controlPath + "/ModelGroupWriter")> 
                <#assign encodeURLModelGroupWriterSaveFile = response.encodeURL(controlPath + "/ModelGroupWriter?savetofile=true")> 
                -->
                <#-- want to leave these out because they are only working so-so, and cause people more problems that they solve, IMHO
                <a href="${encodeURLModelWriter}" target='_blank'>Generate Entity Model XML (all in one)</A><BR>
                <a href="${encodeURLModelWriterSaveFile}<%=response.encodeURL(controlPath + "/ModelWriter?savetofile=true")%>" target='_blank'>Save Entity Model XML to Files</A><BR>
                -->
                <#-- this is not working now anyway...
                <a href="${encodeURLModelGroupWriter}<%=response.encodeURL(controlPath + "/ModelGroupWriter")%>" target='_blank'>Generate Entity Group XML</A><BR>
                <a href="${encodeURLModelGroupWriterSaveFile}<%=response.encodeURL(controlPath + "/ModelGroupWriter?savetofile=true")%>" target='_blank'>Save Entity Group XML to File</A><BR>
                -->
                <a href="${encodeURLModelInduceFromDb}" target='_blank'>${uiLabelMap.WebtoolsInduceModelXMLFromDatabase}</A><BR>
            </#if> 
            <#if packageNames?has_content>
                <hr/>
                <div class="section-header">${uiLabelMap.WebtoolsEntityPackages}</div>
                <#list packageNames as packageName>
                    <#if forstatic>
                        <a href="entityref_main.html#${packageName}" target="entityFrame">${packageName}</a><br/>
                    <#else>
                        <#assign encodeURL = response.encodeURL(controlPath + "/view/entityref_main#" + packageName)> 
                        <a href="${encodeURL}" target="entityFrame">${packageName}</a><br/>
                    </#if>
                </#list>
            </#if>
            <#if entitiesList?has_content>
                <hr/>
                <div class="section-header">${uiLabelMap.WebtoolsEntitiesAlpha}</div>
                <#list entitiesList as entity>
                    <#if forstatic>
                        <#assign encodeURL = response.encodeURL(controlPath + "entityref_main#" + entity.entityName)>
                        <a href="${encodeURL}" target="entityFrame">${entity.entityName}</a>
                    <#else>
                        <#if entity.url?has_content>
                            <#assign encodeURL = response.encodeURL(controlPath + "/view/entityref_main#" + entity.entityName)>
                        <#else/>
                            <#-- I don't know about this entity.url stuff, but before cleaning things up here that is where the undefined url variable was -->
                            <#assign encodeURL = response.encodeURL(controlPath + "/view/entityref_main#" + entity.entityName + entity.url)>
                        </#if>
                        <a href="${encodeURL}" target="entityFrame">${entity.entityName}</a>
                    </#if>
                    <br/>
                </#list>
            </#if>
        </div>
    </body>
</html>
