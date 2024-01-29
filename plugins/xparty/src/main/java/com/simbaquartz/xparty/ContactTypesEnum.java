package com.simbaquartz.xparty;

import com.simbaquartz.xparty.utils.PartyTypesEnum;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Represents the party types that can be marked as a contact. Only a person or a company can be marked as a contact for now*/
public enum ContactTypesEnum {
  PERSON(PartyTypesEnum.PERSON.getPartyTypeId(), PartyTypesEnum.PERSON.getDescription()),
  COMPANY(PartyTypesEnum.COMPANY.getPartyTypeId(), PartyTypesEnum.COMPANY.getDescription());

  private final String typeId;
  private final String description;

  ContactTypesEnum(String typeId, String description) {
    this.typeId = typeId;
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  public String getTypeId() {
    return typeId;
  }

  /**
   * Returns the list of Enum names as a list, example ["PERSON","PARTY_GROUP"]
   * @return
   */
  public static List<String> getTypeIds() {
    return Stream.of(ContactTypesEnum.values()).map(ContactTypesEnum::getTypeId).collect(Collectors.toList());
  }
}
