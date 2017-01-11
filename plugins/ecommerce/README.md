<img src="http://ofbiz.apache.org/images/logo.png" alt="Apache OFBiz" />

# Ecommerce component

## more information
How to use Janrain Engage Social Login.

1.Get API Key from http://www.janrain.com/products/engage/social-login.
2.Configure file setting : ecommerce.properties you can put it 
          Example:
          --------------------------------------------------------------------------------
            # -- Enable janrain engage (Y/ N) default N
            janrain.enabled=N
            
            # -- Janrain api key (secret)
            janrain.apiKey=exampleKey
            
            # -- Janrain application domain
            janrain.baseUrl=https://example.rpxnow.com
            
            # -- Janrain application name
            janrain.appName=exampleAppName
          --------------------------------------------------------------------------------
3.Restart the server.


How to test Janrain Engage Social Login.
=======================================

1. Go to Login screen.
2. Look the Social Login Widget and you can use these existing accounts to sign-in to your website.
3. First time if account does not exists then system will create new account.
4. After account existing in the system you can use "Social Login Widget" to login account.

===================================================================================================
