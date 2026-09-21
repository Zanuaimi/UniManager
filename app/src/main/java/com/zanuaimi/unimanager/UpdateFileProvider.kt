package com.zanuaimi.unimanager

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File

/** Minimal read-only provider for handing the downloaded APK to Android's package installer. */
class UpdateFileProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        require(mode == "r") { "Update APK provider is read-only" }
        val file = File(requireNotNull(context).cacheDir, "updates/${uri.lastPathSegment}").canonicalFile
        val root = File(requireNotNull(context).cacheDir, "updates").canonicalFile
        require(file.path.startsWith(root.path + File.separator) && file.isFile) { "Unknown update file" }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri): String = "application/vnd.android.package-archive"

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        val file = resolveFile(uri)
        val cursor = MatrixCursor(arrayOf("_display_name", "_size"))
        if (file.isFile) cursor.addRow(arrayOf(file.name, file.length()))
        return cursor
    }

    private fun resolveFile(uri: Uri): File {
        val appContext = requireNotNull(context)
        val root = File(appContext.cacheDir, "updates").canonicalFile
        val file = File(root, uri.lastPathSegment.orEmpty()).canonicalFile
        require(file.path.startsWith(root.path + File.separator) && file.isFile) { "Unknown update file" }
        return file
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
