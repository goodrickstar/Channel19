package com.cb3g.channel19

import androidx.databinding.ObservableBoolean

class Operator internal constructor() {
    var key = ""
    var user_id = ""
    var handle = ""
    var town = ""
    var carrier = ""
    var profileLink = ""
    var rank = ""
    var userLocationString = ""
    var stamp = ""
    var silenced = false
    var subscribed = false
    var invisible = false
    var limit = 50
    var count = 0
    var salutes = 0
    var newbie = 0
    var purgeLimit = 50
    var nearbyLimit = 50
    var admin = false
    var channel: Channel? = Channel()
    var hinderTexts = false
    var hinderPhotos = false
    var blockedFromReservoir = false
    var disableProfile = false
    var searchLimit = 100
    var sharing = false
    var locationEnabled: ObservableBoolean = ObservableBoolean(false)
}