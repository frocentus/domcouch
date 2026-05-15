package com.domcouch.api;

/**
 * Represents a view navigator — a positional cursor over a view's entries,
 * including category rows and totals. Mirrors lotus.domino.ViewNavigator.
 */
public interface ViewNavigator {

    // ---- count / properties ----

    /**
     * @return total number of entries represented by this navigator
     */
    int getCount();

    /**
     * @return the parent view
     */
    View getParentView();

    /**
     * @return the maximum category level in this view (0 for flat views)
     */
    int getMaxLevel();

    // ---- get methods (return ViewEntry, creating an object) ----

    ViewEntry getFirst() throws NotesException;

    ViewEntry getLast() throws NotesException;

    ViewEntry getNext() throws NotesException;

    ViewEntry getNext(ViewEntry entry) throws NotesException;

    ViewEntry getPrev() throws NotesException;

    ViewEntry getPrev(ViewEntry entry) throws NotesException;

    ViewEntry getCurrent() throws NotesException;

    ViewEntry getNth(int n) throws NotesException;

    ViewEntry getPos(String pos) throws NotesException;

    ViewEntry getFirstDocument() throws NotesException;

    ViewEntry getLastDocument() throws NotesException;

    ViewEntry getNextDocument() throws NotesException;

    ViewEntry getPrevDocument() throws NotesException;

    ViewEntry getNextCategory() throws NotesException;

    ViewEntry getPrevCategory() throws NotesException;

    ViewEntry getChild() throws NotesException;

    ViewEntry getChild(ViewEntry entry) throws NotesException;

    ViewEntry getParent() throws NotesException;

    ViewEntry getParent(ViewEntry entry) throws NotesException;

    ViewEntry getNextSibling() throws NotesException;

    ViewEntry getNextSibling(ViewEntry entry) throws NotesException;

    ViewEntry getPrevSibling() throws NotesException;

    ViewEntry getPrevSibling(ViewEntry entry) throws NotesException;

    // ---- goto methods (move cursor, no ViewEntry created) ----

    void gotoFirst() throws NotesException;

    void gotoLast() throws NotesException;

    void gotoNext() throws NotesException;

    void gotoNext(ViewEntry entry) throws NotesException;

    void gotoPrev() throws NotesException;

    void gotoPrev(ViewEntry entry) throws NotesException;

    void gotoEntry(Object entry) throws NotesException;

    void gotoEntry(ViewEntry entry) throws NotesException;

    void gotoPos(String pos) throws NotesException;

    void gotoNth(int n) throws NotesException;

    void gotoFirstDocument() throws NotesException;

    void gotoLastDocument() throws NotesException;

    void gotoNextDocument() throws NotesException;

    void gotoPrevDocument() throws NotesException;

    void gotoNextCategory() throws NotesException;

    void gotoPrevCategory() throws NotesException;

    void gotoChild() throws NotesException;

    void gotoChild(ViewEntry entry) throws NotesException;

    void gotoParent() throws NotesException;

    void gotoParent(ViewEntry entry) throws NotesException;

    void gotoNextSibling() throws NotesException;

    void gotoNextSibling(ViewEntry entry) throws NotesException;

    void gotoPrevSibling() throws NotesException;

    void gotoPrevSibling(ViewEntry entry) throws NotesException;

    // ---- mark methods (no-ops in Couchbase) ----

    void markAllRead();

    void markAllRead(String userName);

    void markAllUnread();

    void markAllUnread(String userName);

    void recycle();
}
