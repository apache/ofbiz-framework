package com.simbaquartz.xsolr.solr.collections;

import java.io.Serializable;

/**
 * Represents a SOLR collection.
 */
public interface ISolrCollection extends Serializable {
    String getDocumentId();
}
