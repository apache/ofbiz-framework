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
    </div>
  </div>
  <div class="breadcrumbs-sep">
    <#if titleProperty??>
        ${uiLabelMap[titleProperty]}
    <#else>${title!}
    </#if>
  </div>
  <div class="breadcrumbs-end">
  </div>
</div>
<div class="clear">
</div>

<#if userLogin??>
<script type="application/javascript">
  var mainmenu = new DropDownMenu(jQuery('#main-navigation'));
  var appmenu = new DropDownMenu(jQuery('#app-navigation'));
</script>
</#if>
