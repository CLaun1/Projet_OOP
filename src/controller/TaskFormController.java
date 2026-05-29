package src.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

import src.Task;
import src.TaskManager;
import src.User;
import src.enumeration.PriorityLevel;
import src.enumeration.TaskCategory;
import src.enumeration.TaskStatus;
import src.exceptions.CircularDependencyException;

import java.io.IOException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class TaskFormController {

    // ── Composants FXML ───────────────────────────────────────────────────
    @FXML private Label      formTitle;
    @FXML private TextField  titleField;
    @FXML private TextArea   descField;
    @FXML private ComboBox<PriorityLevel>  priorityCombo;
    @FXML private ComboBox<TaskCategory>   categoryCombo;
    @FXML private DatePicker deadlinePicker;
    @FXML private ListView<String>         dependenciesList;
    @FXML private Label      errorLabel;

    // ── Dépendances injectées ──────────────────────────────────────────────
    private TaskManager         taskManager;
    private User                currentUser;
    private Map<String, String> passwords;

    // ── Mode édition ───────────────────────────────────────────────────────
    private Task taskToEdit = null;

    // ── Setters ───────────────────────────────────────────────────────────
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
        this.taskToEdit = task;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Initialisation
    // ══════════════════════════════════════════════════════════════════════

    public void initialize() {
        populateCombos();
        populateDependenciesList();

        dependenciesList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        if (taskToEdit != null) {
            formTitle.setText("Modifier la tâche");
            titleField.setText(taskToEdit.getTitle());
            descField.setText(taskToEdit.getDescription());
            priorityCombo.setValue(taskToEdit.getPriorityLevel());
            categoryCombo.setValue(taskToEdit.getTaskCategory());

            if (taskToEdit.getDeadline() != null) {
                deadlinePicker.setValue(taskToEdit.getDeadline()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate());
            }

            // Sélection robuste des dépendances actuelles
            List<String> currentDepIds = taskToEdit.getDependencies().stream()
                .map(Task::getId)
                .collect(Collectors.toList());

            dependenciesList.getItems().forEach(item -> {
                String id = item.substring(0, item.indexOf(" |")).trim();
                if (currentDepIds.contains(id)) {
                    dependenciesList.getSelectionModel().select(item);
                }
            });
        } else {
            formTitle.setText("Nouvelle tâche");
        }
    }

    private void populateCombos() {
        priorityCombo.setItems(FXCollections.observableArrayList(PriorityLevel.values()));
        categoryCombo.setItems(FXCollections.observableArrayList(TaskCategory.values()));
    }

    private void populateDependenciesList() {
        List<String> items = taskManager.getTasks().values().stream()
            .filter(t -> taskToEdit == null || !t.getId().equals(taskToEdit.getId()))
            .map(t -> t.getId() + " | " + t.getTitle() + " [" + t.getTaskStatus().name().replace("_", " ") + "]")
            .sorted()
            .collect(Collectors.toList());

        dependenciesList.setItems(FXCollections.observableArrayList(items));
    }

    // ══════════════════════════════════════════════════════════════════════
    // Sauvegarde et Traitements Métier
    // ══════════════════════════════════════════════════════════════════════

    @FXML
    private void handleSave() {
        errorLabel.setVisible(false);
        String title = titleField.getText().trim();

        if (title.isEmpty()) {
            showError("Le titre est obligatoire.");
            return;
        }
        if (priorityCombo.getValue() == null) {
            showError("Veuillez sélectionner une priorité.");
            return;
        }
        if (categoryCombo.getValue() == null) {
            showError("Veuillez sélectionner une catégorie.");
            return;
        }

        Date deadline = null;
        if (deadlinePicker.getValue() != null) {
            deadline = Date.from(deadlinePicker.getValue()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant());
        }

        if (taskToEdit == null) {
            handleCreate(title, deadline);
        } else {
            handleUpdate(title, deadline);
        }
    }

    private void handleCreate(String title, Date deadline) {
        String id = "T-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Task newTask = new Task(
            id, title, descField.getText().trim(),
            priorityCombo.getValue(), TaskStatus.TODO,
            categoryCombo.getValue(), deadline
        );

        try {
            taskManager.addTask(newTask, currentUser);
            
            // Tentative d'application transactionnelle des dépendances
            boolean success = applyDependencies(newTask);
            if (!success) {
                // Nettoyage complet en cas de problème circulaire
                taskManager.deleteTask(newTask.getId(), currentUser);
                return; 
            }
            navigateToDashboard();
        } catch (Exception e) {
            showError("Erreur lors de la création : " + e.getMessage());
        }
    }

    private void handleUpdate(String title, Date deadline) {
        // Sauvegarde préventive complète de l'état initial des dépendances
        List<Task> initialDeps = new ArrayList<>(taskToEdit.getDependencies());

        try {
            // Étape 1 : Nettoyage complet des anciennes dépendances pour repartir sur une base saine
            List<Task> oldDeps = List.copyOf(taskToEdit.getDependencies());
            for (Task dep : oldDeps) {
                taskManager.removeDependency(taskToEdit.getId(), dep.getId(), currentUser);
            }

            // Étape 2 : Application de la nouvelle configuration sélectionnée
            boolean success = applyDependencies(taskToEdit);
            if (!success) {
                // ÉCHEC TECHNIQUE : Rollback complet vers l'état d'origine exact
                // 1. On nettoie tout ce qui a pu être injecté partiellement lors du traitement défaillant
                List<Task> partialDeps = List.copyOf(taskToEdit.getDependencies());
                for (Task partial : partialDeps) {
                    taskManager.removeDependency(taskToEdit.getId(), partial.getId(), currentUser);
                }
                // 2. On restaure les dépendances initiales sauvegardées
                for (Task initialDep : initialDeps) {
                    taskManager.addDependency(taskToEdit.getId(), initialDep.getId(), currentUser);
                }
                return; 
            }

            // Étape 3 : Application des informations textuelles
            taskToEdit.setTitle(title);
            taskToEdit.updateDescription(descField.getText().trim());
            taskToEdit.changePriority(priorityCombo.getValue());
            taskToEdit.setDeadline(deadline);

            taskToEdit.addHistoryEntry(new src.TaskHistoryEntry(
                "Tâche modifiée par " + currentUser.getName(), currentUser));

            navigateToDashboard();

        } catch (Exception e) {
            showError("Erreur lors de la modification : " + e.getMessage());
        }
    }

    /**
     * Tente d'associer séquentiellement les dépendances sélectionnées.
     * En cas de cycle, la boucle est immédiatement interrompue.
     */
    private boolean applyDependencies(Task task) {
        for (String selected : dependenciesList.getSelectionModel().getSelectedItems()) {
            String depId = selected.substring(0, selected.indexOf(" |")).trim();
            try {
                taskManager.addDependency(task.getId(), depId, currentUser);
            } catch (CircularDependencyException e) {
                showError("Dépendance circulaire bloquante détectée avec la tâche : " + depId);
                return false;
            } catch (Exception e) {
                showError("Erreur d'affectation de dépendance : " + e.getMessage());
                return false;
            }
        }
        return true;
    }

    @FXML
    private void handleCancel() {
        navigateToDashboard();
    }

    // ══════════════════════════════════════════════════════════════════════
    // Navigation Sécurisée par lookup ID
    // ══════════════════════════════════════════════════════════════════════

    private void navigateToDashboard() {
        try {
            // Recherche universelle du conteneur racine de l'application par ID CSS/FXML
            BorderPane mainBorderPane = (BorderPane) titleField.getScene().lookup("#mainBorderPane");
            
            if (mainBorderPane == null) {
                throw new IOException("Conteneur pivot '#mainBorderPane' introuvable dans la scène courante.");
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("../view/DashboardView.fxml"));
            Parent dashboardView = loader.load();
            
            DashboardController dc = loader.getController();
            dc.setTaskManager(taskManager);
            dc.setCurrentUser(currentUser);
            dc.setPasswords(passwords);
            dc.initialize(); // Forcer la mise à jour des statistiques dynamiques

            // Ré-injection propre au centre du Layout principal
            mainBorderPane.setCenter(dashboardView);

        } catch (IOException e) {
            showError("Erreur critique d'arborescence UI : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}