// src/main/java/upeu/edu/pe/shared/context/AuditContext.java
package unmsm.edu.pe.shared.context;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class AuditContext {

    private String currentUser;

    public String getCurrentUser() {
        return currentUser != null ? currentUser : "system";
    }

    public void setCurrentUser(String currentUser) {
        this.currentUser = currentUser;
    }

    public void clear() {
        this.currentUser = null;
    }
}