package src.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.beans.property.SimpleStringProperty;

import src.Admin;
import src.Engineer;
import src.Manager;
import src.Task;
import src.TaskHistoryEntry;
import src.TaskManager;
import src.User;
import src.enumeration.TaskStatus;
import src.exceptions.InvalidRoleException;
import src.exceptions.InvalidTaskStateException;
import src.exceptions.TaskNotFoundException;

import java.io.IOException;
import java.util.Map;

public class TaskDetailController {

    // ── Composants FXML — En-tête ──────────────────────────────────────────
    @FXML private Label  taskTitleLabel;
    @FXML private Button assignBtn;
    @FXML private Button startBtn;
    @FXML private Button completeBtn;
    @FXML private Button deleteBtn;

    // ── Composants FXML — Panneau gauche ──────────────────────────────────
    @FXML private Label    statusLabel;
    @FXML private Label    priorityLabel;
    @FXML private Label    categoryLabel;
    @FXML private Label    assigneeLabel;
    @FXML private Label    deadlineLabel;
    @FXML private TextArea descArea;
    @FXML private ListView<String> depsListView;

    // ── Composants FXML — Panneau droit (historique) ───────────────────────
    @FXML private TableView<TaskHistoryEntry>           historyTable;
    @FXML private TableColumn<TaskHistoryEntry, String> colTimestamp;
    @FXML private TableColumn<TaskHistoryEntry, String> colUser;
    @FXML private TableColumn<TaskHistoryEntry, String> colAction;

    // ── Dépendances injectées ──────────────────────────────────────────────
    private TaskManager         taskManager;
    private User                currentUser;
    private Map<String, String> passwords;
    private Task                task; // tâche actuellement affichée

