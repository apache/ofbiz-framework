package com.simbaquartz.xcommon.models.account;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * A user object represents a compact version of an account {@link ApplicationUser} in MMO that can
 * be given access to various workspaces, projects, and tasks.
 *
 * <p>Like other objects in the system, users are referred to by numerical IDs. However, the special
 * string identifier self can be used anywhere a user ID is accepted, to refer to the current
 * authenticated user.
 */
@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class User {
    /**
     * Identifier of the user. Represents the users party id.
     */
    @JsonProperty("id")
    private String id;

    /**
     * Display name of the user.
     */
    @JsonProperty("displayName")
    private String displayName;

    /**
     * Email of the user.
     */
    @JsonProperty("email")
    private String email;

    /**
     * Photo Url name of the user.
     */
    @JsonProperty("photoUrl")
    private String photoUrl;

    /**
     * Represents whether the member account is enabled or not. Once a member is removed this will
     * return true. Uses the statusId to determine this flag, PARTY_DISABLED represents a disabled member.
     */
    @JsonProperty("disabled")
    private Boolean disabled;

    @JsonProperty("statusId")
    private Boolean statusId;

    /**
     * Whether the user corresponds to the user requesting details (logged in user). The default is
     * False.
     */
    @JsonProperty("self")
    private Boolean self;

    public User(
            String id,
            String displayName,
            String email,
            String photoUrl,
            boolean self,
            boolean disabled) {
        this.id = id;
        this.displayName = displayName;
        this.email = email;
        this.photoUrl = photoUrl;
        this.self = self;
        this.disabled = disabled;
    }

    public User() {
    }

    /**
     * Example usage: <code>
     * Map partyBasicDetails = AxPartyHelper.getPartyBasicDetails(partyObj);
     * assigneeRecordsCount.setUser(User.populateUserModel(partyBasicDetails));
     *
     * </code>
     *
     * @param userDetails
     * @return
     */
/*  public static User populateUserModel(Map userDetails) {
    if (UtilValidate.isEmpty(userDetails)) {
      return null;
    }

    boolean disabled = !AxPartyHelper.isPartyEnabled((String) userDetails.get("statusId"));
    return new User(
        (String) userDetails.get("id"),
        (String) userDetails.get("displayName"),
        (String) userDetails.get("email"),
        (String) userDetails.get("photoUrl"),
        false,
        disabled);
  }*/
}
