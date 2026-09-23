package com.zanuaimi.unimanager.bridge

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A deterministic fake manager service for the bridge contract. These tests exercise the
 * response and fallback matrix without depending on a separately installed UniManager APK.
 */
@RunWith(AndroidJUnit4::class)
class BridgeContractInstrumentationTest {
    private val packageName = "com.example.patched"

    @Test
    fun managerUnavailableProducesExplicitFallbackReason() {
        val service = FakeManagerService(available = false)
        val response = service.request(BridgeProtocol.PING, payload())

        assertEquals("unavailable", response.status)
        assertEquals("manager_unavailable", response.reason)
    }

    @Test
    fun coldStartRetriesUntilServiceAnswers() {
        val service = FakeManagerService(available = true, unavailableRequests = 2)
        val response = retry(service, BridgeProtocol.PING, payload())

        assertEquals(BridgeProtocol.OK, response.status)
        assertEquals("ping", response.operation)
    }

    @Test
    fun missingEntryIsNotRegistered() {
        val response = FakeManagerService().request(BridgeProtocol.READ, payload())

        assertEquals(BridgeProtocol.NOT_REGISTERED, response.status)
        assertTrue(response.reason.isNotBlank())
    }

    @Test
    fun duplicateRegistrationMergesWithoutErasingManagedValues() {
        val service = FakeManagerService()
        service.request(BridgeProtocol.REGISTER, registration("custom", false))
        service.updateConfiguration(JSONObject().put("runtimeOverlaySelectedPreset", "dark"))
        service.request(BridgeProtocol.REGISTER, registration("unipatches", false))

        val response = service.request(BridgeProtocol.READ, payload())
        assertEquals(BridgeProtocol.OK, response.status)
        assertEquals("dark", response.configuration?.getString("runtimeOverlaySelectedPreset"))
        assertEquals(false, response.configuration?.getBoolean("runtimeOverlayEnableMonitorsOnLaunch"))
    }

    @Test
    fun unsupportedCapabilityIsReported() {
        val service = FakeManagerService()
        service.request(BridgeProtocol.REGISTER, registration("custom", false, "future.capability.v9"))

        val response = service.request(BridgeProtocol.READ, payload())
        assertEquals(BridgeProtocol.UNSUPPORTED, response.status)
        assertTrue(response.reason.contains("future.capability.v9"))
    }

    @Test
    fun invalidPayloadReturnsError() {
        val response = FakeManagerService().request(BridgeProtocol.READ, "not-json")

        assertEquals(BridgeProtocol.ERROR, response.status)
    }

    @Test
    fun permissionFailureReturnsUnauthorized() {
        val service = FakeManagerService(authorizedPackage = "another.package")
        val response = service.request(BridgeProtocol.READ, payload())

        assertEquals(BridgeProtocol.UNAUTHORIZED, response.status)
    }

    @Test
    fun presetAndModuleChangesAreReturnedAtomically() {
        val service = FakeManagerService()
        service.request(BridgeProtocol.REGISTER, registration("custom", false))
        service.updateConfiguration(
            JSONObject()
                .put("runtimeOverlaySelectedPreset", "revanced-inspired")
                .put("runtimeOverlayActivateStatisticsOnLaunch", true),
        )

        val response = service.request(BridgeProtocol.READ, payload())
        assertEquals(BridgeProtocol.OK, response.status)
        assertEquals(packageName, response.packageName)
        assertEquals("overlay.config.v2", response.capabilities?.getString(0))
        assertEquals("revanced-inspired", response.configuration?.getString("runtimeOverlaySelectedPreset"))
        assertEquals(true, response.configuration?.getBoolean("runtimeOverlayActivateStatisticsOnLaunch"))
    }

    private fun retry(service: FakeManagerService, operation: Int, payload: String): FakeResponse {
        repeat(3) {
            val response = service.request(operation, payload)
            if (response.status != "unavailable") return response
        }
        return service.request(operation, payload)
    }

    private fun payload(): String = JSONObject().put("package_name", packageName).toString()

