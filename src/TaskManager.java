package src;

import java.io.*;
import java.util.*;

import enumeration.PriorityLevel;
import enumeration.TaskCategory;
import enumeration.TaskStatus;
import exceptions.*;

/**
 * Central controller of the STRMS.
 * Enforces all business rules, manages task lifecycle,
 * handles dependencies, permissions, and persistence.
 */
public class TaskManager {

    // ── Data Structures ──────────────────────────────────────────────────────

    /** All tasks indexed by their unique ID for O(1) lookup. */
    private final HashMap<String, Task> tasks = new HashMap<>();

    /** All users indexed by their unique ID. */
    private final HashMap<String, User> users = new HashMap<>();

    /** Tasks currently being worked on (no duplicates). */
    private final HashSet<Task> inProgressTasks = new HashSet<>();

    /**
     * Ready-to-execute tasks ordered by priority (CRITICAL first).
     * A task is "ready" if all its dependencies are DONE.
     */
    private final PriorityQueue<Task> taskQueue = new PriorityQueue<>();

    // ── User management ──────────────────────────────────────────────────────

    public void addUser(User user) {
        users.put(user.getId(), user);
    }

    public User findUser(String userId) throws TaskNotFoundException {
        User user = users.get(userId);
        if (user == null) {
            throw new TaskNotFoundException("User:" + userId);
        }
        return user;
    }

    public HashMap<String, User> getUsers() { return users; }

    // ── Task CRUD ────────────────────────────────────────────────────────────

    /**
     * Adds a new task to the system.
     * Verifies that the requesting user has create permission.
     * Throws DuplicateTaskException if a task with the same ID already exists.
     */
    public void addTask(Task task, User requestedBy)
            throws InvalidRoleException, DuplicateTaskException {

        if (!requestedBy.canCreateTask()) {
            throw new InvalidRoleException(requestedBy.getName(), "create a task");
        }
        if (tasks.containsKey(task.getId())) {
            throw new DuplicateTaskException(task.getId());
        }

        tasks.put(task.getId(), task);

        // If no dependencies, directly queue the task
        if (task.allDependenciesDone()) {
            taskQueue.offer(task);
        }

        task.addHistoryEntry(new TaskHistoryEntry(
            "Task created with status " + task.getTaskStatus(), requestedBy));

        System.out.println("[TaskManager] Task '" + task.getTitle() + "' added by " + requestedBy.getName());
    }

    /**
     * Deletes a task from the system.
     * Only users with delete permission can do this.
     */
    public void deleteTask(String taskId, User requestedBy)
            throws InvalidRoleException, TaskNotFoundException {

        if (!requestedBy.canDeleteTask()) {
            throw new InvalidRoleException(requestedBy.getName(), "delete a task");
        }
        Task task = findTask(taskId);

        tasks.remove(taskId);
        inProgressTasks.remove(task);
        taskQueue.remove(task);

        System.out.println("[TaskManager] Task '" + task.getTitle() + "' deleted by " + requestedBy.getName());
    }

    /**
     * Assigns a task to an engineer.
     * Only users with assign permission can do this.
     * The task must not already be assigned.
     */
    public void assignTask(String taskId, Engineer engineer, User requestedBy)
            throws InvalidRoleException, TaskNotFoundException,
                   DependencyNotCompletedException, InvalidTaskStateException {

        if (!requestedBy.canAssignTask()) {
            throw new InvalidRoleException(requestedBy.getName(), "assign a task");
        }

        Task task = findTask(taskId);

        if (task.getTaskStatus() == TaskStatus.DONE) {
            throw new InvalidTaskStateException(task.getTitle(), "DONE", "IN_PROGRESS");
        }

        // Check dependencies before assigning
        for (Task dep : task.getDependencies()) {
            if (dep.getTaskStatus() != TaskStatus.DONE) {
                // Log violation in history
                task.addHistoryEntry(new TaskHistoryEntry(
                    "BLOCKED: Attempted assignment failed — dependency '"
                    + dep.getTitle() + "' is not completed.", requestedBy));
                throw new DependencyNotCompletedException(task.getTitle(), dep.getTitle());
            }
        }

        task.assignEngineer(engineer);
        task.updateStatus(TaskStatus.IN_PROGRESS);
        inProgressTasks.add(task);
        taskQueue.remove(task); // No longer "waiting"

        task.addHistoryEntry(new TaskHistoryEntry(
            "Assigned to engineer '" + engineer.getName() + "' by " + requestedBy.getName()
            + ". Status → IN_PROGRESS", requestedBy));

        System.out.println("[TaskManager] Task '" + task.getTitle()
                + "' assigned to " + engineer.getName());
    }

