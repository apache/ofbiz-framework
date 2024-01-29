package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;


/**
 * Represents a Client Details object.
 **/

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2017-07-07T13:00:22.061+05:30")
public class ClientDetail   {
  
  private String acceptLanguage = null;
  private String browserHeight = null;
  private String browserIp = null;
  private String browserWidth = null;
  private String sessionHash = null;
  private String userAgent = null;

  /**
   * Language which accepts.
   **/
  
  @JsonProperty("accept_language")
  public String getAcceptLanguage() {
    return acceptLanguage;
  }
  public void setAcceptLanguage(String acceptLanguage) {
    this.acceptLanguage = acceptLanguage;
  }

  /**
   * The browser screen height in pixels, if available.
   **/
  
  @JsonProperty("browser_height")
  public String getBrowserHeight() {
    return browserHeight;
  }
  public void setBrowserHeight(String browserHeight) {
    this.browserHeight = browserHeight;
  }

  /**
   * The browser IP address.
   **/
  
  @JsonProperty("browser_ip")
  public String getBrowserIp() {
    return browserIp;
  }
  public void setBrowserIp(String browserIp) {
    this.browserIp = browserIp;
  }

  /**
   * The browser screen width in pixels, if available.
   **/
  
  @JsonProperty("browser_width")
  public String getBrowserWidth() {
    return browserWidth;
  }
  public void setBrowserWidth(String browserWidth) {
    this.browserWidth = browserWidth;
  }

  /**
   * A hash of the session.
   **/
  
  @JsonProperty("session_hash")
  public String getSessionHash() {
    return sessionHash;
  }
  public void setSessionHash(String sessionHash) {
    this.sessionHash = sessionHash;
  }

  /**
   * User agent.
   **/
  
  @JsonProperty("user_agent")
  public String getUserAgent() {
    return userAgent;
  }
  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClientDetail clientDetail = (ClientDetail) o;
    return Objects.equals(acceptLanguage, clientDetail.acceptLanguage) &&
        Objects.equals(browserHeight, clientDetail.browserHeight) &&
        Objects.equals(browserIp, clientDetail.browserIp) &&
        Objects.equals(browserWidth, clientDetail.browserWidth) &&
        Objects.equals(sessionHash, clientDetail.sessionHash) &&
        Objects.equals(userAgent, clientDetail.userAgent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(acceptLanguage, browserHeight, browserIp, browserWidth, sessionHash, userAgent);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ClientDetail {\n");
    
    sb.append("    acceptLanguage: ").append(toIndentedString(acceptLanguage)).append("\n");
    sb.append("    browserHeight: ").append(toIndentedString(browserHeight)).append("\n");
    sb.append("    browserIp: ").append(toIndentedString(browserIp)).append("\n");
    sb.append("    browserWidth: ").append(toIndentedString(browserWidth)).append("\n");
    sb.append("    sessionHash: ").append(toIndentedString(sessionHash)).append("\n");
    sb.append("    userAgent: ").append(toIndentedString(userAgent)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

