package com.example.nutriia.analisisIA

/**
 * Diccionario Nutricional Universal y Validador Pediátrico de NutrIA.
 * Contiene datos nutricionales de referencia INSP/USDA/NOM-043 para frutas, verduras,
 * legumbres, lácteos, comida mexicana tradicional, comida rápida/chatarra y marcas comerciales.
 */
object DiccionarioNutricionalUniversal {

    private val baseNutricional = mapOf(
        // ── COMIDA RÁPIDA / CHATARRA (ULTRAPROCESADOS) ──
        "hamburguesa" to NutritionInfo(calories = 275.0, protein = 14.5, carbohydrates = 27.0, fat = 13.0, sugar = 4.5, fiber = 1.5, sodium = 520.0),
        "burger" to NutritionInfo(calories = 275.0, protein = 14.5, carbohydrates = 27.0, fat = 13.0, sugar = 4.5, fiber = 1.5, sodium = 520.0),
        "pizza" to NutritionInfo(calories = 266.0, protein = 11.3, carbohydrates = 31.0, fat = 10.4, sugar = 3.6, fiber = 2.3, sodium = 590.0),
        "papas fritas" to NutritionInfo(calories = 312.0, protein = 3.4, carbohydrates = 41.4, fat = 14.7, sugar = 0.3, fiber = 3.8, sodium = 480.0),
        "papas a la francesa" to NutritionInfo(calories = 312.0, protein = 3.4, carbohydrates = 41.4, fat = 14.7, sugar = 0.3, fiber = 3.8, sodium = 480.0),
        "hot dog" to NutritionInfo(calories = 290.0, protein = 10.4, carbohydrates = 24.2, fat = 16.8, sugar = 4.5, fiber = 1.2, sodium = 680.0),
        "perro caliente" to NutritionInfo(calories = 290.0, protein = 10.4, carbohydrates = 24.2, fat = 16.8, sugar = 4.5, fiber = 1.2, sodium = 680.0),
        "nuggets" to NutritionInfo(calories = 296.0, protein = 15.3, carbohydrates = 18.5, fat = 18.0, sugar = 0.5, fiber = 1.2, sodium = 560.0),
        "nugget" to NutritionInfo(calories = 296.0, protein = 15.3, carbohydrates = 18.5, fat = 18.0, sugar = 0.5, fiber = 1.2, sodium = 560.0),
        "refresco" to NutritionInfo(calories = 42.0, protein = 0.0, carbohydrates = 10.6, fat = 0.0, sugar = 10.6, fiber = 0.0, sodium = 11.0),
        "coca-cola" to NutritionInfo(calories = 42.0, protein = 0.0, carbohydrates = 10.6, fat = 0.0, sugar = 10.6, fiber = 0.0, sodium = 11.0),
        "coca cola" to NutritionInfo(calories = 42.0, protein = 0.0, carbohydrates = 10.6, fat = 0.0, sugar = 10.6, fiber = 0.0, sodium = 11.0),
        "sabritas" to NutritionInfo(calories = 520.0, protein = 6.0, carbohydrates = 54.0, fat = 31.0, sugar = 2.0, fiber = 4.0, sodium = 780.0),
        "doritos" to NutritionInfo(calories = 510.0, protein = 7.0, carbohydrates = 58.0, fat = 28.0, sugar = 3.0, fiber = 4.5, sodium = 810.0),
        "cheetos" to NutritionInfo(calories = 540.0, protein = 5.5, carbohydrates = 53.0, fat = 34.0, sugar = 2.5, fiber = 2.0, sodium = 840.0),
        "gansito" to NutritionInfo(calories = 410.0, protein = 4.0, carbohydrates = 58.0, fat = 18.0, sugar = 38.0, fiber = 1.5, sodium = 220.0),
        "chocorroles" to NutritionInfo(calories = 430.0, protein = 4.5, carbohydrates = 60.0, fat = 19.5, sugar = 42.0, fiber = 1.2, sodium = 240.0),
        "dona" to NutritionInfo(calories = 420.0, protein = 5.5, carbohydrates = 52.0, fat = 21.5, sugar = 26.0, fiber = 1.8, sodium = 320.0),
        "donut" to NutritionInfo(calories = 420.0, protein = 5.5, carbohydrates = 52.0, fat = 21.5, sugar = 26.0, fiber = 1.8, sodium = 320.0),
        "galletas oreo" to NutritionInfo(calories = 480.0, protein = 4.8, carbohydrates = 69.0, fat = 20.0, sugar = 38.0, fiber = 2.5, sodium = 410.0),

        // ── COMIDA MEXICANA TRADICIONAL ──
        "taco al pastor" to NutritionInfo(calories = 215.0, protein = 13.5, carbohydrates = 18.2, fat = 9.8, sugar = 2.5, fiber = 2.1, sodium = 380.0),
        "tacos al pastor" to NutritionInfo(calories = 215.0, protein = 13.5, carbohydrates = 18.2, fat = 9.8, sugar = 2.5, fiber = 2.1, sodium = 380.0),
        "taco de bistec" to NutritionInfo(calories = 195.0, protein = 16.0, carbohydrates = 17.5, fat = 6.5, sugar = 1.0, fiber = 2.0, sodium = 320.0),
        "tacos de bistec" to NutritionInfo(calories = 195.0, protein = 16.0, carbohydrates = 17.5, fat = 6.5, sugar = 1.0, fiber = 2.0, sodium = 320.0),
        "taco de suadero" to NutritionInfo(calories = 260.0, protein = 14.0, carbohydrates = 17.0, fat = 15.5, sugar = 0.8, fiber = 1.9, sodium = 410.0),
        "carnitas" to NutritionInfo(calories = 270.0, protein = 18.5, carbohydrates = 0.0, fat = 21.0, sugar = 0.0, fiber = 0.0, sodium = 450.0),
        "quesadilla" to NutritionInfo(calories = 245.0, protein = 12.0, carbohydrates = 24.0, fat = 11.5, sugar = 1.2, fiber = 2.5, sodium = 390.0),
        "quesadilla de queso" to NutritionInfo(calories = 245.0, protein = 12.0, carbohydrates = 24.0, fat = 11.5, sugar = 1.2, fiber = 2.5, sodium = 390.0),
        "enchiladas" to NutritionInfo(calories = 165.0, protein = 9.5, carbohydrates = 14.0, fat = 8.0, sugar = 2.0, fiber = 2.3, sodium = 390.0),
        "enchiladas verdes" to NutritionInfo(calories = 165.0, protein = 9.5, carbohydrates = 14.0, fat = 8.0, sugar = 2.0, fiber = 2.3, sodium = 390.0),
        "enchiladas rojas" to NutritionInfo(calories = 165.0, protein = 9.5, carbohydrates = 14.0, fat = 8.0, sugar = 2.0, fiber = 2.3, sodium = 390.0),
        "pozole" to NutritionInfo(calories = 115.0, protein = 7.5, carbohydrates = 11.5, fat = 4.5, sugar = 1.0, fiber = 2.0, sodium = 360.0),
        "chilaquiles" to NutritionInfo(calories = 210.0, protein = 8.5, carbohydrates = 21.0, fat = 10.5, sugar = 2.2, fiber = 2.8, sodium = 420.0),
        "tamal" to NutritionInfo(calories = 230.0, protein = 6.5, carbohydrates = 26.0, fat = 11.5, sugar = 1.8, fiber = 2.2, sodium = 440.0),
        "tamales" to NutritionInfo(calories = 230.0, protein = 6.5, carbohydrates = 26.0, fat = 11.5, sugar = 1.8, fiber = 2.2, sodium = 440.0),
        "mole con pollo" to NutritionInfo(calories = 185.0, protein = 14.0, carbohydrates = 10.5, fat = 9.8, sugar = 4.5, fiber = 1.8, sodium = 350.0),
        "caldo de pollo" to NutritionInfo(calories = 55.0, protein = 4.5, carbohydrates = 4.0, fat = 2.2, sugar = 1.5, fiber = 1.0, sodium = 260.0),
        "sopa de verduras" to NutritionInfo(calories = 45.0, protein = 1.8, carbohydrates = 7.5, fat = 0.8, sugar = 2.2, fiber = 2.0, sodium = 240.0),
        "sopa de fideo" to NutritionInfo(calories = 85.0, protein = 2.2, carbohydrates = 14.5, fat = 2.1, sugar = 1.8, fiber = 1.1, sodium = 290.0),
        "arroz rojo" to NutritionInfo(calories = 130.0, protein = 2.5, carbohydrates = 26.0, fat = 1.8, sugar = 0.8, fiber = 1.2, sodium = 280.0),
        "arroz blanco" to NutritionInfo(calories = 125.0, protein = 2.4, carbohydrates = 27.0, fat = 0.5, sugar = 0.2, fiber = 0.8, sodium = 150.0),
        "frijoles refritos" to NutritionInfo(calories = 140.0, protein = 6.0, carbohydrates = 18.0, fat = 5.5, sugar = 0.5, fiber = 5.0, sodium = 420.0),
        "frijoles de la olla" to NutritionInfo(calories = 127.0, protein = 8.7, carbohydrates = 22.8, fat = 0.5, sugar = 0.3, fiber = 6.4, sodium = 2.0),
        "frijol" to NutritionInfo(calories = 127.0, protein = 8.7, carbohydrates = 22.8, fat = 0.5, sugar = 0.3, fiber = 6.4, sodium = 2.0),
        "tortilla de maíz" to NutritionInfo(calories = 218.0, protein = 5.7, carbohydrates = 45.0, fat = 2.8, sugar = 1.0, fiber = 6.3, sodium = 35.0),
        "tortilla" to NutritionInfo(calories = 218.0, protein = 5.7, carbohydrates = 45.0, fat = 2.8, sugar = 1.0, fiber = 6.3, sodium = 35.0),
        "huevo con jamon" to NutritionInfo(calories = 165.0, protein = 11.5, carbohydrates = 2.0, fat = 12.5, sugar = 1.0, fiber = 0.5, sodium = 420.0),
        "huevo revuelto" to NutritionInfo(calories = 148.0, protein = 10.0, carbohydrates = 1.0, fat = 11.0, sugar = 0.8, fiber = 0.0, sodium = 140.0),
        "huevo cocido" to NutritionInfo(calories = 155.0, protein = 12.6, carbohydrates = 1.1, fat = 10.6, sugar = 1.1, fiber = 0.0, sodium = 124.0),
        "huevo" to NutritionInfo(calories = 148.0, protein = 10.0, carbohydrates = 1.0, fat = 11.0, sugar = 0.8, fiber = 0.0, sodium = 140.0),

        // ── FRUTAS ──
        "manzana" to NutritionInfo(calories = 52.0, protein = 0.3, carbohydrates = 14.0, fat = 0.2, sugar = 10.4, fiber = 2.4, sodium = 1.0),
        "platano" to NutritionInfo(calories = 89.0, protein = 1.1, carbohydrates = 23.0, fat = 0.3, sugar = 12.0, fiber = 2.6, sodium = 1.0),
        "plátano" to NutritionInfo(calories = 89.0, protein = 1.1, carbohydrates = 23.0, fat = 0.3, sugar = 12.0, fiber = 2.6, sodium = 1.0),
        "banana" to NutritionInfo(calories = 89.0, protein = 1.1, carbohydrates = 23.0, fat = 0.3, sugar = 12.0, fiber = 2.6, sodium = 1.0),
        "fresa" to NutritionInfo(calories = 32.0, protein = 0.7, carbohydrates = 7.7, fat = 0.3, sugar = 4.9, fiber = 2.0, sodium = 1.0),
        "fresas" to NutritionInfo(calories = 32.0, protein = 0.7, carbohydrates = 7.7, fat = 0.3, sugar = 4.9, fiber = 2.0, sodium = 1.0),
        "mango" to NutritionInfo(calories = 60.0, protein = 0.8, carbohydrates = 15.0, fat = 0.4, sugar = 13.7, fiber = 1.6, sodium = 1.0),
        "papaya" to NutritionInfo(calories = 43.0, protein = 0.5, carbohydrates = 11.0, fat = 0.3, sugar = 7.8, fiber = 1.7, sodium = 8.0),
        "naranja" to NutritionInfo(calories = 47.0, protein = 0.9, carbohydrates = 12.0, fat = 0.1, sugar = 9.0, fiber = 2.4, sodium = 0.0),
        "sandia" to NutritionInfo(calories = 30.0, protein = 0.6, carbohydrates = 7.6, fat = 0.2, sugar = 6.2, fiber = 0.4, sodium = 1.0),
        "sandía" to NutritionInfo(calories = 30.0, protein = 0.6, carbohydrates = 7.6, fat = 0.2, sugar = 6.2, fiber = 0.4, sodium = 1.0),
        "uvas" to NutritionInfo(calories = 69.0, protein = 0.7, carbohydrates = 18.0, fat = 0.2, sugar = 15.0, fiber = 0.9, sodium = 2.0),
        "uva" to NutritionInfo(calories = 69.0, protein = 0.7, carbohydrates = 18.0, fat = 0.2, sugar = 15.0, fiber = 0.9, sodium = 2.0),
        "piña" to NutritionInfo(calories = 50.0, protein = 0.5, carbohydrates = 13.0, fat = 0.1, sugar = 10.0, fiber = 1.4, sodium = 1.0),
        "aguacate" to NutritionInfo(calories = 160.0, protein = 2.0, carbohydrates = 8.5, fat = 14.7, sugar = 0.7, fiber = 6.7, sodium = 7.0),
        "guayaba" to NutritionInfo(calories = 68.0, protein = 2.6, carbohydrates = 14.3, fat = 1.0, sugar = 8.9, fiber = 5.4, sodium = 2.0),
        "pera" to NutritionInfo(calories = 57.0, protein = 0.4, carbohydrates = 15.0, fat = 0.1, sugar = 9.8, fiber = 3.1, sodium = 1.0),
        "melon" to NutritionInfo(calories = 34.0, protein = 0.8, carbohydrates = 8.2, fat = 0.2, sugar = 7.9, fiber = 0.9, sodium = 16.0),
        "melón" to NutritionInfo(calories = 34.0, protein = 0.8, carbohydrates = 8.2, fat = 0.2, sugar = 7.9, fiber = 0.9, sodium = 16.0),

        // ── VERDURAS ──
        "zanahoria" to NutritionInfo(calories = 41.0, protein = 0.9, carbohydrates = 9.6, fat = 0.2, sugar = 4.7, fiber = 2.8, sodium = 69.0),
        "brocoli" to NutritionInfo(calories = 34.0, protein = 2.8, carbohydrates = 6.6, fat = 0.4, sugar = 1.7, fiber = 2.6, sodium = 33.0),
        "brócoli" to NutritionInfo(calories = 34.0, protein = 2.8, carbohydrates = 6.6, fat = 0.4, sugar = 1.7, fiber = 2.6, sodium = 33.0),
        "calabacita" to NutritionInfo(calories = 17.0, protein = 1.2, carbohydrates = 3.1, fat = 0.3, sugar = 2.5, fiber = 1.0, sodium = 8.0),
        "calabaza" to NutritionInfo(calories = 26.0, protein = 1.0, carbohydrates = 6.5, fat = 0.1, sugar = 2.8, fiber = 0.5, sodium = 1.0),
        "jitomate" to NutritionInfo(calories = 18.0, protein = 0.9, carbohydrates = 3.9, fat = 0.2, sugar = 2.6, fiber = 1.2, sodium = 5.0),
        "tomate" to NutritionInfo(calories = 18.0, protein = 0.9, carbohydrates = 3.9, fat = 0.2, sugar = 2.6, fiber = 1.2, sodium = 5.0),
        "espinaca" to NutritionInfo(calories = 23.0, protein = 2.9, carbohydrates = 3.6, fat = 0.4, sugar = 0.4, fiber = 2.2, sodium = 79.0),
        "espinacas" to NutritionInfo(calories = 23.0, protein = 2.9, carbohydrates = 3.6, fat = 0.4, sugar = 0.4, fiber = 2.2, sodium = 79.0),
        "nopal" to NutritionInfo(calories = 15.0, protein = 1.4, carbohydrates = 3.3, fat = 0.1, sugar = 1.1, fiber = 2.2, sodium = 21.0),
        "nopales" to NutritionInfo(calories = 15.0, protein = 1.4, carbohydrates = 3.3, fat = 0.1, sugar = 1.1, fiber = 2.2, sodium = 21.0),
        "chayote" to NutritionInfo(calories = 19.0, protein = 0.8, carbohydrates = 4.5, fat = 0.1, sugar = 1.7, fiber = 1.7, sodium = 2.0),
        "lechuga" to NutritionInfo(calories = 15.0, protein = 1.2, carbohydrates = 2.9, fat = 0.2, sugar = 1.2, fiber = 1.3, sodium = 10.0),
        "pepino" to NutritionInfo(calories = 15.0, protein = 0.7, carbohydrates = 3.6, fat = 0.1, sugar = 1.7, fiber = 0.5, sodium = 2.0),
        "papa" to NutritionInfo(calories = 86.0, protein = 1.9, carbohydrates = 20.0, fat = 0.1, sugar = 0.8, fiber = 1.8, sodium = 6.0),
        "papa cocida" to NutritionInfo(calories = 86.0, protein = 1.9, carbohydrates = 20.0, fat = 0.1, sugar = 0.8, fiber = 1.8, sodium = 6.0),

        // ── LEGUMBRES ──
        "lentejas" to NutritionInfo(calories = 116.0, protein = 9.0, carbohydrates = 20.1, fat = 0.4, sugar = 1.8, fiber = 7.9, sodium = 2.0),
        "lenteja" to NutritionInfo(calories = 116.0, protein = 9.0, carbohydrates = 20.1, fat = 0.4, sugar = 1.8, fiber = 7.9, sodium = 2.0),
        "garbanzos" to NutritionInfo(calories = 164.0, protein = 8.9, carbohydrates = 27.4, fat = 2.6, sugar = 4.8, fiber = 7.6, sodium = 7.0),
        "habas" to NutritionInfo(calories = 110.0, protein = 7.6, carbohydrates = 19.5, fat = 0.4, sugar = 1.5, fiber = 5.4, sodium = 5.0),

        // ── LÁCTEOS Y MARCAS COMERCIALES ──
        "leche entera" to NutritionInfo(calories = 61.0, protein = 3.2, carbohydrates = 4.8, fat = 3.3, sugar = 5.1, fiber = 0.0, sodium = 43.0),
        "leche deslactosada" to NutritionInfo(calories = 48.0, protein = 3.1, carbohydrates = 4.8, fat = 1.8, sugar = 4.8, fiber = 0.0, sodium = 45.0),
        "leche lala" to NutritionInfo(calories = 60.0, protein = 3.1, carbohydrates = 4.7, fat = 3.2, sugar = 4.7, fiber = 0.0, sodium = 45.0),
        "leche alpura" to NutritionInfo(calories = 59.0, protein = 3.1, carbohydrates = 4.8, fat = 3.1, sugar = 4.8, fiber = 0.0, sodium = 44.0),
        "leche" to NutritionInfo(calories = 58.0, protein = 3.1, carbohydrates = 4.8, fat = 2.8, sugar = 4.8, fiber = 0.0, sodium = 44.0),
        "queso panela" to NutritionInfo(calories = 220.0, protein = 17.5, carbohydrates = 2.8, fat = 15.5, sugar = 1.5, fiber = 0.0, sodium = 480.0),
        "queso oaxaca" to NutritionInfo(calories = 285.0, protein = 22.0, carbohydrates = 2.0, fat = 21.0, sugar = 1.0, fiber = 0.0, sodium = 580.0),
        "quesillo" to NutritionInfo(calories = 285.0, protein = 22.0, carbohydrates = 2.0, fat = 21.0, sugar = 1.0, fiber = 0.0, sodium = 580.0),
        "queso fresco" to NutritionInfo(calories = 260.0, protein = 18.0, carbohydrates = 3.0, fat = 20.0, sugar = 1.5, fiber = 0.0, sodium = 510.0),
        "yogurt natural" to NutritionInfo(calories = 61.0, protein = 3.5, carbohydrates = 4.7, fat = 3.3, sugar = 4.7, fiber = 0.0, sodium = 46.0),
        "danonino" to NutritionInfo(calories = 115.0, protein = 6.2, carbohydrates = 14.5, fat = 3.5, sugar = 12.8, fiber = 0.2, sodium = 55.0),
        "yakult" to NutritionInfo(calories = 66.0, protein = 1.2, carbohydrates = 15.4, fat = 0.1, sugar = 14.2, fiber = 0.0, sodium = 17.0),
        "gerber papilla" to NutritionInfo(calories = 65.0, protein = 0.5, carbohydrates = 15.5, fat = 0.2, sugar = 12.0, fiber = 1.5, sodium = 5.0),
        "gerber" to NutritionInfo(calories = 65.0, protein = 0.5, carbohydrates = 15.5, fat = 0.2, sugar = 12.0, fiber = 1.5, sodium = 5.0),
        "pan bimbo" to NutritionInfo(calories = 260.0, protein = 8.5, carbohydrates = 48.0, fat = 3.5, sugar = 5.5, fiber = 4.5, sodium = 450.0),
        "avena" to NutritionInfo(calories = 68.0, protein = 2.5, carbohydrates = 12.0, fat = 1.4, sugar = 0.5, fiber = 1.7, sodium = 49.0),
        "avena cocida" to NutritionInfo(calories = 68.0, protein = 2.5, carbohydrates = 12.0, fat = 1.4, sugar = 0.5, fiber = 1.7, sodium = 49.0),
        "pechuga de pollo" to NutritionInfo(calories = 165.0, protein = 31.0, carbohydrates = 0.0, fat = 3.6, sugar = 0.0, fiber = 0.0, sodium = 74.0),
        "pollo" to NutritionInfo(calories = 165.0, protein = 31.0, carbohydrates = 0.0, fat = 3.6, sugar = 0.0, fiber = 0.0, sodium = 74.0),
        "carne de res" to NutritionInfo(calories = 250.0, protein = 26.0, carbohydrates = 0.0, fat = 16.0, sugar = 0.0, fiber = 0.0, sodium = 72.0),
        "pescado" to NutritionInfo(calories = 110.0, protein = 23.0, carbohydrates = 0.0, fat = 2.0, sugar = 0.0, fiber = 0.0, sodium = 60.0)
    )

