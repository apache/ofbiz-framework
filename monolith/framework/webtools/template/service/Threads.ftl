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
<#if parameters.maxElements?has_content><#assign maxElements = parameters.maxElements?number/><#else><#assign maxElements = 10/></#if>

    <p>${uiLabelMap.WebtoolsThisThread}<b> ${currentThread.getName()} (${currentThread.getId()})</b></p>
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
      <#if javaThread??>
        <#assign stackTraceArray = allThreadStackTrace.get(javaThread.getName())/>
        <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
          <td valign="top">${(javaThread.getThreadGroup().getName())!}</td>
          <td valign="top">${javaThread.getId()?string}</td>
          <td valign="top">
            <b>${javaThread.getName()!}</b>
            <#list 1..maxElements as stackIdx>
              <#assign stackElement = stackTraceArray[stackIdx]!/>
              <#if (stackElement.toString())?has_content><div>${stackElement.toString()}</div></#if>
            </#list>
          </td>
          <td valign="top">${javaThread.getState().name()!}&nbsp;</td>
          <td valign="top">${javaThread.getPriority()}</td>
          <td valign="top">${javaThread.isDaemon()?string}<#-- /${javaThread.isAlive()?string}/${javaThread.isInterrupted()?string} --></td>
        </tr>
      </#if>
      <#-- toggle the row color -->
      <#assign alt_row = !alt_row>
      </#list>
    </table>
