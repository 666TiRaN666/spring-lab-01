package kz.iitu.springlab.catalog;
import kz.iitu.springlab.web.Genre;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByGenre(Genre genre);
    List<Book> findByTitleContainingIgnoreCase(String fragment);
    List<Book> findByYearBetweenOrderByYearDesc(int from, int to);
    List<Book> findTop3ByGenreOrderByYearDesc(Genre genre);
    long countByAuthorName(String name);
    @Query("select b from Book b where b.genre = :genre and b.year >= :year order by b.title")
    List<Book> search(@Param("genre") Genre genre, @Param("year") int year);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Book b set b.archived = true where b.year < :year")
    int archiveOlderThan(@Param("year") int year);
    @Query("select b from Book b join fetch b.author")
    List<Book> findAllWithAuthor();
    @Override @EntityGraph(attributePaths = "author")
    Optional<Book> findById(Long id);
    @Override @EntityGraph(attributePaths = "author")
    Page<Book> findAll(Pageable pageable);
    @EntityGraph(attributePaths = "author")
    Page<Book> findByGenre(Genre genre, Pageable pageable);
    @EntityGraph(attributePaths = "author")
    Page<Book> findByAuthorNameContainingIgnoreCase(String author, Pageable pageable);
    @EntityGraph(attributePaths = "author")
    Page<Book> findByGenreAndAuthorNameContainingIgnoreCase(Genre genre, String author, Pageable pageable);
    Slice<Book> findByArchivedFalse(Pageable pageable);
    @EntityGraph(attributePaths = "author")
    Window<Book> findFirst3ByOrderByIdAsc(ScrollPosition position);
    Optional<Book> findFirstByTitleIgnoreCaseAndAuthorNameIgnoreCase(String title, String author);
}
