package be.todo.ui;

import be.todo.model.Task;
import be.todo.service.TodoService;

import java.time.LocalDate;
import java.util.List;

final class CommandProcessor {
    private final TodoService service;

    CommandProcessor(TodoService service) {
        this.service = service;
    }

    boolean process(String line) {
        if (line == null) return false;
        String trimmed = line.trim();
        if (trimmed.isEmpty()) return true;

        String[] args = tokenize(trimmed);
        String cmd = args[0].toLowerCase();

        try {
            switch (cmd) {
                case "help" -> printHelp();
                case "add" -> handleAdd(args);
                case "list" -> handleList(args);
                case "done" -> handleDone(args);
                case "remove" -> handleRemove(args);
                case "due" -> handleDue(args);
                case "clear-done" -> handleClearDone();
                case "stats" -> handleStats();
                case "exit", "quit" -> { System.out.println("Bye!"); return false; }
                default -> { System.out.println("Unknown command: " + cmd); printHelp(); }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
        return true;
    }

    private void handleAdd(String[] args) {
        if (args.length < 2) { System.out.println("Usage: add \"title\" [--due yyyy-mm-dd]"); return; }
        String title = args[1];
        LocalDate due = null;
        if (args.length >= 4 && "--due".equals(args[2])) due = LocalDate.parse(args[3]);
        Task t = service.add(title, due);
        System.out.println("Added: " + t);
    }

    private void handleList(String[] args) {
        boolean all = false, pending = false, done = false;
        for (int j = 1; j < args.length; j++) {
            switch (args[j]) {
                case "--all" -> all = true;
                case "--pending" -> pending = true;
                case "--done" -> done = true;
            }
        }
        List<Task> tasks = all || (!pending && !done)
                ? service.listAll()
                : (done ? service.listDone() : service.listPending());
        if (tasks.isEmpty()) { System.out.println("(no tasks)"); return; }
        tasks.forEach(t -> System.out.println(t.toString()));
    }

    private void handleDone(String[] args) {
        if (args.length < 2) { System.out.println("Usage: done <id>"); return; }
        long id = Long.parseLong(args[1]);
        boolean ok = service.markDone(id);
        System.out.println(ok ? "Marked done: #" + id : "Task not found or already done: #" + id);
    }

    private void handleRemove(String[] args) {
        if (args.length < 2) { System.out.println("Usage: remove <id>"); return; }
        long id = Long.parseLong(args[1]);
        boolean ok = service.remove(id);
        System.out.println(ok ? "Removed: #" + id : "Task not found: #" + id);
    }

    private void handleDue(String[] args) {
        if (args.length < 3) { System.out.println("Usage: due <id> yyyy-mm-dd"); return; }
        long id = Long.parseLong(args[1]);
        LocalDate due = LocalDate.parse(args[2]);
        boolean ok = service.updateDue(id, due);
        System.out.println(ok ? "Updated due date for #" + id + " to " + due : "Task not found: #" + id);
    }

    private void handleClearDone() {
        int removed = service.clearDone();
        System.out.println("Removed " + removed + " done tasks");
    }

    private void handleStats() {
        var s = service.stats();
        System.out.printf("Total: %d, Done: %d, Pending: %d%n", s.total(), s.done(), s.pending());
    }

    static void printHelp() {
        System.out.println("""
                Todo CLI (interactive)
                Commands:
                  help
                  add "<title>" [--due yyyy-mm-dd]
                  list [--all] [--pending] [--done]
                  done <id>
                  remove <id>
                  due <id> yyyy-mm-dd
                  clear-done
                  stats
                  exit | quit
                """);
    }

    static String[] tokenize(String line) {
        var out = new java.util.ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false, escape = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (escape) { cur.append(c); escape = false; continue; }
            if (c == '\\') { escape = true; continue; }
            if (c == '"') { inQuotes = !inQuotes; continue; }
            if (!inQuotes && Character.isWhitespace(c)) {
                if (cur.length() > 0) { out.add(cur.toString()); cur.setLength(0); }
                continue;
            }
            cur.append(c);
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out.toArray(String[]::new);
    }
}
