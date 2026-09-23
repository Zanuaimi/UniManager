package com.zanuaimi.unimanager.bridge

import org.json.JSONArray
import org.json.JSONObject

/** Stable wire-level constants shared by the UniManager service and patched APK clients. */
object BridgeProtocol {
    const val LEGACY_VERSION = 1
    const val CURRENT_VERSION = 2

    const val REGISTER = 1
    const val READ = 2
    const val UPDATE = 3
    const val PING = 4

    const val OK = "ok"
    const val NOT_REGISTERED = "not_registered"
    const val UNAUTHORIZED = "unauthorized"
    const val UNSUPPORTED = "unsupported"
    const val ERROR = "error"
    const val PAYLOAD_TOO_LARGE = "payload_too_large"

    fun response(
        status: String,
        operation: String,
        packageName: String = "",
        reason: String = "",
        configuration: JSONObject? = null,
        fingerprint: String = "",
        capabilities: JSONArray? = null,
        managerVersion: String = "",
    ): String = JSONObject().apply {
        put("status", status)
        put("protocol_version", CURRENT_VERSION)
        put("operation", operation)
        if (packageName.isNotBlank()) put("package_name", packageName)
        if (reason.isNotBlank()) put("fallback_reason", reason)
        if (fingerprint.isNotBlank()) put("metadata_fingerprint", fingerprint)
        if (capabilities != null) put("capabilities", capabilities)
        if (managerVersion.isNotBlank()) put("manager_version", managerVersion)
        if (configuration != null) put("configuration", configuration)
    }.toString()
}
