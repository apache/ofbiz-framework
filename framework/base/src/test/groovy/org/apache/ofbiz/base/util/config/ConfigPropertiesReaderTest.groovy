package org.apache.ofbiz.base.util.config

import org.apache.ofbiz.base.config.TypesafeConfigImplReader
import org.apache.ofbiz.base.util.UtilProperties
import org.junit.jupiter.api.Test

// codenarc-disable UnnecessaryGString
class ConfigPropertiesReaderTest extends ConfigReaderTest {

    private TypesafeConfigImplReader confReader

    @Test
    void testGetNormalPropertyValue() {
        confReader = initReaderAndProperties('{}', 'tast', 'some.test.property=somevalue')
        assert UtilProperties.getPropertyValue('tast', 'some.test.property') == 'somevalue'
    }

    @Test
    void testGetHoconPropertyValueInline() {
        confReader = initReaderAndProperties("""{
    "tast": {
      "some.test.property": "somevalue"
    }
}""")
        assert UtilProperties.getPropertyValue('tast', 'some.test.property') == 'somevalue'
    }

    @Test
    void testGetHoconPropertyValueNested() {
        confReader = initReaderAndProperties("""{
    "tast": {
      "some" : {
        "test" : { "property": "somevalue" }
      }
    }
}""")
        assert UtilProperties.getPropertyValue('tast', 'some.test.property') == 'somevalue'
    }

    @Test
    void testGetHoconPropertyValueInlineOverload() {
        confReader = initReaderAndProperties("""{
    "tast": {
      "some.test.property": "somevalueOVERLOADED"
    }
}""", 'tast', 'some.test.property=somevalue')
        assert UtilProperties.getPropertyValue('tast', 'some.test.property') == 'somevalueOVERLOADED'
    }

    @Test
    void testGetHoconPropertyValueNestedOverload() {
        confReader = initReaderAndProperties("""{
    "tast": {
      "some" : {
        "test" : { "property": "somevalueOVERLOADEDAGAIN" }
      }
    }
}""", 'tast', 'some.test.property=somevalue')
        assert UtilProperties.getPropertyValue('tast', 'some.test.property') == 'somevalueOVERLOADEDAGAIN'
    }

}
