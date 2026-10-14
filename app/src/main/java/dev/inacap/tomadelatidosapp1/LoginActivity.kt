package dev.inacap.tomadelatidosapp1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializamos la base de datos
        dbHelper = DatabaseHelper(this)

        // IMPORTANTE: Asegúrate de que los IDs coincidan con tu activity_login.xml
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<Button>(R.id.btnRegister) // Nuevo botón de registro

        // Lógica de Inicio de Sesión
        btnLogin.setOnClickListener {
            val user = etUsername.text.toString().trim()
            val pass = etPassword.text.toString().trim()

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, getString(R.string.msg_complete_fields), Toast.LENGTH_SHORT).show()
            } else {
                if (dbHelper.checkUser(user, pass)) {
                    Toast.makeText(this, getString(R.string.msg_login_success), Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, getString(R.string.msg_login_error), Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Lógica de Registro
        btnRegister.setOnClickListener {
            val user = etUsername.text.toString().trim()
            val pass = etPassword.text.toString().trim()

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, getString(R.string.msg_complete_fields), Toast.LENGTH_SHORT).show()
            } else {
                // Verificar si el usuario ya existe
                if (dbHelper.userExists(user)) {
                    Toast.makeText(this, getString(R.string.msg_user_exists), Toast.LENGTH_SHORT).show()
                } else {
                    // Si no existe, lo registramos
                    val isRegistered = dbHelper.addUser(user, pass)
                    if (isRegistered) {
                        Toast.makeText(this, getString(R.string.msg_register_success), Toast.LENGTH_SHORT).show()
                        // Opcional: limpiar los campos tras un registro exitoso o dejar que haga login
                    } else {
                        Toast.makeText(this, getString(R.string.msg_register_error), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}