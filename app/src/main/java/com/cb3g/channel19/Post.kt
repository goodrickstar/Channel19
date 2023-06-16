package com.cb3g.channel19

class Post {
    constructor(type: Int) {
        this.type = type
    }

    constructor()


    var postId = "none"
    var stamp = Utils.UTC()
    var locked = false
    var type = 1
    var pinned = false

    var likes = 0
    var dislikes = 0

    var imageLink = "none"
    var image_height = 0
    var image_width = 0

    var caption = "none"

    var facebookId = "none"
    var profileLink = "none"
    var handle = "none"

    var remarks = 0
    var latest_facebookId = "none"
    var latest_profileLink = "none"
    var latest_handle = "none"
    var latest_remark = "none"

    var webLink = "none"
    var webDescription = "none"
    var options : List<PollOption> = ArrayList()

}
