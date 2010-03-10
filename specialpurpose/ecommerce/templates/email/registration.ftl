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

<#-- All fields from Person and UserLogin are available to this template -->

${person.firstName},
<br /><br />
${uiLabelMap.EcommerceThankForRegistering} MyStore.com. <#-- MyStore.com (not a variable why?) must be adapted - JLR 1/6/5 -->
<br /><br /><br />

${uiLabelMap.EcommerceAccountLogin} ${userLogin.userLoginId}<br />
${uiLabelMap.EcommercePassword}<br /><br />

${uiLabelMap.EcommerceLosePassword}<br /><br />

${uiLabelMap.EcommerceThankYou},<br />
MyStore.com <#-- MyStore.com (not a variable why?) must be adapted - JLR 1/6/5 -->
<br /><br />

