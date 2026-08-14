package com.example.nutriia.sueldo

// ═══════════════════════════════════════════════════════════════════════════
// MOTOR DE DIETAS — RECETAS MEXICANAS OFICIALES  (versión 2.2)
//
// FUENTES VERIFICADAS:
// [1] IMSS — Guía Alimentación Sana, Variada y Suficiente 0-12 meses (2020)
//     Procedimiento Alimentación Guarderías IMSS 2023
// [2] SEP / Secretaría de Salud Nuevo León —
//     Catálogo de Desayunos, Refrigerios y Comidas Escolares
// [3] OMS/PAHO — Principios de orientación para alimentación complementaria
// [4] Cultura.gob.mx — Gastronomía Poblana
// [5] COCO 2023 — Consenso de Alimentación Complementaria México
// [6] IMSS — NutrIMSS Infancia / Guía Nutrición Escolar
// [7] Secretaría de Salud — Guías Alimentarias para la Población Mexicana 2023
//
// CAMBIOS v2.2 (sobre v2.1):
//  1. SMG actualizado a 2026 en todos los comentarios
//  2. costoEstimadoPorNivelEtapa() recalibrado vs canasta PACIC/PROFECO 2026
//  3. Nuevas recetas omega-3 económicas: Sierra y Sardina (BASICO/MEDIO)
//  4. Nota de precio en receta Salmón (nivel ALTO)
//  5. resumenNutricional() — alimentosClave añade sierra y sardina
//  6. mapAlimentoAlergeno() — Sierra, Sardina y Mojarra mapeadas a PESCADO
//
// FUENTES DE PRECIO (v2.2):
//  CONASAMI dic 2025 — SMG $315.04/día · $9,451/mes (zona general)
//  PROFECO "Quién es Quién" ene–mar 2026
//  SNIIM Puebla mar 2026 — tortilla $17.69/kg
//  ANPEC mar 2026 — pescados frescos
//  Canasta PACIC 24 prod. ~$874/semana · ~$3,758/mes familia 4 personas
//
// NOTA CLÍNICA: Este módulo es orientativo. No sustituye consulta médica.
// ═══════════════════════════════════════════════════════════════════════════

object DietaEngine {

    fun macrosPorEdad(meses: Int): MacroObjetivo = when {
        meses < 6   -> MacroObjetivo(450,  550,  9.1, 55.0, 40.0,  0.27, 200.0, 400, 2.0)
        meses < 9   -> MacroObjetivo(650,  800, 11.0, 45.0, 45.0, 11.0,  260.0, 500, 3.0)
        meses < 12  -> MacroObjetivo(750,  950, 13.0, 40.0, 47.0, 11.0,  260.0, 500, 3.0)
        meses < 24  -> MacroObjetivo(900, 1000, 13.0, 35.0, 50.0,  7.0,  700.0, 300, 3.0)
        meses < 48  -> MacroObjetivo(1000, 1300,13.0, 35.0, 52.0,  7.0,  700.0, 400, 4.0)
        meses < 72  -> MacroObjetivo(1300, 1600,20.0, 30.0, 55.0, 10.0, 1000.0, 450, 5.0)
        meses < 108 -> MacroObjetivo(1700, 2000,36.0, 28.0, 57.0, 10.0, 1300.0, 500, 7.0)
        else        -> MacroObjetivo(2000, 2450,41.0, 25.0, 58.0,  8.0, 1300.0, 600, 8.0)
    }

    fun etapaLabel(meses: Int): String = when {
        meses < 6   -> "Lactancia exclusiva (0-6 meses)"
        meses < 12  -> "Inicio de sólidos (6-12 meses)"
        meses < 24  -> "Diversificación (1-2 años)"
        meses < 36  -> "Alimentación variada (2-3 años)"
        meses < 60  -> "Preescolar (3-5 años)"
        meses < 96  -> "Escolar temprano (5-8 años)"
        meses < 144 -> "Escolar (8-12 años)"
        else        -> "Adolescencia (12+ años)"
    }

