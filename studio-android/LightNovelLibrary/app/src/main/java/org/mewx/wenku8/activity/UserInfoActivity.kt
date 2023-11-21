package org.mewx.wenku8.activity

import android.graphics.Bitmap

/**
 * Created by MewX on 2015/6/14.
 * User Info Activity.
 */
class UserInfoActivity : BaseMaterialActivity() {
    // private vars
    private var mFirebaseAnalytics: FirebaseAnalytics? = null
    private var rivAvatar: RoundedImageView? = null
    private var tvUserName: TextView? = null
    private var tvNickyName: TextView? = null
    private var tvScore: TextView? = null
    private var tvExperience: TextView? = null
    private var tvRank: TextView? = null
    private var tvLogout: TextView? = null
    private var ui: UserInfo? = null
    private var agui: AsyncGetUserInfo? = null
    @Override
    protected fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initMaterialStyle(R.layout.layout_account_info)

        // Init Firebase Analytics on GA4.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        // get views
        rivAvatar = findViewById(R.id.user_avatar)
        tvUserName = findViewById(R.id.username)
        tvNickyName = findViewById(R.id.nickname)
        tvScore = findViewById(R.id.score)
        tvExperience = findViewById(R.id.experience)
        tvRank = findViewById(R.id.rank)
        tvLogout = findViewById(R.id.btn_logout)

