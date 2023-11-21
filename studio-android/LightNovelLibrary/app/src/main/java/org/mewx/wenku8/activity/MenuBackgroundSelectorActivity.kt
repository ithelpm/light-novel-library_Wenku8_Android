package org.mewx.wenku8.activity

import android.app.Activity

/**
 * Created by MewX on 2015/7/29.
 * Let user select a menu background.
 */
class MenuBackgroundSelectorActivity : BaseMaterialActivity() {
    @Override
    protected fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initMaterialStyle(R.layout.layout_menu_background_selector)

        // Init Firebase Analytics on GA4.
        FirebaseAnalytics.getInstance(this)

        // Init listeners.
        for (id in viewIdToSettingItemMap.keySet()) {
            findViewById(id).setOnClickListener { v ->
                GlobalConfig.setToAllSetting(GlobalConfig.SettingItems.menu_bg_id, viewIdToSettingItemMap.get(id))
                this@MenuBackgroundSelectorActivity.finish()
            }
        }
    }

    @Override
    fun onCreateOptionsMenu(menu: Menu?): Boolean {
        getMenuInflater().inflate(R.menu.menu_bg_selector, menu)
        val drawable: Drawable = menu.getItem(0).getIcon()
        if (drawable != null) {
            drawable.mutate()
            drawable.setColorFilter(getResources().getColor(R.color.default_white), PorterDuff.Mode.SRC_ATOP)
        }
        return true
    }

    @Override
    fun onOptionsItemSelected(menuItem: MenuItem?): Boolean {
        if (menuItem.getItemId() === android.R.id.home) {
            onBackPressed()
        } else if (menuItem.getItemId() === R.id.action_find) {
            if (Build.VERSION.SDK_INT >= 19) {
                val intent = Intent()
                intent.setAction(Intent.ACTION_OPEN_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.setType("image/*")
                startActivityForResult(intent, 1)
            } else {
                // load custom image
                val i = Intent(this, FilePickerActivity::class.java)
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true)
                i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE)
                i.putExtra(FilePickerActivity.EXTRA_START_PATH,
                        if (GlobalConfig.pathPickedSave == null || GlobalConfig.pathPickedSave.length() === 0) Environment.getExternalStorageDirectory().getPath() else GlobalConfig.pathPickedSave)
                startActivityForResult(i, 0)
            }
        }
        return super.onOptionsItemSelected(menuItem)
    }

    @Override
    protected fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) {
            // User cancelled action.
            return
        }
        if (requestCode == 0) {
            // get ttf path
            if (data.getBooleanExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)) {
                val clip: ClipData = data.getClipData()
                if (clip != null) {
                    for (i in 0 until clip.getItemCount()) {
                        val uri: Uri = clip.getItemAt(i).getUri()
                        // Do something with the URI
                        runSaveCustomMenuBackground(uri.toString().replaceAll("file://", ""))
                    }
                }
            } else {
                val uri: Uri = data.getData()
                // Do something with the URI
                if (uri != null) {
                    runSaveCustomMenuBackground(uri.toString().replaceAll("file://", ""))
                }
            }
        } else if (requestCode == 1) {
            // API >= 19, from System file picker.
            val mediaUri: Uri = data.getData()
            if (mediaUri == null || mediaUri.getPath() == null) {
                return  // shouldn't happen.
            }
            val copiedFilePath: String = (GlobalConfig.getDefaultStoragePath() + GlobalConfig.customFolderName + File.separator).toString() + "menu_bg"
            try {
                LightCache.copyFile(getApplicationContext().getContentResolver().openInputStream(mediaUri), copiedFilePath, true)
                runSaveCustomMenuBackground(copiedFilePath.replaceAll("file://", ""))
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                Toast.makeText(this, "Exception: $e", Toast.LENGTH_SHORT).show()
                // Failed to copy. Just ignore it.
            }
        }
    }

    private fun runSaveCustomMenuBackground(path: String?) {
        val options: BitmapFactory.Options?
        try {
            BitmapFactory.decodeFile(path)
        } catch (oome: OutOfMemoryError) {
            // Ooming, load the smaller bitmap.
            try {
                options = Options()
                options.inSampleSize = 2
                val bitmap: Bitmap = BitmapFactory.decodeFile(path, options)
                        ?: throw Exception("PictureDecodeFailedException")
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Exception: $e", Toast.LENGTH_SHORT).show()
                return
            }
        }
        GlobalConfig.setToAllSetting(GlobalConfig.SettingItems.menu_bg_id, "0")
        GlobalConfig.setToAllSetting(GlobalConfig.SettingItems.menu_bg_path, path)
        finish()
    }

    companion object {
        private val viewIdToSettingItemMap: Map<Integer?, String?>? = object : HashMap<Integer?, String?>() {
            init {
                put(R.id.bg01, "1")
                put(R.id.bg02, "2")
                put(R.id.bg03, "3")
                put(R.id.bg04, "4")
                put(R.id.bg05, "5")
            }
        }
    }
}