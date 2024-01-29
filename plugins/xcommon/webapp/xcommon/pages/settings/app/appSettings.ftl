<#--Amazon settings-->
<div class="mt-10">
  <div class="md:grid md:grid-cols-3 md:gap-6">
    <div class="md:col-span-1">
      <div class="px-4 sm:px-0">
        <h3 class="text-lg font-medium leading-6 text-gray-900">Application Settings
          Settings</h3>
        <p class="mt-1 text-sm text-gray-600">
          Manage application branding and related settings.
        </p>
      </div>
    </div>
    <div class="mt-5 md:mt-0 md:col-span-2">
      <div class="shadow overflow-hidden sm:rounded-md"
           x-data="{isSyncEnabled: <#if awsConfiguration.isSyncEnabled>true<#else>false</#if>}">
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
                <input id="awsSyncEnabled" name="awsSyncEnabled" type="checkbox"
                       @change="refreshHasFormChanged()" x-model="isSyncEnabled"
                       value="syncEnabled"
                       <#if awsConfiguration.isSyncEnabled>checked</#if>
                       class="focus:ring-indigo-500 h-4 w-4 text-indigo-600 border-gray-300 rounded">
              </div>
              <div class="ml-3 text-sm">
                <label for="awsSyncEnabled"
                       class="font-medium text-gray-700">Sync enabled?</label>
                <p class="text-gray-500">When enabled uploaded files will be synced with
                  configured S3 bucket.</p>
              </div>
            </div>
          <#--Warning when sync is not enabled-->
            <div x-cloak x-show="!isSyncEnabled" x-transition
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
                <h3 class="text-sm font-semibold">WARNING: Sync disabled!!</h3>
                <p class="text-sm">Uploaded documents will not be synced to the AWS S3
                  bucket. Enable sync in production</p>
              </div>
              <div class="flex-none">
                <button type="button"
                        class="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-yellow-800 bg-yellow-300 hover:bg-yellow-400 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-yellow-500">
                  Enable sync
                </button>
              </div>
            </div>
          </div>
          <div class="px-4 py-5 sm:p-6">
          <#--Bucket name-->
            <div class="w-full mb-4">
              <label for="bucketName" class="block text-sm font-medium text-gray-700">Bucket
                name</label>
              <input type="text" name="bucketName" id="bucketName"
                     @keyup="refreshHasFormChanged()"
                     autocomplete="given-name"
                     value="${awsConfiguration.bucketName!}"
                     placeHolder="Example: app-s3-bucket"
                     class="mt-1 focus:ring-indigo-500 focus:border-indigo-500 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md">
              <p class="mt-2 text-sm text-gray-500">
                Name of the S3 configured bucket. Usually available <a
                  class="text-blue-600 hover:underline"
                  href="https://s3.console.aws.amazon.com/s3/buckets"
                  target="_blank">here</a>
              </p>
            </div>
            <h3 class="text-base font-medium text-gray-900 mb-4 mt-6">Security
              credentials</h3>
          <#--Access key and secret key -->
            <div class="w-full mb-4">
              <label for="accessKey" class="block text-sm font-medium text-gray-700">Access
                key</label>
              <input type="text" name="accessKey" id="accessKey" autocomplete="given-name"
                     @keyup="refreshHasFormChanged()"
                     placeHolder="Example: AKIAQLGPLKW53RYW4BFM"
                     value="${awsConfiguration.accessKey!}"
                     class="mt-1 focus:ring-indigo-500 focus:border-indigo-500 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md border">
              <p class="mt-2 text-sm text-gray-500">
                Access key for the S3 configured user. You can manage users and obtain
                credentials
                from <a class="text-blue-600 hover:underline"
                        href="https://console.aws.amazon.com/iamv2/home?#/users"
                        target="_blank">here</a>.
              </p>
            </div>
          <#--Secret key-->
            <div class="mb-4">
              <label for="secretKey" class="block text-sm font-medium text-gray-700">
                Secret key
              </label>
              <div class="mt-1">
                          <textarea id="secretKey" name="secretKey" rows="2"
                                    class="px-3 py-2 shadow-sm focus:ring-indigo-500 focus:border-indigo-500 mt-1 block w-full sm:text-sm border border-gray-300 rounded-md"
                                    placeholder="Usually looks something like e7w3OYSQyFQihHKOx9u9fewX0cGL3aCzs258n7BW">${awsConfiguration.secretKey!!}</textarea>
              </div>
            </div>
          <#--Region-->
            <div class="col-span-6 sm:col-span-3">
              <label for="region"
                     class="block text-sm font-medium text-gray-700">Region</label>
              <select id="region" name="region"
                      placeHolder="Select region"
                      class="mt-1 block w-full py-2 px-3 border border-gray-300 bg-white rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm">
                <option></option>
              <#list awsAvailableRegions as awsAvailableRegion>
                <option value="${awsAvailableRegion}"
                        <#if awsConfiguration.region! == awsAvailableRegion>selected</#if>>${awsAvailableRegion}</option>
              </#list>
              </select>
            </div>
          </div>
        </div>
        <div x-data="testAwsS3Connection()">
          <div class="px-4 py-3 bg-gray-50 flex items-center justify-between sm:px-6">
            <button type="button" x-bind:disabled="isTestingConnectivity"
                    @click="testConnectivity()"
                    x-bind:class="{ 'opacity-50': isTestingConnectivity }"
                    class="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-yellow-800 bg-yellow-300 hover:bg-yellow-400 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-yellow-500 disabled:opacity-50">
                        <span x-cloak x-show="isTestingConnectivity">
                          <svg class="inline-block animate-spin -ml-1 mr-3 h-5 w-5 text-white"
                               xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor"
                                    stroke-width="4"></circle>
                            <path class="opacity-75" fill="currentColor"
                                  d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                          </svg>
                        </span>
              <span x-cloak x-show="isTestingConnectivity">Testing connectivity...</span>
              <span x-cloak x-show="!isTestingConnectivity">Test connectivity</span>
            </button>
            <button id="updateValuesActionButton" type="submit"
                    :disabled="isFormSubmitted || !hasFormChanged"
                    @click="submitForm"
                    x-bind:class="{ 'opacity-50': isFormSubmitted }"
                    class="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50">
                        <span x-cloak x-show="isFormSubmitted">
                          <svg class="inline-block animate-spin -ml-1 mr-3 h-5 w-5 text-white"
                               xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor"
                                    stroke-width="4"></circle>
                            <path class="opacity-75" fill="currentColor"
                                  d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                          </svg>
                        </span>
              <span x-cloak x-show="isFormSubmitted">Saving settings...</span>
              <span x-cloak x-show="!isFormSubmitted">Save settings</span>
            </button>
          </div>
        <#--Connection successful/failure message-->
          <div x-cloak x-show="showConnectionResults"
               class="mb-2">
          <#--First row-->
            <div class="px-6 pt-4 flex items-center justify-between rounded space-x-2">
            <#--Connection successful message-->
              <div class="flex-none text-green-700" x-show="connected">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16"
                     fill="currentColor"
                     class="bi bi-check-circle-fill" viewBox="0 0 16 16">
                  <path
                      d="M16 8A8 8 0 1 1 0 8a8 8 0 0 1 16 0zm-3.97-3.03a.75.75 0 0 0-1.08.022L7.477 9.417 5.384 7.323a.75.75 0 0 0-1.06 1.06L6.97 11.03a.75.75 0 0 0 1.079-.02l3.992-4.99a.75.75 0 0 0-.01-1.05z"/>
                </svg>
              </div>
              <div class="text-green-600 flex-auto" x-show="connected">
                <h3 class="text-base font-medium">Connected successfully!</h3>
              </div>
            <#--Connection failed message-->
              <div class="flex-none text-red-700" x-show="!connected">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-exclamation-circle-fill" viewBox="0 0 16 16">
                  <path d="M16 8A8 8 0 1 1 0 8a8 8 0 0 1 16 0zM8 4a.905.905 0 0 0-.9.995l.35 3.507a.552.552 0 0 0 1.1 0l.35-3.507A.905.905 0 0 0 8 4zm.002 6a1 1 0 1 0 0 2 1 1 0 0 0 0-2z"/>
                </svg>
              </div>
              <div class="text-red-800 flex-auto" x-show="!connected">
                <h3 class="text-base font-medium">Connection failed...</h3>
              </div>
            <#--Dismiss button-->
              <div class="text-green-800">
                <button x-on:click="showConnectionResults = false" type="button"
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
          <#--List of objects when connection is successful-->
            <div x-cloak x-show="connected">
              <h3 class="text-base mb-4 px-6 font-medium ">Objects found</h3>
              <div class="divide-y divide-gray-200">
                <template x-for="objectFound in objectsFoundOnS3Bucket">
                  <div class="flex items-center space-x-2 px-4 py-2 hover:bg-gray-100">
                    <div class="p-2">
                    <#-- Folder icon -->
                      <span class="text-blue-900 flex-none w-8 h-8 grid place-items-center" x-cloak x-show="objectFound.isFolder">
                                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="currentColor" class="bi bi-folder-fill" viewBox="0 0 16 16">
  <path d="M9.828 3h3.982a2 2 0 0 1 1.992 2.181l-.637 7A2 2 0 0 1 13.174 14H2.825a2 2 0 0 1-1.991-1.819l-.637-7a1.99 1.99 0 0 1 .342-1.31L.5 3a2 2 0 0 1 2-2h3.672a2 2 0 0 1 1.414.586l.828.828A2 2 0 0 0 9.828 3zm-8.322.12C1.72 3.042 1.95 3 2.19 3h5.396l-.707-.707A1 1 0 0 0 6.172 2H2.5a1 1 0 0 0-1 .981l.006.139z"/>
