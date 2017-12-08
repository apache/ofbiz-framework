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
package org.apache.ofbiz.entity.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;

/**
 * Generic Entity - Entity Definition Checker
 *
 */
public class ModelEntityChecker {

    public static final String module = ModelEntityChecker.class.getName();

    public static void checkEntities(Delegator delegator, List<String> warningList) throws GenericEntityException {
        ModelReader reader = delegator.getModelReader();

        Set<String> reservedWords = new HashSet<>();
        if (Debug.infoOn()) {
            Debug.logInfo("[initReservedWords] array length = " + rwArray.length, module);
        }
        for (int i = 0; i < rwArray.length; i++) {
            reservedWords.add(rwArray[i]);
        }

        Map<String, Set<String>> packages = new HashMap<>();
        Set<String> packageNames = new TreeSet<>();
        Set<String> tableNames = new HashSet<>();

        //put the entityNames TreeSets in a HashMap by packageName
        Collection<String> ec = reader.getEntityNames();
        Set<String> entityNames = new HashSet<>(ec);
        for (String eName: ec) {
            ModelEntity ent = reader.getModelEntity(eName);

            //make sure the table name is in the list of all table names, if
            // not null
            if (UtilValidate.isNotEmpty(ent.getPlainTableName()))
                    tableNames.add(ent.getPlainTableName());

            Set<String> entities = packages.get(ent.getPackageName());
            if (entities == null) {
                entities = new TreeSet<>();
                packages.put(ent.getPackageName(), entities);
                packageNames.add(ent.getPackageName());
            }
            entities.add(eName);
        }

        Set<String> fkNames = new HashSet<>();
        Set<String> indexNames = new HashSet<>();

        for (String pName: packageNames) {
            Set<String> entities = packages.get(pName);
            for (String entityName: entities) {
                String helperName = delegator.getEntityHelperName(entityName);
                String groupName = delegator.getEntityGroupName(entityName);
                ModelEntity entity = reader.getModelEntity(entityName);
                if (helperName == null) {
                    //only show group name warning if helper name not found
                    if (UtilValidate.isEmpty(groupName)) {
                            warningList.add("[GroupNotFound] No Group Name found for entity " + entity.getEntityName() + ".");
                    } else {
                        warningList.add("[HelperNotFound] No Helper (DataSource) definition found for entity [" + entity.getEntityName() + "] because there is no helper (datasource) configured for the entity group it is in: [" + groupName + "]");
                    }
                }
                if (entity.getPlainTableName() != null && entity.getPlainTableName().length() > 30) {
                        warningList.add("[TableNameGT30] Table name [" + entity.getPlainTableName() + "] of entity " + entity.getEntityName() + " is longer than 30 characters.");
                }
                if (entity.getPlainTableName() != null && reservedWords.contains(entity.getPlainTableName().toUpperCase(Locale.getDefault()))) {
                        warningList.add("[TableNameRW] Table name [" + entity.getPlainTableName() + "] of entity " + entity.getEntityName() + " is a reserved word.");
                }

                // don't check columns/relations/keys when never-check is set to "true"
                if (entity.getNeverCheck()) {
                    continue;
                }

                Set<String> ufields = new HashSet<>();
                Iterator<ModelField> fieldIter = entity.getFieldsIterator();
                while (fieldIter.hasNext()) {
                    ModelField field = fieldIter.next();
                    ModelFieldType type = delegator.getEntityFieldType(entity,field.getType());

                    if (ufields.contains(field.getName())) {
                        warningList.add("[FieldNotUnique] Field [" + field.getName() + " of entity " + entity.getEntityName() + " is not unique for that entity.");
                    } else {
                        ufields.add(field.getName());
                    }
                    if (field.getColName().length() > 30 && !(entity instanceof ModelViewEntity)) {
                        warningList.add("[FieldNameGT30] Column name [" + field.getColName() + "] of entity " + entity.getEntityName() + " is longer than 30 characters.");
                    }
                    if (field.getColName().length() == 0) {
                        warningList.add("[FieldNameEQ0] Column name for field name \"" + field.getName() + "\" of entity " + entity.getEntityName() + " is empty (zero length).");
                    }
                    if (reservedWords.contains(field.getColName().toUpperCase(Locale.getDefault())))
                            warningList.add("[FieldNameRW] Column name " + field.getColName() + " of entity " + entity.getEntityName() + " is a reserved word.");
                    if (type == null) {
                        StringBuilder warningMsg = new StringBuilder();
                        warningMsg.append("[FieldTypeNotFound] Field type " + field.getType() + " of entity " + entity.getEntityName() + " not found in field type definitions");
                        if (helperName == null) {
                            warningMsg.append(" (no helper definition found)");
                        }
                        warningMsg.append(".");
                        warningList.add(warningMsg.toString());
                    }
                }
                if (entity.getRelationsSize() > 0) {
                    Iterator<ModelIndex> indexIter = entity.getIndexesIterator();
                    while (indexIter.hasNext()) {
                        ModelIndex index = indexIter.next();

                        if (indexNames.contains(index.getName())) {
                            warningList.add("[IndexDuplicateName] Index on entity "
                                            + entity.getEntityName() + " has a duplicate index-name \""
                                            + index.getName() + "\".");
                        } else {
                            indexNames.add(index.getName());
                        }

                        if (tableNames.contains(index.getName())) {
                            warningList.add("[IndexTableDupName] Index on entity "
                                            + entity.getEntityName() + " has an index-name \""
                                            + index.getName() + "\" that is also being used as a table name.");
                        }

                        if (fkNames.contains(index.getName())) {
                            warningList.add("[IndexFKDupName] Index on entity "
                                            + entity.getEntityName()
                                            + " has an index-name \""
                                            + index.getName()
                                            + "\" that is also being used as a Foreign Key name.");
                        }

                        // make sure all names are <= 18 characters
                        if (index.getName().length() > 18) {
                            warningList.add("[IndexNameGT18] The index name " + index.getName() + " (length:" + index.getName().length()
                                            + ") was greater than 18 characters in length for entity " + entity.getEntityName() + ".");
                        }
                    }

                    Set<String> relations = new HashSet<>();
                    for (int r = 0; r < entity.getRelationsSize(); r++) {
                        ModelRelation relation = entity.getRelation(r);

                        if (!entityNames.contains(relation.getRelEntityName())) {
                            warningList.add("[RelatedEntityNotFound] Related entity " + relation.getRelEntityName()
                                            + " of entity " + entity.getEntityName() + " not found.");
                        }
                        if (relations.contains(relation.getTitle() + relation.getRelEntityName())) {
                            warningList.add("[RelationNameNotUnique] Relation " + relation.getTitle() + relation.getRelEntityName()
                                            + " of entity "+ entity.getEntityName() + " is not unique for that entity.");
                        } else {
                            relations.add(relation.getTitle() + relation.getRelEntityName());
                        }

                        if (relation.getFkName().length() > 0) {
                            if (fkNames.contains(relation.getFkName())) {
                                warningList.add("[RelationFkDuplicate] Relation to "+ relation.getRelEntityName()
                                                + " from entity " + entity.getEntityName() + " has a duplicate fk-name \""
                                                + relation.getFkName() + "\".");
                            } else {
                                fkNames.add(relation.getFkName());
                            }
                            if (tableNames.contains(relation.getFkName())) {
                                warningList.add("[RelationFkTableDup] Relation to " + relation.getRelEntityName() + " from entity "
                                                + entity.getEntityName() + " has an fk-name \""
                                                + relation.getFkName() + "\" that is also being used as a table name.");
                            }
                            if (indexNames.contains(relation.getFkName())) {
                                warningList.add("[RelationFkTableDup] Relation to " + relation.getRelEntityName() + " from entity "
                                                + entity.getEntityName() + " has an fk-name \""
                                                + relation.getFkName() + "\" that is also being used as an index name.");
                            }
                        }

                        // make sure all FK names are <= 18 characters
                        if (relation.getFkName().length() > 18) {
                            warningList.add("[RelFKNameGT18] The foreign key named " + relation.getFkName() 
                                            + " (length:" + relation.getFkName().length()
                                            + ") was greater than 18 characters in length for relation " + relation.getTitle() + relation.getRelEntityName()
                                            + " of entity " + entity.getEntityName() + ".");
                        }

                        ModelEntity relatedEntity = null;
                        try {
                            relatedEntity = reader.getModelEntity(relation.getRelEntityName());
                        } catch (GenericEntityException e) {
                            Debug.logInfo("Entity referred to in relation is not defined: " + relation.getRelEntityName(), module);
                        }
                        if (relatedEntity != null) {
                            //if relation is of type one, make sure keyMaps
                            // match the PK of the relatedEntity
                            if ("one".equals(relation.getType()) || "one-nofk".equals(relation.getType())) {
                                if (relatedEntity.getPksSize() != relation.getKeyMaps().size())
                                        warningList.add("[RelatedOneKeyMapsWrongSize] The number of primary keys (" + relatedEntity.getPksSize()
                                                        + ") of related entity " + relation.getRelEntityName()
                                                        + " does not match the number of keymaps (" + relation.getKeyMaps().size()
                                                        + ") for relation of type one \"" + relation.getTitle() + relation.getRelEntityName()
                                                        + "\" of entity " + entity.getEntityName() + ".");
                                Iterator<ModelField> pksIter = relatedEntity.getPksIterator();
                                while (pksIter.hasNext()) {
                                    ModelField pk = pksIter.next();
                                    if (relation.findKeyMapByRelated(pk.getName()) == null) {
                                        warningList.add("[RelationOneRelatedPrimaryKeyMissing] The primary key \"" + pk.getName()
                                                        + "\" of related entity " + relation.getRelEntityName()
                                                        + " is missing in the keymaps for relation of type one " + relation.getTitle() + relation.getRelEntityName()
                                                        + " of entity " + entity.getEntityName() + ".");
                                    }
                                }
                            }
                        }

                        //make sure all keyMap 'fieldName's match fields of
                        // this entity
                        //make sure all keyMap 'relFieldName's match fields of
                        // the relatedEntity
                        for (ModelKeyMap keyMap : relation.getKeyMaps()) {

                            ModelField field = entity.getField(keyMap.getFieldName());
                            ModelField rfield = null;
                            if (relatedEntity != null) {
                                rfield = relatedEntity.getField(keyMap.getRelFieldName());
                            }
                            if (rfield == null) {
                                warningList.add("[RelationRelatedFieldNotFound] The field \"" + keyMap.getRelFieldName()
                                                + "\" of related entity " + relation.getRelEntityName()
                                                + " was specified in the keymaps but is not found for relation " + relation.getTitle() + relation.getRelEntityName()
                                                + " of entity " + entity.getEntityName() + ".");
                            }
                            if (field == null) {
                                warningList.add("[RelationFieldNotFound] The field " + keyMap.getFieldName()
                                                + " was specified in the keymaps but is not found for relation " + relation.getTitle() + relation.getRelEntityName()
                                                + " of entity " + entity.getEntityName() + ".");
                            }
                            if (field != null && rfield != null) {
                                //this was the old check, now more constrained
                                // to keep things cleaner:
                                // if (!field.getType().equals(rfield.getType())
                                // &&
                                // !field.getType().startsWith(rfield.getType())
                                // &&
                                // !rfield.getType().startsWith(field.getType()))
                                // {
                                if (!field.getType().equals(rfield.getType())) {
                                    warningList.add("[RelationFieldTypesDifferent] The field type ("+ field.getType()
                                                    + ") of " + field.getName() + " of entity " + entity.getEntityName()
                                                    + " is not the same as field type (" + rfield.getType() + ") of "
                                                    + rfield.getName() + " of entity " + relation.getRelEntityName() + " for relation "
                                                    + relation.getTitle() + relation.getRelEntityName() + ".");
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    protected static final String[] rwArray = { "ABORT", "ABS", "ABSOLUTE",
            "ACCEPT", "ACCES", "ACCESS", "ACS", "ACTION", "ACTIVATE", "ADD", "ADDFORM",
            "ADMIN", "AFTER", "AGGREGATE", "ALIAS", "ALL", "ALLOCATE", "ALTER",
            "ANALYZE", "AND", "ANDFILENAME", "ANY", "ANYFINISH", "APPEND",
            "ARCHIVE", "ARE", "ARRAY", "AS", "ASC", "ASCENDING", "ASCII",
            "ASSERT", "ASSERTION", "ASSIGN", "AT", "ATTRIBUTE", "ATTRIBUTES",
            "AUDIT", "AUTHID", "AUTHORIZATION", "AUTONEXT", "AUTO_INCREMENT",
            "AVERAGE", "AVG", "AVGU", "AVG_ROW_LENGTH",

            "BACKOUT", "BACKUP", "BEFORE", "BEGIN", "BEGINLOAD", "BEGINMODIFY",
            "BEGINNING", "BEGWORK", "BETWEEN", "BETWEENBY", "BINARY",
            "BINARY_INTEGER", "BIT", "BIT_LENGTH", "BLOB", "BODY", "BOOLEAN",
            "BORDER", "BOTH", "BOTTOM", "BREADTH", "BREAK", "BREAKDISPLAY",
            "BROWSE", "BUFERED", "BUFFER", "BUFFERED", "BULK", "BY", "BYTE",

            "CALL", "CANCEL", "CASCADE", "CASCADED", "CASE", "CAST", "CATALOG",
            "CHANGE", "CHAR", "CHAR_LENGTH", "CHAR_BASE", "CHARACTER",
            "CHARACTER_LENGTH", "CHAR_CONVERT", "CHECK", "CHECKPOINT",
            "CHECKSUM", "CHR2FL", "CHR2FLO", "CHR2FLOA", "CHR2FLOAT",
            "CHR2INT", "CLASS", "CLEAR", "CLEARROW", "CLIPPED", "CLOB",
            "CLOSE", "CLUSTER", "CLUSTERED", "CLUSTERING", "COALESCE", "COBOL",
            "COLD", "COLLATE", "COLLATION", "COLLECT", "COLUMN", "COLUMNS",
            "COMMAND", "COMMENT", "COMMIT", "COMMITTED", "COMPLETION",
            "COMPRESS", "COMPUTE", "CONCAT", "COND", "CONDITION", "CONFIG",
            "CONFIRM", "CONNECT", "CONNECTION", "CONSTANT", "CONSTRAINT",
            "CONSTRAINTS", "CONSTRUCT", "CONSTRUCTOR", "CONTAIN", "CONTAINS",
            "CONTAINSTABLE", "CONTINUE", "CONTROLROW", "CONVERT", "COPY",
            "CORRESPONDING", "COUNT", "COUNTU", "COUNTUCREATE", "CRASH",
            "CREATE", "CROSS", "CUBE", "CURRENT", "CURRENT_DATE",
            "CURRENT_PATH", "CURRENT_ROLE", "CURRENT_SESSION", "CURRENT_TIME",
            "CURRENT_TIMESTAMP", "CURRENT_USER", "CURSOR", "CURVAL", "CYCLE",

            "DATA", "DATALINK", "DATABASE", "DATABASES", "DATAPAGES",
            "DATA_PGS", "DATE", "DATETIME", "DAY", "DAY_HOUR", "DAY_MINUTE",
            "DAY_SECOND", "DAYNUM", "DAYOFMONTH", "DAYOFWEEK", "DAYOFYEAR",
            "DBA", "DBCC", "DBE", "DBEFILE", "DBEFILEO", "DBEFILESET",
            "DBSPACE", "DBYTE", "DEALLOCATE", "DEC", "DECENDING", "DECIMAL",
            "DECLARE", "DEFAULT", "DEFAULTS", "DEFER", "DEFERRABLE", "DEFINE",
            "DEFINITION", "DELAY_KEY_WRITE", "DELAYED", "DELETE", "DELETEROW",
            "DENY", "DEPTH", "DEREF", "DESC", "DESCENDING", "DESCENDNG",
            "DESCRIBE", "DESCRIPTOR", "DESTPOS", "DESTROY", "DEVICE",
            "DEVSPACE", "DIAGNOSTICS", "DICTIONARY", "DIRECT", "DIRTY",
            "DISCONNECT", "DISK", "DISPLACE", "DISPLAY", "DISTINCT",
            "DISTINCTROW", "DISTRIBUTED", "DISTRIBUTION", "DIV", "DO", "DOES",
            "DOMAIN", "DOUBLE", "DOWN", "DROP", "DUAL", "DUMMY", "DUMP",
            "DUPLICATES",

            "EACH", "EBCDIC", "EDITADD", "EDITUPDATE", "ED_STRING", "ELSE",
            "ELSEIF", "ELSIF", "ENCLOSED", "END", "ENDDATA", "ENDDISPLAY",
            "ENDFORMS", "ENDIF", "ENDING", "ENDLOAD", "ENDLOOP", "ENDMODIFY",
            "ENDPOS", "ENDRETRIEVE", "ENDSELECT", "ENDWHILE", "END_ERROR",
            "END_EXEC", "END_FETCH", "END_FOR", "END_GET", "END_MODIFY",
            "END_PLACE", "END_SEGMENT_S", "END_SEGMENT_STRING", "END_STORE",
            "END_STREAM", "ENUM", "EQ", "EQUALS", "ERASE", "ERROR", "ERRLVL",
            "ERROREXIT", "ESCAPE", "ESCAPED", "EVALUATE", "EVALUATING",
            "EVERY", "EXCEPT", "EXCEPTION", "EXCLUSIVE", "EXEC", "EXECUTE",
            "EXISTS", "EXIT", "EXPAND", "EXPANDING", "EXPLAIN", "EXPLICIT",
            "EXTEND", "EXTENDS", "EXTENT", "EXTERNAL", "EXTRACT",

            "FALSE", "FETCH", "FIELD", "FIELDS", "FILE", "FILENAME",
            "FILLFACTOR", "FINALISE", "FINALIZE", "FINDSTR", "FINISH", "FIRST",
            "FIRSTPOS", "FIXED", "FL", "FLOAT", "FLOAT4", "FLOAT8", "FLUSH",
            "FOR", "FORALL", "FOREACH", "FOREIGN", "FORMAT", "FORMDATA",
            "FORMINIT", "FORMS", "FORTRAN", "FOUND", "FRANT", "FRAPHIC",
            "FREE", "FREETEXT", "FREETEXTTABLE", "FROM", "FRS", "FULL",
            "FUNCTION",

            "GE", "GENERAL", "GET", "GETFORM", "GETOPER", "GETROW", "GLOBAL",
            "GLOBALS", "GO", "GOTO", "GRANT", "GRANTS", "GRAPHIC", "GROUP",
            "GROUPING", "GT",

            "HANDLER", "HASH", "HAVING", "HEAP", "HEADER", "HELP", "HELPFILE",
            "HELP_FRS", "HIGH_PRIORITY", "HOLD", "HOLDLOCK", "HOSTS", "HOUR",
            "HOUR_MINUTE", "HOUR_SECOND",

            "IDENTIFIED", "IDENTIFIELD", "IDENTITY", "IDENTITY_INSERT", "IF",
            "IFDEF", "IGNORE", "IMAGE", "IMMEDIATE", "IMMIDIATE", "IMPLICIT",
            "IN", "INCLUDE", "INCREMENT", "INDEX", "INDEXED", "INDEXNAME",
            "INDEXPAGES", "INDICATOR", "INFIELD", "INFILE", "INFO", "INGRES",
            "INIT", "INITIAL", "INITIALISE", "INITIALIZE", "INITIALLY",
            "INITTABLE", "INNER", "INOUT", "INPUT", "INQUIRE_EQUEL",
            "INQUIRE_FRS", "INQUIRE_INGRES", "INQUIR_FRS", "INSERT",
            "INSERT_ID", "INSERTROW", "INSTRUCTIONS", "INT", "INT1", "INT2CHR",
            "INT2", "INT3", "INT4", "INT8", "INTEGER", "INTEGRITY",
            "INTERESECT", "INTERFACE", "INTERRUPT", "INTERSECT", "INTERVAL",
            "INTO", "INTSCHR", "INVOKE", "IS", "ISAM", "ISOLATION", "ITERATE",

            "JAVA", "JOIN", "JOURNALING",

            "KEY", "KEYS", "KILL",

            "LABEL", "LANGUAGE", "LARGE", "LAST", "LAST_INSERT_ID", "LASTPOS",
            "LATERAL", "LE", "LEADING", "LEAVE", "LEFT", "LENGTH", "LENSTR",
            "LESS", "LET", "LEVEL", "LIKE", "LIKEPROCEDURETP", "LIMIT",
            "LIMITED", "LINE", "LINENO", "LINES", "LINK", "LIST", "LISTEN",
            "LOAD", "LOADTABLE", "LOADTABLERESUME", "LOCAL", "LOCALTIME",
            "LOCALTIMESTAMP", "LOCATION", "LOCATOR", "LOCK", "LOCKING", "LOG",
            "LOGS", "LONG", "LONGBLOB", "LONGTEXT", "LOOP", "LOW_PRIORITY",
            "LOWER", "LPAD", "LT",

            "MAIN", "MANUITEM", "MARGIN", "MATCH", "MATCHES", "MATCHING",
            "MAX", "MAX_ROWS", "MAXEXTENTS", "MAXPUBLICUNION", "MAXRECLEN",
            "MDY", "MEDIUMBLOB", "MEDIUMINT", "MEDIUMTEXT", "MEETS", "MENU",
            "MENUITEM", "MENUITEMSCREEN", "MESSAGE", "MESSAGERELOCATE",
            "MESSAGESCROLL", "MFETCH", "MIDDLEINT", "MIN", "MIN_ROWS",
            "MINRECLEN", "MINRETURNUNTIL", "MINUS", "MINUTE", "MINUTE_SECOND",
            "MIRROREXIT", "MISLABEL", "MISSING", "MIXED", "MOD", "MODE",
            "MODIFIES", "MODIFY", "MODIFYREVOKEUPDATE", "MODULE", "MONEY",
            "MONITOR", "MONTH", "MONTHNAME", "MOVE", "MULTI", "MYISAM",

            "NAME", "NAMES", "NATIONAL", "NATURAL", "NATURALN", "NCHAR",
            "NCLOB", "NE", "NEED", "NEW", "NEWLOG", "NEXT", "NEXTSCROLLDOWN",
            "NEXTVAL", "NO", "NOAUDIT", "NOCHECK", "NOCOMPRESS", "NOCOPY",
            "NOCR", "NOJOURNALING", "NOLIST", "NOLOG", "NONCLUSTERED", "NONE",
            "NORMAL", "NORMALIZE", "NOSYSSORT", "NOT", "NOTFFOUND", "NOTFOUND",
            "NOTIFY", "NOTRANS", "NOTRIM", "NOTRIMSCROLLUP", "NOTROLLBACKUSER",
            "NOWAIT", "NULL", "NULLIF", "NULLIFY", "NULLSAVEUSING", "NULLVAL",
            "NUMBER", "NUMBER_BASE", "NUMERIC", "NXFIELD",

            "OBJECT", "OCIROWID", "OCTET_LENGTH", "OF", "OFF", "OFFLINE",
            "OFFSET", "OFFSETS", "OFSAVEPOINTVALUES", "OLD", "ON", "ONCE",
            "ONLINE", "ONLY", "ONSELECTWHERE", "ONTO", "OPAQUE", "OPEN",
            "OPENDATASOURCE", "OPENQUERY", "OPENROWSET", "OPENXML",
            "OPENSETWHILE", "OPENSLEEP", "OPERATION", "OPERATOR", "OPTIMIZE",
            "OPTION", "OPTIONALLY", "OPTIONS", "OR", "ORDER", "ORDERSQLWORK",
            "ORDINALITY", "ORGANIZATION", "ORSOMEWITH", "ORSORT", "OTHERS",
            "OTHERWISE", "OUT", "OUTER", "OUTFILE", "OUTPUT", "OUTPUT_PAGE",
            "OUTSTOP", "OVER", "OVERLAPS", "OWNER", "OWNERSHIP",

            "PACK_KEYS", "PACKAGE", "PAD", "PAGE", "PAGENO", "PAGES", "PARAM",
            "PARAMETER", "PARAMETERS", "PARTIAL", "PARTITION", "PASCAL",
            "PASSWORD", "PATH", "PATHNAME", "PATTERN", "PAUSE", "PCTFREE",
            "PERCENT", "PERIOD", "PERM", "PERMANENT", "PERMIT", "PERMITSUM",
            "PIPE", "PLACE", "PLAN", "PLI", "PLS_INTEGER", "POS", "POSITION",
            "POSITIVE", "POSITIVEN", "POSTFIX", "POWER", "PRAGMA", "PRECEDES",
            "PRECISION", "PREFIX", "PREORDER", "PREPARE", "PREPARETABLE",
            "PRESERVE", "PREV", "PREVIOUS", "PREVISION", "PRIMARY", "PRINT",
            "PRINTER", "PRINTSCREEN", "PRINTSCREENSCROLL", "PRINTSUBMENU",
            "PRINTSUMU", "PRIOR", "PRIV", "PRIVATE", "PRIVILAGES",
            "PRIVILAGESTHEN", "PRIVILEGES", "PROC", "PROCEDURE", "PROCESS",
            "PROCESSEXIT", "PROCESSLIST", "PROGRAM", "PROGUSAGE", "PROMPT",
            "PROMPTSCROLLDOWN", "PROMPTTABLEDATA", "PROTECT", "PSECT",
            "PUBLIC", "PUBLICREAD", "PUT", "PUTFORM", "PUTFORMSCROLLUP",
            "PUTFORMUNLOADTABLE", "PUTOPER", "PUTOPERSLEEP", "PUTROW",
            "PUTROWSUBMENU", "PUTROWUP",

            "QUERY", "QUICK", "QUIT",

            "RAISERROR", "RANGE", "RANGETO", "RAW", "RDB$DB_KEY", "RDB$LENGTH",
            "RDB$MISSING", "RDB$VALUE", "RDB4DB_KEY", "RDB4LENGTH",
            "RDB4MISSING", "RDB4VALUE", "READ", "READS", "READONLY",
            "READPASS", "READTEXT", "READWRITE", "READY", "READ_ONLY",
            "READ_WRITE", "REAL", "RECONFIGURE", "RECONNECT", "RECORD",
            "RECOVER", "RECURSIVE", "REDISPLAY", "REDISPLAYTABLEDATA",
            "REDISPLAYVALIDATE", "REDO", "REDUCED", "REF", "REFERENCES",
            "REFERENCING", "REGEXP", "REGISTER", "REGISTERUNLOADDATA",
            "REGISTERVALIDROW", "REJECT", "RELATIVE", "RELEASE", "RELOAD",
            "RELOCATE", "RELOCATEUNIQUE", "REMOVE", "REMOVEUPRELOCATEV",
            "REMOVEVALIDATE", "REMOVEWHENEVER", "RENAME", "REPEAT",
            "REPEATABLE", "REPEATED", "REPEATVALIDROW", "REPLACE",
            "REPLACEUNTIL", "REPLICATION", "REPLSTR", "REPORT",
            "REQUEST_HANDLE", "RESERVED_PGS", "RESERVING", "RESET", "RESIGNAL",
            "RESOURCE", "REST", "RESTART", "RESTORE", "RESTRICT", "RESULT",
            "RESUME", "RETRIEVE", "RETRIEVEUPDATE", "RETURN", "RETURNS",
            "RETURNING", "REVERSE", "REVOKE", "RIGHT", "RLIKE", "ROLE",
            "ROLLBACK", "ROLLFORWARD", "ROLLBACK", "ROLLUP", "ROUND",
            "ROUTINE", "ROW", "ROWCNT", "ROWCOUNT", "ROWGUID_COL", "ROWID",
            "ROWLABEL", "ROWNUM", "ROWS", "ROWTYPE", "RPAD", "RULE", "RUN",
            "RUNTIME",

            "SAMPLSTDEV", "SAVE", "SAVEPOINT", "SAVEPOINTWHERE", "SAVEVIEW",
            "SCHEMA", "SCOPE", "SCREEN", "SCROLL", "SCROLLDOWN", "SCROLLUP",
            "SEARCH", "SECOND", "SECTION", "SEGMENT", "SEL", "SELE", "SELEC",
            "SELECT", "SELUPD", "SEPERATE", "SEQUENCE", "SERIAL", "SESSION",
            "SESSION_USER", "SET", "SETOF", "SETS", "SETWITH", "SET_EQUEL",
            "SET_FRS", "SET_INGRES", "SETUSER", "SHARE", "SHARED", "SHORT",
            "SHOW", "SHUTDOWN", "SIGNAL", "SIZE", "SKIP", "SLEEP",
            "SMALLFLOAT", "SMALLINT", "SOME", "SONAME", "SORT", "SORTERD",
            "SOUNDS", "SOURCEPOS", "SPACE", "SPACES", "SPECIFIC",
            "SPECIFICTYPE", "SQL", "SQL_BIG_RESULT", "SQL_BIG_SELECTS",
            "SQL_BIG_TABLES", "SQL_LOG_OFF", "SQL_LOG_UPDATE",
            "SQL_LOW_PRIORITY_UPDATES", "SQL_SELECT_LIMIT", "SQL_SMALL_RESULT",
            "SQL_WARNINGS", "SQLCODE", "SQLDA", "SQLERRM", "SQLERROR",
            "SQLEXCEPTION", "SQLEXEPTION", "SQLEXPLAIN", "SQLNOTFOUND",
            "SQLSTATE", "SQLWARNING", "SQRT", "STABILITY", "START", "STARTING",
            "STARTPOS", "START_SEGMENT", "START_SEGMENTED_?", "START_STREAM",
            "START_TRANSACTION", "STATE", "STATIC", "STATISTICS", "STATUS",
            "STDDEV", "STDEV", "STEP", "STOP", "STORE", "STRAIGHT_JOIN",
            "STRING", "STRUCTURE", "SUBMENU", "SUBSTR", "SUBSTRING", "SUBTYPE",
            "SUCCEEDS", "SUCCESFULL", "SUCCESSFULL", "SUCCESSFUL", "SUM", "SUMU", "SUPERDBA",
            "SYB_TERMINATE", "SYNONYM", "SYSDATE", "SYSSORT", "SYSTEM_USER",

            "TABLE", "TABLEDATA", "TABLES", "TEMP", "TEMPORARY", "TERMINATE",
            "TERMINATED", "TEXT", "TEXTSIZE", "THAN", "THEN", "THROUGH",
            "THRU", "TID", "TIME", "TIMESTAMP", "TIMEZONE_HOUR",
            "TIMEZONE_MINUTE", "TINYBLOB", "TINYINT", "TINYTEXT", "TO",
            "TODAY", "TOLOWER", "TOP", "TOTAL", "TOUPPER", "TP", "TRAILER",
            "TRAILING", "TRAN", "TRANS", "TRANSACTION", "TRANSACTION_HANDLE",
            "TRANSFER", "TRANSLATE", "TRANSLATION", "TREAT", "TRIGGER",
            "TRING", "TRUE", "TRUNC", "TRUNCATE", "TSEQUAL", "TYPE",

            "UID", "UNBUFFERED", "UNDER", "UNDO", "UNION", "UNIQUE", "UNKNOWN",
            "UNLISTEN", "UNLOAD", "UNLOADDATA", "UNLOADTABLE", "UNLOCK",
            "UNTIL", "UP", "UPDATE", "UPDATETEXT", "UPPER", "USAGE", "USE",
            "USED_PGS", "USER", "USING", "UTC_TIME",

            "VACUUM", "VALIDATE", "VALIDROW", "VALUE", "VALUES", "VARBINARY",
            "VARC", "VARCH", "VARCHA", "VARCHAR", "VARGRAPHIC", "VARIABLE",
            "VARIABLES", "VARIANCE", "VARYING", "VERB_TIME", "VERBOSE",
            "VERIFY", "VERSION", "VIEW",

            "WAIT", "WAITFOR", "WAITING", "WARNING", "WEEKDAY", "WHEN",
            "WHENEVER", "WHERE", "WHILE", "WINDOW", "WITH", "WITHOUT", "WORK",
            "WRAP", "WRITE", "WRITEPASS", "WRITETEXT",

            "YEAR", "YEARS",

            "ZEROFILL", "ZONE" };

    private ModelEntityChecker() {}
}

