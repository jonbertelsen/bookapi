package app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import app.entities.Book;
import app.entities.Member;
import app.entities.Loan;
import app.entities.Role;
import app.security.SecurityRole;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;

public class Populate {
    // Import the original db.json once, only into an entirely empty library.
    public static void populate(EntityManagerFactory emf) {
        try (var em = emf.createEntityManager()) {
            if (em.createQuery("select count(b) from Book b", Long.class).getSingleResult() > 0
                    || em.createQuery("select count(m) from Member m", Long.class).getSingleResult() > 0
                    || em.createQuery("select count(l) from Loan l", Long.class).getSingleResult() > 0) return;
            try (var input = Populate.class.getResourceAsStream("/db.json")) {
                var data = new ObjectMapper().readTree(input);
                var tx = em.getTransaction();
                try {
                    tx.begin();
                    for (SecurityRole role : SecurityRole.values()) {
                        if (em.find(Role.class, role.getRoleName()) == null) {
                            em.persist(new Role(role.getRoleName()));
                        }
                    }
                    for (var row : data.get("books")) {
                        Book book = new Book();
                        book.setId(row.get("id").asText());
                        book.setTitle(row.get("title").asText());
                        book.setAuthor(row.get("author").asText());
                        em.persist(book);
                    }
                    for (var row : data.get("members")) {
                        Member member = new Member();
                        member.setId(row.get("id").asText());
                        member.setName(row.get("name").asText());
                        member.initializeCredentials();
                        for (var roleName : row.get("roles")) {
                            Role role = em.find(Role.class, roleName.asText());
                            if (role == null) {
                                throw new IllegalStateException("Unknown seed member role: " + roleName.asText());
                            }
                            member.addRole(role);
                        }
                        em.persist(member);
                    }
                    for (var row : data.get("loans")) {
                        Loan loan = new Loan();
                        loan.setId(row.get("id").asText());
                        loan.setBook(em.find(Book.class, row.get("bookId").asText()));
                        loan.setMember(em.find(Member.class, row.get("memberId").asText()));
                        loan.setDueDate(LocalDate.parse(row.get("dueDate").asText()));
                        loan.setReturned(row.get("returned").asBoolean());
                        em.persist(loan);
                    }
                    tx.commit();
                } catch (RuntimeException ex) {
                    if (tx.isActive()) tx.rollback();
                    throw ex;
                }
            } catch (java.io.IOException ex) {
                throw new IllegalStateException("Could not read seed data", ex);
            }
        }
    }
}
