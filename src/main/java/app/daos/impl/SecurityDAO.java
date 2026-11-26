package app.daos.impl;

import app.daos.ISecurityDAO;
import app.entities.Member;
import app.exceptions.ApiException;
import app.security.PasswordService;
import jakarta.persistence.EntityManagerFactory;

public class SecurityDAO implements ISecurityDAO {
    private final EntityManagerFactory emf;
    private static final String DUMMY_HASH = PasswordService.hash("nonexistent-member");

    public SecurityDAO(EntityManagerFactory emf) { this.emf = emf; }

    @Override
    public Member getVerifiedMember(String username, String password) {
        try (var em = emf.createEntityManager()) {
            var members = em.createQuery("select m from Member m where m.username = :username", Member.class)
                    .setParameter("username", username).getResultList();
            Member member = members.isEmpty() ? null : members.getFirst();
            // Do the same expensive password check for unknown usernames.
            boolean valid = PasswordService.verify(password, member == null ? DUMMY_HASH : member.getPasswordHash());
            if (member == null || !valid) throw new ApiException(401, "Invalid username or password");
            return member;
        }
    }

    @Override
    public Member getMember(String id) {
        try (var em = emf.createEntityManager()) {
            Member member = em.find(Member.class, id);
            if (member == null) throw new ApiException(401, "Member no longer exists");
            return member;
        }
    }
}
