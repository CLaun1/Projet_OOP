package src.controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import src.Admin;
import src.Engineer;
import src.Manager;
import src.TaskManager;
import src.User;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class UserManagerController {

    // ── Composants injectés depuis le FXML ────────────────────────────────
    @FXML private TableView<User>           userTable;
    @FXML private TableColumn<User, String> colId;
    @FXML private TableColumn<User, String> colName;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, Number> colTasks;
    @FXML private TableColumn<User, Void>   colActions;

    // ── Dépendances injectées ──────────────────────────────────────────────
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
    // Initialisation et Configuration des Colonnes
    // ══════════════════════════════════════════════════════════════════════

    public void initialize() {
        // 1. Liaison des données simples
        colId.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getId()));
        colName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        colEmail.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEmail()));
        
        // Extraction textuelle du rôle basée sur l'implémentation de la classe
        colRole.setCellValueFactory(cellData -> {
            User u = cellData.getValue();
            return new SimpleStringProperty(u.getRole() != null ? u.getRole().toString() : "Inconnu");
        });

        // 2. Calcul du nombre de tâches assignées via le polymorphisme AssignedUser
        colTasks.setCellValueFactory(cellData -> {
            User u = cellData.getValue();
            long taskCount = taskManager.getTasks().values().stream()
                .filter(t -> t.getAssignedUser() != null && t.getAssignedUser().getId().equals(u.getId()))
                .count();
            return new SimpleIntegerProperty((int) taskCount);
        });

        // 3. Injection des boutons d'actions (Suppression)
        setupActionsColumn();
    }

    /**
     * Charge ou rafraîchit la liste des utilisateurs dans le tableau.
     */
    public void loadUserData() {
        if (taskManager != null) {
            userTable.setItems(FXCollections.observableArrayList(taskManager.getUsers().values()));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Actions et Dialogues
    // ══════════════════════════════════════════════════════════════════════

    @FXML
    private void openAddUserDialog() {
        // Restriction de sécurité IHM (Seul l'Admin crée un compte)
        if (!(currentUser instanceof Admin)) {
            showAlert(Alert.AlertType.ERROR, "Accès refusé", "Seuls les administrateurs peuvent ajouter des utilisateurs.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Ajouter un utilisateur");
        dialog.setHeaderText("Saisissez les informations du nouveau collaborateur.");

        ButtonType btnEnregistrer = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnEnregistrer, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setStyle("-fx-padding: 20;");

        TextField nameF = new TextField();
        nameF.setPromptText("Nom complet");
        TextField emailF = new TextField();
        emailF.setPromptText("adresse@email.com");
        PasswordField passF = new PasswordField();
        passF.setPromptText("Mot de passe");
        
        // Ajout du rôle Manager dans les options de la ComboBox
        ComboBox<String> roleCombo = new ComboBox<>(FXCollections.observableArrayList("Ingénieur", "Manager", "Administrateur"));
        roleCombo.setValue("Ingénieur");
        roleCombo.setMaxWidth(Double.MAX_VALUE);

        grid.add(new Label("Nom :"), 0, 0);
        grid.add(nameF, 1, 0);
        grid.add(new Label("Email :"), 0, 1);
        grid.add(emailF, 1, 1);
        grid.add(new Label("Mot de passe :"), 0, 2);
        grid.add(passF, 1, 2);
        grid.add(new Label("Rôle :"), 0, 3);
        grid.add(roleCombo, 1, 3);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == btnEnregistrer) {
            String name = nameF.getText().trim();
            String email = emailF.getText().trim().toLowerCase();
            String password = passF.getText();
            String selectedRole = roleCombo.getValue();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Champs vides", "Tous les champs sont obligatoires.");
                return;
            }

            if (passwords.containsKey(email)) {
                showAlert(Alert.AlertType.ERROR, "Erreur de création", "Un utilisateur avec cet email existe déjà.");
                return;
            }

            String id = "U-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            
            // Remplacement du switch expression par un switch classique (rétrocompatible)
            User newUser;
            switch (selectedRole) {
                case "Administrateur":
                    newUser = new Admin(id, name, email);
                    break;
                case "Manager":
                    newUser = new Manager(id, name, email);
                    break;
                default:
                    newUser = new Engineer(id, name, email);
                    break;
            }

            try {
                taskManager.addUser(newUser, currentUser);
                passwords.put(email, password);
                
                loadUserData();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "L'utilisateur " + name + " a bien été créé.");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ajouter l'utilisateur : " + e.getMessage());
            }
        }
    }

    /**
     * Génère dynamiquement le bouton de suppression pour chaque ligne du tableau.
     */
    private void setupActionsColumn() {
        colActions.setCellFactory(param -> new TableCell<Void, User>() {
            private final Button deleteBtn = new Button("Supprimer");

            {
                deleteBtn.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-padding: 4 8;");
                deleteBtn.setOnAction(event -> {
                    User targetUser = getTableView().getItems().get(getIndex());
                    handleDeleteUser(targetUser);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox container = new HBox(deleteBtn);
                    container.setAlignment(Pos.CENTER);
                    setGraphic(container);
                }
            }
        });
    }

    private void handleDeleteUser(User targetUser) {
        if (!(currentUser instanceof Admin)) {
            showAlert(Alert.AlertType.ERROR, "Action interdite", "Seuls les administrateurs possèdent les droits de suppression.");
            return;
        }

        if (targetUser.getId().equals(currentUser.getId())) {
            showAlert(Alert.AlertType.WARNING, "Opération impossible", "Vous ne pouvez pas supprimer votre propre compte actif.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de suppression");
        confirm.setHeaderText("Supprimer l'utilisateur : " + targetUser.getName() + " ?");
        confirm.setContentText("Attention, cette action supprimera également ses assignations de tâches secondaires.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                taskManager.deleteUser(targetUser.getId(), currentUser);
                passwords.remove(targetUser.getEmail().toLowerCase());
                
                loadUserData();
                showAlert(Alert.AlertType.INFORMATION, "Utilisateur supprimé", "Le compte a été retiré avec succès.");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur de suppression", e.getMessage());
            }
        }
    }

    // ── Utilitaire d'alertes ───────────────────────────────────────────────
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}