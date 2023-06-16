package com.cb3g.channel19

import android.view.View
import android.widget.ImageView
import android.widget.TextView

import com.example.android.multidex.myapplication.R

internal class TextHolder(v: View) {
    val name: TextView = v.findViewById(R.id.profile_name)
    val content: TextView = v.findViewById(R.id.text_content)
    val stamp: TextView = v.findViewById(R.id.profile_stamp)
    val profile: ImageView = v.findViewById(R.id.profile_pic)
    val menu: ImageView = v.findViewById(R.id.profile_menu)
}
