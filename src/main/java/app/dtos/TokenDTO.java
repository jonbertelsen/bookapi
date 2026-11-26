package app.dtos;

public record TokenDTO(String token, String tokenType, long expiresIn, MemberDTO member) {}
