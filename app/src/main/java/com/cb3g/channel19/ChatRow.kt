package com.cb3g.channel19

class ChatRow {
    var message_id = ""
    var stamp = ""
    var from_id = ""
    var f_handle = ""
    var text = ""
    var profileLink = ""
    var photo = 0
    var url = ""
    var height = 500
    var width = 500

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChatRow

        if (message_id != other.message_id) return false
        if (stamp != other.stamp) return false
        if (from_id != other.from_id) return false
        if (f_handle != other.f_handle) return false
        if (text != other.text) return false
        if (profileLink != other.profileLink) return false
        if (photo != other.photo) return false
        if (url != other.url) return false
        if (height != other.height) return false
        if (width != other.width) return false

        return true
    }

    override fun hashCode(): Int {
        var result = message_id.hashCode()
        result = 31 * result + stamp.hashCode()
        result = 31 * result + from_id.hashCode()
        result = 31 * result + f_handle.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + profileLink.hashCode()
        result = 31 * result + photo
        result = 31 * result + url.hashCode()
        result = 31 * result + height
        result = 31 * result + width
        return result
    }

}