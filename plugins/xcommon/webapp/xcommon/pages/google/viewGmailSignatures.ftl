<div id="gmailSignaturesPane" style="width:600px;">


    <#if signature??>
    <div class="ui styled accordion">
        <div class="title active">
            <i class="dropdown icon"></i>
            Configured Email Signature
        </div>
        <div class="content active" style="overflow-y: auto;max-height: 450px;">
            ${StringUtil.wrapString(signature!)}
        </div>
    </div>
    <#else>
        <p class="fsd-font-m">No email signature found!</p>
        <p class="fsd-font-mi">Seems like you don't have any email signatures set up yet. Use the option below to download configured signatures from your connected mailbox account.</p>
    </#if>

    <hr/>
    <a href="javascript:void(0);" class="ax-btnPrimary" onclick="
            $(this).addClass('ui loading button');
            App.doBindWithoutLoader(
            {},
            '<@ofbizUrl>DownloadGmailSignatures</@ofbizUrl>',
            function(response){
                $(this).removeClass('ui loading button');
                //Refresh Modal dialog
                App.openModal({
                    content : {},
                    url     :   '<@ofbizUrl>ViewGmailSignatures</@ofbizUrl>',
                    onCancel:   App.closeModal(),
                    title   :   'Signatures from Mailbox',
                    showRequiredIndicator : false,
                    showLoader: true
                });
            },
            null
            );
            ">Download From Mailbox</a>
</div>