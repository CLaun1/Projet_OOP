package tests;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

import src.*;
import src.enumeration.PriorityLevel;
import src.enumeration.TaskCategory;
import src.enumeration.TaskStatus;
import src.exceptions.*;

/**
 * JUnit 5 test suite for TaskManager.
 * Covers: dependencies, circular detection, graph integrity,
 *         state transitions, role permissions.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TaskManagerTest {

    private TaskManager manager;
    private Admin       alice;
    private Manager     bob;
    private Engineer    charlie;

    private Task task1, task2, task3, task4;

    @BeforeEach
    void setUp() {
        manager = new TaskManager();

        // ── Constructeurs adaptés à tes classes ───────────────────────────
        alice   = new Admin("A01",    "Alice",   "alice@strms.com",   1);
        bob     = new Manager("M01",  "Bob",     "bob@strms.com",     "DevTeam");
        charlie = new Engineer("E01", "Charlie", "charlie@strms.com", "Backend");

        manager.addUser(alice);
        manager.addUser(bob);
        manager.addUser(charlie);

        // ── Tâches réutilisées dans tous les tests ────────────────────────
        task1 = new Task("T1", "Define Requirements", "Gather all requirements",
                PriorityLevel.HIGH, TaskStatus.TODO, TaskCategory.DOCUMENTATION, null);
        task2 = new Task("T2", "Design Architecture", "System design",
                PriorityLevel.HIGH, TaskStatus.TODO, TaskCategory.FEATURE, null);
        task3 = new Task("T3", "Implement Modules", "Core implementation",
                PriorityLevel.MEDIUM, TaskStatus.TODO, TaskCategory.FEATURE, null);
        task4 = new Task("T4", "System Testing", "Full test suite",
                PriorityLevel.LOW, TaskStatus.TODO, TaskCategory.BUGFIX, null);
    }

    // ── Ajout de tâches ───────────────────────────────────────────────────────

    @Test @Order(1)
    @DisplayName("Admin can add tasks")
    void testAdminCanAddTask() {
        assertDoesNotThrow(() -> manager.addTask(task1, alice));
        assertNotNull(manager.getTasks().get("T1"));
    }

    @Test @Order(2)
    @DisplayName("Engineer cannot add tasks — InvalidRoleException")
    void testEngineerCannotAddTask() {
        assertThrows(InvalidRoleException.class,
            () -> manager.addTask(task1, charlie));
    }

    @Test @Order(3)
    @DisplayName("Duplicate task ID throws DuplicateTaskException")
    void testDuplicateTask() throws Exception {
        manager.addTask(task1, alice);
        Task duplicate = new Task("T1", "Duplicate", "dup",
                PriorityLevel.LOW, TaskStatus.TODO, TaskCategory.RESEARCH, null);
        assertThrows(DuplicateTaskException.class,
            () -> manager.addTask(duplicate, alice));
    }

    // ── Dépendances valides ───────────────────────────────────────────────────

    @Test @Order(4)
    @DisplayName("Valid linear dependency chain T1→T2→T3→T4")
    void testAddValidDependency() throws Exception {
        manager.addTask(task1, alice);
        manager.addTask(task2, alice);
        manager.addTask(task3, alice);
        manager.addTask(task4, alice);

        assertDoesNotThrow(() -> manager.addDependency("T2", "T1", alice));
        assertDoesNotThrow(() -> manager.addDependency("T3", "T2", alice));
        assertDoesNotThrow(() -> manager.addDependency("T4", "T3", alice));

        assertEquals(1, task2.getDependencies().size());
        assertTrue(task2.getDependencies().contains(task1));
    }

    // ── Détection de dépendances circulaires ──────────────────────────────────

    @Test @Order(5)
    @DisplayName("Direct circular dependency T1→T1 rejected")
    void testSelfDependencyRejected() throws Exception {
        manager.addTask(task1, alice);
        assertThrows(CircularDependencyException.class,
            () -> manager.addDependency("T1", "T1", alice));
    }

    @Test @Order(6)
    @DisplayName("Circular dependency T1→T2→T3→T1 rejected")
    void testCircularDependency() throws Exception {
        manager.addTask(task1, alice);
        manager.addTask(task2, alice);
        manager.addTask(task3, alice);

        manager.addDependency("T2", "T1", alice); // T2 dépend de T1
        manager.addDependency("T3", "T2", alice); // T3 dépend de T2

        // T1 dépend de T3 → cycle T1→T2→T3→T1
        assertThrows(CircularDependencyException.class,
            () -> manager.addDependency("T1", "T3", alice));
    }

    // ── Intégrité du graphe après rejet ───────────────────────────────────────

    @Test @Order(7)
    @DisplayName("Graph unchanged after circular dependency rejection")
    void testGraphIntegrity() throws Exception {
        manager.addTask(task1, alice);
        manager.addTask(task2, alice);
        manager.addTask(task3, alice);

        manager.addDependency("T2", "T1", alice);
        manager.addDependency("T3", "T2", alice);

        int depsBeforeRejection = task1.getDependencies().size();

        try {
            manager.addDependency("T1", "T3", alice);
        } catch (CircularDependencyException e) {
            // rejet attendu
        }

        // T1 ne doit avoir aucune dépendance nouvelle après le rejet
        assertEquals(depsBeforeRejection, task1.getDependencies().size(),
            "Dependency list of T1 must be unchanged after rejection");
    }

    // ── Suppression de dépendance ─────────────────────────────────────────────

    @Test @Order(8)
    @DisplayName("Removing a dependency unblocks the task when all deps done")
    void testRemoveDependency() throws Exception {
        manager.addTask(task1, alice);
        manager.addTask(task2, alice);

        manager.addDependency("T2", "T1", alice);

        // T2 doit être BLOCKED car T1 n'est pas DONE
        assertEquals(TaskStatus.BLOCKED, task2.getTaskStatus(),
            "T2 should be BLOCKED because T1 is not done");

        manager.removeDependency("T2", "T1", alice);

        // T2 doit revenir à TODO après suppression de la dépendance
        assertEquals(TaskStatus.TODO, task2.getTaskStatus(),
            "T2 should be back to TODO after dependency removal");
    }

    // ── Transitions d'état invalides ──────────────────────────────────────────

    @Test @Order(9)
    @DisplayName("Cannot transition a DONE task — InvalidTaskStateException")
    void testInvalidState() throws Exception {
        manager.addTask(task1, alice);

        // assignTask utilise bob (Manager) qui a canAssignTask() = true
        manager.assignTask("T1", charlie, bob);
        manager.completeTask("T1", charlie);

        assertThrows(InvalidTaskStateException.class,
            () -> manager.updateTask("T1", TaskStatus.IN_PROGRESS, charlie));
    }

    @Test @Order(10)
    @DisplayName("Cannot start task with incomplete dependencies — DependencyNotCompletedException")
    void testCannotStartBlockedTask() throws Exception {
        manager.addTask(task1, alice);
        manager.addTask(task2, alice);

        manager.addDependency("T2", "T1", alice); // T2 bloquée sur T1

        // Tenter d'assigner T2 alors que T1 n'est pas DONE
        assertThrows(DependencyNotCompletedException.class,
            () -> manager.assignTask("T2", charlie, bob));
    }

    // ── Permissions par rôle ──────────────────────────────────────────────────

    @Test @Order(11)
    @DisplayName("Engineer cannot delete tasks — InvalidRoleException")
    void testRolePermissions() throws Exception {
        manager.addTask(task1, alice);
        assertThrows(InvalidRoleException.class,
            () -> manager.deleteTask("T1", charlie));
    }

    @Test @Order(12)
    @DisplayName("Only assigned engineer can complete a task")
    void testOnlyAssignedEngineerCompletes() throws Exception {
        manager.addTask(task1, alice);
        manager.assignTask("T1", charlie, bob);

        // Un autre ingénieur ne peut pas compléter la tâche de charlie
        Engineer dave = new Engineer("E02", "Dave", "dave@strms.com", "Frontend");
        manager.addUser(dave);

        assertThrows(InvalidRoleException.class,
            () -> manager.completeTask("T1", dave));
    }

    // ── Workflow complet ──────────────────────────────────────────────────────

    @Test @Order(13)
    @DisplayName("Full lifecycle: create → assign → complete → unblock next task")
    void testFullWorkflow() throws Exception {
        manager.addTask(task1, alice);
        manager.addTask(task2, alice);

        manager.addDependency("T2", "T1", alice);
        assertEquals(TaskStatus.BLOCKED, task2.getTaskStatus());

        manager.assignTask("T1", charlie, bob);
        assertEquals(TaskStatus.IN_PROGRESS, task1.getTaskStatus());

        manager.completeTask("T1", charlie);
        assertEquals(TaskStatus.DONE, task1.getTaskStatus());

        // T2 doit être automatiquement débloquée par activateDependentTasks()
        assertEquals(TaskStatus.TODO, task2.getTaskStatus(),
            "T2 should be unblocked now that T1 is DONE");
    }

    // ── Tâche introuvable ─────────────────────────────────────────────────────

    @Test @Order(14)
    @DisplayName("findTask with unknown ID throws TaskNotFoundException")
    void testTaskNotFound() {
        assertThrows(TaskNotFoundException.class,
            () -> manager.findTask("NONEXISTENT"));
    }
}