    // ── Setters d'injection ───────────────────────────────────────────────
    public void setTaskManager(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public void setPasswords(Map<String, String> passwords) {
        this.passwords = passwords;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Initialisation manuelle (après injection)
    // ══════════════════════════════════════════════════════════════════════

    public void initialize() {
        if (task == null) return;

        fillTaskInfo();
        fillDependencies();
        fillHistory();
        configureButtonsForRole();
    }

    // ══════════════════════════════════════════════════════════════════════
    // Remplissage des informations de la tâche
    // ══════════════════════════════════════════════════════════════════════

    private void fillTaskInfo() {
        taskTitleLabel.setText(task.getTitle());
        descArea.setText(task.getDescription());

        // Statut avec couleur
        String status = task.getTaskStatus().name().replace("_", " ");
        statusLabel.setText(status);
        statusLabel.setStyle(switch (task.getTaskStatus().name()) {
            case "TODO"        -> "-fx-text-fill: #888888; -fx-font-weight: bold;";
            case "IN_PROGRESS" -> "-fx-text-fill: #185FA5; -fx-font-weight: bold;";
            case "BLOCKED"     -> "-fx-text-fill: #BA7517; -fx-font-weight: bold;";
            case "DONE"        -> "-fx-text-fill: #3B6D11; -fx-font-weight: bold;";
            default            -> "";
        });

        // Priorité avec couleur
        priorityLabel.setText(task.getPriorityLevel().name());
        priorityLabel.setStyle(switch (task.getPriorityLevel().name()) {
            case "CRITICAL" -> "-fx-text-fill: #A32D2D; -fx-font-weight: bold;";
            case "HIGH"     -> "-fx-text-fill: #BA7517; -fx-font-weight: bold;";
            case "MEDIUM"   -> "-fx-text-fill: #185FA5;";
            default         -> "-fx-text-fill: #888888;";
        });

        categoryLabel.setText(task.getTaskCategory().name());

        assigneeLabel.setText(
            task.getAssignedEngineer() != null
                ? task.getAssignedEngineer().getName()
                : "Non assigné"
        );

        deadlineLabel.setText(
            task.getDeadline() != null
                ? task.getDeadline().toString()
                : "Aucune échéance"
        );
    }

    // ── Dépendances ────────────────────────────────────────────────────────

    private void fillDependencies() {
        ObservableList<String> deps = FXCollections.observableArrayList();

        if (task.getDependencies().isEmpty()) {
            deps.add("Aucune dépendance");
        } else {
            for (Task dep : task.getDependencies()) {
                String line = dep.getId() + " — " + dep.getTitle()
                    + "  [" + dep.getTaskStatus().name().replace("_", " ") + "]";
                deps.add(line);
            }
        }

        depsListView.setItems(deps);

        // Colorer les lignes selon le statut de la dépendance
        depsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("DONE")) {
                        setStyle("-fx-text-fill: #3B6D11;");
                    } else if (item.contains("BLOCKED")) {
                        setStyle("-fx-text-fill: #BA7517;");
                    } else if (item.contains("IN PROGRESS")) {
                        setStyle("-fx-text-fill: #185FA5;");
                    } else {
                        setStyle("-fx-text-fill: #888888;");
                    }
                }
            }
        });
    }

    // ── Historique ─────────────────────────────────────────────────────────

    private void fillHistory() {
        colTimestamp.setCellValueFactory(data ->
            new SimpleStringProperty(
                data.getValue().getTimestamp() != null
                    ? data.getValue().getTimestamp().toString()
                    : ""
            ));

        colUser.setCellValueFactory(data ->
            new SimpleStringProperty(
                data.getValue().getPerformedBy() != null
                    ? data.getValue().getPerformedBy().getName()
                    : "Système"
            ));

        colAction.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getAction()));

        ObservableList<TaskHistoryEntry> history =
            FXCollections.observableArrayList(task.getHistory());
        historyTable.setItems(history);
        historyTable.setPlaceholder(new Label("Aucune action enregistrée."));
    }

    // ══════════════════════════════════════════════════════════════════════
    // Boutons contextuels selon le rôle et l'état de la tâche
    // ══════════════════════════════════════════════════════════════════════

    private void configureButtonsForRole() {
        boolean isAdmin    = currentUser instanceof Admin;
        boolean isManager  = currentUser instanceof Manager;
        boolean isEngineer = currentUser instanceof Engineer;
        boolean isDone     = task.getTaskStatus() == TaskStatus.DONE;
        boolean isAssignedEngineer = isEngineer
            && task.getAssignedEngineer() != null
            && task.getAssignedEngineer().getId().equals(currentUser.getId());

        // Assigner : Manager ou Admin, tâche non DONE
        assignBtn.setVisible((isAdmin || isManager) && !isDone);
        assignBtn.setManaged((isAdmin || isManager) && !isDone);

        // Démarrer : ingénieur assigné, tâche TODO ou BLOCKED
        boolean canStart = isAssignedEngineer
            && (task.getTaskStatus() == TaskStatus.TODO
            ||  task.getTaskStatus() == TaskStatus.BLOCKED);
        startBtn.setVisible(canStart);
        startBtn.setManaged(canStart);

        // Terminer : ingénieur assigné, tâche IN_PROGRESS
        boolean canComplete = isAssignedEngineer
            && task.getTaskStatus() == TaskStatus.IN_PROGRESS;
        completeBtn.setVisible(canComplete);
        completeBtn.setManaged(canComplete);

        // Supprimer : Admin uniquement
        deleteBtn.setVisible(isAdmin);
        deleteBtn.setManaged(isAdmin);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Actions des boutons
    // ══════════════════════════════════════════════════════════════════════

    @FXML
    private void handleBack() {
        navigateTo("DashboardView.fxml", true);
    }

    @FXML
    private void handleAssign() {
        // Ouvrir un dialog pour choisir un ingénieur
        ChoiceDialog<String> dialog = new ChoiceDialog<>();
        dialog.setTitle("Assigner la tâche");
        dialog.setHeaderText("Choisir un ingénieur pour : " + task.getTitle());
        dialog.setContentText("Ingénieur :");

        // Remplir avec les ingénieurs disponibles
        taskManager.getUsers().values().stream()
            .filter(u -> u instanceof Engineer)
            .map(User::getName)
            .forEach(name -> dialog.getItems().add(name));

        dialog.showAndWait().ifPresent(chosenName -> {
            Engineer engineer = (Engineer) taskManager.getUsers().values().stream()
                .filter(u -> u instanceof Engineer && u.getName().equals(chosenName))
                .findFirst().orElse(null);

            if (engineer != null) {
                try {
                    taskManager.assignTask(task.getId(), engineer, currentUser);
                    refresh();
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleStart() {
        try {
            taskManager.updateTask(task.getId(), TaskStatus.IN_PROGRESS, currentUser);
            refresh();
        } catch (InvalidTaskStateException | TaskNotFoundException
               | InvalidRoleException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    private void handleComplete() {
        try {
            taskManager.completeTask(task.getId(), currentUser);
            refresh();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer la tâche \"" + task.getTitle() + "\" ?",
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    taskManager.deleteTask(task.getId(), currentUser);
                    navigateTo("DashboardView.fxml", true);
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
                }
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // Rafraîchissement après action
    // ══════════════════════════════════════════════════════════════════════

    private void refresh() {
        // Recharger la tâche depuis le TaskManager pour avoir l'état à jour
        try {
            this.task = taskManager.findTask(task.getId());
            fillTaskInfo();
            fillDependencies();
            fillHistory();
            configureButtonsForRole();
        } catch (TaskNotFoundException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Navigation
    // ══════════════════════════════════════════════════════════════════════

    private void navigateTo(String fxmlFile, boolean isDashboard) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("../view/" + fxmlFile));

            if (isDashboard) {
                DashboardController dc = new DashboardController();
                dc.setTaskManager(taskManager);
                dc.setCurrentUser(currentUser);
                dc.setPasswords(passwords);
                loader.setController(dc);
            }

            Parent root = loader.load();

            Stage stage = (Stage) taskTitleLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur de navigation", e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type, content, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
