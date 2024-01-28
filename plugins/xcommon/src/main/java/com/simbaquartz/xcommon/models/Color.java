package com.simbaquartz.xcommon.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Represents a Color object that can be used with tasks/statuses/projects etc.
 */
@Data
@Builder
@EqualsAndHashCode(callSuper=false)
@AllArgsConstructor
@NoArgsConstructor
public class Color {

  /**
   * A unique id for the color
   */
  @JsonProperty("id")
  private String id;

  /**
   * Foreground color  hex code like #1d1d1d
   */
  @JsonProperty("foreground")
  private String foreground;

  /**
   * Background color hex code like #ac725e
   */
  @JsonProperty("background")
  private String background;

  /**
   * Returns default color.
   * @return
   */
  public static Color defaultColor(){
    Color defaultColor = Color.builder().build();
    defaultColor.setId("DEFAULT");
    defaultColor.setBackground("#007aff");
    defaultColor.setForeground("#ffffff");
    return defaultColor;
  }
}
