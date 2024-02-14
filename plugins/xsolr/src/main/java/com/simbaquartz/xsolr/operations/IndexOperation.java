package com.simbaquartz.xsolr.operations;

public enum IndexOperation {
    ADD("add"),
    SET("set"),
    REMOVE("remove");

    private final String solrAtomicUpdateModifier;

    IndexOperation(String solrAtomicUpdateModifier) {
        this.solrAtomicUpdateModifier = solrAtomicUpdateModifier;
    }

    public static IndexOperation getEnumByName(String description) {
        switch (description) {
            case "add":
                return IndexOperation.ADD;
            case "set":
                return IndexOperation.SET;
            case "remove":
                return IndexOperation.REMOVE;
            default:
                return null;
        }
    }

    public String getSolrAtomicUpdateModifier() {
        return this.solrAtomicUpdateModifier;
    }
}