</svg>
                              </span>
                    <#-- File icon -->
                      <span class="text-gray-900 flex-none  w-8 h-8 grid place-items-center" x-cloak x-show="!objectFound.isFolder">
                                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="currentColor" class="bi bi-file-earmark" viewBox="0 0 16 16">
                                  <path d="M14 4.5V14a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V2a2 2 0 0 1 2-2h5.5L14 4.5zm-3 0A1.5 1.5 0 0 1 9.5 3V1H4a1 1 0 0 0-1 1v12a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1V4.5h-2z"/>
                                </svg>
                              </span>
                    </div>
                    <div class="flex-auto min-w-0">
                      <div class="truncate" x-text="objectFound.key"></div>
                      <div class="flex space-x-2 items-center">
                        <div class="text-sm font-medium">Owned by <strong
                            x-text="objectFound.ownerName"></strong></div>
                        <div class="text-sm font-medium" x-show="objectFound.sizeFormatted !== '0'">Size <strong
                            x-text="objectFound.sizeFormatted"></strong></div>
                      </div>
                    </div>
                  </div>
                </template>
              </div>
            </div>
          <#--Error message when connection is unsuccessful-->
            <div x-cloak x-show="!connected" class="bg-red-100 px-8 py-3 text-red-800">
              <h3 class="text-base font-medium ">Error details:</h3>
              <p class="text-sm" x-text:="errorResponse"></p>
            </div>
          </div>
        </div>

      </div>
    </div>
  </div>
</div>
