package com.example.voicenotes.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.voicenotes.databinding.ActivityLoginBinding
import com.example.voicenotes.utils.SessionManager

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            when {
                email.isEmpty() || password.isEmpty() -> showDialog(
                    "Missing Information",
                    "Please enter both email and password."
                )
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> showDialog(
                    "Invalid Email",
                    "Please enter a valid email address."
                )
                else -> {
                    val name = session.validateLogin(email, password)
                    if (name != null) {
                        session.login(name, email)
                        Toast.makeText(this, "Welcome back, $name!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    } else {
                        showDialog("Login Failed", "Incorrect email or password.")
                    }
                }
            }
        }

        binding.tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun showDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { d, _ -> d.dismiss() }
            .show()
    }
}
