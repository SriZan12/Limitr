package com.example.limitr.services.blockerServices


//@AndroidEntryPoint
//class AppFoundReceiver : BroadcastReceiver() {
//
//    @Inject
//    lateinit var limitrDao: LimitrDao
//
//    @Inject
//    lateinit var overlayScreen: OverlayScreen
//
//    override fun onReceive(context: Context?, intent: Intent?) {
//        Timber.d("I AM INSIDE RECEIVER")
//        Timber.d("APPNAMERECEIVER = ${intent?.getStringExtra(context?.getString(R.string.appName))}")
//        Timber.d("APPpackageNAMERECEIVER = ${intent?.getStringExtra(context?.getString(R.string.packageName))}")
//        Timber.d("I AM INSIDE RECEIVER")
//        val appName = intent?.getStringExtra(context?.getString(R.string.appName))
//        val appPackage = intent?.getStringExtra(context?.getString(R.string.packageName))
//
//        checkApp(appName = appName!!, appPackage = appPackage!!, context = context!!)
//    }
//
//    private fun checkApp(appName: String, appPackage: String?, context: Context) {
//
//        Timber.d("INSIDE CHECK APP")
//
//        if (limitrDao.getAppName(appName = appName)?.appName == appName) {
//
//            Timber.d("OVERLAY_DISPLAYED = $OVERLAY_DISPLAYED")
//
//            if (!OVERLAY_DISPLAYED) {
//
//                val am = context.getSystemService(Activity.ACTIVITY_SERVICE) as ActivityManager?
//                am!!.killBackgroundProcesses(appPackage)
//
//                Timber.d("OVERLAY SHOWN")
//
//                overlayScreen.showOverlayScreen(
//                    appName = appName,
//                    context = context,
//
//                ) {
//                    launchBlockingActivity(
//                        appName = appName,
//                        appPackage = appPackage,
//                        context = context
//                    )
//
//                    OVERLAY_DISPLAYED = true
//                }
//            }
//        }
//    }
//
//    private fun launchBlockingActivity(appName: String, appPackage: String?, context: Context) {
//        val blockedIntent = Intent(context, ActivityBlocked::class.java)
//        blockedIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
//        blockedIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//        blockedIntent.putExtra(context.getString(R.string.appName), appName)
//        blockedIntent.putExtra(context.getString(R.string.packageName), appPackage)
//        context.applicationContext.startActivity(blockedIntent)
//    }




//    private fun getBlockedAppViewModel(context: Context): BlockedAppVM {
//        val factory =
//            ViewModelProvider.AndroidViewModelFactory.getInstance(applicationContext as Application)
//        return ViewModelProvider(context as ViewModelStoreOwner, factory)[BlockedAppVM::class.java]
//    }
//}