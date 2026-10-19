package kz.iitu.springlab.catalog;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BookService {

    private final BookRepository repository;

    public BookService(BookRepository repository) {
        this.repository = repository;
    }

    public List<Book> findAll(String author) {
        if (author == null || author.isBlank()) {
            return repository.findAll();
        }
        return repository.findAll().stream()
                .filter(b -> b.author().equalsIgnoreCase(author)
                        || b.author().toLowerCase().contains(author.toLowerCase()))
                .toList();
    }

    public Optional<Book> findById(long id) {
        return repository.findById(id);
    }

    public Book requireById(long id) {
        return repository.findById(id).orElseThrow(() -> new BookNotFoundException(id));
    }

    public Book create(Book book) {
        assertNoDuplicate(book.title(), book.author(), null);
        return repository.save(new Book(null, book.title(), book.author(), book.year(),
                book.genre(), book.createdAt()));
    }

    public Optional<Book> replace(long id, Book book) {
        if (repository.findById(id).isEmpty()) {
            throw new BookNotFoundException(id);
        }
        assertNoDuplicate(book.title(), book.author(), id);
        Book existing = repository.findById(id).orElseThrow();
        return Optional.of(repository.save(new Book(id, book.title(), book.author(), book.year(),
                book.genre() != null ? book.genre() : existing.genre(),
                existing.createdAt())));
    }

    public Book update(long id, Book book) {
        return replace(id, book).orElseThrow();
    }

    public boolean delete(long id) {
        if (!repository.deleteById(id)) {
            throw new BookNotFoundException(id);
        }
        return true;
    }

    // Variant 5 (lab05): bulk create, kept for regression
    public List<Book> createAll(List<Book> books) {
        return books.stream().map(this::create).toList();
    }

    private void assertNoDuplicate(String title, String author, Long excludeId) {
        if (title == null || author == null) {
            return;
        }
        boolean exists = repository.findAll().stream()
                .anyMatch(b -> (excludeId == null || !b.id().equals(excludeId))
                        && b.title().equalsIgnoreCase(title.trim())
                        && b.author().equalsIgnoreCase(author.trim()));
        if (exists) {
            throw new DuplicateTitleException(title);
        }
    }
}
