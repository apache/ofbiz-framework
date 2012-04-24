package org.ofbiz.jcr.services;

import java.util.Date;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.VersionManager;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.jcr.access.jackrabbit.JackrabbitRepositoryAccessor;
import org.ofbiz.jcr.loader.JCRFactoryUtil;
import org.ofbiz.jcr.orm.jackrabbit.data.JackrabbitNews;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;

public class JackrabbitServices {

    private static String module = JackrabbitServices.class.getName();

    public static Map<String, Object> determineJackrabbitRepositorySpeed(DispatchContext ctx, Map<String, Object> context) throws UnsupportedRepositoryOperationException, RepositoryException {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Integer maxNodes = (Integer) context.get("maxNodes");

        Long start = 0l;
        Long diff = 0l;

        Session session = JCRFactoryUtil.getSession();
        VersionManager vm = session.getWorkspace().getVersionManager();
        start = new Date().getTime();
        for (int i = 0; i <= maxNodes; i++) {
            try {
                // add a node
                Node n = session.getRootNode().addNode("__Speedtest_Node-" + i);
                n.addMixin("mix:versionable");
                n.setProperty("anyProperty", "Blah");
                session.save();
                vm.checkin(n.getPath());

                vm.checkout("/__Speedtest_Node-" + i);
                // remove the node
                session.removeItem("/__Speedtest_Node-" + i);
                session.save();
            } catch (Exception e) {
                Debug.logError(e, module);
            }
        }

        session.logout();
        diff = (new Date().getTime() - start);
        result.put("repositoryDirectAccessTime", diff.toString());

        JackrabbitRepositoryAccessor access = new JackrabbitRepositoryAccessor(userLogin);
        start = new Date().getTime();
        for (int i = 0; i <= maxNodes; i++) {
            try {
                JackrabbitNews news = new JackrabbitNews("/__Speedtest_Node-" + i, "de", "", null, "");
                access.storeContentObject(news);
                access.removeContentObject("/__Speedtest_Node-" + i);
            } catch (Exception e) {
                Debug.logError(e, module);
            }

        }

        access.closeAccess();
        diff = (new Date().getTime() - start);
        result.put("repositoryOcmAccessTime", diff.toString());

        return result;
    }
}
