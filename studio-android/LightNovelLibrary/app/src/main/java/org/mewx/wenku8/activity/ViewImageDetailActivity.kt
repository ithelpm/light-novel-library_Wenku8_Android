package org.mewx.wenku8.activity

import android.app.Activity

/**
 * Created by MewX on 2015/7/28.
 * View large image activity.
 */
class ViewImageDetailActivity : BaseMaterialActivity() {
    private var path: String? = null
    private var fileName: String? = null
    private var imageView: SubsamplingScaleImageView? = null
    @Override
    protected fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initMaterialStyle(R.layout.layout_view_image_detail, StatusBarColor.DARK)

        // Init Firebase Analytics on GA4.
        FirebaseAnalytics.getInstance(this)

        // fetch value
        path = getIntent().getStringExtra("path")
        fileName = if (path.contains("/")) path.split("/").get(path.split("/").length - 1) else "default.jpg"
        Log.d(TAG, "onCreate: path = $path")
        Log.d(TAG, "onCreate: fileName = $fileName")
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(fileName)
        }

        // set image
        imageView = findViewById(R.id.image_scalable)
        imageView.setImage(ImageSource.uri(path))
        imageView.setMaxScale(4.0f)
        imageView.setOnClickListener(object : OnClickListener() {
            private var shown = true
            @Override
            fun onClick(v: View?) {
                if (shown) {
                    // hide
                    shown = false
                    hideNavigationBar()
                    findViewById(R.id.toolbar_actionbar).setVisibility(View.INVISIBLE)
                    findViewById(R.id.image_detail_bot).setVisibility(View.INVISIBLE)
                    getTintManager().setStatusBarAlpha(0.0f)
                    getTintManager().setNavigationBarAlpha(0.0f)
                } else {
                    shown = true
                    showNavigationBar()
                    findViewById(R.id.toolbar_actionbar).setVisibility(View.VISIBLE)
                    findViewById(R.id.image_detail_bot).setVisibility(View.VISIBLE)
                    getTintManager().setStatusBarAlpha(0.9f)
                    getTintManager().setNavigationBarAlpha(0.8f)
                }
            }
        })

        // set on click listeners
        findViewById(R.id.btn_rotate).setOnClickListener { v ->
            when (imageView.getOrientation()) {
                SubsamplingScaleImageView.ORIENTATION_0 -> imageView.setOrientation(SubsamplingScaleImageView.ORIENTATION_90)
                SubsamplingScaleImageView.ORIENTATION_90 -> imageView.setOrientation(SubsamplingScaleImageView.ORIENTATION_180)
                SubsamplingScaleImageView.ORIENTATION_180 -> imageView.setOrientation(SubsamplingScaleImageView.ORIENTATION_270)
                SubsamplingScaleImageView.ORIENTATION_270 -> imageView.setOrientation(SubsamplingScaleImageView.ORIENTATION_0)
            }
        }
        findViewById(R.id.btn_rotate).setOnLongClickListener { v ->
            Toast.makeText(this@ViewImageDetailActivity, getResources().getString(R.string.reader_rotate), Toast.LENGTH_SHORT).show()
            true
        }
        findViewById(R.id.btn_download).setOnClickListener { v ->
            // For API >= 29, does not show a directory picker; instead, save to DCIM/wenku8 directly.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                insertImageToDcimFolder()
                Toast.makeText(this@ViewImageDetailActivity, "已保存： DCIM/wenku8/$fileName", Toast.LENGTH_SHORT).show()
            } else {
                val i = Intent(this@ViewImageDetailActivity, FilePickerActivity::class.java)
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true)
                i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR)
                i.putExtra(FilePickerActivity.EXTRA_START_PATH,
                        if (GlobalConfig.pathPickedSave == null || GlobalConfig.pathPickedSave.length() === 0) Environment.getExternalStorageDirectory().getPath() else GlobalConfig.pathPickedSave)
                startActivityForResult(i, 0)
            }
        }
        findViewById(R.id.btn_download).setOnLongClickListener { v ->
            Toast.makeText(this@ViewImageDetailActivity, getResources().getString(R.string.reader_download), Toast.LENGTH_SHORT).show()
            true
        }
    }

    /**
     * Saves the image in this context to the DCIM/wenku8 folder.
     *
     *
     * Note that this is only tested on API 29 - 33.
     */
    private fun insertImageToDcimFolder() {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/wenku8")
        // Adds the date meta data to ensure the image is added at the front of the gallery.
        contentValues.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis())
        contentValues.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        val resolver: ContentResolver = getContentResolver()
        val imageUri: Uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        // Open an OutputStream to write data to the imageUri
        try {
            val outputStream: OutputStream = resolver.openOutputStream(imageUri)
            outputStream.write(LightCache.loadFile(path))
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
            // TODO: handle the exception better.
            Toast.makeText(this, "Failed: $e", Toast.LENGTH_SHORT).show()
        }
    }

    @Override
    protected fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Saving images to local storage.
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            if (data.getBooleanExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)) {
                val clip: ClipData = data.getClipData()
                if (clip != null) {
                    for (i in 0 until clip.getItemCount()) {
                        val uri: Uri = clip.getItemAt(i).getUri()
                        // Do something with the URI
                        runSaveProcedure(uri.toString())
                    }
                }
            } else {
                val uri: Uri = data.getData()
                // Do something with the URI
                if (uri != null) {
                    runSaveProcedure(uri.toString())
                }
            }
        }
    }

    private fun runSaveProcedure(uri: String?) {
        val newuri: String = uri.replaceAll("file://", "")
        GlobalConfig.pathPickedSave = newuri
        Builder(this)
                .theme(Theme.LIGHT)
                .title(R.string.dialog_title_save_file_name)
                .content(getResources().getString(R.string.dialog_content_saved_path) + newuri)
                .inputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
                .input("", "", false, object : InputCallback() {
                    @Override
                    fun onInput(@NonNull dialog: MaterialDialog?, input: CharSequence?) {
                        if (LightCache.testFileExist(newuri + File.separator + input + ".jpg")) {
                            // judge force write
                            Builder(this@ViewImageDetailActivity)
                                    .onPositive { unused1, unused2 ->
                                        // copy file from 'path' to 'uri + File.separator + input + ".jpg"'
                                        LightCache.copyFile(path, newuri + File.separator + input + ".jpg", true)
                                        Toast.makeText(this@ViewImageDetailActivity, "已保存：" + newuri + File.separator + input + ".jpg", Toast.LENGTH_SHORT).show()
                                    }
                                    .onNegative { unused1, unused2 -> Toast.makeText(this@ViewImageDetailActivity, "目标文件名已存在，未保存。", Toast.LENGTH_SHORT).show() }
                                    .theme(Theme.LIGHT)
                                    .titleColorRes(R.color.dlgTitleColor)
                                    .backgroundColorRes(R.color.dlgBackgroundColor)
                                    .contentColorRes(R.color.dlgContentColor)
                                    .positiveColorRes(R.color.dlgPositiveButtonColor)
                                    .negativeColorRes(R.color.dlgNegativeButtonColor)
                                    .title(R.string.dialog_title_found_file)
                                    .content(R.string.dialog_content_force_write_file)
                                    .contentGravity(GravityEnum.CENTER)
                                    .positiveText(R.string.dialog_positive_yes)
                                    .negativeText(R.string.dialog_negative_no)
                                    .show()
                        } else {
                            // copy file from 'path' to 'uri + File.separator + input + ".jpg"'
                            LightCache.copyFile(path, newuri + File.separator + input + ".jpg", true)
                            Toast.makeText(this@ViewImageDetailActivity, "已保存：" + newuri + File.separator + input + ".jpg", Toast.LENGTH_SHORT).show()
                        }
                    }
                }).show()
    }

    @Override
    protected fun onResume() {
        super.onResume()
        showNavigationBar()
    }

    private fun hideNavigationBar() {
        // This work only for android 4.4+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // set navigation bar status, remember to disable "setNavigationBarTintEnabled"
            val flags: Int = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            getWindow().getDecorView().setSystemUiVisibility(flags)

            // Code below is to handle presses of Volume up or Volume down.
            // Without this, after pressing volume buttons, the navigation bar will
            // show up and won't hide
            val decorView: View = getWindow().getDecorView()
            decorView.setOnSystemUiVisibilityChangeListener { visibility ->
                if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN === 0) {
                    decorView.setSystemUiVisibility(flags)
                }
            }
        }
    }

    private fun showNavigationBar() {
        // This work only for android 4.4+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // set navigation bar status, remember to disable "setNavigationBarTintEnabled"
            val flags: Int = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            getWindow().getDecorView().setSystemUiVisibility(flags)

            // Code below is to handle presses of Volume up or Volume down.
            // Without this, after pressing volume buttons, the navigation bar will
            // show up and won't hide
            val decorView: View = getWindow().getDecorView()
            decorView.setOnSystemUiVisibilityChangeListener { visibility ->
                if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN === 0) {
                    decorView.setSystemUiVisibility(flags)
                }
            }
        }
    }

    @Override
    fun onOptionsItemSelected(menuItem: MenuItem?): Boolean {
        when (menuItem.getItemId()) {
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(menuItem)
    }

    @Override
    fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(0, R.anim.fade_out)
    }

    companion object {
        private val TAG: String? = ViewImageDetailActivity::class.java.getSimpleName()
    }
}