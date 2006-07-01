<#import "bloglib.ftl" as blog/>
<@blog.renderAncestryPath trail=ancestorList?default([])/>
    ${singleWrapper.renderFormString()}

