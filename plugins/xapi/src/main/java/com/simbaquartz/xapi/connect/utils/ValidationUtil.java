package com.simbaquartz.xapi.connect.utils;

import org.apache.ofbiz.base.util.UtilValidate;

import java.util.regex.Pattern;

public class ValidationUtil {

    public static boolean isAlphaNumeric (String text, int size){
        String alphaNumeric ="[a-zA-Z0-9 ]";
        String range = "{1,"+size+"}";
        String regExp = "^"+alphaNumeric+range+"$";
        Pattern pattern = Pattern.compile(regExp, Pattern.CASE_INSENSITIVE);
        return pattern.matcher(text).matches();
    }

    public static boolean validateLength(String text, int size) {
        if (text.length() > size) {
            return true;
        }
        return false;
    }
}
