package org.mewx.wenku8.activity

import android.Manifest

class MainActivity : BaseMaterialActivity() {
    // This is for fragment switch
    enum class FragmentMenuOption {
        RKLIST, LATEST, FAV, CONFIG
    }

    private var status: FragmentMenuOption? = FragmentMenuOption.LATEST
    fun getCurrentFragment(): FragmentMenuOption? {
        return status
    }

    fun setCurrentFragment(f: FragmentMenuOption?) {
        status = f
    }

    private var mNavigationDrawerFragment: NavigationDrawerFragment? = null
    private var mFirebaseAnalytics: FirebaseAnalytics? = null
    private fun initialApp() {
        // load language
        val locale: Locale
        locale = when (GlobalConfig.getCurrentLang()) {
            TC -> Locale.TRADITIONAL_CHINESE
            SC -> Locale.SIMPLIFIED_CHINESE
            else -> Locale.SIMPLIFIED_CHINESE
        }
        val config = Configuration()
        config.locale = locale
        Locale.setDefault(locale)
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics())

        // Requests storage RW permissions only when save file migration is not done.
        if (SaveFileMigration.migrationCompleted()) {
            Log.i(TAG, "Save file migration has completed.")
        } else {
            // Write permission.
            if (missingPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(this, arrayOf<String?>(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_WRITE_EXTERNAL)
            }
        }

        // Read permissions.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // FIXME: this doesn't work on the first launch yet (it works on the second+ launch somehow).
            if (missingPermission(Manifest.permission.READ_MEDIA_IMAGES)) {
                ActivityCompat.requestPermissions(this, arrayOf<String?>(Manifest.permission.READ_MEDIA_IMAGES), REQUEST_READ_MEDIA_IMAGES)
            }
        } else {
            if (missingPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(this, arrayOf<String?>(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_READ_EXTERNAL)
            }
        }

        // Create save folder.
        if (Build.VERSION.SDK_INT >= EXTERNAL_SAVE_MIGRATION_API) {
            // Does not support external storage if the file isn't already created.
            if (!LightCache.testFileExist(SaveFileMigration.getExternalStoragePath())) {
                GlobalConfig.setExternalStoragePathAvailable(false)
            }
            // Else, start migration.
        } else {
            // FIXME: these imgs folders are actually no in use.
            LightCache.saveFile(GlobalConfig.getDefaultStoragePath() + "imgs", ".nomedia", "".getBytes(), false)
            LightCache.saveFile(GlobalConfig.getDefaultStoragePath() + GlobalConfig.customFolderName, ".nomedia", "".getBytes(), false)
            LightCache.saveFile(GlobalConfig.getBackupStoragePath() + "imgs", ".nomedia", "".getBytes(), false)
            LightCache.saveFile(GlobalConfig.getBackupStoragePath() + GlobalConfig.customFolderName, ".nomedia", "".getBytes(), false)
            GlobalConfig.setExternalStoragePathAvailable(LightCache.testFileExist((SaveFileMigration.getExternalStoragePath() + "imgs" + File.separator).toString() + ".nomedia", true))
        }

        // execute background action
        LightUserSession.aiui = AsyncInitUserInfo()
        LightUserSession.aiui.execute()
        GlobalConfig.loadAllSetting()

        // check new version and load notice text
        Wenku8API.NoticeString = GlobalConfig.loadSavedNotice()
    }

