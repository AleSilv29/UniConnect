package com.example.uniconnect.socialFeed.model

import com.google.firebase.Timestamp;

class UserModel {
    var Email: String? = null
    var Group: String? = null
    var Year: String? = null
    private var createdTimestamp: Timestamp? = null
    var userId: String? = null
    var fcmToken: String? = null

    constructor()

    constructor(Email: String?, Group: String?, Year: String?, createdTimestamp: Timestamp?, userId: String?, fcmToken: String?) {
        this.Email = Email
        this.Group = Group
        this.Year = Year
        this.createdTimestamp = createdTimestamp
        this.userId = userId
        this.fcmToken = fcmToken
    }

}