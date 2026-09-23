package com.zanuaimi.unimanager.bridge

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BridgeProtocolTest {
    @Test
    fun responseContainsAtomicConfigurationEnvelope() {
        val configuration = JSONObject().put("preset", "revanced-inspired")
        val capabilities = JSONArray().put("overlay.config.v2").put("block_ads.v1")

        val response = JSONObject(
            BridgeProtocol.response(
                status = BridgeProtocol.OK,
                operation = "read",
                packageName = "example.app",
                configuration = configuration,
                fingerprint = "abc123",
                capabilities = capabilities,
                managerVersion = "1.0.0",
            ),
        )

        assertEquals(BridgeProtocol.OK, response.getString("status"))
        assertEquals(BridgeProtocol.CURRENT_VERSION, response.getInt("protocol_version"))
        assertEquals("example.app", response.getString("package_name"))
        assertEquals("abc123", response.getString("metadata_fingerprint"))
        assertEquals("revanced-inspired", response.getJSONObject("configuration").getString("preset"))
        assertEquals(2, response.getJSONArray("capabilities").length())
    }

    @Test
    fun responsePreservesExplicitFallbackReason() {
        val response = JSONObject(
            BridgeProtocol.response(
                status = BridgeProtocol.NOT_REGISTERED,
                operation = "read",
                packageName = "example.app",
                reason = "No registry entry exists for this package.",
            ),
        )

        assertEquals(BridgeProtocol.NOT_REGISTERED, response.getString("status"))
        assertTrue(response.getString("fallback_reason").isNotBlank())
    }
}