    /**
     * For API 29+, migrate saves from external storage to internal storage.
     */
    private fun startOldSaveMigration() {
        if (Build.VERSION.SDK_INT < EXTERNAL_SAVE_MIGRATION_API || SaveFileMigration.migrationCompleted()
                || (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                        && missingPermission(Manifest.permission.READ_EXTERNAL_STORAGE))) {
            return
        }

        // The permission issue for Android API 33+.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && SaveFileMigration.migrationEligible()) {
            Log.d(TAG, "startOldSaveMigration: Eligible")
            Builder(this@MainActivity)
                    .theme(Theme.LIGHT)
                    .backgroundColorRes(R.color.dlgBackgroundColor)
                    .contentColorRes(R.color.dlgContentColor)
                    .positiveColorRes(R.color.dlgPositiveButtonColor)
                    .neutralColorRes(R.color.dlgNegativeButtonColor)
                    .negativeColorRes(R.color.myAccentColor)
                    .content(R.string.system_save_need_to_migrate)
                    .positiveText(R.string.dialog_positive_upgrade) // This neutral text is needed because some users couldn't get system file picker.
                    .neutralText(R.string.dialog_negative_pass_for_now)
                    .negativeText(R.string.dialog_negative_never)
                    .onPositive { unused1, unused2 ->
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                        intent.addCategory(Intent.CATEGORY_DEFAULT)
                        startActivityForResult(Intent.createChooser(intent, "Choose directory"), REQUEST_READ_EXTERNAL_SAVES)
                    } // Do nothing for onNeutral.
                    .onNegative { dialog, which -> SaveFileMigration.markMigrationCompleted() }
                    .cancelable(false)
                    .show()

            // Return early to wait for the permissions grant on the directory.
            return
        }

