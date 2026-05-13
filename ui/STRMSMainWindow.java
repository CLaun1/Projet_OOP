package ui;

import src.*;
import src.enumeration.*;
import src.exceptions.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Main graphical interface for the STRMS.
 * Swing-based dashboard with panels for task management,
 * user management, dependency management, and history.
 */
public class STRMSMainWindow extends JFrame {

    // ── Colors & Fonts (theme) ────────────────────────────────────────────────
    private static final Color BG_DARK      = new Color(18, 24, 38);
    private static final Color BG_PANEL     = new Color(28, 36, 54);
    private static final Color BG_CARD      = new Color(38, 48, 70);
    private static final Color ACCENT_BLUE  = new Color(64, 156, 255);
    private static final Color ACCENT_GREEN = new Color(52, 211, 153);
    private static final Color ACCENT_RED   = new Color(248, 113, 113);
    private static final Color ACCENT_AMBER = new Color(251, 191, 36);
    private static final Color TEXT_PRIMARY = new Color(230, 237, 255);
    private static final Color TEXT_MUTED   = new Color(140, 155, 190);

    private static final Font FONT_TITLE  = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font FONT_LABEL  = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_BODY   = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_MONO   = new Font("Consolas", Font.PLAIN, 12);

    // ── Core ──────────────────────────────────────────────────────────────────
    private final TaskManager manager = new TaskManager();

    // ── UI Components ─────────────────────────────────────────────────────────
    private JTable     taskTable;
    private DefaultTableModel taskTableModel;
    private JTextArea  logArea;
    private JLabel     lblTodo, lblBlocked, lblInProgress, lblDone;
    private JComboBox<String> cbUsers;

    // ── Pre-loaded demo data ──────────────────────────────────────────────────
    private Admin    alice;
    private Manager  bob;
    private Engineer charlie, diana;

    public STRMSMainWindow() {
        super("STRMS — Smart Task & Resource Management System");
        initDemoData();
        buildUI();
        refreshTable();
        refreshDashboard();
        setVisible(true);
    }

    // ── Demo data ─────────────────────────────────────────────────────────────

    private void initDemoData() {
        alice   = new Admin("A01",   "Alice",   "alice@strms.com",   1);
        bob     = new Manager("M01", "Bob",     "bob@strms.com",     "DevTeam");
        charlie = new Engineer("E01","Charlie", "charlie@strms.com", "Backend");
        diana   = new Engineer("E02","Diana",   "diana@strms.com",   "Frontend");

        manager.addUser(alice);
        manager.addUser(bob);
        manager.addUser(charlie);
        manager.addUser(diana);

        try {
            Task t1 = new Task("T001","Design DB Schema","Define all tables and relations",
                    PriorityLevel.CRITICAL, TaskStatus.TODO, TaskCategory.DOCUMENTATION,
                    new Date(System.currentTimeMillis() + 86400000L));
            Task t2 = new Task("T002","Implement Auth","JWT + Spring Security",
                    PriorityLevel.HIGH, TaskStatus.TODO, TaskCategory.FEATURE,
                    new Date(System.currentTimeMillis() + 172800000L));
            Task t3 = new Task("T003","Fix login bug","Null pointer on empty password",
                    PriorityLevel.HIGH, TaskStatus.TODO, TaskCategory.BUGFIX,
                    new Date(System.currentTimeMillis() + 259200000L));
            Task t4 = new Task("T004","Write API docs","OpenAPI specification",
                    PriorityLevel.MEDIUM, TaskStatus.TODO, TaskCategory.DOCUMENTATION,
                    new Date(System.currentTimeMillis() + 345600000L));

            manager.addTask(t1, alice);
            manager.addTask(t2, alice);
            manager.addTask(t3, alice);
            manager.addTask(t4, alice);

            // T2 depends on T1
            manager.addDependency("T002","T001", alice);

        } catch (Exception e) {
            System.err.println("Demo data error: " + e.getMessage());
        }
    }

    // ── UI Construction ───────────────────────────────────────────────────────

