package com.example.uniformecircular

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "usuario.db"
        private const val DATABASE_VERSION = 18
        
        // Tabla Usuarios
        const val TABLE_USUARIO = "usuarios"
        const val COL_ID = "id"
        const val COL_USUARIO = "usuario"
        const val COL_PASSWORD = "password"
        const val COL_TELEFONO = "telefono"
        const val COL_PUNTOS = "puntos"

        // Tabla Prendas
        const val TABLE_PRENDAS = "prendas"
        const val COL_PRENDA_ID = "id_prenda"
        const val COL_PRENDA_ID_USUARIO = "id_usuario"
        const val COL_PRENDA_TITULO = "titulo"
        const val COL_PRENDA_DESCRIPCION = "descripcion"
        const val COL_PRENDA_CARRERA = "carrera"
        const val COL_PRENDA_TALLA = "talla"
        const val COL_PRENDA_GENERO = "genero"
        const val COL_PRENDA_TIPO = "tipo_transaccion"
        const val COL_PRENDA_PUNTOS = "precio_puntos"
        const val COL_PRENDA_IMAGEN = "imagen_uri"
        const val COL_PRENDA_ESTADO = "estado" // "DISPONIBLE", "EN PROCESO" o "CANJEADO"
        const val COL_PRENDA_ESTADO_FISICO = "estado_fisico" // "Nuevo", "Seminuevo", "Usado"
        const val COL_PRENDA_ID_RECEPTOR = "id_receptor"
        const val COL_PRENDA_OBSERVACION = "observacion"
    }
    // Habilitar foreign keys
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
        db.enableWriteAheadLogging()
    }
    // Creamos la base de datos
    override fun onCreate(db: SQLiteDatabase) {
        val createUsuariosTable = """
           CREATE TABLE $TABLE_USUARIO(
           $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
           $COL_USUARIO TEXT UNIQUE,
           $COL_PASSWORD TEXT,
           $COL_TELEFONO TEXT,
           $COL_PUNTOS INTEGER DEFAULT 100 --Bono de bienvenida al registrarse por primeva vez.
           )
       """.trimIndent()
        // Tabla de Prendas
        val createPrendasTable = """
            CREATE TABLE $TABLE_PRENDAS(
            $COL_PRENDA_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_PRENDA_ID_USUARIO INTEGER,
            $COL_PRENDA_TITULO TEXT,
            $COL_PRENDA_DESCRIPCION TEXT,
            $COL_PRENDA_CARRERA TEXT,
            $COL_PRENDA_TALLA TEXT,
            $COL_PRENDA_GENERO TEXT,
            $COL_PRENDA_TIPO TEXT,
            $COL_PRENDA_PUNTOS INTEGER,
            $COL_PRENDA_IMAGEN TEXT,
            $COL_PRENDA_ESTADO TEXT DEFAULT 'DISPONIBLE',
            $COL_PRENDA_ESTADO_FISICO TEXT DEFAULT 'Usado',
            $COL_PRENDA_ID_RECEPTOR INTEGER,
            $COL_PRENDA_OBSERVACION TEXT,
            FOREIGN KEY($COL_PRENDA_ID_USUARIO) REFERENCES $TABLE_USUARIO($COL_ID) ON DELETE CASCADE,
            FOREIGN KEY($COL_PRENDA_ID_RECEPTOR) REFERENCES $TABLE_USUARIO($COL_ID) ON DELETE SET NULL
            )
        """.trimIndent()
        // Crear las tablas
        db.execSQL(createUsuariosTable)
        db.execSQL(createPrendasTable)
    }
    // Actualizamos la versión de la base de datos
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 16) {
            try {
                db.execSQL("ALTER TABLE $TABLE_PRENDAS ADD COLUMN $COL_PRENDA_GENERO TEXT DEFAULT 'Unisex'")
            } catch (e: Exception) { /* Ignorar si ya existe */ }
        }
        if (oldVersion < 17) {
            try {
                db.execSQL("ALTER TABLE $TABLE_PRENDAS ADD COLUMN $COL_PRENDA_ESTADO_FISICO TEXT DEFAULT 'Usado'")
            } catch (e: Exception) { /* Ignorar si ya existe */ }
        }
        if (oldVersion < 18) {
            try {
                db.execSQL("ALTER TABLE $TABLE_PRENDAS ADD COLUMN $COL_PRENDA_OBSERVACION TEXT")
            } catch (e: Exception) { /* Ignorar si ya existe */ }
        }
    }

    // --- MÉTODOS DE USUARIO ---
    fun insertarUsuario(usuario: String, password: String, telefono: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_USUARIO, usuario)
            put(COL_PASSWORD, password)
            put(COL_TELEFONO, telefono)
            // MEJORA 4: El administrador inicia con 0 puntos, los usuarios con el bono de 100.
            put(COL_PUNTOS, if (usuario.trim().lowercase() == "admin") 0 else 100)
        }
        val resultado = db.insert(TABLE_USUARIO, null, values)
        return resultado != -1L
    }
    // Verificamos si el usuario existe en la base de datos
    fun verificarUsuario(usuario: String, password: String): Boolean {
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_USUARIO WHERE $COL_USUARIO = ? AND $COL_PASSWORD = ?"
        val cursor = db.rawQuery(query, arrayOf(usuario, password))
        val existe = cursor.count > 0
        cursor.close()
        return existe
    }
    // Verificamos si el usuario existe en la base de datos
    fun existeUsuario(usuario: String): Boolean {
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_USUARIO WHERE $COL_USUARIO = ?"
        val cursor = db.rawQuery(query, arrayOf(usuario))
        val existe = cursor.count > 0
        cursor.close()
        return existe
    }

    // Recuperar contraseña validando usuario y teléfono
    fun recuperarPassword(usuario: String, telefono: String): String? {
        val db = readableDatabase
        val query = "SELECT $COL_PASSWORD FROM $TABLE_USUARIO WHERE $COL_USUARIO = ? AND $COL_TELEFONO = ?"
        val cursor = db.rawQuery(query, arrayOf(usuario, telefono))
        var password: String? = null
        if (cursor.moveToFirst()) {
            password = cursor.getString(0)
        }
        cursor.close()
        return password
    }

    // Obtenemos el ID del usuario por su nombre de usuario
    fun obtenerUsuarioId(usuario: String): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT $COL_ID FROM $TABLE_USUARIO WHERE $COL_USUARIO = ?", arrayOf(usuario))
        var id = -1
        if (cursor.moveToFirst()) {
            id = cursor.getInt(0)
        }
        cursor.close()
        return id
    }
    // Obtenemos el nombre del usuario por su ID
    fun obtenerNombreUsuario(idUsuario: Int): String? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT $COL_USUARIO FROM $TABLE_USUARIO WHERE $COL_ID = ?", arrayOf(idUsuario.toString()))
        var nombre: String? = null
        if (cursor.moveToFirst()) {
            nombre = cursor.getString(0)
        }
        cursor.close()
        return nombre
    }

    // Obtenemos el teléfono del usuario por su ID
    fun obtenerTelefonoUsuario(idUsuario: Int): String? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT $COL_TELEFONO FROM $TABLE_USUARIO WHERE $COL_ID = ?", arrayOf(idUsuario.toString()))
        var telefono: String? = null
        if (cursor.moveToFirst()) {
            telefono = cursor.getString(0)
        }
        cursor.close()
        return telefono
    }
    // Actualizamos el teléfono del usuario por su nombre de usuario
    fun actualizarTelefonoUsuario(usuario: String, nuevoTelefono: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TELEFONO, nuevoTelefono)
        }
        val result = db.update(TABLE_USUARIO, values, "$COL_USUARIO = ?", arrayOf(usuario))
        return result > 0
    }

    // Obtener los puntos de un usuario
    fun obtenerPuntosUsuario(idUsuario: Int): Int {
        val db = readableDatabase
        val cursor = db.query(TABLE_USUARIO, arrayOf(COL_PUNTOS), "$COL_ID = ?", arrayOf(idUsuario.toString()), null, null, null)
        var puntos = 0
        if (cursor.moveToFirst()) {
            puntos = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PUNTOS))
        }
        cursor.close()
        return puntos
    }

    fun obtenerConteoAportes(idUsuario: Int): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_PRENDAS WHERE $COL_PRENDA_ID_USUARIO = ?", arrayOf(idUsuario.toString()))
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count
    }

    // Actualizar puntos de un usuario
    fun sumarPuntosUsuario(idUsuario: Int, puntosASumar: Int): Boolean {
        val puntosActuales = obtenerPuntosUsuario(idUsuario)
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_PUNTOS, puntosActuales + puntosASumar)
        }
        val result = db.update(TABLE_USUARIO, values, "$COL_ID = ?", arrayOf(idUsuario.toString()))
        return result > 0
    }

    fun restarPuntosUsuario(idUsuario: Int, puntosARestar: Int): Boolean {
        val puntosActuales = obtenerPuntosUsuario(idUsuario)
        val nuevoBalance = if (puntosActuales >= puntosARestar) puntosActuales - puntosARestar else 0
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_PUNTOS, nuevoBalance)
        }
        val result = db.update(TABLE_USUARIO, values, "$COL_ID = ?", arrayOf(idUsuario.toString()))
        return result > 0
    }

    // Insertamos una prenda en la base de datos
    fun insertarPrenda(idUsuario: Int, titulo: String, desc: String, carrera: String, talla: String, genero: String, tipo: String, puntos: Int, imagen: String?, estadoFisico: String = "Usado"): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_PRENDA_ID_USUARIO, idUsuario)
            put(COL_PRENDA_TITULO, titulo)
            put(COL_PRENDA_DESCRIPCION, desc)
            put(COL_PRENDA_CARRERA, carrera)
            put(COL_PRENDA_TALLA, talla)
            put(COL_PRENDA_GENERO, genero)
            put(COL_PRENDA_TIPO, tipo)
            put(COL_PRENDA_PUNTOS, puntos)
            put(COL_PRENDA_IMAGEN, imagen)
            put(COL_PRENDA_ESTADO_FISICO, estadoFisico)
        }
        val result = db.insert(TABLE_PRENDAS, null, values)
        return result != -1L
    }
    // Obtenemos el catálogo de prendas desde la base de datos
    fun obtenerCatalogo(): Cursor {
        val db = readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_PRENDAS ORDER BY $COL_PRENDA_ID DESC", null)
    }

    fun obtenerPrendasPorUsuario(idUsuario: Int): List<Prenda> {
        val db = readableDatabase
        val cursor = db.query(TABLE_PRENDAS, null, "$COL_PRENDA_ID_USUARIO = ?", arrayOf(idUsuario.toString()), null, null, "$COL_PRENDA_ID DESC")
        val lista = mutableListOf<Prenda>()
        if (cursor.moveToFirst()) {
            do {
                lista.add(Prenda(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COL_PRENDA_ID)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COL_PRENDA_ID_USUARIO)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_PRENDA_TITULO)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_PRENDA_DESCRIPCION)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_PRENDA_CARRERA)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_PRENDA_TALLA)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_PRENDA_GENERO)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_PRENDA_TIPO)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COL_PRENDA_PUNTOS)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_PRENDA_IMAGEN)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_PRENDA_ESTADO)),
                    if (cursor.isNull(cursor.getColumnIndexOrThrow(COL_PRENDA_ID_RECEPTOR))) null
                    else cursor.getInt(cursor.getColumnIndexOrThrow(COL_PRENDA_ID_RECEPTOR)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_PRENDA_ESTADO_FISICO))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    // Vacíamos el catálogo de prendas
    fun vaciarCatalogo() {
        val db = writableDatabase
        db.execSQL("DELETE FROM $TABLE_PRENDAS")
    }

    // Marcar una prenda como CANJEADA (asignando un receptor)
    fun marcarComoCanjeada(idPrenda: Int, idReceptor: Int? = null): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_PRENDA_ESTADO, "CANJEADO")
            if (idReceptor != null) {
                put(COL_PRENDA_ID_RECEPTOR, idReceptor)
            }
        }
        val result = db.update(TABLE_PRENDAS, values, "$COL_PRENDA_ID = ?", arrayOf(idPrenda.toString()))
        return result > 0
    }

    // Cancelar una reserva ("EN PROCESO") y volver a "DISPONIBLE"
    fun cancelarReserva(idPrenda: Int): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_PRENDA_ESTADO, "DISPONIBLE")
            putNull(COL_PRENDA_ID_RECEPTOR)
        }
        val result = db.update(TABLE_PRENDAS, values, "$COL_PRENDA_ID = ?", arrayOf(idPrenda.toString()))
        return result > 0
    }

    // Eliminar una prenda específica por su ID con reembolso de puntos si aplica
    fun eliminarPrenda(idPrenda: Int): Boolean {
        val db = writableDatabase
        var receptorId = -1
        var puntosAReembolsar = 0
        var estadoPrenda = ""

        // 1. Verificar si la prenda tiene una reserva activa para devolver los puntos
        val projection = arrayOf(COL_PRENDA_ID_RECEPTOR, COL_PRENDA_PUNTOS, COL_PRENDA_ESTADO)
        val cursor = db.query(TABLE_PRENDAS, projection, "$COL_PRENDA_ID = ?", arrayOf(idPrenda.toString()), null, null, null)
        
        if (cursor.moveToFirst()) {
            receptorId = if (cursor.isNull(cursor.getColumnIndexOrThrow(COL_PRENDA_ID_RECEPTOR))) -1 
                         else cursor.getInt(cursor.getColumnIndexOrThrow(COL_PRENDA_ID_RECEPTOR))
            puntosAReembolsar = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PRENDA_PUNTOS))
            estadoPrenda = cursor.getString(cursor.getColumnIndexOrThrow(COL_PRENDA_ESTADO))
        }
        cursor.close()

        // 2. Si la prenda estaba reservada ("EN PROCESO"), devolvemos los puntos al receptor
        if (estadoPrenda == "EN PROCESO" && receptorId != -1) {
            sumarPuntosUsuario(receptorId, puntosAReembolsar)
        }

        // 3. Eliminar físicamente la prenda de la base de datos
        val result = db.delete(TABLE_PRENDAS, "$COL_PRENDA_ID = ?", arrayOf(idPrenda.toString()))
        return result > 0
    }

    // Marcar una prenda como OBSERVADA y guardar el motivo
    fun observarPrenda(idPrenda: Int, motivo: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_PRENDA_ESTADO, "OBSERVADA")
            put(COL_PRENDA_OBSERVACION, motivo)
        }
        val result = db.update(TABLE_PRENDAS, values, "$COL_PRENDA_ID = ?", arrayOf(idPrenda.toString()))
        return result > 0
    }

    // Actualizar una prenda con nuevos datos
    fun actualizarPrenda(
        idPrenda: Int,
        titulo: String,
        desc: String,
        carrera: String,
        talla: String,
        genero: String,
        tipo: String,
        puntos: Int,
        imagen: String?,
        estadoFisico: String
    ): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_PRENDA_TITULO, titulo)
            put(COL_PRENDA_DESCRIPCION, desc)
            put(COL_PRENDA_CARRERA, carrera)
            put(COL_PRENDA_TALLA, talla)
            put(COL_PRENDA_GENERO, genero)
            put(COL_PRENDA_TIPO, tipo)
            put(COL_PRENDA_PUNTOS, puntos)
            put(COL_PRENDA_IMAGEN, imagen)
            put(COL_PRENDA_ESTADO_FISICO, estadoFisico)
            put(COL_PRENDA_ESTADO, "DISPONIBLE") // Al actualizar, vuelve a estar disponible
            putNull(COL_PRENDA_OBSERVACION) // Se limpia la observación
        }
        val result = db.update(TABLE_PRENDAS, values, "$COL_PRENDA_ID = ?", arrayOf(idPrenda.toString()))
        return result > 0
    }

    // Limpiar observación (cuando el alumno corrige la prenda)
    fun corregirPrenda(idPrenda: Int): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_PRENDA_ESTADO, "DISPONIBLE")
            putNull(COL_PRENDA_OBSERVACION)
        }
        val result = db.update(TABLE_PRENDAS, values, "$COL_PRENDA_ID = ?", arrayOf(idPrenda.toString()))
        return result > 0
    }
}