    /**
     * Marks a task as DONE.
     * Only the assigned engineer can complete a task.
     * After completion, checks if any blocked tasks can now be unblocked.
     */
    public void completeTask(String taskId, User requestedBy)
            throws TaskNotFoundException, InvalidRoleException, InvalidTaskStateException {

        Task task = findTask(taskId);

        // Only the assigned engineer may complete
        Engineer assigned = task.getAssignedEngineer();
        if (assigned == null || !assigned.getId().equals(requestedBy.getId())) {
            throw new InvalidRoleException(requestedBy.getName(),
                "complete task '" + task.getTitle() + "' (not the assigned engineer)");
        }

        if (task.getTaskStatus() == TaskStatus.DONE) {
            throw new InvalidTaskStateException(task.getTitle(), "DONE", "DONE");
        }

        task.markAsDone();
        inProgressTasks.remove(task);

        task.addHistoryEntry(new TaskHistoryEntry(
            "Task marked as DONE by " + requestedBy.getName(), requestedBy));

        System.out.println("[TaskManager] Task '" + task.getTitle() + "' completed.");

        // Unblock tasks that were waiting on this one
        activateDependentTasks(task);
    }

    /**
     * Updates the status of a task (general-purpose).
     * Enforces valid state transitions.
     */
    public void updateTask(String taskId, TaskStatus newStatus, User requestedBy)
            throws TaskNotFoundException, InvalidTaskStateException,
                   InvalidRoleException, DependencyNotCompletedException {

        Task task = findTask(taskId);

        TaskStatus current = task.getTaskStatus();

        // DONE is a terminal state
        if (current == TaskStatus.DONE) {
            throw new InvalidTaskStateException(task.getTitle(), "DONE", newStatus.name());
        }

        // Cannot move to IN_PROGRESS if dependencies are unfinished
        if (newStatus == TaskStatus.IN_PROGRESS) {
            for (Task dep : task.getDependencies()) {
                if (dep.getTaskStatus() != TaskStatus.DONE) {
                    task.addHistoryEntry(new TaskHistoryEntry(
                        "BLOCKED: Status change to IN_PROGRESS refused — dependency '"
                        + dep.getTitle() + "' incomplete.", requestedBy));
                    throw new DependencyNotCompletedException(task.getTitle(), dep.getTitle());
                }
            }
            inProgressTasks.add(task);
        }

        task.updateStatus(newStatus);
        task.addHistoryEntry(new TaskHistoryEntry(
            "Status changed from " + current + " to " + newStatus
            + " by " + requestedBy.getName(), requestedBy));

        System.out.println("[TaskManager] Task '" + task.getTitle()
                + "' status: " + current + " → " + newStatus);
    }

    // ── Dependencies ─────────────────────────────────────────────────────────

    /**
     * Adds a dependency: taskId depends on dependsOnId.
     * Validates: both tasks exist, no self-dependency, no circular dependency.
     */
    public void addDependency(String taskId, String dependsOnId, User requestedBy)
            throws TaskNotFoundException, CircularDependencyException,
                   InvalidTaskStateException, InvalidRoleException {

        Task task      = findTask(taskId);
        Task dependsOn = findTask(dependsOnId);

        // A task cannot depend on itself
        if (taskId.equals(dependsOnId)) {
            throw new CircularDependencyException(taskId, dependsOnId);
        }

        // A completed task should not gain new dependencies
        if (task.getTaskStatus() == TaskStatus.DONE) {
            throw new InvalidTaskStateException(task.getTitle(), "DONE", "adding dependency");
        }

        // Check for circular dependency BEFORE modifying the graph
        if (detectCircularDependency(dependsOn, task)) {
            task.addHistoryEntry(new TaskHistoryEntry(
                "REJECTED: Adding dependency on '" + dependsOn.getTitle()
                + "' would create a circular dependency.", requestedBy));
            throw new CircularDependencyException(task.getTitle(), dependsOn.getTitle());
        }

        task.addDependency(dependsOn);

        // If the dependency is not done, block the task
        if (dependsOn.getTaskStatus() != TaskStatus.DONE
                && task.getTaskStatus() != TaskStatus.IN_PROGRESS) {
            task.updateStatus(TaskStatus.BLOCKED);
            taskQueue.remove(task);
        }

        task.addHistoryEntry(new TaskHistoryEntry(
            "Dependency added: task now depends on '"
            + dependsOn.getTitle() + "'. Added by " + requestedBy.getName(), requestedBy));

        System.out.println("[TaskManager] Dependency added: '" + task.getTitle()
                + "' now depends on '" + dependsOn.getTitle() + "'");
    }

