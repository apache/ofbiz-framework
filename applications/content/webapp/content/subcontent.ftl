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
<html>
<head>
<title>SubContent Test</title>
</head>
<body>
<table border="2" bordercolor="green">
<tr><th><h2>SubContent Test</h2></th></tr>
<tr><th><@editRenderSubContent  mapKey="keyA" subDataResourceTypeId="ELECTRONIC_TEXT" mimeTypeId="text/plain">123<@renderSubContent/>456</@editRenderSubContent></th>
<td>abc<@renderSubContent mapKey="keyA"/>xyz</td>
</tr>

<tr><th><@editRenderSubContent  mapKey="keyB" subDataResourceTypeId="IMAGE_OBJECT">123<@renderSubContent/>456</@editRenderSubContent></th>
<td>abc<@renderSubContent mapKey="keyB"/>xyz</td>
</tr>

<tr><th><@editRenderSubContent  mapKey="keyC" subDataResourceTypeId="URL_RESOURCE">123<a href="<@renderSubContent/>" ><@renderSubContent subDataResourceTypeId="DESCRIPTION"/></a>456</@editRenderSubContent></th>
<td>abc<@renderSubContent mapKey="keyC"/>xyz</td>
</tr>

<tr><th><@editRenderSubContent  mapKey="keyD" subDataResourceTypeId="ELECTRONIC_TEXT" mimeTypeId="text/html">123<@renderSubContent/>456</@editRenderSubContent></th>
<td>abc<@renderSubContent mapKey="keyD"/>xyz</td>
</tr>

</table>
</body>
</html>
