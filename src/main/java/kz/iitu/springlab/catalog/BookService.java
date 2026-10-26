package kz.iitu.springlab.catalog;
import jakarta.persistence.EntityManager;
import kz.iitu.springlab.web.Genre;
import kz.iitu.springlab.web.dto.BookMapper;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class BookService {
    private final BookRepository repository;
    private final EntityManager em;
    public BookService(BookRepository repository, EntityManager em) { this.repository = repository; this.em = em; }
    public List<Book> findAll(String author) {
        return repository.findAllWithAuthor().stream().filter(b -> author == null || author.isBlank()
                || b.author().toLowerCase(Locale.ROOT).contains(author.toLowerCase(Locale.ROOT))).toList();
    }
    public Page<Book> find(Genre genre, String author, Pageable pageable) {
        if (author != null && !author.isBlank()) return genre == null
                ? repository.findByAuthorNameContainingIgnoreCase(author, pageable)
                : repository.findByGenreAndAuthorNameContainingIgnoreCase(genre, author, pageable);
        return genre == null ? repository.findAll(pageable) : repository.findByGenre(genre, pageable);
    }
    public Map<String, Object> scroll(Long after, BookMapper mapper) {
        ScrollPosition position = after == null ? ScrollPosition.keyset()
                : ScrollPosition.forward(Map.of("id", after));
        Window<Book> window = repository.findFirst3ByOrderByIdAsc(position);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", window.getContent().stream().map(mapper::toResponse).toList());
        result.put("hasNext", window.hasNext());
        result.put("nextAfter", window.isEmpty() ? null : window.getContent().getLast().id());
        return result;
    }
    public Optional<Book> findById(long id) { return repository.findById(id); }
    public Book requireById(long id) { return findById(id).orElseThrow(() -> new BookNotFoundException(id)); }
    @Transactional
    public Book create(Book book) {
        assertNoDuplicate(book.title(), book.author(), null);
        book.setAuthor(resolveAuthor(book.author()));
        return repository.save(book);
    }
    private Author resolveAuthor(String name) {
        var authors = em.createQuery("select a from Author a where lower(a.name) = lower(:name)", Author.class)
                .setParameter("name", name).setMaxResults(1).getResultList();
        if (!authors.isEmpty()) return authors.getFirst();
        Author author = new Author(name, "Unknown"); em.persist(author); return author;
    }
    @Transactional
    public Optional<Book> replace(long id, Book book) {
        Book existing = requireById(id);
        assertNoDuplicate(book.title(), book.author(), id);
        existing.setTitle(book.title()); existing.setGenre(book.genre()); existing.setYear(book.year());
        existing.setAuthor(resolveAuthor(book.author()));
        return Optional.of(existing);
    }
    @Transactional
    public Book update(long id, Book book) { return replace(id, book).orElseThrow(); }
    @Transactional
    public boolean delete(long id) { repository.delete(requireById(id)); return true; }
    @Transactional
    public List<Book> createAll(List<Book> books) { return books.stream().map(this::create).toList(); }
    private void assertNoDuplicate(String title, String author, Long excludeId) {
        repository.findFirstByTitleIgnoreCaseAndAuthorNameIgnoreCase(title.trim(), author.trim())
                .filter(b -> !b.id().equals(excludeId)).ifPresent(b -> { throw new DuplicateTitleException(title); });
    }
}
