package app.config;

import app.entities.Member;
import app.entities.Role;
import jakarta.persistence.EntityManagerFactory;

public class SecurityConfig {
    /** Add credentials to pre-security data without resetting existing passwords. */
    public static void initializeMembers(EntityManagerFactory emf) {
        try (var em = emf.createEntityManager()) {
            var tx = em.getTransaction();
            try {
                tx.begin();
                Role user = em.find(Role.class, "user");
                if (user == null) { user = new Role("user"); em.persist(user); }
                Role admin = em.find(Role.class, "admin");
                if (admin == null) { admin = new Role("admin"); em.persist(admin); }
                for (Member member : em.createQuery("select m from Member m", Member.class).getResultList()) {
                    member.initializeCredentials();
                    // Seed defaults only; preserve later role assignments on restart.
                    if (member.getRoles().isEmpty()) member.addRole("2".equals(member.getId()) ? admin : user);
                }
                em.flush();
                em.createNativeQuery("alter table member alter column username set not null").executeUpdate();
                em.createNativeQuery("alter table member alter column password_hash set not null").executeUpdate();
                tx.commit();
            } catch (RuntimeException ex) {
                if (tx.isActive()) tx.rollback();
                throw ex;
            }
        }
    }
}
