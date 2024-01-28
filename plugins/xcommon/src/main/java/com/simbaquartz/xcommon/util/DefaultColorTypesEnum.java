package com.simbaquartz.xcommon.util;

/**
 * Available default color types
 */
public enum DefaultColorTypesEnum {
  DEFAULT("DEFAULT", "Default color", "#007aff", "#ffffff"),
  BLUE("blue", "Color blue", "#039be5", "#ffffff"),
  LAVENDER("lavender", "Color lavender", "#7986cb", "#ffffff"),
  SAGE("sage", "Color sage", "#33b679", "#ffffff"),
  GRAPE("grape", "Color grape", "#8e24aa", "#ffffff"),
  FLAMINGO("flamingo", "Color flamingo", "#e67c73", "#ffffff"),
  BANANA("banana", "Color banana", "#f6c026", "#ffffff"),
  TANGERINE("tangerine", "Color tangerine", "#f5511d", "#ffffff"),
  PEACOCK("peacock", "Color peacock", "#039be5", "#ffffff"),
  GRAPHITE("graphite", "Color graphite", "#616161", "#ffffff"),
  BLUEBERRY("blueberry", "Color blueberry", "#3f51b5", "#ffffff"),
  BASIL("basil", "Color basil", "#0b8043", "#ffffff"),
  TOMATO("tomato", "Color tomato", "#d60000", "#ffffff");

  private String id;
  private String description;
  private String backgroundColor;
  private String foregroundColor;

  DefaultColorTypesEnum(
      String colorId, String description, String backgroundColor, String foregroundColor) {
    this.id = colorId;
    this.description = description;
    this.backgroundColor = backgroundColor;
    this.foregroundColor = foregroundColor;
  }

  public String getId() {
    return id;
  }

  public String getDescription() {
    return description;
  }

  public String getBackgroundColor() {
    return backgroundColor;
  }

  public String getForegroundColor() {
    return foregroundColor;
  }
}
