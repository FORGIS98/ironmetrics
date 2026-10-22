package com.forgis.ironmetrics.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimeUtilsTest {
    @Test fun parsesHms() = assertEquals(3750, TimeUtils.parse("01:02:30"))
    @Test fun parsesMs() = assertEquals(3180, TimeUtils.parse("53:00"))
    @Test fun rejectsInvalid() {
        assertNull(TimeUtils.parse("abc"))
        assertNull(TimeUtils.parse("00:61:00"))
    }
    @Test fun formats() = assertEquals("00:53:00", TimeUtils.format(3180))
}
