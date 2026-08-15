package com.example.nutriia.embarazo

import com.example.nutriia.sueldo.NivelIngreso
import com.example.nutriia.sueldo.RegionMexico
import com.example.nutriia.sueldo.Alergeno
import com.example.nutriia.sueldo.TipoComida

// ═══════════════════════════════════════════════════════════════════════════
// MOTOR DE DIETAS — EMBARAZO (v1.0)
//
// FUENTES DE REFERENCIA:
// [1] Secretaría de Salud México — Guías Alimentarias para la Población Mexicana 2023
// [2] IMSS — Control prenatal y nutrición materna
// [3] Nestlé Materna / FamilyNes — contenido educativo sobre nutrición en el embarazo
//     (ácido fólico, hierro 30 mg/día, hidratación 2.3 L/día, distribución de comidas)
// [4] Chef Oropeza — recetario "Embarazadas" (referencia de estilo/nombres de platillos,
//     NO fuente clínica)
//
// NOTA CLÍNICA: Este módulo es orientativo. No sustituye consulta médica ni
// valoración de un nutriólogo/ginecólogo. Toda condición médica declarada por
// la usuaria (diabetes gestacional, hipertensión, anemia, etc.) debe filtrar
// recetas y mostrar alertas — nunca debe ignorarse silenciosamente.
// ═══════════════════════════════════════════════════════════════════════════

object DietaEmbarazoEngine {

    fun trimestrePorSemana(semanas: Int): TrimestreEmbarazo = when {
        semanas <= 13 -> TrimestreEmbarazo.PRIMERO
        semanas <= 27 -> TrimestreEmbarazo.SEGUNDO
        else          -> TrimestreEmbarazo.TERCERO
    }

    fun macrosPorTrimestre(trimestre: TrimestreEmbarazo): MacroObjetivoEmbarazo = when (trimestre) {
        // Fuente [1][3]: sin kcal extra necesarias en 1er trimestre;
        // +300 kcal aprox. 2do trimestre; +450 kcal aprox. 3er trimestre.
        // Hierro: 30 mg/día (el doble de una mujer no embarazada). Folato:
        // crítico en 1er trimestre. Agua: 2.3 L/día en todo el embarazo.
        TrimestreEmbarazo.PRIMERO  -> MacroObjetivoEmbarazo(0,   71.0, 27.0, 1000.0, 600.0, 1.4, 2.3)
        TrimestreEmbarazo.SEGUNDO  -> MacroObjetivoEmbarazo(300, 71.0, 27.0, 1000.0, 600.0, 1.4, 2.3)
        TrimestreEmbarazo.TERCERO  -> MacroObjetivoEmbarazo(450, 71.0, 27.0, 1000.0, 600.0, 1.4, 2.3)
    }

