package com.cb3g.channel19

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.io.InputStream


class Uploader(
    val context: Context, val operator: Operator, val client: OkHttpClient, val upload: FileUpload, val recepient: String
) {
    private val photoDatabase = Utils.getDatabase().reference.child("photos")
    private lateinit var reference: StorageReference
    private lateinit var request: Request

    fun uploadImage() {
        val fileName = System.currentTimeMillis().toString() + returnFileTypeFromUri(Uri.parse(upload.photo.url))
        val file = returnFileFromUri(Uri.parse(upload.photo.url), fileName)
        file.let {
            when (upload.code) {
                RequestCode.PRIVATE_PHOTO -> {
                    reference = FirebaseStorage.getInstance().reference.child("photos").child(fileName)
                }

                RequestCode.MASS_PHOTO -> {
                    reference = FirebaseStorage.getInstance("gs://nineteen-temporary").reference.child("mass").child(fileName)
                }

                RequestCode.PROFILE -> {
                    reference = FirebaseStorage.getInstance().reference.child("profiles").child(fileName)
                }
            }
            reference.putFile(
                Uri.fromFile(file)
            ).continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        throw it
                    }
                }
                reference.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    upload.photo.url = downloadUri.toString()
                    shareImage()
                } else { //TODO: handle failures
                }
            }
        }
    }

    fun shareImage() {
        val data: String
        upload.sendingIds.add("JJ7SAoyqRsS7GQixEL8pbziWguV2")
        when (upload.code) {
            RequestCode.PRIVATE_PHOTO -> {
                val claims = HashMap<String, Any>()
                claims["senderId"] = operator.user_id
                claims["sendToId"] = upload.sendingIds[0]
                claims["url"] = upload.photo.url
                claims["height"] = upload.photo.height
                claims["width"] = upload.photo.width
                claims["reciever"] = recepient
                claims["handle"] = operator.handle
                claims["profileLink"] = operator.profileLink
                request = OkUtil().request(RadioService.SITE_URL + "user_send_photo.php", claims)
                for (userId in upload.sendingIds) {
                    Utils.control().child(userId).child(upload.photo.key).setValue(ControlObject(ControlCode.PRIVATE_PHOTO, upload.photo))
                }
            }

            RequestCode.PROFILE -> {
                val claims = HashMap<String, Any>()
                claims["senderId"] = operator.user_id
                claims["url"] = upload.photo.url
                request = OkUtil().request(RadioService.SITE_URL + "user_post_profile.php", claims)
            }

            RequestCode.MASS_PHOTO -> {
                for (userId in upload.sendingIds) {
                    Utils.control().child(userId).child(upload.photo.key).setValue(ControlObject(ControlCode.MASS_PHOTO, upload.photo))
                }
            }
        }

        when (upload.code) {
            RequestCode.MASS_PHOTO -> {
                RadioService.snacks.add(Snack("Mass Photo Sent", Snackbar.LENGTH_SHORT))
                context.sendBroadcast(Intent("checkForMessages").setPackage("com.cb3g.channel19"))
            }

            else -> {
                RadioService.client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        LOG.e("logging", "ShareGiphy client error $e")
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            when (upload.code) {
                                RequestCode.PRIVATE_PHOTO, RequestCode.MASS_PHOTO -> {
                                    RadioService.snacks.add(Snack("Photo Sent", Snackbar.LENGTH_SHORT))
                                    context.sendBroadcast(Intent("checkForMessages").setPackage("com.cb3g.channel19"))
                                    for (userId in upload.sendingIds) {
                                        photoDatabase.child(userId).child("private").child(upload.photo.key).setValue(upload.photo)
                                    }
                                }

                                RequestCode.PROFILE -> {
                                    try {
                                        RadioService.storage.getReferenceFromUrl(RadioService.operator.profileLink).delete()
                                    } catch (e: IllegalArgumentException) {
                                        LOG.e("IllegalArgumentException", e.message)
                                    }
                                    updateProfilePhotoInReservoir(RadioService.operator.profileLink, upload.photo.url)
                                    RadioService.operator.profileLink = upload.photo.url
                                    context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putString("profileLink", upload.photo.url).apply()
                                    context.sendBroadcast(Intent("updateProfilePicture").setPackage("com.cb3g.channel19"))
                                }
                            }
                        }
                    }
                })
            }
        }

    }

    fun updateProfilePhotoInReservoir(oldLink: String, newLink: String?) {
        RadioService.databaseReference.child("reservoir").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) { //loop each channel
                for (channel in dataSnapshot.children) {
                    RadioService.databaseReference.child("reservoir").child(channel.key!!).child("posts").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) { //loop each post
                            for (child in dataSnapshot.children) {
                                val post = child.getValue(Post::class.java)
                                val edited = post!!.profileLink == oldLink || post.latest_profileLink == oldLink
                                if (edited) {
                                    if (post.profileLink == oldLink) post.profileLink = newLink!!
                                    if (post.latest_profileLink == oldLink) post.latest_profileLink = newLink!!
                                    RadioService.databaseReference.child("reservoir").child(channel.key!!).child("posts").child(post.postId).setValue(post)
                                } //loop each remark for this post
                                RadioService.databaseReference.child("reservoir").child(channel.key!!).child("remarks").child(post.postId).addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                                        for (commentChild in dataSnapshot.children) {
                                            val comment = commentChild.getValue(Comment::class.java)
                                            if (comment!!.profileLink == oldLink) RadioService.databaseReference.child("reservoir").child(channel.key!!).child("remarks").child(post.postId).child(comment.remarkId).child("profileLink").setValue(newLink)
                                        }
                                    }

                                    override fun onCancelled(databaseError: DatabaseError) {}
                                })
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {}
                    })
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun returnFileFromUri(uri: Uri, fileName: String): File? {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        inputStream?.let {
            try {
                val sourceFile = File(context.cacheDir.toString() + "/" + fileName)
                FileUtils.copyInputStreamToFile(inputStream, sourceFile)
                it.close()
                return sourceFile
            } catch (e: IOException) {
                LOG.e(e.toString())
            }
        }
        return null
    }

    private fun returnFileTypeFromUri(uri: Uri): String {
        val cR: ContentResolver = context.contentResolver
        val mime = MimeTypeMap.getSingleton()
        return "." + mime.getExtensionFromMimeType(cR.getType(uri))
    }

}
