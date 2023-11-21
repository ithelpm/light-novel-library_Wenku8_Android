package org.mewx.wenku8.util

import android.content.ContentValues

/**
 * Light Network
 *
 * This class achieve the basic network protocol:
 * HttpPost ...
 */
object LightNetwork {
    /**
     * Encode UTF-8 character to http postable style. For example: "妹" = "%E5%A6%B9"
     * @param str input string
     * @return result encoded string or empty string
     */
    fun encodeToHttp(str: String?): String? {
        return encodeToHttp(str, "UTF-8")
    }

    /**
     * Universal encoding to percent-encoding.
     * @param str the string to be encoded.
     * @param encoding the encoding in text form.
     * @return the percent-encoded text.
     */
    fun encodeToHttp(str: String?, encoding: String?): String? {
        val enc: String
        enc = try {
            URLEncoder.encode(str, encoding)
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
            Log.v("MewX-Net", Objects.requireNonNull(e.getMessage()))
            "" // prevent crash
        }
        return enc
    }

    /**
     * A post method, the values must to be <String></String>, String> pair.
     * @param u base url
     * @param values <String></String>, String> pair
     * @return raw bytes or null!
     */
    @Nullable
    fun LightHttpPostConnection(u: String?, values: ContentValues?): ByteArray? {
        return LightHttpPostConnection(u, values, true)
    }

    @Nullable
    fun LightHttpPostConnection(u: String?, values: ContentValues?, withSession: Boolean): ByteArray? {
        // new API, initial
        val url: URL?
        val http: HttpURLConnection
        try {
            url = URL(u)
            http = url.openConnection() as HttpURLConnection
            http.setRequestMethod("POST")
            http.setRequestProperty("Accept-Encoding", "gzip") // set gzip
            if (withSession && LightUserSession.getSession().length() !== 0) {
                http.setRequestProperty("Cookie", "PHPSESSID=" + LightUserSession.getSession())
            }
            http.setConnectTimeout(3000)
            http.setReadTimeout(5000)
            http.setDoOutput(true) // has input name value pair
            http.setInstanceFollowRedirects(true) // enable redirects
        } catch (e: Exception) {
            e.printStackTrace()
            return null // null means failure
        }

        // make request args
        val params = StringBuilder()
        for (key in values.keySet()) {
            if (values.get(key) !is String) continue
            params.append("&").append(key).append("=").append(values.get(key)) // now, like "&a=1&b=1&c=1"
        }

        // request
        val bytes: ByteArray = params.toString().getBytes()
        return try {
            http.getOutputStream().write(bytes) // set args
            var inStream: InputStream? = http.getInputStream() // input stream
            val outStream = ByteArrayOutputStream() // output stream
            if (http.getContentEncoding() != null && http.getContentEncoding().toLowerCase().contains("gzip")) {
                // using 'gzip'
                inStream = GZIPInputStream(BufferedInputStream(inStream))
            }

            // get session, save it all the time, prevent getting new session id
            if (http.getHeaderField("Set-Cookie") != null && http.getHeaderField("Set-Cookie").contains("PHPSESSID")) {
                val index: Int = http.getHeaderField("Set-Cookie").indexOf("PHPSESSID")
                LightUserSession.setSession(
                        http.getHeaderField("Set-Cookie").substring(index + 9 + 1, http.getHeaderField("Set-Cookie").indexOf(";", index))
                )
            }
            val buffer = ByteArray(1024)
            var len: Int
            while (inStream.read(buffer).also { len = it } != -1) outStream.write(buffer, 0, len) // read to outStream
            val data: ByteArray = outStream.toByteArray() // copy to ByteArray
            outStream.close()
            inStream.close()
            data // return value
        } catch (e: IOException) {
            e.printStackTrace()
            null // null means failure
        }
    }

    /**
     * LightHttpDownload:
     *
     * Give direct url to download file in one time, so this only fits small
     * size files.
     *
     * @param url
     * : direct file url with extension
     * @return return correct bytes or null
     */
    @Nullable
    fun LightHttpDownload(url: String?): ByteArray? {
        val inputStream: InputStream
        return try {
            val localURL = URL(url)
            val httpURLConnection: HttpURLConnection = localURL.openConnection() as HttpURLConnection
            httpURLConnection.setConnectTimeout(3000)
            httpURLConnection.setReadTimeout(8000)
            if (httpURLConnection.getResponseCode() !== HttpURLConnection.HTTP_OK) throw Exception("HTTP Request is not success, Response code is " + httpURLConnection.getResponseCode())
            inputStream = httpURLConnection.getInputStream()
            val b = ByteArray(1024)
            val byteArrayOutputStream = ByteArrayOutputStream()
            var len: Int
            while (inputStream.read(b).also { len = it } != -1) byteArrayOutputStream.write(b, 0, len)
            byteArrayOutputStream.close()
            inputStream.close()
            byteArrayOutputStream.close()
            byteArrayOutputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    } //	/**
    //	 * Download to file, part by part, prevent OOM error.
    //	 * @param url
    //	 * @param filepath
    //	 * @return
    //	 */
    //	public static boolean LightHttpDownloadToFile(String url, String filepath) {
    //
    //		// a replacer
    ////		url = url.replace(fromEle, toEle);
    //
    //		return false;
    //	}
}