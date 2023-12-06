package com.cb3g.channel19

import java.util.ArrayList

class PollOption {
    var label = ""
    var votes = ArrayList<String>()


    constructor(label: String, votes: ArrayList<String>) {
        this.label = label
        this.votes = votes
    }

    constructor()
}
