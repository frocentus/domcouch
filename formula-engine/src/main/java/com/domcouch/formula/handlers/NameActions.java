package com.domcouch.formula.handlers;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal hierarchical name parser for @Name formula function.
 * Lives in formula-engine to avoid dependency on domino-couchbase-lib.
 * <p>
 * Supports canonical ("CN=.../O=.../C=...") and abbreviated (".../.../...") formats.
 */
final class NameActions {

    private NameActions() {}

    static String apply(String action, String nameStr) {
        if (nameStr == null || nameStr.isEmpty()) return "";

        // Split into components: label=value or just value
        String[] parts = nameStr.split("/");
        List<Comp> comps = new ArrayList<>();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            int eq = p.indexOf('=');
            if (eq >= 0) {
                comps.add(new Comp(p.substring(0, eq).toUpperCase(), p.substring(eq + 1)));
            } else {
                comps.add(new Comp("", p));
            }
        }

        String act = action.toUpperCase();
        return switch (act) {
            case "CANONICALIZE" -> toCanonical(comps);
            case "ABBREVIATE"   -> toAbbreviated(comps);
            case "CN"           -> findByLabel(comps, "CN");
            case "O"            -> findByLabel(comps, "O");
            case "OU"           -> findNthOu(comps, 1);
            case "OU1"          -> findNthOu(comps, 1);
            case "OU2"          -> findNthOu(comps, 2);
            case "OU3"          -> findNthOu(comps, 3);
            case "OU4"          -> findNthOu(comps, 4);
            case "C"            -> findByLabel(comps, "C");
            case "G"            -> findByLabel(comps, "G");
            case "S"            -> findByLabel(comps, "S");
            case "I"            -> findByLabel(comps, "I");
            case "Q"            -> findByLabel(comps, "Q");
            case "A"            -> findByLabel(comps, "A");
            case "P"            -> findByLabel(comps, "P");
            case "HIERARCHYONLY" -> hierarchyOnly(comps);
            case "LP", "LOCALPART" -> "";
            case "PHRASE"          -> "";
            default -> "";
        };
    }

    private static String toCanonical(List<Comp> comps) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < comps.size(); i++) {
            if (i > 0) sb.append('/');
            Comp c = comps.get(i);
            if (!c.label.isEmpty()) {
                sb.append(c.label).append('=');
            } else {
                sb.append(inferLabel(i, comps.size())).append('=');
            }
            sb.append(c.value);
        }
        return sb.toString();
    }

    private static String toAbbreviated(List<Comp> comps) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < comps.size(); i++) {
            if (i > 0) sb.append('/');
            sb.append(comps.get(i).value);
        }
        return sb.toString();
    }

    private static String findByLabel(List<Comp> comps, String label) {
        for (Comp c : comps) {
            if (c.label.equals(label)) return c.value;
        }
        return "";
    }

    private static String findNthOu(List<Comp> comps, int n) {
        int count = 0;
        for (Comp c : comps) {
            if (c.label.equals("OU")) {
                count++;
                if (count == n) return c.value;
            }
        }
        return "";
    }

    private static String hierarchyOnly(List<Comp> comps) {
        // Strip CN, return everything after
        boolean foundCn = false;
        StringBuilder sb = new StringBuilder();
        for (Comp c : comps) {
            if (!foundCn && c.label.equals("CN")) {
                foundCn = true;
                continue;
            }
            if (sb.length() > 0) sb.append('/');
            if (!c.label.isEmpty()) sb.append(c.label).append('=');
            sb.append(c.value);
        }
        return sb.toString();
    }

    private static String inferLabel(int index, int total) {
        if (total == 1) return "CN";
        if (index == 0) return "CN";
        if (index == total - 1) return "C";
        if (index == total - 2 && total > 2) return "O";
        return "OU";
    }

    private record Comp(String label, String value) {}
}
