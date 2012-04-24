package org.ofbiz.jcr.orm;

public interface OfbizRepositoryMapping {
    /**
     * Return the Node Path.
     *
     * @return
     */
    String getPath();

    /**
     * Set the Node Path.
     *
     * @param path
     */
    void setPath(String path);

    /**
     * Return the current Version of the content object.
     *
     * @return
     */
    public String getVersion();

    /**
     * Set the node version.
     *
     * @param version
     */
    public void setVersion(String version);

    /**
     * Set the party ID of the user which created the content
     *
     * @param partyId
     */
    public void setPartyThatCreatedTheContent(String partyId);

    /**
     * Returns the party ID of the user which created the content
     *
     * @return
     */
    public String getPartyThatCreatedTheContent();

    /**
     * Set the party ID of the user which modifies the content last
     *
     * @param partyId
     */
    public void setLastUpdatedParty(String partyId);

    /**
     * Returns the party ID of the user which modifies the content last
     *
     * @return
     */
    public String getLastUpdatedParty();
}