Project: jackson-jr

------------------------------------------------------------------------
=== Releases ===
------------------------------------------------------------------------

2.11.0 (not yet released)

-

2.10.1 (not yet released)

-

2.10.0 (26-Sep-2019)

#60: Add support for reading "root value" streams (linefeed separated/concatenated)
#63: Change default for `JSON.Feature.USE_FIELDS` to `true` (from false) in 2.10
#65: Allow registration of custom readers, writers (to support 3rd party, custom types)
#66: Add `Json.mapOfFrom(Class)` to support binding POJO-valued maps
 (suggested by ocind@github)
- Add `JrsNull` node type for `jr-stree` package
- Add JDK9+ `module-info.class` using Moditect
- Update Retrofit2 version (2.0.0 -> 2.5.0)

2.9.10 (21-Sep-2019)

No changes since 2.9.9

2.9.9 (16-May-2019)

- Fix an issue with Maps-of-Lists, Lists-of-Maps

2.9.8 (15-Dec-2018)
2.9.7 (19-Sep-2018)
2.9.6 (12-Jun-2018)
2.9.5 (26-Mar-2018)
2.9.4 (24-Jan-2018)
2.9.3 (09-Dec-2017)
2.9.2 (14-Oct-2017)
2.9.1 (07-Sep-2017)
2.9.0 (30-Jul-2017)

No changes since 2.8

2.8.10 (not yet released)

#53: `java.io.File` is not a valid source for anyFrom()/mapFrom()
 (reported by CoreyTeffetalor@github)

2.8.9 (12-Jun-2017)

#50: Duplicate key detection does not work
 (reported by inorick@github)

2.8.8 (05-Apr-2017)
2.8.7 (21-Feb-2017)
2.8.6 (12-Jan-2017)
2.8.5 (14-Nov-2016)

No changes since 2.8.4

2.8.4 (14-Oct-2016)

#49: ArrayIndexOutOfBoundsException when parsing large Map
 (reported by Michael J)

2.8.3 (17-Sep-2016)
2.8.2 (30-Aug-2016)
2.8.1 (19-Jul-2016)

No changes since 2.8.0

2.8.0 (04-Jul-2016)

#26: Allow use of public fields for getting/setting values
#43: Add convenience read method for reading trees via `JSON`

2.7.9 (not yet released)

2.7.8 (26-Sep-2016)
2.7.7 (27-Aug-2016)
2.7.6 (23-Jul-2016)

No changes since 2.7.5.

2.7.5 (11-Jun-2016)

#42: Incorrect `jackson-core` dependency form parent pom leads to inclusion
  of non-shaded core jar in `jr-all`
 (reported by Adam V)

2.7.4 (29-Apr-2016)

No changes since 2.7.3.

2.7.3 (16-Mar-2016)

#37: Update Jackson Jr Retrofit 2 Converter for Retrofit 2.0.0
 (contributed by GulajavaMinistudio@github)
#38: PRETTY_PRINT_OUTPUT with composer doesn't work
 (reported by weizhu-us@github)

2.7.2 (26-Feb-2016)
2.7.1 (02-Feb-2016)

No changes since 2.7.0

2.7.0 (10-Jan-2016)

#28: Remove misspelled `JSON.Feature.USE_IS_SETTERS`
#29: Add `JSON.Feature.WRITE_DATES_AS_TIMESTAMP`, enabling of which allows
 serialization of `java.util.Date` as long
 (suggested by Giulio P (gpiancastelli@github))
#30: Add initial version of jackson-jr - based Retrofit2 Converter
 (contributed by GulajavaMinistudio@github)
#31: Fix failure writing UUID, URL and URI
 (reported by Mitsunori K (komamitsu@github))
#34: Add basic read-only (immutable) tree model impementation (stree)

2.6.6 (05-Apr-2016)

#40: Cannot read empty or singleton arrays with JSON.arrayOfFrom
 (reported by Giulio P)

2.6.5 (19-Jan-2015)
2.6.4 (07-Dec-2015)

#27: JSON.Feature.READ_JSON_ARRAYS_AS_JAVA_ARRAYS does not work
- Minor fix to resolution of self-referential types (fix #28 from java-classmate)

2.6.3 (12-Oct-2015)
2.6.2 (14-Sep-2015)
2.6.1 (09-Aug-2015)

No changes since 2.6.0

2.6.0 (19-Jul-2015)

#24: String/byte[] composers can not write POJOs (ObjectCodec not linked)
- Minor performance optimizations, using new jackson-core 2.6 methods

2.5.3 (24-Apr-2015)
2.5.2 (29-Mar-2015)
2.5.1 (06-Feb-2015)
2.5.0 (01-Jan-2015)

No changes since 2.4.

2.4.4 (24-Nov-2014)
2.4.3 (04-Oct-2014)

No changes since 2.4.2

2.4.2 (13-Aug-2014)

#15: Problem with Number to Long conversions
 (reported by "gsmiro@github")
#16: Error serializing POJO-valued Maps
 (reported by Zac M (ZacWolf@github))

2.4.1 (17-Jun-2014)

No changes since 2.4.0

2.4.0 (29-May-2014)

#9: Support lazily materialized Maps with `JSON.Feature.USE_DEFERRED_MAPS`
#11: Replace use of `java.bean.Introspector` since Android SDK doesn't have it.

2.3.3 (10-Apr-2014)

No changes, but depends on Jackson 2.3.3 core.

2.3.1 (26-Mar-2014)

#6: Support reading of `Enum` values from String representation
#8: Handle generic Collection and Map types properly

2.3.0 (17-Mar-2014)

The very first publicly available release!
