package xyz.hisname.fireflyiii.util

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.net.toFile
import androidx.core.net.toUri
import okhttp3.ResponseBody
import java.io.*
import java.lang.Exception


// https://gist.github.com/micer/ae5de2984dbbdb386dd262782cfdb39c
class FileUtils {

    companion object {

        private val folderDirectory by lazy { File(Environment.getExternalStorageDirectory(), "FireflyIIIMobile") }

        fun getPathFromUri(context: Context, uri: Uri?): String? {
            val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

            // DocumentProvider
            if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val type = split[0]

                    if ("primary".equals(type, ignoreCase = true)) {
                        val path = StringBuilder()
                        path.append(Environment.getExternalStorageDirectory())
                        path.append("/")
                        path.append(split[1])
                        return path.toString()
                    }
                } else if (isDownloadsDocument(uri)) {
                    val id = DocumentsContract.getDocumentId(uri)
                    if (id.isNotEmpty()) {
                        if (id.startsWith("raw:")) {
                            return id.replaceFirst("raw:", "");
                        }
                        return try {
                            val contentUri = ContentUris.withAppendedId(
                                    Uri.parse("content://downloads/public_downloads"), id.toLong())
                            getDataColumn(context, contentUri, null, null)
                        } catch (e: NumberFormatException) {
                            null
                        }
                    }
                } else if (isMediaDocument(uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val type = split[0]

                    var contentUri: Uri? = null
                    if ("image" == type) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    } else if ("video" == type) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    } else if ("audio" == type) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    }

                    val selection = "_id=?"
                    val selectionArgs = arrayOf(split[1])

                    return getDataColumn(context, contentUri, selection, selectionArgs)
                }// MediaProvider
                // DownloadsProvider
            } else if ("content".equals(uri?.scheme, ignoreCase = true)) {

                // Return the remote address
                return if (isGooglePhotosUri(uri)) uri?.lastPathSegment else getDataColumn(context, uri, null, null)

            } else if ("file".equals(uri?.scheme, ignoreCase = true)) {
                return uri?.path
            }// File
            // MediaStore (and general)

            return null
        }

        fun getFileName(context: Context, uri: Uri): String?{
            var result: String? = null
            if (uri.scheme == "content") {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor.use { curse ->
                    if (curse != null && curse.moveToFirst()) {
                        result = curse.getString(curse.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                    }
                }
            }
            if (result == null) {
                result = uri.path
                val cut = result?.lastIndexOf('/')
                if (cut != -1) {
                    result = result?.substring(cut?.plus(1) ?: 0)
                }
            }
            return result
        }

        fun getMimeType(context: Context, uri: Uri): String{
            val mimeType: String?
            mimeType = if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                context.contentResolver.getType(uri)
            } else {
                val fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri
                        .toString())
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        fileExtension.toLowerCase())
            }
            return mimeType
        }

        fun writeResponseToDisk(body: ResponseBody, fileName: String): Boolean {
            if(!createDirIfNotExists()){
                return false
            }
            try {
                var inputStream: InputStream? = null
                var outputStream: OutputStream? = null

                try {
                    val fileReader = ByteArray(4096)
                    var fileSizeDownloaded: Long = 0
                    inputStream = body.byteStream()
                    outputStream = FileOutputStream("$folderDirectory/$fileName")
                    while (true) {
                        val read = inputStream.read(fileReader)
                        if (read == -1) {
                            break
                        }
                        outputStream.write(fileReader, 0, read)
                        fileSizeDownloaded += read.toLong()
                    }

                    outputStream.flush()
                    return true
                } catch (e: IOException) {
                    return false
                } finally {
                    inputStream?.close()
                    outputStream?.close()
                }
            } catch (e: IOException) {
                return false
            }
        }

        private fun createDirIfNotExists(): Boolean{
            var ret = true
            if(!folderDirectory.exists()){
                if(!folderDirectory.mkdir()){
                    ret = false
                }
            }
            return ret
        }

        fun openFile(context: Context, fileName: String): Boolean{
            val fileToOpen = File("$folderDirectory/$fileName")
            return if(fileToOpen.exists() && !fileToOpen.isDirectory){
                val fileIntent = Intent(Intent.ACTION_VIEW)
                fileIntent.setDataAndType(Uri.parse("$folderDirectory/$fileName"), getMimeType(context, "$folderDirectory/$fileName".toUri()))
                val openFileIntent = Intent.createChooser(fileIntent, "Open File")
                context.startActivity(openFileIntent)
                true
            } else {
                false
            }
        }

        private fun isMediaDocument(uri: Uri?): Boolean {
            return "com.android.providers.media.documents" == uri?.authority
        }

        private fun isExternalStorageDocument(uri: Uri?): Boolean {
            return "com.android.externalstorage.documents" == uri?.authority
        }

        private fun isDownloadsDocument(uri: Uri?): Boolean {
            return "com.android.providers.downloads.documents" == uri?.authority
        }

        private fun getDataColumn(context: Context, uri: Uri?, selection: String?,
                                  selectionArgs: Array<String>?): String? {
            var cursor: Cursor? = null
            val column = "_data"
            val projection = arrayOf(column)
            try {
                cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndexOrThrow(column)
                    return cursor.getString(index)
                }
            } finally {
                cursor?.close()
            }
            return null
        }

        private fun isGooglePhotosUri(uri: Uri?): Boolean {
            return "com.google.android.apps.photos.content" == uri?.authority
        }
    }
}