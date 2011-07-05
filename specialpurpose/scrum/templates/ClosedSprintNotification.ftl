
<html xmlns="http://www.w3.org/1999/xhtml">
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <title>${title}</title>
    </head>
    <body>
        <p>Hello ${person.firstName?if_exists} ${person.lastName?if_exists},</p>
        <p>Your Sprint <b>${sprint.workEffortName?if_exists} [${sprint.workEffortId}]</b> in the project <b>${project.workEffortName?if_exists} [${project.workEffortId?if_exists}]</b> 
            of the product <b>${prodcut.internalName?if_exists} [${prodcut.productId?if_exists}]</b> has been Closed.
        <br />
        <br />
        The complete information about this sprint can be found <a href="${StringUtil.wrapString(baseSecureUrl?if_exists)}/scrum/control/ViewSprint?sprintId=${sprint.workEffortId}">here.....</a>
        <br /><br />
        Regards.<br /><br />
        Thank you for your business.
    </body>
</html>