/*
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

import java.util.Arrays;
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
import org.apache.ofbiz.entity.config.model.Datasource;
import org.apache.ofbiz.entity.config.model.EntityConfig;

/**
 * Generic Entity - Entity Definition Checker
 */
public final class ModelEntityChecker {

    private static final String MODULE = ModelEntityChecker.class.getName();

    public static void checkEntities(Delegator delegator, List<String> warningList) throws GenericEntityException {
        ModelReader reader = delegator.getModelReader();

        if (Debug.infoOn()) {
            Debug.logInfo("[initReservedWords] array length = " + RW_ARRAY.length, MODULE);
        }
        Set<String> reservedWords = new HashSet<>(Arrays.asList(RW_ARRAY));

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
            if (UtilValidate.isNotEmpty(ent.getPlainTableName())) {
                tableNames.add(ent.getPlainTableName());
            }

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
                        warningList.add(String.format("[GroupNotFound] No Group Name found for entity %s.", entityName));
                    } else {
                        warningList.add(String.format("[HelperNotFound] No Helper (DataSource) definition found for entity [%s]"
                                + " because there is no helper (datasource) configured for the entity group it is in: [%s]",
                                entityName, groupName));
                    }
                }
                Datasource datasource = EntityConfig.getDatasource(delegator.getGroupHelperInfo(groupName).getHelperBaseName());
                int maxTableNameSize = datasource.getMaxTableNameSize();
                if (entity.getPlainTableName() != null && entity.getPlainTableName().length() > maxTableNameSize) {
                    warningList.add(String.format("[TableNameGT%s] Table name [%s] of entity %s is longer than %s characters.",
                            maxTableNameSize, entity.getPlainTableName(), entityName, maxTableNameSize));
                }
                if (entity.getPlainTableName() != null && reservedWords.contains(entity.getPlainTableName().toUpperCase(Locale.getDefault()))) {
                    warningList.add(String.format("[TableNameRW] Table name [%s] of entity %s is a reserved word.",
                            entity.getPlainTableName(), entityName));
                }

                // don't check columns/relations/keys when never-check is set to "true"
                if (entity.getNeverCheck()) {
                    continue;
                }

                Set<String> uniqueFields = new HashSet<>();
                Iterator<ModelField> fieldIter = entity.getFieldsIterator();
                while (fieldIter.hasNext()) {
                    ModelField field = fieldIter.next();
                    ModelFieldType type = delegator.getEntityFieldType(entity, field.getType());

                    if (uniqueFields.contains(field.getName())) {
                        warningList.add(String.format("[FieldNotUnique] Field [%s] of entity %s is not unique for that entity.",
                                field.getName(), entityName));
                    } else {
                        uniqueFields.add(field.getName());
                    }
                    int maxColumnNameSize = datasource.getMaxColumnNameSize();
                    if (field.getColName().length() > maxColumnNameSize && !(entity instanceof ModelViewEntity)) {
                        warningList.add(String.format("[FieldNameGT%s] Column name [%s] of entity %s is longer than %s characters.",
                                maxColumnNameSize, field.getColName(), entityName, maxColumnNameSize));
                    }
                    if (field.getColName().isEmpty()) {
                        warningList.add(String.format("[FieldNameEQ0] Column name for field name \"%s\" of entity %S is empty (zero length).",
                                field.getName(), entityName));
                    }
                    if (reservedWords.contains(field.getColName().toUpperCase(Locale.getDefault()))) {
                        warningList.add(String.format("[FieldNameRW] Column name %s of entity %s is a reserved word.",
                                field.getColName(), entityName));
                    }
                    if (type == null) {
                        StringBuilder warningMsg = new StringBuilder("[FieldTypeNotFound] Field type ")
                                .append(field.getType())
                                .append(" of entity ")
                                .append(entityName)
                                .append(" not found in field type definitions");
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
                            warningList.add(String.format("[IndexDuplicateName] Index on entity %s has a duplicate index-name \"%s\".",
                                    entityName, index.getName()));
                        } else {
                            indexNames.add(index.getName());
                        }

                        if (tableNames.contains(index.getName())) {
                            warningList.add(String.format("[IndexTableDupName] Index on entity %s has an index-name \"%s\""
                                            + " that is also being used as a table name.",
                                    entityName, index.getName()));
                        }

                        if (fkNames.contains(index.getName())) {
                            warningList.add(String.format("[IndexFKDupName] Index on entity %s has an index-name \"%s\""
                                            + " that is also being used as a Foreign Key name.",
                                    entityName, index.getName()));
                        }

                        // make sure all names are <= maxIndexNameSize characters
                        int maxIndexNameSize = datasource.getMaxIndexNameSize();
                        if (index.getName().length() > maxIndexNameSize) {
                            warningList.add(String.format("[IndexNameGT%s] The index name %s (length: %s) was greater than %s"
                                            + " characters in length for entity %s.",
                                    maxIndexNameSize, index.getName(), index.getName().length(), maxIndexNameSize, entityName));
                        }
                    }

                    Set<String> relations = new HashSet<>();
                    for (int r = 0; r < entity.getRelationsSize(); r++) {
                        ModelRelation relation = entity.getRelation(r);

                        if (!entityNames.contains(relation.getRelEntityName())) {
                            warningList.add(String.format("[RelatedEntityNotFound] Related entity %s of entity %s not found.",
                                    relation.getRelEntityName(), entityName));
                        }
                        if (relations.contains(relation.getTitle() + relation.getRelEntityName())) {
                            warningList.add(String.format("[RelationNameNotUnique] Relation %s%s of entity %s is not unique for that entity.",
                                    relation.getTitle(), relation.getRelEntityName(), entityName));
                        } else {
                            relations.add(relation.getTitle() + relation.getRelEntityName());
                        }

                        if (!relation.getFkName().isEmpty()) {
                            if (fkNames.contains(relation.getFkName())) {
                                warningList.add(String.format("[RelationFkDuplicate] Relation to %s"
                                                + " from entity %s has a duplicate fk-name \"%s\".",
                                        relation.getRelEntityName(), entityName, relation.getFkName()));
                            } else {
                                fkNames.add(relation.getFkName());
                            }
                            if (tableNames.contains(relation.getFkName())) {
                                warningList.add(String.format("[RelationFkTableDup] Relation to %s from entity "
                                                + "%s has an fk-name \"%s\" that is also being used as a table name.",
                                        relation.getRelEntityName(), entityName, relation.getFkName()));
                            }
                            if (indexNames.contains(relation.getFkName())) {
                                warningList.add(String.format("[RelationFkTableDup] Relation to %s from entity %s has an fk-name \""
                                                + "%s\" that is also being used as an index name.",
                                        relation.getRelEntityName(), entityName, relation.getFkName()));
                            }
                        }

                        // make sure all FK names are <= maxFkNameSize characters
                        int maxFkNameSize = datasource.getMaxFkNameSize();
                        if (relation.getFkName().length() > maxFkNameSize) {
                            warningList.add(String.format("[RelFKNameGT%s] The foreign key named %s"
                                    + " (length %s) was greater than %s characters in length for relation %s%s of entity %s.",
                                    maxFkNameSize, relation.getFkName(), relation.getFkName().length(),
                                    maxFkNameSize, relation.getTitle(), relation.getRelEntityName(), entityName));
                        }

                        ModelEntity relatedEntity = null;
                        try {
                            relatedEntity = reader.getModelEntity(relation.getRelEntityName());
                        } catch (GenericEntityException e) {
                            Debug.logInfo("Entity referred to in relation is not defined: " + relation.getRelEntityName(), MODULE);
                        }
                        if (relatedEntity != null) {
                            //if relation is of type one, make sure keyMaps
                            // match the PK of the relatedEntity
                            if ("one".equals(relation.getType()) || "one-nofk".equals(relation.getType())) {
                                if (relatedEntity.getPksSize() != relation.getKeyMaps().size()) {
                                    warningList.add(String.format("[RelatedOneKeyMapsWrongSize] The number of primary keys (%s) of related entity %s"
                                                    + " does not match the number of keymaps (%s) for relation of type one \"%s%s\" of entity %s.",
                                            relatedEntity.getPksSize(), relation.getRelEntityName(),
                                            relation.getKeyMaps().size(), relation.getTitle(), relation.getRelEntityName(),
                                            entityName));
                                }
                                Iterator<ModelField> pksIter = relatedEntity.getPksIterator();
                                while (pksIter.hasNext()) {
                                    ModelField pk = pksIter.next();
                                    if (relation.findKeyMapByRelated(pk.getName()) == null) {
                                        warningList.add(String.format("[RelationOneRelatedPrimaryKeyMissing] The primary key \"%s\""
                                                        + " of related entity %s is missing in the keymaps for relation \"%s%s\" of type one"
                                                        + " of entity %s.",
                                                pk.getName(), relation.getRelEntityName(), relation.getTitle(),
                                                relation.getRelEntityName(), entityName));
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
                                warningList.add(String.format("[RelationRelatedFieldNotFound] The field \"%s\" of related entity"
                                        + " %s was specified in the keymaps but is not found for relation %s%s of entity %s.",
                                        keyMap.getRelFieldName(), relation.getRelEntityName(),
                                        relation.getTitle(), relation.getRelEntityName(), entityName));
                            }
                            if (field == null) {
                                warningList.add(String.format("[RelationFieldNotFound] The field %s was specified in the keymaps but is not found"
                                        + " for relation %s%s of entity %s.",
                                        keyMap.getFieldName(), relation.getTitle(), relation.getRelEntityName(), entityName));
                            }
                            if (field != null && rfield != null) {
                                if (!field.getType().equals(rfield.getType())) {
                                    warningList.add(String.format("[RelationFieldTypesDifferent] The field type (%s) of %s of entity %s"
                                            + " is not the same as field type (%s) of %s of entity %s for relation %s%s.",
                                            field.getType(), field.getName(), entityName, rfield.getType(),
                                            rfield.getName(), relation.getRelEntityName(), relation.getTitle(),
                                            relation.getRelEntityName()));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static final String[] RW_ARRAY = {"ABORT", "ABS", "ABSOLUTE",
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

    private ModelEntityChecker() { }
}

