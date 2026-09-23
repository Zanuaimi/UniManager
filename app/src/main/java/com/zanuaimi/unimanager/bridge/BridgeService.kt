package com.zanuaimi.unimanager.bridge

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Parcel
import android.os.IBinder
import org.json.JSONObject
import com.zanuaimi.unimanager.data.AppRegistry
import java.nio.charset.StandardCharsets

class BridgeService : Service() {
    private val registry by lazy { AppRegistry(this) }

    private val binder = object : Binder() {
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (reply == null) return false
            val protocol = data.readInt()
            if (protocol != 1) {
                reply.writeNoException()
                reply.writeString("{\"status\":\"unsupported_protocol\",\"protocol_version\":1}")
                return true
            }
            val payload = data.readString().orEmpty()
            val response = if (payload.toByteArray(StandardCharsets.UTF_8).size > MAX_PAYLOAD_LENGTH) {
                "{\"status\":\"payload_too_large\"}"
            } else if (!isCallerAuthorized(payload)) {
                "{\"status\":\"unauthorized_caller\"}"
            } else {
                runCatching {
                    when (code) {
                        1 -> registry.register(payload)
                        2 -> registry.read(payload)
                        3 -> registry.update(payload)
                        else -> "{\"status\":\"unsupported_operation\"}"
                    }
                }.getOrElse { "{\"status\":\"invalid_request\"}" }
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

    private companion object {
        // Keep requests well below Binder's transaction limit. Configuration payloads do not
        // contain the optional overlay image, so 512 KiB leaves room for Binder framing.
        const val MAX_PAYLOAD_LENGTH = 512 * 1024
    }

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
