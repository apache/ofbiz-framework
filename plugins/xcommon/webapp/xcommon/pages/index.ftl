<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html lang="en">
<head>
  <meta http-equiv="Content-Type" content="text/html;charset=UTF-8">
  <title>Welcome to common component</title>
  <link href="https://unpkg.com/@tailwindcss/forms/dist/forms.min.css" rel="stylesheet">
  <script src="https://cdn-tailwindcss.vercel.app/"></script>
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
<#include "./layout/navbar.ftl" />
  <main>
    <div class="max-w-7xl mx-auto p-3 sm:p-6 lg:px-8 grid place-items-center">
      <!-- Replace with your content -->
      <div class="border rounded-lg mb-4 shadow max-w-lg">
        <div class="p-4">
        <h1 class="order-1 text-gray-900 text-3xl font-semibold tracking-tight mt-2">Welcome to
          common
          component</h1>
        <p class="pt-2">This component offers common utilities and configuration options for your
          application settings</p>
        </div>
        <div class="my-3">
          <a href="<@ofbizUrl>settings</@ofbizUrl>" class="p-4 text-blue-600 hover:underline hover:bg-gray-100 px-3 py-2 inline-flex w-full">
            <div class="w-full flex items-center justify-between space-x-2">
              <div class="bg-gray-100 rounded-md text-gray-800 grid place-items-center w-10 h-10">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16"
                     fill="currentColor" class="bi bi-gear" viewBox="0 0 16 16">
                  <path
                      d="M8 4.754a3.246 3.246 0 1 0 0 6.492 3.246 3.246 0 0 0 0-6.492zM5.754 8a2.246 2.246 0 1 1 4.492 0 2.246 2.246 0 0 1-4.492 0z"/>
                  <path
                      d="M9.796 1.343c-.527-1.79-3.065-1.79-3.592 0l-.094.319a.873.873 0 0 1-1.255.52l-.292-.16c-1.64-.892-3.433.902-2.54 2.541l.159.292a.873.873 0 0 1-.52 1.255l-.319.094c-1.79.527-1.79 3.065 0 3.592l.319.094a.873.873 0 0 1 .52 1.255l-.16.292c-.892 1.64.901 3.434 2.541 2.54l.292-.159a.873.873 0 0 1 1.255.52l.094.319c.527 1.79 3.065 1.79 3.592 0l.094-.319a.873.873 0 0 1 1.255-.52l.292.16c1.64.893 3.434-.902 2.54-2.541l-.159-.292a.873.873 0 0 1 .52-1.255l.319-.094c1.79-.527 1.79-3.065 0-3.592l-.319-.094a.873.873 0 0 1-.52-1.255l.16-.292c.893-1.64-.902-3.433-2.541-2.54l-.292.159a.873.873 0 0 1-1.255-.52l-.094-.319zm-2.633.283c.246-.835 1.428-.835 1.674 0l.094.319a1.873 1.873 0 0 0 2.693 1.115l.291-.16c.764-.415 1.6.42 1.184 1.185l-.159.292a1.873 1.873 0 0 0 1.116 2.692l.318.094c.835.246.835 1.428 0 1.674l-.319.094a1.873 1.873 0 0 0-1.115 2.693l.16.291c.415.764-.42 1.6-1.185 1.184l-.291-.159a1.873 1.873 0 0 0-2.693 1.116l-.094.318c-.246.835-1.428.835-1.674 0l-.094-.319a1.873 1.873 0 0 0-2.692-1.115l-.292.16c-.764.415-1.6-.42-1.184-1.185l.159-.291A1.873 1.873 0 0 0 1.945 8.93l-.319-.094c-.835-.246-.835-1.428 0-1.674l.319-.094A1.873 1.873 0 0 0 3.06 4.377l-.16-.292c-.415-.764.42-1.6 1.185-1.184l.292.159a1.873 1.873 0 0 0 2.692-1.115l.094-.319z"/>
                </svg>
              </div>
              <div class="flex-auto">
                Manage
                application settings
              </div>
              <div class="flex-none">
                <span class="inline ml-2"><svg
                    xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                    class="bi bi-arrow-right-circle inline" viewBox="0 0 16 16">
                  <path fill-rule="evenodd"
                        d="M1 8a7 7 0 1 0 14 0A7 7 0 0 0 1 8zm15 0A8 8 0 1 1 0 8a8 8 0 0 1 16 0zM4.5 7.5a.5.5 0 0 0 0 1h5.793l-2.147 2.146a.5.5 0 0 0 .708.708l3-3a.5.5 0 0 0 0-.708l-3-3a.5.5 0 1 0-.708.708L10.293 7.5H4.5z"/>
                </svg></span>
              </div>

            </div>
          </a>
        </div>
      </div>
      <!-- /End replace -->
    </div>
  </main>
</div>
<#include "./layout/scripts.ftl" />
</body>
</html>