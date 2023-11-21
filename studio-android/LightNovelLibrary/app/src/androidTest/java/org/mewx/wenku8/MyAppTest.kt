package org.mewx.wenku8

import android.app.Activity

@SmallTest
class MyAppTest {
    @InjectMocks
    private val myApp: MyApp? = MyApp()

    @Mock
    private val activity: Activity? = null
    @Before
    fun init() {
        initMocks(this)
    }

    @Test
    fun getContextTest() {
        `when`(myApp.getApplicationContextLocal()).thenReturn(activity)
        myApp.onCreate()
        assertEquals(activity, MyApp.getContext())
    }

    @Test
    fun getContextNullTest() {
        `when`(myApp.getApplicationContextLocal()).thenReturn(null)
        myApp.onCreate()
        assertNull(MyApp.getContext())
    }
}