package org.apache.ofbiz.content.ftp;

import org.apache.ofbiz.base.util.GeneralException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface FtpClientInterface {

    /**
     * Initialization of a file transfer client, and connection to the given server
     *
     * @param hostname hostname to connect to
     * @param username username to login with
     * @param password password to login with
     * @param port     port to connect to the server, optional
     * @param timeout  timeout for connection process, optional
     * @throws IOException
     */
    void connect(String hostname, String username, String password, Long port, Long timeout) throws IOException, GeneralException;

    /**
     * Copy of the give file to the connected server into the path.
     *
     * @param path     path to copy the file to
     * @param fileName name of the copied file
     * @param file     data to copy
     * @throws IOException
     */
    void copy(String path, String fileName, InputStream file) throws IOException;

    List<String> list(String path) throws IOException;

    void setBinaryTransfer(boolean isBinary) throws IOException;

    void setPassiveMode(boolean isPassive) throws IOException;

    /**
     * Close opened connection
     */
    void closeConnection() throws IOException;
}
