package src;

import enumeration.PriorityLevel;
import enumeration.TaskCategory;
import enumeration.TaskStatus;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Task implements Comparable<Task> {

    private String id;              // Unique string ID (e.g., "T001")
    private String title;
    private String description;
    private PriorityLevel priorityLevel;
    private TaskStatus taskStatus;
    private TaskCategory taskCategory;
    private Date deadline;
    private Engineer assignedEngineer;  // null if unassigned

    private final List<Task> dependencies    = new ArrayList<>(); // Tasks that must be DONE before this one
    private final List<TaskHistoryEntry> history = new ArrayList<>();

    // ── Constructors ────────────────────────────────────────────────────────

    public Task(String id, String title, String description,
                PriorityLevel priorityLevel, TaskStatus taskStatus,
                TaskCategory taskCategory, Date deadline) {
        this.id            = id;
        this.title         = title;
        this.description   = description;
        this.priorityLevel = priorityLevel;
        this.taskStatus    = taskStatus;
        this.taskCategory  = taskCategory;
        this.deadline      = deadline;
    }

    // ── Core methods ─────────────────────────────────────────────────────────

    /**
     * Updates task status. Does NOT enforce business rules (TaskManager does that).
     */
    public void updateStatus(TaskStatus newStatus) {
        this.taskStatus = newStatus;
    }

    /** Appends an entry to this task's history. */
    public void addHistoryEntry(TaskHistoryEntry entry) {
        history.add(entry);
    }

    /**
     * Adds a prerequisite task to this task's dependency list.
     * Circular dependency prevention is handled by TaskManager BEFORE calling this.
     */
    public void addDependency(Task dependency) {
        if (!dependencies.contains(dependency)) {
            dependencies.add(dependency);
        }
    }

    /** Removes a prerequisite task from the dependency list. */
    public void removeDependency(Task dependency) {
        dependencies.remove(dependency);
    }

    /**
     * Returns true if ALL dependencies are in DONE state (or there are none).
     */
    public boolean allDependenciesDone() {
        for (Task dep : dependencies) {
            if (dep.getTaskStatus() != TaskStatus.DONE) {
                return false;
            }
        }
        return true;
    }

    /** Marks the task as DONE. Terminal state — no further transitions allowed. */
    public void markAsDone() {
        this.taskStatus = TaskStatus.DONE;
    }

    public void changePriority(PriorityLevel newPriorityLevel) {
        this.priorityLevel = newPriorityLevel;
    }

    public void updateDescription(String newDesc) {
        this.description = newDesc;
    }

    public void assignEngineer(Engineer engineer) {
        this.assignedEngineer = engineer;
    }

    /** Prints a formatted summary of the task to the console. */
    public void displayTask() {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("  Task ID       : " + id);
        System.out.println("  Title         : " + title);
        System.out.println("  Description   : " + description);
        System.out.println("  Status        : " + taskStatus);
        System.out.println("  Priority      : " + priorityLevel);
        System.out.println("  Category      : " + taskCategory);
        System.out.println("  Deadline      : " + (deadline != null ? deadline.toString() : "N/A"));
        System.out.println("  Assigned To   : " + (assignedEngineer != null ? assignedEngineer.getName() : "Unassigned"));
        System.out.print  ("  Dependencies  : ");
        if (dependencies.isEmpty()) {
            System.out.println("None");
        } else {
            for (Task dep : dependencies) {
                System.out.print("[" + dep.getId() + " - " + dep.getTitle() + "] ");
            }
            System.out.println();
        }
        System.out.println("  History (" + history.size() + " entries):");
        for (TaskHistoryEntry entry : history) {
            System.out.println("    " + entry.toString());
        }
        System.out.println("╚══════════════════════════════════════╝");
    }

    // ── Comparable — higher priority = smaller ordinal = first in PriorityQueue ──

    @Override
    public int compareTo(Task other) {
        // CRITICAL(0) < HIGH(1) < MEDIUM(2) < LOW(3) → reverse ordinal for min-heap
        return Integer.compare(
            other.priorityLevel.ordinal(),
            this.priorityLevel.ordinal()
        );
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public String getId()                       { return id; }
    public String getTitle()                    { return title; }
    public String getDescription()              { return description; }
    public PriorityLevel getPriorityLevel()     { return priorityLevel; }
    public TaskStatus getTaskStatus()           { return taskStatus; }
    public TaskCategory getTaskCategory()       { return taskCategory; }
    public Date getDeadline()                   { return deadline; }
    public Engineer getAssignedEngineer()       { return assignedEngineer; }
    public List<Task> getDependencies()         { return dependencies; }
    public List<TaskHistoryEntry> getHistory()  { return history; }

    public void setId(String id)                { this.id = id; }
    public void setTitle(String title)          { this.title = title; }
    public void setDeadline(Date deadline)      { this.deadline = deadline; }

    @Override
    public String toString() {
        return "[" + id + "] " + title + " (" + taskStatus + " | " + priorityLevel + ")";
    }
}
