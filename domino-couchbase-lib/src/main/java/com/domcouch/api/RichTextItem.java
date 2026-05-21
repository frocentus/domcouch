package com.domcouch.api;

/**
 * Represents a Rich Text item on a Document. Mirrors lotus.domino.RichTextItem.
 * <p>
 * Rich text content is stored as a JSON array of "segments" — text runs,
 * doclinks, newlines, and section markers. Each segment has a {@code type}
 * field and type-specific properties.
 */
public interface RichTextItem extends Item {

    // ---- segment types ----

    /** Plain text segment: {"type":"text","text":"...","style":{...}} */
    String SEG_TEXT = "text";
    /** Doc link segment: {"type":"doclink","unid":"...","comment":"..."} */
    String SEG_DOCLINK = "doclink";
    /** Newline segment: {"type":"newline","count":N} */
    String SEG_NEWLINE = "newline";
    /** Section start: {"type":"section","title":"...","open":true} */
    String SEG_SECTION = "section";
    /** Section end: {"type":"sectionend"} */
    String SEG_SECTION_END = "sectionend";

    // ---- content manipulation ----

    /**
     * Append plain text to the rich text body.
     * @param text the text to append
     */
    void appendText(String text);

    /**
     * Append a document link.
     * @param doc the document to link to
     * @param comment display text for the link (may be empty)
     */
    void appendDocLink(Document doc, String comment);

    /**
     * Add one or more newlines.
     * @param count number of newlines (1 = single, 2 = paragraph break)
     */
    void addNewLine(int count);

    /**
     * Begin a collapsible section.
     * @param title the section heading
     */
    void beginSection(String title);

    /** End the current section. */
    void endSection();

    /**
     * Begin an insert position. Text appended until {@link #endInsert()}
     * goes to this position.
     */
    void beginInsert();

    /** End the insert position. */
    void endInsert();

    // ---- formatting ----

    /**
     * Apply a named font style to subsequently appended text.
     * @param name font definition name (from the document's font table)
     * @param style style bits (BOLD=1, ITALIC=2, UNDERLINE=4, STRIKEOUT=8)
     * @return a RichTextStyle object that can be modified and applied
     */
    RichTextStyle getNotesFont(String name, int style);

    /**
     * Append text with a specific style.
     * @param style the style to apply
     * @param text the text to append
     */
    void appendStyle(RichTextStyle style, String text);

    // ---- query ----

    /**
     * @return the number of segments in this rich text item
     */
    int getSegmentCount();

    /**
     * @return the plain-text representation (all text segments concatenated)
     */
    String getPlainText();

    /**
     * @return true if the MIME type matches (e.g., "text/html")
     */
    boolean isMimeType(String mimeType);

    /**
     * @return the MIME type, or "text/plain" for non-MIME rich text
     */
    String getMimeType();

    /**
     * Compact the rich text body (remove empty segments).
     */
    void compact();

    /**
     * @return the rich text content as a JSON string
     */
    String getContentJSON();
}