    val RECETAS: List<RecetaEmbarazo> = listOf(
        // ─────────────────────────────────────────────────────────────────────
        // DESAYUNOS (6 recetas)
        // ─────────────────────────────────────────────────────────────────────
        RecetaEmbarazo(
            nombre = "Avena cremosa con plátano y nueces",
            ingredientes = listOf("1/2 taza de avena", "1 taza de leche pasteurizada", "1/2 plátano", "1 cda de nueces picadas"),
            preparacion = "Cocina la avena en la leche pasteurizada a fuego medio durante 5 minutos hasta espesar. Sirve templada con plátano en rodajas y nueces picadas.",
            kcal = 320,
            tipoComida = TipoComida.DESAYUNO,
            trimestreMinimo = TrimestreEmbarazo.PRIMERO,
            nivelMinimo = NivelIngreso.BASICO,
            fuente = "IMSS — Control prenatal y nutrición materna",
            alergenos = listOf(Alergeno.LACTEOS, Alergeno.NUECES),
            condicionesExcluidas = listOf("diabetes gestacional")
        ),
        RecetaEmbarazo(
            nombre = "Huevos revueltos a la mexicana con frijoles",
            ingredientes = listOf("2 huevos", "1/2 jitomate picado", "1/4 de cebolla picada", "1 cdta de aceite de canola", "1/2 taza de frijoles de la olla"),
            preparacion = "Bate los huevos. En una sartén con aceite cocina la cebolla y el jitomate por 2 minutos. Vierte el huevo y revuelve constantemente hasta que quede totalmente cocido. Sirve con frijoles calientes.",
            kcal = 290,
            tipoComida = TipoComida.DESAYUNO,
            trimestreMinimo = TrimestreEmbarazo.PRIMERO,
            nivelMinimo = NivelIngreso.BASICO,
            fuente = "Secretaría de Salud México — Guías Alimentarias 2023",
            alergenos = listOf(Alergeno.HUEVO)
        ),
        RecetaEmbarazo(
            nombre = "Molletes integrales con queso panela",
            ingredientes = listOf("1 pan bolillo integral sin migajón", "1/2 taza de frijoles refritos caseros", "60 g de queso panela pasteurizado", "Pico de gallo al gusto"),
            preparacion = "Corta el bolillo por la mitad, unta los frijoles calientes y coloca rebanadas de queso panela pasteurizado. Calienta en comal hasta dorar el pan. Acompaña con pico de gallo sin exceso de sal.",
            kcal = 310,
            tipoComida = TipoComida.DESAYUNO,
            trimestreMinimo = TrimestreEmbarazo.PRIMERO,
            nivelMinimo = NivelIngreso.MEDIO_BAJO,
            fuente = "IMSS — Control prenatal y nutrición materna",
            alergenos = listOf(Alergeno.LACTEOS, Alergeno.TRIGO),
            condicionesExcluidas = listOf("hipertension")
        ),
        RecetaEmbarazo(
            nombre = "Licuado energético de fresa y avena",
            ingredientes = listOf("1 taza de leche pasteurizada descremada", "1/2 taza de fresas lavadas y desinfectadas", "3 cdas de avena", "1 cdta de semillas de chía"),
            preparacion = "Licua la leche descremada con las fresas, la avena y las semillas de chía hasta obtener una mezcla homogénea. Sirve de inmediato y consume fresco.",
            kcal = 240,
            tipoComida = TipoComida.DESAYUNO,
            trimestreMinimo = TrimestreEmbarazo.PRIMERO,
            nivelMinimo = NivelIngreso.MEDIO_BAJO,
            fuente = "Nestlé Materna",
            alergenos = listOf(Alergeno.LACTEOS),
            condicionesExcluidas = listOf("diabetes gestacional")
        ),
        RecetaEmbarazo(
            nombre = "Omelet de espinacas y champiñones",
            ingredientes = listOf("2 huevos", "1 taza de espinacas tiernas lavadas", "1/2 taza de champiñones", "1 cdta de aceite de oliva", "30 g de queso Oaxaca pasteurizado"),
            preparacion = "Saltea las espinacas y champiñones en aceite de oliva. Bate los huevos y añádelos a la sartén. Coloca el queso Oaxaca en el centro, dobla a la mitad y cocina hasta que el huevo esté completamente firme y cocido.",
            kcal = 280,
            tipoComida = TipoComida.DESAYUNO,
            trimestreMinimo = TrimestreEmbarazo.SEGUNDO,
            nivelMinimo = NivelIngreso.MEDIO,
            fuente = "Chef Oropeza — Embarazadas",
            alergenos = listOf(Alergeno.HUEVO, Alergeno.LACTEOS)
        ),
        RecetaEmbarazo(
            nombre = "Enchiladas verdes de requesón",
            ingredientes = listOf("3 tortillas de maíz", "90 g de requesón pasteurizado", "1/2 taza de salsa verde casera templada", "1/4 de aguacate", "1 cdta de cilantro picado"),
            preparacion = "Pasa las tortillas por comal. Rellénalas con el requesón pasteurizado y dóblalas. Baña con la salsa verde caliente, decora con rebanadas de aguacate y espolvorea cilantro fresco.",
            kcal = 380,
            tipoComida = TipoComida.DESAYUNO,
            trimestreMinimo = TrimestreEmbarazo.SEGUNDO,
            nivelMinimo = NivelIngreso.ALTO,
            fuente = "Chef Oropeza — Embarazadas",
            alergenos = listOf(Alergeno.LACTEOS, Alergeno.MAIZ)
        ),

        // ─────────────────────────────────────────────────────────────────────
        // COMIDAS (8 recetas)
        // ─────────────────────────────────────────────────────────────────────
        RecetaEmbarazo(
            nombre = "Caldo de pollo con verduras y arroz",
            ingredientes = listOf("100 g de pechuga de pollo sin piel", "1/2 calabacita picada", "1 zanahoria en rodajas", "1/2 papa picada", "1/2 taza de arroz cocido"),
            preparacion = "Cuece el pollo en agua hirviendo con ajo y cebolla durante 25 minutos. Añade la zanahoria y papa, cocina 10 minutos. Agrega calabacita y arroz, cuece hasta que todo esté suave. Sirve caliente.",
            kcal = 390,
            tipoComida = TipoComida.COMIDA,
            trimestreMinimo = TrimestreEmbarazo.PRIMERO,
            nivelMinimo = NivelIngreso.BASICO,
            fuente = "IMSS — Control prenatal y nutrición materna"
        ),
        RecetaEmbarazo(
            nombre = "Tacos de tinga de pollo económicos",
            ingredientes = listOf("3 tortillas de maíz", "100 g de pechuga de pollo deshebrada cocida", "1/2 cebolla fileteada", "1 jitomate grande licuado con ajo", "1 cdta de aceite"),
            preparacion = "Acitrona la cebolla en aceite. Vierte el jitomate licuado y sazona 5 minutos. Agrega el pollo deshebrado y cocina a fuego lento hasta que se reduzca el líquido. Calienta las tortillas y sirve.",
            kcal = 340,
            tipoComida = TipoComida.COMIDA,
            trimestreMinimo = TrimestreEmbarazo.PRIMERO,
            nivelMinimo = NivelIngreso.BASICO,
            fuente = "Secretaría de Salud México — Guías Alimentarias 2023",
            alergenos = listOf(Alergeno.MAIZ)
        ),
        RecetaEmbarazo(
            nombre = "Lentejas guisadas con plátano macho",
            ingredientes = listOf("1 taza de lentejas cocidas", "1/4 de plátano macho picado", "1/2 jitomate picado", "1/4 de cebolla picada", "1 cdta de aceite"),
            preparacion = "Sofríe la cebolla y el jitomate en aceite. Agrega las lentejas cocidas con su caldo y los trozos de plátano macho. Cocina a fuego lento durante 10 minutos hasta que el plátano esté suave.",
            kcal = 320,
            tipoComida = TipoComida.COMIDA,
            trimestreMinimo = TrimestreEmbarazo.PRIMERO,
            nivelMinimo = NivelIngreso.BASICO,
            fuente = "Secretaría de Salud México — Guías Alimentarias 2023",
            condicionesExcluidas = listOf("diabetes gestacional")
        ),
        RecetaEmbarazo(
            nombre = "Tazón de lentejas con fajitas de pollo y aguacate",
            ingredientes = listOf("1 taza de lentejas cocidas calientes", "80 g de pechuga de pollo a la plancha", "1/3 de aguacate", "1/2 taza de pico de gallo"),
            preparacion = "Cocina perfectamente las fajitas de pollo en un sartén. En un tazón coloca las lentejas, añade encima las fajitas cocidas, el aguacate picado y acompaña con pico de gallo fresco.",
            kcal = 430,
            tipoComida = TipoComida.COMIDA,
            trimestreMinimo = TrimestreEmbarazo.PRIMERO,
            nivelMinimo = NivelIngreso.MEDIO_BAJO,
            fuente = "Chef Oropeza — Embarazadas"
        ),
        RecetaEmbarazo(
            nombre = "Pescado a la veracruzana cocido (Tilapia)",
            ingredientes = listOf("120 g de filete de tilapia", "1 jitomate picado", "1/4 cebolla fileteada", "1/4 taza de pimientos", "5 aceitunas", "1 cdta de aceite de oliva"),
            preparacion = "En una sartén con aceite saltea la cebolla, pimientos y jitomate. Añade las aceitunas y coloca el filete de tilapia encima. Tapa y cocina 10 minutos a fuego bajo hasta que el pescado esté firme y bien cocido.",
            kcal = 310,
            tipoComida = TipoComida.COMIDA,
            trimestreMinimo = TrimestreEmbarazo.SEGUNDO,
            nivelMinimo = NivelIngreso.MEDIO_BAJO,
            fuente = "Secretaría de Salud México — Guías Alimentarias 2023",
            alergenos = listOf(Alergeno.PESCADO),
            condicionesExcluidas = listOf("hipertension"),
            toleranciaNauseas = false
        ),
        RecetaEmbarazo(
            nombre = "Picadillo de res magro con verduras",
            ingredientes = listOf("100 g de carne molida de res magra", "1/2 zanahoria picada", "1/2 papa picada", "1/2 taza de puré de jitomate natural", "1 cdta de aceite", "3 tortillas de maíz"),
            preparacion = "Dora la carne en aceite asegurando su cocción completa. Integra la zanahoria, papa y puré de jitomate. Tapa y deja hervir a fuego medio por 15 minutos hasta ablandar las verduras. Sirve con tortillas.",
            kcal = 410,
            tipoComida = TipoComida.COMIDA,
            trimestreMinimo = TrimestreEmbarazo.PRIMERO,
            nivelMinimo = NivelIngreso.MEDIO,
            fuente = "IMSS — Control prenatal y nutrición materna",
            alergenos = listOf(Alergeno.MAIZ)
        ),
        RecetaEmbarazo(
            nombre = "Fajitas de res magra con pimientos y quinoa",
            ingredientes = listOf("120 g de filete de res", "1/2 taza de quinoa cocida", "1/2 taza de pimiento rojo y verde", "1/4 cebolla", "1 cdta de aceite de aguacate"),
            preparacion = "Saltea la cebolla y pimientos en aceite de aguacate. Agrega las tiras de res y cocina completamente a temperatura media. Sirve caliente sobre una cama de quinoa cocida.",
            kcal = 450,
            tipoComida = TipoComida.COMIDA,
            trimestreMinimo = TrimestreEmbarazo.TERCERO,
            nivelMinimo = NivelIngreso.ALTO,
            fuente = "Chef Oropeza — Embarazadas"
        ),
        RecetaEmbarazo(
            nombre = "Salmón al horno con costra de chía y camote",
            ingredientes = listOf("120 g de filete de salmón", "1 cdta de semillas de chía", "100 g de camote cocido", "1 taza de espárragos", "1 cdta de aceite de oliva"),
            preparacion = "Pasa el salmón por las semillas de chía. Hornea a 180°C durante 15 minutos hasta que esté bien cocido. Sazona los espárragos al comal con aceite y sirve todo junto con el camote machacado.",
            kcal = 480,
            tipoComida = TipoComida.COMIDA,
            trimestreMinimo = TrimestreEmbarazo.TERCERO,
            nivelMinimo = NivelIngreso.ALTO,
            fuente = "Nestlé Materna",
            alergenos = listOf(Alergeno.PESCADO),
            toleranciaNauseas = false
        ),

        // ─────────────────────────────────────────────────────────────────────
        // CENAS (6 recetas)
        // ─────────────────────────────────────────────────────────────────────
        RecetaEmbarazo(
            nombre = "Quesadillas sencillas con aguacate",
            ingredientes = listOf("2 tortillas de maíz", "60 g de queso Oaxaca pasteurizado", "1/4 de aguacate"),
            preparacion = "Coloca el queso deshebrado sobre las tortillas de maíz en un comal caliente. Dobla y calienta por ambos lados hasta fundir el queso. Abre y agrega rebanadas de aguacate antes de servir.",
            kcal = 310,
            tipoComida = TipoComida.CENA,
            trimestreMinimo = TrimestreEmbarazo.PRIMERO,
            nivelMinimo = NivelIngreso.BASICO,
            fuente = "Secretaría de Salud México — Guías Alimentarias 2023",
            alergenos = listOf(Alergeno.LACTEOS, Alergeno.MAIZ)
        ),
        RecetaEmbarazo(
            nombre = "Ensalada de atún con yogur y galletas saladas",
            ingredientes = listOf("1 lata de atún en agua escurrido", "1/2 taza de chícharos y zanahoria cocidos", "1 cda de yogur griego sin azúcar", "5 galletas saladas horneadas"),
            preparacion = "Mezcla el atún escurrido con las veggies y el yogur griego. Acompaña con las galletas saladas horneadas. Evita agregar sal adicional.",
            kcal = 280,
            tipoComida = TipoComida.CENA,
            trimestreMinimo = TrimestreEmbarazo.SEGUNDO,
            nivelMinimo = NivelIngreso.BASICO,
            fuente = "IMSS — Control prenatal y nutrición materna",
            alergenos = listOf(Alergeno.PESCADO, Alergeno.LACTEOS, Alergeno.TRIGO),
            condicionesExcluidas = listOf("hipertension"),
            toleranciaNauseas = false
        ),
        RecetaEmbarazo(
            nombre = "Tostadas horneadas con frijol y pollo deshebrado",
            ingredientes = listOf("2 tostadas de maíz horneadas", "80 g de pechuga de pollo deshebrada cocida", "1/2 taza de frijoles negros refritos caseros", "30 g de queso panela desmoronado"),
            preparacion = "Unta los frijoles sobre las tostadas crujientes. Coloca encima el pollo deshebrado caliente y decora con queso panela pasteurizado desmoronado.",
            kcal = 320,
            tipoComida = TipoComida.CENA,
            trimestreMinimo = TrimestreEmbarazo.PRIMERO,
            nivelMinimo = NivelIngreso.MEDIO_BAJO,
            fuente = "Secretaría de Salud México — Guías Alimentarias 2023",
            alergenos = listOf(Alergeno.LACTEOS, Alergeno.MAIZ)
        ),
        RecetaEmbarazo(
            nombre = "Sopita de pasta casera con calabacita y pollo",
            ingredientes = listOf("40 g de pasta seca", "80 g de pechuga de pollo cocida en cubos", "1/2 calabacita picada", "1 taza de caldo de pollo desgrasado"),
            preparacion = "Hierve la pasta y la calabacita en el caldo de pollo durante 10 minutos. Agrega los cubos de pollo previamente cocidos y calienta durante 2 minutos más. Sirve caliente.",
            kcal = 290,
            tipoComida = TipoComida.CENA,
            trimestreMinimo = TrimestreEmbarazo.SEGUNDO,
            nivelMinimo = NivelIngreso.MEDIO_BAJO,
            fuente = "IMSS — Control prenatal y nutrición materna",
            alergenos = listOf(Alergeno.TRIGO)
        ),
        RecetaEmbarazo(
            nombre = "Pan integral tostado con aguacate y huevo cocido",
            ingredientes = listOf("1 rebanada de pan integral tostado", "1/2 aguacate maduro machacado", "1 huevo duro picado"),
            preparacion = "Cuece el huevo en agua hirviendo por 10 minutos asegurando que yema y clara estén completamente firmes. Machaca el aguacate sobre el pan tostado y coloca el huevo duro picado encima con una pizca de pimienta.",
            kcal = 280,
            tipoComida = TipoComida.CENA,
            trimestreMinimo = TrimestreEmbarazo.TERCERO,
            nivelMinimo = NivelIngreso.MEDIO,
            fuente = "Chef Oropeza — Embarazadas",
            alergenos = listOf(Alergeno.HUEVO, Alergeno.TRIGO)
        ),
        RecetaEmbarazo(
            nombre = "Tacos de pescado tilapia con aderezo de yogur",
            ingredientes = listOf("100 g de filete de tilapia cocida", "2 tortillas de maíz", "1/2 taza de col picada y desinfectada", "1 cda de yogur natural pasteurizado"),
            preparacion = "Cocina la tilapia al comal con una pizca de aceite de oliva hasta que esté bien cocida y firme. Sirve el pescado desmenuzado en las tortillas calientes, añade la col y adereza con el yogur.",
            kcal = 320,
            tipoComida = TipoComida.CENA,
            trimestreMinimo = TrimestreEmbarazo.TERCERO,
            nivelMinimo = NivelIngreso.ALTO,
            fuente = "Chef Oropeza — Embarazadas",
            alergenos = listOf(Alergeno.PESCADO, Alergeno.MAIZ, Alergeno.LACTEOS),
            toleranciaNauseas = false
        ),

        // ─────────────────────────────────────────────────────────────────────
        // COLACIONES (6 recetas)
        // ─────────────────────────────────────────────────────────────────────
        RecetaEmbarazo(
            nombre = "Melón picado con pepitas de girasol",
            ingredientes = listOf("1 taza de melón picado", "1 cda de semillas de girasol"),
            preparacion = "Sirve el melón fresco picado en un plato hondo y añade las semillas de girasol peladas por encima. Consume fresco.",
            kcal = 110,
            tipoComida = TipoComida.COLACION,
            trimestreMinimo = TrimestreEmbarazo.PRIMERO,
            nivelMinimo = NivelIngreso.BASICO,
            fuente = "Secretaría de Salud México — Guías Alimentarias 2023"
        ),
        RecetaEmbarazo(
            nombre = "Manzana picada con yogur natural",
            ingredientes = listOf("1/2 manzana roja picada", "120 g de yogur natural pasteurizado sin azúcar"),
            preparacion = "En un tazón pequeño sirve el yogur natural y añade la manzana lavada y picada encima. Revuelve bien.",
            kcal = 130,
            tipoComida = TipoComida.COLACION,
            trimestreMinimo = TrimestreEmbarazo.PRIMERO,
            nivelMinimo = NivelIngreso.BASICO,
            fuente = "IMSS — Control prenatal y nutrición materna",
            alergenos = listOf(Alergeno.LACTEOS, Alergeno.FRUCTOSA)
        ),
        RecetaEmbarazo(
            nombre = "Plátano con mantequilla de cacahuate natural",
            ingredientes = listOf("1/2 plátano maduro", "1 cda de crema de cacahuate sin azúcar"),
            preparacion = "Rebana el medio plátano y úntale la mantequilla de cacahuate natural de manera uniforme. Snack energético e ideal para colación vespertina.",
            kcal = 170,
            tipoComida = TipoComida.COLACION,
            trimestreMinimo = TrimestreEmbarazo.SEGUNDO,
            nivelMinimo = NivelIngreso.MEDIO_BAJO,
            fuente = "Nestlé Materna",
            alergenos = listOf(Alergeno.CACAHUATE),
            condicionesExcluidas = listOf("diabetes gestacional")
        ),
        RecetaEmbarazo(
            nombre = "Pepitas de calabaza tostadas con guayaba",
            ingredientes = listOf("30 g de pepitas de calabaza", "2 guayabas en rodajas"),
            preparacion = "Tuesta ligeramente las pepitas de calabaza sin sal añadida. Sirve y acompaña con las guayabas bien lavadas y picadas, ricas en vitamina C.",
            kcal = 180,
            tipoComida = TipoComida.COLACION,
            trimestreMinimo = TrimestreEmbarazo.SEGUNDO,
            nivelMinimo = NivelIngreso.MEDIO_BAJO,
            fuente = "Secretaría de Salud México — Guías Alimentarias 2023"
        ),
        RecetaEmbarazo(
            nombre = "Nueces de castilla y pera fresca",
            ingredientes = listOf("30 g de nueces de castilla", "1/2 pera madura picada"),
            preparacion = "Lava la pera, córtala en cubos pequeños y sirve en un plato acompañada de las nueces de castilla para un aporte de grasas saludables.",
            kcal = 210,
            tipoComida = TipoComida.COLACION,
            trimestreMinimo = TrimestreEmbarazo.TERCERO,
            nivelMinimo = NivelIngreso.MEDIO,
            fuente = "Chef Oropeza — Embarazadas",
            alergenos = listOf(Alergeno.NUECES, Alergeno.FRUCTOSA)
        ),
        RecetaEmbarazo(
            nombre = "Pudín de chía y fresas con leche de almendra",
            ingredientes = listOf("2 cdas de chía", "1/2 taza de leche de almendra sin azúcar", "1/2 taza de fresas rebanadas"),
            preparacion = "Mezcla la chía en el tarro con leche de almendra. Refrigera 4 horas hasta que absorba y espese. Decora con las fresas lavadas y rebanadas.",
            kcal = 150,
            tipoComida = TipoComida.COLACION,
            trimestreMinimo = TrimestreEmbarazo.TERCERO,
            nivelMinimo = NivelIngreso.ALTO,
            fuente = "Nestlé Materna",
            alergenos = listOf(Alergeno.NUECES)
        ),
        RecetaEmbarazo(
            nombre = "Tazón de mango y plátano con yogur",
            ingredientes = listOf("1/2 taza de mango picado", "1/2 plátano", "3 cdas de yogur griego natural pasteurizado", "1/4 taza de agua o hielo", "1 cda de hojuelas de avena", "1 cdta de chía", "1 cdta de chocolate oscuro rallado"),
            preparacion = "Licua el mango, el plátano, el yogur griego y el agua o hielo hasta obtener una consistencia cremosa. Sirve en un tazón y decora por encima con la avena, la chía y el chocolate oscuro rallado.",
            kcal = 230,
            tipoComida = TipoComida.DESAYUNO,
            trimestreMinimo = TrimestreEmbarazo.PRIMERO,
            nivelMinimo = NivelIngreso.MEDIO,
            fuente = "Chef Oropeza",
            alergenos = listOf(Alergeno.LACTEOS, Alergeno.FRUCTOSA),
            condicionesExcluidas = listOf("diabetes gestacional")
        ),
        RecetaEmbarazo(
            nombre = "Waffles de avena con salsa de guayaba",
            ingredientes = listOf("1/2 taza de harina de trigo integral", "3 cdas de hojuelas de avena", "1/2 cdta de polvo para hornear", "1 huevo entero", "1/2 taza de leche pasteurizada", "1 guayaba sin semillas", "2 cdas de yogur natural pasteurizado", "1 cdta de miel de abeja", "1/2 cdta de mantequilla"),
            preparacion = "Licua la harina integral, avena, polvo para hornear, el huevo y la leche hasta tener una masa lisa. Cocina en wafflera engrasada con mantequilla hasta dorar. Para la salsa, licua la guayaba con el yogur y la miel. Baña los waffles con la salsa y sirve.",
            kcal = 320,
            tipoComida = TipoComida.DESAYUNO,
            trimestreMinimo = TrimestreEmbarazo.PRIMERO,
            nivelMinimo = NivelIngreso.MEDIO_BAJO,
            fuente = "Chef Oropeza",
            alergenos = listOf(Alergeno.HUEVO, Alergeno.LACTEOS, Alergeno.TRIGO),
            condicionesExcluidas = listOf("diabetes gestacional")
        ),
        RecetaEmbarazo(
            nombre = "Ensalada tibia de garbanzo con pollo y calabaza",
            ingredientes = listOf("1/2 taza de garbanzo cocido templado", "80 g de pechuga de pollo cocida y deshebrada", "1/2 calabacita picada y ligeramente cocida al vapor", "2 cdas de yogur griego natural pasteurizado", "1/2 cdta de curry en polvo", "1 limón"),
            preparacion = "Mezcla en un tazón los garbanzos cocidos y templados con el pollo deshebrado y la calabacita ligeramente cocida al vapor. Aparte, mezcla el yogur griego con el curry y limón para hacer un aderezo seguro (sin huevo crudo). Adereza la ensalada antes de consumir.",
            kcal = 310,
            tipoComida = TipoComida.COMIDA,
            trimestreMinimo = TrimestreEmbarazo.PRIMERO,
            nivelMinimo = NivelIngreso.MEDIO,
            fuente = "Chef Oropeza (adaptación segura para embarazo)",
            alergenos = listOf(Alergeno.LACTEOS)
        ),
        RecetaEmbarazo(
            nombre = "Brochetas de pollo y vegetales al limón y orégano",
            ingredientes = listOf("100 g de pechuga de pollo en cubos", "1/2 calabacita en rodajas gruesas", "1/4 de pimiento verde en cuadros", "2 champiñones en mitades", "1 cda de aceite de oliva", "1 cda de jugo de limón", "1/2 cdta de orégano seco", "1/4 cdta de ajo en polvo", "2 cdas de yogur griego pasteurizado"),
            preparacion = "Mezcla en un tazón el aceite de oliva, limón, orégano, ajo, sal y pimienta. Divide la marinada en dos partes: una para barnizar el pollo y vegetales crudos en la brocheta, y otra limpia que reservarás. Arma las brochetas alternando pollo y vegetales. Cocina al sartén o comal hasta que el pollo esté completamente cocido al centro. Sirve acompañado del yogur griego mezclado con la marinada limpia reservada.",
            kcal = 340,
            tipoComida = TipoComida.COMIDA,
            trimestreMinimo = TrimestreEmbarazo.PRIMERO,
            nivelMinimo = NivelIngreso.MEDIO,
            fuente = "Chef Oropeza",
            alergenos = listOf(Alergeno.LACTEOS)
        ),
        RecetaEmbarazo(
            nombre = "Fettuccine con pollo en salsa cremosa de pimiento",
            ingredientes = listOf("60 g de pasta fettuccine", "80 g de pechuga de pollo en tiras", "1 pimiento rojo asado y limpio", "1/4 de cebolla asada", "1 diente de ajo asado", "1/2 taza de leche evaporada pasteurizada", "1 cdta de aceite de oliva"),
            preparacion = "Cuece el fettuccine en agua hirviendo con sal hasta que esté al dente. Licua el pimiento rojo asado, la cebolla, el ajo y la leche evaporada pasteurizada. En un sartén con aceite cocina perfectamente las tiras de pollo. Vierte la salsa de pimiento sobre el pollo y deja hervir. Agrega la pasta, integra todo y sirve caliente.",
            kcal = 390,
            tipoComida = TipoComida.COMIDA,
            trimestreMinimo = TrimestreEmbarazo.PRIMERO,
            nivelMinimo = NivelIngreso.MEDIO_BAJO,
            fuente = "Chef Oropeza",
            alergenos = listOf(Alergeno.LACTEOS, Alergeno.TRIGO)
        ),
        RecetaEmbarazo(
            nombre = "Fusilli en salsa cremosa de poblano",
            ingredientes = listOf("60 g de pasta fusilli de sémola", "1 chile poblano asado, pelado y desvenado", "1/4 cebolla asada", "1 diente de ajo asado", "1/2 taza de media crema pasteurizada o leche evaporada", "30 g de queso parmesano rallado", "1 cdta de aceite de oliva"),
            preparacion = "Cuece la pasta fusilli en agua hirviendo con sal hasta que esté al dente. Aparte, licua el chile poblano, la cebolla, el ajo y la media crema pasteurizada. Calienta la salsa en una sartén con aceite de oliva durante 5 minutos a fuego medio. Añade la pasta escurrida, mezcla bien y sirve caliente espolvoreado con queso parmesano.",
            kcal = 380,
            tipoComida = TipoComida.CENA,
            trimestreMinimo = TrimestreEmbarazo.SEGUNDO,
            nivelMinimo = NivelIngreso.MEDIO,
            fuente = "Chef Oropeza",
            alergenos = listOf(Alergeno.LACTEOS, Alergeno.TRIGO)
        ),
        RecetaEmbarazo(
            nombre = "Tortitas de arroz y brócoli con queso cheddar",
            ingredientes = listOf("1/2 taza de brócoli cocido y picado", "1/2 taza de arroz blanco o integral cocido", "1/4 taza de queso cheddar pasteurizado rallado", "1 huevo entero", "2 cdas de harina de trigo integral", "1 cdta de aceite de canola"),
            preparacion = "Mezcla en un tazón el brócoli picado, el arroz cocido, el queso cheddar y el huevo batido. Añade la harina integral y revuelve hasta integrar. Forma tortitas con las manos. En una sartén caliente con aceite de canola, cocina las tortitas 3 minutos por lado hasta que estén doradas y el huevo esté bien cocido.",
            kcal = 280,
            tipoComida = TipoComida.CENA,
            trimestreMinimo = TrimestreEmbarazo.PRIMERO,
            nivelMinimo = NivelIngreso.BASICO,
            fuente = "Chef Oropeza",
            alergenos = listOf(Alergeno.HUEVO, Alergeno.LACTEOS, Alergeno.TRIGO)
        ),
        RecetaEmbarazo(
            nombre = "Sopa de pasta con brócoli y queso cheddar",
            ingredientes = listOf("50 g de pasta corta (letras o codito)", "1/2 taza de floretes de brócoli", "1 taza de caldo de verduras casero", "1/4 taza de media crema pasteurizada", "1/4 taza de queso cheddar pasteurizado rallado", "1 cdta de cebolla picada", "1 cdta de aceite de oliva"),
            preparacion = "Cuece la pasta en agua hirviendo hasta que esté suave y reserva. En una olla calienta el aceite y sofríe la cebolla. Agrega el caldo de verduras y la media crema pasteurizada, calienta hasta hervir a fuego bajo. Añade el queso cheddar y remueve constantemente para que se funda. Agrega la pasta y el brócoli (cocido previamente al vapor), calienta 3 minutos y sirve.",
            kcal = 310,
            tipoComida = TipoComida.CENA,
            trimestreMinimo = TrimestreEmbarazo.SEGUNDO,
            nivelMinimo = NivelIngreso.MEDIO_BAJO,
            fuente = "Chef Oropeza",
            alergenos = listOf(Alergeno.LACTEOS, Alergeno.TRIGO)
        ),
        RecetaEmbarazo(
            nombre = "Helado suave de plátano con canela",
            ingredientes = listOf("2 plátanos maduros", "1/2 taza de leche de coco o almendra pasteurizada", "1 cdta de canela en polvo", "1 cda de nuez picada", "1 cdta de miel de maple"),
            preparacion = "Pela los plátanos y córtalos en rodajas. Colócalos en un recipiente hermético y congélalos por 4 horas. Coloca los plátanos congelados en la licuadora potente junto con la leche vegetal y la canela. Procesa hasta obtener una textura suave y cremosa de helado. Sirve de inmediato y espolvorea con nuez picada y miel si lo deseas.",
            kcal = 160,
            tipoComida = TipoComida.COLACION,
            trimestreMinimo = TrimestreEmbarazo.PRIMERO,
            nivelMinimo = NivelIngreso.BASICO,
            fuente = "Chef Oropeza",
            alergenos = listOf(Alergeno.NUECES),
            condicionesExcluidas = listOf("diabetes gestacional")
        ),
        RecetaEmbarazo(
            nombre = "Mini donas de plátano y chocolate al horno",
            ingredientes = listOf("1 plátano maduro", "1 huevo", "1/2 taza de harina de almendras", "1/4 cdta de polvo para hornear", "2 cdas de chispas de chocolate amargo"),
            preparacion = "Machaca el plátano en un tazón. Añade el huevo y bate bien. Incorpora la harina de almendras, el polvo para hornear y las chispas de chocolate. Vierte en moldes para mini donas engrasados y hornea a 180°C durante 15 minutos hasta que estén firmes y el huevo esté totalmente cocido.",
            kcal = 190,
            tipoComida = TipoComida.COLACION,
            trimestreMinimo = TrimestreEmbarazo.PRIMERO,
            nivelMinimo = NivelIngreso.ALTO,
            fuente = "Chef Oropeza (adaptación segura para embarazo)",
            alergenos = listOf(Alergeno.HUEVO, Alergeno.NUECES),
            condicionesExcluidas = listOf("diabetes gestacional")
        ),
        RecetaEmbarazo(
            nombre = "Té helado de mango y jengibre (bajo en cafeína)",
            ingredientes = listOf("1/2 taza de mango picado", "1 rebanada pequeña de jengibre fresco", "1 bolsa de té negro descafeinado", "1 taza de agua hirviendo", "Hielos al gusto", "1 cdta de miel de abeja"),
            preparacion = "Prepara el té en la taza de agua hirviendo con el jengibre fresco; deja reposar 5 minutos y retira la bolsa and el jengibre. En la licuadora procesa la pulpa de mango con el té templado y la miel. Sirve en un vaso alto con abundantes hielos. Ideal para reducir las náuseas.",
            kcal = 70,
            tipoComida = TipoComida.COLACION,
            trimestreMinimo = TrimestreEmbarazo.PRIMERO,
            nivelMinimo = NivelIngreso.BASICO,
            fuente = "Chef Oropeza (adaptación baja en cafeína)",
            alergenos = listOf(Alergeno.FRUCTOSA),
            condicionesExcluidas = listOf("diabetes gestacional", "anemia")
        )
    )

