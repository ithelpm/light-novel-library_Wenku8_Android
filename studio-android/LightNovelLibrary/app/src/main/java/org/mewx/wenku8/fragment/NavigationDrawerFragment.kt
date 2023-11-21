package org.mewx.wenku8.fragment

import android.annotation.SuppressLint

class NavigationDrawerFragment : Fragment() {
    private var mFirebaseAnalytics: FirebaseAnalytics? = null
    private var mFragmentContainerView: View? = null
    private var bgImage: ImageView? = null
    private var mainActivity: MainActivity? = null
    private var mDrawerLayout: DrawerLayout? = null
    private var mActionBarDrawerToggle: ActionBarDrawerToggle? = null
    private var tvUserName: TextView? = null
    private var rivUserAvatar: RoundedImageView? = null
    private var fakeDarkSwitcher = false
    @Nullable
    @Override
    fun onCreateView(@NonNull inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_main_menu, container, false)
    }

    private fun generateNavigationButtonOnClickListener(
            @NonNull targetFragment: MainActivity.FragmentMenuOption?, @NonNull fragment: Fragment?): View.OnClickListener? {
        return View.OnClickListener { v ->
            if (mainActivity.getCurrentFragment() === targetFragment) {
                // Already selected, so just ignore.
                return@OnClickListener
            }
            clearAllButtonColor()
            setHighLightButton(targetFragment)
            mainActivity.setCurrentFragment(targetFragment)
            mainActivity.changeFragment(fragment)
            closeDrawer()

            // Analysis.
            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, fragment.getClass().getSimpleName())
            bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, fragment.getClass().getSimpleName())
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
        }
    }

    @Override
    fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // set button clicked listener, mainly working on change fragment in MainActivity.
        try {
            mainActivity.findViewById(R.id.main_menu_rklist).setOnClickListener(
                    generateNavigationButtonOnClickListener(
                            MainActivity.FragmentMenuOption.RKLIST, RKListFragment())
            )
            mainActivity.findViewById(R.id.main_menu_latest).setOnClickListener(
                    generateNavigationButtonOnClickListener(
                            MainActivity.FragmentMenuOption.LATEST, LatestFragment())
            )
            mainActivity.findViewById(R.id.main_menu_fav).setOnClickListener(
                    generateNavigationButtonOnClickListener(
                            MainActivity.FragmentMenuOption.FAV, FavFragment())
            )
            mainActivity.findViewById(R.id.main_menu_config).setOnClickListener(
                    generateNavigationButtonOnClickListener(
                            MainActivity.FragmentMenuOption.CONFIG, ConfigFragment())
            )
            mainActivity.findViewById(R.id.main_menu_open_source).setOnClickListener { v ->
                val fragmentActivity: FragmentActivity = getActivity() ?: return@setOnClickListener
                Builder(fragmentActivity)
                        .theme(Theme.LIGHT)
                        .title(R.string.main_menu_statement)
                        .content(GlobalConfig.getOpensourceLicense())
                        .stackingBehavior(StackingBehavior.ALWAYS)
                        .positiveColorRes(R.color.dlgPositiveButtonColor)
                        .positiveText(R.string.dialog_positive_known)
                        .show()
            }
            mainActivity.findViewById(R.id.main_menu_dark_mode_switcher).setOnClickListener { v -> openOrCloseDarkMode() }
        } catch (e: NullPointerException) {
            Toast.makeText(mainActivity, "NullPointerException in onActivityCreated();", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }

        // User Account
        val activity: FragmentActivity = getActivity()
        if (activity != null) {
            rivUserAvatar = activity.findViewById(R.id.user_avatar)
            tvUserName = activity.findViewById(R.id.user_name)
        }
        val ocl: View.OnClickListener = View.OnClickListener { v ->
            if (!LightUserSession.getLogStatus() && GlobalConfig.isNetworkAvailable(getActivity())) {
                if (!LightUserSession.isUserInfoSet()) {
                    val intent = Intent(getActivity(), UserLoginActivity::class.java)
                    startActivity(intent)
                } else {
                    // show dialog to login, error to jump to login activity
                    if (LightUserSession.aiui.getStatus() === AsyncTask.Status.FINISHED) {
                        Toast.makeText(getActivity(), "Relogged.", Toast.LENGTH_SHORT).show()
                        LightUserSession.aiui = AsyncInitUserInfo()
                        LightUserSession.aiui.execute()
                    }
                }
            } else if (!GlobalConfig.isNetworkAvailable(getActivity())) {
                // no network, no log in
                Toast.makeText(getActivity(), getResources().getString(R.string.system_network_error), Toast.LENGTH_SHORT).show()
            } else {
                // Logged, click to info page
                val intent = Intent(getActivity(), UserInfoActivity::class.java)
                startActivity(intent)
            }
        }
        rivUserAvatar.setOnClickListener(ocl)
        tvUserName.setOnClickListener(ocl)

        // Initial: set color states here ...
        // get net work status, no net -> FAV
        if (activity != null && !GlobalConfig.isNetworkAvailable(activity)) {
            clearAllButtonColor()
            setHighLightButton(MainActivity.FragmentMenuOption.FAV)
            mainActivity.setCurrentFragment(MainActivity.FragmentMenuOption.FAV)
            mainActivity.changeFragment(FavFragment())
        } else {
            clearAllButtonColor()
            setHighLightButton(mainActivity.getCurrentFragment())
            mainActivity.changeFragment(LatestFragment())
        }
        // TODO: need to track the initial fragment.

        // set menu background
        if (activity != null) {
            bgImage = activity.findViewById(R.id.bg_img)
            updateMenuBackground()
        }
    }

    @Override
    fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    fun setup(fragmentId: Int, drawerLayout: DrawerLayout?, toolbar: Toolbar?) {
        // get MainActivity
        mainActivity = getActivity() as MainActivity?
        if (mainActivity == null) Toast.makeText(getActivity(), "mainActivity == null !!! in setup()", Toast.LENGTH_SHORT).show()

        // Init Firebase Analytics on GA4.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(mainActivity)
        mFragmentContainerView = mainActivity.findViewById(fragmentId)
        mDrawerLayout = drawerLayout
        mActionBarDrawerToggle = object : ActionBarDrawerToggle(mainActivity, mDrawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close) {
            @Override
            fun onDrawerClosed(drawerView: View?) {
                super.onDrawerClosed(drawerView)
                if (!isAdded()) return
                mainActivity.invalidateOptionsMenu()
            }

            @Override
            fun onDrawerOpened(drawerView: View?) {
                super.onDrawerOpened(drawerView)
                if (!isAdded()) return
                mainActivity.invalidateOptionsMenu()
                updateNavigationBar()
            }
        }
        mDrawerLayout.post { mActionBarDrawerToggle.syncState() }
        mDrawerLayout.addDrawerListener(mActionBarDrawerToggle)
        updateNavigationBar()
    }

    private fun clearOneButtonColor(iconId: Int, textId: Int, backgroundId: Int) {
        // Clear icon color.
        val imageButton: ImageButton = mainActivity.findViewById(iconId)
        if (imageButton != null) {
            imageButton.setColorFilter(getResources().getColor(R.color.menu_text_color))
        }

        // Clear text color.
        val textView: TextView = mainActivity.findViewById(textId)
        if (textView != null) {
            textView.setTextColor(getResources().getColor(R.color.menu_text_color))
        }

        // Clear view background color (only works for API 16+).
        val tableRow: TableRow = mainActivity.findViewById(backgroundId)
        if (tableRow != null) {
            tableRow.setBackground(getResources().getDrawable(R.drawable.btn_menu_item))
        }
    }

    /**
     * This function clear all the effects on button, and it needs API Level 16.
     * So if the device is between 4.0-4.1, it will appear no effects.
     *
     * Notice:
     * Once the enum MainActivity.FRAGMENT_LIST changes, this function show be edited.
     */
    private fun clearAllButtonColor() {
        clearOneButtonColor(R.id.main_menu_ic_rklist, R.id.main_menu_text_rklist, R.id.main_menu_rklist)
        clearOneButtonColor(R.id.main_menu_ic_latest, R.id.main_menu_text_latest, R.id.main_menu_latest)
        clearOneButtonColor(R.id.main_menu_ic_fav, R.id.main_menu_text_fav, R.id.main_menu_fav)
        clearOneButtonColor(R.id.main_menu_ic_config, R.id.main_menu_text_config, R.id.main_menu_config)
    }

    @SuppressLint("NewApi")
    private fun setHighLightButton(iconId: Int, textId: Int, backgroundId: Int) {
        val icon: ImageButton = mainActivity.findViewById(iconId)
        if (icon != null) {
            icon.setColorFilter(getResources().getColor(R.color.menu_text_color_selected))
        }
        val textView: TextView = mainActivity.findViewById(textId)
        if (textView != null) {
            textView.setTextColor(getResources().getColor(R.color.menu_item_white))
        }

        // Set view background color (only works for API 16+).
        val tableRow: TableRow = mainActivity.findViewById(backgroundId)
        if (tableRow != null) {
            tableRow.setBackground(getResources().getDrawable(R.drawable.btn_menu_item_selected))
        }
    }

    /**
     * This function will highlight the button selected, and switch to the fragment in MainActivity.
     *
     * @param f the target fragment, type MainActivity.FRAGMENT_LIST.
     */
    private fun setHighLightButton(f: MainActivity.FragmentMenuOption?) {
        when (f) {
            RKLIST -> setHighLightButton(R.id.main_menu_ic_rklist, R.id.main_menu_text_rklist, R.id.main_menu_rklist)
            LATEST -> setHighLightButton(R.id.main_menu_ic_latest, R.id.main_menu_text_latest, R.id.main_menu_latest)
            FAV -> setHighLightButton(R.id.main_menu_ic_fav, R.id.main_menu_text_fav, R.id.main_menu_fav)
            CONFIG -> setHighLightButton(R.id.main_menu_ic_config, R.id.main_menu_text_config, R.id.main_menu_config)
        }
    }

    /**
     * Judge whether the dark mode is open. If is open, close it; else open it.
     */
    private fun openOrCloseDarkMode() {
        val darkModeSwitcherText: TextView = mainActivity.findViewById(R.id.main_menu_dark_mode_switcher)
        if (darkModeSwitcherText != null) {
            // Set view background color (only works for API 16+).
            darkModeSwitcherText.setTextColor(getResources().getColor(
                    if (fakeDarkSwitcher) /*switch off*/ R.color.menu_text_color else  /*switch on*/ R.color.menu_text_color_selected
            ))
            darkModeSwitcherText.setBackground(getResources().getDrawable(
                    if (fakeDarkSwitcher) /*switch off*/ R.drawable.btn_menu_item else  /*switch on*/ R.drawable.btn_menu_item_selected
            ))
        }
        fakeDarkSwitcher = !fakeDarkSwitcher
        Toast.makeText(getActivity(), "夜间模式到阅读界面去试试~", Toast.LENGTH_SHORT).show()
    }

    private fun updateNavigationBar() {
        if (Build.VERSION.SDK_INT < 19) {
            // Transparency is not supported in below KitKat.
            return
        }

        // test navigation bar exist
        val activity: FragmentActivity = getActivity()
        val navBar: Point = LightTool.getNavigationBarSize(getActivity())

        // TODO: fix this margin for screen cutout.
        val ll: LinearLayout = mainActivity.findViewById(R.id.main_menu_bottom_layout)
        if (activity != null && navBar.y === 0) {
            ll.setPadding(0, 0, 0, 0) // hide
        } else if (activity != null && (navBar.y < 10 || navBar.y >= LightTool.getAppUsableScreenSize(activity).y)) {
            ll.setPadding(0, 0, 0, LightTool.getAppUsableScreenSize(activity).y / 10)
        } else {
            ll.setPadding(0, 0, 0, navBar.y) // show
        }
    }

    @Override
    fun onResume() {
        super.onResume()

        // user info update
        if (LightUserSession.isUserInfoSet() && !tvUserName.getText().toString().equals(LightUserSession.getUsernameOrEmail())
                && (LightCache.testFileExist(GlobalConfig.getFirstUserAvatarSaveFilePath())
                        || LightCache.testFileExist(GlobalConfig.getSecondUserAvatarSaveFilePath()))) {
            tvUserName.setText(LightUserSession.getUsernameOrEmail())
            val avatarPath: String
            avatarPath = if (LightCache.testFileExist(GlobalConfig.getFirstUserAvatarSaveFilePath())) GlobalConfig.getFirstUserAvatarSaveFilePath() else GlobalConfig.getSecondUserAvatarSaveFilePath()
            val options: BitmapFactory.Options = Options()
            options.inSampleSize = 2
            val bm: Bitmap = BitmapFactory.decodeFile(avatarPath, options)
            if (bm != null) rivUserAvatar.setImageBitmap(bm)
        } else if (!LightUserSession.isUserInfoSet()) {
            tvUserName.setText(getResources().getString(R.string.main_menu_not_login))
            rivUserAvatar.setImageDrawable(getResources().getDrawable(R.drawable.ic_noavatar))
        }

        // update menu background
        updateMenuBackground()
    }

    private fun updateMenuBackground() {
        val settingMenuBgId: String = GlobalConfig.getFromAllSetting(GlobalConfig.SettingItems.menu_bg_id)
        if (settingMenuBgId != null) {
            when (settingMenuBgId) {
                "0" -> {
                    var bmMenuBackground: Bitmap
                    try {
                        bmMenuBackground = BitmapFactory.decodeFile(GlobalConfig.getFromAllSetting(GlobalConfig.SettingItems.menu_bg_path))
                    } catch (oome: OutOfMemoryError) {
                        try {
                            val options: BitmapFactory.Options = Options()
                            options.inSampleSize = 2
                            bmMenuBackground = BitmapFactory.decodeFile(GlobalConfig.getFromAllSetting(GlobalConfig.SettingItems.menu_bg_path), options)
                            if (bmMenuBackground == null) throw Exception("PictureLoadFailureException")
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(getActivity(), """
     ${"Exception: " + e.toString()}
     可能的原因有：图片不在内置SD卡；图片格式不正确；图片像素尺寸太大，请使用小一点的图，谢谢，此功能为试验性功能；
     """.trimIndent(), Toast.LENGTH_SHORT).show()
                            bgImage.setImageDrawable(getResources().getDrawable(R.drawable.bg_avatar_04))
                            return
                        }
                    }
                    bgImage.setImageBitmap(bmMenuBackground)
                }

                "1" -> bgImage.setImageDrawable(getResources().getDrawable(R.drawable.bg_avatar_01))
                "2" -> bgImage.setImageDrawable(getResources().getDrawable(R.drawable.bg_avatar_02))
                "3" -> bgImage.setImageDrawable(getResources().getDrawable(R.drawable.bg_avatar_03))
                "4" -> bgImage.setImageDrawable(getResources().getDrawable(R.drawable.bg_avatar_04))
                "5" -> bgImage.setImageDrawable(getResources().getDrawable(R.drawable.bg_avatar_05))
            }
        }
    }

    fun openDrawer() {
        mDrawerLayout.openDrawer(mFragmentContainerView)
    }

    fun closeDrawer() {
        mDrawerLayout.closeDrawer(mFragmentContainerView)
    }

    @Override
    fun onDetach() {
        super.onDetach()
    }

    fun isDrawerOpen(): Boolean {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView)
    }

    @Override
    fun onSaveInstanceState(@NonNull outState: Bundle?) {
        super.onSaveInstanceState(outState)
    }

    companion object {
        private val TAG: String? = NavigationDrawerFragment::class.java.getSimpleName()
    }
}