    val RECETAS: List<RecetaMexicana> = listOf(

        // ══════════════════════════════════════════════
        // 6-7 MESES — IMSS Guía 0-12 meses
        // ══════════════════════════════════════════════

        RecetaMexicana(
            nombre       = "Puré de zanahoria",
            ingredientes = listOf("60 g de zanahoria", "1 cdta de aceite de oliva"),
            preparacion  = "Pela la zanahoria. Hierve 2 tazas de agua; al primer hervor agrega " +
                    "la zanahoria y cuece 5 minutos. Licua con 1/4 taza de agua de " +
                    "cocción y el aceite. Cuela. Sin sal ni azúcar.",
            kcal = 35, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 6,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020",
            alergenos = emptyList()
        ),

        RecetaMexicana(
            nombre       = "Puré de calabaza",
            ingredientes = listOf("60 g de calabacita italiana", "1 cdta de aceite de oliva"),
            preparacion  = "Retira extremos de la calabaza. Hierve 2 tazas de agua; agrega la " +
                    "calabaza y cuece 5 minutos. Licua con 1/4 taza de agua de cocción " +
                    "y el aceite. Cuela. Sin sal ni azúcar.",
            kcal = 25, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 6,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020",
            alergenos = emptyList()
        ),

        RecetaMexicana(
            nombre       = "Puré de chayote",
            ingredientes = listOf("60 g de chayote", "1 cdta de aceite de oliva"),
            preparacion  = "Pela el chayote. Hierve 2 tazas de agua; agrega el chayote y cuece " +
                    "5 minutos. Licua con 1/4 taza de agua de cocción y el aceite. " +
                    "Cuela. Sin sal ni azúcar.",
            kcal = 22, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 6,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020",
            alergenos = emptyList()
        ),

        RecetaMexicana(
            nombre       = "Puré de chícharo",
            ingredientes = listOf("30 g de chícharo limpio", "1 cdta de aceite de oliva"),
            preparacion  = "Hierve 2 tazas de agua; agrega chícharos y cuece 5 minutos. " +
                    "Licua con 1/4 taza de agua de cocción y aceite. Cuela bien. " +
                    "Sin sal ni azúcar.",
            kcal = 40, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 6,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020",
            alergenos = emptyList()
        ),

        RecetaMexicana(
            nombre       = "Puré de papaya",
            ingredientes = listOf("50 g de papaya (1 taza)"),
            preparacion  = "Pela y corta la papaya. Tritura. Cuela. No requiere cocción. " +
                    "Sin azúcar añadida (OMS/COCO 2023: no azúcar antes de 2 años).",
            kcal = 22, tipoComida = TipoComida.COLACION,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 6,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020",
            alergenos = emptyList()
        ),

        RecetaMexicana(
            nombre       = "Puré de manzana",
            ingredientes = listOf("50 g de manzana (1/2 pieza)"),
            preparacion  = "Corta la manzana y retira semillas. Hierve 2 tazas de agua; agrega " +
                    "la manzana y cuece 3 minutos. Licua con agua de cocción. Cuela. " +
                    "Sin azúcar añadida (OMS/COCO 2023).",
            kcal = 25, tipoComida = TipoComida.COLACION,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 6,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020",
            alergenos = listOf(Alergeno.FRUCTOSA)
        ),

        RecetaMexicana(
            nombre       = "Puré de pera",
            ingredientes = listOf("50 g de pera (1/2 pieza)"),
            preparacion  = "Corta la pera y retira semillas. Hierve 2 tazas de agua; agrega la " +
                    "pera y cuece 3 minutos. Licua con agua de cocción. Cuela. " +
                    "Sin azúcar añadida (OMS/COCO 2023).",
            kcal = 25, tipoComida = TipoComida.COLACION,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 6,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020",
            alergenos = listOf(Alergeno.FRUCTOSA)
        ),

        RecetaMexicana(
            nombre       = "Puré de plátano con fórmula",
            ingredientes = listOf(
                "30 g de plátano (1/2 pieza)",
                "30 ml de agua hervida",
                "4.30 g de fórmula infantil (1 medida)"
            ),
            preparacion  = "Prepara la fórmula con el agua hervida. Pela el plátano. " +
                    "Tritura con la fórmula rehidratada. Cuela. Sin azúcar añadida.",
            kcal = 55, tipoComida = TipoComida.COLACION,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 6,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020",
            alergenos = listOf(Alergeno.LACTEOS)
        ),

        RecetaMexicana(
            nombre       = "Puré de guayaba",
            ingredientes = listOf("50 g de guayaba (1 pieza)"),
            preparacion  = "Hierve 1 taza de agua; agrega la guayaba y cuece 3 minutos. " +
                    "Licua con agua de cocción. Cuela bien para eliminar semillas. " +
                    "Sin azúcar añadida.",
            kcal = 20, tipoComida = TipoComida.COLACION,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 6,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020",
            alergenos = emptyList()
        ),

        // ══════════════════════════════════════════════
        // 6-7 MESES — CENA
        // IMSS: la última toma del día puede ser puré de verdura suave
        // ══════════════════════════════════════════════

        RecetaMexicana(
            nombre       = "Crema de calabaza con aceite de oliva",
            ingredientes = listOf("60 g de calabacita", "1 cdta de aceite de oliva"),
            preparacion  = "Hierve la calabacita hasta muy blanda (8 min). Licua con el aceite " +
                    "y 1/4 taza de agua de cocción hasta textura cremosa. Cuela. " +
                    "Tibio. Sin sal ni azúcar. Ofrecer antes de la última toma de leche.",
            kcal = 30, tipoComida = TipoComida.CENA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 6,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020",
            alergenos = emptyList()
        ),

        RecetaMexicana(
            nombre       = "Crema de zanahoria con aceite",
            ingredientes = listOf("60 g de zanahoria", "1 cdta de aceite de canola"),
            preparacion  = "Pela y trocea la zanahoria. Cuece 10 min en agua hasta muy suave. " +
                    "Licua con 1/4 taza de agua de cocción y el aceite. Cuela fino. " +
                    "Servir tibio. Sin sal ni azúcar.",
            kcal = 38, tipoComida = TipoComida.CENA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 6,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020",
            alergenos = emptyList()
        ),

        // ══════════════════════════════════════════════
        // 7-9 MESES — IMSS Guía 0-12 meses
        // NOTA: avena/gluten NO antes de 8 meses (OMS/COCO 2023)
        // ══════════════════════════════════════════════

        RecetaMexicana(
            nombre       = "Puré de arroz integral",
            ingredientes = listOf("1/2 taza de arroz integral", "1 cdta de aceite de oliva"),
            preparacion  = "Remoja el arroz 1 hora y escurre. Cuece en 1.5 tazas de agua a fuego " +
                    "lento. Licua agregando poco a poco el aceite hasta mezcla homogénea. " +
                    "Sin sal ni azúcar.",
            kcal = 110, tipoComida = TipoComida.DESAYUNO,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 7,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020",
            alergenos = emptyList()
        ),

        RecetaMexicana(
            nombre       = "Puré de avena",
            ingredientes = listOf("1/2 taza de hojuelas de avena"),
            preparacion  = "En olla con agua caliente cuece la avena a fuego lento. Licua con " +
                    "agua de cocción hasta mezcla homogénea. Sin sal ni azúcar. " +
                    "NOTA: no ofrecer antes de 8 meses (puede contener trazas de gluten).",
            kcal = 75, tipoComida = TipoComida.DESAYUNO,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 8,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020 / COCO 2023",
            alergenos = listOf(Alergeno.TRIGO)
        ),

        RecetaMexicana(
            nombre       = "Puré de pollo con papa",
            ingredientes = listOf(
                "30 g de pechuga de pollo",
                "10 g de papa blanca",
                "1 cdta de aceite de oliva"
            ),
            preparacion  = "Pela la papa. Cuece la pechuga y la papa en agua. Licua usando caldo " +
                    "de cocción. Agrega el aceite poco a poco. Cuela. Sin sal.",
            kcal = 85, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 8,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020",
            alergenos = emptyList()
        ),

        RecetaMexicana(
            nombre       = "Puré de res con papa",
            ingredientes = listOf(
                "30 g de chambarete de res",
                "10 g de papa blanca",
                "1 cdta de aceite de oliva"
            ),
            preparacion  = "Pela la papa. Cuece la carne y la papa en agua. Licua usando caldo " +
                    "de cocción. Agrega el aceite poco a poco. Cuela. Sin sal.",
            kcal = 90, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 8,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020",
            alergenos = emptyList()
        ),

        RecetaMexicana(
            nombre       = "Puré de frijol",
            ingredientes = listOf("15 g de frijol seco", "1 cdta de aceite de oliva"),
            preparacion  = "Remoja el frijol 15 min. Cuece en agua suficiente 50 min. " +
                    "Licua con caldo de cocción y el aceite. Cuela. Sin sal.",
            kcal = 80, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 8,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020",
            alergenos = emptyList()
        ),

        RecetaMexicana(
            nombre       = "Puré de lenteja",
            ingredientes = listOf("15 g de lentejas", "1 cdta de aceite de oliva"),
            preparacion  = "Remoja las lentejas 15 min. Cuece 40 min. Licua con caldo de " +
                    "cocción y el aceite. Cuela. Sin sal.",
            kcal = 75, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 8,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020",
            alergenos = emptyList()
        ),

        // ── 8-9 meses CENA ───────────────────────────────────────────────────

        RecetaMexicana(
            nombre       = "Caldo de pollo simple con verduras licuadas",
            ingredientes = listOf(
                "30 g de pechuga de pollo",
                "1/4 pieza de zanahoria",
                "1/4 pieza de calabacita"
            ),
            preparacion  = "Cuece el pollo con zanahoria y calabacita en 1 taza de agua. " +
                    "Retira el pollo y licua las verduras con el caldo. Cuela fino. " +
                    "Servir tibio. Sin sal.",
            kcal = 55, tipoComida = TipoComida.CENA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 8,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020",
            alergenos = emptyList()
        ),

        RecetaMexicana(
            nombre       = "Puré de lenteja con zanahoria (cena)",
            ingredientes = listOf(
                "15 g de lentejas",
                "1/4 pieza de zanahoria",
                "1 cdta de aceite de oliva"
            ),
            preparacion  = "Remoja lentejas 15 min. Cuece con zanahoria 40 min. Licua con " +
                    "caldo de cocción y el aceite. Cuela. Servir tibio. Sin sal.",
            kcal = 85, tipoComida = TipoComida.CENA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 8,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020",
            alergenos = emptyList()
        ),

        // ══════════════════════════════════════════════
        // 9-11 MESES — IMSS Procedimiento Guarderías 2023
        // Yema de huevo ANTES que clara (introducir clara hasta 12 meses)
        // ══════════════════════════════════════════════

        RecetaMexicana(
            nombre       = "Puré de yema de huevo con verduras",
            ingredientes = listOf(
                "1 yema de huevo cocida",
                "2 cdas de puré de zanahoria o papa",
                "1 cdta de aceite de oliva"
            ),
            preparacion  = "Cocer el huevo duro. Separar SOLO la yema (desechar la clara). " +
                    "Mezclar con el puré de verdura y el aceite hasta consistencia suave. " +
                    "Sin sal. IMPORTANTE: no ofrecer la clara hasta los 12 meses " +
                    "(IMSS Procedimiento Guarderías 2023).",
            kcal         = 70,
            tipoComida   = TipoComida.COMIDA,
            nivelMinimo  = NivelIngreso.BASICO,
            edadMinMeses = 9,
            fuente       = "IMSS — Procedimiento Alimentación Guarderías 2023",
            alergenos    = listOf(Alergeno.HUEVO)
        ),

        // ── 9-11 meses CENA ──────────────────────────────────────────────────

        RecetaMexicana(
            nombre       = "Sopa de arroz integral con verduras",
            ingredientes = listOf(
                "2 cdas de arroz integral cocido",
                "1/4 pieza de zanahoria rallada",
                "1/4 pieza de calabacita",
                "1 cdta de aceite de canola"
            ),
            preparacion  = "Cuece arroz en agua. Por separado ablanda las verduras en 1/2 taza " +
                    "de agua. Mezcla todo. Tritura ligeramente con tenedor (textura " +
                    "grumosa para trabajar masticación). Agrega aceite. Sin sal.",
            kcal = 90, tipoComida = TipoComida.CENA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 9,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020",
            alergenos = emptyList()
        ),

        RecetaMexicana(
            nombre       = "Crema de frijol con aceite (cena)",
            ingredientes = listOf(
                "15 g de frijol negro o bayo cocido",
                "1/4 taza de caldo de cocción",
                "1 cdta de aceite de oliva"
            ),
            preparacion  = "Licua los frijoles cocidos con su caldo y el aceite hasta textura " +
                    "suave. Cuela. Calentar a fuego suave. Servir tibio. Sin sal.",
            kcal = 78, tipoComida = TipoComida.CENA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 9,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020",
            alergenos = emptyList()
        ),

        // ══════════════════════════════════════════════
        // 10-12 MESES — IMSS Guía 0-12 meses
        // ══════════════════════════════════════════════

        RecetaMexicana(
            nombre       = "Caldo tlalpeño con aguacate",
            ingredientes = listOf(
                "50 g de pierna o muslo de pollo",
                "1/4 pieza de zanahoria",
                "1/4 pieza de chayote",
                "1/4 pieza de papa blanca",
                "1/2 pieza de jitomate mediano",
                "1/4 pieza de cebolla",
                "1/2 diente de ajo",
                "1 cdta de aceite de canola",
                "1/4 pieza de aguacate"
            ),
            preparacion  = "Pela y pica las verduras. Cuece el pollo en agua con cebolla; " +
                    "una vez cocido retira y pica. Reserva el caldo. Licua jitomate con " +
                    "ajo. En cacerola fríe el caldillo; agrega caldo, verduras y tapa. " +
                    "Cuando hierva añade el pollo. Cuece lento. Sirve con aguacate. " +
                    "Sin sal.",
            kcal = 180, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 10,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020",
            alergenos = emptyList()
        ),

        RecetaMexicana(
            nombre       = "Sopita casera de pasta con carne",
            ingredientes = listOf(
                "1/4 paquete de pasta integral corta",
                "30 g de pulpa de res molida",
                "1 pieza de zanahoria",
                "1/2 pieza de jitomate mediano",
                "1/4 pieza de cebolla",
                "1/2 diente de ajo",
                "1 cdta de aceite de canola"
            ),
            preparacion  = "Pela y pica la zanahoria. Licua jitomate con ajo y cebolla. " +
                    "En cacerola con aceite dora la pasta; agrega carne y zanahoria. " +
                    "Incorpora jitomate molido y 1 taza de agua. Tapa y cuece lento. " +
                    "Sin sal.",
            kcal = 220, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 10,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020",
            alergenos = listOf(Alergeno.TRIGO)
        ),

        RecetaMexicana(
            nombre       = "Arroz a la mexicana",
            ingredientes = listOf(
                "1/4 taza de arroz integral",
                "1/2 pieza de jitomate mediano",
                "1/4 pieza de cebolla mediana",
                "1/2 diente de ajo",
                "1 cdta de aceite de canola"
            ),
            preparacion  = "Remoja el arroz 1 hora y escurre. En cacerola con aceite dora el " +
                    "arroz ligeramente. Licua jitomate con ajo y cebolla. Añade al arroz " +
                    "con 1 taza de agua. Tapa y cuece lento. Sin sal.",
            kcal = 150, tipoComida = TipoComida.DESAYUNO,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 10,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020",
            alergenos = emptyList()
        ),

        RecetaMexicana(
            nombre       = "Arroz con leche y canela",
            ingredientes = listOf(
                "1/4 taza de arroz integral",
                "240 ml de agua hervida",
                "8 medidas de fórmula infantil",
                "1 rama de canela"
            ),
            preparacion  = "Remoja el arroz 1 hora y escurre. Prepara la fórmula según " +
                    "indicaciones del fabricante. En cacerola agrega fórmula, canela y " +
                    "arroz. Tapa y cuece lento. SIN azúcar añadida (OMS/COCO 2023: " +
                    "no azúcar antes de 2 años).",
            kcal = 180, tipoComida = TipoComida.DESAYUNO,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 10,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020",
            alergenos = listOf(Alergeno.LACTEOS)
        ),

        RecetaMexicana(
            nombre       = "Entomatado de pollo con tortilla",
            ingredientes = listOf(
                "50 g de pechuga de pollo",
                "1/2 pieza de jitomate",
                "1/4 pieza de cebolla",
                "1/2 diente de ajo",
                "1 cdta de aceite de canola",
                "1 tortilla de maíz"
            ),
            preparacion  = "Cuece la pechuga con cebolla y ajo. Retira y pica. Licua jitomate. " +
                    "En sartén fríe el caldillo y agrega el pollo. Cuece lento. " +
                    "Servir con tortilla de maíz. Sin sal.",
            kcal = 195, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 10,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020",
            alergenos = listOf(Alergeno.MAIZ)
        ),

        RecetaMexicana(
            nombre       = "Picadillo de res con verduras",
            ingredientes = listOf(
                "50 g de pulpa de res",
                "1/4 pieza de papa blanca",
                "1/4 pieza de zanahoria",
                "1/4 pieza de chayote",
                "1/2 pieza de jitomate",
                "1/4 pieza de cebolla",
                "1 cdta de aceite de canola"
            ),
            preparacion  = "Pela y pica finamente todas las verduras y la carne. Licua jitomate " +
                    "con cebolla. En sartén dora la carne; agrega verduras y caldillo. " +
                    "Tapa y cuece lento. Sin sal.",
            kcal = 200, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 10,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020",
            alergenos = emptyList()
        ),

        RecetaMexicana(
            nombre       = "Sopa Juliana de verduras",
            ingredientes = listOf(
                "1/4 pieza de zanahoria",
                "1/4 pieza de calabacita",
                "1/4 pieza de chayote",
                "1/2 atado de acelgas",
                "15 g de poro",
                "1/2 pieza de jitomate",
                "1 cdta de aceite de canola"
            ),
            preparacion  = "Pela y pica zanahoria y chayote. Pica calabacita y poro. " +
                    "Corta acelgas en tiras. Licua jitomate. Fríe el poro; agrega " +
                    "verduras y jitomate con 1 taza de agua. Al hervor agrega acelgas. " +
                    "Tapa y cuece lento. Sin sal.",
            kcal = 65, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 10,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020",
            alergenos = emptyList()
        ),

        RecetaMexicana(
            nombre       = "Fideos a la boloñesa con verduras",
            ingredientes = listOf(
                "1/4 paquete de pasta integral corta",
                "50 g de pulpa de res molida",
                "1/4 pieza de chayote",
                "1/4 pieza de zanahoria",
                "1/4 pieza de calabacita",
                "1/2 pieza de jitomate",
                "1 cdta de aceite de canola"
            ),
            preparacion  = "Pela y pica zanahoria y chayote. Pica calabacita. Licua jitomate. " +
                    "En cacerola fríe carne moviendo. Agrega verduras y pasta. Añade " +
                    "jitomate y 1/2 taza de agua. Cuece lento. Sin sal.",
            kcal = 280, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 10,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020",
            alergenos = listOf(Alergeno.TRIGO)
        ),

        RecetaMexicana(
            nombre       = "Espagueti campirano con verduras",
            ingredientes = listOf(
                "1/4 paquete de pasta integral larga",
                "1/4 pieza de chayote",
                "1/4 pieza de zanahoria",
                "1/4 pieza de calabacita",
                "1/2 pieza de jitomate",
                "1/4 pieza de cebolla",
                "1 cdta de aceite de canola"
            ),
            preparacion  = "Cuece la pasta en agua con cebolla. Escurre. Pela y pica verduras. " +
                    "Licua jitomate con ajo. Fríe cebolla; incorpora verduras. Agrega " +
                    "jitomate y 1/2 taza de agua. Cuece lento. Incorpora la pasta. " +
                    "Sin sal.",
            kcal = 250, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 10,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020",
            alergenos = listOf(Alergeno.TRIGO)
        ),

        RecetaMexicana(
            nombre       = "Pollo en salsa verde",
            ingredientes = listOf(
                "50 g de pechuga de pollo",
                "1/2 taza de salsa verde de tomatillo",
                "1/4 pieza de cebolla",
                "1/2 diente de ajo",
                "1 cdta de aceite de canola"
            ),
            preparacion  = "Cuece la pechuga en agua con cebolla y ajo. Retira y desmenuza. " +
                    "En sartén agrega el pollo y la salsa verde. Cuece lento hasta " +
                    "espesar. Servir con puré de papa o tortilla blanda. Sin sal.",
            kcal = 165, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 10,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020",
            alergenos = emptyList()
        ),

        RecetaMexicana(
            nombre       = "Albóndigas de res con puré de espinaca",
            ingredientes = listOf(
                "50 g de carne molida de res",
                "1/2 pieza de jitomate",
                "1/4 pieza de papa para el puré",
                "1/2 atado de espinacas",
                "1 cdta de aceite de canola"
            ),
            preparacion  = "Sazona y amasa la carne (sin sal). Forma pequeñas albóndigas. " +
                    "Calienta la salsa de jitomate e introduce las albóndigas. Tapa " +
                    "10 min. Para el puré: cuece papa y tritura con espinacas ablandadas. " +
                    "Servir juntos.",
            kcal = 230, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 11,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020",
            alergenos = emptyList()
        ),

        RecetaMexicana(
            nombre       = "Arroz cubano con frijoles",
            ingredientes = listOf(
                "1/4 taza de arroz integral",
                "1/2 taza de frijol seco",
                "1/4 pieza de cebolla",
                "1 cdta de aceite de canola",
                "1/2 diente de ajo"
            ),
            preparacion  = "Remoja frijoles 1 hora. Cuece con cebolla y ajo. Licua con caldo. " +
                    "Dora el arroz y agrega 1 taza de agua. Cuece lento. Incorpora los " +
                    "frijoles licuados y termina la cocción. Sin sal.",
            kcal = 280, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 10,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020",
            alergenos = emptyList()
        ),

        // ── 10-12 meses CENA ─────────────────────────────────────────────────

        RecetaMexicana(
            nombre       = "Sopa de fideo con verduras (cena bebé)",
            ingredientes = listOf(
                "1/8 paquete de fideo fino",
                "1/4 pieza de zanahoria",
                "1/4 pieza de calabacita",
                "1/4 pieza de cebolla",
                "1/2 pieza de jitomate",
                "1 cdta de aceite de canola"
            ),
            preparacion  = "Dora el fideo en aceite. Licua jitomate con cebolla y agrega. " +
                    "Incorpora las verduras picadas muy finas y 1 taza de agua caliente. " +
                    "Cuece lento tapado 10 min. Textura blanda. Sin sal.",
            kcal = 130, tipoComida = TipoComida.CENA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 10,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020",
            alergenos = listOf(Alergeno.TRIGO)
        ),

        RecetaMexicana(
            nombre       = "Puré de papa con pollo desmenuzado (cena)",
            ingredientes = listOf(
                "1/4 pieza de papa blanca",
                "30 g de pechuga de pollo cocida",
                "1 cdta de aceite de oliva"
            ),
            preparacion  = "Cuece la papa hasta suave. Aplasta con el aceite hasta textura " +
                    "cremosa. Desmenuza el pollo muy fino. Mezcla. Servir tibio. Sin sal.",
            kcal = 120, tipoComida = TipoComida.CENA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 10,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020",
            alergenos = emptyList()
        ),

        // ══════════════════════════════════════════════
        // BRECHA 12-18 MESES
        // IMSS Procedimiento Guarderías 2023 + OMS
        // Huevo completo válido desde 12 meses
        // Leche entera de vaca desde 12 meses (OMS)
        // ══════════════════════════════════════════════

        RecetaMexicana(
            nombre       = "Huevo revuelto con jitomate y tortilla blanda",
            ingredientes = listOf(
                "1 huevo entero",
                "1/2 pieza de jitomate picado",
                "1/4 pieza de cebolla picada",
                "1 cdta de aceite de canola",
                "1 tortilla de maíz blanda"
            ),
            preparacion  = "Calienta el aceite. Acitrona la cebolla y el jitomate 2 min. " +
                    "Bate el huevo completo (clara + yema, válido desde 12 meses) y " +
                    "agrégalo. Revuelve hasta cocido. Poca sal yodada. " +
                    "Servir con tortilla blanda partida en trozos pequeños.",
            kcal = 175, tipoComida = TipoComida.DESAYUNO,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 12,
            fuente    = "IMSS — Procedimiento Alimentación Guarderías 2023",
            alergenos = listOf(Alergeno.HUEVO, Alergeno.MAIZ)
        ),

        RecetaMexicana(
            nombre       = "Atole de maíz con leche entera",
            ingredientes = listOf(
                "2 cdas de masa o harina de maíz",
                "1 taza de leche entera",
                "1 rama de canela"
            ),
            preparacion  = "Disuelve la masa en la leche fría. Agrega la canela. Calienta a " +
                    "fuego medio moviendo constantemente hasta que espese (~8 min). " +
                    "Sin azúcar (OMS/COCO 2023: limitar azúcar antes de 2 años). " +
                    "Bebida energética rica en calcio.",
            kcal = 140, tipoComida = TipoComida.DESAYUNO,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 12,
            fuente    = "IMSS — NutrIMSS Infancia / COCO 2023",
            alergenos = listOf(Alergeno.LACTEOS, Alergeno.MAIZ)
        ),

        RecetaMexicana(
            nombre       = "Taco de frijoles con queso fresco",
            ingredientes = listOf(
                "1 tortilla de maíz blanda",
                "1/4 taza de frijoles cocidos machacados",
                "20 g de queso fresco desmoronado",
                "1 cdta de aceite de canola"
            ),
            preparacion  = "Calienta los frijoles machacados con el aceite en sartén. " +
                    "Unta en la tortilla tibia. Agrega queso fresco. Dobla o enrolla. " +
                    "Fórmula proteína completa de bajo costo: maíz + legumbre + lácteo.",
            kcal = 200, tipoComida = TipoComida.DESAYUNO,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 12,
            fuente    = "IMSS — NutrIMSS Infancia 2020",
            alergenos = listOf(Alergeno.MAIZ, Alergeno.LACTEOS)
        ),

        RecetaMexicana(
            nombre       = "Sopa de pasta con verduras (12-18 meses)",
            ingredientes = listOf(
                "1/4 paquete de pasta corta integral",
                "1/4 pieza de zanahoria",
                "1/4 pieza de calabacita",
                "1/2 pieza de jitomate",
                "1/4 pieza de cebolla",
                "1 cdta de aceite de canola"
            ),
            preparacion  = "Licua jitomate con cebolla. Dora la pasta en aceite. Agrega " +
                    "verduras picadas finas y el jitomate licuado. Añade 1.5 tazas de " +
                    "agua caliente. Cuece tapado a fuego lento hasta que la pasta esté " +
                    "suave. Poca sal yodada.",
            kcal = 210, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 12,
            fuente    = "IMSS — Procedimiento Alimentación Guarderías 2023",
            alergenos = listOf(Alergeno.TRIGO)
        ),

        RecetaMexicana(
            nombre       = "Guisado de pollo con zanahoria y papa",
            ingredientes = listOf(
                "50 g de pechuga de pollo",
                "1/4 pieza de papa",
                "1/4 pieza de zanahoria",
                "1/2 pieza de jitomate",
                "1/4 pieza de cebolla",
                "1 cdta de aceite de canola",
                "1 tortilla de maíz"
            ),
            preparacion  = "Pica pollo y verduras en trozos pequeños. Cuece el pollo en agua. " +
                    "Licua jitomate con cebolla. En sartén dora cebolla, agrega el " +
                    "caldillo, las verduras y el pollo. Cuece tapado hasta suave. " +
                    "Poca sal yodada. Servir con tortilla.",
            kcal = 230, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 12,
            fuente    = "IMSS — Procedimiento Alimentación Guarderías 2023",
            alergenos = listOf(Alergeno.MAIZ)
        ),

        RecetaMexicana(
            nombre       = "Lenteja guisada con zanahoria (12-18 m)",
            ingredientes = listOf(
                "20 g de lentejas secas",
                "1/4 pieza de zanahoria",
                "1/4 pieza de papa",
                "1/4 pieza de jitomate",
                "1/4 pieza de cebolla",
                "1 cdta de aceite de canola"
            ),
            preparacion  = "Remoja lentejas 20 min. Cuece 30 min. Pica verduras finamente. " +
                    "Licua jitomate con cebolla. En olla agrega todo junto y cuece " +
                    "hasta verduras suaves. Poca sal. Consistencia espesa para " +
                    "facilitar la pinza del bebé.",
            kcal = 160, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 12,
            fuente    = "IMSS — NutrIMSS Infancia 2020 / OMS PAHO",
            alergenos = emptyList()
        ),

        RecetaMexicana(
            nombre       = "Quesadilla de queso panela (12-18 m)",
            ingredientes = listOf(
                "1 tortilla de maíz",
                "30 g de queso panela",
                "1/4 pieza de jitomate rebanado"
            ),
            preparacion  = "Calienta el comal. Coloca la tortilla y agrega el queso panela. " +
                    "Dobla a la mitad y calienta por ambos lados hasta que el queso " +
                    "esté derretido. Servir con jitomate rebanado. " +
                    "Calcio + proteína + energía para el lactante mayor.",
            kcal = 195, tipoComida = TipoComida.DESAYUNO,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 12,
            fuente    = "IMSS — Procedimiento Alimentación Guarderías 2023",
            alergenos = listOf(Alergeno.MAIZ, Alergeno.LACTEOS)
        ),

        RecetaMexicana(
            nombre       = "Sopa de verduras con arroz (12-18 m)",
            ingredientes = listOf(
                "2 cdas de arroz cocido",
                "1/4 pieza de calabacita",
                "1/4 pieza de zanahoria",
                "1/2 atado pequeño de espinaca",
                "1/4 pieza de cebolla",
                "1 cdta de aceite de canola"
            ),
            preparacion  = "Pica finamente todas las verduras. Acitrona la cebolla en aceite. " +
                    "Agrega zanahoria y calabacita con 1 taza de agua. Cuece 10 min. " +
                    "Agrega espinaca y el arroz cocido. Cocina 3 min más. Poca sal yodada.",
            kcal = 110, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 12,
            fuente    = "IMSS — Procedimiento Alimentación Guarderías 2023",
            alergenos = emptyList()
        ),

        // ── 12-18 meses CENA ─────────────────────────────────────────────────

        RecetaMexicana(
            nombre       = "Sopa de fideo con jitomate (12-18 m)",
            ingredientes = listOf(
                "1/8 paquete de fideo fino",
                "1/2 pieza de jitomate",
                "1/4 pieza de cebolla",
                "1/2 diente de ajo",
                "1 cdta de aceite de canola"
            ),
            preparacion  = "Dora el fideo en aceite. Licua jitomate con cebolla y ajo. " +
                    "Vierte sobre el fideo y agrega 1 taza de agua caliente. " +
                    "Tapa y cuece lento 8 min. Textura blanda. Poca sal yodada.",
            kcal = 140, tipoComida = TipoComida.CENA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 12,
            fuente    = "IMSS — Procedimiento Alimentación Guarderías 2023",
            alergenos = listOf(Alergeno.TRIGO)
        ),

        RecetaMexicana(
            nombre       = "Crema de chayote con aceite de oliva",
            ingredientes = listOf(
                "1/2 pieza de chayote",
                "1/4 pieza de cebolla",
                "1 cdta de aceite de oliva"
            ),
            preparacion  = "Pela el chayote y trocea. Cuece con cebolla en 1 taza de agua 10 min. " +
                    "Licua con agua de cocción y el aceite hasta textura cremosa. " +
                    "Cuela si es necesario. Servir tibio. Poca sal yodada.",
            kcal = 75, tipoComida = TipoComida.CENA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 12,
            fuente    = "IMSS — Procedimiento Alimentación Guarderías 2023",
            alergenos = emptyList()
        ),

        RecetaMexicana(
            nombre       = "Caldo de res con verduras (cena 12 m)",
            ingredientes = listOf(
                "30 g de chambarete de res",
                "1/4 pieza de zanahoria",
                "1/4 pieza de papa",
                "1/4 pieza de chayote",
                "1/4 pieza de elote"
            ),
            preparacion  = "Cuece el chambarete en agua hasta suave. Agrega las verduras " +
                    "cortadas en trozos pequeños. Cuece 15 min más. Deshuesa la carne " +
                    "y pica. Poca sal yodada. Servir tibio con caldo y verduras.",
            kcal = 145, tipoComida = TipoComida.CENA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 12,
            fuente    = "IMSS — Procedimiento Alimentación Guarderías 2023",
            alergenos = emptyList()
        ),

        // ══════════════════════════════════════════════
        // 18 MESES A 3 AÑOS — SEP/SS Nuevo León
        // ══════════════════════════════════════════════

        RecetaMexicana(
            nombre       = "Huevo revuelto con tomate y taco de frijol",
            ingredientes = listOf(
                "1 huevo + 1 clara",
                "1/2 cdta de mantequilla sin sal",
                "1/2 pieza de tomate picado",
                "1/3 taza de frijoles molidos",
                "1 tortilla de maíz"
            ),
            preparacion  = "Calentar sartén con mantequilla. Agregar tomate hasta ablandar. " +
                    "Añadir huevo completo (clara + yema — válido desde 12 meses) y " +
                    "revolver. Preparar taquito con frijoles. Acompañar con 1/2 taza " +
                    "de leche entera.",
            kcal = 325, tipoComida = TipoComida.DESAYUNO,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 18,
            fuente    = "SEP/SS Nuevo León — Programa Salud Para Aprender",
            alergenos = listOf(Alergeno.HUEVO, Alergeno.LACTEOS, Alergeno.MAIZ)
        ),

        RecetaMexicana(
            nombre       = "Mollete con aguacate y frijoles",
            ingredientes = listOf(
                "1 rebanada de pan integral",
                "1/4 taza de frijoles molidos",
                "50 g de queso panela rallado",
                "3 rebanadas de aguacate",
                "1/2 pieza de tomate picado"
            ),
            preparacion  = "Tostar el pan. Untar frijoles. Agregar queso y calentar hasta que " +
                    "se derrita. Agregar tomate y decorar con aguacate.",
            kcal = 333, tipoComida = TipoComida.DESAYUNO,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 18,
            fuente    = "SEP/SS Nuevo León — Programa Salud Para Aprender",
            alergenos = listOf(Alergeno.TRIGO, Alergeno.LACTEOS)
        ),

        RecetaMexicana(
            nombre       = "Quesadilla con verduras y frijoles",
            ingredientes = listOf(
                "1 tortilla de maíz",
                "40 g de queso manchego rallado",
                "1 cdta de aceite",
                "1/2 pieza de tomate picado",
                "1/4 taza de frijoles molidos",
                "Cebolla y cilantro al gusto"
            ),
            preparacion  = "Doblar tortilla con queso para formar quesadilla. En sartén dorar " +
                    "cebolla, agregar tomate y cilantro. Calentar frijoles y agregar a " +
                    "la quesadilla.",
            kcal = 318, tipoComida = TipoComida.DESAYUNO,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 18,
            fuente    = "SEP/SS Nuevo León — Programa Salud Para Aprender",
            alergenos = listOf(Alergeno.LACTEOS, Alergeno.MAIZ)
        ),

        RecetaMexicana(
            nombre       = "Mini hotcakes de avena con fresas",
            ingredientes = listOf(
                "1/2 taza de avena en hojuelas",
                "1 huevo",
                "1/2 taza de leche descremada",
                "1/2 cdta de polvo para hornear",
                "1 cdta de vainilla",
                "1 cdta de canela",
                "1 cdta de aceite",
                "1/4 taza de fresas picadas"
            ),
            preparacion  = "Licuar la avena hasta obtener harina. Mezclar con leche, huevo, " +
                    "polvo para hornear, vainilla y canela. Sin azúcar añadida " +
                    "(COCO 2023: limitar azúcar en menores de 3 años). Calentar sartén " +
                    "engrasado. Verter en círculos pequeños. Cocinar 2-3 min por lado. " +
                    "Servir con fresas picadas.",
            kcal = 336, tipoComida = TipoComida.DESAYUNO,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 24,
            fuente    = "SEP/SS Nuevo León — Programa Salud Para Aprender",
            alergenos = listOf(Alergeno.HUEVO, Alergeno.LACTEOS, Alergeno.TRIGO)
        ),

        RecetaMexicana(
            nombre       = "Caldo de pollo con verduras y arroz",
            ingredientes = listOf(
                "1 pierna de pollo (60 g)",
                "1/2 pieza de calabacita",
                "1/2 taza de zanahoria",
                "1/4 taza de apio",
                "1/4 taza de arroz cocido",
                "1/2 pieza de papa",
                "1 tortilla de maíz"
            ),
            preparacion  = "En olla agregar agua y el pollo. Picar verduras y añadir; primero " +
                    "la papa y después las demás. Preparar arroz aparte y añadir al caldo " +
                    "al servir. Acompañar con tortilla. Poca sal yodada.",
            kcal = 373, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 18,
            fuente    = "SEP/SS Nuevo León — Programa Salud Para Aprender",
            alergenos = listOf(Alergeno.MAIZ)
        ),

        RecetaMexicana(
            nombre       = "Albóndigas de res con arroz integral",
            ingredientes = listOf(
                "60 g de carne molida cruda",
                "1/2 pieza de calabacita",
                "1/2 taza de brócoli",
                "1/2 taza de salsa de tomate natural",
                "1/4 taza de arroz integral cocido"
            ),
            preparacion  = "Mezclar calabacita con brócoli y carne. Formar albóndigas y " +
                    "refrigerar 10 min. Calentar salsa e introducir albóndigas. Tapar " +
                    "10 minutos. Servir con arroz y agua de jamaica sin azúcar.",
            kcal = 410, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 24,
            fuente    = "SEP/SS Nuevo León — Programa Salud Para Aprender",
            alergenos = emptyList()
        ),

        // ── 18-36 meses CENA ─────────────────────────────────────────────────

        RecetaMexicana(
            nombre       = "Sopa de lenteja con verduras (cena 18 m)",
            ingredientes = listOf(
                "20 g de lentejas",
                "1/4 pieza de zanahoria",
                "1/4 pieza de papa",
                "1/4 pieza de cebolla",
                "1/4 pieza de jitomate",
                "1 cdta de aceite de canola"
            ),
            preparacion  = "Remoja lentejas 20 min. Cuece con todas las verduras picadas y " +
                    "la cebolla hasta suave (~35 min). Aplasta ligeramente con cuchara. " +
                    "Agrega aceite. Poca sal yodada. Rica en hierro y proteína vegetal.",
            kcal = 165, tipoComida = TipoComida.CENA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 18,
            fuente    = "SEP/SS Nuevo León — Programa Salud Para Aprender / IMSS NutrIMSS",
            alergenos = emptyList()
        ),

        RecetaMexicana(
            nombre       = "Crema de calabaza con frijol",
            ingredientes = listOf(
                "1 pieza de calabacita",
                "1/4 taza de frijol cocido",
                "1/4 pieza de cebolla",
                "1 cdta de aceite de oliva"
            ),
            preparacion  = "Cuece calabacita y cebolla hasta suave. Licua con frijoles y aceite. " +
                    "Agrega agua de cocción hasta consistencia deseada. Calentar y " +
                    "servir. Poca sal yodada. Proteína + fibra + energía.",
            kcal = 135, tipoComida = TipoComida.CENA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 18,
            fuente    = "IMSS — NutrIMSS Infancia 2020",
            alergenos = emptyList()
        ),

        RecetaMexicana(
            nombre       = "Sopa de papa con poro (cena 2 años)",
            ingredientes = listOf(
                "1/2 pieza de papa",
                "30 g de poro (parte blanca)",
                "1/4 pieza de cebolla",
                "1 cdta de aceite de oliva",
                "1/2 taza de leche entera"
            ),
            preparacion  = "Pica papa, poro y cebolla. Sofríe poro y cebolla en aceite. " +
                    "Agrega papa y 1 taza de agua. Cuece 15 min. Licua parcialmente " +
                    "con la leche. Servir tibio. Poca sal yodada.",
            kcal = 180, tipoComida = TipoComida.CENA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 24,
            fuente    = "SEP/SS Nuevo León — Programa Salud Para Aprender",
            alergenos = listOf(Alergeno.LACTEOS)
        ),

        // ══════════════════════════════════════════════
        // PREESCOLAR 3-6 y ESCOLAR 6-12 — SEP/SS NL
        // ══════════════════════════════════════════════

        RecetaMexicana(
            nombre       = "Hotcake de harina integral con fresas",
            ingredientes = listOf(
                "1/2 taza de harina integral",
                "1 huevo",
                "1/4 taza de leche descremada",
                "1 cda de vainilla",
                "5 fresas picadas",
                "1 cdta de chispas de chocolate obscuro"
            ),
            preparacion  = "Mezclar harina, huevo, leche y vainilla hasta homogenizar. Calentar " +
                    "comal engrasado. Verter en círculos medianos. Cocinar 2-3 min por " +
                    "lado. Servir con fresas y chocolate. Mínimo azúcar añadida.",
            kcal = 415, tipoComida = TipoComida.DESAYUNO,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 36,
            fuente    = "SEP/SS Nuevo León — Programa Salud Para Aprender",
            alergenos = listOf(Alergeno.HUEVO, Alergeno.LACTEOS, Alergeno.TRIGO)
        ),

        RecetaMexicana(
            nombre       = "Tacos de huevo con nopales y pico de gallo",
            ingredientes = listOf(
                "2 tortillas de harina integral",
                "2 huevos",
                "1/2 taza de nopales picados",
                "1/3 taza de cebolla picada",
                "1/4 taza de tomate picado",
                "1 cdta de aceite",
                "2 cdtas de frijoles molidos"
            ),
            preparacion  = "Calentar aceite. Agregar nopales hasta que pierdan el agua. " +
                    "Añadir cebolla, tomate y huevo. Revolver e integrar. " +
                    "Formar taquitos. Acompañar con frijoles.",
            kcal = 397, tipoComida = TipoComida.DESAYUNO,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 36,
            fuente    = "SEP/SS Nuevo León — Programa Salud Para Aprender",
            alergenos = listOf(Alergeno.HUEVO, Alergeno.TRIGO)
        ),

        RecetaMexicana(
            nombre       = "Avena reposada con manzana y cacao",
            ingredientes = listOf(
                "1/2 taza de avena en hojuelas",
                "1 taza de leche descremada",
                "Cacao sin azúcar al gusto",
                "Canela al gusto",
                "1 manzana pequeña picada"
            ),
            preparacion  = "Mezclar avena, leche, cacao sin azúcar y canela en contenedor. " +
                    "Tapar y reposar en refrigerador toda la noche. Por la mañana " +
                    "calentar y agregar manzana picada. Puede consumirse fría. " +
                    "La dulzura natural de la manzana es suficiente.",
            kcal = 270, tipoComida = TipoComida.DESAYUNO,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 36,
            fuente    = "SEP/SS Nuevo León — Programa Salud Para Aprender / COCO 2023",
            alergenos = listOf(Alergeno.LACTEOS, Alergeno.TRIGO, Alergeno.FRUCTOSA)
        ),

        RecetaMexicana(
            nombre       = "Chilaquiles con pollo y queso panela",
            ingredientes = listOf(
                "4 tortillas de maíz",
                "90 g de pechuga de pollo desmenuzada",
                "40 g de queso panela rallado",
                "1 taza de salsa de tomate natural",
                "3 cdtas de aceite"
            ),
            preparacion  = "Cortar tortillas en cuadritos. Dorar en aceite. Agregar pollo " +
                    "cocido y salsa. Añadir queso panela al final.",
            kcal = 565, tipoComida = TipoComida.DESAYUNO,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 36,
            fuente    = "SEP/SS Nuevo León — Programa Salud Para Aprender",
            alergenos = listOf(Alergeno.MAIZ, Alergeno.LACTEOS)
        ),

        RecetaMexicana(
            nombre       = "Enchiladas suizas con ensalada",
            ingredientes = listOf(
                "4 tortillas de maíz",
                "90 g de pechuga desmenuzada",
                "1 taza de salsa verde",
                "40 g de queso panela rallado",
                "1 taza de lechuga",
                "1/3 pieza de aguacate",
                "1/2 pieza de tomate rebanado"
            ),
            preparacion  = "Cocer pechuga con ajo, cebolla y sal. Desmenuzar. Pasar tortillas " +
                    "por aceite, rellenar con pollo y envolver. Bañar con salsa verde y " +
                    "queso. Servir con ensalada.",
            kcal = 658, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 36,
            fuente    = "SEP/SS Nuevo León — Programa Salud Para Aprender",
            alergenos = listOf(Alergeno.MAIZ, Alergeno.LACTEOS)
        ),

        RecetaMexicana(
            nombre       = "Caldo Tlalpeño con arroz",
            ingredientes = listOf(
                "90 g de pechuga de pollo",
                "1/2 pieza de papa",
                "1/2 taza de zanahoria",
                "1/3 taza de calabacita",
                "2 cdtas de apio",
                "1/3 pieza de aguacate",
                "1/2 taza de arroz blanco",
                "3 tortillas de maíz"
            ),
            preparacion  = "Cocer pollo en agua con ajo y cebolla. Picar verduras en cubos y " +
                    "agregar al caldo. Servir con arroz, aguacate y tortillas.",
            kcal = 637, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 36,
            fuente    = "SEP/SS Nuevo León — Programa Salud Para Aprender",
            alergenos = listOf(Alergeno.MAIZ)
        ),

        RecetaMexicana(
            nombre       = "Pollo en salsa verde con brócoli y arroz",
            ingredientes = listOf(
                "60 g de pechuga desmenuzada",
                "1 taza de brócoli",
                "1/2 taza de salsa verde de tomatillo",
                "1 cdta de aceite",
                "1/2 taza de arroz rojo",
                "2 tortillas de maíz"
            ),
            preparacion  = "Cocer pechuga en agua con ajo y cebolla. Desmenuzar. Calentar " +
                    "aceite, agregar pollo y dorar. Agregar salsa verde. Servir con " +
                    "arroz rojo y tortillas.",
            kcal = 471, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 36,
            fuente    = "SEP/SS Nuevo León — Programa Salud Para Aprender",
            alergenos = listOf(Alergeno.MAIZ)
        ),

        RecetaMexicana(
            nombre       = "Tacos de fajitas de pollo con pimiento",
            ingredientes = listOf(
                "60 g de pollo cocido en tiras",
                "2 tortillas de maíz",
                "1/4 pieza de cebolla en tiras",
                "1/4 pieza de pimiento verde",
                "1/4 pieza de pimiento amarillo",
                "1 cdta de aceite"
            ),
            preparacion  = "Calentar aceite. Agregar pollo y verduras en tiras. Tapar hasta " +
                    "cocer. Calentar tortillas y formar tacos.",
            kcal = 487, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 36,
            fuente    = "SEP/SS Nuevo León — Programa Salud Para Aprender",
            alergenos = listOf(Alergeno.MAIZ)
        ),

        RecetaMexicana(
            nombre       = "Tostadas de salpicón de res",
            ingredientes = listOf(
                "105 g de carne deshebrada",
                "4 tostadas horneadas",
                "2 piezas de tomate picado",
                "1/4 taza de cebolla picada",
                "Cilantro al gusto",
                "1/2 pieza de aguacate",
                "Jugo de limón y aceite de oliva"
            ),
            preparacion  = "Cocer carne con ajo, cebolla y sal. Deshebrar. Mezclar con tomate, " +
                    "cebolla, cilantro y aderezo de limón. Agregar aguacate. " +
                    "Servir sobre tostadas horneadas.",
            kcal = 650, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 48,
            fuente    = "SEP/SS Nuevo León — Programa Salud Para Aprender",
            alergenos = listOf(Alergeno.MAIZ)
        ),

        RecetaMexicana(
            nombre       = "Sincronizadas con pavo y queso panela",
            ingredientes = listOf(
                "4 tortillas de maíz",
                "4 rebanadas de pechuga de pavo",
                "50 g de queso panela",
                "1/3 pieza de aguacate",
                "1/2 pieza de tomate"
            ),
            preparacion  = "Calentar tortilla en comal. Agregar pavo y queso panela. Colocar " +
                    "otra tortilla encima. Voltear por ambos lados. Servir con aguacate.",
            kcal = 571, tipoComida = TipoComida.DESAYUNO,
            nivelMinimo = NivelIngreso.MEDIO_BAJO, edadMinMeses = 36,
            fuente    = "SEP/SS Nuevo León — Programa Salud Para Aprender",
            alergenos = listOf(Alergeno.MAIZ, Alergeno.LACTEOS)
        ),

        RecetaMexicana(
            nombre       = "Licuado de plátano con avena y amaranto",
            ingredientes = listOf(
                "3/4 pieza de plátano",
                "1/4 taza de amaranto",
                "1 taza de leche descremada",
                "1/4 taza de avena",
                "10 almendras"
            ),
            preparacion  = "Agregar todos los ingredientes a la licuadora. Licuar hasta mezcla " +
                    "uniforme. Puede agregar hielo al gusto. Sin azúcar añadida.",
            kcal = 407, tipoComida = TipoComida.DESAYUNO,
            nivelMinimo = NivelIngreso.MEDIO_BAJO, edadMinMeses = 36,
            fuente    = "SEP/SS Nuevo León — Programa Salud Para Aprender",
            alergenos = listOf(Alergeno.LACTEOS, Alergeno.TRIGO, Alergeno.NUECES)
        ),

        // ── Preescolar/Escolar CENA ───────────────────────────────────────────

        RecetaMexicana(
            nombre       = "Sopa de flor de calabaza con elote",
            ingredientes = listOf(
                "1 taza de flores de calabaza limpias",
                "1/4 taza de granos de elote",
                "1/4 pieza de cebolla",
                "1/2 pieza de jitomate",
                "1 cdta de aceite de canola"
            ),
            preparacion  = "Sofríe cebolla y jitomate. Agrega flores de calabaza y elote. " +
                    "Añade 1.5 tazas de agua caliente. Cuece lento 10 min. Poca sal yodada. " +
                    "Receta tradicional mexicana rica en vitaminas A y C.",
            kcal = 95, tipoComida = TipoComida.CENA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 36,
            fuente    = "Secretaría de Salud — Guías Alimentarias México 2023 / IMSS NutrIMSS",
            regiones  = listOf(RegionMexico.GENERAL),
            alergenos = listOf(Alergeno.MAIZ)
        ),

        RecetaMexicana(
            nombre       = "Sopa de papa con leche y poro",
            ingredientes = listOf(
                "1 pieza de papa mediana",
                "50 g de poro",
                "1/2 taza de leche entera",
                "1/4 pieza de cebolla",
                "1 cdta de aceite de oliva"
            ),
            preparacion  = "Pela y trocea la papa. Pica poro y cebolla. Sofríe en aceite. " +
                    "Agrega papa y 1 taza de agua. Cuece 15 min. Licua parcialmente. " +
                    "Incorpora leche y calienta sin hervir. Poca sal yodada.",
            kcal = 190, tipoComida = TipoComida.CENA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 36,
            fuente    = "SEP/SS Nuevo León — Programa Salud Para Aprender",
            alergenos = listOf(Alergeno.LACTEOS)
        ),

        RecetaMexicana(
            nombre       = "Caldo de frijol con epazote y tortilla",
            ingredientes = listOf(
                "1/2 taza de frijol negro cocido",
                "1/2 taza de caldo de frijol",
                "Epazote al gusto",
                "1/4 pieza de cebolla",
                "1 cdta de aceite de canola",
                "2 tortillas de maíz"
            ),
            preparacion  = "Calienta el caldo de frijol con epazote y cebolla. Añade los " +
                    "frijoles enteros. Sazona con aceite. Poca sal yodada. " +
                    "Servir con tortillas. Cena ligera y nutritiva tradicional mexicana.",
            kcal = 220, tipoComida = TipoComida.CENA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 36,
            fuente    = "IMSS — NutrIMSS Infancia 2020 / Cultura.gob.mx",
            alergenos = listOf(Alergeno.MAIZ)
        ),

        RecetaMexicana(
            nombre       = "Sopa de verdura con pasta (cena escolar)",
            ingredientes = listOf(
                "1/4 paquete de pasta corta",
                "1/4 pieza de zanahoria",
                "1/4 pieza de calabacita",
                "1/4 pieza de chayote",
                "1/2 pieza de jitomate",
                "1 cdta de aceite de canola"
            ),
            preparacion  = "Dora la pasta en aceite. Licua jitomate con cebolla. Agrega todas " +
                    "las verduras picadas en cubos y el jitomate licuado. Añade 1.5 " +
                    "tazas de agua. Cuece lento tapado 12 min. Poca sal yodada.",
            kcal = 210, tipoComida = TipoComida.CENA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 36,
            fuente    = "SEP/SS Nuevo León — Programa Salud Para Aprender",
            alergenos = listOf(Alergeno.TRIGO)
        ),

        // ══════════════════════════════════════════════
        // NIVEL MEDIO — Secretaría de Salud + IMSS NutrIMSS + SEP
        // ══════════════════════════════════════════════

        RecetaMexicana(
            nombre       = "Filete de mojarra al vapor con verduras",
            ingredientes = listOf(
                "80 g de filete de mojarra o tilapia",
                "1/4 pieza de limón",
                "1/2 taza de brócoli",
                "1/4 pieza de zanahoria",
                "1 cdta de aceite de oliva",
                "2 tortillas de maíz"
            ),
            preparacion  = "Sazona el filete con limón. Cocina al vapor 8-10 min hasta que " +
                    "se deshaga fácilmente. Cuece brócoli y zanahoria al vapor 5 min. " +
                    "Baña con aceite de oliva. Servir con tortillas. " +
                    "Rico en omega-3 y proteína de alto valor biológico.",
            kcal = 280, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.MEDIO, edadMinMeses = 12,
            fuente    = "Secretaría de Salud — Guías Alimentarias México 2023 / IMSS NutrIMSS",
            alergenos = listOf(Alergeno.PESCADO, Alergeno.MAIZ)
        ),

        RecetaMexicana(
            nombre       = "Milanesa de pollo al horno con ensalada",
            ingredientes = listOf(
                "80 g de pechuga de pollo aplanada",
                "2 cdas de avena molida (empanizador)",
                "1 huevo",
                "1 taza de lechuga",
                "1/2 pieza de jitomate",
                "1/4 pieza de aguacate",
                "1 cdta de aceite de oliva"
            ),
            preparacion  = "Pasa el pollo por huevo batido y avena molida. Coloca en charola " +
                    "con aceite y hornea a 180°C por 20 min. Prepara ensalada con " +
                    "lechuga, jitomate y aguacate. Aderezo de limón. Sin fritura.",
            kcal = 380, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.MEDIO, edadMinMeses = 36,
            fuente    = "SEP/SS Nuevo León — Programa Salud Para Aprender",
            alergenos = listOf(Alergeno.HUEVO, Alergeno.TRIGO)
        ),

        RecetaMexicana(
            nombre       = "Tinga de res con tostadas y verduras",
            ingredientes = listOf(
                "90 g de res deshebrada",
                "1 taza de jitomate",
                "1/4 pieza de cebolla",
                "1 chile chipotle en adobo (opcional/suave)",
                "4 tostadas horneadas",
                "1/3 pieza de aguacate",
                "Lechuga al gusto"
            ),
            preparacion  = "Cuece la res en agua con ajo. Deshebra. Licua jitomate con cebolla " +
                    "y chile chipotle (muy poco). Sofríe y agrega la carne. Cuece lento " +
                    "15 min. Servir en tostadas con lechuga y aguacate.",
            kcal = 510, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.MEDIO, edadMinMeses = 48,
            fuente    = "Secretaría de Salud — Guías Alimentarias México 2023",
            alergenos = listOf(Alergeno.MAIZ)
        ),

        RecetaMexicana(
            nombre       = "Yogur natural con fruta y amaranto",
            ingredientes = listOf(
                "1/2 taza de yogur natural sin azúcar",
                "1/4 taza de amaranto tostado",
                "1/2 plátano rebanado",
                "5 fresas picadas"
            ),
            preparacion  = "Servir el yogur en tazón. Agregar el amaranto tostado para textura " +
                    "crujiente. Decorar con plátano y fresas. Sin azúcar añadida. " +
                    "El amaranto aporta proteína completa y calcio.",
            kcal = 210, tipoComida = TipoComida.COLACION,
            nivelMinimo = NivelIngreso.MEDIO, edadMinMeses = 24,
            fuente    = "IMSS — NutrIMSS Infancia / Secretaría de Salud 2023",
            alergenos = listOf(Alergeno.LACTEOS)
        ),

        RecetaMexicana(
            nombre       = "Omelette de verduras con queso panela",
            ingredientes = listOf(
                "2 huevos",
                "1/4 pieza de pimiento",
                "1/4 pieza de cebolla",
                "1/4 pieza de jitomate",
                "30 g de queso panela rallado",
                "1 cdta de aceite de oliva"
            ),
            preparacion  = "Bate los huevos. Pica finamente las verduras. Saltea las verduras " +
                    "en aceite 2 min. Vierte el huevo sobre las verduras en sartén. " +
                    "Agrega queso panela. Dobla el omelette al coagular. Poca sal yodada.",
            kcal = 290, tipoComida = TipoComida.DESAYUNO,
            nivelMinimo = NivelIngreso.MEDIO, edadMinMeses = 36,
            fuente    = "Secretaría de Salud — Guías Alimentarias México 2023",
            alergenos = listOf(Alergeno.HUEVO, Alergeno.LACTEOS)
        ),

        RecetaMexicana(
            nombre       = "Pasta integral con atún y verduras al horno",
            ingredientes = listOf(
                "1/2 taza de pasta integral",
                "1 lata pequeña de atún en agua",
                "1/4 pieza de calabacita",
                "1/4 pieza de zanahoria",
                "1/4 taza de jitomate en cubos",
                "1 cdta de aceite de oliva"
            ),
            preparacion  = "Cuece la pasta al dente. Escurre el atún. Saltea verduras picadas " +
                    "en aceite 5 min. Mezcla con pasta y atún. Hornea a 180°C por " +
                    "10 min opcional. Poca sal yodada.",
            kcal = 390, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.MEDIO, edadMinMeses = 36,
            fuente    = "SEP/SS Nuevo León — Programa Salud Para Aprender",
            alergenos = listOf(Alergeno.TRIGO, Alergeno.PESCADO)
        ),

        RecetaMexicana(
            nombre       = "Crema de zanahoria con jengibre (cena MEDIO)",
            ingredientes = listOf(
                "2 piezas de zanahoria",
                "1/4 pieza de cebolla",
                "1/4 cdta de jengibre fresco rallado",
                "1 cdta de aceite de oliva",
                "1/2 taza de leche entera"
            ),
            preparacion  = "Pela y trocea zanahorias. Cuece con cebolla en agua 15 min. Licua " +
                    "con jengibre, aceite y leche. Calienta sin hervir. Poca sal yodada. " +
                    "El jengibre añade antioxidantes. Sin crema.",
            kcal = 165, tipoComida = TipoComida.CENA,
            nivelMinimo = NivelIngreso.MEDIO, edadMinMeses = 36,
            fuente    = "Secretaría de Salud — Guías Alimentarias México 2023",
            alergenos = listOf(Alergeno.LACTEOS)
        ),

        // ══════════════════════════════════════════════
        // OMEGA-3 ECONÓMICO — Alternativas al salmón  ← NUEVO v2.2
        // Sierra ~$200/kg · Sardina en lata ~$14–48
        // Fuente: ANPEC mar 2026 / PROFECO ene 2026
        // ══════════════════════════════════════════════

        RecetaMexicana(
            nombre       = "Filete de sierra a la plancha con verduras",
            ingredientes = listOf(
                "80 g de filete de sierra",
                "1/4 pieza de limón",
                "1/2 taza de brócoli",
                "1/4 pieza de zanahoria",
                "1 cdta de aceite de oliva",
                "2 tortillas de maíz"
            ),
            preparacion  = "Sazona el filete de sierra con limón. Cocina en sartén con aceite " +
                    "a fuego medio 3–4 min por lado. Cuece brócoli y zanahoria al vapor " +
                    "5 min. Servir con tortillas. " +
                    "NOTA NUTRICIONAL: La sierra aporta omega-3 similar al salmón a " +
                    "~1/3 del costo (ANPEC 2026: ~$200/kg vs >$300/kg del salmón). " +
                    "Proteína de alto valor biológico y vitamina D.",
            kcal = 270, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.MEDIO, edadMinMeses = 12,
            fuente    = "Secretaría de Salud — Guías Alimentarias México 2023 / ANPEC 2026",
            alergenos = listOf(Alergeno.PESCADO, Alergeno.MAIZ)
        ),

        RecetaMexicana(
            nombre       = "Sardina en jitomate con arroz y verduras",
            ingredientes = listOf(
                "1 lata de sardina en jitomate (125 g aprox.)",
                "1/4 taza de arroz integral",
                "1/4 pieza de zanahoria",
                "1/4 pieza de calabacita",
                "1 cdta de aceite de canola",
                "2 tortillas de maíz"
            ),
            preparacion  = "Cuece el arroz. Saltea zanahoria y calabacita en aceite 5 min. " +
                    "Incorpora la sardina escurrida y desmiga. Mezcla con el arroz. " +
                    "Poca sal yodada. Servir con tortillas. " +
                    "NOTA NUTRICIONAL: La sardina en lata es la proteína animal más " +
                    "económica con omega-3 disponible (PROFECO 2026: $14–48/lata). " +
                    "Rica en calcio (huesos blandos comestibles) y vitamina D.",
            kcal = 310, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 18,
            fuente    = "Secretaría de Salud — Guías Alimentarias México 2023 / PROFECO 2026",
            alergenos = listOf(Alergeno.PESCADO, Alergeno.MAIZ)
        ),

        // ══════════════════════════════════════════════
        // NIVEL ALTO — SS México + IMSS NutrIMSS
        // ══════════════════════════════════════════════

        // NOTA v2.2: Receta de salmón actualizada con nota de precio 2026
        RecetaMexicana(
            nombre       = "Salmón al vapor con puré de camote",
            ingredientes = listOf(
                "80 g de filete de salmón",
                "1 pieza de camote mediano",
                "1/2 taza de espinacas",
                "1 cdta de aceite de oliva",
                "1/4 pieza de limón"
            ),
            preparacion  = "Sazona el salmón con limón. Cocina al vapor 10 min. Cuece el camote " +
                    "y tritura con aceite de oliva hasta textura suave. Saltea espinacas " +
                    "con ajo 2 min. Rico en omega-3, vitamina A y hierro. " +
                    "NOTA DE COSTO 2026: Salmón >$300/kg en Puebla (Distribuidora Cholula). " +
                    "Si el presupuesto es ajustado, sustituir por sierra (~$200/kg) " +
                    "o sardina en lata (~$14–48) con perfil nutricional similar.",
            kcal = 380, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.ALTO, edadMinMeses = 12,
            fuente    = "Secretaría de Salud — Guías Alimentarias México 2023 / OMS PAHO",
            alergenos = listOf(Alergeno.PESCADO)
        ),

        RecetaMexicana(
            nombre       = "Bowl de quinoa con pollo y aguacate",
            ingredientes = listOf(
                "1/4 taza de quinoa",
                "80 g de pechuga de pollo",
                "1/3 pieza de aguacate",
                "1/2 taza de jitomate cherry",
                "1/4 taza de pepino",
                "Jugo de limón y aceite de oliva"
            ),
            preparacion  = "Cuece quinoa en 1/2 taza de agua 15 min. Cuece pechuga a la plancha " +
                    "sin aceite extra. Pica jitomate y pepino. Monta el bowl con quinoa " +
                    "de base, pollo, verduras y aguacate. Aderezo de limón y aceite.",
            kcal = 450, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.ALTO, edadMinMeses = 48,
            fuente    = "Secretaría de Salud — Guías Alimentarias México 2023",
            alergenos = emptyList()
        ),

        RecetaMexicana(
            nombre       = "Huevos benedictinos con aguacate (versión saludable)",
            ingredientes = listOf(
                "2 huevos",
                "1/3 pieza de aguacate",
                "1/2 pieza de jitomate",
                "1 rebanada de pan integral tostado",
                "1 cdta de aceite de oliva"
            ),
            preparacion  = "Pocha los huevos en agua con un chorro de vinagre (2-3 min). " +
                    "Tuesta el pan. Aplasta el aguacate con limón. Monta: pan tostado, " +
                    "aguacate, jitomate rebanado y huevo pochado encima. Poca sal yodada.",
            kcal = 420, tipoComida = TipoComida.DESAYUNO,
            nivelMinimo = NivelIngreso.ALTO, edadMinMeses = 48,
            fuente    = "Secretaría de Salud — Guías Alimentarias México 2023",
            alergenos = listOf(Alergeno.HUEVO, Alergeno.TRIGO)
        ),

        RecetaMexicana(
            nombre       = "Caldo tlalpeño premium con garbanzo",
            ingredientes = listOf(
                "100 g de pechuga de pollo",
                "1/4 taza de garbanzo cocido",
                "1/4 pieza de zanahoria",
                "1/4 pieza de chayote",
                "1 chile chipotle seco remojado (opcional/suave)",
                "1/3 pieza de aguacate",
                "2 tortillas de maíz"
            ),
            preparacion  = "Cuece el pollo en agua con ajo. Agrega garbanzo y verduras. " +
                    "Añade chile chipotle rehydratado y colado para sabor suave. " +
                    "Cuece 15 min. Desmenuza el pollo. Servir con aguacate y tortillas.",
            kcal = 510, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.ALTO, edadMinMeses = 48,
            fuente    = "IMSS — NutrIMSS Infancia / Cultura.gob.mx",
            alergenos = listOf(Alergeno.MAIZ)
        ),

        RecetaMexicana(
            nombre       = "Smoothie de yogur con espinaca y fruta",
            ingredientes = listOf(
                "1/2 taza de yogur griego natural sin azúcar",
                "1 taza de espinaca baby",
                "1/2 plátano",
                "1/4 taza de mango",
                "1/4 taza de leche entera"
            ),
            preparacion  = "Licua todos los ingredientes hasta mezcla homogénea. Sin azúcar " +
                    "añadida. La fruta aporta dulzura natural. Rico en proteína, calcio, " +
                    "hierro y vitaminas. Servir inmediatamente.",
            kcal = 255, tipoComida = TipoComida.COLACION,
            nivelMinimo = NivelIngreso.ALTO, edadMinMeses = 24,
            fuente    = "Secretaría de Salud — Guías Alimentarias México 2023",
            alergenos = listOf(Alergeno.LACTEOS)
        ),

        RecetaMexicana(
            nombre       = "Sopa de lentejas rojas con espinaca (cena ALTO)",
            ingredientes = listOf(
                "1/4 taza de lentejas rojas",
                "1 taza de espinaca",
                "1/4 pieza de cebolla",
                "1/4 pieza de jitomate",
                "1 diente de ajo",
                "1 cdta de aceite de oliva extra virgen"
            ),
            preparacion  = "Las lentejas rojas no necesitan remojo. Cuece con cebolla, jitomate " +
                    "y ajo en 1.5 tazas de agua 20 min. Agrega espinaca al final 3 min. " +
                    "Rocía aceite de oliva extra virgen al servir. Poca sal yodada.",
            kcal = 195, tipoComida = TipoComida.CENA,
            nivelMinimo = NivelIngreso.ALTO, edadMinMeses = 24,
            fuente    = "Secretaría de Salud — Guías Alimentarias México 2023 / OMS PAHO",
            alergenos = emptyList()
        ),

        // ══════════════════════════════════════════════
        // COLACIONES — SEP/SS Nuevo León + IMSS
        // ══════════════════════════════════════════════

        RecetaMexicana(
            nombre       = "Fresas con pan tostado y almendras",
            ingredientes = listOf(
                "1 taza de fresas picadas",
                "1 pieza de pan tostado integral",
                "1 cda de almendras fileteadas"
            ),
            preparacion  = "Picar fresas. Servir con pan tostado y almendras. " +
                    "Combinación: fruta + cereal + oleaginosa. Sin azúcar añadida.",
            kcal = 186, tipoComida = TipoComida.COLACION,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 24,
            fuente    = "SEP/SS Nuevo León — Programa Salud Para Aprender",
            alergenos = listOf(Alergeno.TRIGO, Alergeno.NUECES)
        ),

        RecetaMexicana(
            nombre       = "Manzana con pan tostado y hummus",
            ingredientes = listOf(
                "1 pieza de manzana",
                "1 pieza de pan tostado integral",
                "2 cdas de hummus de garbanzo"
            ),
            preparacion  = "Rebanar manzana. Servir con pan tostado y hummus. " +
                    "El hummus aporta proteína vegetal y hierro.",
            kcal = 191, tipoComida = TipoComida.COLACION,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 30,
            fuente    = "SEP/SS Nuevo León — Programa Salud Para Aprender",
            alergenos = listOf(Alergeno.TRIGO, Alergeno.FRUCTOSA)
        ),

        RecetaMexicana(
            nombre       = "Elote con limón y cacahuate tostado",
            ingredientes = listOf(
                "1 taza de elote desgranado",
                "1 limón",
                "28 piezas de cacahuate tostado natural",
                "Chile en polvo bajo en sodio al gusto"
            ),
            preparacion  = "Cocer el elote. Agregar jugo de limón y chile al gusto. " +
                    "Servir con cacahuate tostado.",
            kcal = 348, tipoComida = TipoComida.COLACION,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 36,
            fuente    = "SEP/SS Nuevo León — Programa Salud Para Aprender",
            alergenos = listOf(Alergeno.MAIZ, Alergeno.CACAHUATE)
        ),

        RecetaMexicana(
            nombre       = "Agua de jamaica natural",
            ingredientes = listOf(
                "1 taza de flores de jamaica",
                "1 litro de agua"
            ),
            preparacion  = "Hervir agua y agregar flores de jamaica. Reposar 10 min. Colar. " +
                    "Servir sin azúcar. Rica en antioxidantes y vitamina C. " +
                    "IMPORTANTE: no añadir azúcar (OMS/COCO 2023).",
            kcal = 15, tipoComida = TipoComida.COLACION,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 12,
            fuente    = "IMSS — NutrIMSS Infancia / COCO 2023",
            alergenos = emptyList()
        ),

        RecetaMexicana(
            nombre       = "Plátano con cacahuate natural",
            ingredientes = listOf(
                "1/2 pieza de plátano",
                "1 cda de cacahuate natural tostado sin sal"
            ),
            preparacion  = "Rebana el plátano. Servir con cacahuate. Combinación de energía " +
                    "rápida (carbohidrato) + grasa saludable + proteína vegetal. " +
                    "PRECAUCIÓN: alergia a cacahuate — supervisar primera exposición.",
            kcal = 145, tipoComida = TipoComida.COLACION,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 18,
            fuente    = "IMSS — NutrIMSS Infancia 2020 / SEP/SS Nuevo León",
            alergenos = listOf(Alergeno.CACAHUATE)
        ),

        RecetaMexicana(
            nombre       = "Jícama con limón y chile suave",
            ingredientes = listOf(
                "1 taza de jícama en bastones",
                "1/2 limón",
                "Chile en polvo muy suave al gusto"
            ),
            preparacion  = "Pela y corta la jícama en bastones. Agrega limón y chile suave. " +
                    "Snack crujiente bajo en calorías, alto en fibra y vitamina C. " +
                    "Ideal para desarrollo de masticación.",
            kcal = 50, tipoComida = TipoComida.COLACION,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 24,
            fuente    = "SEP/SS Nuevo León — Programa Salud Para Aprender / IMSS NutrIMSS",
            alergenos = emptyList()
        ),

        RecetaMexicana(
            nombre       = "Pepino con limón y chile (colación)",
            ingredientes = listOf(
                "1 pieza de pepino mediano",
                "1/2 limón",
                "Chile en polvo muy suave al gusto"
            ),
            preparacion  = "Pela y rebana el pepino. Agrega limón y chile al gusto. " +
                    "Alto contenido de agua, bajo en calorías. Hidratante y refrescante. " +
                    "Colación tradicional mexicana de bajo costo.",
            kcal = 35, tipoComida = TipoComida.COLACION,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 18,
            fuente    = "SEP/SS Nuevo León — Programa Salud Para Aprender",
            alergenos = emptyList()
        ),

        RecetaMexicana(
            nombre       = "Zanahoria rallada con limón y pepitas",
            ingredientes = listOf(
                "1 pieza de zanahoria mediana rallada",
                "1 cda de pepitas de calabaza tostadas",
                "1/2 limón"
            ),
            preparacion  = "Ralla la zanahoria. Agrega jugo de limón. Mezcla con pepitas. " +
                    "Vitamina A (zanahoria) + zinc y grasa saludable (pepita). " +
                    "Colación mexicana de bajo costo y alto valor nutricional.",
            kcal = 95, tipoComida = TipoComida.COLACION,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 24,
            fuente    = "IMSS — NutrIMSS Infancia 2020",
            alergenos = emptyList()
        ),

        RecetaMexicana(
            nombre       = "Guayaba con cacahuate (colación)",
            ingredientes = listOf(
                "2 piezas de guayaba",
                "15 g de cacahuate tostado sin sal"
            ),
            preparacion  = "Lavar las guayabas. Servir enteras o partidas a la mitad. " +
                    "Acompañar con cacahuate. Vitamina C de guayaba + proteína y grasa " +
                    "saludable de cacahuate. Una de las frutas más económicas de México.",
            kcal = 110, tipoComida = TipoComida.COLACION,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 18,
            fuente    = "IMSS — NutrIMSS Infancia 2020 / SEP/SS Nuevo León",
            alergenos = listOf(Alergeno.CACAHUATE)
        ),

        RecetaMexicana(
            nombre       = "Tuna con limón (colación de temporada)",
            ingredientes = listOf(
                "2 piezas de tuna roja o verde",
                "1/2 limón"
            ),
            preparacion  = "Pela las tunas con cuidado (usar guantes). Rebana y agrega limón. " +
                    "Fruta de temporada mexicana rica en vitamina C y fibra. Sin costo " +
                    "adicional de azúcar. Disponible agosto-octubre.",
            kcal = 60, tipoComida = TipoComida.COLACION,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 24,
            fuente    = "IMSS — NutrIMSS Infancia 2020 / Cultura.gob.mx",
            alergenos = emptyList()
        ),

        RecetaMexicana(
            nombre       = "Yogur natural con fresas y granola de avena",
            ingredientes = listOf(
                "1/2 taza de yogur natural sin azúcar",
                "1/4 taza de fresas picadas",
                "2 cdas de avena tostada"
            ),
            preparacion  = "Servir el yogur. Agregar fresas picadas y avena tostada como " +
                    "granola artesanal sin azúcar. Proteína + calcio + fibra + vitamina C.",
            kcal = 165, tipoComida = TipoComida.COLACION,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 24,
            fuente    = "SEP/SS Nuevo León — Programa Salud Para Aprender",
            alergenos = listOf(Alergeno.LACTEOS, Alergeno.TRIGO)
        ),

        RecetaMexicana(
            nombre       = "Mango con limón y chile tajín suave",
            ingredientes = listOf(
                "1/2 pieza de mango ataulfo o manila",
                "1/2 limón",
                "Chile tajín suave al gusto (mínimo)"
            ),
            preparacion  = "Pela y rebana el mango. Agrega limón y tajín suave en mínima cantidad. " +
                    "Rico en vitamina C y A. Fruta de temporada mexicana de primavera-verano. " +
                    "NOTA COCO 2023: limitar condimentos picantes en menores de 3 años.",
            kcal = 80, tipoComida = TipoComida.COLACION,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 36,
            fuente    = "SEP/SS Nuevo León — Programa Salud Para Aprender / COCO 2023",
            alergenos = emptyList()
        ),

        RecetaMexicana(
            nombre       = "Palomitas de maíz naturales sin mantequilla",
            ingredientes = listOf(
                "2 cdas de maíz palomero",
                "1 cdta de aceite de canola"
            ),
            preparacion  = "Calienta el aceite en olla tapada. Agrega el maíz. Tapa y espera " +
                    "a que revienten todas las palomitas moviendo la olla. Sin sal, sin " +
                    "mantequilla. Colación de volumen bajo en calorías, alta en fibra.",
            kcal = 95, tipoComida = TipoComida.COLACION,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 48,
            fuente    = "SEP/SS Nuevo León — Programa Salud Para Aprender",
            alergenos = listOf(Alergeno.MAIZ)
        ),

        // ══════════════════════════════════════════════
        // RECETAS EXCLUSIVAS DE PUEBLA
        // ══════════════════════════════════════════════

        RecetaMexicana(
            nombre       = "Arroz a la poblana con epazote",
            ingredientes = listOf(
                "1/4 taza de arroz integral",
                "1/2 pieza de chile poblano asado sin semillas",
                "1/4 taza de granos de elote",
                "1 diente de ajo",
                "1/4 pieza de cebolla",
                "Epazote al gusto",
                "1 cdta de aceite de canola"
            ),
            preparacion  = "Asar el chile poblano, retirar piel y semillas, picar fino. " +
                    "Remojar arroz 1 hora y escurrir. Dorar arroz en aceite. Licuar ajo " +
                    "y cebolla y agregar al arroz. Incorporar elote, chile y 1 taza de " +
                    "agua. Agregar epazote. Tapar y cocer lento.",
            kcal = 180, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 24,
            fuente    = "Cultura.gob.mx — Gastronomía Poblana",
            regiones  = listOf(RegionMexico.PUEBLA, RegionMexico.CENTRO, RegionMexico.SUR),
            alergenos = listOf(Alergeno.MAIZ)
        ),

        RecetaMexicana(
            nombre       = "Enfrijoladas poblanas",
            ingredientes = listOf(
                "3 tortillas de maíz",
                "1/2 taza de frijoles negros cocidos",
                "40 g de queso fresco desmoronado",
                "1/4 pieza de cebolla picada",
                "1 cdta de aceite"
            ),
            preparacion  = "Licuar frijoles con su caldo hasta obtener salsa. Calentar en " +
                    "sartén con aceite. Pasar tortillas por la salsa caliente y doblar. " +
                    "Servir con queso fresco y cebolla.",
            kcal = 320, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 24,
            fuente    = "Cultura.gob.mx — Gastronomía Poblana",
            regiones  = listOf(RegionMexico.PUEBLA, RegionMexico.CENTRO, RegionMexico.SUR),
            alergenos = listOf(Alergeno.MAIZ, Alergeno.LACTEOS)
        ),

        RecetaMexicana(
            nombre       = "Atole de maíz azul con canela",
            ingredientes = listOf(
                "2 cdas de masa o harina de maíz azul",
                "1 taza de leche entera",
                "1 rama de canela"
            ),
            preparacion  = "Disolver la masa en la leche fría. Agregar canela. Calentar a fuego " +
                    "medio moviendo constantemente hasta espesar. Sin piloncillo ni " +
                    "azúcar añadida (COCO 2023). Bebida tradicional poblana rica en " +
                    "calcio y energía.",
            kcal = 130, tipoComida = TipoComida.DESAYUNO,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 12,
            fuente    = "Cultura.gob.mx — Gastronomía Poblana / COCO 2023",
            regiones  = listOf(RegionMexico.PUEBLA, RegionMexico.CENTRO),
            alergenos = listOf(Alergeno.LACTEOS, Alergeno.MAIZ)
        ),

        RecetaMexicana(
            nombre       = "Sopa de amaranto con plátano",
            ingredientes = listOf(
                "1/4 taza de amaranto tostado",
                "1/2 plátano rebanado",
                "1 taza de leche entera",
                "Canela al gusto"
            ),
            preparacion  = "Calentar la leche. Agregar el amaranto tostado y la canela. Mezclar " +
                    "bien. Servir con plátano rebanado encima. El amaranto es un " +
                    "superalimento mexicano con proteína completa y calcio. Sin azúcar.",
            kcal = 220, tipoComida = TipoComida.DESAYUNO,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 12,
            fuente    = "IMSS — Guía Alimentación 0-12 meses, 2020",
            regiones  = listOf(RegionMexico.GENERAL),
            alergenos = listOf(Alergeno.LACTEOS)
        ),

        RecetaMexicana(
            nombre       = "Pipián verde con pollo y tortilla",
            ingredientes = listOf(
                "60 g de pechuga de pollo",
                "2 cdas de pepitas (semillas de calabaza)",
                "1/2 taza de tomate verde",
                "Chile serrano suave al gusto",
                "Cilantro y epazote al gusto",
                "2 tortillas de maíz"
            ),
            preparacion  = "Tostar las pepitas en sartén seco. Licuar con tomate verde, chile, " +
                    "cilantro y epazote. Cocer el pollo en agua. Agregar la salsa de " +
                    "pipián al caldo con el pollo desmenuzado. Cocer lento 10 min. " +
                    "Servir con tortillas.",
            kcal = 380, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 30,
            fuente    = "Cultura.gob.mx — Gastronomía Poblana",
            regiones  = listOf(RegionMexico.PUEBLA, RegionMexico.SUR),
            alergenos = listOf(Alergeno.MAIZ)
        ),

        RecetaMexicana(
            nombre       = "Chileatole de pollo poblano",
            ingredientes = listOf(
                "60 g de pechuga de pollo desmenuzada",
                "2 cdas de masa de maíz",
                "1/2 pieza de chile poblano asado sin semillas",
                "1/4 taza de granos de elote",
                "Epazote al gusto",
                "2 tortillas de maíz"
            ),
            preparacion  = "Cocer el pollo en agua. Disolver la masa en un poco de agua fría. " +
                    "Licuar chile poblano con un poco de caldo. Agregar al caldo la masa " +
                    "disuelta, chile, elote y epazote. Incorporar el pollo desmenuzado. " +
                    "Cocer moviendo hasta espesar. Servir con tortillas.",
            kcal = 290, tipoComida = TipoComida.COMIDA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 30,
            fuente    = "Cultura.gob.mx — Gastronomía Poblana",
            regiones  = listOf(RegionMexico.PUEBLA, RegionMexico.CENTRO),
            alergenos = listOf(Alergeno.MAIZ)
        ),

        // ── Puebla CENA ───────────────────────────────────────────────────────

        RecetaMexicana(
            nombre       = "Caldo de habas con epazote (cena poblana)",
            ingredientes = listOf(
                "1/4 taza de habas secas peladas",
                "Epazote al gusto",
                "1/4 pieza de cebolla",
                "1 chile verde suave",
                "1 cdta de aceite de canola"
            ),
            preparacion  = "Remoja las habas 1 hora. Cuece en agua con cebolla y chile verde " +
                    "hasta suave (~40 min). Agrega epazote los últimos 5 min. " +
                    "Agrega aceite. Poca sal yodada. Cena tradicional poblana de " +
                    "proteína vegetal y hierro.",
            kcal = 155, tipoComida = TipoComida.CENA,
            nivelMinimo = NivelIngreso.BASICO, edadMinMeses = 24,
            fuente    = "Cultura.gob.mx — Gastronomía Poblana / IMSS NutrIMSS",
            regiones  = listOf(RegionMexico.PUEBLA, RegionMexico.CENTRO),
            alergenos = emptyList()
        )
    )

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH v2.1 — Helpers para filtrar por alimentos registrados
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Extrae palabras clave de un nombre de alimento registrado.
     * Elimina prefijos comunes ("pure de", "papilla de", etc.) y
     * palabras muy cortas para quedarse con el término significativo.
     */
    private fun extraerPalabrasClave(nombre: String): List<String> {
        val stopWords = setOf(
            "pure", "puré", "de", "en", "con", "al", "la", "el", "del", "los", "las",
            "papilla", "crema", "sopa", "caldo", "guiso", "guisado",
            "cocida", "cocido", "cocidos", "molida", "molido",
            "frita", "frito", "trozos", "liso", "grumoso", "muy", "sin",
            "integral", "natural", "casera", "casero", "simple"
        )
        return nombre
            .lowercase()
            .map { c ->
                when (c) {
                    'á' -> 'a'; 'é' -> 'e'; 'í' -> 'i'; 'ó' -> 'o'; 'ú' -> 'u'
                    else -> c
                }
            }
            .joinToString("")
            .split(Regex("[\\s+\\-/()]+"))
            .map { it.trim() }
            .filter { it.length >= 4 && it !in stopWords }
            .distinct()
    }

