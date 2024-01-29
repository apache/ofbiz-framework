<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html lang="en">
<head>
  <meta http-equiv="Content-Type" content="text/html;charset=UTF-8">
  <title>Manage application settings here</title>
  <link href="https://unpkg.com/@tailwindcss/forms/dist/forms.min.css" rel="stylesheet">
  <script src="https://cdn-tailwindcss.vercel.app/"></script>
  <#include "../layout/commonStyles.ftl" />
  <style type="text/css">
    [type='text'], [type='email'], [type='url'], [type='password'], [type='number'], [type='date'], [type='datetime-local'], [type='month'], [type='search'], [type='tel'], [type='time'], [type='week'], [multiple], textarea, select {
      -webkit-appearance: none;
      -moz-appearance: none;
      appearance: none;
      padding-top: 0.5rem;
      padding-right: 0.75rem;
      padding-bottom: 0.5rem;
      padding-left: 0.75rem;
      font-size: 1rem;
      line-height: 1.5rem;
      --tw-shadow: 0 0 #0000;
    }

    [type='text']:focus, [type='email']:focus, [type='url']:focus, [type='password']:focus, [type='number']:focus, [type='date']:focus, [type='datetime-local']:focus, [type='month']:focus, [type='search']:focus, [type='tel']:focus, [type='time']:focus, [type='week']:focus, [multiple]:focus, textarea:focus, select:focus {
      outline: 2px solid transparent;
      outline-offset: 2px;
      --tw-ring-inset: var(--tw-empty, /*!*/ /*!*/);
      --tw-ring-offset-width: 0px;
      --tw-ring-offset-color: #fff;
      --tw-ring-color: #2563eb;
      --tw-ring-offset-shadow: var(--tw-ring-inset) 0 0 0 var(--tw-ring-offset-width) var(--tw-ring-offset-color);
      --tw-ring-shadow: var(--tw-ring-inset) 0 0 0 calc(1px + var(--tw-ring-offset-width)) var(--tw-ring-color);
      box-shadow: var(--tw-ring-offset-shadow), var(--tw-ring-shadow), var(--tw-shadow);
      border-color: #2563eb;
    }

    input::-moz-placeholder, textarea::-moz-placeholder {
      color: #6b7280;
      opacity: 1;
    }

    input:-ms-input-placeholder, textarea:-ms-input-placeholder {
      color: #6b7280;
      opacity: 1;
    }

    input::placeholder, textarea::placeholder {
      color: #6b7280;
      opacity: 1;
    }

    ::-webkit-datetime-edit-fields-wrapper {
      padding: 0;
    }

    ::-webkit-date-and-time-value {
      min-height: 1.5em;
    }

    select {
      background-image: url("data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 20 20'%3e%3cpath stroke='%236b7280' stroke-linecap='round' stroke-linejoin='round' stroke-width='1.5' d='M6 8l4 4 4-4'/%3e%3c/svg%3e");
      background-position: right 0.5rem center;
      background-repeat: no-repeat;
      background-size: 1.5em 1.5em;
      padding-right: 2.5rem;
      -webkit-print-color-adjust: exact;
      color-adjust: exact;
    }

    [multiple] {
      background-image: initial;
      background-position: initial;
      background-repeat: unset;
      background-size: initial;
      padding-right: 0.75rem;
      -webkit-print-color-adjust: unset;
      color-adjust: unset;
    }

    [type='checkbox']:focus, [type='radio']:focus {
      outline: 2px solid transparent;
      outline-offset: 2px;
      --tw-ring-inset: var(--tw-empty, /*!*/ /*!*/);
      --tw-ring-offset-width: 2px;
      --tw-ring-offset-color: #fff;
      --tw-ring-color: #2563eb;
      --tw-ring-offset-shadow: var(--tw-ring-inset) 0 0 0 var(--tw-ring-offset-width) var(--tw-ring-offset-color);
      --tw-ring-shadow: var(--tw-ring-inset) 0 0 0 calc(2px + var(--tw-ring-offset-width)) var(--tw-ring-color);
      box-shadow: var(--tw-ring-offset-shadow), var(--tw-ring-shadow), var(--tw-shadow);
    }

    [type='checkbox']:checked, [type='radio']:checked {
      border-color: transparent;
      background-color: currentColor;
      background-size: 100% 100%;
      background-position: center;
      background-repeat: no-repeat;
    }

    [type='checkbox']:checked {
      background-image: url("data:image/svg+xml,%3csvg viewBox='0 0 16 16' fill='white' xmlns='http://www.w3.org/2000/svg'%3e%3cpath d='M12.207 4.793a1 1 0 010 1.414l-5 5a1 1 0 01-1.414 0l-2-2a1 1 0 011.414-1.414L6.5 9.086l4.293-4.293a1 1 0 011.414 0z'/%3e%3c/svg%3e");
    }

    [type='radio']:checked {
      background-image: url("data:image/svg+xml,%3csvg viewBox='0 0 16 16' fill='white' xmlns='http://www.w3.org/2000/svg'%3e%3ccircle cx='8' cy='8' r='3'/%3e%3c/svg%3e");
    }

    [type='checkbox']:checked:hover, [type='checkbox']:checked:focus, [type='radio']:checked:hover, [type='radio']:checked:focus {
      border-color: transparent;
      background-color: currentColor;
    }

    [type='checkbox']:indeterminate {
      background-image: url("data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 16 16'%3e%3cpath stroke='white' stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M4 8h8'/%3e%3c/svg%3e");
      border-color: transparent;
      background-color: currentColor;
      background-size: 100% 100%;
      background-position: center;
      background-repeat: no-repeat;
    }

    [type='checkbox']:indeterminate:hover, [type='checkbox']:indeterminate:focus {
      border-color: transparent;
      background-color: currentColor;
    }

    [type='file'] {
      background: unset;
      border-color: inherit;
      border-width: 0;
      border-radius: 0;
      padding: 0;
      font-size: unset;
      line-height: inherit;
    }

    [type='file']:focus {
      outline: 1px auto -webkit-focus-ring-color;
    }
  </style>
