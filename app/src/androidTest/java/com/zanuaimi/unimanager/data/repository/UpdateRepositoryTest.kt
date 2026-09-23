package com.zanuaimi.unimanager.data.repository

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UpdateRepositoryTest {
    private val repository = UpdateRepository(
        InstrumentationRegistry.getInstrumentation().targetContext as Context,
    )

    @Test
    fun newerSemanticReleaseIsDetected() {
        assertTrue(repository.compareVersions("1.8.0", "1.7.3") > 0)
        assertTrue(repository.compareVersions("v1.8.0", "v1.7.3") > 0)
    }
}
