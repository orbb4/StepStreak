package com.example.stepstreak.data.repository

import com.example.stepstreak.data.model.Friendship
import com.example.stepstreak.data.model.VisitedCell
import com.example.stepstreak.data.model.WalkSession

import com.google.firebase.database.FirebaseDatabase

fun addFriend(uid1: String, uid2: String) {
    val key = "${uid1}_${uid2}"

    val friendship = Friendship(
        friend1 = uid1,
        friend2 = uid2,
        status = "pending"
    )

    val db = FirebaseDatabase.getInstance().reference

    val updates = hashMapOf<String, Any>(
        "friendships/$key" to friendship,
        "userFriends/$uid1/$uid2" to "pending",
        "userFriends/$uid2/$uid1" to "pending"
    )

    db.updateChildren(updates)
}


fun createWalkSession(uid: String) {
    val db = FirebaseDatabase.getInstance().reference
    val sessionKey = db.child("walkSessions").push().key!!

    val session = WalkSession(user_id = uid)

    val updates = hashMapOf<String, Any>(
        "walkSessions/$sessionKey" to session,
        "userSessions/$uid/$sessionKey" to true
    )

    db.updateChildren(updates)
}


fun addVisitedCell(sessionId: String, lat: Int, lon: Int) {
    val db = FirebaseDatabase.getInstance().reference
    val cellKey = db.child("visitedCells").push().key!!

    val cell = VisitedCell(
        walk_session_id = sessionId,
        lat = lat,
        lon = lon
    )

    val updates = hashMapOf<String, Any>(
        "visitedCells/$cellKey" to cell,
        "sessionCells/$sessionId/$cellKey" to true
    )

    db.updateChildren(updates)
}


