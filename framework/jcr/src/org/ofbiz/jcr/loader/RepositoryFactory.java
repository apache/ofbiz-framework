package org.ofbiz.jcr.loader;

import java.io.File;
import java.util.Hashtable;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.core.TransientRepository;

public class RepositoryFactory implements ObjectFactory {

    private static final Map<Object, Object> cache = new ReferenceMap();

    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable environment) throws RepositoryException {
        synchronized (cache) {
            Object instance = cache.get(obj);
            if (instance == null && obj instanceof Reference) {
                Reference ref = (Reference) obj;
                String repHomeDir = ref.get(JCRContainer.REP_HOME_DIR).getContent().toString();
                // check if the repository is already started, than use it
                // otherwise create it
                File lock = new File(repHomeDir);
                if (lock.exists()) {
                    instance = JcrUtils.getRepository(lock.toURI().toString());
                } else {
                    instance = new TransientRepository(ref.get(JCRContainer.DEFAULT_JCR_CONFIG_PATH).getContent().toString(), repHomeDir);
                }

                cache.put(obj, instance);
            }

            return instance;
        }
    }

}
