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
package org.apache.ofbiz.entity.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.transaction.Transaction;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.datasource.GenericHelperInfo;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelField;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionFactoryLoader;
import org.apache.ofbiz.entity.transaction.TransactionUtil;

/**
 * Sequence Utility to get unique sequences from named sequence banks
 */
public class SequenceUtil {

    private static final String MODULE = SequenceUtil.class.getName();

    private final ConcurrentMap<String, SequenceBank> sequences = new ConcurrentHashMap<>();
    private final GenericHelperInfo helperInfo;
    private final String tableName;
    private final String nameColName;
    private final String idColName;

    public SequenceUtil(GenericHelperInfo helperInfo, ModelEntity seqEntity, String nameFieldName, String idFieldName) {
        this.helperInfo = helperInfo;
        if (seqEntity == null) {
            throw new IllegalArgumentException("The sequence model entity was null but is required.");
        }
        this.tableName = seqEntity.getTableName(helperInfo.getHelperBaseName());

        ModelField nameField = seqEntity.getField(nameFieldName);

        if (nameField == null) {
            throw new IllegalArgumentException("Could not find the field definition for the sequence name field " + nameFieldName);
        }
        this.nameColName = nameField.getColName();

        ModelField idField = seqEntity.getField(idFieldName);

        if (idField == null) {
            throw new IllegalArgumentException("Could not find the field definition for the sequence id field " + idFieldName);
        }
        this.idColName = idField.getColName();
    }

    /**
     * Gets next seq id.
     * @param seqName        the seq name
     * @param staggerMax     the stagger max
     * @param seqModelEntity the seq model entity
     * @return the next seq id
     */
    public Long getNextSeqId(String seqName, long staggerMax, ModelEntity seqModelEntity) {
        SequenceBank bank = this.getBank(seqName, seqModelEntity);
        return bank.getNextSeqId(staggerMax);
    }

    /**
     * Force bank refresh.
     * @param seqName    the seq name
     * @param staggerMax the stagger max
     */
    public void forceBankRefresh(String seqName, long staggerMax) {
        // don't use the get method because we don't want to create if it fails
        SequenceBank bank = sequences.get(seqName);
        if (bank == null) {
            return;
        }

        bank.refresh(staggerMax);
    }

    private SequenceBank getBank(String seqName, ModelEntity seqModelEntity) {
        SequenceBank bank = sequences.get(seqName);

        if (bank == null) {
            long bankSize = SequenceBank.DEF_BANK_SIZE;
            if (seqModelEntity != null && seqModelEntity.getSequenceBankSize() != null) {
                bankSize = seqModelEntity.getSequenceBankSize().longValue();
                if (bankSize > SequenceBank.MAX_BANK_SIZE) bankSize = SequenceBank.MAX_BANK_SIZE;
            }
            bank = new SequenceBank(seqName, bankSize);
            SequenceBank bankFromCache = sequences.putIfAbsent(seqName, bank);
            bank = bankFromCache != null ? bankFromCache : bank;
        }

        return bank;
    }

    private final class SequenceBank {
        public static final long DEF_BANK_SIZE = 10;
        public static final long MAX_BANK_SIZE = 5000;
        public static final long START_SEQ_ID = 10000;

        private final String seqName;
        private final long bankSize;
        private final String updateForLockStatement;
        private final String selectSequenceStatement;

        private long curSeqId;
        private long maxSeqId;

        private SequenceBank(String seqName, long bankSize) {
            this.seqName = seqName;
            curSeqId = 0;
            maxSeqId = 0;
            this.bankSize = bankSize;
            updateForLockStatement = "UPDATE " + SequenceUtil.this.tableName + " SET " + SequenceUtil.this.idColName + "="
                    + SequenceUtil.this.idColName + " WHERE " + SequenceUtil.this.nameColName + "='" + this.seqName + "'";
            selectSequenceStatement = "SELECT " + SequenceUtil.this.idColName + " FROM " + SequenceUtil.this.tableName + " WHERE "
                    + SequenceUtil.this.nameColName + "='" + this.seqName + "'";
        }

        private Long getNextSeqId(long staggerMax) {
            long stagger = 1;
            if (staggerMax > 1) {
                stagger = (long) Math.ceil(Math.random() * staggerMax);
                if (stagger == 0) stagger = 1;
            }
            synchronized (this) {
                if ((curSeqId + stagger) <= maxSeqId) {
                    long retSeqId = curSeqId;
                    curSeqId += stagger;
                    return retSeqId;
                } else {
                    fillBank(stagger);
                    if ((curSeqId + stagger) <= maxSeqId) {
                        long retSeqId = curSeqId;
                        curSeqId += stagger;
                        return retSeqId;
                    } else {
                        Debug.logError("Fill bank failed, returning null", MODULE);
                        return null;
                    }
                }
            }
        }

        private synchronized void refresh(long staggerMax) {
            this.curSeqId = this.maxSeqId;
            this.fillBank(staggerMax);
        }

