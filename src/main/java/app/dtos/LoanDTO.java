package app.dtos;

import app.entities.Loan;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class LoanDTO {
    private String id;
    private String bookId;
    private String memberId;
    private LocalDate dueDate;
    private Boolean returned;

    public LoanDTO(Loan entity) {
        this.id = entity.getId();
        this.bookId = entity.getBook().getId();
        this.memberId = entity.getMember().getId();
        this.dueDate = entity.getDueDate();
        this.returned = entity.isReturned();
    }
}
