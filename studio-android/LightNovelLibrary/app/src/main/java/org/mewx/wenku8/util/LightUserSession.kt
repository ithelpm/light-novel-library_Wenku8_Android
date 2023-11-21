package org.mewx.wenku8.util

import android.content.Intent

/**
 * Created by MewX on 2015/5/17.
 * This file is secret. Not open-source.
 */
object LightUserSession {
    // open part
    var aiui: AsyncInitUserInfo? = null // not exec

    // Secret part
    private var logStatus = false // true - logged in; false - not logged in.
    private var usernameOrEmail: String? = null
    private var password: String? = null
    private var SESSION: String? = null

    // no null returned
    fun getLoggedAs(): String? {
        return if (logStatus && SESSION != null && SESSION.length() !== 0 && isUserInfoSet()) usernameOrEmail else ""
    }

    fun getUsernameOrEmail(): String? {
        return if (usernameOrEmail == null) "" else usernameOrEmail
    }

    fun getPassword(): String? {
        return if (password == null) "" else password
    }

    // no null returned, default is ""
    fun getSession(): String? {
        if (SESSION != null) {
            Log.d(LightUserSession::class.java.getSimpleName(), SESSION)
        }
        return if (SESSION == null) "" else SESSION
    }

    fun setSession(s: String?) {
        if (s != null && s.length() !== 0) SESSION = s
    }

    fun getLogStatus(): Boolean {
        return logStatus
    }

    fun loadUserInfoSet(): Boolean {
        val bytes: ByteArray
        bytes = if (LightCache.testFileExist(GlobalConfig.getFirstFullUserAccountSaveFilePath())) {
            LightCache.loadFile(GlobalConfig.getFirstFullUserAccountSaveFilePath())
        } else if (LightCache.testFileExist(GlobalConfig.getSecondFullUserAccountSaveFilePath())) {
            LightCache.loadFile(GlobalConfig.getSecondFullUserAccountSaveFilePath())
        } else {
            return false // file read failed
        }
        try {
            Log.d("MewX", String(bytes, "UTF-8"))
            decAndSetUserFile(String(bytes, "UTF-8"))
        } catch (e: Exception) {
            e.printStackTrace()
            return false // exception
        }
        return true
    }

    fun saveUserInfoSet(): Boolean {
        LightCache.saveFile(GlobalConfig.getFirstFullUserAccountSaveFilePath(), encUserFile().getBytes(), true)
        if (!LightCache.testFileExist(GlobalConfig.getFirstFullUserAccountSaveFilePath())) {
            LightCache.saveFile(GlobalConfig.getSecondFullUserAccountSaveFilePath(), encUserFile().getBytes(), true)
            return LightCache.testFileExist(GlobalConfig.getSecondFullUserAccountSaveFilePath())
        }
        return true
    }

    /**
     * Send the login call to server.
     * Must be called in async thread.
     * @param usernameOrEmail the username or email string.
     * @param password the password string.
     * @return the returned bytes from server.
     */
    private fun executeLoginRequest(usernameOrEmail: String?, password: String?): ByteArray? {
        if (usernameOrEmail == null || password == null) {
            return null
        }
        return if (usernameOrEmail.contains("@")) {
            // Assuming it's email.
            LightNetwork.LightHttpPostConnection(Wenku8API.BASE_URL, Wenku8API.getUserLoginEmailParams(usernameOrEmail, password))
        } else {
            // Assuming it's username.
            LightNetwork.LightHttpPostConnection(Wenku8API.BASE_URL, Wenku8API.getUserLoginParams(usernameOrEmail, password))
        }
    }

    // async action
    fun doLoginFromFile(): Wenku8Error.ErrorCode? {
        // This function will read from file, if failed return false
        if (!isUserInfoSet()) loadUserInfoSet()
        if (!isUserInfoSet()) return Wenku8Error.ErrorCode.USER_INFO_EMPTY
        val b = executeLoginRequest(usernameOrEmail, password)
                ?: return Wenku8Error.ErrorCode.NETWORK_ERROR
        return try {
            val result = String(b, "UTF-8")
            if (!LightTool.isInteger(result)) {
                return Wenku8Error.ErrorCode.RETURNED_VALUE_EXCEPTION
            }
            if (Wenku8Error.getSystemDefinedErrorCode(Integer.valueOf(result)) === Wenku8Error.ErrorCode.SYSTEM_1_SUCCEEDED) logStatus = true
            Wenku8Error.getSystemDefinedErrorCode(Integer.valueOf(result)) // get excepted returned value
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
            Wenku8Error.ErrorCode.BYTE_TO_STRING_EXCEPTION
        }
    }