</head>
<body class="font-sans">
<div class="min-h-full">
<#include "../layout/navbar.ftl" />
  <header class="bg-white shadow">
    <div class="max-w-7xl mx-auto py-6 px-4 sm:px-6 lg:px-8">
    <#--Breadcrumb-->
      <nav aria-label="Breadcrumb"
           class="flex items-center text-gray-500 text-sm font-medium space-x-2 whitespace-nowrap">
        <a href="<@ofbizUrl>main</@ofbizUrl>" class="hover:text-gray-900">
          Home
        </a>
        <svg aria-hidden="true" width="24" height="24" fill="none" class="flex-none text-gray-300">
          <path d="M10.75 8.75l3.5 3.25-3.5 3.25" stroke="currentColor" stroke-width="1.5"
                stroke-linecap="round" stroke-linejoin="round"></path>
        </svg>
        <a href="<@ofbizUrl>settings</@ofbizUrl>" aria-current="page"
           class="truncate hover:text-gray-900">
          Settings
        </a>
      </nav>
    <#--Heading-->
      <div class="mt-4">
        <h1 class="text-3xl font-bold text-gray-900">Application
          settings</h1>
        <p class="pt-2">Manage your application settings here. Changes made are live so be
          careful.</p>
      </div>
    </div>
  </header>
  <main>
    <div class="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
      <!-- Replace with your content -->
      <div>
      <#--Message bar-->
      <#if transactionMessage??>
        <div x-data="{ showMessageBar: true }" x-show="showMessageBar"
             class="bg-green-100 p-4 flex items-center justify-between rounded space-x-2 my-6">
          <div class="text-green-700">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                 class="bi bi-check-circle-fill" viewBox="0 0 16 16">
              <path
                  d="M16 8A8 8 0 1 1 0 8a8 8 0 0 1 16 0zm-3.97-3.03a.75.75 0 0 0-1.08.022L7.477 9.417 5.384 7.323a.75.75 0 0 0-1.06 1.06L6.97 11.03a.75.75 0 0 0 1.079-.02l3.992-4.99a.75.75 0 0 0-.01-1.05z"/>
            </svg>
          </div>
          <div class="text-green-800 flex-auto">
            <h3 class="font-semibold">Application settings have been updated successfully!</h3>
          </div>
          <div class="text-green-800">
            <button x-on:click="showMessageBar = false"
                    class="inline-flex items-center justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-red-600 hover:bg-red-700 hover:text-white">
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                   class="bi bi-x mr-2" viewBox="0 0 16 16">
                <path
                    d="M4.646 4.646a.5.5 0 0 1 .708 0L8 7.293l2.646-2.647a.5.5 0 0 1 .708.708L8.707 8l2.647 2.646a.5.5 0 0 1-.708.708L8 8.707l-2.646 2.647a.5.5 0 0 1-.708-.708L7.293 8 4.646 5.354a.5.5 0 0 1 0-.708z"/>
              </svg>
              Dismiss
            </button>
          </div>
        </div>
      </#if>

        <div x-data="formChangeTracker()">
          <form id="appSettingsForm" action="<@ofbizUrl>settings</@ofbizUrl>" method="POST">
            <input type="hidden" name="action" value="UPDATE"/>
            <#--Email settings-->
            <#include "./email/emailSettings.ftl" />
            <#--AWS storage settings-->
            <#include "./cloud/aws/storage/awsStorageSettings.ftl" />
            <#--Application settings-->
            <#--<#include "./app/appSettings.ftl" />-->
          </form>
        </div>
        <div class="hidden sm:block" aria-hidden="true">
          <div class="py-5">
            <div class="border-t border-gray-200"></div>
          </div>
        </div>
      </div>
      <!-- /End replace -->
    </div>
  </main>
</div>
<#include "../layout/scripts.ftl" />
</body>
</html>