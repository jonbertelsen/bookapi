package app.daos.impl;

import app.daos.IDAO;
import app.dtos.LoanDTO;
import app.entities.*;
import app.exceptions.ApiException;
import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;

public class LoanDAO implements IDAO<LoanDTO, String> {
    private final EntityManagerFactory emf;
    private static LoanDAO instance;

    private LoanDAO(EntityManagerFactory emf) { this.emf = emf; }

    public static synchronized LoanDAO getInstance(EntityManagerFactory emf) {
        if (instance == null || instance.emf != emf) instance = new LoanDAO(emf);
        return instance;
    }

    private Loan find(EntityManager em, String id) {
        Loan entity = em.find(Loan.class, id);
        if (entity == null) throw new ApiException(404, "Loan not found");
        return entity;
    }

    @Override
    public LoanDTO read(String id) {
        try (var em = emf.createEntityManager()) { return new LoanDTO(find(em, id)); }
    }

    @Override
    public List<LoanDTO> readAll() {
        try (var em = emf.createEntityManager()) {
            return em.createQuery("select e from Loan e order by e.id", Loan.class)
                    .getResultStream().map(LoanDTO::new).toList();
        }
    }

    @Override
    public LoanDTO create(LoanDTO dto) {
        try (var em = emf.createEntityManager()) {
            var tx = em.getTransaction();
            try {
                tx.begin();
                Loan entity = new Loan();
                entity.setId(UUID.randomUUID().toString());
                apply(em, entity, dto);
                em.persist(entity);
                tx.commit();
                return new LoanDTO(entity);
            } catch (RuntimeException ex) {
                if (tx.isActive()) tx.rollback();
                throw ex;
            }
        }
    }

    @Override
    public LoanDTO update(String id, LoanDTO dto) {
        try (var em = emf.createEntityManager()) {
            var tx = em.getTransaction();
            try {
                tx.begin();
                Loan entity = find(em, id);
                apply(em, entity, dto);
                tx.commit();
                return new LoanDTO(entity);
            } catch (RuntimeException ex) {
                if (tx.isActive()) tx.rollback();
                throw ex;
            }
        }
    }

    private void apply(EntityManager em, Loan entity, LoanDTO dto) {
        Book book = em.find(Book.class, dto.getBookId(), LockModeType.PESSIMISTIC_WRITE);
        Member member = em.find(Member.class, dto.getMemberId());
        if (book == null || member == null) throw new ApiException(404, "Book or member not found");
        if (!Boolean.TRUE.equals(dto.getReturned())) {
            Long active = em.createQuery("select count(l) from Loan l where l.book.id = :bookId and l.returned = false and l.id <> :id", Long.class)
                    .setParameter("bookId", book.getId()).setParameter("id", entity.getId()).getSingleResult();
            if (active > 0) throw new ApiException(409, "Book is already on loan");
        }
        entity.setBook(book);
        entity.setMember(member);
        entity.setDueDate(dto.getDueDate());
        entity.setReturned(Boolean.TRUE.equals(dto.getReturned()));
    }

    @Override
    public void delete(String id) {
        try (var em = emf.createEntityManager()) {
            var tx = em.getTransaction();
            try {
                tx.begin();
                Loan entity = find(em, id);
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
        try (var em = emf.createEntityManager()) { return em.find(Loan.class, id) != null; }
    }
}
