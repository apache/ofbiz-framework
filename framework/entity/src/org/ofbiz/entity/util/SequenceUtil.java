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
package org.ofbiz.entity.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.Map;

import javax.transaction.Transaction;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.datasource.GenericHelperInfo;
import org.ofbiz.entity.jdbc.ConnectionFactory;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;

/**
 * Sequence Utility to get unique sequences from named sequence banks
 * Uses a collision detection approach to safely get unique sequenced ids in banks from the database
 */
public class SequenceUtil {

    public static final String module = SequenceUtil.class.getName();

    private final Map<String, SequenceBank> sequences = new Hashtable<String, SequenceBank>();
    private final GenericHelperInfo helperInfo;
    private final long bankSize;
    private final String tableName;
    private final String nameColName;
    private final String idColName;
    private final boolean clustered;

    public SequenceUtil(GenericDelegator delegator, GenericHelperInfo helperInfo, ModelEntity seqEntity, String nameFieldName, String idFieldName) {
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
        long bankSize = SequenceBank.defaultBankSize;
        if (seqEntity.getSequenceBankSize() != null) {
            bankSize = seqEntity.getSequenceBankSize().longValue();
        }
        this.bankSize = bankSize;
        clustered = delegator.useDistributedCacheClear() || "Y".equals(UtilProperties.getPropertyValue("general.properties", "clustered"));                
    }

