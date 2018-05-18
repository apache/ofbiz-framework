package org.apache.ofbiz.content.ftp;

import org.apache.commons.io.IOUtils;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.client.subsystem.sftp.SftpClient;
import org.apache.ofbiz.base.util.UtilValidate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Basic client to copy files to an ssh ftp server
 */
public class SshFtpClient implements FtpClientInterface {

    public static final String module = SshFtpClient.class.getName();

    private SshClient client;
    private SftpClient sftp;

    public SshFtpClient() {
        client = SshClient.setUpDefaultClient();
        client.start();
    }

    @Override
    public void connect(String hostname, String username, String password, Long port, Long timeout) throws IOException {
        if (port == null) port = 22l;
        if (timeout == null) timeout = 10000l;

        if (sftp != null) return;
        ClientSession session = client.connect(username, hostname, port.intValue()).verify(timeout.intValue()).getSession();
        session.addPasswordIdentity(password);
        session.auth().verify(timeout.intValue());
        sftp = session.createSftpClient();
    }

    @Override
    public void copy(String path, String fileName, InputStream file) throws IOException {
        OutputStream os = sftp.write((UtilValidate.isNotEmpty(path) ? path + "/" : "") + fileName);
        IOUtils.copy(file, os);
        os.close();
    }

    @Override
    public List<String> list(String path) throws IOException {
        SftpClient.CloseableHandle handle = sftp.openDir((UtilValidate.isNotEmpty(path) ? path + "/" : ""));
        List<String> fileNames = new ArrayList<>();
        for (SftpClient.DirEntry dirEntry : sftp.listDir(handle)) {
            fileNames.add(dirEntry.getFilename());
        }
        return fileNames;
    }

    @Override
    public void setBinaryTransfer(boolean isBinary) throws IOException {
    }

    @Override
    public void setPassiveMode(boolean isPassive) throws IOException {
    }

    @Override
    public void closeConnection() {
        if (sftp != null) {
            client.stop();
            sftp = null;
        }
    }
}
