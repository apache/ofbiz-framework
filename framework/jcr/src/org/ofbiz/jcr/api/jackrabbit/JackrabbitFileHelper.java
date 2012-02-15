package org.ofbiz.jcr.api.jackrabbit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.GregorianCalendar;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.jcr.access.jackrabbit.ConstantsJackrabbit;
import org.ofbiz.jcr.access.jackrabbit.JackrabbitRepositoryAccessor;
import org.ofbiz.jcr.api.JcrFileHelper;
import org.ofbiz.jcr.orm.OfbizRepositoryMapping;
import org.ofbiz.jcr.orm.jackrabbit.JackrabbitFile;
import org.ofbiz.jcr.orm.jackrabbit.JackrabbitFolder;
import org.ofbiz.jcr.orm.jackrabbit.JackrabbitHierarchyNode;
import org.ofbiz.jcr.orm.jackrabbit.JackrabbitResource;
import org.ofbiz.jcr.util.jackrabbit.JcrUtilJackrabbit;

/**
 * This Helper class encapsulate the jcr file content bean. it provide all
 * attributes and operations which are necessary to work with the content
 * repository.
 *
 * The concrete implementations covers the different content use case related
 * workflows. I.E. Different behavior for File/Folder or Text content.
 *
 * The Helper classes should be build on top of the generic JCR implementation
 * in the Framework.
 *
 */
public class JackrabbitFileHelper extends JackrabbitAbstractHelper implements JcrFileHelper {

    private final static String module = JackrabbitFileHelper.class.getName();

    private JackrabbitHierarchyNode hierarchy = null;

    public JackrabbitFileHelper(GenericValue userLogin) {
        super(new JackrabbitRepositoryAccessor(userLogin));
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ofbiz.jcr.api.jackrabbit.FileHelper#getRepositoryContent(java.lang
     * .String)
     */
    @Override
    public JackrabbitHierarchyNode getRepositoryContent(String contentPath) throws ClassCastException, PathNotFoundException {
        return getRepositoryContent(contentPath, null);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ofbiz.jcr.api.jackrabbit.FileHelper#getRepositoryContent(java.lang
     * .String, java.lang.String)
     */
    @Override
    public JackrabbitHierarchyNode getRepositoryContent(String contentPath, String version) throws ClassCastException, PathNotFoundException {
        OfbizRepositoryMapping orm = null;
        if (version != null) {
            orm = super.access.getContentObject(contentPath, version);
        } else {
            orm = super.access.getContentObject(contentPath);
        }

        if (orm instanceof JackrabbitFile) {
            JackrabbitFile fileObj = (JackrabbitFile) orm;
            hierarchy = fileObj;
            return fileObj;
        } else if (orm instanceof JackrabbitFolder) {
            JackrabbitFolder fileObj = (JackrabbitFolder) orm;
            hierarchy = fileObj;
            return fileObj;
        }

        throw new ClassCastException("The content object for the path: " + contentPath + " is not a file content object. This Helper can only handle content objects with the type: " + JackrabbitFile.class.getName());
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ofbiz.jcr.api.jackrabbit.FileHelper#storeContentInRepository(byte[],
     * java.lang.String, java.lang.String)
     */
    @Override
    public void storeContentInRepository(byte[] fileData, String fileName, String folderPath) throws ObjectContentManagerException, RepositoryException {
        storeContentInRepository(new ByteArrayInputStream(fileData), fileName, folderPath);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ofbiz.jcr.api.jackrabbit.FileHelper#storeContentInRepository(java
     * .io.InputStream, java.lang.String, java.lang.String)
     */
    @Override
    public void storeContentInRepository(InputStream fileData, String fileName, String folderPath) throws ObjectContentManagerException, RepositoryException {
        if (UtilValidate.isEmpty(folderPath)) {
            throw new ObjectContentManagerException("Please specify a folder path, the folder path should not be empty!");
        } else if (ConstantsJackrabbit.ROOTPATH.equals(folderPath)) {
            throw new ObjectContentManagerException("Please specify a folder, a file content can't be stored directly under root.");
        }

        JackrabbitResource ormResource = createResource(fileData);

        JackrabbitFile ormFile = createFile(fileName, ormResource);

        // Create the folder if necessary, otherwise we just update the folder
        // content
        folderPath = JcrUtilJackrabbit.createAbsoluteNodePath(folderPath);
        if (super.access.checkIfNodeExist(folderPath)) {
            OfbizRepositoryMapping orm = super.access.getContentObject(folderPath);
            if (orm instanceof JackrabbitFolder) {
                JackrabbitFolder ormFolder = (JackrabbitFolder) orm;
                ormFolder.addChild(ormFile);
                super.access.updateContentObject(ormFolder);
            }
        } else {
            JackrabbitFolder ormFolder = createFolder(folderPath, ormFile);
            super.access.storeContentObject(ormFolder);
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see org.ofbiz.jcr.api.jackrabbit.FileHelper#isFileContent()
     */
    @Override
    public boolean isFileContent() {
        return (hierarchy instanceof JackrabbitFile);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ofbiz.jcr.api.jackrabbit.FileHelper#isFolderContent()
     */
    @Override
    public boolean isFolderContent() {
        return (hierarchy instanceof JackrabbitFolder);
    }

    private static String getMimeTypeFromInputStream(InputStream is) {
        if (!TikaInputStream.isTikaInputStream(is)) {
            is = TikaInputStream.get(is);
        }
        Tika tika = new Tika();
        try {
            return tika.detect(is);
        } catch (IOException e) {
            Debug.logError(e, module);
            return "application/octet-stream";
        }
    }

    /**
     * Creates a Jackrabbit Folder Object which should be stored in the
     * repository.
     *
     * @param folderPath
     * @param ormFile
     * @return
     */
    private JackrabbitFolder createFolder(String folderPath, JackrabbitFile ormFile) {
        // create the ORM folder Object
        JackrabbitFolder ormFolder = new JackrabbitFolder();
        ormFolder.addChild(ormFile);
        ormFolder.setPath(folderPath);
        return ormFolder;
    }

    /**
     * Creates a Jackrabbit File Object which is needed for a Jackrabbit Folder
     * Object.
     *
     * @param fileName
     * @param ormResource
     * @return
     */
    private JackrabbitFile createFile(String fileName, JackrabbitResource ormResource) {
        // create an ORM File Object
        JackrabbitFile ormFile = new JackrabbitFile();
        ormFile.setCreationDate(new GregorianCalendar());
        ormFile.setResource(ormResource);
        ormFile.setPath(fileName);
        return ormFile;
    }

    /**
     * Creates a Jackrabbit Resource Object which is needed for a Jackrabbit
     * File Object.
     *
     * @param fileData
     * @return
     */
    private JackrabbitResource createResource(InputStream fileData) {
        // create an ORM Resource Object
        JackrabbitResource ormResource = new JackrabbitResource();
        ormResource.setData(fileData);
        ormResource.setMimeType(getMimeTypeFromInputStream(fileData));
        ormResource.setLastModified(new GregorianCalendar());
        return ormResource;
    }

}
