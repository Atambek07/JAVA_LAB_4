package kg.course.library.repository;

import kg.course.library.Isbn;
import kg.course.library.entity.Loan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий выдач.
 * <p>
 * Методы чтения используют {@code @EntityGraph(attributePaths = {"book", "reader"})},
 * чтобы подгрузить книгу и читателя одним SQL-запросом (join fetch) и устранить
 * проблему N+1 при формировании ответа {@code GET /api/loans}.
 * </p>
 */
public interface LoanRepository extends JpaRepository<Loan, Long> {

    /** Все выдачи с книгой и читателем, отсортированные по дате выдачи. */
    @EntityGraph(attributePaths = {"book", "reader"})
    @Query("select l from Loan l order by l.issuedOn asc, l.id asc")
    List<Loan> findAllOrdered();

    /** Выдача по номеру с книгой и читателем. */
    @EntityGraph(attributePaths = {"book", "reader"})
    @Query("select l from Loan l where l.id = :id")
    Optional<Loan> findLoanById(@Param("id") Long id);

    /** Активные выдачи читателя. */
    @EntityGraph(attributePaths = {"book", "reader"})
    List<Loan> findByReaderCardNumberAndReturnedOnIsNull(String cardNumber);

    /**
     * Активные выдачи со сроком возврата раньше заданной даты
     * (то есть просроченные), отсортированные по сроку возврата.
     */
    @EntityGraph(attributePaths = {"book", "reader"})
    List<Loan> findByReturnedOnIsNullAndDueOnBeforeOrderByDueOnAsc(LocalDate date);

    /** Количество активных выдач книги (используется для проверки доступности экземпляров). */
    long countByBookIsbnAndReturnedOnIsNull(Isbn isbn);

    /** Количество активных выдач читателя (проверка лимита категории). */
    long countByReaderCardNumberAndReturnedOnIsNull(String cardNumber);

    /** Есть ли у читателя просроченные выдачи. */
    boolean existsByReaderCardNumberAndReturnedOnIsNullAndDueOnBefore(String cardNumber, LocalDate date);

    /** Есть ли активные выдачи книги (запрет удаления книги). */
    boolean existsByBookIsbnAndReturnedOnIsNull(Isbn isbn);

    /** Удаление всех выдач книги (необходимо перед удалением самой книги). */
    void deleteByBookIsbn(Isbn isbn);
}
