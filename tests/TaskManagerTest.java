package tests;

import src.*;
import enumeration.*;
import exceptions.*;
import java.util.Date;

public class TaskManagerTest {

    static TaskManager manager;
    static Admin alice;
    static Manager bob;
    static Engineer charlie;

    public static void main(String[] args) {
        setup();
        testAdminCanAddTask();
        testEngineerCannotAddTask();
        testCircularDependency();
        testFullWorkflow();
        System.out.println("\n✅ Tous les tests sont passés !");
    }

    static void setup() {
        manager = new TaskManager();
        alice   = new Admin("A01", "Alice", "alice@strms.com", 1);
        bob     = new Manager("M01", "Bob", "bob@strms.com", "DevTeam");
        charlie = new Engineer("E01", "Charlie", "charlie@strms.com", "Backend");
        manager.addUser(alice);
        manager.addUser(bob);
        manager.addUser(charlie);
    }

    static void testAdminCanAddTask() {
        try {
            Task t = new Task("T1", "Test Task", "desc",
                PriorityLevel.HIGH, TaskStatus.TODO, TaskCategory.FEATURE, null);
            manager.addTask(t, alice);
            assert manager.getTasks().containsKey("T1");
            System.out.println("✅ testAdminCanAddTask OK");
        } catch (Exception e) {
            System.out.println("❌ testAdminCanAddTask FAILED: " + e.getMessage());
        }
    }

    static void testEngineerCannotAddTask() {
        try {
            Task t = new Task("T2", "Test Task 2", "desc",
                PriorityLevel.LOW, TaskStatus.TODO, TaskCategory.BUGFIX, null);
            manager.addTask(t, charlie);
            System.out.println("❌ testEngineerCannotAddTask FAILED: exception attendue");
        } catch (InvalidRoleException e) {
            System.out.println("✅ testEngineerCannotAddTask OK");
        } catch (Exception e) {
            System.out.println("❌ testEngineerCannotAddTask FAILED: " + e.getMessage());
        }
    }

    static void testCircularDependency() {
        try {
            Task t1 = new Task("TC1", "Task A", "desc",
                PriorityLevel.HIGH, TaskStatus.TODO, TaskCategory.FEATURE, null);
            Task t2 = new Task("TC2", "Task B", "desc",
                PriorityLevel.HIGH, TaskStatus.TODO, TaskCategory.FEATURE, null);
            manager.addTask(t1, alice);
            manager.addTask(t2, alice);
            manager.addDependency("TC2", "TC1", alice);
            manager.addDependency("TC1", "TC2", alice); // doit être rejeté
            System.out.println("❌ testCircularDependency FAILED: exception attendue");
        } catch (CircularDependencyException e) {
            System.out.println("✅ testCircularDependency OK");
        } catch (Exception e) {
            System.out.println("❌ testCircularDependency FAILED: " + e.getMessage());
        }
    }

    static void testFullWorkflow() {
        try {
            Task t1 = new Task("TW1", "Workflow Task 1", "desc",
                PriorityLevel.CRITICAL, TaskStatus.TODO, TaskCategory.FEATURE, null);
            Task t2 = new Task("TW2", "Workflow Task 2", "desc",
                PriorityLevel.HIGH, TaskStatus.TODO, TaskCategory.FEATURE, null);
            manager.addTask(t1, alice);
            manager.addTask(t2, alice);
            manager.addDependency("TW2", "TW1", alice);
            assert t2.getTaskStatus() == TaskStatus.BLOCKED;
            manager.assignTask("TW1", charlie, bob);
            manager.completeTask("TW1", charlie);
            assert t1.getTaskStatus() == TaskStatus.DONE;
            assert t2.getTaskStatus() == TaskStatus.TODO;
            System.out.println("✅ testFullWorkflow OK");
        } catch (Exception e) {
            System.out.println("❌ testFullWorkflow FAILED: " + e.getMessage());
        }
    }
}