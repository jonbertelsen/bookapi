package app.controllers.impl;

import app.config.HibernateConfig;
import app.controllers.IController;
import app.daos.impl.BookDAO;
import app.dtos.BookDTO;
import io.javalin.http.Context;

public class BookController implements IController<BookDTO, String> {
    private final BookDAO dao;

    public BookController() {
        dao = BookDAO.getInstance(HibernateConfig.getEntityManagerFactory("library"));
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
    public BookDTO validateEntity(Context ctx) {
        return ctx.bodyValidator(BookDTO.class).required()
                .check(dto -> dto.getTitle() != null && !dto.getTitle().isBlank(), "title must be set")
                .check(dto -> dto.getAuthor() != null && !dto.getAuthor().isBlank(), "author must be set")
                .get();
    }
}