    // ─────────────────────────────────────────────────────────────────────
    // Alimentos a evitar durante el embarazo (fuente [2][3])
    // ─────────────────────────────────────────────────────────────────────
    val ALIMENTOS_A_EVITAR: List<AlimentoRiesgoEmbarazo> = listOf(
        AlimentoRiesgoEmbarazo("Pescado y carne crudos o poco cocidos", "Riesgo de listeriosis/toxoplasmosis"),
        AlimentoRiesgoEmbarazo("Quesos no pasteurizados (panela artesanal sin pasteurizar, algunos quesos frescos de rancho)", "Riesgo de listeriosis"),
        AlimentoRiesgoEmbarazo("Pescados altos en mercurio (tiburón, pez espada, blanquillo, atún patudo en exceso)", "Neurotoxicidad para el feto"),
        AlimentoRiesgoEmbarazo("Huevo crudo o tibio", "Riesgo de salmonelosis"),
        AlimentoRiesgoEmbarazo("Alcohol", "Sin nivel seguro conocido durante el embarazo"),
        AlimentoRiesgoEmbarazo("Embutidos fríos sin recalentar", "Riesgo de listeriosis"),
        AlimentoRiesgoEmbarazo("Exceso de cafeína (más de 200 mg/día, ~2 tazas de café)", "Asociado a bajo peso al nacer"),
        AlimentoRiesgoEmbarazo("Frutas y verduras sin lavar", "Riesgo de toxoplasmosis")
    )

