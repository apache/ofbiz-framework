Provides a list of Windows time zones (the zones in the time zone selection dialog in Windows), and their associated Java TimeZone objects. Data was obtained from the CLDR (http://unicode.org/repos/cldr/trunk/common/supplemental/windowsZones.xml).

Contains a list of mappings in the following form:

```java
ZONEMAPPINGS.add(new TimeZoneMapping("W. Europe Standard Time", "Europe/Berlin", "(GMT +01:00) Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna"));
ZONEMAPPINGS.add(new TimeZoneMapping("Pacific Standard Time", "America/Los_Angeles", "(GMT -08:00) Pacific Time (US & Canada)"));
```

Where the first parameter is the "Windows standard name", the second parameter is the "Olson name" (this is what is used by Java as a time zone's identifier), and the third parameter is the "Windows display name" (this is what is displayed by Windows in the time zone selection dialog).