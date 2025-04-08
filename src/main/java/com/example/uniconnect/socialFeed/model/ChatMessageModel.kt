package com.example.uniconnect.socialFeed.model

import com.google.firebase.Timestamp

class ChatMessageModel {
    var message: String? = null
    var senderId: String? = null
    var timestamp: Timestamp? = null

    constructor() {}

    constructor(message: String?, senderId: String?, timestamp: Timestamp?) {
        this.message = message
        this.senderId = senderId
        this.timestamp = timestamp
    }


}