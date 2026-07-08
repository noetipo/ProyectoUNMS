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
}
