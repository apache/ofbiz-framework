package com.fidelissd.zcp.xcommon.config;

public class AppConfiguration {

  /**
   * Primary party id that is use to hold application level configuration, name, logo, this is then
   * used to extract logo and other relevant information so it can be changed to meet different
   * application needs. Type data is stored in plugins/xcommon/data/XcommonAppData.xml
   */
  public static final String APP_PARTY_ID = "SAAS_APP";

  /**
   * Primary Super admin userLoginId for the application, used to store UserPreferences and create a
   * master login account for super admin. Type data is stored in
   * plugins/xcommon/data/XcommonAppData.xml
   */
  public static final String APP_USER_LOGIN_ID = "app-super-admin";

  /**
   * Content type id used to associate logo content with the app party. Type data is stored in
   * plugins/xcommon/data/XcommonAppData.xml
   */
  public static final String APP_LOGO_TYPE_ID = "APP_LOGO";

  public static final String APP_LOGO_ID = "APP_LOGO";

  /**
   * Content type id used to associate email logo, can be same as logo or different based on type of
   * logo. Type data is stored in plugins/xcommon/data/XcommonAppData.xml
   */
  public static final String APP_LOGO_EMAIL_TYPE_ID = "APP_ELOGO";

  public static final String APP_LOGO_EMAIL_ID = "APP_ELOGO";

  /**
   * Content type id used to associate email footer logo, can be same as logo, grayscale or
   * different based on type of logo. Type data is stored in plugins/xcommon/data/XcommonAppData.xml
   */
  public static final String APP_LOGO_EMAIL_FOOTER_TYPE_ID = "APP_EFLOGO";

  public static final String APP_LOGO_EMAIL_FOOTER_ID = "APP_EFLOGO";

  public static final String APP_EMAIL_ID = "APP_EMAIL";
  public static final String APP_PHONE_ID = "APP_PHONE";
  public static final String APP_ADDRESS_ID = "APP_ADDRESS";

  public static final String APP_WEBSITE_ID = "APP_WEB_LINK";
  public static final String APP_WEBSITE_PRIVACY_ID = "APP_WEB_LINK_PRVCY";
  public static final String APP_WEBSITE_HELP_CENTER_ID = "APP_WEB_LINK_HC";
  public static final String APP_WEBSITE_TERMS_ID = "APP_WEB_LINK_TERMS";
  public static final String APP_WEBSITE_CONTACT_ID = "APP_WEB_LINK_CNTCT";

  /**
   * Contact mech id for social media links Type data is stored in
   * plugins/xcommon/data/XcommonAppData.xml
   */
  public static final String APP_SOCIAL_LINKEDIN_ID = "APP_LINKEDIN";

  public static final String APP_SOCIAL_FACEBOOK_ID = "APP_FACEBOOK";
  public static final String APP_SOCIAL_INSTAGRAM_ID = "APP_INSTAGRAM";
  public static final String APP_SOCIAL_TWITTER_ID = "APP_TWITTER";
  public static final String APP_SOCIAL_YOUTUBE_ID = "APP_YOUTUBE";
}
