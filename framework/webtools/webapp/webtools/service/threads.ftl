<#--
Copyright 2001-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
-->
<#assign javaVer = Static["java.lang.System"].getProperty("java.vm.version")/>
<#assign isJava5 = javaVer.startsWith("1.5")/>
<#if parameters.maxElements?has_content><#assign maxElements = parameters.maxElements?number/><#else><#assign maxElements = 5/></#if>

<div class="head3">Service Engine Threads</div>
<table cellpadding="2" cellspacing="0" border="1" width="100%">
  <tr>
    <td><div class="tableheadtext">${uiLabelMap.WebtoolsThread}</div></td>
    <td><div class="tableheadtext">${uiLabelMap.CommonStatus}</div></td>
    <td><div class="tableheadtext">${uiLabelMap.WebtoolsJob}</div></td>
    <td><div class="tableheadtext">${uiLabelMap.WebtoolsService}</div></td>
    <td><div class="tableheadtext">${uiLabelMap.CommonTime} (ms)</div></td>
  </tr>
  <#list threads as thread>
  <tr>
    <td><div class="tabletext">${thread.threadName?if_exists}&nbsp;</div></td>
    <td><div class="tabletext">${thread.status?if_exists}&nbsp;</div></td>
    <td><div class="tabletext">${thread.jobName?default("[${uiLabelMap.CommonNone}]")}</div></td>
    <td><div class="tabletext">${thread.serviceName?default("[${uiLabelMap.CommonNone}]")}</div></td>
    <td><div class="tabletext">${thread.runTime?if_exists}&nbsp;</div></td>
  </tr>
  </#list>
</table>

<hr/>
<div class="head3">General Java Threads</div>
<div class="tabletext">Note: certain things only work in Java 5. Java Version is ${javaVer}; is Java 5? ${isJava5?string}}</div>
<table cellpadding="2" cellspacing="0" border="1" width="100%">
  <tr>
    <td><div class="tableheadtext">Group</div></td>
    <td><div class="tableheadtext">ID</div></td>
    <td><div class="tableheadtext">${uiLabelMap.WebtoolsThread}</div></td>
    <td><div class="tableheadtext">${uiLabelMap.CommonStatus}</div></td>
    <td><div class="tableheadtext">Priority</div></td>
    <td><div class="tableheadtext">Daemon</div></td>
  </tr>
  <#list allThreadList as javaThread>
    <#if javaThread?exists>
      <#if isJava5><#assign stackTraceArray = javaThread.getStackTrace()/></#if>
      <tr>
        <td><div class="tabletext">${(javaThread.getThreadGroup().getName())?if_exists}&nbsp;</div></td>
        <td><div class="tabletext"><#if isJava5>${javaThread.getId()?string}</#if>&nbsp;</div></td>
        <td>
          <div class="tableheadtext">${javaThread.getName()?if_exists}&nbsp;</div>
          <#if isJava5>
            <#list 1..maxElements as stackIdx>
              <#assign stackElement = stackTraceArray[stackIdx]?if_exists/>
              <#if (stackElement.toString())?has_content><div class="tabletext">${stackElement.toString()}&nbsp;</div></#if>
            </#list>
          </#if>
        </td>
        <td><div class="tabletext"><#if isJava5>${javaThread.getState().name()?if_exists}</#if>&nbsp;</div></td>
        <td><div class="tabletext">${javaThread.getPriority()}</div></td>
        <td><div class="tabletext">${javaThread.isDaemon()?string}<#-- /${javaThread.isAlive()?string}/${javaThread.isInterrupted()?string} --></div></td>
      </tr>
    </#if>
  </#list>
</table>

