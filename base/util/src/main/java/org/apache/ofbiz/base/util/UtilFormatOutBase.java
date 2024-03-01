package org.apache.ofbiz.base.util;

public class UtilFormatOutBase {
    /**
     * Encodes an XML string replacing the characters '&lt;', '&gt;', '&quot;', '&#39;', '&amp;'
     * @param inString The plain value String
     * @return The encoded String
     */
    public static String encodeXmlValue(String inString) {
        String retString = inString;

        retString = StringUtil.replaceString(retString, "&", "&amp;");
        retString = StringUtil.replaceString(retString, "<", "&lt;");
        retString = StringUtil.replaceString(retString, ">", "&gt;");
        retString = StringUtil.replaceString(retString, "\"", "&quot;");
        retString = StringUtil.replaceString(retString, "'", "&apos;");
        return retString;
    }
}