        /*
           The algorithm to get the new sequence id in a thread safe way is the following:
           1 - run an update with no changes to get a lock on the record
               1bis - if no record is found, try to create and update it to get the lock
           2 - select the record (now locked) to get the curSeqId
           3 - increment the sequence
           The three steps are executed in one dedicated database transaction.
         */
        private void fillBank(long stagger) {
            // no need to get a new bank, SeqIds available
            if ((curSeqId + stagger) <= maxSeqId) {
                return;
            }

            long bankSize = this.bankSize;
            if (stagger > 1) {
                // NOTE: could use staggerMax for this, but if that is done it would be easier to guess a valid next id without a brute force attack
                bankSize = stagger * DEF_BANK_SIZE;
            }

            if (bankSize > MAX_BANK_SIZE) {
                bankSize = MAX_BANK_SIZE;
            }

            Transaction suspendedTransaction = null;
            try {
                suspendedTransaction = TransactionUtil.suspend();

                boolean beganTransaction = false;
                try {
                    beganTransaction = TransactionUtil.begin();

                    Connection connection = null;
                    Statement stmt = null;
                    ResultSet rs = null;

                    try {
                        connection = TransactionFactoryLoader.getInstance().getConnection(SequenceUtil.this.helperInfo);
                    } catch (SQLException sqle) {
                        Debug.logWarning("Unable to establish a connection with the database. Error was:" + sqle.toString(), MODULE);
                        throw sqle;
                    } catch (GenericEntityException e) {
                        Debug.logWarning("Unable to establish a connection with the database. Error was: " + e.toString(), MODULE);
                        throw e;
                    }
                    if (connection == null) {
                        throw new GenericEntityException("Unable to establish a connection with the database, connection was null...");
                    }

                    try {
                        stmt = connection.createStatement();
                        String sql = null;
                        // 1 - run an update with no changes to get a lock on the record
                        if (stmt.executeUpdate(updateForLockStatement) <= 0) {
                            Debug.logWarning("Lock failed; no sequence row was found, will try to add a new one for sequence: " + seqName, MODULE);
                            sql = "INSERT INTO " + SequenceUtil.this.tableName + " (" + SequenceUtil.this.nameColName + ", "
                                    + SequenceUtil.this.idColName + ") VALUES ('" + this.seqName + "', " + START_SEQ_ID + ")";
                            try {
                                stmt.executeUpdate(sql);
                            } catch (SQLException sqle) {
                                // insert failed: this means that another thread inserted the record; then retry to run an update with no changes to
                                // get a lock on the record
                                if (stmt.executeUpdate(updateForLockStatement) <= 0) {
                                    // This should never happen
                                    throw new GenericEntityException("No rows changed when trying insert new sequence: " + seqName);
                                }

                            }
                        }
                        // 2 - select the record (now locked) to get the curSeqId
                        rs = stmt.executeQuery(selectSequenceStatement);
                        boolean sequenceFound = rs.next();
                        if (sequenceFound) {
                            curSeqId = rs.getLong(SequenceUtil.this.idColName);
                        }
                        rs.close();
                        if (!sequenceFound) {
                            throw new GenericEntityException("Failed to find the sequence record for sequence: " + seqName);
                        }
                        // 3 - increment the sequence
                        sql = "UPDATE " + SequenceUtil.this.tableName + " SET " + SequenceUtil.this.idColName + "=" + SequenceUtil.this.idColName
                                + "+" + bankSize + " WHERE " + SequenceUtil.this.nameColName + "='" + this.seqName + "'";
                        if (stmt.executeUpdate(sql) <= 0) {
                            throw new GenericEntityException("Update failed, no rows changes for seqName: " + seqName);
                        }

                        TransactionUtil.commit(beganTransaction);

                    } catch (SQLException sqle) {
                        Debug.logWarning(sqle, "SQL Exception:" + sqle.getMessage(), MODULE);
                        throw sqle;
                    } finally {
                        try {
                            if (stmt != null) stmt.close();
                        } catch (SQLException sqle) {
                            Debug.logWarning(sqle, "Error closing statement in sequence util", MODULE);
                        }
                        try {
                            connection.close();
                        } catch (SQLException sqle) {
                            Debug.logWarning(sqle, "Error closing connection in sequence util", MODULE);
                        }
                    }
                } catch (SQLException | GenericEntityException e) {
                    // reset the sequence fields and return (note: it would be better to throw an exception)
                    curSeqId = 0;
                    maxSeqId = 0;
                    String errMsg = "General error in getting a sequenced ID";
                    Debug.logError(e, errMsg, MODULE);
                    try {
                        TransactionUtil.rollback(beganTransaction, errMsg, e);
                    } catch (GenericTransactionException gte2) {
                        Debug.logError(gte2, "Unable to rollback transaction", MODULE);
                    }
                    return;
                }
            } catch (GenericTransactionException e) {
                Debug.logError(e, "System Error suspending transaction in sequence util", MODULE);
                // reset the sequence fields and return (note: it would be better to throw an exception)
                curSeqId = 0;
                maxSeqId = 0;
                return;
            } finally {
                if (suspendedTransaction != null) {
                    try {
                        TransactionUtil.resume(suspendedTransaction);
                    } catch (GenericTransactionException e) {
                        Debug.logError(e, "Error resuming suspended transaction in sequence util", MODULE);
                        // reset the sequence fields and return (note: it would be better to throw an exception)
                        curSeqId = 0;
                        maxSeqId = 0;
                        return;
                    }
                }
            }

            maxSeqId = curSeqId + bankSize;
            if (Debug.infoOn()) {
                Debug.logInfo("Got bank of sequenced IDs for [" + this.seqName + "]; curSeqId=" + curSeqId + ", maxSeqId=" + maxSeqId + ", bankSize="
                        + bankSize, MODULE);
            }
        }
    }
}