    // async action
    fun doLoginFromGiven(name: String?, pwd: String?): Wenku8Error.ErrorCode? {
        // This function will test given name:pwd, if pass(receive '1'), save file, else return false
        val b = executeLoginRequest(name, pwd)
                ?: return Wenku8Error.ErrorCode.NETWORK_ERROR
        return try {
            val result = String(b, "UTF-8")
            if (!LightTool.isInteger(result)) {
                return Wenku8Error.ErrorCode.RETURNED_VALUE_EXCEPTION
            }
            if (Wenku8Error.getSystemDefinedErrorCode(Integer.valueOf(result)) === Wenku8Error.ErrorCode.SYSTEM_1_SUCCEEDED) {
                logStatus = true

                // save user info
                setUserInfo(name, pwd)
                saveUserInfoSet()

                // TODO: activate session keeper
            }
            Wenku8Error.getSystemDefinedErrorCode(Integer.valueOf(result)) // get excepted returned value
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
            Wenku8Error.ErrorCode.BYTE_TO_STRING_EXCEPTION
        }
    }

    fun logOut() {
        logStatus = false
        setUserInfo("", "")

        // delete files
        if (!LightCache.deleteFile(GlobalConfig.getFirstFullUserAccountSaveFilePath())) {
            LightCache.deleteFile(GlobalConfig.getSecondFullUserAccountSaveFilePath())
        }
        if (!LightCache.deleteFile(GlobalConfig.getFirstUserAvatarSaveFilePath())) {
            LightCache.deleteFile(GlobalConfig.getSecondUserAvatarSaveFilePath())
        }
    }

    // async action
    fun heartbeatLogin(): Wenku8Error.ErrorCode? {
        // call from HeartbeatSessionKeeper, send login operation
        Toast.makeText(MyApp.getContext(), "Heartbeat test", Toast.LENGTH_SHORT).show()
        return doLoginFromFile()
    }

    /**
     * This function will only judge whether the username and the password var is set.
     * Not judging what letters are they contains.
     * @return true is okay.
     */
    fun isUserInfoSet(): Boolean {
        return usernameOrEmail != null && password != null && usernameOrEmail.length() !== 0 && password.length() !== 0
    }

    fun setUserInfo(username: String?, password: String?) {
        usernameOrEmail = username
        LightUserSession.password = password
    }

