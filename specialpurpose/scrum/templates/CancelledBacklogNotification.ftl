
<html xmlns="http://www.w3.org/1999/xhtml">
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <title>${title}</title>
    </head>
    <body>
        <p>Hello ${person.firstName?if_exists} ${person.lastName?if_exists},</p>
        <p>Your Customer Request <b>${custRequest.custRequestName?if_exists} [${custRequest.custRequestId}] </b> <#if informationMap.workEffortId?has_content>in sprint <b>${informationMap.workEffortName?if_exists} [${informationMap.workEffortId?if_exists}]</b></#if> 
            <#if informationMap.productId?has_content>of the product <b>${informationMap.internalName?if_exists} [${informationMap.productId?if_exists}]</#if></b> has been CANCELLED.
        <p>Your Reference: ${custRequest.requesterId?if_exists}</p>
        <p>Reason for Cancellation: ${custRequest.reason?if_exists}</p>
        <br />
        <br />
        <#if custRequest.fromPartyId == partyIdTo>
        The complete information about this request can be found <a href="${StringUtil.wrapString(baseSecureUrl?if_exists)}/scrum/control/ViewProdBacklogItem?custRequestId=${custRequest.custRequestId?if_exists}">here.....</a>
        <br /><br />
        </#if>
        Regards.<br /><br />
        Thank you for your business.
    </body>
</html>
