package com.example.uniconnect.socialFeed.model

import com.google.firebase.Timestamp

class ChatroomModel {
    var chatroomId: String? = null
    var userIds: List<String>? = null
    var lastMessageTimestamp: Timestamp? = null
    var lastMessageSenderId: String? = null
    var lastMessage: String? = null

    constructor() {}

    constructor(
        chatroomId: String?,
        userIds: List<String>,
        lastMessageTimestamp: Timestamp?,
        lastMessageSenderId: String?,
    ) {
        this.chatroomId = chatroomId
        this.userIds = userIds
        this.lastMessageTimestamp = lastMessageTimestamp
        this.lastMessageSenderId = lastMessageSenderId
    }
}