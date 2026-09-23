package com.zanuaimi.unimanager.bridge

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Parcel
import android.os.IBinder
import android.util.Log
import org.json.JSONObject
import com.zanuaimi.unimanager.BuildConfig
import com.zanuaimi.unimanager.data.AppRegistry
import java.nio.charset.StandardCharsets

class BridgeService : Service() {
    companion object {
        private const val TAG = "UniManagerBridge"
        private const val MAX_PAYLOAD_LENGTH = 512 * 1024
    }

    private val registry by lazy { AppRegistry(this) }

    private val binder = object : Binder() {
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (reply == null) return false
            val protocol = data.readInt()
            if (protocol != BridgeProtocol.LEGACY_VERSION && protocol != BridgeProtocol.CURRENT_VERSION) {
                reply.writeNoException()
                reply.writeString(BridgeProtocol.response(
                    BridgeProtocol.UNSUPPORTED,
                    "unknown",
                    reason = "Unsupported bridge protocol v$protocol",
                ))
                return true
            }
            val payload = data.readString().orEmpty()
            val packageName = packageName(payload)
            Log.d(TAG, "request protocol=$protocol operation=$code package=${packageName.ifBlank { "<unknown>" }}")
            val response = if (payload.toByteArray(StandardCharsets.UTF_8).size > MAX_PAYLOAD_LENGTH) {
                if (protocol == BridgeProtocol.CURRENT_VERSION) {
                    BridgeProtocol.response(BridgeProtocol.PAYLOAD_TOO_LARGE, operationName(code), packageName, "Request exceeds the bridge payload limit.")
                } else "{\"status\":\"payload_too_large\"}"
            } else if (!isValidPayload(payload)) {
                Log.w(TAG, "invalid bridge payload for operation=$code")
                if (protocol == BridgeProtocol.CURRENT_VERSION) {
                    BridgeProtocol.response(BridgeProtocol.ERROR, operationName(code), reason = "The bridge payload is not valid JSON or has no package name.")
                } else "{\"status\":\"invalid_request\"}"
            } else if (!isCallerAuthorized(payload)) {
                Log.w(TAG, "unauthorized caller for package=${packageName.ifBlank { "<unknown>" }}")
                if (protocol == BridgeProtocol.CURRENT_VERSION) {
                    BridgeProtocol.response(BridgeProtocol.UNAUTHORIZED, operationName(code), packageName, "The calling UID does not own the requested package.")
                } else "{\"status\":\"unauthorized_caller\"}"
            } else {
                runCatching {
                    if (protocol == BridgeProtocol.CURRENT_VERSION) {
                        v2Response(code, payload, packageName)
                    } else {
                        when (code) {
                            BridgeProtocol.REGISTER -> registry.register(payload)
                            BridgeProtocol.READ -> registry.read(payload)
                            BridgeProtocol.UPDATE -> registry.update(payload)
                            else -> "{\"status\":\"unsupported_operation\"}"
                        }
                    }
                }.getOrElse { error ->
                    Log.e(TAG, "bridge operation failed code=$code package=$packageName", error)
                    if (protocol == BridgeProtocol.CURRENT_VERSION) {
                        BridgeProtocol.response(BridgeProtocol.ERROR, operationName(code), packageName, error.message ?: "Bridge operation failed.")
                    } else "{\"status\":\"invalid_request\"}"
                }
            }
            reply.writeNoException()
            reply.writeString(
                if (response.toByteArray(StandardCharsets.UTF_8).size > MAX_PAYLOAD_LENGTH) {
                    "{\"status\":\"response_too_large\"}"
                } else response,
            )
            return true
        }
    }

    private fun v2Response(code: Int, payload: String, packageName: String): String {
        return when (code) {
            BridgeProtocol.PING -> BridgeProtocol.response(
                BridgeProtocol.OK,
                "ping",
                packageName,
                managerVersion = BuildConfig.VERSION_NAME,
            )
            BridgeProtocol.READ -> {
                val app = registry.get(packageName)
                when {
                    app == null -> BridgeProtocol.response(
                        BridgeProtocol.NOT_REGISTERED,
                        "read",
                        packageName,
                        "No UniManager registry entry exists for this package.",
                    )
                    registry.status(app).kind != AppRegistry.StatusKind.READY -> BridgeProtocol.response(
                        BridgeProtocol.UNSUPPORTED,
                        "read",
                        packageName,
                        registry.status(app).detail,
                        fingerprint = app.optString("manager_metadata_fingerprint"),
                        capabilities = app.optJSONArray("capabilities"),
                    )
                    else -> BridgeProtocol.response(
                        BridgeProtocol.OK,
                        "read",
                        packageName,
                        configuration = app.optJSONObject("configuration") ?: JSONObject(),
                        fingerprint = app.optString("manager_metadata_fingerprint"),
                        capabilities = app.optJSONArray("capabilities"),
                    )
                }
            }
            BridgeProtocol.REGISTER -> {
                val result = registry.register(payload)
                val app = registry.get(packageName)
                val failure = registryFailure(result)
                if (failure != null) BridgeProtocol.response(
                    BridgeProtocol.ERROR,
                    "register",
                    packageName,
                    failure,
                )
                else if (app == null) BridgeProtocol.response(BridgeProtocol.ERROR, "register", packageName, "Registration did not produce a stored entry.")
                else BridgeProtocol.response(
                    BridgeProtocol.OK,
                    "register",
                    packageName,
                    configuration = app.optJSONObject("configuration") ?: JSONObject(),
                    fingerprint = app.optString("manager_metadata_fingerprint"),
                    capabilities = app.optJSONArray("capabilities"),
                )
            }
            BridgeProtocol.UPDATE -> {
                val result = registry.update(payload)
                val app = registry.get(packageName)
                val failure = registryFailure(result)
                if (failure != null) BridgeProtocol.response(
                    BridgeProtocol.ERROR,
                    "update",
                    packageName,
                    failure,
                )
                else if (app == null || result == "{}") BridgeProtocol.response(BridgeProtocol.ERROR, "update", packageName, "The registry rejected the update.")
                else BridgeProtocol.response(
                    BridgeProtocol.OK,
                    "update",
                    packageName,
                    configuration = app.optJSONObject("configuration") ?: JSONObject(),
                    fingerprint = app.optString("manager_metadata_fingerprint"),
                    capabilities = app.optJSONArray("capabilities"),
                )
            }
            else -> BridgeProtocol.response(BridgeProtocol.UNSUPPORTED, operationName(code), packageName, "Unsupported bridge operation.")
        }
    }

    private fun operationName(code: Int): String = when (code) {
        BridgeProtocol.REGISTER -> "register"
        BridgeProtocol.READ -> "read"
        BridgeProtocol.UPDATE -> "update"
        BridgeProtocol.PING -> "ping"
        else -> "unknown"
    }

    private fun packageName(payload: String): String = runCatching {
        JSONObject(payload).optString("package_name").trim()
    }.getOrDefault("")

    private fun isValidPayload(payload: String): Boolean = runCatching {
        JSONObject(payload).optString("package_name").trim().isNotBlank()
    }.getOrDefault(false)

    private fun registryFailure(result: String): String? = runCatching {
        JSONObject(result).optString("status").takeIf(String::isNotBlank)
    }.getOrNull()

    private fun isCallerAuthorized(payload: String): Boolean {
        // The service is exported so patched APKs remain compatible even when Android
        // installed them before UniManager and did not grant the optional custom
        // permission. Package ownership is the authoritative check: a caller may only
        // read or update the registry entry for one of its own packages.
        val packageName = runCatching { JSONObject(payload).optString("package_name").trim() }
            .getOrDefault("")
        if (packageName.isBlank()) return false
        return packageManager.getPackagesForUid(Binder.getCallingUid())
            ?.contains(packageName) == true
    }

    override fun onBind(intent: Intent?): IBinder = binder
}
