package com.domcouch.impl;

import com.domcouch.api.RichTextStyle;

/**
 * Immutable RichTextStyle implementation.
 */
public class CouchbaseRichTextStyle implements RichTextStyle {

    private final String fontName;
    private final int fontSize;
    private final int style;
    private final String color;

    public CouchbaseRichTextStyle(String fontName, int fontSize, int style, String color) {
        this.fontName = fontName;
        this.fontSize = fontSize;
        this.style = style;
        this.color = color;
    }

    @Override public String getFontName() { return fontName; }
    @Override public int getFontSize() { return fontSize; }
    @Override public int getStyle() { return style; }
    @Override public String getColor() { return color; }

    @Override
    public RichTextStyle with(String properties) {
        String fn = fontName;
        int fs = fontSize;
        int st = style;
        String cl = color;
        for (String prop : properties.split(";")) {
            String[] kv = prop.trim().split(":", 2);
            String key = kv[0].trim().toLowerCase();
            if (kv.length == 1) {
                switch (key) {
                    case "bold": st |= BOLD; break;
                    case "italic": st |= ITALIC; break;
                    case "underline": st |= UNDERLINE; break;
                    case "strikeout": st |= STRIKEOUT; break;
                    case "plain": st = 0; break;
                }
            } else {
                String val = kv[1].trim();
                switch (key) {
                    case "font-name": fn = val; break;
                    case "size":
                        try { fs = Integer.parseInt(val); } catch (NumberFormatException ignored) {}
                        break;
                    case "color": cl = val; break;
                }
            }
        }
        return new CouchbaseRichTextStyle(fn, fs, st, cl);
    }

    @Override
    public String toString() {
        return "RichTextStyle[" + fontName + " " + fontSize + "pt " + style + " #" + color + "]";
    }
}