    fun recetasPorPerfil(
        trimestre:  TrimestreEmbarazo,
        nivel:      NivelIngreso,
        tipo:       TipoComida,
        region:     RegionMexico = RegionMexico.GENERAL,
        alergenos:  List<Alergeno> = emptyList(),
        condiciones: List<String> = emptyList(),
        alimentosRegistrados: List<String> = emptyList()
    ): List<RecetaEmbarazo> {
        val nivelesAccesibles = when (nivel) {
            NivelIngreso.BASICO     -> listOf(NivelIngreso.BASICO)
            NivelIngreso.MEDIO_BAJO -> listOf(NivelIngreso.BASICO, NivelIngreso.MEDIO_BAJO)
            NivelIngreso.MEDIO      -> listOf(NivelIngreso.BASICO, NivelIngreso.MEDIO_BAJO, NivelIngreso.MEDIO)
            NivelIngreso.ALTO       -> NivelIngreso.entries.toList()
        }
        val trimestresAccesibles = TrimestreEmbarazo.entries.filter { it.ordinal <= trimestre.ordinal }

        val candidatas = RECETAS.filter { r ->
            r.tipoComida == tipo &&
                r.nivelMinimo in nivelesAccesibles &&
                r.trimestreMinimo in trimestresAccesibles &&
                r.esSeguraParaPerfil(alergenos, condiciones) &&
                (r.regiones.contains(RegionMexico.GENERAL) || r.regiones.contains(region))
        }

        if (alimentosRegistrados.isEmpty()) return candidatas

        val conIngredientes = candidatas.filter { receta ->
            alimentosRegistrados.any { nombreRegistrado ->
                receta.contieneIngrediente(nombreRegistrado)
            }
        }
        val sinIngredientes = candidatas.filter { it !in conIngredientes }
        return conIngredientes + sinIngredientes
    }

