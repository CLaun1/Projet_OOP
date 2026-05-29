package src;

public class Admin extends User {

    private final int accessLevel;

    /** Constructeur sans accessLevel — accessLevel par défaut = 1 */
    public Admin(String id, String name, String email) {
        super(id, name, email);
        this.accessLevel = 1;
    }

    /** Constructeur avec accessLevel — utilisé dans Main et les tests */
    public Admin(String id, String name, String email, int accessLevel) {
        super(id, name, email);
        this.accessLevel = accessLevel;
    }

    public int getAccessLevel() { return accessLevel; }

    @Override public boolean canCreateTask()     { return true; }
    @Override public boolean canDeleteTask()     { return true; }
    @Override public boolean canAssignTask()     { return true; }
    @Override public boolean canGenerateReport() { return true; }
    @Override public boolean canUpdateTask()     { return true; }
}