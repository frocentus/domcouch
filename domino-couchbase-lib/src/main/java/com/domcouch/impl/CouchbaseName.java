package com.domcouch.impl;

import com.domcouch.api.Name;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses and represents a Domino hierarchical name.
 * <p>
 * Format examples:
 * <pre>
 *   Canonical:   CN=John Smith/OU=Dev/O=Acme/C=US
 *   Abbreviated: John Smith/Dev/Acme/US
 *   Full:        G=John/S=Smith/CN=John Smith/OU=Dev/O=Acme/C=US
 * </pre>
 * Components are stored in left-to-right order (most-specific first).
 */
public class CouchbaseName implements Name {

    private final List<Component> components;
    private final String language;
    private final String addr821;
    private final String addr822;

    private CouchbaseName(List<Component> components, String language,
                          String addr821, String addr822) {
        this.components = components;
        this.language = language != null ? language : "";
        this.addr821 = addr821 != null ? addr821 : "";
        this.addr822 = addr822 != null ? addr822 : "";
    }

    /**
     * Parse a name string. Accepts both canonical and abbreviated formats.
     */
    public static CouchbaseName parse(String name) {
        if (name == null || name.isBlank()) {
            return new CouchbaseName(List.of(), "", "", "");
        }
        return new Parser(name).parse();
    }

    // ---- full-name formats ----

