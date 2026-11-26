package app.daos;

import app.entities.Member;

public interface ISecurityDAO {
    Member getVerifiedMember(String username, String password);
    Member getMember(String id);
}
