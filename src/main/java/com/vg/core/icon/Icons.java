package com.vg.core.icon;

public enum Icons {

    SETTINGS('\uE8B8'),
    SEARCH('\uE8B6'),
    CLOSE('\uE5CD'),
    HOME('\uE88A'),
    MENU('\uE5D2'),
    CHECK('\uE5CA'),
    ARROW_LEFT('\uE5CB'),
    ARROW_RIGHT('\uE5CC'),
    PLUS('\uE145'),
    MINUS('\uE15B'),
    STAR('\uE838'),
    HEART('\uE87D'),
    TRASH('\uE872'),
    EDIT('\uE3C9'),
    EYE('\uE8F4'),
    LOCK('\uE897'),
    DOWNLOAD('\uE2C4'),
    UPLOAD('\uE2C6'),
    REFRESH('\uE5D5'),
    COPY('\uE14D'),
    WARNING('\uE002'),
    INFO('\uE88E'),
    FOLDER('\uE2C7'),
    PERSON('\uE7FD'),
    EMAIL('\uE0BE'),
    MIC('\uE029'),
    CLEAR('\uE14C'),
    VISIBILITY('\uE8F4'),
    VISIBILITY_OFF('\uE8F5');

    private final char glyph;

    Icons(char glyph) {
        this.glyph = glyph;
    }

    public char glyph() {
        return glyph;
    }

    public int codepoint() {
        return glyph;
    }

    public String glyphString() {
        return String.valueOf(glyph);
    }
}
