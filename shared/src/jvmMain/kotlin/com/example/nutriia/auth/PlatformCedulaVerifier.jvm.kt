package com.example.nutriia.auth

actual object PlatformCedulaVerifier {
    actual suspend fun verificarCedulaNativa(cedula: String): ResultadoCedula {
        return ResultadoCedula(
            valida = true,
            cedula = cedula,
            nombreTitular = "Especialista Validado (JVM Test)",
            genero = "No especificado",
            institucion = "Universidad Nacional",
            profesion = "Licenciatura en Nutrición",
            entidad = "Ciudad de México",
            anoRegistro = "2020",
            mensaje = "Cédula verificada en entorno de pruebas JVM"
        )
    }
}
