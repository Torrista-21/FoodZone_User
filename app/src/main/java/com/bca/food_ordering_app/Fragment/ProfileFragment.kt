package com.bca.food_ordering_app.Fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

import com.bca.food_ordering_app.databinding.FragmentProfileBinding
import com.bca.food_ordering_app.model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener



class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private lateinit var userId : String
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        userId = auth.currentUser?.uid?:""
        binding = FragmentProfileBinding.inflate(inflater,container,false)

        setUserId()

        binding.apply {
             profileName.isEnabled = false
            profileEmail.isEnabled = false
            profilePhone.isEnabled = false
            profileAddress.isEnabled = false

        binding.editProfile.setOnClickListener {

            profileName.isEnabled = !profileName.isEnabled
            profileEmail.isEnabled = !profileEmail.isEnabled
            profileAddress.isEnabled = !profileAddress.isEnabled
            profilePhone.isEnabled = !profilePhone.isEnabled

        }
        }

        binding.saveInformation.setOnClickListener {
            val name = binding.profileName.text.toString()
            val email = binding.profileEmail.text.toString()
            val address= binding.profileAddress.text.toString()
            val phone = binding.profilePhone.text.toString()
            updateUi(name,email,address,phone)
        }
        return binding.root
    }

    private fun updateUi(
        name: String,
        email: String,
        address: String,
        phone: String
    ) {
        val userId =  auth.currentUser?.uid
        if(userId != null){
            val userReference = database.getReference("UsersInformation").child(userId)
            val userData = hashMapOf(
                "name" to name,
                "address" to address,
                "email" to email,
                "phone" to phone,
            )
            userReference.setValue(userData).addOnSuccessListener {
                Toast.makeText(requireContext(), "Information Updated Successfully", Toast.LENGTH_SHORT).show()
            }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Updating Information Failed", Toast.LENGTH_SHORT).show()
                }


        }
    }

    private fun setUserId() {

        if(userId.isNotEmpty()){
            val userReference = database.getReference("UsersInformation").child(userId)
                userReference.addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if(snapshot.exists()){
                            val userProfile = snapshot.getValue(UserModel::class.java)
                            if(userProfile != null){
                                binding.profileName.setText(userProfile.name)
                                binding.profileAddress.setText(userProfile.address)
                                binding.profilePhone.setText(userProfile.phone)
                                binding.profileEmail.setText(userProfile.email)
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }

                })
            }

    }


}