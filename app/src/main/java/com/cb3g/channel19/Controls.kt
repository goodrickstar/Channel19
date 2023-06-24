package com.cb3g.channel19

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.android.multidex.myapplication.R
import java.util.*

@Suppress("SpellCheckingInspection")
class Controls : Fragment() {
    private var boTV: TextView? = null
    private var ringTV: TextView? = null
    private var purgeLimitTV: TextView? = null
    private var pauseLimitTV: TextView? = null
    private var nearbyLimitTV: TextView? = null
    private var gps: CheckBox? = null
    private var share: CheckBox? = null
    private var checkListener: CompoundButton.OnCheckedChangeListener? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.controls, container, false)
    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        super.onViewCreated(v, savedInstanceState)
        val settings = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        val backgroundOn = v.findViewById<RadioButton>(R.id.backgroundOn)
        val backgroundOff = v.findViewById<RadioButton>(R.id.backgroundOff)
        val btOn = v.findViewById<RadioButton>(R.id.btOn)
        val btOff = v.findViewById<RadioButton>(R.id.btOff)
        val hold = v.findViewById<RadioButton>(R.id.hold)
        val toggle = v.findViewById<RadioButton>(R.id.toggle)
        val vibrateon = v.findViewById<RadioButton>(R.id.vibrateon)
        val vibrateoff = v.findViewById<RadioButton>(R.id.vibrateoff)
        val up = v.findViewById<CheckBox>(R.id.up)
        val down = v.findViewById<CheckBox>(R.id.down)
        val kick = v.findViewById<CheckBox>(R.id.kick)
        val welcome = v.findViewById<CheckBox>(R.id.welcome)
        val pmenable = v.findViewById<CheckBox>(R.id.pmenable)
        val photoenable = v.findViewById<CheckBox>(R.id.photoenable)
        val behaviorsw = v.findViewById<RadioGroup>(R.id.behaviorswitch)
        val vibratesw = v.findViewById<RadioGroup>(R.id.vibrateswitch)
        val bluetoothsw = v.findViewById<RadioGroup>(R.id.btSW)
        val backgroundsw = v.findViewById<RadioGroup>(R.id.backgroundswitch)
        val blackout = v.findViewById<SeekBar>(R.id.boBar)
        val ring = v.findViewById<SeekBar>(R.id.ringDelay)
        val pauseLimit = v.findViewById<SeekBar>(R.id.pauseLimitBar)
        val purgeLimit = v.findViewById<SeekBar>(R.id.purgeLimit)
        purgeLimit.progress = RadioService.operator.purgeLimit
        val nearbyLimit = v.findViewById<SeekBar>(R.id.nearbyLimit)
        nearbyLimit.progress = RadioService.operator.nearbyLimit
        val temp = settings.getInt("black", 10)
        val tempTwo = settings.getInt("ring", 1500)
        val pauseLimitInt = settings.getInt("pauseLimit", 150)
        checkListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            Utils.vibrate(buttonView)
            requireContext().sendBroadcast(Intent("nineteenBoxSound"))
            when (buttonView.id) {
                R.id.photoenable -> settings.edit().putBoolean("photos", isChecked).apply()
                R.id.pmenable -> settings.edit().putBoolean("pmenabled", isChecked).apply()
                R.id.kick -> settings.edit().putBoolean("kicksound", isChecked).apply()
                R.id.welcome -> settings.edit().putBoolean("welcomesound", isChecked).apply()
                R.id.up -> settings.edit().putBoolean("overideup", isChecked).apply()
                R.id.down -> settings.edit().putBoolean("overidedown", isChecked).apply()
                R.id.share -> {
                    RadioService.operator.sharing = isChecked
                    settings.edit().putBoolean("sharing", isChecked).apply()
                    requireContext().sendBroadcast(
                        Intent("sharingChange").putExtra(
                            "data",
                            isChecked
                        )
                    )
                }

                R.id.gps -> {
                    if (ActivityCompat.checkSelfPermission(
                            requireActivity().applicationContext,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        requireContext().sendBroadcast(
                            Intent("nineteenShowBlank").putExtra(
                                "title",
                                resources.getString(R.string.gps_access_title)
                            ).putExtra("content", resources.getString(R.string.gps_access_info))
                        )
                    } else {
                        RadioService.operator.locationEnabled.set(isChecked)
                        settings.edit().putBoolean("locationEnabled", isChecked).apply()
                    }
                    requireContext().sendBroadcast(
                        Intent("sharingChange").putExtra(
                            "data",
                            isChecked
                        )
                    )
                    if (!isChecked) RadioService.operator.userLocationString = ""
                }
            }
        }
        val radioListener = RadioGroup.OnCheckedChangeListener { group, checkedId ->
            Utils.vibrate(v)
            requireContext().sendBroadcast(Intent("nineteenBoxSound"))
            when (group.id) {
                R.id.group40 -> settings.edit().putBoolean("darkmap", checkedId == R.id.themeDark)
                    .apply()

                R.id.backgroundswitch -> settings.edit()
                    .putBoolean("custom", checkedId == R.id.backgroundOn).apply()

                R.id.btSW -> requireContext().sendBroadcast(
                    Intent("nineteenBluetoothSettingChange").putExtra(
                        "data",
                        checkedId == R.id.btOn
                    )
                )

                R.id.behaviorswitch -> settings.edit().putBoolean("holdmic", checkedId == R.id.hold)
                    .apply()

                R.id.vibrateswitch ->
                    settings.edit().putBoolean("vibrate", checkedId == R.id.vibrateswitch).apply()
            }
        }
        val seekbarListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                when (seekBar.id) {
                    R.id.ringDelay -> if (progress > 0)
                        ringTV?.text = resources.getString(R.string.mic_animation_speed)
                    else
                        ringTV?.text = resources.getString(R.string.mic_animation_off)

                    R.id.boBar -> if (progress > 0)
                        boTV?.text = resources.getString(R.string.black_out_delay)
                    else
                        boTV?.text = resources.getString(R.string.black_out_off)

                    R.id.pauseLimitBar -> pauseLimitTV?.text =
                        resources.getString(R.string.pauseLimit) + String.format(
                            Locale.getDefault(),
                            "%,d",
                            progress + 50
                        )

                    R.id.purgeLimit -> {
                        RadioService.operator.purgeLimit = progress
                        settings.edit().putInt("purgeLimit", progress).apply()
                        if (progress > 0) purgeLimitTV?.text =
                            resources.getString(R.string.purge_limit) + " " + progress
                        else purgeLimitTV?.text = resources.getString(R.string.unlimited)
                    }

                    R.id.nearbyLimit -> {
                        RadioService.operator.nearbyLimit = progress
                        settings.edit().putInt("nearbyLimit", progress).apply()
                        if (progress > 0) nearbyLimitTV?.text =
                            resources.getString(R.string.nearby_limit) + " " + progress + "m"
                        else nearbyLimitTV?.text =
                            resources.getString(R.string.nearby_limit) + " " + resources.getString(R.string.disabled_text)
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                Utils.vibrate(v)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                Utils.vibrate(seekBar)
                when (seekBar.id) {
                    R.id.ringDelay -> settings.edit().putInt("ring", seekBar.progress).apply()
                    R.id.boBar -> settings.edit().putInt("black", seekBar.progress).apply()
                    R.id.pauseLimitBar -> requireContext().sendBroadcast(
                        Intent("pauseLimitChange").putExtra(
                            "data",
                            seekBar.progress
                        )
                    )
                }
            }
        }
        blackout.progressDrawable.colorFilter = Utils.colorFilter(Color.WHITE)
        purgeLimit.progressDrawable.colorFilter = Utils.colorFilter(Color.WHITE)
        nearbyLimit.progressDrawable.colorFilter = Utils.colorFilter(Color.WHITE)
        pauseLimit.progressDrawable.colorFilter = Utils.colorFilter(Color.WHITE)
        ring.progressDrawable.colorFilter = Utils.colorFilter(Color.WHITE)
        blackout.thumb.colorFilter = Utils.colorFilter(Color.WHITE)
        purgeLimit.thumb.colorFilter = Utils.colorFilter(Color.WHITE)
        nearbyLimit.thumb.colorFilter = Utils.colorFilter(Color.WHITE)
        pauseLimit.thumb.colorFilter = Utils.colorFilter(Color.WHITE)
        ring.thumb.colorFilter = Utils.colorFilter(Color.WHITE)
        boTV = v.findViewById(R.id.boTV)
        val browse = v.findViewById<TextView>(R.id.browse)
        browse.setOnClickListener {
            Utils.vibrate(it)
            requireContext().sendBroadcast(Intent("nineteenClickSound"))
            requireContext().sendBroadcast(Intent("browseBackgrounds"))
        }
        ringTV = v.findViewById(R.id.delayTV)
        purgeLimitTV = v.findViewById(R.id.purgeLimitTV)
        nearbyLimitTV = v.findViewById(R.id.nearbyLimitTV)
        pauseLimitTV = v.findViewById(R.id.pauseLimit)
        gps = v.findViewById(R.id.gps)
        share = v.findViewById(R.id.share)
        blackout.progress = temp
        ring.progress = tempTwo
        pauseLimit.progress = pauseLimitInt
        pauseLimitTV?.text = resources.getString(R.string.pauseLimit) + String.format(
            Locale.getDefault(),
            "%,d",
            pauseLimitInt + 50
        )
        if (RadioService.operator.purgeLimit > 0) purgeLimitTV?.text =
            resources.getString(R.string.purge_limit) + " " + RadioService.operator.purgeLimit
        else purgeLimitTV?.text = resources.getString(R.string.unlimited)
        if (RadioService.operator.nearbyLimit > 0) nearbyLimitTV?.text =
            resources.getString(R.string.nearby_limit) + " " + RadioService.operator.nearbyLimit + "m"
        else nearbyLimitTV?.text =
            resources.getString(R.string.nearby_limit) + " " + resources.getString(R.string.disabled_text)
        if (temp > 0)
            boTV?.text = resources.getString(R.string.black_out_delay)
        else
            boTV?.text = resources.getString(R.string.black_out_off)
        if (tempTwo > 0)
            ringTV?.text = resources.getString(R.string.mic_animation_speed)
        else
            ringTV?.text = resources.getString(R.string.mic_animation_off)
        kick.isChecked = settings.getBoolean("kicksound", true)
        welcome.isChecked = settings.getBoolean("welcomesound", true)
        pmenable.isChecked = settings.getBoolean("pmenabled", true)
        photoenable.isChecked = settings.getBoolean("photos", true)
        gps?.isChecked =
            RadioService.operator.locationEnabled.get() && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        share?.isChecked = RadioService.operator.sharing
        if (settings.getBoolean("custom", false))
            backgroundOn.isChecked = true
        else
            backgroundOff.isChecked = true
        if (settings.getBoolean("bluetooth", true))
            btOn.isChecked = true
        else
            btOff.isChecked = true
        if (settings.getBoolean("holdmic", true))
            hold.isChecked = true
        else
            toggle.isChecked = true
        if (settings.getBoolean("vibrate", true))
            vibrateon.isChecked = true
        else
            vibrateoff.isChecked = true
        if (settings.getBoolean("darkmap", true)) {
            val darkTheme = v.findViewById<RadioButton>(R.id.themeDark)
            darkTheme.isChecked = true
        } else {
            val lightTheme = v.findViewById<RadioButton>(R.id.themeLight)
            lightTheme.isChecked = true
        }
        val themes = v.findViewById<RadioGroup>(R.id.group40)
        themes.setOnCheckedChangeListener(radioListener)
        up.isChecked = settings.getBoolean("overideup", false)
        down.isChecked = settings.getBoolean("overidedown", false)
        gps!!.setOnCheckedChangeListener(checkListener)
        share!!.setOnCheckedChangeListener(checkListener)
        up.setOnCheckedChangeListener(checkListener)
        down.setOnCheckedChangeListener(checkListener)
        kick.setOnCheckedChangeListener(checkListener)
        welcome.setOnCheckedChangeListener(checkListener)
        photoenable.setOnCheckedChangeListener(checkListener)
        pmenable.setOnCheckedChangeListener(checkListener)
        behaviorsw.setOnCheckedChangeListener(radioListener)
        vibratesw.setOnCheckedChangeListener(radioListener)
        bluetoothsw.setOnCheckedChangeListener(radioListener)
        backgroundsw.setOnCheckedChangeListener(radioListener)
        ring.setOnSeekBarChangeListener(seekbarListener)
        blackout.setOnSeekBarChangeListener(seekbarListener)
        pauseLimit.setOnSeekBarChangeListener(seekbarListener)
        purgeLimit.setOnSeekBarChangeListener(seekbarListener)
        nearbyLimit.setOnSeekBarChangeListener(seekbarListener)
    }
}