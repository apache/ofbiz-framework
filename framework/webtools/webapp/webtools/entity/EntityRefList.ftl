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
          A.listtext {font-family: Helvetica,sans-serif; font-size: 10pt; font-weight: bold; text-decoration: none; color: blue;}
          A.listtext:hover {color:red;}
        </style>
    </head>
    <body bgcolor="#FFFFFF">
        <div align="left">
            <#if !forstatic>
                <#assign encodeURLMain = response.encodeURL(controlPath + "/main")> 
                <#assign encodeURLRefMain = response.encodeURL(controlPath + "/view/entityref_main")> 
                <#assign encodeURLCheckDb = response.encodeURL(controlPath + "/view/checkdb")> 
                <#assign encodeURLModelInduceFromDb = response.encodeURL(controlPath + "/view/ModelInduceFromDb")> 
                <a href="${encodeURLMain}" target='main' class='listtext'>${uiLabelMap.WebtoolsPopupWebToolsMain}</A><BR>
                <a href="${encodeURLRefMain}" target="entityFrame" class='listtext'>${uiLabelMap.WebtoolsEntityReferenceMainPage}</A><BR>
                <a href="${encodeURLCheckDb}" target="entityFrame" class='listtext'>${uiLabelMap.WebtoolsCheckUpdateDatabase}</A>
                <hr/>
                <!--
                <#assign encodeURLModelWriter = response.encodeURL(controlPath + "/ModelWriter")> 
                <#assign encodeURLModelWriterSaveFile = response.encodeURL(controlPath + "/ModelWriter?savetofile=true")> 
                <#assign encodeURLModelGroupWriter = response.encodeURL(controlPath + "/ModelGroupWriter")> 
                <#assign encodeURLModelGroupWriterSaveFile = response.encodeURL(controlPath + "/ModelGroupWriter?savetofile=true")> 
                -->
                <!-- want to leave these out because they are only working so-so, and cause people more problems that they solve, IMHO
                <a href="${encodeURLModelWriter}" target='_blank' class='listtext'>Generate Entity Model XML (all in one)</A><BR>
                <a href="${encodeURLModelWriterSaveFile}<%=response.encodeURL(controlPath + "/ModelWriter?savetofile=true")%>" target='_blank' class='listtext'>Save Entity Model XML to Files</A><BR>
                -->
                <!-- this is not working now anyway...
                <a href="${encodeURLModelGroupWriter}<%=response.encodeURL(controlPath + "/ModelGroupWriter")%>" target='_blank' class='listtext'>Generate Entity Group XML</A><BR>
                <a href="${encodeURLModelGroupWriterSaveFile}<%=response.encodeURL(controlPath + "/ModelGroupWriter?savetofile=true")%>" target='_blank' class='listtext'>Save Entity Group XML to File</A><BR>
                -->
                <a href="${encodeURLModelInduceFromDb}" target='_blank' class='listtext'>${uiLabelMap.WebtoolsInduceModelXMLFromDatabase}</A><BR>
                <hr/>
            </#if>            
            <#list packageNames as packageName>
                <#if forstatic>
                    <a href="entityref_main.html#${packageName}" target="entityFrame" class='listtext'>${packageName}</a><br/>
                <#else>
                    <#assign encodeURL = response.encodeURL(controlPath + "/view/entityref_main#" + packageName)> 
                    <a href="${encodeURL}" target="entityFrame" class='listtext'>${packageName}</a><br/>
                </#if>
            </#list>
            <hr/>
            <#list entitiesList as entity>
                <#if forstatic>
                    <#assign encodeURL = response.encodeURL(controlPath + "entityref_main#" + entity.entityName)>
                    <a href="${encodeURL}" target="entityFrame" class='listtext'>${entity.entityName}</a>
                <#else>
                    <#if entity.url?has_content>
                        <#assign encodeURL = response.encodeURL(controlPath + "/view/entityref_main#" + entity.entityName)>
                    <#else/>
                        <#-- I don't know about this entity.url stuff, but before cleaning things up here that is where the undefined url variable was -->
                        <#assign encodeURL = response.encodeURL(controlPath + "/view/entityref_main#" + entity.entityName + entity.url)>
                    </#if>
                    <a href="${encodeURL}" target="entityFrame" class='listtext'>${entity.entityName}</a>
                </#if>
                <br/>
            </#list>
            <br/>
        </div>
    </body>
</html>
