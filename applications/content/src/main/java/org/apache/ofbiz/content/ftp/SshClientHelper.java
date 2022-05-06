package org.apache.ofbiz.content.ftp;

import org.apache.sshd.client.ClientBuilder;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.config.keys.ClientIdentityLoader;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.kex.BuiltinDHFactories;
import org.apache.sshd.common.signature.BuiltinSignatures;

import java.util.ArrayList;

public abstract class SshClientHelper {

    private static SshClient client = null;

    public static SshClient getSshClient() {
        if (client == null) {
            client = SshClient.setUpDefaultClient();
            client.setClientIdentityLoader(ClientIdentityLoader.DEFAULT);
            client.setKeyExchangeFactories(NamedFactory.setUpTransformedFactories(
                    false,
                    BuiltinDHFactories.VALUES,
                    ClientBuilder.DH2KEX));
            client.setSignatureFactories(new ArrayList<>(BuiltinSignatures.VALUES));
        }
        if (!client.isStarted()) {
            client.start();
        }
        return client;
    }

}