    @Override
    public String getCanonical() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < components.size(); i++) {
            if (i > 0) sb.append('/');
            Component c = components.get(i);
            String label = canonicalLabel(c.type);
            if (label != null) {
                sb.append(label).append('=');
            }
            sb.append(c.value);
        }
        if (!language.isEmpty()) {
            sb.append('@').append(language);
        }
        return sb.toString();
    }

    @Override
    public String getAbbreviated() {
        // Abbreviated: values only, separated by /
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < components.size(); i++) {
            if (i > 0) sb.append('/');
            sb.append(components.get(i).value);
        }
        return sb.toString();
    }

    // ---- individual components ----

    @Override
    public String getCommon() {
        return get(ComponentType.CN);
    }

    @Override
    public String getOrganization() {
        return get(ComponentType.O);
    }

    @Override
    public String getOrgUnit1() { return getOrgUnit(1); }
    @Override
    public String getOrgUnit2() { return getOrgUnit(2); }
    @Override
    public String getOrgUnit3() { return getOrgUnit(3); }
    @Override
    public String getOrgUnit4() { return getOrgUnit(4); }

    private String getOrgUnit(int n) {
        int count = 0;
        for (Component c : components) {
            if (c.type == ComponentType.OU) {
                count++;
                if (count == n) return c.value;
            }
        }
        return "";
    }

    @Override
    public String getCountry() {
        return get(ComponentType.C);
    }

    @Override
    public boolean isHierarchical() {
        // A name is hierarchical if it has at least one OU, O, or C component
        for (Component c : components) {
            if (c.type == ComponentType.OU || c.type == ComponentType.O
                    || c.type == ComponentType.C) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getGiven()     { return get(ComponentType.G); }
    @Override
    public String getSurname()   { return get(ComponentType.S); }
    @Override
    public String getInitials()  { return get(ComponentType.I); }
    @Override
    public String getGeneration(){ return get(ComponentType.Q); }
    @Override
    public String getADMD()      { return get(ComponentType.A); }
    @Override
    public String getPRMD()      { return get(ComponentType.P); }

    @Override
    public String getAddr821() {
        return addr821;
    }

    @Override
    public String getAddr822() {
        return addr822;
    }

    @Override
    public String getAddr822LocalPart() {
        if (!addr822.isEmpty()) {
            int lt = addr822.indexOf('<');
            int gt = addr822.indexOf('>', lt);
            if (lt >= 0 && gt > lt) {
                String inside = addr822.substring(lt + 1, gt);
                int at = inside.indexOf('@');
                if (at >= 0) return inside.substring(0, at);
                return inside;
            }
        }
        // Fallback: derive from CN + ADMD/PRMD
        String cn = getCommon();
        String admd = getADMD();
        String prmd = getPRMD();
        if (!cn.isEmpty() && (!admd.isEmpty() || !prmd.isEmpty())) {
            String domain = !admd.isEmpty() ? admd : prmd;
            String local = cn.replace(' ', '.').toLowerCase();
            return local;
        }
        return "";
    }

    @Override
    public String getAddr822Phrase() {
        if (!addr822.isEmpty()) {
            int lt = addr822.indexOf('<');
            if (lt > 0) {
                String phrase = addr822.substring(0, lt).trim();
                if (phrase.startsWith("\"") && phrase.endsWith("\"")) {
                    phrase = phrase.substring(1, phrase.length() - 1);
                }
                return phrase;
            }
        }
        return getCommon();
    }

    @Override
    public String getLanguage() {
        return language;
    }

    // ---- helpers ----

    private String get(ComponentType type) {
        for (Component c : components) {
            if (c.type == type) return c.value;
        }
        return "";
    }

    private static String canonicalLabel(ComponentType type) {
        return switch (type) {
            case CN -> "CN";
            case OU -> "OU";
            case O  -> "O";
            case C  -> "C";
            case G  -> "G";
            case S  -> "S";
            case I  -> "I";
            case Q  -> "Q";
            case A  -> "A";
            case P  -> "P";
            default -> null;
        };
    }

    private static ComponentType labelToType(String label) {
        if (label == null) return null;
        return switch (label.toUpperCase()) {
            case "CN" -> ComponentType.CN;
            case "OU" -> ComponentType.OU;
            case "O"  -> ComponentType.O;
            case "C"  -> ComponentType.C;
            case "G"  -> ComponentType.G;
            case "S"  -> ComponentType.S;
            case "I"  -> ComponentType.I;
            case "Q"  -> ComponentType.Q;
            case "A"  -> ComponentType.A;
            case "P"  -> ComponentType.P;
            default   -> null;
        };
    }

    /** @return true if this label is a known hierarchical name component */
    private static boolean isKnownLabel(String label) {
        return labelToType(label) != null;
    }

    // ---- internal types ----

    enum ComponentType { CN, OU, O, C, G, S, I, Q, A, P, UNKNOWN }

    record Component(ComponentType type, String value) {}

    /**
     * Parses hierarchical name strings.
     */
    static class Parser {
        private final String input;
        private int pos;

        Parser(String input) {
            this.input = input;
            this.pos = 0;
        }

        CouchbaseName parse() {
            List<Component> comps = new ArrayList<>();
            String lang = "";
            String ad821 = "";
            String ad822 = "";

            // Check for internet address formats
            if (input.contains("<") && input.contains("@")) {
                // RFC 822: "phrase <local@domain>" or just <local@domain>
                int lt = input.indexOf('<');
                int gt = input.indexOf('>', lt);
                ad822 = input;
                // Extract RFC 821 part
                if (lt >= 0 && gt > lt) {
                    ad821 = input.substring(lt + 1, gt);
                }
                // Parse phrase as CN=phrase if present
                if (lt > 0) {
                    String phrase = input.substring(0, lt).trim();
                    if (phrase.startsWith("\"") && phrase.endsWith("\"")) {
                        phrase = phrase.substring(1, phrase.length() - 1);
                    }
                    if (!phrase.isEmpty()) {
                        comps.add(new Component(ComponentType.CN, phrase));
                    }
                }
                // Parse local@domain
                if (!ad821.isEmpty()) {
                    int at = ad821.indexOf('@');
                    if (at >= 0) {
                        String local = ad821.substring(0, at);
                        String domain = ad821.substring(at + 1);
                        if (!local.isEmpty()) {
                            // Could add as separate component
                        }
                        // domain might be ADMD/PRMD/C
                        if (!domain.isEmpty()) {
                            String[] parts = domain.split("\\.");
                            if (parts.length >= 3) {
                                comps.add(new Component(ComponentType.A,
                                        parts[0]));
                                comps.add(new Component(ComponentType.P, parts[1]));
                                comps.add(new Component(ComponentType.C, parts[2]));
                            } else if (parts.length == 2) {
                                comps.add(new Component(ComponentType.A, parts[0]));
                                comps.add(new Component(ComponentType.C, parts[1]));
                            }
                        }
                    }
                }
                return new CouchbaseName(comps, lang, ad821, ad822);
            }

            // Check for plain internet address (user@domain)
            if (input.contains("@") && !input.contains("/") && !input.contains("=")) {
                int at = input.indexOf('@');
                String local = input.substring(0, at);
                String domain = input.substring(at + 1);
                ad821 = input;
                ad822 = local + " <" + input + ">";
                if (!local.isEmpty()) {
                    comps.add(new Component(ComponentType.CN, local));
                }
                String[] parts = domain.split("\\.");
                if (parts.length >= 2) {
                    if (parts.length >= 3) {
                        comps.add(new Component(ComponentType.A, parts[0]));
                        if (parts.length > 3) {
                            comps.add(new Component(ComponentType.P, parts[1]));
                        }
                    }
                    comps.add(new Component(ComponentType.C, parts[parts.length - 1]));
                }
                return new CouchbaseName(comps, lang, ad821, ad822);
            }

            // Heuristic: if name contains '=' it's canonical, otherwise abbreviated
            boolean isCanonical = input.contains("=");

            // Check for @language suffix
            String remaining = input;
            int atSign = input.lastIndexOf('@');
            if (atSign > 0 && !input.substring(atSign).contains("/")
                    && !input.substring(atSign).contains("=")) {
                String suffix = input.substring(atSign + 1);
                if (!suffix.contains("=") && !suffix.contains("/")) {
                    lang = suffix;
                    remaining = input.substring(0, atSign);
                }
            }

            String[] parts = remaining.split("/");
            if (isCanonical) {
                for (String part : parts) {
                    if (part.isEmpty()) continue;
                    int eq = part.indexOf('=');
                    if (eq >= 0) {
                        String label = part.substring(0, eq);
                        String value = part.substring(eq + 1);
                        ComponentType type = labelToType(label);
                        comps.add(new Component(type != null ? type : ComponentType.UNKNOWN, value));
                    } else {
                        comps.add(new Component(ComponentType.UNKNOWN, part));
                    }
                }
            } else {
                // Abbreviated: label-less. Assign types by position from the RIGHT.
                // Pattern: CN / [OU1 / OU2 / OU3 / OU4] / O / C
                // The last is C, second-last is O, rest are OU (from right), leftmost is CN.
                int n = parts.length;
                for (int i = 0; i < n; i++) {
                    if (parts[i].isEmpty()) continue;
                    ComponentType type;
                    if (n == 1) {
                        type = ComponentType.CN;  // flat name
                    } else if (i == 0) {
                        type = ComponentType.CN;  // first = common name
                    } else if (i == n - 1) {
                        type = ComponentType.C;   // last = country
                    } else if (i == n - 2 && n > 2) {
                        type = ComponentType.O;   // second-last = org
                    } else {
                        type = ComponentType.OU;  // middle = org unit
                    }
                    comps.add(new Component(type, parts[i]));
                }
            }

            return new CouchbaseName(comps, lang, ad821, ad822);
        }

        /**
         * For abbreviated names, infer component type by position (left to right).
         * CN / OU1 / OU2 / OU3 / OU4 / O / C
         */
        private static ComponentType inferTypeByPosition(int index) {
            return switch (index) {
                case 0 -> ComponentType.CN;
                case 1 -> ComponentType.OU;
                case 2 -> ComponentType.OU;
                case 3 -> ComponentType.OU;
                case 4 -> ComponentType.OU;
                default -> {
                    // The last two components are typically O and C
                    // We don't know the total count yet, so this runs in order
                    yield ComponentType.O; // Will be corrected post-hoc
                }
            };
        }
    }
}