    public Long getNextSeqId(String seqName, long staggerMax, ModelEntity seqModelEntity) {
        SequenceBank bank = this.getBank(seqName, seqModelEntity);
        return bank.getNextSeqId(staggerMax);
    }

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
            synchronized(this) {
                bank = sequences.get(seqName);
                if (bank == null) {
                    bank = new SequenceBank(seqName);
                    sequences.put(seqName, bank);
                }
            }
        }

        return bank;
    }

    private class SequenceBank {
        public static final long defaultBankSize = 10;
        public static final long maxBankSize = 5000;
        public static final long startSeqId = 10000;
        public static final long minWaitMillis = 5;
        public static final long maxWaitMillis = 50;
        public static final int maxTries = 5;

        private long curSeqId;
        private long maxSeqId;
        private final String seqName;

        private SequenceBank(String seqName) {
            this.seqName = seqName;
            curSeqId = 0;
            maxSeqId = 0;
            fillBank(1);
        }

        private synchronized Long getNextSeqId(long staggerMax) {
            long stagger = 1;
            if (staggerMax > 1) {
                stagger = Math.round(Math.random() * staggerMax);
                if (stagger == 0) stagger = 1;
            }

            if ((curSeqId + stagger) <= maxSeqId) {
                Long retSeqId = Long.valueOf(curSeqId);
                curSeqId += stagger;
                return retSeqId;
            } else {
                fillBank(stagger);
                if ((curSeqId + stagger) <= maxSeqId) {
                    Long retSeqId = Long.valueOf(curSeqId);
                    curSeqId += stagger;
                    return retSeqId;
                } else {
                    Debug.logError("[SequenceUtil.SequenceBank.getNextSeqId] Fill bank failed, returning null", module);
                    return null;
                }
            }
        }

        private void refresh(long staggerMax) {
            this.curSeqId = this.maxSeqId;
            this.fillBank(staggerMax);
        }

        private synchronized void fillBank(long stagger) {
            //Debug.logWarning("[SequenceUtil.SequenceBank.fillBank] Starting fillBank Thread Name is: " + Thread.currentThread().getName() + ":" + Thread.currentThread().toString(), module);

            // no need to get a new bank, SeqIds available
            if ((curSeqId + stagger) <= maxSeqId) return;

            long bankSize = SequenceUtil.this.bankSize;
            if (stagger > 1) {
                // NOTE: could use staggerMax for this, but if that is done it would be easier to guess a valid next id without a brute force attack
                bankSize = stagger * defaultBankSize;
            }

            if (bankSize > maxBankSize) bankSize = maxBankSize;

            long val1 = 0;
            long val2 = 0;

            // NOTE: the fancy ethernet type stuff is for the case where transactions not available, or something funny happens with really sensitive timing (between first select and update, for example)
            int numTries = 0;

            while (val1 + bankSize != val2) {
                if (Debug.verboseOn()) Debug.logVerbose("[SequenceUtil.SequenceBank.fillBank] Trying to get a bank of sequenced ids for " +
                        this.seqName + "; start of loop val1=" + val1 + ", val2=" + val2 + ", bankSize=" + bankSize, module);

                // not sure if this synchronized block is totally necessary, the method is synchronized but it does do a wait/sleep
                // outside of this block, and this is the really sensitive block, so making sure it is isolated; there is some overhead
                // to this, but very bad things can happen if we try to do too many of these at once for a single sequencer
                synchronized (this) {
                    Transaction suspendedTransaction = null;
                    try {
                        //if we can suspend the transaction, we'll try to do this in a local manual transaction
                        suspendedTransaction = TransactionUtil.suspend();

                        boolean beganTransaction = false;
                        try {
                            beganTransaction = TransactionUtil.begin();

                            Connection connection = null;
                            Statement stmt = null;
                            ResultSet rs = null;

                            try {
                                connection = ConnectionFactory.getConnection(SequenceUtil.this.helperInfo);
                            } catch (SQLException sqle) {
                                Debug.logWarning("[SequenceUtil.SequenceBank.fillBank]: Unable to esablish a connection with the database... Error was:" + sqle.toString(), module);
                                throw sqle;
                            } catch (GenericEntityException e) {
                                Debug.logWarning("[SequenceUtil.SequenceBank.fillBank]: Unable to esablish a connection with the database... Error was: " + e.toString(), module);
                                throw e;
                            }

                            if (connection == null) {
                                throw new GenericEntityException("[SequenceUtil.SequenceBank.fillBank]: Unable to esablish a connection with the database, connection was null...");
                            }

                            String sql = null;

                            try {
                                // we shouldn't need this, and some TX managers complain about it, so not including it: connection.setAutoCommit(false);

                                stmt = connection.createStatement();
                                if (clustered) {
                                    sql = "SELECT " + SequenceUtil.this.idColName + " FROM " + SequenceUtil.this.tableName + " WHERE " + SequenceUtil.this.nameColName + "='" + this.seqName + "'" + " FOR UPDATE";                                    
                                } else {
                                    sql = "SELECT " + SequenceUtil.this.idColName + " FROM " + SequenceUtil.this.tableName + " WHERE " + SequenceUtil.this.nameColName + "='" + this.seqName + "'";
                                }
                                rs = stmt.executeQuery(sql);
                                boolean gotVal1 = false;
                                if (rs.next()) {
                                    val1 = rs.getLong(SequenceUtil.this.idColName);
                                    gotVal1 = true;
                                }
                                rs.close();

                                if (!gotVal1) {
                                    Debug.logWarning("[SequenceUtil.SequenceBank.fillBank] first select failed: will try to add new row, result set was empty for sequence [" + seqName + "] \nUsed SQL: " + sql + " \n Thread Name is: " + Thread.currentThread().getName() + ":" + Thread.currentThread().toString(), module);
                                    sql = "INSERT INTO " + SequenceUtil.this.tableName + " (" + SequenceUtil.this.nameColName + ", " + SequenceUtil.this.idColName + ") VALUES ('" + this.seqName + "', " + startSeqId + ")";
                                    if (stmt.executeUpdate(sql) <= 0) {
                                        throw new GenericEntityException("No rows changed when trying insert new sequence row with this SQL: " + sql);
                                    }
                                    continue;
                                }

                                sql = "UPDATE " + SequenceUtil.this.tableName + " SET " + SequenceUtil.this.idColName + "=" + SequenceUtil.this.idColName + "+" + bankSize + " WHERE " + SequenceUtil.this.nameColName + "='" + this.seqName + "'";
                                if (stmt.executeUpdate(sql) <= 0) {
                                    throw new GenericEntityException("[SequenceUtil.SequenceBank.fillBank] update failed, no rows changes for seqName: " + seqName);
                                }
                                if (clustered) {
                                    sql = "SELECT " + SequenceUtil.this.idColName + " FROM " + SequenceUtil.this.tableName + " WHERE " + SequenceUtil.this.nameColName + "='" + this.seqName + "'" + " FOR UPDATE";                                    

                                } else {
                                    sql = "SELECT " + SequenceUtil.this.idColName + " FROM " + SequenceUtil.this.tableName + " WHERE " + SequenceUtil.this.nameColName + "='" + this.seqName + "'";
                                }
                                rs = stmt.executeQuery(sql);
                                boolean gotVal2 = false;
                                if (rs.next()) {
                                    val2 = rs.getLong(SequenceUtil.this.idColName);
                                    gotVal2 = true;
                                }

                                rs.close();

                                if (!gotVal2) {
                                    throw new GenericEntityException("[SequenceUtil.SequenceBank.fillBank] second select failed: aborting, result set was empty for sequence: " + seqName);
                                }

                                // got val1 and val2 at this point, if we don't have the right difference between them, force a rollback (with
                                //setRollbackOnly and NOT with an exception because we don't want to break from the loop, just err out and
                                //continue), then flow out to allow the wait and loop thing to happen
                                if (val1 + bankSize != val2) {
                                    TransactionUtil.setRollbackOnly("Forcing transaction rollback in sequence increment because we didn't get a clean update, ie a conflict was found, so not saving the results", null);
                                }
                            } catch (SQLException sqle) {
                                Debug.logWarning(sqle, "[SequenceUtil.SequenceBank.fillBank] SQL Exception while executing the following:\n" + sql + "\nError was:" + sqle.getMessage(), module);
                                throw sqle;
                            } finally {
                                try {
                                    if (stmt != null) stmt.close();
                                } catch (SQLException sqle) {
                                    Debug.logWarning(sqle, "Error closing statement in sequence util", module);
                                }
                                try {
                                    if (connection != null) connection.close();
                                } catch (SQLException sqle) {
                                    Debug.logWarning(sqle, "Error closing connection in sequence util", module);
                                }
                            }
                        } catch (Exception e) {
                            String errMsg = "General error in getting a sequenced ID";
                            Debug.logError(e, errMsg, module);
                            try {
                                TransactionUtil.rollback(beganTransaction, errMsg, e);
                            } catch (GenericTransactionException gte2) {
                                Debug.logError(gte2, "Unable to rollback transaction", module);
                            }

                            // error, break out of the loop to not try to continue forever
                            break;
                        } finally {
                            try {
                                TransactionUtil.commit(beganTransaction);
                            } catch (GenericTransactionException gte) {
                                Debug.logError(gte, "Unable to commit sequence increment transaction, continuing anyway though", module);
                            }
                        }
                    } catch (GenericTransactionException e) {
                        Debug.logError(e, "System Error suspending transaction in sequence util", module);
                    } finally {
                        if (suspendedTransaction != null) {
                            try {
                                TransactionUtil.resume(suspendedTransaction);
                            } catch (GenericTransactionException e) {
                                Debug.logError(e, "Error resuming suspended transaction in sequence util", module);
                            }
                        }
                    }
                }

                if (val1 + bankSize != val2) {
                    if (numTries >= maxTries) {
                        String errMsg = "[SequenceUtil.SequenceBank.fillBank] maxTries (" + maxTries + ") reached for seqName [" + this.seqName + "], giving up.";
                        Debug.logError(errMsg, module);
                        return;
                    }

                    // collision happened, wait a bounded random amount of time then continue
                    long waitTime = (long) (Math.random() * (maxWaitMillis - minWaitMillis) + minWaitMillis);

                    Debug.logWarning("[SequenceUtil.SequenceBank.fillBank] Collision found for seqName [" + seqName + "], val1=" + val1 + ", val2=" + val2 + ", val1+bankSize=" + (val1 + bankSize) + ", bankSize=" + bankSize + ", waitTime=" + waitTime, module);

                    try {
                        // using the Thread.sleep to more reliably lock this thread: this.wait(waitTime);
                        java.lang.Thread.sleep(waitTime);
                    } catch (Exception e) {
                        Debug.logWarning(e, "Error waiting in sequence util", module);
                        return;
                    }
                }

                numTries++;
            }

            curSeqId = val1;
            maxSeqId = val2;
            if (Debug.infoOn()) Debug.logInfo("Got bank of sequenced IDs for [" + this.seqName + "]; curSeqId=" + curSeqId + ", maxSeqId=" + maxSeqId + ", bankSize=" + bankSize, module);
            //Debug.logWarning("[SequenceUtil.SequenceBank.fillBank] Ending fillBank Thread Name is: " + Thread.currentThread().getName() + ":" + Thread.currentThread().toString(), module);
        }
    }
}
