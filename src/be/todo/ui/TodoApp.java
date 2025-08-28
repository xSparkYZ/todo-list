package be.todo.ui;

import be.todo.service.TodoService;
import be.todo.storage.TaskRepository;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class TodoApp {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");

        Path dataFile = Path.of("todo-data.json");
        boolean forceCli = false;

        List<String> rest = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--file" -> {
                    if (i + 1 >= args.length) {
                        System.err.println("Missing value for --file");
                        return;
                    }
                    dataFile = Path.of(args[++i]);
                }
                case "--cli" -> forceCli = true;
                case "--gui" -> forceCli = false; // explicit GUI
                default -> rest.add(args[i]);
            }
        }
        args = rest.toArray(String[]::new);

        var repo = new TaskRepository(dataFile);
        var service = new TodoService(repo);

        if (!forceCli) {
            new SwingApp(service).start();
            return;
        }

        var processor = new CommandProcessor(service);
        if (args.length == 0 || "shell".equalsIgnoreCase(args[0])) {
            System.out.println("(using data file: " + dataFile.toAbsolutePath() + ")");
            new ConsoleShell(processor).run();
        } else {
            processor.process(String.join(" ", args));
        }
    }
}