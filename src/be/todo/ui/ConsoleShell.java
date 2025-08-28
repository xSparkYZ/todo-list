package be.todo.ui;

import java.io.BufferedReader;
import java.io.InputStreamReader;

final class ConsoleShell {
    private final CommandProcessor processor;

    ConsoleShell(CommandProcessor processor) {
        this.processor = processor;
    }

    void run() {
        System.out.println("Interactive mode. Type 'help' or 'exit'.");
        var in = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                System.out.print("todo> ");
                String line = in.readLine();
                if (!processor.process(line)) break;
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }
}
