package org.ofbiz.jcr.api.jackrabbit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.jcr.access.JcrRepositoryAccessor;
import org.ofbiz.jcr.api.JcrHelper;

public abstract class JackrabbitAbstractHelper implements JcrHelper {

    private static String module = JackrabbitAbstractHelper.class.getName();

    protected final JcrRepositoryAccessor access;

    public JackrabbitAbstractHelper(JcrRepositoryAccessor accessor) {
        this.access = accessor;
    }

    /* (non-Javadoc)
     * @see org.ofbiz.jcr.api.jackrabbit.JcrHelper#closeContentSession()
     */
    @Override
    public void closeContentSession() {
        access.closeAccess();
    }

    /* (non-Javadoc)
     * @see org.ofbiz.jcr.api.jackrabbit.JcrHelper#removeContentObject(java.lang.String)
     */
    @Override
    public void removeContentObject(String contentPath) {
        access.removeContentObject(contentPath);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.jcr.api.jackrabbit.JcrHelper#queryData(java.lang.String)
     */
    @Override
    public List<Map<String, String>> queryData(String query) throws RepositoryException {
        QueryResult qr = this.access.queryForRepositoryData(query);

        List<Map<String, String>> resultNodePaths = new ArrayList<Map<String, String>>();
        RowIterator rows = qr.getRows();
        while (rows.hasNext()) {
            Row row = rows.nextRow();
            Map<String, String> content = FastMap.newInstance();
            content.put("path", row.getPath());
            content.put("score", String.valueOf(row.getScore()));

            resultNodePaths.add(content);
            if (Debug.isOn(Debug.INFO)) {
                Debug.logInfo("For query: " + query + " found node with path: " + row.getPath(), module);
            }

        }

        return resultNodePaths;
    }
}
