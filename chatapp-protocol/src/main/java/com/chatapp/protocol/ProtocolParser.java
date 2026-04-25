package com.chatapp.protocol;

import java.util.ArrayList;
import java.util.List;

/**
 * ProtocolParser.java
 * Safe, robust parser for the chat protocol.
 * Handles escaping of the separator character and enforces bounds.
 */
public class ProtocolParser {

    private static final char SEP_CHAR = '|';
    private static final char ESCAPE_CHAR = '\\';

    private ProtocolParser() {}

    /**
     * Safely splits a protocol line into parts, respecting escape sequences.
     * @param line the raw protocol line
     * @return array of parts, or empty array if line is null/empty/too long
     */
    public static String[] parse(String line) {
        if (line == null || line.isEmpty()) {
            return new String[0];
        }
        if (line.length() > Protocol.MAX_MESSAGE_LENGTH) {
            System.err.println("[PROTOCOL] Message exceeds max length (" + Protocol.MAX_MESSAGE_LENGTH + "), rejected.");
            return new String[0];
        }

        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaping = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (escaping) {
                current.append(c);
                escaping = false;
            } else if (c == ESCAPE_CHAR) {
                escaping = true;
            } else if (c == SEP_CHAR) {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        parts.add(current.toString());

        return parts.toArray(new String[0]);
    }

    /**
     * Escapes separator characters in a value so it won't break protocol parsing.
     */
    public static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace(String.valueOf(SEP_CHAR), "\\" + SEP_CHAR);
    }

    /**
     * Safely gets a part from the parsed array, returning empty string if out of bounds.
     */
    public static String getPart(String[] parts, int index) {
        if (parts == null || index < 0 || index >= parts.length) {
            return "";
        }
        return parts[index];
    }

    /**
     * Checks if the parsed message has at least the expected number of parts.
     */
    public static boolean hasMinParts(String[] parts, int min) {
        return parts != null && parts.length >= min;
    }

    /**
     * Builds a protocol message from parts, escaping each part.
     */
    public static String build(String... parts) {
        if (parts == null || parts.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(SEP_CHAR);
            sb.append(escape(parts[i]));
        }
        return sb.toString();
    }
}