    /**
     * Removes a dependency between two tasks and re-evaluates blocked status.
     */
    public void removeDependency(String taskId, String dependsOnId, User requestedBy)
            throws TaskNotFoundException {

        Task task      = findTask(taskId);
        Task dependsOn = findTask(dependsOnId);

        task.removeDependency(dependsOn);

        // Re-evaluate: if all remaining deps are done, unblock
        if (task.allDependenciesDone() && task.getTaskStatus() == TaskStatus.BLOCKED) {
            task.updateStatus(TaskStatus.TODO);
            taskQueue.offer(task);
        }

        task.addHistoryEntry(new TaskHistoryEntry(
            "Dependency on '" + dependsOn.getTitle() + "' removed by "
            + requestedBy.getName(), requestedBy));

        System.out.println("[TaskManager] Dependency removed: '" + task.getTitle()
                + "' no longer depends on '" + dependsOn.getTitle() + "'");
    }

    /**
     * Depth-First Search to detect if 'current' can reach 'target' through dependencies.
     * If so, adding target → current would create a cycle.
     *
     * @param current The node we start from (the proposed new dependency)
     * @param target  The node we are checking reachability to
     * @return true if a cycle would be created
     */
    public boolean detectCircularDependency(Task current, Task target) {
        if (current == target) return true;
        Set<String> visited = new HashSet<>();
        return dfs(current, target, visited);
    }

    private boolean dfs(Task node, Task target, Set<String> visited) {
        if (node.getId().equals(target.getId())) return true;
        if (visited.contains(node.getId())) return false;
        visited.add(node.getId());
        for (Task dep : node.getDependencies()) {
            if (dfs(dep, target, visited)) return true;
        }
        return false;
    }

    // ── Task lookup & listing ─────────────────────────────────────────────────

    public Task findTask(String taskId) throws TaskNotFoundException {
        Task task = tasks.get(taskId);
        if (task == null) throw new TaskNotFoundException(taskId);
        return task;
    }

    public void printInProgressTasks() {
        System.out.println("\n── In-Progress Tasks ──────────────────");
        if (inProgressTasks.isEmpty()) {
            System.out.println("  (none)");
        } else {
            for (Task t : inProgressTasks) {
                System.out.println("  " + t);
            }
        }
        System.out.println("──────────────────────────────────────\n");
    }

    public void printAllTasks() {
        System.out.println("\n── All Tasks ──────────────────────────");
        if (tasks.isEmpty()) {
            System.out.println("  (none)");
        } else {
            for (Task t : tasks.values()) {
                System.out.println("  " + t);
            }
        }
        System.out.println("──────────────────────────────────────\n");
    }

    public HashMap<String, Task> getTasks()     { return tasks; }
    public HashSet<Task> getInProgressTasks()   { return inProgressTasks; }
    public PriorityQueue<Task> getTaskQueue()   { return taskQueue; }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    public void displayDashboard() {
        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║         STRMS  DASHBOARD             ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.println("║ Total tasks    : " + pad(tasks.size(), 20) + "║");

        long todo       = count(TaskStatus.TODO);
        long blocked    = count(TaskStatus.BLOCKED);
        long inProgress = count(TaskStatus.IN_PROGRESS);
        long done       = count(TaskStatus.DONE);

        System.out.println("║ TODO           : " + pad(todo, 20) + "║");
        System.out.println("║ BLOCKED        : " + pad(blocked, 20) + "║");
        System.out.println("║ IN_PROGRESS    : " + pad(inProgress, 20) + "║");
        System.out.println("║ DONE           : " + pad(done, 20) + "║");
        System.out.println("╠══════════════════════════════════════╣");

        // Tasks per user
        Map<String, Long> perEngineer = new HashMap<>();
        for (Task t : tasks.values()) {
            Engineer eng = t.getAssignedEngineer();
            if (eng != null) {
                perEngineer.merge(eng.getName(), 1L, Long::sum);
            }
        }
        System.out.println("║ Tasks per engineer:                  ║");
        for (Map.Entry<String, Long> e : perEngineer.entrySet()) {
            System.out.println("║   " + pad(e.getKey() + ": " + e.getValue(), 35) + "║");
        }

        // Overdue tasks (deadline passed and not DONE)
        Date now = new Date();
        long overdue = tasks.values().stream()
            .filter(t -> t.getDeadline() != null
                      && t.getDeadline().before(now)
                      && t.getTaskStatus() != TaskStatus.DONE)
            .count();
        System.out.println("║ Overdue tasks  : " + pad(overdue, 20) + "║");
        System.out.println("╚══════════════════════════════════════╝\n");
    }

