package com.fidelissd.zcp.xcommon.models.company;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fidelissd.zcp.xcommon.collections.FastList;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)

public class Skills {
    /**
     * Unique Skill Type Id
     */
    @JsonProperty("skillTypeId")
    @NotNull(message = "Skill Type is required")
    @NotBlank(message = "Skill Type is required")
    private String skillTypeId;

    /**
     * Number of Years in Experience
     */
    @JsonProperty("yearsExperience")
    private Long yearsExperience;

    /**
     * Rating for skills from 0 to 5
     */
    @Max(5)
    @Min(0)
    @JsonProperty("rating")
    private Long rating;

    /**
     * Skills Level from 1 to 10
     */
    @Max(10)
    @Min(1)
    @JsonProperty("skillLevel")
    private Long skillLevel;

    /**
     * Started Using Date
     */
    @JsonProperty("startedUsingDate")
    private Timestamp startedUsingDate;

    /**
     * Description of Skills
     */
    @JsonProperty("description")
    private String description;


    public static List<Skills> preparePersonSkills(List<Map> partySkillList) {
        List<Skills> personSkillList = FastList.newInstance();
        for (Map partySkill : partySkillList) {
            Skills skills = new Skills();
            skills.setSkillTypeId((String) partySkill.get("skillTypeId"));
            skills.setDescription((String) partySkill.get("description"));
            skills.setRating((Long) partySkill.get("rating"));
            skills.setSkillLevel((Long) partySkill.get("skillLevel"));
            skills.setYearsExperience((Long) partySkill.get("yearsExperience"));
            personSkillList.add(skills);
        }
        return personSkillList;
    }
}