        // The rest Android Q+ devices.
        runExternalSaveMigration()
    }

    private fun runExternalSaveMigration() {
        val progressDialog: MaterialDialog = Builder(this@MainActivity)
                .theme(Theme.LIGHT)
                .content(R.string.system_save_upgrading)
                .progress(false, 1, false)
                .cancelable(false)
                .show()
        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper()) // Handles the UI works.
        executor.execute {

            // Generate the migration plan in async path.
            val filesToCopy: List<Uri?> = SaveFileMigration.generateMigrationPlan()

            // Analysis.
            val saveMigrationFilesTotalParams = Bundle()
            saveMigrationFilesTotalParams.putString("count", "" + filesToCopy.size())
            mFirebaseAnalytics.logEvent("save_migration_files_total", saveMigrationFilesTotalParams)
            if (filesToCopy.isEmpty()) {
                Log.d(TAG, "Empty list of files to copy")
                handler.post(progressDialog::dismiss)
                SaveFileMigration.markMigrationCompleted()
                return@execute
            }
            // Update max in the progress UI.
            handler.post { progressDialog.setMaxProgress(filesToCopy.size()) }

            // Start migration.
            var progress = 0
            var failedFiles = 0
            for (filePath in filesToCopy) {
                try {
                    val targetFilePath: String = SaveFileMigration.migrateFile(filePath)
                    if (!LightCache.testFileExist(targetFilePath, true)) {
                        Log.d(TAG, String.format("Failed migrating: %s (from %s)", targetFilePath, filePath))
                        failedFiles++
                    }
                } catch (e: FileNotFoundException) {
                    failedFiles++
                    e.printStackTrace()
                }
                progress++
                val finalProgress = progress
                handler.post { progressDialog.setProgress(finalProgress) }
            }

            // Adding some delay to prevent UI crashing at the time of reloading.
            try {
                Thread.sleep(2000)
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
            val finalFailedFiles = failedFiles

            // Analysis.
            val saveMigrationFilesFailedParams = Bundle()
            saveMigrationFilesFailedParams.putString("failed", "" + finalFailedFiles)
            mFirebaseAnalytics.logEvent("save_migration_files_failed", saveMigrationFilesFailedParams)
            handler.post {
                SaveFileMigration.markMigrationCompleted()
                progressDialog.dismiss()
                Builder(this@MainActivity)
                        .theme(Theme.LIGHT)
                        .backgroundColorRes(R.color.dlgBackgroundColor)
                        .contentColorRes(R.color.dlgContentColor)
                        .positiveColorRes(R.color.dlgPositiveButtonColor)
                        .content(R.string.system_save_migrated, filesToCopy.size(), finalFailedFiles)
                        .positiveText(R.string.dialog_positive_sure)
                        .onPositive { unused1, unused2 -> reloadApp() }
                        .cancelable(false)
                        .show()
            }
        }
    }

    @Override
    protected fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initMaterialStyle(R.layout.layout_main, HomeIndicatorStyle.HAMBURGER)
        initialApp()

        // Init Firebase Analytics on GA4.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        // UIL setting
        if (ImageLoader.getInstance() == null || !ImageLoader.getInstance().isInited()) {
            GlobalConfig.initImageLoader(this)
        }

        // Updates old save files.
        startOldSaveMigration()

        // set Tool button
        mNavigationDrawerFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_drawer) as NavigationDrawerFragment
        if (mNavigationDrawerFragment != null) {
            mNavigationDrawerFragment.setup(R.id.fragment_drawer, findViewById(R.id.drawer), getToolbar())
        }

        // find search box
        getToolbar().setOnMenuItemClickListener { item ->
            //Toast.makeText(MyApp.getContext(),"called button",Toast.LENGTH_SHORT).show();
            if (item.getItemId() === R.id.action_search) {
                // start search activity
                startActivity(Intent(this@MainActivity, SearchActivity::class.java))
                overridePendingTransition(R.anim.fade_in, R.anim.hold) // fade in animation
            }
            true
        }
    }

    /**
     * Hard menu button works like the soft menu button.
     * And this will control all the menu appearance,
     * I can handle the button list by edit this function.
     *
     * @param menu The options menu in which you place your items, but I ignore this.
     * @return True if shown successfully, False if failed.
     */
    @Override
    fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // only when the navigation draw closed, I draw the menu bar.
        // the menu items will be drawn automatically
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // change title of toolbar
            when (status) {
                FragmentMenuOption.LATEST -> {
                    if (getSupportActionBar() != null) getSupportActionBar().setTitle(getResources().getString(R.string.main_menu_latest))
                    getMenuInflater().inflate(R.menu.menu_latest, menu)
                }

                FragmentMenuOption.RKLIST -> if (getSupportActionBar() != null) getSupportActionBar().setTitle(getResources().getString(R.string.main_menu_rklist))
                FragmentMenuOption.FAV -> if (getSupportActionBar() != null) getSupportActionBar().setTitle(getResources().getString(R.string.main_menu_fav))
                FragmentMenuOption.CONFIG -> if (getSupportActionBar() != null) getSupportActionBar().setTitle(getResources().getString(R.string.main_menu_config))
            }
        } else {
            if (getSupportActionBar() != null) getSupportActionBar().setTitle(getResources().getString(R.string.app_name))
        }
        return true
    }

    /**
     * This function will be called by NavigationDrawerFragment,
     * once called, change fragment.
     *
     * @param targetFragment target fragment.
     */
    fun changeFragment(targetFragment: Fragment?) {
        // temporarily set elevation to remove rank list toolbar shadow
        if (getSupportActionBar() != null) {
            if (status == FragmentMenuOption.RKLIST) getSupportActionBar().setElevation(0) else getSupportActionBar().setElevation(getResources().getDimension(R.dimen.toolbar_elevation))
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, targetFragment, "fragment")
                .setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit()
    }

    @Override
    protected fun onResume() {
        super.onResume()

        // load only the first time this activity is created
        if (!NEW_VERSION_CHECKED.getAndSet(true)) {
            CheckAppNewVersion(this@MainActivity).execute()
            UpdateNotificationMessage().execute()
        }
    }

    @Override
    fun onRequestPermissionsResult(requestCode: Int, @NonNull permissions: Array<String?>?, @NonNull grantResults: IntArray?) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_WRITE_EXTERNAL, REQUEST_READ_EXTERNAL -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // The result will fall through.
                    break
                }
                if (grantResults.size > 0 && grantResults.get(0) == PackageManager.PERMISSION_GRANTED) {
                    reloadApp()
                } else {
                    Toast.makeText(this, getResources().getText(R.string.missing_permission), Toast.LENGTH_LONG).show()
                }
            }

            REQUEST_READ_MEDIA_IMAGES -> if (grantResults.size > 0 && grantResults.get(0) == PackageManager.PERMISSION_GRANTED) {
                reloadApp()
            } else {
                Toast.makeText(this, getResources().getText(R.string.missing_permission), Toast.LENGTH_LONG).show()
            }
        }
    }

    @Override
    protected fun onActivityResult(requestCode: Int, resultCode: Int, @Nullable data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_READ_EXTERNAL_SAVES && resultCode == Activity.RESULT_OK && data != null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                return
            }
            val wenku8Uri: Uri = data.getData()
            val wenku8Path: String = wenku8Uri.getPath()
            if (!wenku8Uri.getLastPathSegment().endsWith("wenku8") || wenku8Path.contains("DCIM") || wenku8Path.contains("Picture")) {
                Log.i(TAG, "LastPathSegment: " + wenku8Uri.getLastPathSegment())
                Log.i(TAG, "Selected path for save migration doesn't look right: $wenku8Uri")
                val saveMigrationParams = Bundle()
                saveMigrationParams.putString("path", wenku8Path)
                saveMigrationParams.putString("valid_path", "false")
                mFirebaseAnalytics.logEvent("save_migration_path_selection", saveMigrationParams)
                Builder(this@MainActivity)
                        .theme(Theme.LIGHT)
                        .backgroundColorRes(R.color.dlgBackgroundColor)
                        .contentColorRes(R.color.dlgContentColor)
                        .positiveColorRes(R.color.dlgPositiveButtonColor)
                        .neutralColorRes(R.color.dlgNegativeButtonColor)
                        .negativeColorRes(R.color.myAccentColor)
                        .content(R.string.dialog_content_wrong_path, wenku8Path.replace("/tree/primary:", "/"))
                        .positiveText(R.string.dialog_positive_retry)
                        .neutralText(R.string.dialog_negative_pass_for_now)
                        .negativeText(R.string.dialog_negative_never)
                        .onPositive { unused1, unused2 -> reloadApp() } // Do nothing for onNeutral.
                        .onNegative { dialog, which -> SaveFileMigration.markMigrationCompleted() }
                        .cancelable(false)
                        .show()
                return
            } else {
                val saveMigrationParams = Bundle()
                saveMigrationParams.putString("path", wenku8Path)
                saveMigrationParams.putString("valid_path", "true")
                mFirebaseAnalytics.logEvent("save_migration_path_selection", saveMigrationParams)
            }
            getContentResolver().takePersistableUriPermission(wenku8Uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            Log.i(TAG, "Selected the right directory: $wenku8Path")
            // Overriding the external path is needed to help generating new paths.
            SaveFileMigration.overrideExternalPath(wenku8Uri)
            runExternalSaveMigration()
        }
    }

    @Override
    fun onBackPressed() {
        if (mNavigationDrawerFragment.isDrawerOpen()) mNavigationDrawerFragment.closeDrawer() else exitBy2Click()
    }

    private fun exitBy2Click() {
        // press twice to exit
        val tExit: Timer?
        if (!isExit) {
            isExit = true // ready to exit
            Toast.makeText(
                    this,
                    this.getResources().getString(R.string.press_twice_to_exit),
                    Toast.LENGTH_SHORT).show()
            tExit = Timer()
            tExit.schedule(object : TimerTask() {
                @Override
                fun run() {
                    isExit = false // cancel exit
                }
            }, 2000) // 2 seconds cancel exit task
        } else {
            finish()
        }
    }

    private fun missingPermission(permissionName: String?): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionName) !== PackageManager.PERMISSION_GRANTED
    }

    private fun reloadApp() {
        //reload my activity with permission granted or use the features what required the permission
        val i: Intent = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName())
        if (i != null) {
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(i)
        }
    }

    companion object {
        private val TAG: String? = MainActivity::class.java.getSimpleName()
        private val EXTERNAL_SAVE_MIGRATION_API: Int = Build.VERSION_CODES.Q // Decrease only.

        // Below request codes can be any value.
        private const val REQUEST_WRITE_EXTERNAL = 100
        private const val REQUEST_READ_EXTERNAL = 101
        private const val REQUEST_READ_MEDIA_IMAGES = 102
        private const val REQUEST_READ_EXTERNAL_SAVES = 103
        private val NEW_VERSION_CHECKED: AtomicBoolean? = AtomicBoolean(false)
        private var isExit: Boolean? = false // used for exit by twice
    }
}