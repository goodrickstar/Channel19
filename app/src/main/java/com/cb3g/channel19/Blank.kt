package com.cb3g.channel19


import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.android.multidex.myapplication.R

//TODO: rewrite in JAVA
class Blank : DialogFragment(), View.OnClickListener {
    var title: TextView? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.blank, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val content = view.findViewById<View>(R.id.content) as TextView
        title = view.findViewById<View>(R.id.black_banner_tv) as TextView
        val ok = view.findViewById<View>(R.id.ok) as TextView
        content.text = arguments?.getString("content")
        title?.text = arguments?.getString("title")
        ok.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        Utils.vibrate(v)
        requireContext().sendBroadcast(Intent("nineteenClickSound"))
        if (title?.text.toString() == "Profile Picture") requireContext().sendBroadcast(Intent("nineteenPickProfile"))
        if (title?.text.toString() == "Please Update") gotoPlayStore()
        if (title?.text.toString() == ("Permissions Needed")) requireContext().sendBroadcast(
            Intent(
                "nineteenAllow"
            )
        )
        if (title?.text.toString()
                .contains("Location")
        ) requireContext().sendBroadcast(Intent("requestGPS"))
        if (title?.text.toString()
                .contains("Camera")
        ) requireContext().sendBroadcast(Intent("nineteenCamera"))
        dismiss()
    }

    private fun gotoPlayStore() {
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + requireContext().packageName)
                ).addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            )
        } catch (e: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=com.cb3g.channel19")
                )
            )
        }
    }


}

