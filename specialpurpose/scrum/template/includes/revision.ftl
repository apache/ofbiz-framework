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
<#if result?has_content>
<div align="center">
<h2>Subversion Information for repository : <b><a href=${result.repository!}> ${result.repository!}</a></b>, revision# <b>${result.revision!}</b></h2>
</div>
<div>
    <br/><h3>Log message</h3>
    <br/><pre>${result.logMessage}</pre>
</div>
<div>
    <#assign oldrevision = result.revision?number - 1 >
    <br/><h3>The differences between revisions: ${oldrevision!} and ${result.revision!} </h3>
    <br/><pre>${result.diffMessage}</pre>
</div>
</#if>