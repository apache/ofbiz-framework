${singleWrapper.renderFormString()}
<hr/>
<#assign id=requestParameters.contentId?if_exists/>
<@editRenderSubContent contentId="TEMPLATE_MASTER" mapKey="" subContentId=subContentId?if_exists>
<@renderSubContent/>
</@editRenderSubContent>
