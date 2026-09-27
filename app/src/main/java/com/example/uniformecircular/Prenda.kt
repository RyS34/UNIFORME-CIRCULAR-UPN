package com.example.uniformecircular

data class Prenda(
    val id: String = "",
    val idUsuario: String = "",
    val titulo: String = "",
    val descripcion: String = "",
    val carrera: String = "",
    val talla: String = "",
    val genero: String = "",
    val tipoTransaccion: String = "",
    val puntos: Int = 0,
    val imagenUri: String? = null,
    val estado: String = "DISPONIBLE",
    val idReceptor: String? = null,
    val estadoFisico: String = "Usado",
    val observacion: String? = null
)
