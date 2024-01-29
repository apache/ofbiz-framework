package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * Represents a Quote Role object.
 **/

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2018-05-11T11:44:45.432+05:30")
public class QuoteRole   {
  
  private String id = null; //quoteId
  private String partyId = null;
  private String name = null;
  private String roleTypeId = null;
  private String displayName = null;

  /**
   * The unique quote id for the particular quote role.
   **/
  
  @JsonProperty("id")
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }

  /**
   * The unique partyId for the role.
   **/

  @JsonProperty("party_id")
  public String getPartyId() {
    return partyId;
  }
  public void setPartyId(String partyId) {
    this.partyId = partyId;
  }

  /**
   * The name of the party having role.
   **/
  
  @JsonProperty("name")
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  /**
   * The unique role type for the party.
   **/
  
  @JsonProperty("role_type_id")
  public String getRoleTypeId() {
    return roleTypeId;
  }
  public void setRoleTypeId(String roleTypeId) {
    this.roleTypeId = roleTypeId;
  }

  /**
   * The name of the role.
   **/
  
  @JsonProperty("display_name")
  public String getDisplayName() {
    return displayName;
  }
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QuoteRole quoteRole = (QuoteRole) o;
    return Objects.equals(id, quoteRole.id) &&
        Objects.equals(partyId, quoteRole.partyId) &&
        Objects.equals(name, quoteRole.name) &&
        Objects.equals(roleTypeId, quoteRole.roleTypeId) &&
        Objects.equals(displayName, quoteRole.displayName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, partyId, name, roleTypeId, displayName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class QuoteRole {\n");
    
    sb.append("    quoteId: ").append(toIndentedString(id)).append("\n");
    sb.append("    partyId: ").append(toIndentedString(partyId)).append("\n");
    sb.append("    partyName: ").append(toIndentedString(name)).append("\n");
    sb.append("    roleTypeId: ").append(toIndentedString(roleTypeId)).append("\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

