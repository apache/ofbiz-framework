<div id="emailComposerGmail" style="width:700px;">


<form data-dojo-type="dijit/form/Form"  method="post" action="<@ofbizUrl>SendEmailWithGmail</@ofbizUrl>" id="sendemailform" name="sendemailform">

    <input type="hidden" name="sendFrom" value="${sendFrom!}"/>

    <!--TODO: Get based on file uploaded -->
    <input type="hidden" name="attachmentContentIds" value=""/>
    <div class="row">
        <div class="col-sm-3">
            <div class="form-group">
                <label class="control-label">To (comma separated emails):</label>
            </div>
        </div>
        <div class="col-sm-9">
            <input type="text" name="sendTo" value="" size="50"/>
        </div>
    </div>

    <div class="row">
        <div class="col-sm-3">
            <div class="form-group">
                <label class="control-label">CC (comma separated emails):</label>
            </div>
        </div>
        <div class="col-sm-9">
            <input type="text" name="sendCc" value="" size="50"/>
        </div>
    </div>

    <div class="row">
        <div class="col-sm-3">
            <div class="form-group">
                <label class="control-label">BCC (comma separated emails):</label>
            </div>
        </div>
        <div class="col-sm-9">
            <input type="text" name="sendBcc" value="" size="50"/>
        </div>
    </div>
    <div class="row">
        <div class="col-sm-3">
            <div class="form-group">
                <label class="control-label">Subject:</label>
            </div>
        </div>
        <div class="col-sm-9">
            <input type="text" name="subject" value="Testing Subject"/>
        </div>
    </div>

    <div class="row">
        <div class="col-sm-3">
            <div class="form-group">
                <label class="control-label">Email Body:</label>
            </div>
        </div>
        <div class="col-sm-9">
            <textarea name="emailBody">Testing email body</textarea>
        </div>
    </div>


    <hr/>
    <div data-dojo-type="dijit/form/Button" id="emailWithGoogle_Send">Send Email
        <script type="dojo/on" data-dojo-event="click" data-dojo-args="evt">
            var formJson;
            require(['dojo/dom-form'], function(domForm){
                var formId = 'sendemailform';
                formJson = domForm.toObject(formId);
            });
            App.doBind(
                   formJson,
                   "<@ofbizUrl>SendEmailWithGmail</@ofbizUrl>",
                   function(response){
                       App.closeModal();
                   },
                   null
                );
        </script>
    </div>
</form>


</div>