    /**
     * Decrypt user file raw content, which is 0-F byte values, encoded in UTF-8.
     * @param raw UTF-8 Charset raw file content.
     */
    fun decAndSetUserFile(raw: String?) {
        try {
            val a: Array<String?> = raw.split("\\|") // a[0]: username; a[1]: password;
            if (a.size != 2 || a[0].length() === 0 || a[1].length() === 0) {
                setUserInfo("", "")
                return  // fetch error to return
            }

            // dec once
            var temp_username: CharArray = LightBase64.DecodeBase64String(a[0]).toCharArray()
            var temp_password: CharArray = LightBase64.DecodeBase64String(a[1]).toCharArray()

            // reverse main part
            var equal_pos: Int
            var result = String(temp_username)
            equal_pos = result.indexOf('=')
            run {
                var i = 0
                var j = if (equal_pos == -1) temp_username.size - 1 else equal_pos - 1
                while (i < j) {
                    val temp = temp_username[i]
                    temp_username[i] = temp_username[j]
                    temp_username[j] = temp
                    i++
                    j--
                }
            }
            result = String(temp_password)
            equal_pos = result.indexOf('=')
            run {
                var i = 0
                var j = if (equal_pos == -1) temp_password.size - 1 else equal_pos - 1
                while (i < j) {
                    val temp = temp_password[i]
                    temp_password[i] = temp_password[j]
                    temp_password[j] = temp
                    i++
                    j--
                }
            }

            // dec twice
            temp_username = LightBase64.DecodeBase64String(String(temp_username)).toCharArray()
            temp_password = LightBase64.DecodeBase64String(String(temp_password)).toCharArray()

            // exchange caps and uncaps
            for (i in temp_username.indices) {
                if ('a' <= temp_username[i] && temp_username[i] <= 'z') temp_username[i] -= ('a'.code - 'A'.code).toChar() else if ('A' <= temp_username[i] && temp_username[i] <= 'Z') temp_username[i] += ('a'.code - 'A'.code).toChar()
            }
            for (i in temp_password.indices) {
                if ('a' <= temp_password[i] && temp_password[i] <= 'z') temp_password[i] -= ('a'.code - 'A'.code).toChar() else if ('A' <= temp_password[i] && temp_password[i] <= 'Z') temp_password[i] += ('a'.code - 'A'.code).toChar()
            }

            // dec three times
            setUserInfo(LightBase64.DecodeBase64String(String(temp_username)),
                    LightBase64.DecodeBase64String(String(temp_password)))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Encrypt user file to raw content.
     * @return raw file content, fail to return "". (no null returned)
     */
    fun encUserFile(): String? {
        // judge available
        if (!isUserInfoSet()) return "" // empty, not null

        // username, password enc to base64
        var temp_username: CharArray = LightBase64.EncodeBase64(usernameOrEmail).toCharArray()
        var temp_password: CharArray = LightBase64.EncodeBase64(password).toCharArray()

        // cap to uncap, uncap to cap
        for (i in temp_username.indices) {
            if ('a' <= temp_username[i] && temp_username[i] <= 'z') temp_username[i] -= ('a'.code - 'A'.code).toChar() else if ('A' <= temp_username[i] && temp_username[i] <= 'Z') temp_username[i] += ('a'.code - 'A'.code).toChar()
        }
        for (i in temp_password.indices) {
            if ('a' <= temp_password[i] && temp_password[i] <= 'z') temp_password[i] -= ('a'.code - 'A'.code).toChar() else if ('A' <= temp_password[i] && temp_password[i] <= 'Z') temp_password[i] += ('a'.code - 'A'.code).toChar()
        }

        // twice base64, exchange char position, beg to end, end to beg
        var equal_pos: Int
        temp_username = LightBase64.EncodeBase64(String(temp_username)).toCharArray()
        var result = String(temp_username)
        equal_pos = result.indexOf('=')
        run {
            var i = 0
            var j = if (equal_pos == -1) temp_username.size - 1 else equal_pos - 1
            while (i < j) {
                val temp = temp_username[i]
                temp_username[i] = temp_username[j]
                temp_username[j] = temp
                i++
                j--
            }
        }
        temp_password = LightBase64.EncodeBase64(String(temp_password)).toCharArray()
        result = String(temp_password)
        equal_pos = result.indexOf('=')
        var i = 0
        var j = if (equal_pos == -1) temp_password.size - 1 else equal_pos - 1
        while (i < j) {
            val temp = temp_password[i]
            temp_password[i] = temp_password[j]
            temp_password[j] = temp
            i++
            j--
        }

        // three times base64
        result = LightBase64.EncodeBase64(String(temp_username)) + "|" + LightBase64.EncodeBase64(String(temp_password))

        // return value
        return result
    }

    class AsyncInitUserInfo : AsyncTask<Integer?, Integer?, Wenku8Error.ErrorCode?>() {
        @Override
        protected fun doInBackground(vararg params: Integer?): Wenku8Error.ErrorCode? {
            loadUserInfoSet()
            return doLoginFromFile()
        }

        @Override
        protected fun onPostExecute(e: Wenku8Error.ErrorCode?) {
            super.onPostExecute(e)

            // if error code == UN error or PWD error, clear cert & avatar
            if (e === Wenku8Error.ErrorCode.SYSTEM_2_ERROR_USERNAME || e === Wenku8Error.ErrorCode.SYSTEM_3_ERROR_PASSWORD) {
                if (!LightCache.deleteFile(GlobalConfig.getFirstFullUserAccountSaveFilePath())) LightCache.deleteFile(GlobalConfig.getSecondFullUserAccountSaveFilePath())
                if (!LightCache.deleteFile(GlobalConfig.getFirstUserAvatarSaveFilePath())) LightCache.deleteFile(GlobalConfig.getSecondUserAvatarSaveFilePath())
                usernameOrEmail = ""
                password = ""
                Toast.makeText(MyApp.getContext(), MyApp.getContext().getResources().getString(R.string.system_log_info_outofdate), Toast.LENGTH_SHORT).show()
                return
            }
            if (logStatus) {
                // heart beat service
                val intent = Intent(MyApp.getContext(), HeartbeatSessionKeeper::class.java)
                MyApp.getContext().startService(intent)
            }
        }
    }
}