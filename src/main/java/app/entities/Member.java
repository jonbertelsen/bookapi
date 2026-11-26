package app.entities;

import jakarta.persistence.*;
import app.security.PasswordService;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "member")
public class Member {
    @Id
    private String id;
    @Column(nullable = false, columnDefinition = "text")
    private String name;
    // Initially nullable so Hibernate can add columns to an existing database.
    // SecurityConfig backfills existing members and then sets NOT NULL.
    @Column(unique = true, length = 100)
    private String username;

    @JsonIgnore
    @Setter(AccessLevel.NONE)
    @Column(name = "password_hash", length = 60)
    private String passwordHash;

    @Setter(AccessLevel.NONE)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "member_role",
            joinColumns = @JoinColumn(name = "member_id"),
            inverseJoinColumns = @JoinColumn(name = "role_name"))
    private Set<Role> roles = new HashSet<>();

    public void addRole(Role role) { roles.add(role); }

    public void removeRole(String name) { roles.removeIf(role -> role.getName().equals(name)); }

    public Set<String> getRolesAsStrings() {
        return roles.stream().map(Role::getName).collect(Collectors.toSet());
    }

    public void setPassword(String password) {
        passwordHash = PasswordService.hash(password);
    }

    public boolean verifyPassword(String password) {
        return PasswordService.verify(password, passwordHash);
    }

    public void initializeCredentials() {
        if (username == null) username = id;
        if (passwordHash == null) setPassword(PasswordService.DEFAULT_PASSWORD);
    }
}

