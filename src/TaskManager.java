package src;

import src.enumeration.PriorityLevel;
import src.enumeration.TaskCategory;
import src.enumeration.TaskStatus;
import src.exceptions.*;

import java.io.*;
import java.util.*;

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
        if (user != null) {
            users.put(user.getId(), user);
        }
    }

    public void addUser(User user, User requestedBy) throws InvalidRoleException {
        if (!(requestedBy instanceof Admin)) {
            throw new InvalidRoleException(requestedBy.getName(), "add a user");
        }
        addUser(user);
    }

    public User findUser(String userId) throws TaskNotFoundException {
        User user = users.get(userId);
        if (user == null) {
            throw new TaskNotFoundException("User:" + userId);
        }
        return user;
    }

    public void deleteUser(String id, User requestedBy) throws InvalidRoleException, TaskNotFoundException {
        if (!(requestedBy instanceof Admin)) {
            throw new InvalidRoleException(requestedBy.getName(), "delete a user");
        }
        
        if (!users.containsKey(id)) {
            throw new TaskNotFoundException("User ID: " + id);
        }

        // Nettoyage : Désassigner l'utilisateur des tâches en cours
        for (Task t : tasks.values()) {
            if (t.getAssignedUser() != null && t.getAssignedUser().getId().equals(id)) {
                t.assignUser(null);
                if (t.getTaskStatus() == TaskStatus.IN_PROGRESS) {
                    t.updateStatus(TaskStatus.TODO);
                    inProgressTasks.remove(t);
                    taskQueue.offer(t);
                }
                t.addHistoryEntry(new TaskHistoryEntry("Unassigned automatically because the user was deleted.", requestedBy));
            }
        }

        users.remove(id);
        System.out.println("[TaskManager] User '" + id + "' deleted by " + requestedBy.getName());
    }

    public HashMap<String, User> getUsers() { return users; }

    // ── Task CRUD ────────────────────────────────────────────────────────────

    /**
     * Adds a new task to the system.
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
        } else {
            task.updateStatus(TaskStatus.BLOCKED);
        }

        task.addHistoryEntry(new TaskHistoryEntry(
            "Task created with status " + task.getTaskStatus(), requestedBy));

        System.out.println("[TaskManager] Task '" + task.getTitle() + "' added by " + requestedBy.getName());
    }

    /**
     * Deletes a task from the system.
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
     * Assigns a task to an engineer or a manager.
     */
    public void assignTask(String taskId, User engineerOrManager, User requestedBy)
            throws InvalidRoleException, TaskNotFoundException,
                   DependencyNotCompletedException, InvalidTaskStateException {

        if (!requestedBy.canAssignTask()) {
            throw new InvalidRoleException(requestedBy.getName(), "assign a task");
        }

        if (engineerOrManager instanceof Admin) {
            throw new InvalidRoleException(engineerOrManager.getName(), "be assigned to a operational task (Admin restricted)");
        }

        Task task = findTask(taskId);

        if (task.getTaskStatus() == TaskStatus.DONE) {
            throw new InvalidTaskStateException(task.getTitle(), "DONE", "IN_PROGRESS");
        }

        // Check dependencies before assigning
        for (Task dep : task.getDependencies()) {
            if (dep.getTaskStatus() != TaskStatus.DONE) {
                task.addHistoryEntry(new TaskHistoryEntry(
                    "BLOCKED: Attempted assignment failed — dependency '"
                    + dep.getTitle() + "' is not completed.", requestedBy));
                throw new DependencyNotCompletedException(task.getTitle(), dep.getTitle());
            }
        }

        task.assignUser(engineerOrManager);
        task.updateStatus(TaskStatus.IN_PROGRESS);
        inProgressTasks.add(task);
        taskQueue.remove(task); // No longer "waiting"

        task.addHistoryEntry(new TaskHistoryEntry(
            "Assigned to " + engineerOrManager.getRole() + " '" + engineerOrManager.getName() + "' by " + requestedBy.getName()
            + ". Status → IN_PROGRESS", requestedBy));

        System.out.println("[TaskManager] Task '" + task.getTitle()
                + "' assigned to " + engineerOrManager.getName());
    }

    /**
     * Marks a task as DONE.
     */
    public void completeTask(String taskId, User requestedBy)
            throws TaskNotFoundException, InvalidRoleException, InvalidTaskStateException {

        Task task = findTask(taskId);

        User assigned = task.getAssignedUser();
        
        // Authorization check: Only the assigned user or a Manager/Admin can close it
        if (assigned == null || (!assigned.getId().equals(requestedBy.getId()) && !requestedBy.canUpdateTask())) {
            throw new InvalidRoleException(requestedBy.getName(),
                "complete task '" + task.getTitle() + "' (not the assigned operator)");
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
     */
    public void updateTask(String taskId, TaskStatus newStatus, User requestedBy)
            throws TaskNotFoundException, InvalidTaskStateException,
                   InvalidRoleException, DependencyNotCompletedException {

        Task task = findTask(taskId);
        TaskStatus current = task.getTaskStatus();

        if (current == TaskStatus.DONE) {
            throw new InvalidTaskStateException(task.getTitle(), "DONE", newStatus.name());
        }

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

    public void addDependency(String taskId, String dependsOnId, User requestedBy)
            throws TaskNotFoundException, CircularDependencyException,
                   InvalidTaskStateException, InvalidRoleException {

        Task task      = findTask(taskId);
        Task dependsOn = findTask(dependsOnId);

        if (taskId.equals(dependsOnId)) {
            throw new CircularDependencyException(taskId, dependsOnId);
        }

        if (task.getTaskStatus() == TaskStatus.DONE) {
            throw new InvalidTaskStateException(task.getTitle(), "DONE", "adding dependency");
        }

        if (detectCircularDependency(dependsOn, task)) {
            task.addHistoryEntry(new TaskHistoryEntry(
                "REJECTED: Adding dependency on '" + dependsOn.getTitle()
                + "' would create a circular dependency.", requestedBy));
            throw new CircularDependencyException(task.getTitle(), dependsOn.getTitle());
        }

        task.addDependency(dependsOn);

        if (dependsOn.getTaskStatus() != TaskStatus.DONE && task.getTaskStatus() != TaskStatus.IN_PROGRESS) {
            task.updateStatus(TaskStatus.BLOCKED);
            taskQueue.remove(task);
        }

        task.addHistoryEntry(new TaskHistoryEntry(
            "Dependency added: task now depends on '"
            + dependsOn.getTitle() + "'. Added by " + requestedBy.getName(), requestedBy));
    }

    public void removeDependency(String taskId, String dependsOnId, User requestedBy)
            throws TaskNotFoundException {

        Task task      = findTask(taskId);
        Task dependsOn = findTask(dependsOnId);

        task.removeDependency(dependsOn);

        if (task.allDependenciesDone() && task.getTaskStatus() == TaskStatus.BLOCKED) {
            task.updateStatus(TaskStatus.TODO);
            taskQueue.offer(task);
        }

        task.addHistoryEntry(new TaskHistoryEntry(
            "Dependency on '" + dependsOn.getTitle() + "' removed by "
            + requestedBy.getName(), requestedBy));
    }

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

        Map<String, Long> perUser = new HashMap<>();
        for (Task t : tasks.values()) {
            User u = t.getAssignedUser();
            if (u != null) {
                perUser.merge(u.getName() + " (" + u.getRole() + ")", 1L, Long::sum);
            }
        }
        System.out.println("║ Tasks per operator:                  ║");
        for (Map.Entry<String, Long> e : perUser.entrySet()) {
            System.out.println("║   " + pad(e.getKey() + ": " + e.getValue(), 35) + "║");
        }

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
        return tasks.values().stream().filter(t -> t.getTaskStatus() == status).count();
    }

    private String pad(Object value, int width) {
        String s = String.valueOf(value);
        return s + " ".repeat(Math.max(0, width - s.length()));
    }

    // ── File Persistence ──────────────────────────────────────────────────────

    public void saveTasksToFile(String filename) throws FilePersistenceException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("id,title,description,priority,status,category,deadline,assignedUser");
            writer.newLine();
            for (Task t : tasks.values()) {
                String userId = t.getAssignedUser() != null ? t.getAssignedUser().getId() : "";
                String deadline = t.getDeadline() != null ? String.valueOf(t.getDeadline().getTime()) : "";
                writer.write(String.join(",",
                    t.getId(), t.getTitle(), t.getDescription(),
                    t.getPriorityLevel().name(), t.getTaskStatus().name(),
                    t.getTaskCategory().name(), deadline, userId));
                writer.newLine();
            }
            System.out.println("[TaskManager] Tasks saved to '" + filename + "'");
        } catch (IOException e) {
            throw new FilePersistenceException(filename, e.getMessage());
        }
    }

    public void loadTasksFromFile(String filename) throws FilePersistenceException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line = reader.readLine(); // skip header
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length < 8) continue;

                Date deadline = parts[6].isEmpty() ? null : new Date(Long.parseLong(parts[6]));

                Task task = new Task(
                    parts[0], parts[1], parts[2],
                    PriorityLevel.valueOf(parts[3]),
                    TaskStatus.valueOf(parts[4]),
                    TaskCategory.valueOf(parts[5]),
                    deadline
                );

                if (!parts[7].isEmpty()) {
                    User u = users.get(parts[7]);
                    if (u != null) {
                        task.assignUser(u);
                    }
                }

                tasks.put(task.getId(), task);
                
                // Re-populate system memory queues
                if (task.getTaskStatus() == TaskStatus.IN_PROGRESS) {
                    inProgressTasks.add(task);
                } else if (task.getTaskStatus() == TaskStatus.TODO) {
                    taskQueue.offer(task);
                }
            }
            System.out.println("[TaskManager] Tasks loaded from '" + filename + "'");
        } catch (IOException | IllegalArgumentException e) {
            throw new FilePersistenceException(filename, e.getMessage());
        }
    }

    // ── Report generation ─────────────────────────────────────────────────────

    public void generateReport(String filename) throws FilePersistenceException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("=== STRMS Task Report ===\n\n");
            for (Task t : tasks.values()) {
                writer.write("Task: " + t.getTitle() + " [" + t.getId() + "]\n");
                writer.write("  Status   : " + t.getTaskStatus() + "\n");
                writer.write("  Priority : " + t.getPriorityLevel() + "\n");
                writer.write("  Category : " + t.getTaskCategory() + "\n");
                writer.write("  Assigned : " + (t.getAssignedUser() != null
                    ? t.getAssignedUser().getName() + " (" + t.getAssignedUser().getRole() + ")" : "Unassigned") + "\n");
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