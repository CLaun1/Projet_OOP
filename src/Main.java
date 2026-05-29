package src;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import src.controller.LoginController;

import java.util.HashMap;
import java.util.Map;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        // ── 1. Initialisation du système ───────────────────────────────────
        TaskManager taskManager = new TaskManager();

        // ── 2. Création des utilisateurs ───────────────────────────────────
        Admin    alice   = new Admin("U-001",    "Alice",   "alice@strms.fr",   1);
        Manager  bob     = new Manager("U-002",  "Bob",     "bob@strms.fr",     "DevTeam");
        Engineer charlie = new Engineer("U-003", "Charlie", "charlie@strms.fr", "Backend");

        taskManager.addUser(alice);
        taskManager.addUser(bob);
        taskManager.addUser(charlie);

        // ── 3. Mots de passe ───────────────────────────────────────────────
        Map<String, String> passwords = new HashMap<>();
        passwords.put("alice@strms.fr",   "admin123");
        passwords.put("bob@strms.fr",     "manager123");
        passwords.put("charlie@strms.fr", "engineer123");

        // ── 4. Chargement des tâches persistées ───────────────────────────
        try {
            taskManager.loadTasksFromFile("data/tasks.csv");
        } catch (Exception e) {
            System.out.println("[Main] Aucun fichier de sauvegarde trouvé, démarrage vide.");
        }

        // ── 5. Chargement du LoginView ────────────────────────────────────
        // Chemin relatif au package src — tes FXML sont dans src/view/
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("view/LoginView.fxml"));
        Parent root = loader.load();

        // ── 6. Injection dans LoginController ────────────────────────────
        LoginController loginController = loader.getController();
        loginController.setTaskManager(taskManager);
        loginController.setPasswords(passwords);

        // ── 7. Affichage de la fenêtre ────────────────────────────────────
        primaryStage.setTitle("STRMS");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(450);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}