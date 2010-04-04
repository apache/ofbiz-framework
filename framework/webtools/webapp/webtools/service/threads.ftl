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
<#assign javaVer = Static["java.lang.System"].getProperty("java.vm.version")/>
<#assign isJava5 = javaVer.startsWith("1.5")/>
<#if parameters.maxElements?has_content><#assign maxElements = parameters.maxElements?number/><#else><#assign maxElements = 10/></#if>

<div class="screenlet">
  <div class="screenlet-title-bar">
    <h3>${uiLabelMap.WebtoolsServiceEngineThreads}</h3>
  </div>
  <div class="screenlet-body"> 
    <table class="basic-table hover-bar" cellspacing="0">
      <tr class="header-row">
        <td>${uiLabelMap.WebtoolsThread}</td>
        <td>${uiLabelMap.CommonStatus}</td>
        <td>${uiLabelMap.WebtoolsJob}</td>
        <td>${uiLabelMap.WebtoolsService}</td>
        <td>${uiLabelMap.WebtoolsUsage}</td>
        <td>${uiLabelMap.WebtoolsTTL} (ms)</td>
        <td>${uiLabelMap.CommonTime} (ms)</td>
      </tr>
      <#assign alt_row = false>
      <#list threads as thread>
      <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
        <td>${thread.threadId?if_exists} ${thread.threadName?if_exists}</td>
        <td>${thread.status?if_exists}</td>
        <td>${thread.jobName?default("${uiLabelMap.CommonNone}")}</td>
        <td>${thread.serviceName?default("${uiLabelMap.CommonNone}")}</td>
        <td>${thread.usage?if_exists}</td>
        <td>${thread.ttl?if_exists}</td>
        <td>${thread.runTime?if_exists}</td>
      </tr>
      <#-- toggle the row color -->
      <#assign alt_row = !alt_row>
      </#list>
    </table>
  </div>
</div>
<div class="screenlet">
  <div class="screenlet-title-bar">
    <h3>${uiLabelMap.WebtoolsGeneralJavaThreads}</h3>
  </div>
  <div class="screenlet-body"> 
    <br />
    <p>${uiLabelMap.WebtoolsThisThread}<b> ${Static["java.lang.Thread"].currentThread().getName()} (${Static["java.lang.Thread"].currentThread().getId()})</b></p>
    <p>${uiLabelMap.WebtoolsJavaVersionIs5} <#if isJava5> ${uiLabelMap.CommonYes}<#else>${uiLabelMap.CommonNo}</#if></p>
    <br />
    <table class="basic-table hover-bar" cellspacing="0">
      <tr class="header-row">
        <td>${uiLabelMap.WebtoolsGroup}</td>
        <td>${uiLabelMap.WebtoolsThreadId}</td>
        <td>${uiLabelMap.WebtoolsThread}</td>
        <td>${uiLabelMap.CommonStatus}</td>
        <td>${uiLabelMap.WebtoolsPriority}</td>
        <td>${uiLabelMap.WebtoolsDaemon}</td>
      </tr>
      <#assign alt_row = false>
      <#list allThreadList as javaThread>
      <#if javaThread?exists>
        <#if isJava5><#assign stackTraceArray = javaThread.getStackTrace()/></#if>
        <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
          <td valign="top">${(javaThread.getThreadGroup().getName())?if_exists}</td>
          <td valign="top"><#if isJava5>${javaThread.getId()?string}</#if></td>
          <td valign="top">
            <b>${javaThread.getName()?if_exists}</b>
            <#if isJava5>
              <#list 1..maxElements as stackIdx>
                <#assign stackElement = stackTraceArray[stackIdx]?if_exists/>
                <#if (stackElement.toString())?has_content><div>${stackElement.toString()}</div></#if>
              </#list>
            </#if>
          </td>
          <td valign="top"><#if isJava5>${javaThread.getState().name()?if_exists}</#if>&nbsp;</td>
          <td valign="top">${javaThread.getPriority()}</td>
          <td valign="top">${javaThread.isDaemon()?string}<#-- /${javaThread.isAlive()?string}/${javaThread.isInterrupted()?string} --></td>
        </tr>
      </#if>
      <#-- toggle the row color -->
      <#assign alt_row = !alt_row>
      </#list>
    </table>
  </div>
</div>