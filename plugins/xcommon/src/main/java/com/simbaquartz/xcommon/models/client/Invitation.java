package com.simbaquartz.xcommon.models.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.simbaquartz.xcommon.models.Phone;
import com.simbaquartz.xcommon.models.account.User;
import java.sql.Timestamp;
import java.util.List;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;

/**
 * For sending out invitations to other members of an organization via email.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Invitation {
    @JsonProperty("id")
    private String id;// PartyInvitationId

    @NotNull(message = "Email id is required. Can not be empty.")
    @NotBlank(message = "Email id is required. Can not be empty.")
    @Email(message = "Please enter a valid email address. Example user@example.org")
    @JsonProperty("email")
    private String email;

    @NotNull(message = "First name is required. Can not be blank.")
    @NotBlank(message = "First name is required. Can not be empty.")
    @Size(min = 2, message = "First name should be at least two characters long.")
    @JsonProperty("fullName")
    private String fullName;

    //used by get pending invitations api to return the name of the invited person
    @JsonProperty("toName")
    private String toName;

    @JsonProperty("phone")
    private Phone phone = null;

    @JsonProperty("designation")
    private String designation;

    @JsonProperty("roles")
    private List<String> roleIds;

    @JsonProperty("status")
    private String status;

    @JsonProperty("lastInvitedOn")
    private Timestamp lastInvitedOn;

    /**
     * Date when the invitation was accepted.
     */
    @JsonProperty("acceptedDate")
    private Timestamp acceptedDate;

    @JsonProperty("invitedBy")
    private User invitedBy;

    /**
     * Invitee (person who was invited), populated and returned for accepted invitations.
     */
    @JsonProperty("invitee")
    private User invitee;

    /**
     * Number of times the invitee has been reminded
     */
    @JsonProperty("remindedCount")
    private int remindedCount;

    /**
     * When the invitation is going to expire.
     */
    @JsonProperty("expirationDate")
    private Timestamp expirationDate;
}
