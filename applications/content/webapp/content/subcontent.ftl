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
