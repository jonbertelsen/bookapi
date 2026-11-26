package app.dtos;

import app.entities.Member;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.util.Set;
import lombok.AccessLevel;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
@NoArgsConstructor
public class MemberDTO {
    private String id;
    private String name;
    private String username;
    @Setter(AccessLevel.NONE)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Set<String> roles;

    public MemberDTO(Member entity) {
        this.id = entity.getId();
        this.name = entity.getName();
        this.username = entity.getUsername();
        this.roles = entity.getRolesAsStrings();
    }
}
