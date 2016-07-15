/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.ofbiz.content.search;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.Delegator;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;

public class DocumentIndexer extends Thread {

    public static final String module = DocumentIndexer.class.getName();

    private static Map<String, DocumentIndexer> documentIndexerMap = new HashMap<String, DocumentIndexer>();
    private LinkedBlockingQueue<LuceneDocument> documentIndexQueue = new LinkedBlockingQueue<LuceneDocument>();
    private Delegator delegator;
    private Directory indexDirectory;
    // TODO: Move to property file
    private static final int UNCOMMITTED_DOC_LIMIT = 100;

    private DocumentIndexer(Delegator delegator, String indexName) {
        this.delegator = delegator;
        try {
            this.indexDirectory = FSDirectory.open(new File(SearchWorker.getIndexPath(indexName)).toPath());
        } catch (CorruptIndexException e) {
            Debug.logError("Corrupted lucene index: "  + e.getMessage(), module);
        } catch (LockObtainFailedException e) {
            Debug.logError("Could not obtain Lock on lucene index "  + e.getMessage(), module);
        } catch (IOException e) {
            Debug.logError(e.getMessage(), module);
        }
    }

    public static synchronized DocumentIndexer getInstance(Delegator delegator, String indexName) {
        String documentIndexerId = delegator.getDelegatorName() + "_" + indexName;
        DocumentIndexer documentIndexer = documentIndexerMap.get(documentIndexerId);
        if (documentIndexer == null) {
            documentIndexer = new DocumentIndexer(delegator, indexName);
            documentIndexer.setName("DocumentIndexer_" + delegator.getDelegatorName() + "_" + indexName);
            documentIndexer.start();
            documentIndexerMap.put(documentIndexerId, documentIndexer);
        }
        return documentIndexer;
    }

    @Override
    public void run() {
        IndexWriter indexWriter = null;
        int uncommittedDocs = 0;
        while (true) {
            LuceneDocument ofbizDocument;
            try {
                // Execution will pause here until the queue receives a LuceneDocument for indexing
                ofbizDocument = documentIndexQueue.take();
            } catch (InterruptedException e) {
                Debug.logError(e, module);
                if (indexWriter != null) {
                    try {
                        indexWriter.close();
                        indexWriter = null;
                    } catch(IOException ioe) {
                        Debug.logError(ioe, module);
                    }
                }
                break;
            }
            Term documentIdentifier = ofbizDocument.getDocumentIdentifier();
            Document document = ofbizDocument.prepareDocument(this.delegator);
            if (indexWriter == null) {
                try {
                	StandardAnalyzer analyzer = new StandardAnalyzer();
                	analyzer.setVersion(SearchWorker.getLuceneVersion());
                    indexWriter  = new IndexWriter(this.indexDirectory, new IndexWriterConfig(analyzer));
                } catch (CorruptIndexException e) {
                    Debug.logError("Corrupted lucene index: "  + e.getMessage(), module);
                    break;
                } catch (LockObtainFailedException e) {
                    Debug.logError("Could not obtain Lock on lucene index "  + e.getMessage(), module);
                    // TODO: put the thread to sleep waiting for the locked to be released
                    break;
                } catch (IOException e) {
                    Debug.logError(e.getMessage(), module);
                    break;
                }
            }
            try {
                if (document == null) {
                    indexWriter.deleteDocuments(documentIdentifier);
                    if (Debug.infoOn()) Debug.logInfo(getName() + ": deleted Lucene document: " + ofbizDocument, module);
                } else {
                    indexWriter.updateDocument(documentIdentifier, document);
                    if (Debug.infoOn()) Debug.logInfo(getName() + ": indexed Lucene document: " + ofbizDocument, module);
                }
            } catch(Exception e) {
                Debug.logError(e, getName() + ": error processing Lucene document: " + ofbizDocument, module);
                if (documentIndexQueue.peek() == null) {
                    try {
                        indexWriter.close();
                        indexWriter = null;
                    } catch(IOException ioe) {
                        Debug.logError(ioe, module);
                    }
                }
                continue;
            }
            uncommittedDocs++;
            if (uncommittedDocs == UNCOMMITTED_DOC_LIMIT || documentIndexQueue.peek() == null) {
                // limit reached or queue empty, time to commit
                try {
                    indexWriter.commit();
                } catch (IOException e) {
                    Debug.logError(e, module);
                }
                uncommittedDocs = 0;
            }
            if (documentIndexQueue.peek() == null) {
                try {
                    indexWriter.close();
                    indexWriter = null;
                } catch (IOException e) {
                    Debug.logError(e, module);
                }
            }
        }
    }

    public boolean queue(LuceneDocument document) {
        return documentIndexQueue.add(document);
    }
}
