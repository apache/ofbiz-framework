<html>
    <head>
        <title>PREVIEW</title>
    </head>
    <body>
        <pre>
            From: ${(contactMech.infoString)!}
            Subject: ${(communicationEvent.subject)!}
        </pre>
        <hr/>
        ${StringUtil.wrapString(communicationEvent.content)!}

    </body>
</html>