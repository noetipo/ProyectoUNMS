// src/main/java/upeu/edu/pe/shared/utils/TextNormalizer.java
package unmsm.edu.pe.shared.utils;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TextNormalizer {

    public String normalize(String text) {
        if (text == null) {
            return null;
        }

        return text
                .trim()
                .replaceAll("\\s+", " ")
                .toUpperCase();
    }

    public String normalizeSpaces(String text) {
        if (text == null) {
            return null;
        }

        return text
                .trim()
                .replaceAll("\\s+", " ");
    }

    public String toUpperCase(String text) {
        if (text == null) {
            return null;
        }

        return text.trim().toUpperCase();
    }
}