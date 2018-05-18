package org.apache.ofbiz.content.ftp;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class SecureFtpClient implements FtpClientInterface {

    public static final String module = SecureFtpClient.class.getName();

    /**
     * TODO : to implements
     */
    @Override
    public void connect(String hostname, String username, String password, Long port, Long timeout) throws IOException {

    }

    @Override
    public void copy(String path, String fileName, InputStream file) throws IOException {

    }

    @Override
    public List<String> list(String path) throws IOException {
        return null;
    }

    @Override
    public void setBinaryTransfer(boolean isBinary) throws IOException {

    }

    @Override
    public void setPassiveMode(boolean isPassive) throws IOException {

    }

    @Override
    public void closeConnection() {

    }
}
