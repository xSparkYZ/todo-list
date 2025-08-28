package be.todo.service;

import be.todo.model.Task;
import be.todo.storage.TaskRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TodoService {
    private final TaskRepository repo;

    public TodoService(TaskRepository repo) {
        this.repo = repo;
    }

    public Task add(String title, LocalDate due) {
        Task t = repo.create(title);
        if (due != null) {
            updateDue(t.getId(), due);
            return getById(t.getId()).orElse(t);
        }
        return t;
    }

    public List<Task> listAll() { return repo.findAll(); }

    public List<Task> listPending() {
        List<Task> out = new ArrayList<>();
        for (Task t : repo.findAll()) if (!t.isDone()) out.add(t);
        return out;
    }

    public List<Task> listDone() {
        List<Task> out = new ArrayList<>();
        for (Task t : repo.findAll()) if (t.isDone()) out.add(t);
        return out;
    }

    public Optional<Task> getById(long id) {
        return repo.findAll().stream().filter(t -> t.getId() == id).findFirst();
    }

    public boolean markDone(long id) {
        List<Task> all = repo.findAll();
        boolean changed = false;
        List<Task> updated = new ArrayList<>(all.size());
        for (Task t : all) {
            if (t.getId() == id && !t.isDone()) {
                updated.add(t.withDone(true));
                changed = true;
            } else {
                updated.add(t);
            }
        }
        if (changed) repo.saveAll(updated);
        return changed;
    }

    public boolean remove(long id) {
        List<Task> all = repo.findAll();
        int before = all.size();
        all.removeIf(t -> t.getId() == id);
        if (all.size() < before) {
            repo.saveAll(all);
            return true;
        }
        return false;
    }

    public boolean updateDue(long id, LocalDate due) {
        List<Task> all = repo.findAll();
        boolean changed = false;
        List<Task> updated = new ArrayList<>(all.size());
        for (Task t : all) {
            if (t.getId() == id) {
                updated.add(t.withDue(due));
                changed = true;
            } else {
                updated.add(t);
            }
        }
        if (changed) repo.saveAll(updated);
        return changed;
    }

    public int clearDone() {
        List<Task> all = repo.findAll();
        int before = all.size();
        all.removeIf(Task::isDone);
        int removed = before - all.size();
        if (removed > 0) repo.saveAll(all);
        return removed;
    }

    public Stats stats() {
        int total = repo.findAll().size();
        int done = listDone().size();
        return new Stats(total, done, total - done);
    }

    public record Stats(int total, int done, int pending) { }
}
