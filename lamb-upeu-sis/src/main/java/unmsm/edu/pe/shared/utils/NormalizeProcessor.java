// src/main/java/upeu/edu/pe/shared/utils/NormalizeProcessor.java
package unmsm.edu.pe.shared.utils;

import unmsm.edu.pe.shared.annotations.Normalize;
import java.lang.reflect.Field;

public class NormalizeProcessor {

    public static void processNormalizeAnnotations(Object entity) {
        if (entity == null) {
            return;
        }

        Class<?> clazz = entity.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(Normalize.class)) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(entity);

                    if (value instanceof String) {
                        String stringValue = (String) value;
                        Normalize annotation = field.getAnnotation(Normalize.class);
                        String normalizedValue = normalizeString(stringValue, annotation.value());
                        field.set(entity, normalizedValue);
                    }
                } catch (IllegalAccessException e) {
                    System.err.println("Error processing @Normalize annotation on field: " + field.getName());
                    e.printStackTrace();
                }
            }
        }
    }

    private static String normalizeString(String text, Normalize.NormalizeType type) {
        if (text == null) {
            return null;
        }

        String normalized = text.trim().replaceAll("\\s+", " ");

        switch (type) {
            case SPACES_ONLY:
                return normalized;
            case UPPERCASE:
                return normalized.toUpperCase();
            case LOWERCASE:
                return normalized.toLowerCase();
            case TITLE_CASE:
                return toTitleCase(normalized);
            default:
                return normalized;
        }
    }

    private static String toTitleCase(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : text.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }

        return result.toString();
    }
}