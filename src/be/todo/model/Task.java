package be.todo.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Objects;

public final class Task {
    private final long id;
    private final String title;
    private final boolean done;
    private final OffsetDateTime createdAt;
    private final LocalDate due; // nullable

    public Task(long id, String title, boolean done, OffsetDateTime createdAt, LocalDate due) {
        this.id = id;
        this.title = Objects.requireNonNull(title, "title");
        this.done = done;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.due = due;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public boolean isDone() { return done; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public LocalDate getDue() { return due; }

    public Task withDone(boolean newDone) { return new Task(id, title, newDone, createdAt, due); }
    public Task withDue(LocalDate newDue) { return new Task(id, title, done, createdAt, newDue); }


    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder(128);
        sb.append('{');
        sb.append("\"id\":").append(id).append(',');
        sb.append("\"title\":\"").append(escape(title)).append("\",");
        sb.append("\"done\":").append(done).append(',');
        sb.append("\"createdAt\":\"").append(createdAt).append("\",");
        if (due != null) {
            sb.append("\"due\":\"").append(due).append('"');
        } else {
            sb.append("\"due\":null");
        }
        sb.append('}');
        return sb.toString();
    }

    public static Task fromJson(String json) {
        String s = json.trim();
        if (!s.startsWith("{") || !s.endsWith("}")) {
            throw new IllegalArgumentException("Invalid JSON line: " + s);
        }
        long id = getLong(s, "\"id\":", ',');
        String title = getString(s, "\"title\":\"", '"');
        boolean done = getBoolean(s, "\"done\":", ',');
        String created = getString(s, "\"createdAt\":\"", '"');
        String dueStr = getNullableString(s, "\"due\":");
        LocalDate due = null;
        if (dueStr != null && !"null".equals(dueStr)) {
            if (dueStr.startsWith("\"") && dueStr.endsWith("\"")) {
                dueStr = dueStr.substring(1, dueStr.length() - 1);
            }
            try { due = LocalDate.parse(dueStr); } catch (DateTimeParseException ignored) {}
        }
        OffsetDateTime createdAt = OffsetDateTime.parse(created);
        return new Task(id, title, done, createdAt, due);
    }

    private static long getLong(String s, String key, char terminator) {
        int i = s.indexOf(key);
        if (i < 0) throw new IllegalArgumentException("Missing key: " + key);
        i += key.length();
        int j = s.indexOf(terminator, i);
        if (j < 0) j = s.indexOf('}', i);
        String num = s.substring(i, j).trim();
        return Long.parseLong(num);
    }

    private static boolean getBoolean(String s, String key, char terminator) {
        int i = s.indexOf(key);
        if (i < 0) throw new IllegalArgumentException("Missing key: " + key);
        i += key.length();
        int j = s.indexOf(terminator, i);
        if (j < 0) j = s.indexOf('}', i);
        String val = s.substring(i, j).trim();
        return Boolean.parseBoolean(val);
    }

    private static String getString(String s, String key, char endQuote) {
        int i = s.indexOf(key);
        if (i < 0) throw new IllegalArgumentException("Missing key: " + key);
        i += key.length();
        int j = s.indexOf(endQuote, i);
        if (j < 0) throw new IllegalArgumentException("Bad string for key: " + key);
        return unescape(s.substring(i, j));
    }

    private static String getNullableString(String s, String key) {
        int i = s.indexOf(key);
        if (i < 0) return null;
        i += key.length();
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        if (i >= s.length()) return null;
        if (s.charAt(i) == 'n') { // null
            return "null";
        } else if (s.charAt(i) == '"') {
            int j = s.indexOf('"', i + 1);
            if (j < 0) return null;
            return s.substring(i, j + 1);
        }
        return null;
    }

    private static String unescape(String s) {
        return s.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    @Override
    public String toString() {
        return "#" + id + " [" + (done ? "x" : " ") + "] " + title +
                (due != null ? " (due " + due + ")" : "") +
                " — created " + createdAt;
    }
}
