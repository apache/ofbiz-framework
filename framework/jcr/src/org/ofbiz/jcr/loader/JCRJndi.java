package org.ofbiz.jcr.loader;

import javax.jcr.Repository;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;

import org.ofbiz.base.config.GenericConfigException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.JNDIContextFactory;

public class JCRJndi {

    public static final String module = JCRJndi.class.getName();

    public final static String ADDR_TYPE_FOR_REPOSITORY_HOME_DIR = "REPHOME";
    public final String ADDR_TYPE_FOR_CONFIG_FILE_PATH = "CONFPATH";

    private final String jndiName;
    private final String configFilePath;
    private final String repositoryHomeDir;

    public JCRJndi(String configFilePath, String jndiName, String repositoryHomeDir) {
        this.configFilePath = configFilePath;
        this.jndiName = jndiName;
        this.repositoryHomeDir = repositoryHomeDir;
    }

    public void registerJcrToJndi() {
        InitialContext jndiContext = null;

        try {
            jndiContext = getInitialContext();
        } catch (GenericConfigException e) {
            Debug.logError(e, module);
        }

        bindRepository(jndiContext);
        // Test JNDI bind
        RepositoryLoader.getRepository();
    }

    public void unbindRepository() {
        try {
            InitialContext jndiContext = getInitialContext();
            jndiContext.unbind(jndiName);
        } catch (NamingException e) {
            Debug.logError(e, module);
        } catch (GenericConfigException e) {
            Debug.logError(e, module);
        }
    }

    private InitialContext getInitialContext() throws GenericConfigException {
        return JNDIContextFactory.getInitialContext("default");
    }

    private void bindRepository(InitialContext jndiContext) {
        try {
            Reference ref = new Reference(Repository.class.getName(), org.ofbiz.jcr.loader.RepositoryFactory.class.getName(), null);
            ref.add(new StringRefAddr(ADDR_TYPE_FOR_REPOSITORY_HOME_DIR, repositoryHomeDir));
            ref.add(new StringRefAddr(ADDR_TYPE_FOR_CONFIG_FILE_PATH, configFilePath));
            jndiContext.bind(jndiName, ref);
            Debug.logInfo("Repository bound to JNDI as " + jndiName, module);
        } catch (NamingException ne) {
            Debug.logError(ne, module);
        }
    }
}
