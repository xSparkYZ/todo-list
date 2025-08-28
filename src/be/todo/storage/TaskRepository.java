package be.todo.storage;

import be.todo.model.Task;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TaskRepository {
    private final Path file;

    public TaskRepository(Path file) {
        this.file = file;
    }

    public List<Task> findAll() {
        ensureFile();
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            List<Task> tasks = new ArrayList<>(lines.size());
            for (String line : lines) {
                String t = line.trim();
                if (!t.isEmpty()) tasks.add(Task.fromJson(t));
            }
            tasks.sort(Comparator.comparingLong(Task::getId));
            return tasks;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read tasks: " + file, e);
        }
    }

    public void saveAll(List<Task> tasks) {
        ensureFile();
        List<String> lines = new ArrayList<>(tasks.size());
        for (Task t : tasks) lines.add(t.toJson());
        try {
            Files.write(file, lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write tasks: " + file, e);
        }
    }

    public long nextId() {
        return findAll().stream().mapToLong(Task::getId).max().orElse(0) + 1;
    }

    public Task create(String title) {
        long id = nextId();
        Task t = new Task(id, title, false, OffsetDateTime.now(), null);
        List<Task> all = findAll();
        all.add(t);
        saveAll(all);
        return t;
    }

    private void ensureFile() {
        try {
            if (Files.notExists(file)) {
                Path parent = file.getParent();
                if (parent != null && Files.notExists(parent)) Files.createDirectories(parent);
                Files.write(file, new byte[0], StandardOpenOption.CREATE);
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot create data file: " + file, e);
        }
    }
}
