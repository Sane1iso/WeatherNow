package com.sanelisolehlohla.weathernow

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class EditProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private var profileImageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 71

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()

        val user = auth.currentUser
        val nameEditText = findViewById<EditText>(R.id.et_edit_name)
        val surnameEditText = findViewById<EditText>(R.id.et_edit_surname)
        val currentPasswordEditText = findViewById<EditText>(R.id.et_edit_current_password)
        val newPasswordEditText = findViewById<EditText>(R.id.et_edit_new_password)
        val profileImageView = findViewById<ImageView>(R.id.iv_profile_picture)

        user?.let {
            val displayName = it.displayName?.split(" ")
            if (displayName != null && displayName.size >= 2) {
                nameEditText.setText(displayName[0])
                surnameEditText.setText(displayName[1])
            }
            it.photoUrl?.let { uri ->
                profileImageView.setImageURI(uri)
                profileImageUri = uri
            }
        }

        findViewById<Button>(R.id.btn_change_picture).setOnClickListener {
            chooseImageFromGallery()
        }

        findViewById<Button>(R.id.btn_remove_picture).setOnClickListener {
            profileImageView.setImageResource(R.drawable.ic_profile_placeholder)
            profileImageUri = null
            user?.updateProfile(UserProfileChangeRequest.Builder().setPhotoUri(null).build())
        }

        findViewById<Button>(R.id.btn_save_profile).setOnClickListener {
            val newName = nameEditText.text.toString()
            val newSurname = surnameEditText.text.toString()
            val currentPassword = currentPasswordEditText.text.toString()
            val newPassword = newPasswordEditText.text.toString()

            if (newName.isNotEmpty() && newSurname.isNotEmpty()) {
                val profileUpdatesBuilder = UserProfileChangeRequest.Builder()
                    .setDisplayName("$newName $newSurname")

                profileImageUri?.let { uri ->
                    profileUpdatesBuilder.setPhotoUri(uri)
                }

                val profileUpdates = profileUpdatesBuilder.build()

                user?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        if (profileImageUri != null) {
                            uploadProfilePictureToFirebase()
                        } else {
                            Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Failed to update profile. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }

                if (currentPassword.isNotEmpty() && newPassword.isNotEmpty()) {
                    updatePassword(currentPassword, newPassword)
                } else if (currentPassword.isEmpty() && newPassword.isNotEmpty()) {
                    Toast.makeText(this, "Please enter your current password to update it.", Toast.LENGTH_SHORT).show()
                }

            } else {
                Toast.makeText(this, "Please complete both name and surname fields.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun chooseImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            profileImageUri = data.data!!
            val profileImageView = findViewById<ImageView>(R.id.iv_profile_picture)
            profileImageView.setImageURI(profileImageUri)
            Toast.makeText(this, "Profile picture selected successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadProfilePictureToFirebase() {
        val storageRef = storage.reference.child("profile_pictures/${UUID.randomUUID()}.jpg")
        profileImageUri?.let { uri ->
            storageRef.putFile(uri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        val user = auth.currentUser
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setPhotoUri(downloadUri)
                            .build()

                        user?.updateProfile(profileUpdates)
                            ?.addOnCompleteListener {
                                Toast.makeText(this, "Profile picture uploaded and updated successfully!", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to upload profile picture. Please try again.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updatePassword(currentPassword: String, newPassword: String) {
        val user = auth.currentUser
        user?.let {
            val email = it.email

            if (email != null) {
                val credential = EmailAuthProvider.getCredential(email, currentPassword)

                it.reauthenticate(credential)
                    .addOnCompleteListener { reauthTask ->
                        if (reauthTask.isSuccessful) {
                            user.updatePassword(newPassword)
                                .addOnCompleteListener { updateTask ->
                                    if (updateTask.isSuccessful) {
                                        Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(this, "Failed to update password. Please try again.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        } else {
                            Toast.makeText(this, "Re-authentication failed. Please check your current password.", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }
}
