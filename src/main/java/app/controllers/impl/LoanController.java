package app.controllers.impl;

import app.config.HibernateConfig;
import app.controllers.IController;
import app.daos.impl.LoanDAO;
import app.dtos.LoanDTO;
import io.javalin.http.Context;

public class LoanController implements IController<LoanDTO, String> {
    private final LoanDAO dao;

    public LoanController() {
        dao = LoanDAO.getInstance(HibernateConfig.getEntityManagerFactory("library"));
    }

    @Override
    public void read(Context ctx) { ctx.json(dao.read(ctx.pathParam("id"))); }

    @Override
    public void readAll(Context ctx) { ctx.json(dao.readAll()); }

    @Override
    public void create(Context ctx) { ctx.status(201).json(dao.create(validateEntity(ctx))); }

    @Override
    public void update(Context ctx) { ctx.json(dao.update(ctx.pathParam("id"), validateEntity(ctx))); }

    public void patch(Context ctx) {
        LoanDTO current = dao.read(ctx.pathParam("id"));
        var body = ctx.bodyAsClass(com.fasterxml.jackson.databind.JsonNode.class);
        if (!body.isObject() || body.size() != 1 || !body.has("returned") || !body.get("returned").isBoolean())
            throw new app.exceptions.ApiException(400, "PATCH requires a returned boolean");
        current.setReturned(body.get("returned").booleanValue());
        ctx.json(dao.update(ctx.pathParam("id"), current));
    }

    @Override
    public void delete(Context ctx) { dao.delete(ctx.pathParam("id")); ctx.status(204); }

    @Override
    public boolean validatePrimaryKey(String id) { return dao.validatePrimaryKey(id); }

    @Override
    public LoanDTO validateEntity(Context ctx) {
        return ctx.bodyValidator(LoanDTO.class).required()
                .check(dto -> dto.getBookId() != null && !dto.getBookId().isBlank(), "bookId must be set")
                .check(dto -> dto.getMemberId() != null && !dto.getMemberId().isBlank(), "memberId must be set")
                .check(dto -> dto.getDueDate() != null, "dueDate must be set")
                .check(dto -> dto.getReturned() != null, "returned must be set")
                .get();
    }
}
