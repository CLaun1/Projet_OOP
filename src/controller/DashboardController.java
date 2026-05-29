package src.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DashboardController {

    // ── Conteneur Principal (Nécessaire pour la navigation fluide) ──────────
    @FXML private BorderPane mainBorderPane;

    // ── Sidebar ────────────────────────────────────────────────────────────
    @FXML private Button usersBtn;
    @FXML private Label  currentUserLabel;

    // ── Cartes statistiques ────────────────────────────────────────────────
    @FXML private VBox cardTodo;
    @FXML private VBox cardInProgress;
    @FXML private VBox cardBlocked;
    @FXML private VBox cardDone;

    // ── Tableau tâches urgentes ────────────────────────────────────────────
    @FXML private TableView<Task>        urgentTasksTable;
    @FXML private TableColumn<Task, String> colTitle;
    @FXML private TableColumn<Task, String> colPriority;
    @FXML private TableColumn<Task, String> colStatus;
    @FXML private TableColumn<Task, String> colAssignee;

    // ── Données injectées ──────────────────────────────────────────────────
    private TaskManager         taskManager;
    private User                currentUser;
    private Map<String, String> passwords;

    public void setTaskManager(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public void setPasswords(Map<String, String> passwords) {
        this.passwords = passwords;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Initialisation
    // ══════════════════════════════════════════════════════════════════════
    
    public void initialize() {
        configureRoleAccess();
        configureTableColumns();
        refreshDashboard();
    }

    // ── Contrôle d'accès selon le rôle ────────────────────────────────────
    private void configureRoleAccess() {
        if (this.currentUser != null) {
            boolean isAdmin   = currentUser instanceof Admin;
            boolean isManager = currentUser instanceof Manager;

            // Masquer et libérer l'espace si l'utilisateur n'est pas Admin
            usersBtn.setVisible(isAdmin);
            usersBtn.setManaged(isAdmin);

            String role = isAdmin   ? "Admin"
                        : isManager ? "Manager"
                        : "Engineer";
            currentUserLabel.setText(currentUser.getName() + " — " + role);
        } else {
            System.out.println("Avertissement : Aucun utilisateur connecté.");
            currentUserLabel.setText("Mode Invité");
        }
    }

    // ── Configuration du TableView ────────────────────────────────────────
    private void configureTableColumns() {
        colTitle.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getTitle()));

        // Colonne Priorité
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

        // Colonne Statut
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

        // Colonne Assigné — Adaptée au polymorphisme User (Manager / Engineer)
        colAssignee.setCellValueFactory(data -> {
            User assignedUser = data.getValue().getAssignedUser();
            if (assignedUser != null) {
                String displayName = assignedUser.getName() + " (" + assignedUser.getRole() + ")";
                return new SimpleStringProperty(displayName);
            }
            return new SimpleStringProperty("Non assigné");
        });

        urgentTasksTable.setPlaceholder(
            new Label("Aucune tâche critique ou haute priorité en attente."));
    }

    // ── Rafraîchissement des données ──────────────────────────────────────
    public void refreshDashboard() {
        if (taskManager != null) {
            refreshStatCards();
            refreshUrgentTasksTable();
        }
    }

    private void refreshStatCards() {
        List<Task> allTasks = new ArrayList<>(taskManager.getTasks().values());

        long countTodo       = allTasks.stream().filter(t -> t.getTaskStatus() == TaskStatus.TODO).count();
        long countInProgress = allTasks.stream().filter(t -> t.getTaskStatus() == TaskStatus.IN_PROGRESS).count();
        long countBlocked    = allTasks.stream().filter(t -> t.getTaskStatus() == TaskStatus.BLOCKED).count();
        long countDone       = allTasks.stream().filter(t -> t.getTaskStatus() == TaskStatus.DONE).count();

        buildCard(cardTodo,       "TODO",        countTodo,       "#888888");
        buildCard(cardInProgress, "IN PROGRESS", countInProgress, "#185FA5");
        buildCard(cardBlocked,    "BLOCKED",     countBlocked,    "#BA7517");
        buildCard(cardDone,       "DONE",        countDone,       "#3B6D11");
    }

    private void buildCard(VBox card, String label, long count, String color) {
        card.getChildren().clear();
        card.setAlignment(Pos.CENTER);
        card.setSpacing(6);
        card.setPrefWidth(140);
        card.setPrefHeight(80);
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-border-color: " + color + ";" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 4, 0, 0, 2);"
        );

        Label countLabel = new Label(String.valueOf(count));
        countLabel.setStyle("-fx-font-size: 28; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        Label nameLabel = new Label(label);
        nameLabel.setStyle("-fx-font-size: 11; -fx-text-fill: " + color + ";");

        card.getChildren().addAll(countLabel, nameLabel);
    }

    private void refreshUrgentTasksTable() {
        List<Task> urgentTasks = new ArrayList<>(taskManager.getTasks().values())
            .stream()
            .filter(t -> t.getTaskStatus() != TaskStatus.DONE)
            .filter(t -> t.getPriorityLevel() == PriorityLevel.CRITICAL || t.getPriorityLevel() == PriorityLevel.HIGH)
            .sorted((a, b) -> {
                // Utilise la méthode compareTo de la classe Task pour respecter l'ordre naturel corrigé
                int cmp = a.compareTo(b);
                return cmp != 0 ? cmp : a.getTitle().compareTo(b.getTitle());
            })
            .collect(Collectors.toList());

        ObservableList<Task> data = FXCollections.observableArrayList(urgentTasks);
        urgentTasksTable.setItems(data);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Navigation (Actions Sidebar)
    // ══════════════════════════════════════════════════════════════════════

    @FXML
    private void showDashboard() {
        mainBorderPane.setCenter(urgentTasksTable.getParent()); 
        refreshDashboard();
    }

    @FXML
    private void showTasks() {
        navigateTo("TaskListView.fxml");
    }

    @FXML
    private void showUsers() {
        if (!(currentUser instanceof Admin)) {
            showAlert(Alert.AlertType.WARNING, "Accès refusé", 
                "Seuls les Admins peuvent accéder à la gestion des utilisateurs.");
            return;
        }
        navigateTo("UserManagerView.fxml");
    }

    @FXML
    private void showReports() {
        navigateTo("ReportView.fxml");
    }

    @FXML
    private void showNotifications() {
        navigateTo("NotificationView.fxml");
    }

    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, 
            "Voulez-vous vraiment vous déconnecter ?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Déconnexion");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                navigateToLogin();
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // Moteur de Navigation Interne (Conservation de la Sidebar)
    // ══════════════════════════════════════════════════════════════════════

    private void navigateTo(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../view/" + fxmlFile));

            Object ctrl = buildController(fxmlFile);
            if (ctrl != null) {
                loader.setController(ctrl);
            }

            Parent root = loader.load();

            if (ctrl != null) {
                injectDependencies(ctrl);
            }

            mainBorderPane.setCenter(root);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur de navigation", 
                "Impossible de charger : " + fxmlFile + "\n" + e.getMessage());
        }
    }

    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../view/LoginView.fxml"));
            LoginController lc = new LoginController();
            lc.setTaskManager(taskManager);
            lc.setPasswords(passwords);
            loader.setController(lc);

            Parent root = loader.load();
            Stage stage = (Stage) mainBorderPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger l'écran de connexion.");
        }
    }

    private Object buildController(String fxmlFile) {
        return switch (fxmlFile) {
            case "LoginView.fxml"           -> new LoginController();
            case "ReportView.fxml"          -> new ReportController();
            case "DependencyGraphView.fxml" -> new DependencyGraphController();
            case "UserManagerView.fxml"     -> null; 
            default                         -> null;
        };
    }

    private void injectDependencies(Object ctrl) {
        try {
            try {
                ctrl.getClass().getMethod("setTaskManager", TaskManager.class).invoke(ctrl, taskManager);
            } catch (NoSuchMethodException ignored) {}

            try {
                ctrl.getClass().getMethod("setCurrentUser", User.class).invoke(ctrl, currentUser);
            } catch (NoSuchMethodException ignored) {}

            try {
                ctrl.getClass().getMethod("setPasswords", Map.class).invoke(ctrl, passwords);
            } catch (NoSuchMethodException ignored) {}

            try {
                ctrl.getClass().getMethod("initialize").invoke(ctrl);
            } catch (NoSuchMethodException ignored) {}

        } catch (Exception e) {
            System.err.println("[DashboardController] Échec de l'injection sur le contrôleur cible : " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type, content, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}