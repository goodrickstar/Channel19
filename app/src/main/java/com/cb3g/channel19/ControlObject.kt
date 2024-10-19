package com.cb3g.channel19

import com.google.firebase.database.Exclude

data class ControlObject(val control: ControlCode = ControlCode.TOAST, val data: Any = "Hello World"){
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "control" to control,
            "data" to data
        )
    }
}