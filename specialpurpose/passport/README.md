<img src="http://ofbiz.apache.org/images/logo.png" alt="Apache OFBiz" />

# Passport component
The passport component is to support users to login with a third party OAuth2 authentication such as GitHub, LinkedIn and etc.


## More information
---------------------------------------
GitHub OAuth2 Configuration Quick Start
---------------------------------------
To learn the whole GitHub OAuth, please read https://developer.github.com/v3/oauth/.
Here are the steps for a quick start in OFBiz trunk:

------ GitHub Side ------
1. Register a GitHub account if you don't have one.

2. Login GitHub.

3. Visit Settings->OAuth applications->Developer applications, click "Register new application" button.

4. In "Register a new OAuth application", fill in your OFBiz website information. Here the 
Authorization callback URL: https://YourWebSiteOrInternetIp:8443/ecomseo/githubResponse
[Note] As you want to use GitHub to authorize your users, your OFBiz should be able to be visited on internet so that GitHub can forward the users back after OAuth login.

5. Click "Register application" button.
You'll get your Client ID and Client Secret, write them down for later configuration.

------ OFBiz Side ------
6. Edit specialpurpose/passport/data/OAuth2GitHubSeedData.xml, fill in the Client ID in clientId, Client Secret in clientSecret and the Authorization callback URL in returnUrl.

7. Login to OFBiz webtools, in "XML Data Import" page, import specialpurpose/passport/data/OAuth2GitHubSeedData.xml.

8. Edit specialpurpose/ecommerce/webapp/ecomseo/WEB-INF/controller.xml, uncomment 
   <include location="component://passport/webapp/passport/WEB-INF/controller-passport.xml"/>

9. Edit specialpurpose/ecommerce/widget/CommonScreens.xml, uncomment
   <include-screen name="ListThirdPartyLogins" location="component://passport/widget/PassportScreens.xml"/>

10. Visit https://YourWebSiteOrInternetIp:8443/ecomseo/checkLogin, click GitHub icon in the "Third Party Login" area. You will be forwarded to GitHub OAuth2 login page. After logging in successfully, you will be logged in OFBiz.

11. The parameters in specialpurpose/passport/config/gitHubAuth.properties:
github.env.prefix: test or live, default is test. This value will be store in envPrefix field of GitHubUser entity when a user logged in by GitHub OAuth2.
github.authenticator.enabled: true or false, default is true. This parameter is for future extension.



-----------------------------------------
LinkedIn OAuth2 Configuration Quick Start
-----------------------------------------
To learn the whole LinkedIn OAuth, please read https://developer.linkedin.com/docs/oauth2.
Here are the steps for a quick start in OFBiz trunk:

------ LinkedIn Side ------
1. Register a LinkedIn account if you don't have one.

2. Visit https://www.linkedin.com/developer/apps, login LinkedIn.

3. Click "Create Application" button.

4. In "Create a New Application", fill in your OFBiz website information. Here the 
Authorization callback URL: https://YourWebSiteOrInternetIp:8443/ecomseo/linkedInResponse
[Note] As you want to use LinkedIn to authorize your users, your OFBiz should be able to be visited on internet so that LinkedIn can forward the users back after OAuth login.

5. Click "Submit" button.
You'll get your Client ID and Client Secret, write them down for later configuration. You should also select r_basicprofile and r_emailaddress in "Default Application Permissions" section.

6. In "Application Settings" page, you can select Application Status to be Development or Live.

------ OFBiz Side ------
7. Edit specialpurpose/passport/data/OAuth2LinkedInSeedData.xml, fill in the Client ID in apiKey, Client Secret in secretKey and the Authorization callback URL in testReturnUrl and/or liveReturnUrl.

8. Login to OFBiz webtools, in "XML Data Import" page, import specialpurpose/passport/data/OAuth2LinkedInSeedData.xml.

9. Edit specialpurpose/ecommerce/webapp/ecomseo/WEB-INF/controller.xml, uncomment 
   <include location="component://passport/webapp/passport/WEB-INF/controller-passport.xml"/>

10. Edit specialpurpose/ecommerce/widget/CommonScreens.xml, uncomment
   <include-screen name="ListThirdPartyLogins" location="component://passport/widget/PassportScreens.xml"/>

11. Visit https://YourWebSiteOrInternetIp:8443/ecomseo/checkLogin, click LinkedIn icon in the "Third Party Login" area. You will be forwarded to LinkedIn OAuth2 login page. After logging in successfully, you will be logged in OFBiz.

12. The parameters in specialpurpose/passport/config/linkedInAuth.properties:
linkedin.env.prefix: test or live, default is test. This value will be store in envPrefix field of LinkedInUser entity when a user logged in by LinkedIn OAuth2.
                     If the value is test, you should set your Application Status to Development, and the "Authorized Redirect URLs" to the same as the testReturnUrl.
                     If the value is live, you should set your Application Status to Live, and the "Authorized Redirect URLs" to the same as the liveReturnUrl.
linkedin.authenticator.enabled: true or false, default is true. This parameter is for future extension.


---------------
     Q&A
---------------
Q. I think my configures are right, why there's no third party login in my /ecomseo/checkLogin page?
A. Please check whether the productStoreId in ThirdPartyLogin and OAuth2GitHub/OAuth2LinkedIn is the same as your web store. 

Q. How could I extend this component to support other OAuth2 providers?
A. 1st, you have to read the providers OAuth2 documents. 2nd, add some new entities. 3rd, add seed data. 4th, add the icon file under specialpurpose/passport/webapp/passport/images/.
   5th, add redirect and response request in controller-passport.xml. 6th, add java files to implement the redirect and response.

