package org.ofbiz.jcr.access.jackrabbit;

import javax.jcr.nodetype.NodeType;

public class ConstantsJackrabbit {
    // JCR Variables
    public static String MESSAGE = "jcr:message";
    public static String FILE = NodeType.NT_FILE;
    public static String FOLDER = NodeType.NT_FOLDER;
    public static String RESOURCE = NodeType.NT_RESOURCE;
    public static String DATA = "jcr:data";
    public static String UNSTRUCTURED = NodeType.NT_UNSTRUCTURED;
    public static String MIMETYPE = "jcr:mimeType";
    public static String MIXIN_LANGUAGE = "mix:language";
    public static String MIXIN_VERSIONING = "mix:versionable";
    public static String ROOTVERSION = "jcr:rootVersion";

    //
    public static String ROOTPATH = "/";
    public static String FILEROOT = ROOTPATH + "fileHome";
    public static String NODEPATHDELIMITER = "/";
}
