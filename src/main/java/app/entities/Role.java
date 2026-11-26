package app.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.Objects;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "role")
public class Role {
    @Id
    @Column(length = 50, nullable = false)
    private String name;

    public Role(String name) { this.name = name; }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof Role role && name != null && name.equals(role.getName());
    }

    @Override
    public int hashCode() { return Objects.hashCode(name); }
}
