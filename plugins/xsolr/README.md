# SOLR Best Practices Guide

## Use beans to map solr schema
Maintaining a copy of solr configuration files (schema.xml/managed-schema.xml) is no longer required with the introduction of SOLJ integration.

Beans can now be easily annotated using @Fields annotation.

## Naming your solr fields
Use hyphen to separate your field names, when adding a new field in your schema make sure you add
it to the managed-schema file as well, if it's not a multivalued item set multivalued="false"
If you use SOLRJ to create the schema for you by default it'll set the multivalued="true" even if
the value you are trying to index is of type String. Keep that in mind.

Example bean.

```java
package com.simbaquartz.xparty.solr.collections;

import com.simbaquartz.xsolr.solr.collections.BaseCollection;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;

import java.util.List;

/**
 * Represents a person document managed in Solr server.
 */
@Data
public class PersonSolrCollection extends BaseCollection{
    private static final String SOLR_DOCUMENT_PREFIX_PERSON = "PRS-";

    /**
     * Represents the SOLR document ID.
     */
    @Setter(AccessLevel.NONE)
    @Field("id")
    String documentId = null;

    /**
     * Represents the type of document, example Product, Order, Person etc.
     */
    @Field("doc-type")
    private String documentType = "person";

    /**
     * Party ID of the person document indexed.
     */
    @Setter(AccessLevel.NONE)
    @Field("party-id")
    private String partyId = null;

    /**
     * Auto prefixes the person prefix and generates the id.
     */
    public void setPartyId(String partyId){
        this.partyId = partyId;
        this.documentId = SOLR_DOCUMENT_PREFIX_PERSON + partyId;
    }

    /**
     * Full name of the person
     */
    @Field("name")
    private String name = null;

    /**
     * A person's primary email.
     */
    @Field("email")
    private String email = null;

    /**
     * A person's phone number.
     * Stored in 10 digit phone format, e.g. 8769990989 for US
     */
    @Field("phone")
    private String phone = null;

    /**
     * A person's phone two digit country code. Example US for USA, IN for India
     */
    @Field("phone-country")
    private String phoneCountryCode = null;

    /**
     * The day the person record was created in the system.
     * Stored in the SOLR supported format 'YYYY-MM-DDThh:mm:ssZ' using SolrUtils.toSolrFormattedDateString
     */
    @Field("created-at")
    private String createdAt = null;

    /**
     * The day the person was last contacted via a message, call or email.
     */
    @Field("last-contacted")
    private String lastContactedAt = null;

    /**
     * List of roles held by this Person, example, customer, vendor, manager, administrator, employee
     */
    @Field("roles")
    private List<String> roles = null;

    /**
     * The country a person is in. Stored as the name of the country.
     */
    @Field("country")
    private String country = null;

    /**
     * The country a person is in. Stored as the two digit country code, US, IN representing geoCode of Geo entity.
     */
    @Field("country-code")
    private String countryCode = null;

    /**
     * A subdivision of a country, such as a state, province or territory.
     */
    @Field("region")
    private String region = null;

    /**
     * A subdivision of a country, such as a state, province or territory.
     */
    @Field("region-code")
    private String regionCode = null;

    /**
     * The city a person is in.
     */
    @Field("city")
    private String city = null;

    /**
     * The timezone name a person is in. For example "America/Los_Angeles"
     */
    @Field("timezone")
    private String timezone = null;

    /**
     * A person's employer's id.
     */
    @Field("employer-id")
    private String employerId = null;

    /**
     * A person's official job title
     */
    @Field("job-title")
    private String jobTitle = null;

    // TODO: @MSS: Need to implement dynamic fields for indexing additional person attributes users can set up
}

```

## Keeping schema changes in sync
To make sure your managed-schema changes are in sync with SOLR repository, do the following:

- Make sure SOLR is running
- Make sure the path to your SOLR server is updated for the variable in `plugins/xsolr/build.gradle`
```groovy
def solrServerCoreConfigFile="C:/servers/solr/solr-8.6.2/server/solr/MTDCORE/conf"
```
- Run the updateSolrSchema task in `plugins/xsolr/build.gradle`, this task will copy the modified managed-schema file from 
your project directory to the server's directory. It'll also reload the CORE for you.