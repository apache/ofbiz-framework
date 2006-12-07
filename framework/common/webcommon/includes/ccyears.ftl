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
<#assign stringYear = thisDate?string("yyyy")>
<#assign thisYear = stringYear?number>

<option></option>
<option value="${thisYear}">${thisYear}</option>
<option value="${thisYear + 1}">${thisYear + 1}</option>
<option value="${thisYear + 2}">${thisYear + 2}</option>
<option value="${thisYear + 3}">${thisYear + 3}</option>
<option value="${thisYear + 4}">${thisYear + 4}</option>
<option value="${thisYear + 5}">${thisYear + 5}</option>
<option value="${thisYear + 6}">${thisYear + 6}</option>
<option value="${thisYear + 7}">${thisYear + 7}</option>
