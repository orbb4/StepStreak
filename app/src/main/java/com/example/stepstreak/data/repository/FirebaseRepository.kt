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

fun sendFriendRequest(
    username: String,
    onResult: (Boolean, String) -> Unit // success, message
) {
    val currentUser = FirebaseAuth.getInstance().currentUser ?: return onResult(false, "No autenticado")
    val db = Firebase.database

    val usernamesRef = db.getReference("usernames").child(username)

    usernamesRef.get().addOnSuccessListener { snapshot ->
        if (!snapshot.exists()) {
            onResult(false, "Usuario no encontrado")
            return@addOnSuccessListener
        }

        val friendUid = snapshot.value.toString()
        val currentUid = currentUser.uid
        val friendshipId = listOf(currentUid, friendUid).sorted().joinToString("_")
        val friendshipRef = db.getReference("friendships").child(friendshipId)

        friendshipRef.get().addOnSuccessListener { friendshipSnap ->
            if (friendshipSnap.exists()) {
                onResult(false, "Ya existe una solicitud o amistad")
                return@addOnSuccessListener
            }

            val friendship = Friendship(
                sender = currentUid,
                receiver = friendUid,
                status = "pending"
            )

            friendshipRef.setValue(friendship)
                .addOnSuccessListener { onResult(true, "Solicitud enviada") }
                .addOnFailureListener { onResult(false, "Error al enviar solicitud") }
        }
    }
}



fun rejectFriendRequest(friendUid: String) {
    val currentUser = FirebaseAuth.getInstance().currentUser ?: return
    val db = Firebase.database

    val ref1 = db.getReference("friendships")
        .child(currentUser.uid)
        .child(friendUid)

    val ref2 = db.getReference("friendships")
        .child(friendUid)
        .child(currentUser.uid)

    // Primero verificamos que la solicitud existe
    ref1.get().addOnSuccessListener { snapshot ->
        if (!snapshot.exists()) {
            Log.d("Friends", "No existe solicitud para rechazar")
            return@addOnSuccessListener
        }

        // Borrar ambos lados
        ref1.removeValue()
        ref2.removeValue()
            .addOnSuccessListener {
                Log.d("Friends", "Solicitud rechazada")
            }
            .addOnFailureListener {
                Log.d("Friends", "Error al rechazar solicitud")
            }
    }
}


fun acceptFriendRequest(friendUid: String) {
    val currentUser = FirebaseAuth.getInstance().currentUser ?: return
    val db = Firebase.database

    val friendshipId = listOf(currentUser.uid, friendUid).sorted().joinToString("_")
    val friendshipRef = db.getReference("friendships").child(friendshipId)

    friendshipRef.child("status").setValue("accepted")
        .addOnSuccessListener {
            Log.d("Friends", "Solicitud aceptada")
        }
        .addOnFailureListener {
            Log.d("Friends", "Error al aceptar la solicitud")
        }
}



fun loadReceivedRequests() {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val db = Firebase.database

    db.getReference("friendships").get().addOnSuccessListener { snapshot ->
        val requests = snapshot.children.mapNotNull { snap ->
            val friendship = snap.getValue(Friendship::class.java)
            if (friendship != null && friendship.receiver == uid && friendship.status == "pending") {
                friendship
            } else null
        }

        Log.d("Friends", "Solicitudes recibidas: $requests")
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


