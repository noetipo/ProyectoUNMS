package unmsm.edu.pe.tesis.infrastructure.export;

/**
 * Texto LITERAL del Dictamen de designación de asesor. Solo se sustituyen los marcadores {...};
 * los considerandos y el pie son parametrizables (no hardcodeados).
 */
public final class PlantillaDictamen {

    private PlantillaDictamen() {}

    public static final String DICTAMEN = """
            UNIVERSIDAD NACIONAL MAYOR DE SAN MARCOS
            Universidad del Perú. Decana de América
            FACULTAD DE {FACULTAD}
            VICEDECANATO DE INVESTIGACIÓN Y POSGRADO – SECCIÓN {NIVEL_SECCION}

            {CIUDAD}, {FECHA_LARGA} del {ANIO}

            DICTAMEN N° {NUMERO_DICTAMEN}

            DICTAMEN: DESIGNACIÓN DE ASESOR

            Visto la solicitud de designación de asesor, registrada con expediente digital N° {EXPEDIENTE}, de fecha {FECHA_SOLICITUD}.

            CONSIDERANDO:

            {CONSIDERANDOS}

            SE DICTAMINA:

            Designar como asesor al {GRADO_ASESOR} {NOMBRE_ASESOR}{BLOQUE_COASESOR} del tema de tesis titulada: “{TITULO_TESIS}” presentado por {TRATAMIENTO_ESTUDIANTE}: {NOMBRE_ESTUDIANTE}, del {PROGRAMA}.

            Regístrese, comuníquese y archívese.


            {NOMBRE_DIRECTOR}
            DIRECTOR DE LA UNIDAD DE POSGRADO

            {PIE}
            """;

    /** Dictamen de aprobación del proyecto de tesis, posterior a la defensa (Etapa 5). */
    public static final String APROBACION = """
            UNIVERSIDAD NACIONAL MAYOR DE SAN MARCOS
            Universidad del Perú. Decana de América
            FACULTAD DE {FACULTAD}
            VICEDECANATO DE INVESTIGACIÓN Y POSGRADO – SECCIÓN {NIVEL_SECCION}

            {CIUDAD}, {FECHA_LARGA} del {ANIO}

            DICTAMEN N° {NUMERO_DICTAMEN}

            DICTAMEN: APROBACIÓN DEL PROYECTO DE TESIS

            Visto el expediente digital N° {EXPEDIENTE}, sobre la aprobación del proyecto de tesis sustentado en la defensa realizada el {FECHA_DEFENSA}, en modalidad {MODALIDAD}.

            CONSIDERANDO:

            {CONSIDERANDOS}

            Que, los revisores designados {REVISORES} evaluaron el proyecto conforme a la rúbrica oficial y emitieron su conformidad;

            Que, realizada la defensa del proyecto, el resultado del acto fue: {RESULTADO};

            SE DICTAMINA:

            Aprobar el proyecto de tesis titulado: “{TITULO_TESIS}”, presentado por {TRATAMIENTO_ESTUDIANTE}: {NOMBRE_ESTUDIANTE}, del {PROGRAMA}.

            La presente aprobación tiene vigencia hasta el {VIGENCIA}, plazo dentro del cual debe ejecutarse la investigación y sustentarse la tesis.

            Regístrese, comuníquese y archívese.


            {NOMBRE_DIRECTOR}
            DIRECTOR DE LA UNIDAD DE POSGRADO

            {PIE}
            """;

    /** Dictamen de designación del Jurado Informante que evalúa el informe final (Etapa 7). */
    public static final String JURADO_INFORME = """
            UNIVERSIDAD NACIONAL MAYOR DE SAN MARCOS
            Universidad del Perú. Decana de América
            FACULTAD DE {FACULTAD}
            VICEDECANATO DE INVESTIGACIÓN Y POSGRADO – SECCIÓN {NIVEL_SECCION}

            {CIUDAD}, {FECHA_LARGA} del {ANIO}

            DICTAMEN N° {NUMERO_DICTAMEN}

            DICTAMEN: DESIGNACIÓN DE JURADO INFORMANTE

            Visto el expediente digital N° {EXPEDIENTE}, sobre la solicitud de Jurado Informante para la evaluación del informe final de tesis.

            CONSIDERANDO:

            {CONSIDERANDOS}

            SE DICTAMINA:

            Designar como Jurado Informante a {JURADO}, quienes evaluarán el informe final de la tesis titulada: “{TITULO_TESIS}” presentado por {TRATAMIENTO_ESTUDIANTE}: {NOMBRE_ESTUDIANTE}, del {PROGRAMA}.

            Regístrese, comuníquese y archívese.


            {NOMBRE_DIRECTOR}
            DIRECTOR DE LA UNIDAD DE POSGRADO

            {PIE}
            """;

    /** Dictamen de Expedito: habilita al doctorando a solicitar su Jurado de Sustentación (puente Etapa 7 → 8). */
    public static final String EXPEDITO = """
            UNIVERSIDAD NACIONAL MAYOR DE SAN MARCOS
            Universidad del Perú. Decana de América
            FACULTAD DE {FACULTAD}
            VICEDECANATO DE INVESTIGACIÓN Y POSGRADO – SECCIÓN {NIVEL_SECCION}

            {CIUDAD}, {FECHA_LARGA} del {ANIO}

            DICTAMEN N° {NUMERO_DICTAMEN}

            DICTAMEN: EXPEDITO PARA SUSTENTACIÓN

            Visto el expediente digital N° {EXPEDIENTE}, sobre la aprobación del informe final de tesis por el Jurado Informante {JURADO}.

            CONSIDERANDO:

            {CONSIDERANDOS}

            SE DICTAMINA:

            Declarar EXPEDITO para sustentar la tesis titulada: “{TITULO_TESIS}” a {TRATAMIENTO_ESTUDIANTE}: {NOMBRE_ESTUDIANTE}, del {PROGRAMA}, quedando habilitado(a) para solicitar su Jurado de Sustentación.

            Regístrese, comuníquese y archívese.


            {NOMBRE_DIRECTOR}
            DIRECTOR DE LA UNIDAD DE POSGRADO

            {PIE}
            """;

    /** Dictamen de designación del Jurado de Sustentación (Etapa 8, acto final). */
    public static final String SUSTENTACION = """
            UNIVERSIDAD NACIONAL MAYOR DE SAN MARCOS
            Universidad del Perú. Decana de América
            FACULTAD DE {FACULTAD}
            VICEDECANATO DE INVESTIGACIÓN Y POSGRADO – SECCIÓN {NIVEL_SECCION}

            {CIUDAD}, {FECHA_LARGA} del {ANIO}

            DICTAMEN N° {NUMERO_DICTAMEN}

            DICTAMEN: DESIGNACIÓN DE JURADO DE SUSTENTACIÓN

            Visto el expediente digital N° {EXPEDIENTE}, sobre la solicitud de sustentación de tesis, contando con el Dictamen de Expedito correspondiente.

            CONSIDERANDO:

            {CONSIDERANDOS}

            SE DICTAMINA:

            Designar como Jurado de Sustentación a {JURADO}, para la sustentación de la tesis titulada: “{TITULO_TESIS}” presentada por {TRATAMIENTO_ESTUDIANTE}: {NOMBRE_ESTUDIANTE}, del {PROGRAMA}.

            Regístrese, comuníquese y archívese.


            {NOMBRE_DIRECTOR}
            DIRECTOR DE LA UNIDAD DE POSGRADO

            {PIE}
            """;
}
