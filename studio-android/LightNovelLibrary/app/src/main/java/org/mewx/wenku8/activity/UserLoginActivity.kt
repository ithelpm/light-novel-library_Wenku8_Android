package org.mewx.wenku8.activity

import android.content.Intent

/**
 * Created by MewX on 2015/6/12.
 * User Login Activity.
 */
class UserLoginActivity : BaseMaterialActivity() {
    // private vars
    private var etUserNameOrEmail: EditText? = null
    private var etPassword: EditText? = null
    @Override
    protected fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initMaterialStyle(R.layout.layout_user_login)

        // Init Firebase Analytics on GA4.
        FirebaseAnalytics.getInstance(this)

        // get views
        etUserNameOrEmail = findViewById(R.id.edit_username_or_email)
        etPassword = findViewById(R.id.edit_password)
        val tvLogin: TextView = findViewById(R.id.btn_login)
        val tvRegister: TextView = findViewById(R.id.btn_register)

        // listeners
        tvLogin.setOnClickListener { v ->
            if (etUserNameOrEmail.getText().toString().length() === 0 || etUserNameOrEmail.getText().toString().length() > 30 || etPassword.getText().toString().length() === 0 || etPassword.getText().toString().length() > 30) {
                Toast.makeText(this@UserLoginActivity, getResources().getString(R.string.system_info_fill_not_complete), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // async login
            val alt: AsyncLoginTask = AsyncLoginTask()
            alt.execute(etUserNameOrEmail.getText().toString(), etPassword.getText().toString())
        }
        tvRegister.setOnClickListener { v ->
            Builder(this@UserLoginActivity)
                    .onPositive { dialog, which ->
                        // show browser list
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.setData(Uri.parse(Wenku8API.REGISTER_URL))
                        val title: String = getResources().getString(R.string.system_choose_browser)
                        val chooser: Intent = Intent.createChooser(intent, title)
                        startActivity(chooser)
                    }
                    .theme(Theme.LIGHT)
                    .backgroundColorRes(R.color.dlgBackgroundColor)
                    .contentColorRes(R.color.dlgContentColor)
                    .positiveColorRes(R.color.dlgPositiveButtonColor)
                    .negativeColorRes(R.color.dlgNegativeButtonColor)
                    .content(R.string.dialog_content_verify_register)
                    .contentGravity(GravityEnum.CENTER)
                    .positiveText(R.string.dialog_positive_ok)
                    .negativeText(R.string.dialog_negative_pass)
                    .show()
        }
    }

    private inner class AsyncLoginTask : AsyncTask<String?, Integer?, Wenku8Error.ErrorCode?>() {
        private var md: MaterialDialog? = null
        private var we: Wenku8Error.ErrorCode? = Wenku8Error.ErrorCode.ERROR_DEFAULT
        @Override
        protected fun onPreExecute() {
            super.onPreExecute()
            md = Builder(this@UserLoginActivity)
                    .theme(Theme.LIGHT)
                    .content(R.string.system_logging_in)
                    .progress(true, 0)
                    .show()
        }

        @Override
        protected fun doInBackground(params: Array<String?>?): Wenku8Error.ErrorCode? {
            // sleep to show dialog
            try {
                Thread.sleep(500)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            we = LightUserSession.doLoginFromGiven(params.get(0), params.get(1))
            if (we === Wenku8Error.ErrorCode.SYSTEM_1_SUCCEEDED) {
                // fetch avatar
                val b: ByteArray = LightNetwork.LightHttpPostConnection(Wenku8API.BASE_URL, Wenku8API.getUserAvatar())
                if (b == null) {
                    val bmp: Bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_noavatar)
                    val baos = ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                    if (!LightCache.saveFile(GlobalConfig.getFirstUserAvatarSaveFilePath(), baos.toByteArray(), true)) LightCache.saveFile(GlobalConfig.getSecondUserAvatarSaveFilePath(), baos.toByteArray(), true)
                } else {
                    if (!LightCache.saveFile(GlobalConfig.getFirstUserAvatarSaveFilePath(), b, true)) LightCache.saveFile(GlobalConfig.getSecondUserAvatarSaveFilePath(), b, true)
                }
            }
            return we
        }

        @Override
        protected fun onPostExecute(i: Wenku8Error.ErrorCode?) {
            super.onPostExecute(i)
            md.dismiss()
            when (i) {
                SYSTEM_1_SUCCEEDED -> {
                    Toast.makeText(MyApp.getContext(), getResources().getString(R.string.system_logged), Toast.LENGTH_SHORT).show()
                    this@UserLoginActivity.finish()
                }

                SYSTEM_2_ERROR_USERNAME -> Toast.makeText(MyApp.getContext(), getResources().getString(R.string.system_username_error), Toast.LENGTH_SHORT).show()
                SYSTEM_3_ERROR_PASSWORD -> Toast.makeText(MyApp.getContext(), getResources().getString(R.string.system_password_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Override
    fun onOptionsItemSelected(menuItem: MenuItem?): Boolean {
        if (menuItem.getItemId() === android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(menuItem)
    }
}