    private void buildUI() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 820);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout(0, 0));

        add(buildHeader(),     BorderLayout.NORTH);
        add(buildLeftPanel(),  BorderLayout.WEST);
        add(buildCenter(),     BorderLayout.CENTER);
        add(buildLogPanel(),   BorderLayout.SOUTH);
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_PANEL);
        header.setBorder(new EmptyBorder(14, 24, 14, 24));

        JLabel title = new JLabel("⚙  STRMS Dashboard");
        title.setFont(FONT_TITLE);
        title.setForeground(ACCENT_BLUE);

        JLabel subtitle = new JLabel("Smart Task & Resource Management System");
        subtitle.setFont(FONT_BODY);
        subtitle.setForeground(TEXT_MUTED);

        JPanel left = new JPanel(new GridLayout(2, 1));
        left.setOpaque(false);
        left.add(title);
        left.add(subtitle);

        // Acting user selector
        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        userPanel.setOpaque(false);
        JLabel lUser = new JLabel("Acting as:");
        lUser.setFont(FONT_LABEL);
        lUser.setForeground(TEXT_MUTED);

        cbUsers = new JComboBox<>(new String[]{
            "Alice (Admin)", "Bob (Manager)", "Charlie (Engineer)", "Diana (Engineer)"
        });
        cbUsers.setFont(FONT_BODY);
        styleCombo(cbUsers);

        userPanel.add(lUser);
        userPanel.add(cbUsers);

        header.add(left,      BorderLayout.WEST);
        header.add(userPanel, BorderLayout.EAST);
        return header;
    }

    // ── Left panel: stats + actions ───────────────────────────────────────────

    private JPanel buildLeftPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_PANEL);
        panel.setBorder(new EmptyBorder(20, 16, 20, 16));
        panel.setPreferredSize(new Dimension(240, 0));

        panel.add(sectionLabel("📊 Statistics"));
        panel.add(Box.createVerticalStrut(10));

        lblTodo       = statCard("TODO",        "0", new Color(100, 140, 255));
        lblBlocked    = statCard("BLOCKED",     "0", ACCENT_RED);
        lblInProgress = statCard("IN PROGRESS", "0", ACCENT_AMBER);
        lblDone       = statCard("DONE",        "0", ACCENT_GREEN);

        panel.add(statRow(lblTodo));
        panel.add(Box.createVerticalStrut(8));
        panel.add(statRow(lblBlocked));
        panel.add(Box.createVerticalStrut(8));
        panel.add(statRow(lblInProgress));
        panel.add(Box.createVerticalStrut(8));
        panel.add(statRow(lblDone));
        panel.add(Box.createVerticalStrut(24));

        panel.add(sectionLabel("⚡ Quick Actions"));
        panel.add(Box.createVerticalStrut(10));

        panel.add(actionButton("＋  Add Task",        this::showAddTaskDialog));
        panel.add(Box.createVerticalStrut(8));
        panel.add(actionButton("👤  Assign Task",      this::showAssignTaskDialog));
        panel.add(Box.createVerticalStrut(8));
        panel.add(actionButton("✔  Complete Task",    this::showCompleteTaskDialog));
        panel.add(Box.createVerticalStrut(8));
        panel.add(actionButton("🔗  Add Dependency",   this::showAddDependencyDialog));
        panel.add(Box.createVerticalStrut(8));
        panel.add(actionButton("🗑  Delete Task",      this::showDeleteTaskDialog));
        panel.add(Box.createVerticalStrut(8));
        panel.add(actionButton("📁  Save to File",     this::saveToFile));
        panel.add(Box.createVerticalStrut(8));
        panel.add(actionButton("📂  Load from File",   this::loadFromFile));

        return panel;
    }

    // ── Center: task table ────────────────────────────────────────────────────

    private JPanel buildCenter() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(16, 16, 8, 16));

        JLabel lbl = new JLabel("  Task List");
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(TEXT_PRIMARY);

        String[] cols = {"ID", "Title", "Status", "Priority", "Category",
                         "Assigned To", "Dependencies", "Deadline"};
        taskTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        taskTable = new JTable(taskTableModel);
        styleTable(taskTable);

        JScrollPane scroll = new JScrollPane(taskTable);
        scroll.setBackground(BG_CARD);
        scroll.getViewport().setBackground(BG_CARD);
        scroll.setBorder(BorderFactory.createLineBorder(BG_PANEL, 1));

        // Double-click → show history
        taskTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) showHistoryDialog();
            }
        });

        panel.add(lbl,    BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        // Refresh button
        JButton refresh = new JButton("⟳ Refresh");
        styleButton(refresh, ACCENT_BLUE);
        refresh.addActionListener(e -> { refreshTable(); refreshDashboard(); });
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnRow.setOpaque(false);
        btnRow.add(refresh);
        panel.add(btnRow, BorderLayout.SOUTH);

        return panel;
    }

    // ── Log panel ─────────────────────────────────────────────────────────────

    private JPanel buildLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_PANEL);
        panel.setBorder(new EmptyBorder(8, 16, 12, 16));
        panel.setPreferredSize(new Dimension(0, 160));

        JLabel lbl = new JLabel("  System Log");
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(TEXT_MUTED);
        panel.add(lbl, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(FONT_MONO);
        logArea.setBackground(BG_DARK);
        logArea.setForeground(ACCENT_GREEN);
        logArea.setLineWrap(true);

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createLineBorder(BG_CARD, 1));
        panel.add(scroll, BorderLayout.CENTER);

        log("STRMS started. " + manager.getTasks().size() + " tasks loaded.");
        return panel;
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    private void showAddTaskDialog() {
        JTextField fId    = new JTextField(10);
        JTextField fTitle = new JTextField(20);
        JTextField fDesc  = new JTextField(20);

        JComboBox<PriorityLevel>  cbPriority = new JComboBox<>(PriorityLevel.values());
        JComboBox<TaskCategory>   cbCategory = new JComboBox<>(TaskCategory.values());

        Object[] fields = {
            "Task ID:",         fId,
            "Title:",           fTitle,
            "Description:",     fDesc,
            "Priority:",        cbPriority,
            "Category:",        cbCategory,
        };

        int res = JOptionPane.showConfirmDialog(this, fields,
                "Add New Task", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (res == JOptionPane.OK_OPTION) {
            try {
                Task task = new Task(
                    fId.getText().trim(),
                    fTitle.getText().trim(),
                    fDesc.getText().trim(),
                    (PriorityLevel) cbPriority.getSelectedItem(),
                    TaskStatus.TODO,
                    (TaskCategory)  cbCategory.getSelectedItem(),
                    null
                );
                manager.addTask(task, currentUser());
                log("✅ Task '" + task.getTitle() + "' added.");
                refreshTable();
                refreshDashboard();
            } catch (Exception e) {
                error(e.getMessage());
            }
        }
    }

    private void showAssignTaskDialog() {
        String[] taskIds = manager.getTasks().keySet().toArray(new String[0]);
        String[] engIds  = manager.getUsers().values().stream()
                .filter(u -> u instanceof Engineer)
                .map(u -> u.getId() + " - " + u.getName())
                .toArray(String[]::new);

        if (taskIds.length == 0 || engIds.length == 0) {
            error("No tasks or engineers available."); return;
        }

        JComboBox<String> cbTask = new JComboBox<>(taskIds);
        JComboBox<String> cbEng  = new JComboBox<>(engIds);
        styleCombo(cbTask); styleCombo(cbEng);

        Object[] fields = {"Task ID:", cbTask, "Engineer:", cbEng};
        int res = JOptionPane.showConfirmDialog(this, fields,
                "Assign Task", JOptionPane.OK_CANCEL_OPTION);

        if (res == JOptionPane.OK_OPTION) {
            try {
                String taskId = (String) cbTask.getSelectedItem();
                String engId  = ((String) cbEng.getSelectedItem()).split(" - ")[0];
                Engineer eng  = (Engineer) manager.getUsers().get(engId);
                manager.assignTask(taskId, eng, currentUser());
                log("✅ Task " + taskId + " assigned to " + eng.getName());
                refreshTable();
                refreshDashboard();
            } catch (Exception e) {
                error(e.getMessage());
            }
        }
    }

    private void showCompleteTaskDialog() {
        String[] taskIds = manager.getInProgressTasks().stream()
                .map(Task::getId).toArray(String[]::new);

        if (taskIds.length == 0) { error("No in-progress tasks."); return; }

        JComboBox<String> cb = new JComboBox<>(taskIds);
        styleCombo(cb);

        int res = JOptionPane.showConfirmDialog(this,
                new Object[]{"Select task to complete:", cb},
                "Complete Task", JOptionPane.OK_CANCEL_OPTION);

        if (res == JOptionPane.OK_OPTION) {
            try {
                String taskId = (String) cb.getSelectedItem();
                manager.completeTask(taskId, currentUser());
                log("✅ Task " + taskId + " marked as DONE.");
                refreshTable();
                refreshDashboard();
            } catch (Exception e) {
                error(e.getMessage());
            }
        }
    }

    private void showAddDependencyDialog() {
        String[] taskIds = manager.getTasks().keySet().toArray(new String[0]);
        if (taskIds.length < 2) { error("Need at least 2 tasks."); return; }

        JComboBox<String> cbTask   = new JComboBox<>(taskIds);
        JComboBox<String> cbDepOn  = new JComboBox<>(taskIds);
        styleCombo(cbTask); styleCombo(cbDepOn);

        int res = JOptionPane.showConfirmDialog(this,
                new Object[]{"Task:", cbTask, "Depends on:", cbDepOn},
                "Add Dependency", JOptionPane.OK_CANCEL_OPTION);

        if (res == JOptionPane.OK_OPTION) {
            try {
                String taskId  = (String) cbTask.getSelectedItem();
                String depId   = (String) cbDepOn.getSelectedItem();
                manager.addDependency(taskId, depId, currentUser());
                log("✅ Dependency added: " + taskId + " → " + depId);
                refreshTable();
            } catch (Exception e) {
                error(e.getMessage());
            }
        }
    }

    private void showDeleteTaskDialog() {
        String[] taskIds = manager.getTasks().keySet().toArray(new String[0]);
        if (taskIds.length == 0) { error("No tasks to delete."); return; }

        JComboBox<String> cb = new JComboBox<>(taskIds);
        styleCombo(cb);

        int res = JOptionPane.showConfirmDialog(this,
                new Object[]{"Select task to delete:", cb},
                "Delete Task", JOptionPane.OK_CANCEL_OPTION);

        if (res == JOptionPane.OK_OPTION) {
            try {
                String taskId = (String) cb.getSelectedItem();
                manager.deleteTask(taskId, currentUser());
                log("🗑 Task " + taskId + " deleted.");
                refreshTable();
                refreshDashboard();
            } catch (Exception e) {
                error(e.getMessage());
            }
        }
    }

    private void showHistoryDialog() {
        int row = taskTable.getSelectedRow();
        if (row < 0) { error("Select a task first."); return; }
        String taskId = (String) taskTableModel.getValueAt(row, 0);

        try {
            Task task = manager.findTask(taskId);
            List<TaskHistoryEntry> history = task.getHistory();

            JTextArea area = new JTextArea(history.isEmpty() ? "(no history)" :
                String.join("\n", history.stream()
                    .map(TaskHistoryEntry::toString)
                    .toArray(String[]::new)));
            area.setFont(FONT_MONO);
            area.setEditable(false);
            area.setBackground(BG_DARK);
            area.setForeground(ACCENT_GREEN);

            JScrollPane scroll = new JScrollPane(area);
            scroll.setPreferredSize(new Dimension(640, 300));

            JOptionPane.showMessageDialog(this, scroll,
                "History — " + task.getTitle(), JOptionPane.PLAIN_MESSAGE);
        } catch (Exception e) {
            error(e.getMessage());
        }
    }

    private void saveToFile() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("tasks.csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                manager.saveTasksToFile(fc.getSelectedFile().getAbsolutePath());
                log("📁 Saved to " + fc.getSelectedFile().getName());
            } catch (Exception e) { error(e.getMessage()); }
        }
    }

    private void loadFromFile() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                manager.loadTasksFromFile(fc.getSelectedFile().getAbsolutePath());
                refreshTable(); refreshDashboard();
                log("📂 Loaded from " + fc.getSelectedFile().getName());
            } catch (Exception e) { error(e.getMessage()); }
        }
    }

    // ── Refresh helpers ───────────────────────────────────────────────────────

    private void refreshTable() {
        taskTableModel.setRowCount(0);
        for (Task t : manager.getTasks().values()) {
            String deps = t.getDependencies().isEmpty() ? "—" :
                String.join(", ", t.getDependencies().stream()
                    .map(Task::getId).toArray(String[]::new));
            String eng = t.getAssignedEngineer() != null
                ? t.getAssignedEngineer().getName() : "—";
            String dl  = t.getDeadline() != null
                ? new java.text.SimpleDateFormat("yyyy-MM-dd").format(t.getDeadline()) : "—";

            taskTableModel.addRow(new Object[]{
                t.getId(), t.getTitle(),
                t.getTaskStatus(), t.getPriorityLevel(),
                t.getTaskCategory(), eng, deps, dl
            });
        }

        // Color rows by status
        taskTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    Object status = table.getModel().getValueAt(row, 2);
                    if (status == TaskStatus.DONE)        c.setBackground(new Color(30, 50, 40));
                    else if (status == TaskStatus.BLOCKED) c.setBackground(new Color(50, 30, 30));
                    else if (status == TaskStatus.IN_PROGRESS) c.setBackground(new Color(50, 45, 20));
                    else                                   c.setBackground(BG_CARD);
                    c.setForeground(TEXT_PRIMARY);
                } else {
                    c.setBackground(ACCENT_BLUE.darker());
                    c.setForeground(Color.WHITE);
                }
                return c;
            }
        });
    }

    private void refreshDashboard() {
        long todo = count(TaskStatus.TODO), blocked = count(TaskStatus.BLOCKED),
             inP  = count(TaskStatus.IN_PROGRESS), done = count(TaskStatus.DONE);
        lblTodo.setText(String.valueOf(todo));
        lblBlocked.setText(String.valueOf(blocked));
        lblInProgress.setText(String.valueOf(inP));
        lblDone.setText(String.valueOf(done));
    }

    private long count(TaskStatus s) {
        return manager.getTasks().values().stream()
                      .filter(t -> t.getTaskStatus() == s).count();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User currentUser() {
        int idx = cbUsers.getSelectedIndex();
        return switch (idx) {
            case 0  -> alice;
            case 1  -> bob;
            case 2  -> charlie;
            default -> diana;
        };
    }

    private void log(String msg) {
        logArea.append("[" + new java.text.SimpleDateFormat("HH:mm:ss")
            .format(new Date()) + "] " + msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void error(String msg) {
        log("⚠ ERROR: " + msg);
        JOptionPane.showMessageDialog(this,
            "<html><body style='width:360px'>" + msg + "</body></html>",
            "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ── Style helpers ─────────────────────────────────────────────────────────

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_LABEL);
        l.setForeground(TEXT_MUTED);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JLabel statCard(String label, String value, Color color) {
        JLabel l = new JLabel(value);
        l.setFont(new Font("Segoe UI", Font.BOLD, 24));
        l.setForeground(color);
        l.putClientProperty("label", label);
        return l;
    }

    private JPanel statRow(JLabel valueLabel) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(BG_CARD);
        row.setBorder(new CompoundBorder(
            BorderFactory.createLineBorder(BG_DARK, 1),
            new EmptyBorder(8, 12, 8, 12)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        row.setAlignmentX(LEFT_ALIGNMENT);

        JLabel lbl = new JLabel((String) valueLabel.getClientProperty("label"));
        lbl.setFont(FONT_BODY);
        lbl.setForeground(TEXT_MUTED);

        row.add(lbl,        BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.EAST);
        return row;
    }

    private JButton actionButton(String text, Runnable action) {
        JButton btn = new JButton(text);
        styleButton(btn, BG_CARD);
        btn.setForeground(TEXT_PRIMARY);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btn.setAlignmentX(LEFT_ALIGNMENT);
        btn.addActionListener(e -> action.run());
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                btn.setBackground(ACCENT_BLUE.darker());
            }
            @Override public void mouseExited(MouseEvent e) {
                btn.setBackground(BG_CARD);
            }
        });
        return btn;
    }

    private void styleButton(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(TEXT_PRIMARY);
        btn.setFont(FONT_BODY);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void styleTable(JTable table) {
        table.setBackground(BG_CARD);
        table.setForeground(TEXT_PRIMARY);
        table.setFont(FONT_BODY);
        table.setRowHeight(32);
        table.setGridColor(BG_PANEL);
        table.setSelectionBackground(ACCENT_BLUE.darker());
        table.setSelectionForeground(Color.WHITE);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.getTableHeader().setBackground(BG_PANEL);
        table.getTableHeader().setForeground(TEXT_MUTED);
        table.getTableHeader().setFont(FONT_LABEL);
    }

    private void styleCombo(JComboBox<?> cb) {
        cb.setBackground(BG_CARD);
        cb.setForeground(TEXT_PRIMARY);
        cb.setFont(FONT_BODY);
    }

    // ── Entry point ───────────────────────────────────────────────────────────

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(STRMSMainWindow::new);
    }
}
