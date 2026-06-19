package com.example.voicenotes.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.voicenotes.databinding.ActivityRegisterBinding
import com.example.voicenotes.utils.SessionManager

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)

        binding.btnRegister.setOnClickListener {
            val name     = binding.etFullName.text.toString().trim()
            val email    = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirm  = binding.etConfirmPassword.text.toString()

            when {
                name.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty() ->
                    showDialog("Missing Information", "Please fill in all fields.")
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                    showDialog("Invalid Email", "Please enter a valid email address.")
                password.length < 6 ->
                    showDialog("Weak Password", "Password must be at least 6 characters.")
                password != confirm ->
                    showDialog("Password Mismatch", "Passwords do not match.")
                session.emailExists(email) ->
                    showDialog("Email Taken", "An account with this email already exists.")
                else -> {
                    session.registerUser(name, email, password)
                    session.login(name, email)
                    Toast.makeText(this, "Account created! Welcome, $name!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                }
            }
        }

        binding.tvGoLogin.setOnClickListener { finish() }
    }

    private fun showDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { d, _ -> d.dismiss() }
            .show()
    }
}
