package kz.iitu.springlab.catalog;

import jakarta.persistence.EntityManager;
import kz.iitu.springlab.web.Genre;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.function.Supplier;

@Component
@Profile("dev")
public class Lab8Evidence implements CommandLineRunner {
    private final EntityManager em;
    private final BookRepository books;
    private final Map<String, Object> results = new LinkedHashMap<>();
    public Lab8Evidence(EntityManager em, BookRepository books) { this.em = em; this.books = books; }
    public Map<String, Object> getResults() { return Collections.unmodifiableMap(results); }

    @Override
    @Transactional
    public void run(String... args) {
        if (books.count() != 0) return;
        Tag java = new Tag("java"), clean = new Tag("clean-code"),
                spring = new Tag("spring"), reference = new Tag("reference");
        for (Tag tag : List.of(java, clean, spring, reference)) em.persist(tag);
        Author bloch = new Author("Joshua Bloch", "USA");
        Author martin = new Author("Robert Martin", "USA");
        Author walls = new Author("Craig Walls", "USA");
        add(bloch, "Effective Java", Genre.TECH, 2018, java, reference);
        add(bloch, "Java Puzzlers", Genre.TECH, 2005, java);
        add(bloch, "Java Collections", Genre.SCIENCE, 2001, reference);
        add(martin, "Clean Code", Genre.TECH, 2008, clean);
        add(martin, "Clean Architecture", Genre.TECH, 2017, clean, reference);
        add(martin, "The Clean Coder", Genre.BIOGRAPHY, 2011, clean);
        add(walls, "Spring in Action", Genre.TECH, 2022, spring, java);
        add(walls, "Spring Boot in Action", Genre.TECH, 2016, spring);
        for (Author a : List.of(bloch, martin, walls)) em.persist(a);
        em.flush(); em.clear();
        measure("findByGenre", () -> books.findByGenre(Genre.TECH).size());
        measure("findByTitleContainingIgnoreCase", () -> books.findByTitleContainingIgnoreCase("java").size());
        measure("findByYearBetweenOrderByYearDesc", () -> books.findByYearBetweenOrderByYearDesc(2005, 2018).size());
        measure("findTop3ByGenreOrderByYearDesc", () -> books.findTop3ByGenreOrderByYearDesc(Genre.TECH).size());
        measure("countByAuthorName", () -> books.countByAuthorName("Joshua Bloch"));
        measure("search", () -> books.search(Genre.TECH, 2016).size());
        measure("nPlusOneBefore", () -> { var rows = books.findAll(); rows.forEach(b -> b.getAuthor().getName()); return rows.size(); });
        measure("nPlusOneAfter", () -> { var rows = books.findAllWithAuthor(); rows.forEach(b -> b.getAuthor().getName()); return rows.size(); });
        measure("page", () -> { var page = books.findAll(PageRequest.of(0, 3, Sort.by("id"))); return Map.of("rows", page.getNumberOfElements(), "total", page.getTotalElements()); });
        measure("slice", () -> { var slice = books.findByArchivedFalse(PageRequest.of(0, 3, Sort.by("id"))); return Map.of("rows", slice.getNumberOfElements(), "hasNext", slice.hasNext()); });
        measure("offsetPage", () -> books.findAll(PageRequest.of(1, 3, Sort.by("id"))).getContent().stream().map(Book::id).toList());
        measure("keysetInitial", () -> books.findFirst3ByOrderByIdAsc(ScrollPosition.keyset()).getContent().stream().map(Book::id).toList());
        measure("keysetAfter3", () -> books.findFirst3ByOrderByIdAsc(ScrollPosition.forward(Map.of("id", 3L))).getContent().stream().map(Book::id).toList());
        measure("keysetAfter6", () -> books.findFirst3ByOrderByIdAsc(ScrollPosition.forward(Map.of("id", 6L))).getContent().stream().map(Book::id).toList());
        measure("archiveOlderThan", () -> books.archiveOlderThan(2010));
        em.createQuery("update Book b set b.archived = false").executeUpdate();
        em.clear();
        // Demonstrate the inverse-only mistake and remove the probe afterwards.
        Author author = em.find(Author.class, 1L);
        Book probe = new Book("Inverse side probe", Genre.TECH, 2020);
        author.getBooks().add(probe); // deliberately missing probe.setAuthor(author)
        em.persist(probe); em.flush();
        Object fk = em.createNativeQuery("select author_id from book where id = :id", Object.class)
                .setParameter("id", probe.id()).getSingleResult();
        results.put("inverseOnlyAuthorId", String.valueOf(fk));
        author.getBooks().remove(probe);
        em.remove(probe); em.flush(); em.clear();
        System.out.println("LAB08_RESULTS " + new tools.jackson.databind.json.JsonMapper().writeValueAsString(results));
    }
    private void add(Author author, String title, Genre genre, int year, Tag... tags) {
        Book book = new Book(title, genre, year); author.addBook(book); book.getTags().addAll(List.of(tags));
    }
    private void measure(String name, Supplier<Object> query) {
        em.flush(); em.clear();
        Statistics stats = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        long before = stats.getPrepareStatementCount();
        System.out.println("LAB08_BEGIN " + name);
        Object value = query.get();
        long count = stats.getPrepareStatementCount() - before;
        results.put(name, Map.of("result", value, "sqlStatements", count));
        System.out.println("LAB08_END " + name + " statements=" + count + " result=" + value);
    }
}
