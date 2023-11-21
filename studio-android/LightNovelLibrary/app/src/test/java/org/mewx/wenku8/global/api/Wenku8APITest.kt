package org.mewx.wenku8.global.api

import org.junit.Assert

@RunWith(MockitoJUnitRunner::class)
class Wenku8APITest {
    @Before
    fun init() {
    }

    @Test
    fun testGetEncryptedMAP() {
        val str = "test"
        val map: Map<String?, String?> = Wenku8API.getEncryptedMAP(str)
        Assert.assertEquals(map.size(), 3)
        Assert.assertEquals(map["appver"], BuildConfig.VERSION_NAME)
        Assert.assertEquals(LightBase64.DecodeBase64String(Objects.requireNonNull(map["request"])), str)
        Assert.assertTrue(Long.parseLong(Objects.requireNonNull(map["timetoken"])) > 0L)
    }
}