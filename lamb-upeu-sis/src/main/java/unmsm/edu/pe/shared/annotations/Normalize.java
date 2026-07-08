package unmsm.edu.pe.shared.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotación para normalizar campos de texto automáticamente
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Normalize {

    /**
     * Tipo de normalización a aplicar
     */
    NormalizeType value() default NormalizeType.SPACES_ONLY;

    /**
     * Tipos de normalización disponibles
     */
    enum NormalizeType {
        /**
         * Solo elimina espacios duplicados y trim
         */
        SPACES_ONLY,

        /**
         * Convierte a mayúsculas y elimina espacios duplicados
         */
        UPPERCASE,

        /**
         * Convierte a minúsculas y elimina espacios duplicados
         */
        LOWERCASE,

        /**
         * Capitaliza primera letra de cada palabra y elimina espacios duplicados
         */
        TITLE_CASE
    }
}