    fun generarPlanSemanal(
        semanas:    Int,
        nivel:      NivelIngreso,
        region:     RegionMexico = RegionMexico.GENERAL,
        alergenos:  List<Alergeno> = emptyList(),
        condiciones: List<String> = emptyList(),
        alimentosRegistrados: List<String> = emptyList()
    ): List<PlanDietaEmbarazoSemanal> {
        val trimestre = trimestrePorSemana(semanas)
        val macros    = macrosPorTrimestre(trimestre)
        val dias      = listOf("Lunes","Martes","Miércoles","Jueves","Viernes","Sábado","Domingo")

        val desayunos  = recetasPorPerfil(trimestre, nivel, TipoComida.DESAYUNO,  region, alergenos, condiciones, alimentosRegistrados)
        val comidas    = recetasPorPerfil(trimestre, nivel, TipoComida.COMIDA,    region, alergenos, condiciones, alimentosRegistrados)
        val cenas      = recetasPorPerfil(trimestre, nivel, TipoComida.CENA,      region, alergenos, condiciones, alimentosRegistrados)
        val colaciones = recetasPorPerfil(trimestre, nivel, TipoComida.COLACION,  region, alergenos, condiciones, alimentosRegistrados)

        return dias.mapIndexed { i, dia ->
            PlanDietaEmbarazoSemanal(
                diaSemana = dia,
                comidas = ComidasDiariasEmbarazo(
                    desayuno  = desayunos.getOrNull(i % desayunos.size.coerceAtLeast(1))?.nombre ?: "Consultar con nutrióloga",
                    colacion1 = colaciones.getOrNull(i % colaciones.size.coerceAtLeast(1))?.nombre ?: "Consultar con nutrióloga",
                    comida    = comidas.getOrNull(i % comidas.size.coerceAtLeast(1))?.nombre ?: "Consultar con nutrióloga",
                    colacion2 = colaciones.getOrNull((i + 1) % colaciones.size.coerceAtLeast(1))?.nombre ?: "Consultar con nutrióloga",
                    cena      = cenas.getOrNull(i % cenas.size.coerceAtLeast(1))?.nombre ?: "Consultar con nutrióloga"
                ),
                macros = macros,
                trimestre = trimestre
            )
        }
    }