    private val terminosChatarra = listOf(
        "hamburguesa", "burger", "pizza", "papas fritas", "papas a la francesa",
        "hot dog", "perro caliente", "nugget", "nuggets", "refresco", "coca-cola", "coca cola", "pepsi",
        "sabritas", "doritos", "cheetos", "frituras", "papas de bolsa", "maruchan", "sopa instantanea",
        "gansito", "chocorroles", "submarinos", "dona", "donut", "dulces", "caramelos",
        "paleta", "gomitas", "salchicha frita", "tocino frito", "tocineta", "oreo", "emperador", "pan dulce industrial"
    )

    fun esComidaChatarra(foodName: String, ingredients: List<String> = emptyList()): Boolean {
        val lower = foodName.lowercase().trim()
        if (terminosChatarra.any { lower.contains(it) }) return true
        val ingLower = ingredients.map { it.lowercase().trim() }
        if (ingLower.any { ing -> terminosChatarra.any { ing.contains(it) } }) return true
        return false
    }

    fun obtenerNutricion(foodName: String): NutritionInfo {
        val lower = foodName.lowercase().trim()

        // 1. Coincidencia exacta
        baseNutricional[lower]?.let { return it }

        // 2. Coincidencia por subcadena
        for ((key, value) in baseNutricional) {
            if (lower.contains(key)) {
                return value
            }
        }

        // 3. Fallback inteligente según categoría general
        return when {
            esComidaChatarra(foodName) -> NutritionInfo(calories = 280.0, protein = 12.0, carbohydrates = 30.0, fat = 13.0, sugar = 5.0, fiber = 1.5, sodium = 540.0)
            lower.contains("taco") || lower.contains("quesadilla") || lower.contains("garnacha") -> NutritionInfo(calories = 210.0, protein = 11.0, carbohydrates = 22.0, fat = 9.0, sugar = 1.5, fiber = 2.5, sodium = 380.0)
            lower.contains("fruta") || lower.contains("jugo") -> NutritionInfo(calories = 55.0, protein = 0.6, carbohydrates = 13.5, fat = 0.2, sugar = 10.0, fiber = 2.0, sodium = 2.0)
            lower.contains("verdura") || lower.contains("ensalada") -> NutritionInfo(calories = 30.0, protein = 1.5, carbohydrates = 6.0, fat = 0.3, sugar = 2.5, fiber = 2.2, sodium = 30.0)
            lower.contains("sopa") || lower.contains("caldo") -> NutritionInfo(calories = 65.0, protein = 3.5, carbohydrates = 8.0, fat = 2.0, sugar = 1.5, fiber = 1.2, sodium = 280.0)
            lower.contains("leche") || lower.contains("yogurt") || lower.contains("queso") -> NutritionInfo(calories = 95.0, protein = 6.0, carbohydrates = 5.5, fat = 5.0, sugar = 4.5, fiber = 0.0, sodium = 120.0)
            else -> NutritionInfo(calories = 145.0, protein = 7.5, carbohydrates = 16.0, fat = 5.5, sugar = 2.0, fiber = 2.0, sodium = 180.0)
        }
    }

