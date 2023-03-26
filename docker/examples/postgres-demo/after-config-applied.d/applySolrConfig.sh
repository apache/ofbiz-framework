#!/usr/bin/env bash
set -x

if [ -d /ofbiz/plugins/solr ]; then
  sed "s/^solr.webapp.domainName=.*/solr.webapp.domainName=${OFBIZ_HOST}/" \
    /ofbiz/plugins/solr/config/solrconfig.properties > /ofbiz/config/solrconfig.properties
fi
