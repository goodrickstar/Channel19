package com.cb3g.channel19


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.android.multidex.myapplication.databinding.BlankBinding

class Blank : DialogFragment(){
    lateinit var binding: BlankBinding
    private lateinit var c: Context

    override fun onAttach(context: Context) {
        super.onAttach(context)
        c = context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = BlankBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.content.text = arguments?.getString("content")
        binding.blackBannerTv.text = arguments?.getString("title")
        binding.ok.setOnClickListener { v ->
            v.clicked(c)
            when (binding.blackBannerTv.text) {
                "Profile Picture" -> c.sendBroadcast(Intent("nineteenPickProfile").setPackage("com.cb3g.channel19"))
                "Please Update" -> gotoPlayStore(c)
                "Permissions Needed" -> requireContext().sendBroadcast(Intent("nineteenAllow").setPackage("com.cb3g.channel19"))
                "Location" -> c.sendBroadcast(Intent("requestGPS").setPackage("com.cb3g.channel19"))
                "Camera" -> c.sendBroadcast(Intent("nineteenCamera").setPackage("com.cb3g.channel19"))
            }
            dismiss()
        }
    }
}

