package dev.inacap.tomadelatidosapp1

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 2
        private const val DATABASE_NAME = "MonitorApp.db"

        // Tabla de Usuarios
        private const val TABLE_USER = "user"
        private const val COLUMN_USER_ID = "user_id"
        private const val COLUMN_USER_NAME = "user_name"
        private const val COLUMN_USER_PASSWORD = "user_password"

        // Tabla de Historial Médico (BPM)
        private const val TABLE_BPM = "registro_bpm"
        private const val COLUMN_BPM_ID = "bpm_id"
        private const val COLUMN_BPM_USUARIO = "bpm_usuario"
        private const val COLUMN_BPM_VALOR = "bpm_valor"
        private const val COLUMN_BPM_FECHA = "bpm_fecha"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Crear tabla de usuarios
        val createTableUser = ("CREATE TABLE " + TABLE_USER + "("
                + COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_USER_NAME + " TEXT,"
                + COLUMN_USER_PASSWORD + " TEXT" + ")")
        db.execSQL(createTableUser)

        // Crear tabla de historial de latidos
        val createTableBPM = ("CREATE TABLE " + TABLE_BPM + "("
                + COLUMN_BPM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_BPM_USUARIO + " TEXT,"
                + COLUMN_BPM_VALOR + " INTEGER,"
                + COLUMN_BPM_FECHA + " TEXT" + ")")
        db.execSQL(createTableBPM)

        // Usuario administrador cifrado por defecto (ISO 27400)
        val values = ContentValues()
        values.put(COLUMN_USER_NAME, "admin")
        values.put(COLUMN_USER_PASSWORD, hashSHA256("1234"))
        db.insert(TABLE_USER, null, values)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USER")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BPM")
        onCreate(db)
    }

    // Funciones de Autenticación
    fun checkUser(username: String, password: String): Boolean {
        val db = this.readableDatabase
        val hashedPass = hashSHA256(password)
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USER WHERE $COLUMN_USER_NAME = ? AND $COLUMN_USER_PASSWORD = ?", arrayOf(username, hashedPass))
        val cursorCount = cursor.count
        cursor.close()
        db.close()
        return cursorCount > 0
    }

    // Verifica si un usuario ya existe para no duplicarlo
    fun userExists(username: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USER WHERE $COLUMN_USER_NAME = ?", arrayOf(username))
        val count = cursor.count
        cursor.close()
        db.close()
        return count > 0
    }

    // Registra un nuevo usuario cifrando su contraseña
    fun addUser(username: String, password: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_USER_NAME, username)
        values.put(COLUMN_USER_PASSWORD, hashSHA256(password))
        
        val result = db.insert(TABLE_USER, null, values)
        db.close()
        return result != -1L // Retorna true si se insertó correctamente
    }

    private fun hashSHA256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // Funciones de Gestión de Datos de Salud
    fun guardarLatido(usuario: String, bpm: Int): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()

        // Obtenemos la fecha y hora exacta del registro
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val fechaActual = sdf.format(Date())

        values.put(COLUMN_BPM_USUARIO, usuario)
        values.put(COLUMN_BPM_VALOR, bpm)
        values.put(COLUMN_BPM_FECHA, fechaActual)

        val result = db.insert(TABLE_BPM, null, values)
        db.close()
        return result != -1L
    }
}