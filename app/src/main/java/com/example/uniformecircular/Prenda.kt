package com.example.uniformecircular

data class Prenda(
    val id: Int,
    val idUsuario: Int,
    val titulo: String,
    val descripcion: String,
    val carrera: String,
    val talla: String,
    val genero: String,
    val tipoTransaccion: String,
    val puntos: Int,
    val imagenUri: String?,
    val estado: String = "DISPONIBLE",
    val idReceptor: Int? = null,
    val estadoFisico: String = "Usado",
    val observacion: String? = null
)