    private fun registration(preset: String, monitors: Boolean, vararg capabilities: String): String {
        val values = JSONObject()
            .put("runtimeOverlaySelectedPreset", preset)
            .put("runtimeOverlayEnableMonitorsOnLaunch", monitors)
        return JSONObject()
            .put("package_name", packageName)
            .put("capabilities", org.json.JSONArray(capabilities.toList() + "overlay.config.v2"))
            .put("configuration", values)
            .toString()
    }

    private class FakeManagerService(
        private val available: Boolean = true,
        private var unavailableRequests: Int = 0,
        private val authorizedPackage: String = "com.example.patched",
    ) {
        private val configurations = mutableMapOf<String, JSONObject>()
        private val capabilities = mutableMapOf<String, org.json.JSONArray>()

        fun request(operation: Int, rawPayload: String): FakeResponse {
            if (!available || unavailableRequests-- > 0) {
                return FakeResponse("unavailable", "manager_unavailable")
            }
            val payload = runCatching { JSONObject(rawPayload) }.getOrNull()
                ?: return FakeResponse(BridgeProtocol.ERROR, "invalid_payload")
            val packageName = payload.optString("package_name")
            if (packageName.isBlank()) return FakeResponse(BridgeProtocol.ERROR, "invalid_payload")
            if (packageName != authorizedPackage) return FakeResponse(BridgeProtocol.UNAUTHORIZED, "caller_not_authorized")
            return when (operation) {
                BridgeProtocol.PING -> FakeResponse(BridgeProtocol.OK, operation = "ping", packageName = packageName)
                BridgeProtocol.REGISTER -> {
                    val incoming = payload.optJSONObject("configuration") ?: JSONObject()
                    val current = configurations[packageName] ?: JSONObject()
                    val merged = JSONObject(current.toString())
                    incoming.keys().forEach { key -> if (!merged.has(key)) merged.put(key, incoming.get(key)) }
                    configurations[packageName] = merged
                    capabilities[packageName] = payload.optJSONArray("capabilities") ?: org.json.JSONArray()
                    FakeResponse(BridgeProtocol.OK, operation = "register", packageName = packageName, configuration = merged, capabilities = capabilities[packageName])
                }
                BridgeProtocol.UPDATE -> {
                    val current = configurations[packageName]
                        ?: return FakeResponse(BridgeProtocol.NOT_REGISTERED, "missing_entry")
                    payload.optJSONObject("configuration")?.keys()?.forEach { key ->
                        current.put(key, payload.getJSONObject("configuration").get(key))
                    }
                    FakeResponse(BridgeProtocol.OK, operation = "update", packageName = packageName, configuration = current, capabilities = capabilities[packageName])
                }
                BridgeProtocol.READ -> {
                    val current = configurations[packageName]
                        ?: return FakeResponse(BridgeProtocol.NOT_REGISTERED, "missing_entry")
                    val known = setOf("overlay.config.v2", "block_ads.v1", "block_ads_hosts.v1")
                    val unsupported = (0 until (capabilities[packageName]?.length() ?: 0))
                        .map { capabilities[packageName]?.optString(it).orEmpty() }
                        .firstOrNull { it !in known }
                    if (unsupported != null) return FakeResponse(BridgeProtocol.UNSUPPORTED, "unsupported_capability:$unsupported")
                    FakeResponse(BridgeProtocol.OK, operation = "read", packageName = packageName, configuration = current, capabilities = capabilities[packageName])
                }
                else -> FakeResponse(BridgeProtocol.UNSUPPORTED, "unsupported_operation")
            }
        }

        fun updateConfiguration(values: JSONObject) {
            val current = configurations[authorizedPackage] ?: JSONObject()
            values.keys().forEach { key -> current.put(key, values.get(key)) }
            configurations[authorizedPackage] = current
        }
    }

    private data class FakeResponse(
        val status: String,
        val reason: String = "",
        val operation: String = "",
        val packageName: String = "",
        val configuration: JSONObject? = null,
        val capabilities: org.json.JSONArray? = null,
    )
}
