package app.daos.impl;

import app.daos.IDAO;
import app.dtos.MemberDTO;
import app.entities.*;
import app.exceptions.ApiException;
import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;

public class MemberDAO implements IDAO<MemberDTO, String> {
    private final EntityManagerFactory emf;
    private static MemberDAO instance;

    private MemberDAO(EntityManagerFactory emf) { this.emf = emf; }

    public static synchronized MemberDAO getInstance(EntityManagerFactory emf) {
        if (instance == null || instance.emf != emf) instance = new MemberDAO(emf);
        return instance;
    }

    private Member find(EntityManager em, String id) {
        Member entity = em.find(Member.class, id);
        if (entity == null) throw new ApiException(404, "Member not found");
        return entity;
    }

    @Override
    public MemberDTO read(String id) {
        try (var em = emf.createEntityManager()) { return new MemberDTO(find(em, id)); }
    }

    @Override
    public List<MemberDTO> readAll() {
        try (var em = emf.createEntityManager()) {
            return em.createQuery("select e from Member e order by e.id", Member.class)
                    .getResultStream().map(MemberDTO::new).toList();
        }
    }

    @Override
    public MemberDTO create(MemberDTO dto) {
        try (var em = emf.createEntityManager()) {
            var tx = em.getTransaction();
            try {
                tx.begin();
                Member entity = new Member();
                entity.setId(UUID.randomUUID().toString());
                entity.addRole(em.getReference(Role.class, "user"));
                apply(em, entity, dto);
                em.persist(entity);
                tx.commit();
                return new MemberDTO(entity);
            } catch (RuntimeException ex) {
                if (tx.isActive()) tx.rollback();
                throw ex;
            }
        }
    }

    @Override
    public MemberDTO update(String id, MemberDTO dto) {
        try (var em = emf.createEntityManager()) {
            var tx = em.getTransaction();
            try {
                tx.begin();
                Member entity = find(em, id);
                apply(em, entity, dto);
                tx.commit();
                return new MemberDTO(entity);
            } catch (RuntimeException ex) {
                if (tx.isActive()) tx.rollback();
                throw ex;
            }
        }
    }

    private void apply(EntityManager em, Member entity, MemberDTO dto) {
        entity.setName(dto.getName());
        if (dto.getUsername() != null) entity.setUsername(dto.getUsername());
        entity.initializeCredentials();
        Long duplicates = em.createQuery("select count(m) from Member m where m.username = :username and m.id <> :id", Long.class)
                .setParameter("username", entity.getUsername()).setParameter("id", entity.getId()).getSingleResult();
        if (duplicates > 0) throw new ApiException(409, "Username is already taken");
    }

    @Override
    public void delete(String id) {
        try (var em = emf.createEntityManager()) {
            var tx = em.getTransaction();
            try {
                tx.begin();
                Member entity = find(em, id);
            if (em.createQuery("select count(l) from Loan l where l.member.id = :id", Long.class)
                    .setParameter("id", id).getSingleResult() > 0)
                throw new ApiException(409, "Cannot delete a member with loan history");
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
        try (var em = emf.createEntityManager()) { return em.find(Member.class, id) != null; }
    }
}
