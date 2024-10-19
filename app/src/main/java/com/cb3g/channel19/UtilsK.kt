package com.cb3g.channel19

import android.util.Log

fun String.log() {
    Log.i("logging", this)
}

fun sendControl(ids :ArrayList<String>, controlObject: ControlObject){
    val key = Utils.getKey()
    val updates = HashMap<String, Any>()
    for(userId in ids){
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