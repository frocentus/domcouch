package com.domcouch.api;

/**
 * Font style for rich text segments. Mirrors lotus.domino.RichTextStyle.
 * <p>
 * Immutable — create via {@link #create()} builder or modify via {@link #with(String)}.
 */
public interface RichTextStyle {

    int BOLD      = 1;
    int ITALIC    = 2;
    int UNDERLINE = 4;
    int STRIKEOUT = 8;

    /** @return the font name (e.g., "Courier", "Helvetica") */
    String getFontName();

    /** @return the font size in points */
    int getFontSize();

    /** @return style bits (BOLD | ITALIC | UNDERLINE | STRIKEOUT) */
    int getStyle();

    /** @return the text color as 6-char hex (e.g., "FF0000" for red) */
    String getColor();

    /**
     * Create a new style with one or more CSS-like property changes.
     * <pre>
     * style.with("font-name:Courier; size:12; color:FF0000; bold; italic")
     * </pre>
     */
    RichTextStyle with(String properties);

    /**
     * @return true if bold is set
     */
    default boolean isBold() { return (getStyle() & BOLD) != 0; }

    /**
     * @return true if italic is set
     */
    default boolean isItalic() { return (getStyle() & ITALIC) != 0; }

    /** Create a default style (10pt Helvetica, no effects). */
    static RichTextStyle create() {
        return new com.domcouch.impl.CouchbaseRichTextStyle("Helvetica", 10, 0, "000000");
    }
}
