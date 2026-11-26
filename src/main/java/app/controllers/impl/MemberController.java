package app.controllers.impl;

import app.config.HibernateConfig;
import app.controllers.IController;
import app.daos.impl.MemberDAO;
import app.dtos.MemberDTO;
import io.javalin.http.Context;

public class MemberController implements IController<MemberDTO, String> {
    private final MemberDAO dao;

    public MemberController() {
        dao = MemberDAO.getInstance(HibernateConfig.getEntityManagerFactory("library"));
    }

    @Override
    public void read(Context ctx) { ctx.json(dao.read(ctx.pathParam("id"))); }

    @Override
    public void readAll(Context ctx) { ctx.json(dao.readAll()); }

    @Override
    public void create(Context ctx) { ctx.status(201).json(dao.create(validateEntity(ctx))); }

    @Override
    public void update(Context ctx) { ctx.json(dao.update(ctx.pathParam("id"), validateEntity(ctx))); }

    @Override
    public void delete(Context ctx) { dao.delete(ctx.pathParam("id")); ctx.status(204); }

    @Override
    public boolean validatePrimaryKey(String id) { return dao.validatePrimaryKey(id); }

    @Override
    public MemberDTO validateEntity(Context ctx) {
        return ctx.bodyValidator(MemberDTO.class).required()
                .check(dto -> dto.getName() != null && !dto.getName().isBlank(), "name must be set")
                .check(dto -> dto.getUsername() == null || dto.getUsername().matches("[A-Za-z0-9_.@-]{1,100}"),
                        "username must contain 1-100 letters, digits, _, ., @ or -")
                .get();
    }
}
