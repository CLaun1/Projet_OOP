package src.controller;

import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import src.Task;
import src.TaskManager;
import src.User;
import src.enumeration.PriorityLevel;
import src.enumeration.TaskStatus;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportController {

    // ── Composants FXML ───────────────────────────────────────────────────
    @FXML private ToggleGroup reportTypeGroup;
    @FXML private TextArea     reportArea;

    // ── Dépendances injectées ──────────────────────────────────────────────
    private TaskManager taskManager;
    private User         currentUser;
    
    // Variable pour conserver le rapport brut sans les messages de succès/erreur d'export
    private String rawReportContent = "";

    public void setTaskManager(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Initialisation
    // ══════════════════════════════════════════════════════════════════════

    public void initialize() {
        reportArea.setText("Sélectionnez un type de rapport pour le générer.");
    }

    // ══════════════════════════════════════════════════════════════════════
    // Génération du rapport via UserData (Robuste aux changements de texte)
    // ══════════════════════════════════════════════════════════════════════

    @FXML
    private void generateReport() {
        Toggle selected = reportTypeGroup.getSelectedToggle();
        if (selected == null) return;

        // Récupération de l'identifiant technique (userData) plutôt que du texte visible
        String reportType = (selected.getUserData() != null) ? selected.getUserData().toString() : "";

        rawReportContent = switch (reportType) {
            case "STATUS"   -> buildReportByStatus();
            case "USER"     -> buildReportByUser();
            case "OVERDUE"  -> buildReportOverdue();
            case "PRIORITY" -> buildReportByPriority();
            default         -> "Type de rapport inconnu.";
        };

        reportArea.setText(rawReportContent);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Rapport 1 — Par statut
    // ══════════════════════════════════════════════════════════════════════

    private String buildReportByStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append(header("RAPPORT PAR STATUT"));

        for (TaskStatus status : TaskStatus.values()) {
            List<Task> filtered = taskManager.getTasks().values().stream()
                .filter(t -> t.getTaskStatus() == status)
                .sorted((a, b) -> a.getTitle().compareTo(b.getTitle()))
                .collect(Collectors.toList());

            sb.append(String.format("%n  %-15s : %d tâche(s)%n", status.name(), filtered.size()));
            for (Task t : filtered) {
                sb.append(taskLine(t));
            }
        }

        sb.append(footer(taskManager.getTasks().size()));
        return sb.toString();
    }

    // ══════════════════════════════════════════════════════════════════════
    // Rapport 2 — Par utilisateur (Adapté au polymorphisme User)
    // ══════════════════════════════════════════════════════════════════════

    private String buildReportByUser() {
        StringBuilder sb = new StringBuilder();
        sb.append(header("RAPPORT PAR UTILISATEUR"));

        Map<String, List<Task>> byUser = taskManager.getTasks().values().stream()
            .collect(Collectors.groupingBy(t -> {
                User u = t.getAssignedUser();
                return (u != null) ? u.getName() + " (" + u.getRole() + ")" : "Non assigné";
            }));

        byUser.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                sb.append(String.format("%n  %s (%d tâche(s))%n", entry.getKey(), entry.getValue().size()));
                entry.getValue().stream()
                    .sorted((a, b) -> a.getTitle().compareTo(b.getTitle()))
                    .forEach(t -> sb.append(taskLine(t)));
            });

        sb.append(footer(taskManager.getTasks().size()));
        return sb.toString();
    }

    // ══════════════════════════════════════════════════════════════════════
    // Rapport 3 — Tâches en retard
    // ══════════════════════════════════════════════════════════════════════

    private String buildReportOverdue() {
        StringBuilder sb = new StringBuilder();
        sb.append(header("RAPPORT — TÂCHES EN RETARD"));

        Date now = new Date();
        List<Task> overdue = taskManager.getTasks().values().stream()
            .filter(t -> t.getDeadline() != null)
            .filter(t -> t.getDeadline().before(now))
            .filter(t -> t.getTaskStatus() != TaskStatus.DONE)
            .sorted((a, b) -> a.getDeadline().compareTo(b.getDeadline()))
            .collect(Collectors.toList());

        if (overdue.isEmpty()) {
            sb.append("\n  Aucune tâche en retard.\n");
        } else {
            sb.append(String.format("%n  %d tâche(s) en retard :%n", overdue.size()));
            for (Task t : overdue) {
                sb.append(String.format("    - %-30s | Échéance : %s | Statut : %s%n",
                    t.getTitle(), t.getDeadline(), t.getTaskStatus().name()));
            }
        }

        sb.append(footer(taskManager.getTasks().size()));
        return sb.toString();
    }

    // ══════════════════════════════════════════════════════════════════════
    // Rapport 4 — Par priorité
    // ══════════════════════════════════════════════════════════════════════

    private String buildReportByPriority() {
        StringBuilder sb = new StringBuilder();
        sb.append(header("RAPPORT PAR PRIORITÉ"));

        for (PriorityLevel priority : PriorityLevel.values()) {
            List<Task> filtered = taskManager.getTasks().values().stream()
                .filter(t -> t.getPriorityLevel() == priority)
                .sorted((a, b) -> a.getTitle().compareTo(b.getTitle()))
                .collect(Collectors.toList());

            sb.append(String.format("%n  %-10s : %d tâche(s)%n", priority.name(), filtered.size()));
            for (Task t : filtered) {
                sb.append(taskLine(t));
            }
        }

        sb.append(footer(taskManager.getTasks().size()));
        return sb.toString();
    }

    // ══════════════════════════════════════════════════════════════════════
    // Export Sécurisé vers fichier .txt
    // ══════════════════════════════════════════════════════════════════════

    @FXML
    private void exportReport() {
        if (rawReportContent.isBlank()) {
            reportArea.setText("Générez d'abord un rapport valide avant d'exporter.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter le rapport");
        fileChooser.setInitialFileName("rapport_strms.txt");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers texte (*.txt)", "*.txt"));

        Stage stage = (Stage) reportArea.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(rawReportContent); 
                
                reportArea.setText(rawReportContent 
                    + "\n\n  ✔ Rapport exporté avec succès :\n  " + file.getAbsolutePath());
            } catch (IOException e) {
                reportArea.setText(rawReportContent 
                    + "\n\n  ✘ Erreur critique lors de l'export : " + e.getMessage());
            }
        }
    }

    // ── Helpers de formatage ───────────────────────────────────────────────

    private String header(String title) {
        return "  ============================================\n" +
               "   " + title + "\n" +
               "   Généré le : " + new Date() + "\n" +
               "   Par       : " + (currentUser != null ? currentUser.getName() : "Inconnu") + "\n" +
               "  ============================================\n";
    }

    private String footer(int total) {
        return "\n  ────────────────────────────────────────────\n" +
               "   Total tâches dans le système : " + total + "\n" +
               "  ============================================\n";
    }

    private String taskLine(Task t) {
        User assignedUser = t.getAssignedUser();
        String assignee = (assignedUser != null) ? assignedUser.getName() + " (" + assignedUser.getRole() + ")" : "Non assigné";
        return String.format("    - %-30s | %-10s | %-12s | %s%n",
            t.getTitle(), t.getPriorityLevel().name(), t.getTaskStatus().name(), assignee);
    }
}