    fun resumenNutricional(
        semanas:    Int,
        condiciones: List<String> = emptyList()
    ): ResumenNutricionalEmbarazo {
        val trimestre = trimestrePorSemana(semanas)
        val macros    = macrosPorTrimestre(trimestre)
        val alertas   = mutableListOf<String>()
        val alertasCondicion = mutableListOf<String>()

        if (trimestre == TrimestreEmbarazo.PRIMERO)
            alertas.add("Ácido fólico (600 µg/día) es crítico en el primer trimestre para el desarrollo del tubo neural")
        alertas.add("Hierro: 30 mg/día — el doble que en una mujer no embarazada, para prevenir anemia")
        alertas.add("Hidratación: 2.3 litros de agua al día")
        alertas.add("Cafeína: no exceder 200 mg/día (aprox. 1-2 tazas de café)")
        alertas.add("Realizar 3 comidas principales + 2 colaciones, sin saltarse el desayuno")
        if (trimestre != TrimestreEmbarazo.PRIMERO)
            alertas.add("Ganancia de peso esperada total: 11-16 kg si el peso pregestacional era adecuado (ajustar con médico si hay bajo peso u obesidad)")

        val condicionesNorm = condiciones.map { it.lowercase() }
        if (condicionesNorm.any { it.contains("diabetes gestacional") })
            alertasCondicion.add("Diabetes gestacional: priorizar carbohidratos complejos, evitar azúcares simples y atoles/postres endulzados; fraccionar comidas")
        if (condicionesNorm.any { it.contains("hipertension") || it.contains("hipertensión") })
            alertasCondicion.add("Hipertensión: reducir sal añadida y evitar embutidos/alimentos procesados")
        if (condicionesNorm.any { it.contains("anemia") })
            alertasCondicion.add("Anemia: reforzar hierro con carnes rojas magras, lentejas y frijol, acompañados de vitamina C para mejorar absorción")
        if (condicionesNorm.any { it.contains("náuseas") || it.contains("vómitos") || it.contains("nausea") })
            alertasCondicion.add("Náuseas o vómitos: prioriza comidas pequeñas y frecuentes, evita alimentos grasosos o de olor fuerte")
        
        // NOTA DE ALINEACIÓN: Las condiciones como la anemia no requieren excluir platos completos del recetario estándar (salvo té/café que inhiben el hierro) ya que las recetas contienen ingredientes nutritivos y el aporte se optimiza mediante el resumen de alertas.

        val alimentosClave = listOf(
            "Vegetales de hoja verde", "Leguminosas", "Huevo bien cocido", "Cítricos",
            "Carnes magras", "Pescado bajo en mercurio bien cocido", "Lácteos pasteurizados", "Frutos secos"
        )

        return ResumenNutricionalEmbarazo(
            trimestreLabel   = trimestre.label,
            macroObjetivo    = macros,
            alimentosClave   = alimentosClave,
            alertas          = alertas,
            alimentosAEvitar = ALIMENTOS_A_EVITAR,
            alertasCondicion = alertasCondicion
        )
    }

    fun necesitaAjusteIA(perfil: PerfilEmbarazo): Boolean {
        val condicionesNorm = perfil.condiciones.map { it.lowercase() }
        val tieneDiabetes = condicionesNorm.any { it.contains("diabetes") }
        val tieneHipertension = condicionesNorm.any { it.contains("hipertens") || it.contains("hipertension") }
        val tieneAnemia = condicionesNorm.any { it.contains("anemia") }

        val edadMaternaExtrema = perfil.edad in 1..19 || perfil.edad > 35
        val esGemelar = perfil.esGemelar

        val imc = perfil.imcPregestacional
        val imcExtremo = imc > 0.0 && (imc < 18.5 || imc >= 30.0)

        return tieneDiabetes || tieneHipertension || tieneAnemia || edadMaternaExtrema || esGemelar || imcExtremo
    }
}
