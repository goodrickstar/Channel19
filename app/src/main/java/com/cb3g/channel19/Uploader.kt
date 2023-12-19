package com.cb3g.channel19

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
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
    val context: Context,
    val operator: User,
    val client: OkHttpClient,
    val upload: FileUpload,
) {
    private lateinit var reference: StorageReference
    private lateinit var request: Request

    fun uploadImage() {
        val fileName = System.currentTimeMillis().toString() + returnFileTypeFromUri(Uri.parse(upload.getUri()))
        val file = returnFileFromUri(Uri.parse(upload.getUri()), fileName)
        file.let {
            when (upload.getCode()) {
                RequestCode.PRIVATE_PHOTO -> {
                    reference = FirebaseStorage.getInstance().reference.child("photos").child(fileName)
                }

                RequestCode.MASS_PHOTO -> {
                    reference = FirebaseStorage.getInstance("gs://nineteen-temporary").reference.child("mass").child(fileName)
                }

                RequestCode.PROFILE -> {
                    Log.i("logging", "refernece set")
                    reference = FirebaseStorage.getInstance().reference.child("profiles").child(fileName)
                }

                else -> {}
            }
            reference.putFile(
                Uri.fromFile(file)).continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        throw it
                    }
                }
                reference.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    upload.setUri(downloadUri.toString())
                    shareImage()
                } else {
                    //TODO: handle failures
                }
            }
        }
    }

    fun shareImage() {
        val data: String
        when (upload.code) {
            RequestCode.PRIVATE_PHOTO -> {
                data = Jwts.builder().setHeader(RadioService.header).claim("senderId", operator.user_id).claim("sendToId", upload.sendToId).claim("handle", operator.handle).claim("caption", upload.caption).claim("reciever", upload.sendToHandle).claim("silenced", operator.silenced.toString()).claim("url", upload.uri).claim("height", upload.height).claim("width", upload.width).claim("profileLink", operator.profileLink).signWith(SignatureAlgorithm.HS256, operator.key).compact()
                request = Request.Builder().url(RadioService.SITE_URL + "user_send_photo.php").post(FormBody.Builder().add("data", data).build()).build()
            }

            RequestCode.MASS_PHOTO -> {
                data = Jwts.builder().setHeader(RadioService.header).claim("userId", operator.user_id).claim("userIds", upload.sendToId).claim("handle", operator.handle).claim("silenced", operator.silenced.toString()).claim("url", upload.uri).claim("height", upload.height).claim("width", upload.width).claim("profileLink", operator.profileLink).signWith(SignatureAlgorithm.HS256, operator.key).compact()
                request = Request.Builder().url(RadioService.SITE_URL + "user_mass_photo.php").post(FormBody.Builder().add("data", data).build()).build()
            }

            RequestCode.PROFILE-> {
                Log.i("logging", "updating profile photo on server")
                data = Jwts.builder().setHeader(RadioService.header).claim("userId", operator.user_id).claim("url", upload.uri).signWith(SignatureAlgorithm.HS256, operator.key).compact()
                request = Request.Builder().url(RadioService.SITE_URL + "user_post_profile.php").post(FormBody.Builder().add("data", data).build()).build()
            }

            else -> {}
        }
        RadioService.client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                LOG.e("logging", "ShareGiphy client error $e")
            }

            override fun onResponse(call: Call, response: Response) {
                Log.i("logging", "onResponse")
                if (response.isSuccessful) {
                    Log.i("logging", "response.isSuccessful")
                    when (upload.code) {
                        RequestCode.PRIVATE_PHOTO, RequestCode.MASS_PHOTO -> {
                            RadioService.snacks.add(Snack("Photo Sent", Snackbar.LENGTH_SHORT))
                            context.sendBroadcast(Intent("checkForMessages").setPackage("com.cb3g.channel19"))
                        }

                        RequestCode.PROFILE -> {
                            Log.i("logging", "PROFILE_SELECTED_FROM_DISK")
                            try {
                                RadioService.storage.getReferenceFromUrl(RadioService.operator.profileLink).delete()
                            } catch (e: IllegalArgumentException) {
                                LOG.e("IllegalArgumentException", e.message)
                            }
                            updateProfilePhotoInReservoir(RadioService.operator.profileLink, upload.uri)
                            RadioService.operator.profileLink = upload.uri
                            context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putString("profileLink", upload.uri).apply()
                            context.sendBroadcast(Intent("updateProfilePicture").setPackage("com.cb3g.channel19"))
                        }

                        else -> {}
                    }
                }
            }
        })
    }

    fun updateProfilePhotoInReservoir(oldLink: String, newLink: String?) {
        RadioService.databaseReference.child("reservoir").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                //loop each channel
                for (channel in dataSnapshot.children) {
                    RadioService.databaseReference.child("reservoir").child(channel.key!!).child("posts").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            //loop each post
                            for (child in dataSnapshot.children) {
                                val post = child.getValue(Post::class.java)
                                val edited = post!!.profileLink == oldLink || post.latest_profileLink == oldLink
                                if (edited) {
                                    if (post.profileLink == oldLink) post.profileLink = newLink!!
                                    if (post.latest_profileLink == oldLink) post.latest_profileLink = newLink!!
                                    RadioService.databaseReference.child("reservoir").child(channel.key!!).child("posts").child(post.postId).setValue(post)
                                }
                                //loop each remark for this post
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
