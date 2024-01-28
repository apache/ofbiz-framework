<#--Email settings-->
<div class="mt-10">
  <div class="md:grid md:grid-cols-3 md:gap-6">
    <div class="md:col-span-1">
      <div class="px-4 sm:px-0">
        <h3 class="text-lg font-medium leading-6 text-gray-900">Email Settings</h3>
        <p class="mt-1 text-sm text-gray-600">
          Manage email related configuration here, use the test email feature to see if it's working as expected.
        </p>
      </div>
    </div>
    <div class="mt-5 md:mt-0 md:col-span-2" x-data="formChangeTracker()">
        <div class="shadow overflow-hidden sm:rounded-md"
             x-data="{isEmailSendingEnabled: <#if emailConfiguration.isEmailSendingEnabled>true<#else>false</#if>}">
          <div class="bg-white">
          <#--unsaved changes tracker-->
            <div x-cloak x-show="hasFormChanged" x-transition
                 class="mt-2 px-4 py-2 bg-gray-100 italic font-medium">
              You have unsaved changes!
            </div>
            <div class="border-b">
            <#--sync enabled-->
              <div class="px-4 py-5 sm:p-6 flex items-start">
                <div class="flex items-center h-5">
                  <input id="isEmailSendingEnabled" name="isEmailSendingEnabled" type="checkbox"
                         @change="refreshHasFormChanged()" x-model="isEmailSendingEnabled"
                         value="emailSendingEnabled"
                         <#if emailConfiguration.isEmailSendingEnabled>checked</#if>
                         class="focus:ring-indigo-500 h-4 w-4 text-indigo-600 border-gray-300 rounded">
                </div>
                <div class="ml-3 text-sm">
                  <label for="isEmailSendingEnabled"
                         class="font-medium text-gray-700">Email sending enabled?</label>
                  <p class="text-gray-500">When enabled emails will be sent out.</p>
                </div>
              </div>
            <#--Warning when email sending is not enabled-->
              <div x-cloak x-show="!isEmailSendingEnabled" x-transition
                   class="relative px-4 py-3 sm:p-6 sm:py-3 flex space-x-4 items-center justify-between bg-yellow-200">
              <#--Top pointer-->
                <div class="absolute left-0 top-0 h-16 w-16 text-yellow-200"
                     style="left:2.55rem;top:-1.25rem;">
                  <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32"
                       fill="currentColor" class="bi bi-caret-up-fill" viewBox="0 0 16 16">
                    <path
                        d="m7.247 4.86-4.796 5.481c-.566.647-.106 1.659.753 1.659h9.592a1 1 0 0 0 .753-1.659l-4.796-5.48a1 1 0 0 0-1.506 0z"/>
                  </svg>
                </div>
                <div class="text-red-600">
                  <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24"
                       fill="currentColor" class="bi bi-exclamation-triangle-fill"
                       viewBox="0 0 16 16">
                    <path
                        d="M8.982 1.566a1.13 1.13 0 0 0-1.96 0L.165 13.233c-.457.778.091 1.767.98 1.767h13.713c.889 0 1.438-.99.98-1.767L8.982 1.566zM8 5c.535 0 .954.462.9.995l-.35 3.507a.552.552 0 0 1-1.1 0L7.1 5.995A.905.905 0 0 1 8 5zm.002 6a1 1 0 1 1 0 2 1 1 0 0 1 0-2z"/>
                  </svg>
                </div>
                <div class="text-yellow-900 flex-auto">
                  <h3 class="text-sm font-semibold">WARNING: Email sending is disabled!!</h3>
                  <p class="text-sm">No emails will be sent out, don't be surprised if you don't see your emails going out duh!!. Enable in production and make sure redirect to email is empty.</p>
                </div>
                <div class="flex-none">
                  <button type="button"
                          class="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-yellow-800 bg-yellow-300 hover:bg-yellow-400 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-yellow-500">
                    Enable emails
                  </button>
                </div>
              </div>
            </div>
            <#--Redirect all emails to email-->
          <#if emailConfiguration.redirectAllEmailsTo?has_content>
          <#--Warning when email sending is not enabled-->
            <div x-cloak
                 class="relative px-4 py-3 sm:p-6 sm:py-3 flex space-x-4 items-center justify-between bg-blue-100">
            <#--Top pointer-->
              <div class="absolute left-0 top-0 h-16 w-16 text-blue-100"
                   style="left:2.55rem;top:-1.25rem;">
                <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32"
                     fill="currentColor" class="bi bi-caret-up-fill" viewBox="0 0 16 16">
                  <path
                      d="m7.247 4.86-4.796 5.481c-.566.647-.106 1.659.753 1.659h9.592a1 1 0 0 0 .753-1.659l-4.796-5.48a1 1 0 0 0-1.506 0z"/>
                </svg>
              </div>
              <div class="text-blue-600">
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="currentColor" class="bi bi-info-circle" viewBox="0 0 16 16">
                  <path d="M8 15A7 7 0 1 1 8 1a7 7 0 0 1 0 14zm0 1A8 8 0 1 0 8 0a8 8 0 0 0 0 16z"/>
                  <path d="m8.93 6.588-2.29.287-.082.38.45.083c.294.07.352.176.288.469l-.738 3.468c-.194.897.105 1.319.808 1.319.545 0 1.178-.252 1.465-.598l.088-.416c-.2.176-.492.246-.686.246-.275 0-.375-.193-.304-.533L8.93 6.588zM9 4.5a1 1 0 1 1-2 0 1 1 0 0 1 2 0z"/>
                </svg>
              </div>
              <div class="text-blue-900 flex-auto">
                <h3 class="text-sm font-semibold">You are in development mode!</h3>
                <p class="text-sm">No emails will be sent out to the actual recipients, all emails will be sent to the redirect to email address. Ignore this warning if you are in development/test mode, for production redirect emails to should be empty.</p>
              </div>
            </div>
          </#if>
            <div class="px-4 py-5 sm:p-6">
              <div class="w-full mb-4">
                <label for="redirectAllEmailsTo" class="block text-sm font-medium text-gray-700">Redirect emails to</label>
                <input type="text" name="redirectAllEmailsTo" id="redirectAllEmailsTo"
                       @keyup="refreshHasFormChanged()"
                       autocomplete="email"
                       value="${emailConfiguration.redirectAllEmailsTo!}"
                       placeHolder="Example: youremail@example.com"
                       class="mt-1 focus:ring-indigo-500 focus:border-indigo-500 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md border">
                <p class="mt-2 text-sm text-gray-500">Catch all email address, all outgoing emails will be redirected to this email address if one is provided.
                  Keep it blank in production.
                </p>
              </div>
            <#--Default from email address-->
              <div class="w-full mb-4">
                <label for="sendFromEmail" class="block text-sm font-medium text-gray-700">Send emails from</label>
                <input type="text" name="sendFromEmail" id="sendFromEmail"
                       @keyup="refreshHasFormChanged()"
                       autocomplete="given-name"
                       value="${emailConfiguration.sendFromEmail!}"
                       placeHolder="Example: no-reply@example.com"
                       class="mt-1 focus:ring-indigo-500 focus:border-indigo-500 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md border">
                <p class="mt-2 text-sm text-gray-500">Emails will be send out from this email address. <br/>Makes up <strong>From</strong> section of the email for example
                  email from <strong>no-reply@example.com</strong>.
                </p>
              </div>
              <h3 class="text-base font-medium text-gray-900 mb-4 mt-6">SMTP credentials (Email server)</h3>
              <#--SMTP host, user and password -->
              <div class="w-full mb-4">
                <label for="emailServerHost" class="block text-sm font-medium text-gray-700">Host name</label>
                <input type="text" name="emailServerHost" id="emailServerHost" autocomplete="given-name"
                       @keyup="refreshHasFormChanged()"
                       placeHolder="Example: email-smtp.us-central.amazonaws.com"
                       value="${emailConfiguration.emailServerHost!}"
                       class="mt-1 focus:ring-indigo-500 focus:border-indigo-500 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md border">
                <p class="mt-2 text-sm text-gray-500">
                  Host name of the email server can be obtained from <a class="text-blue-600 hover:underline"
                          href="https://console.aws.amazon.com/iamv2/home?#/users"
                          target="_blank">here</a>.
                </p>
              </div>
              <div class="w-full mb-4">
                <label for="smtpUser" class="block text-sm font-medium text-gray-700">SMTP Username</label>
                <input type="text" name="smtpUser" id="smtpUser" autocomplete="given-name"
                       @keyup="refreshHasFormChanged()"
                       placeHolder="Example: AKIAQLGPLKW9UPA45B4Y"
                       value="${emailConfiguration.smtpUser!}"
                       class="mt-1 focus:ring-indigo-500 focus:border-indigo-500 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md border">
                <p class="mt-2 text-sm text-gray-500">S3 SES username to be used for authentication available <a class="text-blue-600 hover:underline"
                          href="https://console.aws.amazon.com/iamv2/home?#/users"
                          target="_blank">here</a>.
                </p>
              </div>
            <#--Secret key-->
              <div class="mb-4">
                <label for="smtpUserPassword" class="block text-sm font-medium text-gray-700">
                  SMTP Password
                </label>
                <div class="mt-1">
                          <textarea id="smtpUserPassword" name="smtpUserPassword" rows="2"
                                    class="px-3 py-2 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 mt-1 block w-full sm:text-sm border border-gray-300 rounded-md"
                                    placeholder="Usually looks something like e7w3OYSQyFQihHKOx9u9fewX0cGL3aCzs258n7BW">${emailConfiguration.smtpUserPassword!!}</textarea>
                </div>
              </div>
              <#--Test email recipient-->
              <h3 class="text-base font-medium text-gray-900 mb-4 mt-6">Test your email settings</h3>
              <#--Test email recipient input -->
              <div class="w-full mb-4">
                <label for="sendTestEmailTo" class="block text-sm font-medium text-gray-700">Send test email to</label>
                <input type="text" name="sendTestEmailTo" id="sendTestEmailTo" autocomplete="email"
                       @keyup="refreshHasFormChanged()"
                       placeHolder="Example: youremail@example.com"
                       value="${parameters.sendTestEmailTo!}"
                       class="mt-1 focus:ring-indigo-500 focus:border-indigo-500 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md border">
                <p class="mt-2 text-sm text-gray-500">
                  Recipient of the test email. Clicking on `Send test email` will trigger a test email to be sent to this email address.
                </p>
              </div>

            </div>
          </div>
          <div x-data="testEmailConnectivity()">
            <div class="px-4 py-3 bg-gray-50 flex items-center justify-between sm:px-6">
              <button type="button" x-bind:disabled="isTestingEmailConnectivity"
                      @click="testConnectivity()"
                      x-bind:class="{ 'opacity-50': isTestingEmailConnectivity }"
                      class="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-yellow-800 bg-yellow-300 hover:bg-yellow-400 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-yellow-500 disabled:opacity-50">
                        <span x-cloak x-show="isTestingEmailConnectivity">
                          <svg class="inline-block animate-spin -ml-1 mr-3 h-5 w-5 text-white"
                               xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor"
                                    stroke-width="4"></circle>
                            <path class="opacity-75" fill="currentColor"
                                  d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                          </svg>
                        </span>
                <span x-cloak x-show="isTestingEmailConnectivity">Sending test email...</span>
                <span x-cloak x-show="!isTestingEmailConnectivity">Send test email</span>
              </button>
              <span x-show="redirectAllEmails">All outgoing emails will be redirected to <span></span></span>
              <button id="updateValuesActionButton" type="submit"
                      :disabled="isEmailSettingsFormSubmitted || !hasFormChanged"
                      @click="submitForm"
                      x-bind:class="{ 'opacity-50': isEmailSettingsFormSubmitted }"
                      class="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50">
                        <span x-cloak x-show="isEmailSettingsFormSubmitted">
                          <svg class="inline-block animate-spin -ml-1 mr-3 h-5 w-5 text-white"
                               xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor"
                                    stroke-width="4"></circle>
                            <path class="opacity-75" fill="currentColor"
                                  d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                          </svg>
                        </span>
                <span x-cloak x-show="isEmailSettingsFormSubmitted">Saving settings...</span>
                <span x-cloak x-show="!isEmailSettingsFormSubmitted">Save settings</span>
              </button>
            </div>
          <#--Connection successful/failure message-->
            <div x-cloak x-show="showEmailConnectionResults"
                 class="mb-2">
            <#--First row-->
              <div class="px-6 pt-4 flex items-center justify-between rounded space-x-2">
              <#--Connection successful message-->
                <div class="flex-none text-green-700" x-show="emailConnected">
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16"
                       fill="currentColor"
                       class="bi bi-check-circle-fill" viewBox="0 0 16 16">
                    <path
                        d="M16 8A8 8 0 1 1 0 8a8 8 0 0 1 16 0zm-3.97-3.03a.75.75 0 0 0-1.08.022L7.477 9.417 5.384 7.323a.75.75 0 0 0-1.06 1.06L6.97 11.03a.75.75 0 0 0 1.079-.02l3.992-4.99a.75.75 0 0 0-.01-1.05z"/>
                  </svg>
                </div>
                <div class="text-green-600 flex-auto" x-show="emailConnected">
                  <h3 class="text-base font-medium">Connected successfully!</h3>
                  <p class="text-sm" x-text:="successResponse"></p>
                </div>
              <#--Connection failed message-->
                <div class="flex-none text-red-700" x-show="!emailConnected">
                  <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="currentColor" class="bi bi-exclamation-circle-fill" viewBox="0 0 16 16">
                    <path d="M16 8A8 8 0 1 1 0 8a8 8 0 0 1 16 0zM8 4a.905.905 0 0 0-.9.995l.35 3.507a.552.552 0 0 0 1.1 0l.35-3.507A.905.905 0 0 0 8 4zm.002 6a1 1 0 1 0 0 2 1 1 0 0 0 0-2z"/>
                  </svg>
                </div>
                <div class="text-red-800 flex-auto" x-show="!emailConnected">
                  <h3 class="text-base font-medium" >Connection failed...</h3>
                  <p x-show=""></p>
                </div>
              <#--Dismiss button-->
                <div class="text-green-800">
                  <button x-on:click="showEmailConnectionResults = false" type="button"
                          class="inline-flex items-center justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-red-600 hover:bg-red-700 hover:text-white">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16"
                         fill="currentColor"
                         class="bi bi-x mr-2" viewBox="0 0 16 16">
                      <path
                          d="M4.646 4.646a.5.5 0 0 1 .708 0L8 7.293l2.646-2.647a.5.5 0 0 1 .708.708L8.707 8l2.647 2.646a.5.5 0 0 1-.708.708L8 8.707l-2.646 2.647a.5.5 0 0 1-.708-.708L7.293 8 4.646 5.354a.5.5 0 0 1 0-.708z"/>
                    </svg>
                    Dismiss
                  </button>
                </div>
              </div>
            <#--Error message when connection is unsuccessful-->
              <div x-cloak x-show="!emailConnected" class="bg-red-100 px-8 py-3 text-red-800">
                <h3 class="text-base font-medium ">Error details:</h3>
                <p class="text-sm" x-text:="errorResponse"></p>
              </div>
            </div>
          </div>

        </div>
    </div>
  </div>
</div>
