package com.cb3g.channel19

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View

fun String.log() {
    Log.i("logging", this)
}

fun sendControl(ids :ArrayList<String>, controlObject: ControlObject){
    val key = Utils.getKey()
    val updates = HashMap<String, Any>()
    for(userId in ids.distinct()){
        updates["$userId/$key"] = controlObject.toMap()
    }
    Utils.control().updateChildren(updates)
}
fun sendControl(id :String, controlObject: ControlObject){
    val key = Utils.getKey()
    val updates = HashMap<String, Any>()
    updates["$id/$key"] = controlObject.toMap()
    Utils.control().updateChildren(updates)
}

fun ControlObject.send(ids :ArrayList<String>){
    val key = Utils.getKey()
    val updates = HashMap<String, Any>()
    for(userId in ids){
        updates["$userId/$key"] = this.toMap()
    }
    Utils.control().updateChildren(updates)
}
fun View.clicked(context: Context){
    this.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    context.sendBroadcast(Intent("nineteenClickSound").setPackage("com.cb3g.channel19"))
}

fun gotoPlayStore(context: Context) {
    val appPackageName: String = context.packageName
    try {
        val appStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName"))
        appStoreIntent.setPackage("com.android.vending")
        context.startActivity(appStoreIntent)
    } catch (exception: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")).setPackage("com.cb3g.channel19"))
    }
}