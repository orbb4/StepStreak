package com.example.stepstreak.data.repository

import android.util.Log
import com.example.stepstreak.data.model.Friendship
import com.example.stepstreak.data.model.VisitedCell
import com.example.stepstreak.data.model.WalkSession
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database


val usernamesRef = Firebase.database.getReference("usernames")

fun searchUser(username: String, onResult: (String?) -> Unit) {
    usernamesRef.child(username).get()
        .addOnSuccessListener { snapshot ->
            val uidEncontrado = snapshot.getValue(String::class.java)
            onResult(uidEncontrado)   // puede ser null
        }
}

fun sendFriendRequest(username: String) {
    val currentUser = FirebaseAuth.getInstance().currentUser ?: return
    val db = Firebase.database

    // Buscar usuario por username
    val usersRef = db.getReference("usernames").child(username)

    usersRef.get().addOnSuccessListener { snapshot ->
        if (!snapshot.exists()) {
            Log.d("Friends", "Usuario no encontrado")
            return@addOnSuccessListener
        }

        val friendUid = snapshot.value.toString()

        val friendRequestRef = db.getReference("friend_requests")
            .child(friendUid)
            .child(currentUser.uid)

        friendRequestRef.setValue(true)
            .addOnSuccessListener {
                Log.d("Friends", "Solicitud enviada")
            }
            .addOnFailureListener {
                Log.d("Friends", "Error al enviar solicitud")
            }
    }
}



fun addFriend(currentUid: String, targetUid: String) {
    val friendsRef = Firebase.database.getReference("friends")

    // relación doble
    val updates = mapOf(
        "$currentUid/$targetUid" to true,
        "$targetUid/$currentUid" to true
    )

    friendsRef.updateChildren(updates)
        .addOnSuccessListener {
            Log.d("Friends", "Ahora son amigos")
        }
        .addOnFailureListener {
            Log.e("Friends", "Error al agregar amigo", it)
        }
}


fun getFriends(uid: String, onResult: (List<String>) -> Unit) {
    val friendsRef = Firebase.database.getReference("friends").child(uid)

    friendsRef.get().addOnSuccessListener { snapshot ->
        val lista = snapshot.children.map { it.key!! }
        onResult(lista)
    }
}

fun getUsername(uid: String, onResult: (String?) -> Unit) {
    val usersRef = Firebase.database.getReference("users").child(uid).child("username")
    usersRef.get().addOnSuccessListener { snapshot ->
        onResult(snapshot.getValue(String::class.java))
    }
}



/*
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
*/

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


