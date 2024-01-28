<#assign appDomainUrl = Static["com.simbaquartz.xcommon.util.AppConfigUtil"].getInstance(delegator).getConfig().getAppDomainUrl()>

<!DOCTYPE html>
<html lang="en">
<head>
  <!-- Title -->
  <title>Slack account connected successfully!</title>

  <!-- Required Meta Tags Always Come First -->
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">

  <!-- Favicon -->
  <link rel="shortcut icon" href="<@ofbizContentUrl>/favicon.ico</@ofbizContentUrl>">

  <!-- Google Fonts -->
  <link
      href="//fonts.googleapis.com/css2?family=Roboto:ital,wght@0,300;0,400;0,500;0,700;1,300;1,400;1,500;1,700&display=swap"
      rel="stylesheet">
  <link href="https://unpkg.com/tailwindcss@^2/dist/tailwind.min.css" rel="stylesheet">
  <style>
    @import url('//fonts.googleapis.com/css2?family=Roboto:ital,wght@0,300;0,400;0,500;0,700;1,300;1,400;1,500;1,700&display=swap');

    html, body {
      height: 100%;
      width: 100%;
    }

    h1 {
      font-size: 80px;
      font-weight: 300;
      font-family: 'Roboto', sans-serif;;
    }

    h1 strong {
      font-weight: 700;
    }
  </style>

</head>
<body>
<div class="h-full w-full">
  <input type="hidden" id="appDomainUrlValue" value="${appDomainUrl}" />
  <div class="flex flex-col items-center justify-center h-full mb-4 text-center p-4">
  <#--  Success image  -->
    <div class="flex items-center justify-center text-green-700 text-xxl mb-10">
      <svg xmlns="http://www.w3.org/2000/svg" width="84" height="84" fill="currentColor" class="bi bi-check-circle-fill" viewBox="0 0 16 16">
        <path d="M16 8A8 8 0 1 1 0 8a8 8 0 0 1 16 0zm-3.97-3.03a.75.75 0 0 0-1.08.022L7.477 9.417 5.384 7.323a.75.75 0 0 0-1.06 1.06L6.97 11.03a.75.75 0 0 0 1.079-.02l3.992-4.99a.75.75 0 0 0-.01-1.05z"/>
      </svg>
    </div>
    <div>
      <div class="mb-4">
        <h1 class="text-3xl text-center text-gray-700">Slack account connected successfully!</h1>
        <p>You and your team members will start receiving notifications from your connected account about new updates from your workspace.</p>
      </div>
    <#--  Continue to dashboard  -->
      <div class="flex items-center justify-center mt-4">
        <a href="" class="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded">
          Continue to dashboard at ${appDomainUrl}
        </a>
      </div>
    <#--  Auto redirect in 5 seconds  -->
      <div class="flex items-center justify-center mt-4">
        <p class="text-gray-500 text-sm">You will be redirected to dashboard in 5 seconds.</p>
        <script>
          var url = document.getElementById("appDomainUrlValue").value;
          setTimeout(function () {
            window.location.href = url;
          }, 5000);
        </script>
      </div>
      <script src="https://cdn.jsdelivr.net/gh/alpinejs/alpine@v2.x.x/dist/alpine.min.js" defer></script>
</body>
</html>