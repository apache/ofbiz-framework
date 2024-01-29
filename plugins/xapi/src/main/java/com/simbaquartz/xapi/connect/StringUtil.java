package com.simbaquartz.xapi.connect;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2017-05-09T10:47:15.854-07:00")
public class StringUtil {
  /**
   * Check if the given array contains the given value (with case-insensitive comparison).
   *
   * @param array The array
   * @param value The value to search
   * @return true if the array contains the value
   */
  public static boolean containsIgnoreCase(String[] array, String value) {
    for (String str : array) {
      if (value == null && str == null) return true;
      if (value != null && value.equalsIgnoreCase(str)) return true;
    }
    return false;
  }

  /**
   * Join an array of strings with the given separator.
   * <p>
   * Note: This might be replaced by utility method from commons-lang or guava someday
   * if one of those libraries is added as dependency.
   * </p>
   *
   * @param array     The array of strings
   * @param separator The separator
   * @return the resulting string
   */
  public static String join(String[] array, String separator) {
    int len = array.length;
    if (len == 0) return "";

    StringBuilder out = new StringBuilder();
    out.append(array[0]);
    for (int i = 1; i < len; i++) {
      out.append(separator).append(array[i]);
    }
    return out.toString();
  }

  public static String toCamelCase(String inputString) {
    String result = "";
    if (inputString.length() == 0) {
      return result;
    }
    char firstChar = inputString.charAt(0);
    char firstCharToUpperCase = Character.toLowerCase(firstChar);
    result = result + firstCharToUpperCase;
    for (int i = 1; i < inputString.length(); i++) {
      char currentChar = inputString.charAt(i);
      char previousChar = inputString.charAt(i - 1);
      if (previousChar == ' ') {
        char currentCharToUpperCase = Character.toUpperCase(currentChar);
        result = result + currentCharToUpperCase;
      } else {
        char currentCharToLowerCase = Character.toLowerCase(currentChar);
        result = result + currentCharToLowerCase;
      }
    }
    if (result.contains("/")) {
      result = result.substring(0, result.indexOf("/"));
    }
    return result.replaceAll("\\s+","");
  }
}
