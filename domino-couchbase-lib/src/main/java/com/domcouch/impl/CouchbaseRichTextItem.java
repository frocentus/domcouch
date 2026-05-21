package com.domcouch.impl;

import com.couchbase.client.java.json.JsonArray;
import com.couchbase.client.java.json.JsonObject;
import com.domcouch.api.*;

import java.util.*;

/**
 * RichTextItem backed by a JSON array of content segments.
 * <p>
 * Content is stored as a JSON array of segment objects:
 * <pre>
 * [
 *   {"type":"text","text":"Hello ","style":{"font":"Helvetica","size":10,"style":0,"color":"000000"}},
 *   {"type":"doclink","unid":"ABC...","comment":"See also"},
 *   {"type":"newline","count":1},
 *   {"type":"section","title":"Details","open":true},
 *   ...
 * ]
 * </pre>
 */
public class CouchbaseRichTextItem extends CouchbaseItem implements RichTextItem {

    private final List<JsonObject> segments;
    private RichTextStyle currentStyle;
    private int insertPos = -1;

    public CouchbaseRichTextItem(String name) {
        super(name, RICHTEXT, List.of());
        this.segments = new ArrayList<>();
        this.currentStyle = RichTextStyle.create();
    }

    /** Load from stored JSON array. */
    public CouchbaseRichTextItem(String name, JsonArray storedSegments) {
        super(name, RICHTEXT, List.of());
        this.segments = new ArrayList<>();
        this.currentStyle = RichTextStyle.create();
        for (int i = 0; i < storedSegments.size(); i++) {
            segments.add(storedSegments.getObject(i));
        }
    }

    // ---- content manipulation ----

    @Override
    public void appendText(String text) {
        if (text == null || text.isEmpty()) return;
        JsonObject seg = JsonObject.create()
                .put("type", SEG_TEXT)
                .put("text", text)
                .put("style", serializeStyle(currentStyle));
        addSegment(seg);
    }

    @Override
    public void appendDocLink(Document doc, String comment) {
        JsonObject seg = JsonObject.create()
                .put("type", SEG_DOCLINK)
                .put("unid", doc.getUniversalID())
                .put("comment", comment != null ? comment : "");
        addSegment(seg);
    }

    @Override
    public void addNewLine(int count) {
        JsonObject seg = JsonObject.create()
                .put("type", SEG_NEWLINE)
                .put("count", Math.max(1, count));
        addSegment(seg);
    }

    @Override
    public void beginSection(String title) {
        JsonObject seg = JsonObject.create()
                .put("type", SEG_SECTION)
                .put("title", title != null ? title : "")
                .put("open", true);
        addSegment(seg);
    }

    @Override
    public void endSection() {
        JsonObject seg = JsonObject.create().put("type", SEG_SECTION_END);
        addSegment(seg);
    }

    @Override
    public void beginInsert() {
        insertPos = segments.size();
    }

    @Override
    public void endInsert() {
        insertPos = -1;
    }

    // ---- formatting ----

    @Override
    public RichTextStyle getNotesFont(String name, int style) {
        RichTextStyle s = RichTextStyle.create().with("font-name:" + name);
        if ((style & RichTextStyle.BOLD) != 0) s = s.with("bold");
        if ((style & RichTextStyle.ITALIC) != 0) s = s.with("italic");
        if ((style & RichTextStyle.UNDERLINE) != 0) s = s.with("underline");
        if ((style & RichTextStyle.STRIKEOUT) != 0) s = s.with("strikeout");
        currentStyle = s;
        return s;
    }

    @Override
    public void appendStyle(RichTextStyle style, String text) {
        RichTextStyle saved = currentStyle;
        currentStyle = style;
        appendText(text);
        currentStyle = saved;
    }

    // ---- query ----

    @Override
    public int getSegmentCount() {
        return segments.size();
    }

    @Override
    public String getPlainText() {
        StringBuilder sb = new StringBuilder();
        for (JsonObject seg : segments) {
            switch (seg.getString("type")) {
                case SEG_TEXT:
                    sb.append(seg.getString("text"));
                    break;
                case SEG_NEWLINE:
                    sb.append("\n".repeat(Math.max(0, seg.getInt("count"))));
                    break;
                case SEG_DOCLINK:
                    sb.append("[→ ").append(seg.getString("comment")).append("]");
                    break;
                case SEG_SECTION:
                    sb.append("\n[").append(seg.getString("title")).append("]\n");
                    break;
            }
        }
        return sb.toString();
    }

    @Override
    public boolean isMimeType(String mimeType) {
        return mimeType != null && mimeType.equals(getMimeType());
    }

    @Override
    public String getMimeType() {
        return "text/plain";
    }

    @Override
    public void compact() {
        segments.removeIf(seg ->
            SEG_TEXT.equals(seg.getString("type"))
            && (seg.getString("text") == null || seg.getString("text").isEmpty()));
    }

    @Override
    public String getContentJSON() {
        return buildSegmentArray().toString();
    }

    // ---- Item overrides ----

    @Override
    public int getType() {
        return RICHTEXT;
    }

    @Override
    public Vector<Object> getValues() {
        return new Vector<>(List.of(getPlainText()));
    }

    @Override
    public String getValueString() {
        return getPlainText();
    }

    // ---- internal ----

    private void addSegment(JsonObject seg) {
        if (insertPos >= 0 && insertPos < segments.size()) {
            segments.add(insertPos, seg);
            insertPos++;
        } else {
            segments.add(seg);
        }
    }

    /** Build the full JSON array for storage. */
    JsonArray buildSegmentArray() {
        JsonArray arr = JsonArray.create();
        for (JsonObject seg : segments) {
            arr.add(seg);
        }
        return arr;
    }

    private static JsonObject serializeStyle(RichTextStyle s) {
        return JsonObject.create()
                .put("font", s.getFontName())
                .put("size", s.getFontSize())
                .put("style", s.getStyle())
                .put("color", s.getColor());
    }

    /** Deserialize style from stored JSON, falling back to default. */
    static RichTextStyle deserializeStyle(JsonObject styleObj) {
        if (styleObj == null) return RichTextStyle.create();
        return RichTextStyle.create().with(
                "font-name:" + styleObj.getString("font") + ";" +
                "size:" + styleObj.getInt("size") + ";" +
                "color:" + styleObj.getString("color") +
                (styleObj.getInt("style") != 0 ? ";" + styleBitsToString(styleObj.getInt("style")) : ""));
    }

    private static String styleBitsToString(int bits) {
        StringBuilder sb = new StringBuilder();
        if ((bits & RichTextStyle.BOLD) != 0) sb.append("bold;");
        if ((bits & RichTextStyle.ITALIC) != 0) sb.append("italic;");
        if ((bits & RichTextStyle.UNDERLINE) != 0) sb.append("underline;");
        if ((bits & RichTextStyle.STRIKEOUT) != 0) sb.append("strikeout;");
        return sb.toString();
    }
}
