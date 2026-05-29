// Engineer.java
package src;

public class Engineer extends User {

    private final String specialty;

    public Engineer(String id, String name, String email, String specialty) {
        super(id, name, email);
        this.specialty = specialty;
    }

    public String getSpecialty() { return specialty; }

    @Override public boolean canCreateTask()     { return false; }
    @Override public boolean canDeleteTask()     { return false; }
    @Override public boolean canAssignTask()     { return false; }
    @Override public boolean canGenerateReport() { return false; }
    @Override public boolean canUpdateTask()     { return false; }
}