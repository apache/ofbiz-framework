package org.ofbiz.jcr.loader;

import java.util.Map;

import javax.jcr.Repository;
import javax.naming.NamingException;

import javolution.util.FastMap;

import org.ofbiz.base.config.GenericConfigException;
import org.ofbiz.base.config.ResourceLoader;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.JNDIContextFactory;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.w3c.dom.Element;

public class RepositoryLoader {
    public static final String module = RepositoryLoader.class.getName();
    private static Map<String, Repository> repositories = loadRepositories();

    public static Repository getRepository(String name) {
        return repositories.get(name);
    }

    public static Repository getRepository() {
        return repositories.get("default");
    }

    private static Repository createFromFactory(String repositoryName, ClassLoader loader, String className) {
        try {
            JCRFactory factory = (JCRFactory) loader.loadClass(className).newInstance();
            return factory.getInstance();
        } catch (InstantiationException e) {
            Debug.logError(e, module);
        } catch (IllegalAccessException e) {
            Debug.logError(e, module);
        } catch (ClassNotFoundException e) {
            Debug.logError(e, module);
        }

        return null;
    }

    private static Map<String, Repository> loadRepositories() {
        Map<String, Repository> repos = FastMap.newInstance();

        String configFilePath = JCRContainer.getConfigFilePath();
        Element configRootElement = null;
        try {
            configRootElement = ResourceLoader.getXmlRootElement(configFilePath);
        } catch (GenericConfigException e) {
            Debug.logError(e, "Could not load the jcr configuration in file " + configFilePath, module);
        }

        if (configRootElement == null) {
            Debug.logError("No jcr configuration found in file " + configFilePath, module);
        }

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        // find the default JCR implementations
        for (Element curElement : UtilXml.childElementList(configRootElement, "jcr")) {
            String name = curElement.getAttribute("name");

            // first check if the found repository is already in the repository
            // map
            if (repos.get(name) != null && (repos.get(name) instanceof Repository)) {
                continue;
            }

            String jndiName = curElement.getAttribute("jndi-name");
            if (UtilValidate.isNotEmpty(jndiName)) {
                try {
                    repos.put(name, (Repository) JNDIContextFactory.getInitialContext("default").lookup(jndiName));
                } catch (NamingException e) {
                    Debug.logError(e, module);
                } catch (GenericConfigException e) {
                    Debug.logError(e, module);
                }
                continue;
            }

            String factoryClass = curElement.getAttribute("class");
            if (UtilValidate.isNotEmpty(factoryClass)) {
                try {
                    repos.put(name, createFromFactory(name, loader, factoryClass));
                } catch (Exception e) {
                    Debug.logError(e, module);
                }
            }

        }

        return repos;
    }

}
