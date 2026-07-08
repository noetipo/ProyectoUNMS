package unmsm.edu.pe.tesis.infrastructure.export;

/**
 * Texto LITERAL de las plantillas de asesoría. El texto fuera de los marcadores {...}
 * no se modifica; el renderer solo sustituye los marcadores por los valores resueltos.
 * La primera línea (título) se emplea como encabezado en negrita.
 */
public final class PlantillaAsesoria {

    private PlantillaAsesoria() {}

    /** Solicitud de designación del asesor de tesis (al crear la solicitud). */
    public static final String SOLICITUD = """
            Solicitud designación del asesor de tesis

            “{LEMA_ANIO}”

            {CIUDAD}, {FECHA_LARGA} del {ANIO}

            Señor Doctor
            {NOMBRE_DIRECTOR}
            Director de la Unidad de Posgrado
            Facultad de {FACULTAD}
            Presente.-

            Asunto: Designación de asesor de tesis {NIVEL_ADJ}

            Es grato dirigirme a usted para expresarle un saludo cordial y por intermedio de la presente, en mi calidad de estudiante del {NIVEL} en {PROGRAMA} solicito respetuosamente se designe al {GRADO_ASESOR} {NOMBRE_ASESOR} como asesor de mi tesis titulada: “{TITULO_TESIS}”

            Adjunto carta de aceptación del asesor de tesis.

            Es propicia la oportunidad para expresarle los sentimientos de mi especial consideración.

            Atentamente,

            _________________________
            {APELLIDOS_NOMBRES_ESTUDIANTE}
            {NIVEL} en {PROGRAMA}
            Correo electrónico: {CORREO_ESTUDIANTE}
            Celular: {CELULAR_ESTUDIANTE}
            """;

    /** Carta de aceptación del asesor (al aceptar el docente). */
    public static final String CARTA = """
            Carta de aceptación del asesor y co-asesor

            “{LEMA_ANIO}”

            {CIUDAD}, {FECHA_LARGA} del {ANIO}

            Señor Doctor
            {NOMBRE_DIRECTOR}
            Director de la Unidad de Posgrado
            Facultad de {FACULTAD}
            Presente.-

            Asunto: Aceptación de asesor de tesis {NIVEL_ADJ}

            Es grato dirigirme a usted para expresarle un saludo cordial y en mi condición de docente {CONDICION} de la Facultad de {FACULTAD} acepto ser {TIPO_ASESOR} de la tesis titulada: “{TITULO_TESIS}”, del {GRADO_ESTUDIANTE} {NOMBRE_ESTUDIANTE} del {NIVEL} en {PROGRAMA}.

            Es propicia la oportunidad para expresarle los sentimientos de mi especial consideración.

            Atentamente,

            ____________________________
            {NOMBRE_APELLIDOS_ASESOR}
            """;
}
