package src.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.stage.Stage;

import src.Admin;
import src.Engineer;
import src.Manager;
import src.Task;
import src.TaskManager;
import src.User;
import src.enumeration.PriorityLevel;
import src.enumeration.TaskStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DashboardController {

    // ── Sidebar ────────────────────────────────────────────────────────────
    @FXML private Button usersBtn;
    @FXML private Label  currentUserLabel;

    // ── Cartes statistiques ────────────────────────────────────────────────
    @FXML private VBox cardTodo;
    @FXML private VBox cardInProgress;
    @FXML private VBox cardBlocked;
    @FXML private VBox cardDone;

    // ── Tableau tâches urgentes ────────────────────────────────────────────
    @FXML private TableView<Task>           urgentTasksTable;
    @FXML private TableColumn<Task, String> colTitle;
    @FXML private TableColumn<Task, String> colPriority;
    @FXML private TableColumn<Task, String> colStatus;
    @FXML private TableColumn<Task, String> colAssignee;

    // ── Dépendances injectées depuis LoginController ───────────────────────
    private TaskManager taskManager;
    private User        currentUser;

    // ══════════════════════════════════════════════════════════════════════
    // Injection (appelée depuis LoginController avant initialize)
    // ══════════════════════════════════════════════════════════════════════

    public void setTaskManager(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Initialisation manuelle
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Appelée manuellement depuis LoginController après injection,
     * car @FXML initialize() s'exécute avant setCurrentUser().
     */
    public void initialize() {
        configureRoleAccess();
        configureTableColumns();
        refreshDashboard();
    }

    // ── Contrôle d'accès selon le rôle ────────────────────────────────────

    private void configureRoleAccess() {
        // Ton TaskManager utilise Admin et Manager comme sous-classes de User
        boolean isAdmin   = currentUser instanceof Admin;
        boolean isManager = currentUser instanceof Manager;

        // Bouton "Utilisateurs" : Admin seulement
        usersBtn.setVisible(isAdmin);
        usersBtn.setManaged(isAdmin); // libère l'espace dans le VBox si masqué

        // Label bas de sidebar : nom + rôle
        String role = isAdmin   ? "Admin"
                    : isManager ? "Manager"
                    : "Engineer";
        currentUserLabel.setText(currentUser.getName() + " — " + role);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Configuration du TableView
    // ══════════════════════════════════════════════════════════════════════

    private void configureTableColumns() {

        // ── Colonne Titre ─────────────────────────────────────────────────
        colTitle.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getTitle()));

        // ── Colonne Priorité ──────────────────────────────────────────────
        // Utilise getPriorityLevel() de ton Task
        colPriority.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getPriorityLevel().name()));

        colPriority.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String priority, boolean empty) {
                super.updateItem(priority, empty);
                if (empty || priority == null) {
                    setText(null); setStyle("");
                } else {
                    setText(priority);
                    setStyle(switch (priority) {
                        case "CRITICAL" -> "-fx-text-fill:#A32D2D; -fx-font-weight:bold;";
                        case "HIGH"     -> "-fx-text-fill:#BA7517; -fx-font-weight:bold;";
                        case "MEDIUM"   -> "-fx-text-fill:#185FA5;";
                        default         -> "-fx-text-fill:#888888;";
                    });
                }
            }
        });

        // ── Colonne Statut ────────────────────────────────────────────────
        // Utilise getTaskStatus() de ton Task
        colStatus.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getTaskStatus().name()));

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null); setStyle("");
                } else {
                    setText(status.replace("_", " "));
                    setStyle(switch (status) {
                        case "TODO"        -> "-fx-text-fill:#888888; -fx-font-weight:bold;";
                        case "IN_PROGRESS" -> "-fx-text-fill:#185FA5; -fx-font-weight:bold;";
                        case "BLOCKED"     -> "-fx-text-fill:#BA7517; -fx-font-weight:bold;";
                        case "DONE"        -> "-fx-text-fill:#3B6D11; -fx-font-weight:bold;";
                        default            -> "";
                    });
                }
            }
        });

        // ── Colonne Assigné ───────────────────────────────────────────────
        // Utilise getAssignedEngineer() de ton Task
        colAssignee.setCellValueFactory(data -> {
            Engineer engineer = data.getValue().getAssignedEngineer();
            String name = (engineer != null) ? engineer.getName() : "Non assigné";
            return new SimpleStringProperty(name);
        });

        // Message placeholder si la table est vide
        urgentTasksTable.setPlaceholder(
            new Label("Aucune tâche critique ou haute priorité en attente."));
    }

    // ══════════════════════════════════════════════════════════════════════
    // Rafraîchissement du tableau de bord
    // ══════════════════════════════════════════════════════════════════════

    public void refreshDashboard() {
        refreshStatCards();
        refreshUrgentTasksTable();
    }

    // ── Cartes statistiques ────────────────────────────────────────────────

    private void refreshStatCards() {
        // Utilise getTasks() qui retourne la HashMap<String, Task> de ton TaskManager
        List<Task> allTasks = new ArrayList<>(taskManager.getTasks().values());

        long countTodo       = allTasks.stream()
            .filter(t -> t.getTaskStatus() == TaskStatus.TODO).count();
        long countInProgress = allTasks.stream()
            .filter(t -> t.getTaskStatus() == TaskStatus.IN_PROGRESS).count();
        long countBlocked    = allTasks.stream()
            .filter(t -> t.getTaskStatus() == TaskStatus.BLOCKED).count();
        long countDone       = allTasks.stream()
            .filter(t -> t.getTaskStatus() == TaskStatus.DONE).count();

        buildCard(cardTodo,       "TODO",        countTodo,       "#888888");
        buildCard(cardInProgress, "IN PROGRESS", countInProgress, "#185FA5");
        buildCard(cardBlocked,    "BLOCKED",     countBlocked,    "#BA7517");
        buildCard(cardDone,       "DONE",        countDone,       "#3B6D11");
    }

    /**
     * Vide le VBox de la carte et le reconstruit avec un grand chiffre
     * coloré + un libellé de statut.
     */
    private void buildCard(VBox card, String label, long count, String color) {
        card.getChildren().clear();
        card.setAlignment(Pos.CENTER);
        card.setSpacing(6);
        card.setPrefWidth(140);
        card.setPrefHeight(80);
        card.setStyle(
            "-fx-background-color: white;"          +
            "-fx-border-color: "   + color + ";"    +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 6;"+
            "-fx-background-radius: 6;"             +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 4, 0, 0, 2);"
        );

        Label countLabel = new Label(String.valueOf(count));
        countLabel.setStyle(
            "-fx-font-size: 28; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        Label nameLabel = new Label(label);
        nameLabel.setStyle(
            "-fx-font-size: 11; -fx-text-fill: " + color + ";");

        card.getChildren().addAll(countLabel, nameLabel);
    }

    // ── Tableau tâches urgentes ────────────────────────────────────────────

    private void refreshUrgentTasksTable() {
        // Utilise getTasks() — HashMap disponible dans ton TaskManager
        List<Task> urgentTasks = new ArrayList<>(taskManager.getTasks().values())
            .stream()
            .filter(t -> t.getTaskStatus() != TaskStatus.DONE)
            .filter(t -> t.getPriorityLevel() == PriorityLevel.CRITICAL
                      || t.getPriorityLevel() == PriorityLevel.HIGH)
            .sorted((a, b) -> {
                // CRITICAL avant HIGH, puis ordre alphabétique sur le titre
                int cmp = b.getPriorityLevel().compareTo(a.getPriorityLevel());
                return cmp != 0 ? cmp : a.getTitle().compareTo(b.getTitle());
            })
            .collect(Collectors.toList());

        ObservableList<Task> data = FXCollections.observableArrayList(urgentTasks);
        urgentTasksTable.setItems(data);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Navigation — Boutons sidebar (onAction dans le FXML)
    // ══════════════════════════════════════════════════════════════════════

    @FXML
    private void showDashboard() {
        refreshDashboard(); // déjà sur le dashboard : on rafraîchit
    }

    @FXML
    private void showTasks() {
        navigateTo("/view/TaskListView.fxml");
    }

    @FXML
    private void showUsers() {
        // Double vérification côté contrôleur
        if (!(currentUser instanceof Admin)) {
            showAlert(Alert.AlertType.WARNING,
                "Accès refusé",
                "Seuls les Admins peuvent accéder à la gestion des utilisateurs.");
            return;
        }
        navigateTo("/view/UserManagerView.fxml");
    }

    @FXML
    private void showReports() {
        navigateTo("/view/ReportView.fxml");
    }

    @FXML
    private void showNotifications() {
        navigateTo("/view/NotificationView.fxml");
    }

    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Voulez-vous vraiment vous déconnecter ?",
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Déconnexion");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                navigateTo("/view/LoginView.fxml");
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // Utilitaire de navigation
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Charge un fichier FXML, injecte TaskManager + User dans le
     * contrôleur cible (s'il expose ces setters), puis remplace la scène.
     */
    private void navigateTo(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // Injection dans le contrôleur cible par réflexion
            Object ctrl = loader.getController();
            injectDependencies(ctrl);

            Stage stage = (Stage) urgentTasksTable.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR,
                "Erreur de navigation",
                "Impossible de charger : " + fxmlPath + "\n" + e.getMessage());
        }
    }

    /**
     * Injecte TaskManager, User et appelle initialize() sur le
     * contrôleur cible s'il expose ces méthodes.
     */
    private void injectDependencies(Object ctrl) {
        try {
            try {
                ctrl.getClass()
                    .getMethod("setTaskManager", TaskManager.class)
                    .invoke(ctrl, taskManager);
            } catch (NoSuchMethodException ignored) {}

            try {
                ctrl.getClass()
                    .getMethod("setCurrentUser", User.class)
                    .invoke(ctrl, currentUser);
            } catch (NoSuchMethodException ignored) {}

            try {
                ctrl.getClass()
                    .getMethod("initialize")
                    .invoke(ctrl);
            } catch (NoSuchMethodException ignored) {}

        } catch (Exception e) {
            System.err.println("[DashboardController] Injection échouée : "
                + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type, content, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
