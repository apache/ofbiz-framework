package com.simbaquartz.xapi.connect.models.errors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;


/**
 * Represents an error encountered during a request to the Connect API. See [Handling errors](#handlingerrors) for more information.
 **/

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2017-05-09T10:47:15.854-07:00")
public class Error {


  /**
   * The error's high-level category. See [ErrorCategory](#type-errorcategory) for possible values.
   */
  public enum CategoryEnum {
    API_ERROR("API_ERROR"),

    AUTHENTICATION_ERROR("AUTHENTICATION_ERROR"),

    INVALID_REQUEST_ERROR("INVALID_REQUEST_ERROR"),

    RATE_LIMIT_ERROR("RATE_LIMIT_ERROR");
    private String value;

    CategoryEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }
  }

  private CategoryEnum category = null;

  /**
   * The error's specific code. See [ErrorCode](#type-errorcode) for possible values
   */
  public enum CodeEnum {
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR"),

    UNAUTHORIZED("UNAUTHORIZED"),

    ACCESS_TOKEN_EXPIRED("ACCESS_TOKEN_EXPIRED"),

    ACCESS_TOKEN_MISSING("ACCESS_TOKEN_MISSING"),

    ACCESS_TOKEN_REVOKED("ACCESS_TOKEN_REVOKED"),

    FORBIDDEN("FORBIDDEN"),

    INSUFFICIENT_SCOPES("INSUFFICIENT_SCOPES"),

    APPLICATION_DISABLED("APPLICATION_DISABLED"),

    BAD_REQUEST("BAD_REQUEST"),

    MISSING_REQUIRED_PARAMETER("MISSING_REQUIRED_PARAMETER"),

    INCORRECT_TYPE("INCORRECT_TYPE"),

    INVALID_TIME("INVALID_TIME"),

    INVALID_TIME_RANGE("INVALID_TIME_RANGE"),

    INVALID_VALUE("INVALID_VALUE"),

    INVALID_CURSOR("INVALID_CURSOR"),

    UNKNOWN_QUERY_PARAMETER("UNKNOWN_QUERY_PARAMETER"),

    CONFLICTING_PARAMETERS("CONFLICTING_PARAMETERS"),

    EXPECTED_JSON_BODY("EXPECTED_JSON_BODY"),

    INVALID_SORT_ORDER("INVALID_SORT_ORDER"),

    VALUE_REGEX_MISMATCH("VALUE_REGEX_MISMATCH"),

    VALUE_TOO_SHORT("VALUE_TOO_SHORT"),

    VALUE_TOO_LONG("VALUE_TOO_LONG"),

    VALUE_TOO_LOW("VALUE_TOO_LOW"),

    VALUE_TOO_HIGH("VALUE_TOO_HIGH"),

    VALUE_EMPTY("VALUE_EMPTY"),

    ARRAY_EMPTY("ARRAY_EMPTY"),

    EXPECTED_BOOLEAN("EXPECTED_BOOLEAN"),

    EXPECTED_INTEGER("EXPECTED_INTEGER"),

    EXPECTED_FLOAT("EXPECTED_FLOAT"),

    EXPECTED_STRING("EXPECTED_STRING"),

    EXPECTED_OBJECT("EXPECTED_OBJECT"),

    EXPECTED_ARRAY("EXPECTED_ARRAY"),

    EXPECTED_BASE64_ENCODED_BYTE_ARRAY("EXPECTED_BASE64_ENCODED_BYTE_ARRAY"),

    INVALID_ARRAY_VALUE("INVALID_ARRAY_VALUE"),

    INVALID_ENUM_VALUE("INVALID_ENUM_VALUE"),

    INVALID_CONTENT_TYPE("INVALID_CONTENT_TYPE"),

    INVALID_FORM_VALUE("INVALID_FORM_VALUE"),

    ONE_INSTRUMENT_EXPECTED("ONE_INSTRUMENT_EXPECTED"),

    NO_FIELDS_SET("NO_FIELDS_SET"),

    ACCOUNT_LOCKED("ACCOUNT_LOCKED"),

    ACCOUNT_NOT_FOUND("ACCOUNT_NOT_FOUND"),

    ACCOUNT_ALREADY_EXISTS("ACCOUNT_ALREADY_EXISTS"),

    INVALID_PASSWORD("INVALID_PASSWORD"),

    INVALID_CARD_DATA("INVALID_CARD_DATA"),

    IDEMPOTENCY_KEY_REUSED("IDEMPOTENCY_KEY_REUSED"),

    UNEXPECTED_VALUE("UNEXPECTED_VALUE"),

    NOT_FOUND("NOT_FOUND"),

    REQUEST_TIMEOUT("REQUEST_TIMEOUT"),

    CONFLICT("CONFLICT"),

    REQUEST_ENTITY_TOO_LARGE("REQUEST_ENTITY_TOO_LARGE"),

    UNSUPPORTED_MEDIA_TYPE("UNSUPPORTED_MEDIA_TYPE"),

    RATE_LIMITED("RATE_LIMITED"),

    NOT_IMPLEMENTED("NOT_IMPLEMENTED"),

    SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE"),

    ALREADY_VERIFIED("ALREADY_VERIFIED"),

    DUPLICATE_CONTENT("DUPLICATE_CONTENT"),

    TENANT_NOT_FOUND("TENANT_NOT_FOUND");

    private String value;

    CodeEnum(String value) {
      this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
      return String.valueOf(value);
    }
  }

  private CodeEnum code = null;
  private String message = null;
  private String field = null;

  public Error(CodeEnum errorCode, CategoryEnum errorCategory, String errorMessage, String field) {
    this.code = errorCode;
    this.category = errorCategory;
    this.message = errorMessage;
    this.field = field;
  }

  /**
   * The error's high-level category. See [ErrorCategory](#type-errorcategory) for possible values.
   **/

  @JsonProperty("category")
  public CategoryEnum getCategory() {
    return category;
  }

  public void setCategory(CategoryEnum category) {
    this.category = category;
  }

  /**
   * The error's specific code. See [ErrorCode](#type-errorcode) for possible values
   **/

  @JsonProperty("code")
  public CodeEnum getCode() {
    return code;
  }

  public void setCode(CodeEnum code) {
    this.code = code;
  }

  /**
   * A human-readable description of the error for debugging purposes.
   **/

  @JsonProperty("message")
  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  /**
   * The name of the field provided in the original request that the error pertains to, if any.
   **/

  @JsonProperty("field")
  public String getField() {
    return field;
  }

  public void setField(String field) {
    this.field = field;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Error error = (Error) o;
    return Objects.equals(category, error.category) &&
            Objects.equals(code, error.code) &&
            Objects.equals(message, error.message) &&
            Objects.equals(field, error.field);
  }

  @Override
  public int hashCode() {
    return Objects.hash(category, code, message, field);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Error {\n");

    sb.append("    category: ").append(toIndentedString(category)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
    sb.append("    field: ").append(toIndentedString(field)).append("\n");
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