    private long count(TaskStatus status) {
        return tasks.values().stream()
                    .filter(t -> t.getTaskStatus() == status)
                    .count();
    }

    private String pad(Object value, int width) {
        String s = String.valueOf(value);
        return s + " ".repeat(Math.max(0, width - s.length()));
    }

    // ── File Persistence ──────────────────────────────────────────────────────

    /**
     * Saves all tasks to a CSV file.
     * Format: id,title,description,priority,status,category,deadline,assignedEngineerId
     */
    public void saveTasksToFile(String filename) throws FilePersistenceException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("id,title,description,priority,status,category,deadline,assignedEngineer");
            writer.newLine();
            for (Task t : tasks.values()) {
                String engineerId = t.getAssignedEngineer() != null
                    ? t.getAssignedEngineer().getId() : "";
                String deadline   = t.getDeadline() != null
                    ? String.valueOf(t.getDeadline().getTime()) : "";
                writer.write(String.join(",",
                    t.getId(), t.getTitle(), t.getDescription(),
                    t.getPriorityLevel().name(), t.getTaskStatus().name(),
                    t.getTaskCategory().name(), deadline, engineerId));
                writer.newLine();
            }
            System.out.println("[TaskManager] Tasks saved to '" + filename + "'");
        } catch (IOException e) {
            throw new FilePersistenceException(filename, e.getMessage());
        }
    }

    /**
     * Loads tasks from a CSV file (created by saveTasksToFile).
     * Engineers referenced must already be loaded in the users map.
     */
    public void loadTasksFromFile(String filename) throws FilePersistenceException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line = reader.readLine(); // skip header
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length < 8) continue;

                Date deadline = parts[6].isEmpty() ? null
                    : new Date(Long.parseLong(parts[6]));

                Task task = new Task(
                    parts[0], parts[1], parts[2],
                    PriorityLevel.valueOf(parts[3]),
                    TaskStatus.valueOf(parts[4]),
                    TaskCategory.valueOf(parts[5]),
                    deadline
                );

                if (!parts[7].isEmpty()) {
                    User u = users.get(parts[7]);
                    if (u instanceof Engineer) {
                        task.assignEngineer((Engineer) u);
                    }
                }

                tasks.put(task.getId(), task);
            }
            System.out.println("[TaskManager] Tasks loaded from '" + filename + "'");
        } catch (IOException e) {
            throw new FilePersistenceException(filename, e.getMessage());
        }
    }

    // ── Report generation ─────────────────────────────────────────────────────

    /**
     * Generates a text report of all tasks with their full history.
     */
    public void generateReport(String filename) throws FilePersistenceException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("=== STRMS Task Report ===\n\n");
            for (Task t : tasks.values()) {
                writer.write("Task: " + t.getTitle() + " [" + t.getId() + "]\n");
                writer.write("  Status   : " + t.getTaskStatus() + "\n");
                writer.write("  Priority : " + t.getPriorityLevel() + "\n");
                writer.write("  Category : " + t.getTaskCategory() + "\n");
                writer.write("  Assigned : " + (t.getAssignedEngineer() != null
                    ? t.getAssignedEngineer().getName() : "Unassigned") + "\n");
                writer.write("  History  :\n");
                for (TaskHistoryEntry entry : t.getHistory()) {
                    writer.write("    " + entry.toString() + "\n");
                }
                writer.write("\n");
            }
            System.out.println("[TaskManager] Report saved to '" + filename + "'");
        } catch (IOException e) {
            throw new FilePersistenceException(filename, e.getMessage());
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * After a task is completed, scan all tasks to unblock those
     * whose dependencies are now all DONE.
     */
    private void activateDependentTasks(Task completedTask) {
        for (Task t : tasks.values()) {
            if (t.getTaskStatus() == TaskStatus.BLOCKED && t.allDependenciesDone()) {
                t.updateStatus(TaskStatus.TODO);
                taskQueue.offer(t);
                t.addHistoryEntry(new TaskHistoryEntry(
                    "Automatically unblocked: all dependencies are now DONE.", null));
                System.out.println("[TaskManager] Task '" + t.getTitle()
                        + "' unblocked and added to queue.");
            }
        }
    }
}
