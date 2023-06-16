package com.cb3g.channel19

class Coordinates {
    var userId = ""
    var handle = ""
    var profile = ""
    var latitude = 0.0
    var longitude = 0.0
    var bearing = 0f
    var speed = 0f
    var alititude = 0.0

    constructor()

    constructor(userId: String, handle: String, profile: String, latitude: Double, longitude: Double, bearing: Float, speed: Float, altitude: Double) {
        this.userId = userId
        this.handle = handle
        this.profile = profile
        this.latitude = latitude
        this.longitude = longitude
        this.bearing = bearing
        this.speed = speed
        this.alititude = altitude
    }


}
