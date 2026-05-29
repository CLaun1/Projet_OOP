package src;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import src.enumeration.*;

/**
 * Represents a single unit of work in the STRMS.
 * Encapsulates its own data, maintains dependency list and history.
 * Implements Comparable to allow priority-based ordering in PriorityQueue.
 */
public class Task implements Comparable<Task> {

    private String id;              // Unique string ID (e.g., "T001")
    private String title;
    private String description;
    private PriorityLevel priorityLevel;
    private TaskStatus taskStatus;
    private TaskCategory taskCategory;
    private Date deadline;
    private User assignedUser;      // PolyMorphic: Can be an Engineer or a Manager (null if unassigned)

    private final List<Task> dependencies = new ArrayList<>(); // Tasks that must be DONE before this one
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
     */
    public void addDependency(Task dependency) {
        if (dependency != null && !dependencies.contains(dependency)) {
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

    public void assignUser(User user) {
        this.assignedUser = user;
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
        System.out.println("  Assigned To   : " + (assignedUser != null ? assignedUser.getName() + " (" + assignedUser.getRole() + ")" : "Unassigned"));
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

    // ── Comparable — Corrected Min-Heap Ordinal Ordering ────────────────────

    @Override
    public int compareTo(Task other) {
        // Ordinals: CRITICAL(0), HIGH(1), MEDIUM(2), LOW(3)
        // If 'this' is CRITICAL(0) and 'other' is LOW(3) -> returns negative (-3) -> 'this' goes first.
        return Integer.compare(
            this.priorityLevel.ordinal(),
            other.priorityLevel.ordinal()
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
    public User getAssignedUser()               { return assignedUser; }
    public List<Task> getDependencies()         { return dependencies; }
    public List<TaskHistoryEntry> getHistory()  { return history; }

    public void setId(String id)                { this.id = id; }
    public void setTitle(String title)          { this.title = title; }
    public void setDeadline(Date deadline)      { this.deadline = deadline; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task)) return false;
        Task task = (Task) o;
        return Objects.equals(id, task.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "[" + id + "] " + title + " (" + taskStatus + " | " + priorityLevel + ")";
    }
}