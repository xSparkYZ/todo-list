package be.todo.ui;

import be.todo.model.Task;
import be.todo.service.TodoService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

public final class SwingApp {
    private final TodoService service;

    public SwingApp(TodoService service) {
        this.service = service;
    }

    public void start() {
        SwingUtilities.invokeLater(this::createAndShow);
    }

    private void createAndShow() {
        var frame = new JFrame("Todo");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        var root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        frame.setContentPane(root);

        var titleField = new JTextField();
        titleField.setColumns(24);
        var dueField = new JTextField();
        dueField.setColumns(10);
        dueField.setToolTipText("yyyy-mm-dd (optional)");

        var addBtn = new JButton("Add");
        var refreshBtn = new JButton("Refresh");
        var top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        top.add(new JLabel("Title:"));
        top.add(titleField);
        top.add(new JLabel("Due:"));
        top.add(dueField);
        top.add(addBtn);
        top.add(refreshBtn);
        root.add(top, BorderLayout.NORTH);

        var listModel = new DefaultListModel<Task>();
        var list = new JList<Task>(listModel) {
            @Override public Dimension getPreferredScrollableViewportSize() {
                return new Dimension(580, 300);
            }
        };
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                var comp = super.getListCellRendererComponent(l, value, index, isSelected, cellHasFocus);
                if (value instanceof Task t) {
                    var text = "#" + t.getId() + " " + (t.isDone() ? "[x] " : "[ ] ") + t.getTitle();
                    if (t.getDue() != null) text += "  (due " + t.getDue() + ")";
                    setText(text);
                    if (t.isDone()) setForeground(new Color(0x2e7d32)); // green-ish
                }
                return comp;
            }
        });
        var scroll = new JScrollPane(list);
        root.add(scroll, BorderLayout.CENTER);

        var doneBtn = new JButton("Mark Done");
        var removeBtn = new JButton("Remove");
        var setDueBtn = new JButton("Set Due");
        var showPendingBtn = new JToggleButton("Pending");
        var showDoneBtn = new JToggleButton("Done");
        var showAllBtn = new JToggleButton("All", true);
        ButtonGroup grp = new ButtonGroup();
        grp.add(showAllBtn); grp.add(showPendingBtn); grp.add(showDoneBtn);

        var statsLabel = new JLabel(" ");
        var bottomLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bottomLeft.add(doneBtn);
        bottomLeft.add(removeBtn);
        bottomLeft.add(setDueBtn);
        bottomLeft.add(showAllBtn);
        bottomLeft.add(showPendingBtn);
        bottomLeft.add(showDoneBtn);

        var bottom = new JPanel(new BorderLayout());
        bottom.add(bottomLeft, BorderLayout.WEST);
        bottom.add(statsLabel, BorderLayout.EAST);
        root.add(bottom, BorderLayout.SOUTH);

        Runnable refresh = () -> {
            List<Task> tasks = switch (selectedFilter(showAllBtn, showPendingBtn, showDoneBtn)) {
                case ALL -> service.listAll();
                case PENDING -> service.listPending();
                case DONE -> service.listDone();
            };
            listModel.clear();
            tasks.forEach(listModel::addElement);
            var s = service.stats();
            statsLabel.setText("Total: " + s.total() + " | Done: " + s.done() + " | Pending: " + s.pending());
        };

        addBtn.addActionListener(e -> {
            String title = titleField.getText().trim();
            String dueTxt = dueField.getText().trim();
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Title is required", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            LocalDate due = null;
            if (!dueTxt.isEmpty()) {
                try { due = LocalDate.parse(dueTxt); }
                catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Bad date format (use yyyy-mm-dd)", "Validation", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
            service.add(title, due);
            titleField.setText("");
            dueField.setText("");
            refresh.run();
        });

        refreshBtn.addActionListener(e -> refresh.run());
        showAllBtn.addActionListener(e -> refresh.run());
        showPendingBtn.addActionListener(e -> refresh.run());
        showDoneBtn.addActionListener(e -> refresh.run());

        doneBtn.addActionListener(e -> {
            var t = list.getSelectedValue();
            if (t == null) return;
            service.markDone(t.getId());
            refresh.run();
        });

        removeBtn.addActionListener(e -> {
            var t = list.getSelectedValue();
            if (t == null) return;
            service.remove(t.getId());
            refresh.run();
        });

        setDueBtn.addActionListener(e -> {
            var t = list.getSelectedValue();
            if (t == null) return;
            String due = JOptionPane.showInputDialog(frame, "New due date (yyyy-mm-dd), empty to clear:",
                    t.getDue() != null ? t.getDue().toString() : "");
            if (due == null) return; // cancelled
            LocalDate date = null;
            if (!due.isBlank()) {
                try { date = LocalDate.parse(due.trim()); }
                catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Bad date format", "Validation", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
            service.updateDue(t.getId(), date);
            refresh.run();
        });

        refresh.run();

        frame.pack();
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
    }

    private Filter selectedFilter(AbstractButton all, AbstractButton pending, AbstractButton done) {
        if (pending.isSelected()) return Filter.PENDING;
        if (done.isSelected()) return Filter.DONE;
        return Filter.ALL;
    }

    private enum Filter { ALL, PENDING, DONE }
}
