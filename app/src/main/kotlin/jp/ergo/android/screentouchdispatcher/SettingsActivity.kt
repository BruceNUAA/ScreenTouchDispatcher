package jp.ergo.android.screentouchdispatcher

import android.annotation.TargetApi
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.Preference
import android.preference.SwitchPreference
import android.provider.Settings
import android.widget.Space
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.AdView


class SettingsActivity : SettingsActivityBase() {
    var mAdView: AdView? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.pref_general)

        MobileAds.initialize(getApplicationContext(), "ca-app-pub-3940256099942544~3347511713");
        mAdView = AdView(this)
        mAdView?.adSize = AdSize.SMART_BANNER;
        mAdView?.adUnitId = resources.getString(R.string.banner_ad_unit_id)

        val adRequest = AdRequest.Builder().build()
        mAdView?.loadAd(adRequest)
        mAdView?.let {
            listView.addFooterView(it)
        }

        val switcher = findPreference("switcher") as SwitchPreference
        switcher.isChecked = isServiceRunning(ScreenTouchDispatcherService.javaClass)
        switcher.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            if (preference.key == "switcher") {
                if (newValue is Boolean) {
                    return@OnPreferenceChangeListener onSwitchChanged(newValue)
                }
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        mAdView?.resume();
    }

    override fun onPause() {
        mAdView?.pause()
        super.onPause()
    }

    override fun onDestroy() {
        mAdView?.destroy()
        super.onDestroy()
    }

    private fun onSwitchChanged(newValue: Boolean): Boolean {
        if (newValue) {
            if (isMorLater && !canDrawOverlays(this@SettingsActivity)) {
                // MならPermissionのチェック
                (findPreference("switcher") as SwitchPreference).isChecked = false
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + packageName))
                startActivity(intent)
                return false
            } else {
                val serviceIntent = Intent(this@SettingsActivity, ScreenTouchDispatcherService::class.java)
                serviceIntent.action = ScreenTouchDispatcherService.ACTION_START
                startService(serviceIntent)
            }

        } else {
            val serviceIntent = Intent(this@SettingsActivity, ScreenTouchDispatcherService::class.java)
            serviceIntent.action = ScreenTouchDispatcherService.ACTION_STOP
            startService(serviceIntent)
        }
        return true
    }

    private val isMorLater: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

    @TargetApi(23)
    private fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE).any { serviceClass.name.startsWith(it.service.className) }
    }

}