    fun analisisPediatricoFallback(
        childName: String,
        ageText: String,
        foodName: String,
        ingredients: List<String>,
        nutrition: NutritionInfo,
        esChatarra: Boolean
    ): PediatricAnalysis {
        return if (esChatarra) {
            PediatricAnalysis(
                recommended = false,
                recommendedPortion = "Evitar en la alimentación diaria infantil (0g) o máximo 1/4 pieza casera esporádica",
                benefits = listOf(
                    "Aporte de calorías y macronutrientes rápidos",
                    "Aceptación por sabor agradable pero con bajo valor biológico"
                ),
                warnings = listOf(
                    "Alimento ultraprocesado/chatarra: No recomendado para $childName.",
                    "Aporte excesivo de sodio (${nutrition.sodium.toInt()} mg/100g) y grasas que sobrecargan los riñones del pequeño.",
                    "Fomenta el hábito por alimentos hiperpalatables desplazando frutas y verduras."
                ),
                frequency = "Evitar o consumo ocasional extraordinario"
            )
        } else {
            PediatricAnalysis(
                recommended = true,
                recommendedPortion = "1 porción pequeña adaptada para $ageText (50g a 80g)",
                benefits = listOf(
                    "Aporte de energía saludable y nutrientes esenciales",
                    "Contribuye al crecimiento armónico y buen desarrollo cognitivo"
                ),
                warnings = if (nutrition.sodium > 350) listOf("Contenido moderado de sodio, preferir preparaciones con poca sal.") else emptyList(),
                frequency = "3 a 4 veces por semana"
            )
        }
    }
}
