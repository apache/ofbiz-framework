package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.simbaquartz.xapi.connect.models.common.Color;
import com.fidelissd.zcp.xcommon.models.account.User;
import com.fidelissd.zcp.xcommon.models.media.File;
import java.sql.Timestamp;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * A project status is an update on the progress of a particular project, and is sent out to all
 * project followers when created. These updates include both text describing the update and a color
 * code intended to represent the overall state of the project: “green” for projects that are on
 * track, “yellow” for projects at risk, “red” for projects that are behind, and "blue" for projects
 * on hold.
 *
 * <p>Project statuses can be created and deleted, but not modified.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class Folder {

    /** Unique identifier representing a specific folder Id. */
    @JsonProperty("id")
    private String id = null;

    /** The name of the folder. */
    @JsonProperty("name")
    private String name = null;

    /** The description of the folder. */
    @JsonProperty("description")
    private String description = null;

    /** The parent folder Id. */
    @JsonProperty("parentFolderId")
    private String parentFolderId = null;


    /** Represents color of a project. */
    @JsonProperty("color")
    private Color color = null;

    /** Represents projectId. */
    @JsonProperty("projectId")
    private String projectId = null;

    /** The time at which this project status was created. */
    @JsonProperty("createdAt")
    private Timestamp createdAt = null;

    /** The sequence Id for folder. */
    @JsonProperty("sequenceId")
    private String sequenceId = null;

    /** whoo has created folder. */
    @JsonProperty("ownerPartyId")
    private String ownerPartyId = null;

    @JsonProperty("sharePreference")
    private Share sharePreference = null;

    /**
     * Created by user. A {{@link User}} object represents an account in MMO that can be given access to various
     * workspaces, projects, and tasks.
     */
    @JsonProperty("createdBy")
    private User createdBy = null;

    @JsonProperty("folders")
    private List<Folder> subFolders = null;

    @JsonProperty("files")
    private List<File> files = null;

}
