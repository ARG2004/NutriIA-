package com.example.nutriia.embarazo

import androidx.compose.ui.graphics.Color

enum class NivelSintoma(val labelEs: String, val labelEn: String, val color: Color) {
    NORMAL("Normal", "Normal", Color(0xFF4CAF50)),
    CONSULTA("Consulta Médica", "Doctor Visit Required", Color(0xFFFF9800)),
    URGENCIA("Urgencia Obstétrica", "Obstetric Emergency", Color(0xFFF44336))
}

data class DetalleSintoma(
    val nombreEs: String,
    val nombreEn: String,
    val nivel: NivelSintoma,
    val detalleEs: String,
    val detalleEn: String,
    val recomendacionEs: String,
    val recomendacionEn: String
)

object SintomasAnalyzer {

    fun analizarSintoma(sintoma: String, trimestre: Int): DetalleSintoma {
        val s = sintoma.lowercase()
        return when {
            // --- ALERTAS / URGENCIA ---
            (s.contains("hinchazón") || s.contains("swelling")) && (s.contains("manos") || s.contains("cara") || s.contains("edema") || s.contains("hands") || s.contains("face") || s.contains("sudden") || s.contains("repentina")) -> DetalleSintoma(
                nombreEs = "Hinchazón repentina de manos, cara o pies",
                nombreEn = "Sudden swelling of hands, face, or feet",
                nivel = NivelSintoma.URGENCIA,
                detalleEs = "La hinchazón súbita o edema puede ser un signo importante de preeclampsia, una condición grave de presión arterial alta.",
                detalleEn = "Sudden swelling or edema is a key warning sign of preeclampsia, a serious high blood pressure condition.",
                recomendacionEs = "Acude de inmediato a urgencias y mide tu presión arterial de inmediato.",
                recomendacionEn = "Go to emergency immediately and have your blood pressure checked."
            )
            s.contains("lucecitas") || s.contains("visión borrosa") || s.contains("flashing lights") -> DetalleSintoma(
                nombreEs = "Ver lucecitas (fosfenos) o visión borrosa",
                nombreEn = "Seeing flashing lights or blurred vision",
                nivel = NivelSintoma.URGENCIA,
                detalleEs = "La alteración visual o ver lucecitas indica una afectación del sistema nervioso central debido a presión arterial críticamente elevada.",
                detalleEn = "Visual changes or seeing flashing lights indicate central nervous system involvement due to critically high blood pressure.",
                recomendacionEs = "Trasládate de inmediato al hospital para valoración urgente.",
                recomendacionEn = "Head immediately to the hospital for urgent medical evaluation."
            )
            s.contains("zumbido") || s.contains("tinnitus") || s.contains("ringing") -> DetalleSintoma(
                nombreEs = "Zumbido de oídos constante (tinnitus)",
                nombreEn = "Constant ringing in the ears (tinnitus)",
                nivel = NivelSintoma.URGENCIA,
                detalleEs = "El zumbido constante puede estar ocasionado por presión arterial alta (hipertensión gestacional o preeclampsia).",
                detalleEn = "Constant ringing in the ears can be caused by high blood pressure (gestational hypertension or preeclampsia).",
                recomendacionEs = "Requiere atención médica inmediata. Acude a urgencias.",
                recomendacionEn = "Requires immediate medical attention. Go to emergency."
            )
            (s.contains("dolor de cabeza") || s.contains("headache")) && (s.contains("intenso") || s.contains("headache") || s.contains("intense")) -> DetalleSintoma(
                nombreEs = "Dolor de cabeza intenso y persistente",
                nombreEn = "Very intense and persistent headache",
                nivel = NivelSintoma.URGENCIA,
                detalleEs = "Un dolor de cabeza severo que no cede con el descanso es un síntoma de alarma neurológica relacionado con la preeclampsia.",
                detalleEn = "A severe headache that does not go away with rest is a neurological warning sign related to preeclampsia.",
                recomendacionEs = "Mide tu presión y acude de inmediato a urgencias.",
                recomendacionEn = "Check your blood pressure and go to emergency immediately."
            )
            s.contains("boca del estómago") || s.contains("epigastrio") || s.contains("upper stomach") -> DetalleSintoma(
                nombreEs = "Dolor agudo en la boca del estómago",
                nombreEn = "Sharp pain in the upper stomach",
                nivel = NivelSintoma.URGENCIA,
                detalleEs = "El dolor en la boca del estómago (epigastralgia) puede sugerir inflamación o distensión de la cápsula del hígado por preeclampsia severa (síndrome de HELLP).",
                detalleEn = "Pain in the upper stomach (epigastralgia) can suggest liver capsule distension/inflammation due to severe preeclampsia (HELLP syndrome).",
                recomendacionEs = "Busca atención de urgencias de inmediato.",
                recomendacionEn = "Seek emergency medical attention immediately."
            )
            s.contains("implantación") || s.contains("sangrado de implantación") || s.contains("implantation bleeding") -> DetalleSintoma(
                nombreEs = "Posible sangrado de implantación (muy leve)",
                nombreEn = "Possible implantation bleeding (very light)",
                nivel = NivelSintoma.NORMAL,
                detalleEs = "Ocurre cuando el óvulo fertilizado se adhiere a la pared uterina. Es escaso, dura 1-2 días y es de color rosado o marrón.",
                detalleEn = "Occurs when the fertilized egg attaches to the uterine wall. It is light, lasts 1-2 days, and is pinkish or brownish.",
                recomendacionEs = "Usa toallas sanitarias leves. Si el sangrado es rojo brillante o abundante, acude a urgencias.",
                recomendacionEn = "Use light panty liners. If the bleeding becomes bright red or heavy, go to emergency."
            )
            ((s.contains("sangrado") && !s.contains("nasal") && !s.contains("nasales")) || (s.contains("bleeding") && !s.contains("nasal") && !s.contains("nose")) || s.contains("salida de líquido") || s.contains("fluid leaking")) -> DetalleSintoma(
                nombreEs = "Sangrado vaginal o pérdida de líquido",
                nombreEn = "Vaginal bleeding or fluid leaking",
                nivel = NivelSintoma.URGENCIA,
                detalleEs = "Puede indicar amenaza de aborto (1er trimestre), desprendimiento de placenta (2do/3er trimestre) o ruptura de la fuente.",
                detalleEn = "Could indicate miscarriage threat (1st trimester), placental abruption (2nd/3rd trimester), or rupture of membranes.",
                recomendacionEs = "Ve directamente al área de urgencias de maternidad. Evita esfuerzos.",
                recomendacionEn = "Go directly to the maternity emergency room. Avoid physical strain."
            )
            (s.contains("movimientos") || s.contains("movements")) && (s.contains("disminución") || s.contains("ausencia") || s.contains("decrease") || s.contains("absence")) -> DetalleSintoma(
                nombreEs = "Disminución notable o ausencia de movimientos del bebé",
                nombreEn = "Noticeable decrease or absence of baby movements",
                nivel = NivelSintoma.URGENCIA,
                detalleEs = "La reducción drástica de la actividad del bebé puede ser un síntoma de compromiso o sufrimiento fetal.",
                detalleEn = "A drastic reduction in baby activity can be a sign of fetal compromise or distress.",
                recomendacionEs = "Come algo dulce, recuéstate de lado izquierdo. Si no percibes movimientos en 1 hora, ve al hospital.",
                recomendacionEn = "Eat something sweet, lie on your left side. If you don't feel movements within 1 hour, go to the hospital."
            )
            (s.contains("contracciones") || s.contains("contractions")) && ((s.contains("dolorosas") && !s.contains("no dolorosas")) || s.contains("frecuentes antes de tiempo") || s.contains("before term") || s.contains("painful")) -> DetalleSintoma(
                nombreEs = "Contracciones dolorosas frecuentes antes de la semana 37",
                nombreEn = "Frequent painful contractions before term (week 37)",
                nivel = NivelSintoma.URGENCIA,
                detalleEs = "Las contracciones dolorosas y rítmicas antes de la semana 37 son indicativo de una posible amenaza de parto pretérmino.",
                detalleEn = "Painful and rhythmic contractions before week 37 indicate a possible preterm labor threat.",
                recomendacionEs = "Acude de urgencia al hospital de inmediato para frenar el trabajo de parto.",
                recomendacionEn = "Go immediately to the hospital's emergency room to stop labor progression."
            )

            // --- CONSULTA ---
            s.contains("vómitos frecuentes") || s.contains("vomiting") && s.contains("frequent") -> DetalleSintoma(
                nombreEs = "Vómitos frecuentes (dificultan comer/beber)",
                nombreEn = "Frequent vomiting (impedes eating/drinking)",
                nivel = NivelSintoma.CONSULTA,
                detalleEs = "Los vómitos excesivos pueden provocar deshidratación, pérdida de peso y desequilibrio electrolítico (hiperémesis gravídica).",
                detalleEn = "Excessive vomiting can lead to dehydration, weight loss, and electrolyte imbalance (hyperemesis gravidarum).",
                recomendacionEs = "Agenda consulta médica regular. Toma sorbos pequeños de suero oral.",
                recomendacionEn = "Schedule a regular medical check-up. Take small sips of oral rehydration solutions."
            )
            s.contains("infección vaginal") || s.contains("mal olor") || s.contains("comezón") || s.contains("vaginal infection") -> DetalleSintoma(
                nombreEs = "Infección vaginal o flujo inusual",
                nombreEn = "Vaginal infection or unusual discharge",
                nivel = NivelSintoma.CONSULTA,
                detalleEs = "Las infecciones vaginales son comunes por cambios de pH. Si no se tratan, aumentan el riesgo de ruptura prematura de membranas.",
                detalleEn = "Vaginal infections are common due to pH changes. If left untreated, they increase the risk of premature rupture of membranes.",
                recomendacionEs = "Agenda cita con tu ginecólogo. No uses duchas vaginales ni te automediques.",
                recomendacionEn = "Schedule an appointment with your gynecologist. Do not use vaginal douches or self-medicate."
            )
            s.contains("ardor") || s.contains("molestias al orinar") || s.contains("burning") && s.contains("urinating") -> DetalleSintoma(
                nombreEs = "Ardor, dolor o molestias al orinar",
                nombreEn = "Burning, pain, or discomfort when urinating",
                nivel = NivelSintoma.CONSULTA,
                detalleEs = "Es un síntoma característico de infección de vías urinarias (IVU), la cual es muy común en el embarazo debido a la relajación de los uréteres.",
                detalleEn = "A characteristic symptom of urinary tract infection (UTI), which is highly common in pregnancy due to ureteral relaxation.",
                recomendacionEs = "Agenda consulta regular para un análisis de orina y tratamiento con antibiótico seguro.",
                recomendacionEn = "Schedule a regular consultation for a urinalysis and safe antibiotic treatment."
            )
            s.contains("gripe") || Regex("\\btos\\b").containsMatchIn(s) || s.contains("fiebre menor") || Regex("\\bflu\\b").containsMatchIn(s) || s.contains("fever under") -> DetalleSintoma(
                nombreEs = "Gripe, tos, diarrea o fiebre leve (<38°C)",
                nombreEn = "Flu, cough, diarrhea, or mild fever (<38°C / 100.4°F)",
                nivel = NivelSintoma.CONSULTA,
                detalleEs = "Infecciones virales leves. El sistema inmune está adaptado para proteger al bebé, haciéndote más susceptible a resfriados.",
                detalleEn = "Mild viral infections. The immune system adapts to protect the baby, making you more susceptible to common colds.",
                recomendacionEs = "Agenda consulta si persiste. Mantente muy bien hidratada y descansa. No tomes antivirales ni analgésicos sin receta.",
                recomendacionEn = "Schedule a consultation if it persists. Stay well hydrated and rest. Do not take over-the-counter antivirals or painkillers."
            )
            s.contains("ronchas") || s.contains("picazón persistente") || s.contains("hives") || s.contains("itching") -> DetalleSintoma(
                nombreEs = "Ronchas o picazón persistente en la piel",
                nombreEn = "Hives or persistent itching on the skin",
                nivel = NivelSintoma.CONSULTA,
                detalleEs = "La picazón intensa (prurito), especialmente en palmas de manos y plantas de pies, puede ser signo de colestasis del embarazo.",
                detalleEn = "Intense itching (pruritus), especially on the palms of hands and soles of feet, can be a sign of obstetric cholestasis.",
                recomendacionEs = "Agenda consulta regular para evaluar el estado y función de tu hígado.",
                recomendacionEn = "Schedule a regular visit to evaluate liver enzymes and function."
            )

            // --- NORMAL / TRIMESTRE 1 ---
            s.contains("retraso") || s.contains("menstrual") || s.contains("missed period") -> DetalleSintoma(
                nombreEs = "Retraso menstrual importante",
                nombreEn = "Missed period",
                nivel = NivelSintoma.NORMAL,
                detalleEs = "Es el primer signo físico clásico del embarazo provocado por la implantación y el desarrollo temprano del embrión.",
                detalleEn = "The first classic physical sign of pregnancy caused by implantation and early embryo development.",
                recomendacionEs = "Confirma con prueba y agenda tu primera cita prenatal en el IMSS.",
                recomendacionEn = "Confirm with a test and schedule your first prenatal consultation at IMSS."
            )
            s.contains("somnolencia") || s.contains("sueño") || s.contains("cansancio") || s.contains("fatiga") || s.contains("fatigue") || s.contains("tiredness") -> DetalleSintoma(
                nombreEs = "Fatiga leve y aumento de somnolencia",
                nombreEn = "Mild fatigue and increased sleepiness",
                nivel = NivelSintoma.NORMAL,
                detalleEs = "Durante el primer trimestre, el aumento de la progesterona y el esfuerzo metabólico para formar la placenta te harán sentir muy cansada.",
                detalleEn = "During the first trimester, the surge of progesterone and metabolic effort to build the placenta will make you feel very tired.",
                recomendacionEs = "Duerme siestas cortas, delega esfuerzos y mantén un consumo calórico adecuado.",
                recomendacionEn = "Take short naps, delegate physical tasks, and maintain adequate caloric intake."
            )
            s.contains("náuseas") || s.contains("nausea") -> DetalleSintoma(
                nombreEs = "Náuseas y/o vómitos matutinos",
                nombreEn = "Nausea and/or morning sickness",
                nivel = NivelSintoma.NORMAL,
                detalleEs = "Provocado por el rápido aumento de la hormona del embarazo hCG y la hormona progesterona, que ralentizan la digestión.",
                detalleEn = "Triggered by the rapid increase of pregnancy hormone hCG and progesterone, which slows down digestion.",
                recomendacionEs = "Come porciones pequeñas y frecuentes. Consume galletas saladas al despertar antes de levantarte de la cama.",
                recomendacionEn = "Eat small, frequent meals. Try dry crackers before getting out of bed in the morning."
            )
            s.contains("sensibilidad en los senos") || s.contains("senos") || s.contains("pechos") || s.contains("breast") -> DetalleSintoma(
                nombreEs = "Sensibilidad, tensión e hinchazón en senos",
                nombreEn = "Breast sensitivity, tension, and swelling",
                nivel = NivelSintoma.NORMAL,
                detalleEs = "El incremento de estrógenos y progesterona estimula las glándulas mamarias y aumenta el flujo de sangre en la zona.",
                detalleEn = "The increase in estrogen and progesterone stimulates mammary glands and increases blood flow to the area.",
                recomendacionEs = "Usa un sujetador de maternidad de algodón sin aros, con tirantes anchos para mayor soporte.",
                recomendacionEn = "Wear a soft cotton maternity bra without underwires, with wide straps for better support."
            )
            s.contains("frecuente") && s.contains("vejiga") || s.contains("urination") && s.contains("bladder") -> DetalleSintoma(
                nombreEs = "Micción más frecuente",
                nombreEn = "More frequent urination",
                nivel = NivelSintoma.NORMAL,
                detalleEs = "En el ${trimestre}º trimestre, el útero en crecimiento presiona la vejiga, reduciendo su capacidad de almacenamiento de orina.",
                detalleEn = "In the $trimestre trimester, the expanding uterus presses against the bladder, reducing its urine storage capacity.",
                recomendacionEs = "No contengas la orina para prevenir infecciones. Reduce la ingesta de líquidos 2 horas antes de dormir.",
                recomendacionEn = "Do not hold in urine to prevent infections. Limit fluid intake 2 hours before bedtime."
            )
            s.contains("humor") || s.contains("olores") || s.contains("mood") || s.contains("odors") -> DetalleSintoma(
                nombreEs = "Cambios de humor y aversión a olores",
                nombreEn = "Mood swings and odor aversion",
                nivel = NivelSintoma.NORMAL,
                detalleEs = "Los altos niveles de hormonas impactan los neurotransmisores cerebrales y agudizan el sentido del olfato.",
                detalleEn = "High hormone levels impact brain neurotransmitters and sharpen your sense of smell.",
                recomendacionEs = "Descansa lo suficiente, platica tus emociones y evita olores desencadenantes de náuseas.",
                recomendacionEn = "Get enough rest, share your emotions, and avoid strong smell triggers."
            )

            // --- NORMAL / TRIMESTRE 2 ---
            s.contains("antojos") || s.contains("apetito") || s.contains("cravings") || s.contains("appetite") -> DetalleSintoma(
                nombreEs = "Aparición de antojos y aumento del apetito",
                nombreEn = "Food cravings and increased appetite",
                nivel = NivelSintoma.NORMAL,
                detalleEs = "Las náuseas disminuyen y tu cuerpo solicita más nutrientes para apoyar el rápido desarrollo óseo y de tejidos del bebé.",
                detalleEn = "Nausea subsides, and your body demands more nutrients to support the baby's rapid bone and tissue development.",
                recomendacionEs = "Responde a tu apetito con opciones saludables ricas en calcio, hierro y proteína. Evita la comida chatarra.",
                recomendacionEn = "Satisfy your hunger with healthy foods rich in calcium, iron, and protein. Limit junk food."
            )
            s.contains("costados del abdomen") || s.contains("ligamento redondo") || s.contains("ligament pain") -> DetalleSintoma(
                nombreEs = "Dolor en los costados (ligamento redondo)",
                nombreEn = "Side abdominal pain (round ligament)",
                nivel = NivelSintoma.NORMAL,
                detalleEs = "El útero se estira para alojar al bebé, provocando que los ligamentos redondos que lo sostienen sufran espasmos leves.",
                detalleEn = "The uterus stretches to accommodate the baby, causing the supporting round ligaments to experience mild spasms.",
                recomendacionEs = "Realiza estiramientos prenatales suaves. Evita cambiar de posición de forma muy brusca.",
                recomendacionEn = "Perform gentle prenatal stretches. Avoid sudden changes in body position."
            )
            s.contains("primeros movimientos") || s.contains("burbujas") || s.contains("flutters") || s.contains("movements") && s.contains("baby") -> DetalleSintoma(
                nombreEs = "Sensación de primeros movimientos fetales",
                nombreEn = "Feeling the first baby movements",
                nivel = NivelSintoma.NORMAL,
                detalleEs = "Es una de las mejores etapas del embarazo. Hacia la semana 18-22 comenzarás a sentir pequeñas burbujas o aleteos.",
                detalleEn = "One of the best milestones of pregnancy. Around weeks 18-22 you'll start feeling light bubbles or flutters.",
                recomendacionEs = "Tómate momentos tranquilos para relajarte y conectar con los movimientos de tu bebé.",
                recomendacionEn = "Take quiet moments to relax and bond with your baby's movements."
            )
            s.contains("espalda bajo") || s.contains("gravedad") || s.contains("back pain") || s.contains("lumbar") -> DetalleSintoma(
                nombreEs = "Dolor de espalda bajo y zona lumbar",
                nombreEn = "Lower back pain and lumbar discomfort",
                nivel = NivelSintoma.NORMAL,
                detalleEs = "El crecimiento del vientre desplaza tu centro de gravedad hacia adelante, obligando a tu espalda a sobreesforzarse.",
                detalleEn = "The growing belly shifts your center of gravity forward, forcing your lower back muscles to overcompensate.",
                recomendacionEs = "Usa zapatos planos y cómodos. Mantén una buena postura y utiliza una almohada entre tus piernas al dormir.",
                recomendacionEn = "Wear flat, comfortable shoes. Practice good posture and use a pillow between your knees at night."
            )
            s.contains("calambres") || s.contains("piernas") || s.contains("cramps") || s.contains("leg") -> DetalleSintoma(
                nombreEs = "Calambres nocturnos en las piernas",
                nombreEn = "Nighttime leg cramps",
                nivel = NivelSintoma.NORMAL,
                detalleEs = "Ocurre debido a la presión en nervios de las piernas por el útero o por un desbalance de minerales (calcio/magnesio).",
                detalleEn = "Happens due to pressure on leg nerves from the uterus or a minor mineral imbalance (calcium/magnesium).",
                recomendacionEs = "Realiza estiramientos de pantorrilla antes de dormir. Mantén una ingesta adecuada de lácteos y plátanos.",
                recomendacionEn = "Stretch your calf muscles before sleeping. Maintain adequate intake of dairy and bananas."
            )
            s.contains("estreñimiento") || s.contains("acidez") || s.contains("reflujo") || s.contains("constipation") || s.contains("heartburn") -> DetalleSintoma(
                nombreEs = "Estreñimiento o acidez estomacal (reflujo)",
                nombreEn = "Constipation or heartburn (reflux)",
                nivel = NivelSintoma.NORMAL,
                detalleEs = "La progesterona relaja los músculos del tracto digestivo, ralentizando el tránsito y facilitando el reflujo gástrico.",
                detalleEn = "Progesterone relaxes digestive tract muscles, slowing transit and making acid reflux more common.",
                recomendacionEs = "Come despacio. Incrementa tu consumo de fibra dietética (avena, frutas, verduras) y bebe bastante agua.",
                recomendacionEn = "Eat slowly. Increase dietary fiber intake (oats, fruits, vegetables) and drink plenty of water."
            )

            // --- NORMAL / TRIMESTRE 3 ---
            s.contains("respirar") || s.contains("diafragma") || s.contains("shortness of breath") -> DetalleSintoma(
                nombreEs = "Dificultad leve para respirar",
                nombreEn = "Mild shortness of breath",
                nivel = NivelSintoma.NORMAL,
                detalleEs = "El tamaño uterino en el 3er trimestre empuja el estómago y el diafragma hacia arriba, comprimiendo los pulmones.",
                detalleEn = "The uterine size in the 3rd trimester pushes the stomach and diaphragm upwards, compressing your lungs.",
                recomendacionEs = "Siéntate derecha para dar más espacio a tus pulmones. Realiza actividades despacio, sin agitarte.",
                recomendacionEn = "Sit up straight to give your lungs more space. Perform activities slowly, without overexertion."
            )
            s.contains("braxton hicks") || s.contains("práctica") || s.contains("irregular") -> DetalleSintoma(
                nombreEs = "Contracciones de Braxton Hicks",
                nombreEn = "Braxton Hicks contractions (practice)",
                nivel = NivelSintoma.NORMAL,
                detalleEs = "Son contracciones falsas o de entrenamiento. Son irregulares, no son dolorosas y desaparecen al cambiar de actividad.",
                detalleEn = "False or practice contractions. They are irregular, painless, and disappear when you change position or rest.",
                recomendacionEs = "Si las sientes, recuéstate o toma un baño tibio. Si se vuelven rítmicas o dolorosas, vigílalas.",
                recomendacionEn = "If you feel them, lie down or take a warm bath. If they become rhythmic or painful, track them."
            )
            s.contains("presión pélvica") || s.contains("pelvis") || s.contains("pelvic pressure") -> DetalleSintoma(
                nombreEs = "Presión pélvica aumentada",
                nombreEn = "Increased pelvic pressure",
                nivel = NivelSintoma.NORMAL,
                detalleEs = "El bebé empieza a descender y encajarse en la pelvis, aliviando la respiración pero aumentando el peso sobre el pubis.",
                detalleEn = "The baby begins to descend and drop into the pelvis, easing breathing but increasing weight on the pubic bone.",
                recomendacionEs = "Realiza ejercicios de balanceo pélvico. Usa una faja de soporte prenatal si la molestia es fuerte.",
                recomendacionEn = "Perform pelvic tilt exercises. Use a prenatal support belt if the discomfort is strong."
            )
            s.contains("tapón mucoso") || s.contains("mucus plug") -> DetalleSintoma(
                nombreEs = "Pérdida del tapón mucoso",
                nombreEn = "Loss of mucus plug",
                nivel = NivelSintoma.NORMAL,
                detalleEs = "Es un flujo espeso y gelatinoso que sella el cuello uterino. Indica que este empieza a ablandarse y dilatarse.",
                detalleEn = "A thick, gelatinous discharge sealing the cervix. It indicates that the cervix is softening and dilating.",
                recomendacionEs = "Indica proximidad del parto (días o semanas). Mantén reposo y avisa a tu médico en tu cita ordinaria.",
                recomendacionEn = "Indicates labor is approaching (days or weeks). Rest and report to your doctor during your regular visit."
            )
            s.contains("ansiedad") || s.contains("nerviosismo") || s.contains("nervousness") || s.contains("anxiety") -> DetalleSintoma(
                nombreEs = "Ansiedad o nerviosismo por el parto",
                nombreEn = "Anxiety or nervousness about birth",
                nivel = NivelSintoma.NORMAL,
                detalleEs = "Es una reacción emocional natural ante la cercanía del parto y el inicio de la maternidad.",
                detalleEn = "A natural emotional reaction to the proximity of childbirth and the beginning of motherhood.",
                recomendacionEs = "Platica tus dudas en tu consulta prenatal. Realiza ejercicios de relajación y respiración.",
                recomendacionEn = "Discuss your doubts in your prenatal consultation. Practice relaxation and breathing exercises."
            )

            // --- FALLBACK POR DEFECTO ---
            else -> DetalleSintoma(
                nombreEs = sintoma,
                nombreEn = sintoma,
                nivel = NivelSintoma.NORMAL,
                detalleEs = "Es un síntoma común asociado a los cambios anatómicos y hormonales durante la gestación.",
                detalleEn = "Common symptom associated with anatomical and hormonal changes during pregnancy.",
                recomendacionEs = "Lleva un diario de su frecuencia y coméntalo con tu ginecólogo en tu siguiente cita regular.",
                recomendacionEn = "Keep a log of its frequency and discuss it with your gynecologist during your next regular visit."
            )
        }
    }
}
