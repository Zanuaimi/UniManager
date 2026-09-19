package com.zanuaimi.unimanager.bridge

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Parcel
import android.os.IBinder
import com.zanuaimi.unimanager.data.AppRegistry

class BridgeService : Service() {
    private val registry by lazy { AppRegistry(this) }

    private val binder = object : Binder() {
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (reply == null) return false
            enforceCallingPermission(
                "com.zanuaimi.unimanager.permission.BRIDGE",
                "UniManager bridge permission required",
            )
            val protocol = data.readInt()
            if (protocol != 1) {
                reply.writeNoException()
                reply.writeString("{\"status\":\"unsupported_protocol\",\"protocol_version\":1}")
                return true
            }
            val payload = data.readString().orEmpty()
            val response = when (code) {
                1 -> registry.register(payload)
                2 -> registry.read(payload)
                3 -> registry.update(payload)
                else -> "{\"status\":\"unsupported_operation\"}"
            }
            reply.writeNoException()
            reply.writeString(response)
            return true
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder
}
