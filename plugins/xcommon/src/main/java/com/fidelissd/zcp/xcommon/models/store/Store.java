package com.fidelissd.zcp.xcommon.models.store;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fidelissd.zcp.xcommon.models.Photo;
import com.fidelissd.zcp.xcommon.models.account.ApplicationAccount;
import com.fidelissd.zcp.xcommon.models.people.Person;
import com.fidelissd.zcp.xcommon.models.geo.PostalAddress;
import lombok.Data;

import java.util.List;

/**
 * The Store resource stores information about a Store (shop), such as their country, supported
 * currencies. contact details, their order history, and whether they've agreed to receive email
 * marketing.
 */
@Data
public class Store {
    /**
     * Unique identifier of a Store. For example, storeX may have a sequential system generated store
     * id much like 100001.
     */
    @JsonProperty("id")
    private String id = null;

    /**
     * website id for store
     */
    @JsonProperty("web_site_id")
    private String webSiteId = null;

    /**
     * catalog id for store
     */
    @JsonProperty("catalog_id")
    private String catalogId = null;

    /**
     * Name of the Store. Example "Acme toys Inc."
     */
    @JsonProperty("name")
    private String name = null;

    /**
     * Displays default Currency UOM id set for the store. For example, USD for US dollars, INR for
     * Indian Rupees.
     */
    @JsonProperty("defaultCurrency")
    private String defaultCurrency = null;

    /**
     * The shop's postal address.
     */
    @JsonProperty("address")
    private PostalAddress address = null;

    /**
     * Contact person details of the store.
     */
    @JsonProperty("person")
    private Person person = null;

    /**
     * Primary email of the store. Email to be shown to customers and vendors to contact store.
     */
    @JsonProperty("email")
    private String email = null;

    /**
     * Primary phone of the store.
     */
    @JsonProperty("phone")
    private String phone = null;

    /**
     * Default Unit of Measure (UOM) for products and shipping. E.g. gram, pound etc.
     */
    @JsonProperty("weight_unit")
    private String weightUnit = null;

    /**
     * Logo Image URL of the store.
     */
    @JsonProperty("logo_image")
    private String logoImage = null;

    /**
     * Shop images, store front images to help customer locate the store.
     */
    @JsonProperty("photos")
    private Photo photos = null;

    /**
     * Domain associated with the store. If the store has an online presence, for example
     * mytoysstore.com.
     */
    @JsonProperty("domain")
    private String domain = null;

    /**
     * The time when the store was created, in RFC 3339 format.
     */
    @JsonProperty("created_at")
    private String createdAt = null;

    /**
     * Store business hours. Represents the time periods that this location is open for business.
     * Holds a collection of TimePeriod instances.
     */
    @JsonProperty("businessHours")
    private List<TimePeriod> businessHours = null;

    /**
     * The time when the store was last updated, in RFC 3339 format.
     */
    @JsonProperty("updated_at")
    private String updatedAt = null;

    /**
     * Store level settings/preferences.
     */
    @JsonProperty("settings")
    private StoreSetting settings = null;

    /**
     * Application account, used when creating a new store, if this is passed it'll also create an
     * application account and link the account with store.
     */
    @JsonProperty("account")
    private ApplicationAccount account = null;


}