    /**
     * Devuelve true si la receta contiene al menos una de las palabras clave
     * extraídas del [nombreAlimento] — buscando en ingredientes y nombre de la receta.
     */
    private fun RecetaMexicana.contieneIngrediente(nombreAlimento: String): Boolean {
        val palabrasClave = extraerPalabrasClave(nombreAlimento)
        if (palabrasClave.isEmpty()) return false

        val textoReceta = (ingredientes + listOf(nombre))
            .joinToString(" ")
            .lowercase()
            .map { c ->
                when (c) {
                    'á' -> 'a'; 'é' -> 'e'; 'í' -> 'i'; 'ó' -> 'o'; 'ú' -> 'u'
                    else -> c
                }
            }
            .joinToString("")

        return palabrasClave.any { clave -> textoReceta.contains(clave) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Filtrar recetas por perfil, región, alergias e ingredientes
    // ─────────────────────────────────────────────────────────────────────────

    fun recetasPorPerfil(
        meses:                Int,
        nivel:                NivelIngreso,
        tipo:                 TipoComida,
        region:               RegionMexico      = RegionMexico.GENERAL,
        alergenosNiño:        List<Alergeno>    = emptyList(),
        alimentosRegistrados: List<String>      = emptyList(),
        recetasCustom:        List<RecetaMexicana> = emptyList()
    ): List<RecetaMexicana> {
        val nivelesAccesibles = when (nivel) {
            NivelIngreso.BASICO     -> listOf(NivelIngreso.BASICO)
            NivelIngreso.MEDIO_BAJO -> listOf(NivelIngreso.BASICO, NivelIngreso.MEDIO_BAJO)
            NivelIngreso.MEDIO      -> listOf(NivelIngreso.BASICO, NivelIngreso.MEDIO_BAJO, NivelIngreso.MEDIO)
            NivelIngreso.ALTO       -> NivelIngreso.entries.toList()
        }

        val candidatas = (recetasCustom + RECETAS).filter { r ->
            r.tipoComida == tipo &&
                    r.nivelMinimo in nivelesAccesibles &&
                    meses >= r.edadMinMeses &&
                    r.esSegurasParaPerfil(alergenosNiño) &&
                    (r.regiones.contains(RegionMexico.GENERAL) || r.regiones.contains(region))
        }

        if (alimentosRegistrados.isEmpty()) return candidatas

        val conIngredientes = candidatas.filter { receta ->
            alimentosRegistrados.any { nombreRegistrado ->
                receta.contieneIngrediente(nombreRegistrado)
            }
        }

        // Fallback: si ninguna receta usa los ingredientes registrados,
        // devolver todas las candidatas para no dejar el plan vacío.
        return conIngredientes.ifEmpty { candidatas }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Generar plan semanal
    // ─────────────────────────────────────────────────────────────────────────

    fun generarPlanSemanal(
        meses:                Int,
        nivel:                NivelIngreso,
        region:               RegionMexico   = RegionMexico.GENERAL,
        alergenosNiño:        List<Alergeno> = emptyList(),
        alimentosRegistrados: List<String>   = emptyList(),
        recetasCustom:        List<RecetaMexicana> = emptyList()
    ): List<PlanDietaSemanal> {
        val dias   = listOf("Lunes","Martes","Miércoles","Jueves","Viernes","Sábado","Domingo")
        val macros = macrosPorEdad(meses)

        val desayunos  = recetasPorPerfil(meses, nivel, TipoComida.DESAYUNO,  region, alergenosNiño, alimentosRegistrados, recetasCustom)
            .ifEmpty { listOf(fallback(TipoComida.DESAYUNO,  meses)) }
        val comidas    = recetasPorPerfil(meses, nivel, TipoComida.COMIDA,    region, alergenosNiño, alimentosRegistrados, recetasCustom)
            .ifEmpty { listOf(fallback(TipoComida.COMIDA,    meses)) }
        val cenas      = recetasPorPerfil(meses, nivel, TipoComida.CENA,      region, alergenosNiño, alimentosRegistrados, recetasCustom)
            .ifEmpty { listOf(fallback(TipoComida.CENA,      meses)) }
        val colaciones = recetasPorPerfil(meses, nivel, TipoComida.COLACION,  region, alergenosNiño, alimentosRegistrados, recetasCustom)
            .ifEmpty { listOf(fallback(TipoComida.COLACION,  meses)) }

        return dias.mapIndexed { i, dia ->
            PlanDietaSemanal(
                diaSemana    = dia,
                comidas      = ComidasDiarias(
                    desayuno         = desayunos[i  % desayunos.size].nombre,
                    colacion1        = colaciones[i % colaciones.size].nombre,
                    almuerzo         = comidas[i    % comidas.size].nombre,
                    colacion2        = colaciones[(i + 1) % colaciones.size].nombre,
                    cena             = cenas[i      % cenas.size].nombre,
                    costoEstimadoDia = costoEstimadoPorNivelEtapa(nivel, etapaIndex(meses))
                ),
                macros       = macros,
                nivelIngreso = nivel,
                edadMeses    = meses
            )
        }
    }

    private fun fallback(tipo: TipoComida, meses: Int): RecetaMexicana {
        val nombre = when {
            meses < 6  -> "Leche materna a demanda"
            meses < 12 -> when (tipo) {
                TipoComida.DESAYUNO  -> "Puré de avena con puré de fruta y lactancia"
                TipoComida.COMIDA    -> "Puré de verdura con proteína y lactancia"
                TipoComida.COLACION  -> "Lactancia materna a demanda"
                TipoComida.CENA      -> "Puré de verdura suave con lactancia"
            }
            else -> "Comida balanceada con agua simple"
        }
        return RecetaMexicana(
            nombre, listOf("Según edad y tolerancia"),
            "Seguir recomendaciones OMS/IMSS para la etapa",
            0, tipo, NivelIngreso.BASICO, 0,
            "OMS / IMSS 2020"
        )
    }

    // etapaIndex cubre el rango completo hasta 144 meses
    private fun etapaIndex(meses: Int): Int = when {
        meses < 12  -> 0
        meses < 36  -> 1
        meses < 72  -> 2
        else        -> 3   // cubre 6-12 años y adolescencia (144+)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CAMBIO v2.2 — costoEstimadoPorNivelEtapa() recalibrado PACIC/PROFECO 2026
    //
    // SMG 2026 (CONASAMI dic 2025, zona general):
    //   $315.04 MXN/día  ·  $9,451 MXN/mes
    //
    // Canasta PACIC (24 productos PROFECO, mar 2026):
    //   ~$874/semana · ~$3,758/mes familia de 4 personas (39.8% del SMG mensual)
    //
    // Límite razonable alimentación infantil adicional: ~$1,500–2,500/mes
    //   → $50–83/día para el niño (sobre la canasta familiar base)
    //
    // Precios clave verificados PROFECO/SNIIM/ANPEC mar 2026:
    //   Tortilla (Puebla) $17.69/kg · Pollo $57/kg · Frijol $32/kg
    //   Huevo $45/kg · Jitomate $25.90/kg · Res $124/kg
    //   Mojarra/tilapia $150–164/kg · Sierra $200–275/kg
    //   Salmón >$300/kg · Yogur griego $80/200g · Quinoa $180–250/kg
    //
    // CORRECCIÓN vs v2.1: MEDIO etapa 3 bajó de $140 → $95 (era 50% del SMG)
    //                      ALTO etapa 3 bajó de $185 → $160 (más realista)
    // ─────────────────────────────────────────────────────────────────────────
    private fun costoEstimadoPorNivelEtapa(nivel: NivelIngreso, etapa: Int): Double = when (nivel) {
        //                          etapa:  0      1      2      3
        //                                 <1a   1-3a   3-6a   6-12a
        NivelIngreso.BASICO     -> listOf(27.0,  37.0,  47.0,  55.0)[etapa]  // tortilla+frijol+huevo+pollo
        NivelIngreso.MEDIO_BAJO -> listOf(42.0,  55.0,  68.0,  80.0)[etapa]  // + atún, queso panela, fruta
        NivelIngreso.MEDIO      -> listOf(60.0,  75.0,  88.0,  95.0)[etapa]  // + mojarra, res ocasional
        NivelIngreso.ALTO       -> listOf(85.0, 100.0, 130.0, 160.0)[etapa]  // + salmón, quinoa, yogur griego
    }

    fun resumenNutricional(
        meses:         Int,
        nivel:         NivelIngreso,
        perfilSalud:   PerfilSaludNino = PerfilSaludNino()
    ): ResumenNutricional {
        val macros = macrosPorEdad(meses)
        val alertas = mutableListOf<String>()

        if (meses < 6)
            alertas.add("OMS: lactancia exclusiva los primeros 6 meses")
        if (meses in 6..12)
            alertas.add("IMSS 2023: introducir un alimento nuevo cada 3 días")
        if (meses < 12)
            alertas.add("OMS/IMSS: continuar lactancia materna a demanda")
        if (meses in 6..24)
            alertas.add("OMS/COCO 2023: no añadir azúcar ni sal a los alimentos antes de los 2 años")
        if (meses in 6..8)
            alertas.add("IMSS/COCO 2023: evitar avena y cereales con gluten antes de los 8 meses")
        if (meses in 9..11)
            alertas.add("IMSS 2023: introducir primero la yema de huevo; la clara no antes de los 12 meses")
        if (nivel == NivelIngreso.BASICO && meses in 6..24)
            alertas.add("IMSS: tortilla de maíz + frijol = proteína completa a bajo costo")
        if (nivel == NivelIngreso.BASICO)
            alertas.add("IMSS: acompañar legumbres con vitamina C para mejor absorción de hierro")
        if (meses in 12..24)
            alertas.add("OMS: leche entera desde 12 meses, no reemplaza lactancia antes de 2 años")

        // ── CAMBIO v2.2: alimentosClave actualizado con sierra y sardina ──────
        val alimentosClave = when (nivel) {
            NivelIngreso.BASICO     -> listOf(
                "Frijol","Tortilla","Huevo","Lentejas","Plátano",
                "Nopal","Pollo","Amaranto","Guayaba","Sardina"
            )
            NivelIngreso.MEDIO_BAJO -> listOf(
                "Pollo","Frijol","Huevo","Atún","Sardina",
                "Fruta temporada","Leche","Arroz","Queso panela"
            )
            NivelIngreso.MEDIO      -> listOf(
                "Pechuga","Res","Sierra","Mojarra",
                "Verduras","Frutas","Lácteos","Cereales integrales","Yogur"
            )
            NivelIngreso.ALTO       -> listOf(
                "Salmón","Sierra","Pechuga","Quinoa",
                "Verduras","Frutas","Yogur griego","Nueces","Amaranto"
            )
        }

        val alimentosSeguros = alimentosClave.filter { alimento ->
            val alergenosDelAlimento = mapAlimentoAlergeno(alimento)
            alergenosDelAlimento.none { it in perfilSalud.alergenos }
        }

        return ResumenNutricional(
            etapaLabel            = etapaLabel(meses),
            macroObjetivo         = macros,
            nivelIngreso          = nivel,
            costoMensualEstimado  = costoEstimadoPorNivelEtapa(nivel, etapaIndex(meses)) * 30,
            alimentosClave        = alimentosSeguros,
            alertas               = alertas,
            alertasAlergia        = perfilSalud.generarAdvertencias()
        )
    }

    // ── CAMBIO v2.2: Sierra, Sardina y Mojarra añadidas ──────────────────────
    private fun mapAlimentoAlergeno(alimento: String): List<Alergeno> = when {
        alimento.contains("Huevo",      ignoreCase = true) -> listOf(Alergeno.HUEVO)
        alimento.contains("Leche",      ignoreCase = true) -> listOf(Alergeno.LACTEOS)
        alimento.contains("Lácteos",    ignoreCase = true) -> listOf(Alergeno.LACTEOS)
        alimento.contains("Lácteo",     ignoreCase = true) -> listOf(Alergeno.LACTEOS)
        alimento.contains("Yogur",      ignoreCase = true) -> listOf(Alergeno.LACTEOS)
        alimento.contains("Queso",      ignoreCase = true) -> listOf(Alergeno.LACTEOS)
        alimento.contains("Nueces",     ignoreCase = true) -> listOf(Alergeno.NUECES)
        alimento.contains("Almendras",  ignoreCase = true) -> listOf(Alergeno.NUECES)
        alimento.contains("Cacahuate",  ignoreCase = true) -> listOf(Alergeno.CACAHUATE)
        alimento.contains("Tortilla",   ignoreCase = true) -> listOf(Alergeno.MAIZ)
        alimento.contains("Amaranto",   ignoreCase = true) -> emptyList()
        alimento.contains("Atún",       ignoreCase = true) -> listOf(Alergeno.PESCADO)
        alimento.contains("Salmón",     ignoreCase = true) -> listOf(Alergeno.PESCADO)
        alimento.contains("Sierra",     ignoreCase = true) -> listOf(Alergeno.PESCADO)  // ← v2.2
        alimento.contains("Sardina",    ignoreCase = true) -> listOf(Alergeno.PESCADO)  // ← v2.2
        alimento.contains("Mojarra",    ignoreCase = true) -> listOf(Alergeno.PESCADO)  // ← v2.2
        alimento.contains("Pescado",    ignoreCase = true) -> listOf(Alergeno.PESCADO)
        alimento.contains("Quinoa",     ignoreCase = true) -> emptyList()
        else -> emptyList()
    }
}