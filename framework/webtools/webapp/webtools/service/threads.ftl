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
<#if parameters.maxElements?has_content><#assign maxElements = parameters.maxElements?number/><#else><#assign maxElements = 5/></#if>

<div class="screenlet">
  <div class="screenlet-title-bar">
    <h3>Service Engine Threads</h3>
  </div>
<table class="basic-table" cellspacing="0">
  <tr class="header-row">
    <td>${uiLabelMap.WebtoolsThread}</td>
    <td>${uiLabelMap.CommonStatus}</td>
    <td>${uiLabelMap.WebtoolsJob}</td>
    <td>${uiLabelMap.WebtoolsService}</td>
    <td>${uiLabelMap.CommonTime} (ms)</td>
  </tr>
  <#list threads as thread>
  <tr>
    <td>${thread.threadName?if_exists}</td>
    <td>${thread.status?if_exists}</td>
    <td>${thread.jobName?default("[${uiLabelMap.CommonNone}]")}</td>
    <td>${thread.serviceName?default("[${uiLabelMap.CommonNone}]")}</td>
    <td>${thread.runTime?if_exists}</td>
  </tr>
  </#list>
</table>
</div>
<br />
<div class="screenlet">
  <div class="screenlet-title-bar">
    <h3>General Java Threads</h3>
  </div>
<br />
<p>Note: certain things only work in Java 5. Java Version is ${javaVer}; is Java 5? ${isJava5?string}}<p>
<br />
<table class="basic-table" cellspacing="0">
  <tr class="header-row">
    <td>Group</td>
    <td>ID</td>
    <td>${uiLabelMap.WebtoolsThread}</td>
    <td>${uiLabelMap.CommonStatus}</td>
    <td>Priority</td>
    <td>Daemon</td>
  </tr>
  <#list allThreadList as javaThread>
    <#if javaThread?exists>
      <#if isJava5><#assign stackTraceArray = javaThread.getStackTrace()/></#if>
      <tr>
        <td>${(javaThread.getThreadGroup().getName())?if_exists}</td>
        <td><#if isJava5>${javaThread.getId()?string}</#if></td>
        <td>
          <b>${javaThread.getName()?if_exists}</b>
          <#if isJava5>
            <#list 1..maxElements as stackIdx>
              <#assign stackElement = stackTraceArray[stackIdx]?if_exists/>
              <#if (stackElement.toString())?has_content>${stackElement.toString()}</#if>
            </#list>
          </#if>
        </td>
        <td><#if isJava5>${javaThread.getState().name()?if_exists}</#if>&nbsp;</td>
        <td>${javaThread.getPriority()}</td>
        <td>${javaThread.isDaemon()?string}<#-- /${javaThread.isAlive()?string}/${javaThread.isInterrupted()?string} --></td>
      </tr>
    </#if>
  </#list>
</table>
</div>







