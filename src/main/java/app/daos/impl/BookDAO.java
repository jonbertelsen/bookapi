package app.daos.impl;

import app.daos.IDAO;
import app.dtos.BookDTO;
import app.entities.*;
import app.exceptions.ApiException;
import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;

public class BookDAO implements IDAO<BookDTO, String> {
    private final EntityManagerFactory emf;
    private static BookDAO instance;

    private BookDAO(EntityManagerFactory emf) { this.emf = emf; }

    public static synchronized BookDAO getInstance(EntityManagerFactory emf) {
        if (instance == null || instance.emf != emf) instance = new BookDAO(emf);
        return instance;
    }

    private Book find(EntityManager em, String id) {
        Book entity = em.find(Book.class, id);
        if (entity == null) throw new ApiException(404, "Book not found");
        return entity;
    }

    @Override
    public BookDTO read(String id) {
        try (var em = emf.createEntityManager()) { return new BookDTO(find(em, id)); }
    }

    @Override
    public List<BookDTO> readAll() {
        try (var em = emf.createEntityManager()) {
            return em.createQuery("select e from Book e order by e.id", Book.class)
                    .getResultStream().map(BookDTO::new).toList();
        }
    }

    @Override
    public BookDTO create(BookDTO dto) {
        try (var em = emf.createEntityManager()) {
            var tx = em.getTransaction();
            try {
                tx.begin();
                Book entity = new Book();
                entity.setId(UUID.randomUUID().toString());
                apply(em, entity, dto);
                em.persist(entity);
                tx.commit();
                return new BookDTO(entity);
            } catch (RuntimeException ex) {
                if (tx.isActive()) tx.rollback();
                throw ex;
            }
        }
    }

    @Override
    public BookDTO update(String id, BookDTO dto) {
        try (var em = emf.createEntityManager()) {
            var tx = em.getTransaction();
            try {
                tx.begin();
                Book entity = find(em, id);
                apply(em, entity, dto);
                tx.commit();
                return new BookDTO(entity);
            } catch (RuntimeException ex) {
                if (tx.isActive()) tx.rollback();
                throw ex;
            }
        }
    }

    private void apply(EntityManager em, Book entity, BookDTO dto) {
        entity.setTitle(dto.getTitle());
        entity.setAuthor(dto.getAuthor());
    }

    @Override
    public void delete(String id) {
        try (var em = emf.createEntityManager()) {
            var tx = em.getTransaction();
            try {
                tx.begin();
                Book entity = find(em, id);
            if (em.createQuery("select count(l) from Loan l where l.book.id = :id", Long.class)
                    .setParameter("id", id).getSingleResult() > 0)
                throw new ApiException(409, "Cannot delete a book with loan history");
                em.remove(entity);
                tx.commit();
            } catch (RuntimeException ex) {
                if (tx.isActive()) tx.rollback();
                throw ex;
            }
        }
    }

    @Override
    public boolean validatePrimaryKey(String id) {
        try (var em = emf.createEntityManager()) { return em.find(Book.class, id) != null; }
    }
}