        // sync get info
        agui = AsyncGetUserInfo()
        agui.execute()
    }

    private inner class AsyncGetUserInfo : AsyncTask<Integer?, Integer?, Wenku8Error.ErrorCode?>() {
        private var operation = 0 // 0 is fetch data, 1 is sign
        var md: MaterialDialog? = null
        @Override
        protected fun onPreExecute() {
            super.onPreExecute()
            operation = 0 // init
            md = Builder(this@UserInfoActivity)
                    .theme(Theme.LIGHT)
                    .content(R.string.system_fetching)
                    .progress(true, 0)
                    .cancelable(false)
                    .show()
        }

        @Override
        protected fun doInBackground(vararg params: Integer?): Wenku8Error.ErrorCode? {
            if (params.size == 1 && params[0] == 1) {
                // do sign, then fetch all data
                operation = 1
                val b: ByteArray = LightNetwork.LightHttpPostConnection(Wenku8API.BASE_URL, Wenku8API.getUserSignParams())
                        ?: return Wenku8Error.ErrorCode.NETWORK_ERROR
                try {
                    if (!LightTool.isInteger(String(b))) return Wenku8Error.ErrorCode.STRING_CONVERSION_ERROR else if (Wenku8Error.getSystemDefinedErrorCode(Integer.valueOf(String(b))) === Wenku8Error.ErrorCode.SYSTEM_9_SIGN_FAILED) return Wenku8Error.ErrorCode.SYSTEM_9_SIGN_FAILED
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return try {
                // try fetch
                var b: ByteArray = LightNetwork.LightHttpPostConnection(Wenku8API.BASE_URL, Wenku8API.getUserInfoParams())
                        ?: return Wenku8Error.ErrorCode.NETWORK_ERROR
                var xml = String(b, "UTF-8")
                if (LightTool.isInteger(xml)) {
                    if (Wenku8Error.getSystemDefinedErrorCode(Integer.valueOf(xml)) === Wenku8Error.ErrorCode.SYSTEM_4_NOT_LOGGED_IN) {
                        // do log in
                        val temp: Wenku8Error.ErrorCode = LightUserSession.doLoginFromFile()
                        if (temp !== Wenku8Error.ErrorCode.SYSTEM_1_SUCCEEDED) return temp // return an error code

                        // rquest again
                        b = LightNetwork.LightHttpPostConnection(Wenku8API.BASE_URL, Wenku8API.getUserInfoParams())
                        if (b == null) return Wenku8Error.ErrorCode.NETWORK_ERROR
                        xml = String(b, "UTF-8")
                    } else return Wenku8Error.getSystemDefinedErrorCode(Integer.valueOf(xml))
                }
                Log.d("MewX", xml)
                ui = UserInfo.parseUserInfo(xml)
                if (ui == null) Wenku8Error.ErrorCode.XML_PARSE_FAILED else Wenku8Error.ErrorCode.SYSTEM_1_SUCCEEDED
            } catch (e: Exception) {
                e.printStackTrace()
                Wenku8Error.ErrorCode.STRING_CONVERSION_ERROR
            }
        }

        @Override
        protected fun onPostExecute(errorCode: Wenku8Error.ErrorCode?) {
            super.onPostExecute(errorCode)
            md.dismiss()
            if (operation == 1) {
                // Analysis.
                val checkInParams = Bundle()
                checkInParams.putString("effective_click", "" + (errorCode !== Wenku8Error.ErrorCode.SYSTEM_9_SIGN_FAILED))
                mFirebaseAnalytics.logEvent("daily_check_in", checkInParams)

                // fetch from sign
                if (errorCode === Wenku8Error.ErrorCode.SYSTEM_9_SIGN_FAILED) Toast.makeText(this@UserInfoActivity, getResources().getString(R.string.userinfo_sign_failed), Toast.LENGTH_SHORT).show() else Toast.makeText(this@UserInfoActivity, getResources().getString(R.string.userinfo_sign_successful), Toast.LENGTH_SHORT).show()
                return  // just return
            }
            if (errorCode === Wenku8Error.ErrorCode.SYSTEM_1_SUCCEEDED) {
                // show avatar
                val avatarPath: String
                avatarPath = if (LightCache.testFileExist(GlobalConfig.getFirstUserAvatarSaveFilePath())) GlobalConfig.getFirstUserAvatarSaveFilePath() else GlobalConfig.getSecondUserAvatarSaveFilePath()
                val options: BitmapFactory.Options = Options()
                options.inSampleSize = 2
                val bm: Bitmap = BitmapFactory.decodeFile(avatarPath, options)
                if (bm != null) rivAvatar.setImageBitmap(bm)

                // set texts
                tvUserName.setText(ui.username)
                tvNickyName.setText(ui.nickyname)
                tvScore.setText(Integer.toString(ui.score))
                tvExperience.setText(Integer.toString(ui.experience))
                tvRank.setText(ui.rank)
                tvLogout.setOnClickListener(object : OnClickListener() {
                    @Override
                    fun onClick(v: View?) {
                        Builder(this@UserInfoActivity)
                                .callback(object : ButtonCallback() {
                                    @Override
                                    fun onPositive(dialog: MaterialDialog?) {
                                        super.onPositive(dialog)
                                        val al: AsyncLogout = AsyncLogout()
                                        al.execute()
                                    }
                                })
                                .theme(Theme.LIGHT)
                                .titleColorRes(R.color.default_text_color_black)
                                .backgroundColorRes(R.color.dlgBackgroundColor)
                                .contentColorRes(R.color.dlgContentColor)
                                .positiveColorRes(R.color.dlgPositiveButtonColor)
                                .negativeColorRes(R.color.dlgNegativeButtonColor)
                                .content(R.string.dialog_content_sure_to_logout)
                                .contentGravity(GravityEnum.CENTER)
                                .positiveText(R.string.dialog_positive_ok)
                                .negativeText(R.string.dialog_negative_biao)
                                .show()
                    }
                })
            } else {
                Toast.makeText(this@UserInfoActivity, errorCode.toString(), Toast.LENGTH_SHORT).show()
                this@UserInfoActivity.finish() // end dialog
            }
        }
    }

    private inner class AsyncLogout : AsyncTask<Integer?, Integer?, Wenku8Error.ErrorCode?>() {
        var md: MaterialDialog? = null
        @Override
        protected fun onPreExecute() {
            super.onPreExecute()
            md = Builder(this@UserInfoActivity)
                    .theme(Theme.LIGHT)
                    .content(R.string.system_fetching)
                    .progress(true, 0)
                    .cancelable(false)
                    .show()
        }

        @Override
        protected fun doInBackground(vararg params: Integer?): Wenku8Error.ErrorCode? {
            val b: ByteArray = LightNetwork.LightHttpPostConnection(Wenku8API.BASE_URL, Wenku8API.getUserLogoutParams())
                    ?: return Wenku8Error.ErrorCode.NETWORK_ERROR
            return try {
                val result = String(b, "UTF-8")
                if (!LightTool.isInteger(result)) {
                    Wenku8Error.ErrorCode.RETURNED_VALUE_EXCEPTION
                } else Wenku8Error.getSystemDefinedErrorCode(Integer(result))
                // get 1 or 4 exceptions
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
                Wenku8Error.ErrorCode.BYTE_TO_STRING_EXCEPTION
            }
        }

        @Override
        protected fun onPostExecute(errorCode: Wenku8Error.ErrorCode?) {
            super.onPostExecute(errorCode)
            if (errorCode === Wenku8Error.ErrorCode.SYSTEM_1_SUCCEEDED || errorCode === Wenku8Error.ErrorCode.SYSTEM_4_NOT_LOGGED_IN) {
                LightUserSession.logOut()
                Toast.makeText(this@UserInfoActivity, "Logged out!", Toast.LENGTH_SHORT).show()
            } else Toast.makeText(this@UserInfoActivity, errorCode.toString(), Toast.LENGTH_SHORT).show()

            // terminate this activity
            md.dismiss()
            this@UserInfoActivity.finish()
        }
    }

    @Override
    fun onCreateOptionsMenu(menu: Menu?): Boolean {
        getMenuInflater().inflate(R.menu.menu_user_info, menu)
        return true
    }

    @Override
    fun onOptionsItemSelected(menuItem: MenuItem?): Boolean {
        if (menuItem.getItemId() === android.R.id.home) {
            onBackPressed()
        } else if (menuItem.getItemId() === R.id.action_sign) {
            if (agui != null && agui.getStatus() === AsyncTask.Status.FINISHED) {
                // do sign operation
                agui = AsyncGetUserInfo()
                agui.execute(1)
            } else Toast.makeText(this, "Please wait...", Toast.LENGTH_SHORT).show()
        }
        return super.onOptionsItemSelected(menuItem)
    }
}