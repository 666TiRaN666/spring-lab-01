package kz.iitu.springlab;

import jakarta.persistence.EntityManager;
import kz.iitu.springlab.catalog.*;
import kz.iitu.springlab.web.Genre;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:postgresql://localhost:5432/springlab_test")
@ActiveProfiles("dev")
@Transactional
class SpringLab01ApplicationTests {
    @Autowired BookRepository books;
    @Autowired BookService service;
    @Autowired EntityManager em;
    @Autowired Lab8Evidence evidence;

    @Test
    void associationsAndDerivedQueriesUsePostgres() {
        assertTrue(em.createNativeQuery("select version()", String.class).getSingleResult().toString().startsWith("PostgreSQL"));
        assertEquals(8, books.count());
        Book first = books.findById(1L).orElseThrow();
        assertEquals("Joshua Bloch", first.getAuthor().getName());
        assertEquals(2, first.getTags().size());
        assertEquals(3, first.getAuthor().getBooks().size());
        assertEquals(6, books.findByGenre(Genre.TECH).size());
        assertEquals(3, books.findByTitleContainingIgnoreCase("jAvA").size());
        assertTrue(books.findByTitleContainingIgnoreCase("%").isEmpty());
        assertEquals(6, books.findByYearBetweenOrderByYearDesc(2005, 2018).size());
        assertEquals(3, books.findTop3ByGenreOrderByYearDesc(Genre.TECH).size());
        assertEquals(3, books.countByAuthorName("Joshua Bloch"));
        assertEquals(4, books.search(Genre.TECH, 2016).size());
    }

    @Test
    void pagingAndKeysetHaveNoMissingOrRepeatedBooks() {
        var page = books.findAll(PageRequest.of(1, 3, Sort.by(Sort.Direction.DESC, "year")));
        assertEquals(8, page.getTotalElements());
        assertEquals(3, page.getTotalPages());
        assertEquals(java.util.List.of(2016, 2011, 2008), page.stream().map(Book::year).toList());
        var position = ScrollPosition.keyset();
        var ids = new java.util.ArrayList<Long>();
        boolean more;
        do {
            var window = books.findFirst3ByOrderByIdAsc(position);
            ids.addAll(window.stream().map(Book::id).toList());
            more = window.hasNext();
            if (more) position = (KeysetScrollPosition) window.positionAt(window.size() - 1);
        } while (more);
        assertEquals(java.util.List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L), ids);
        assertEquals(4L, ((java.util.Map<?, ?>) evidence.getResults().get("nPlusOneBefore")).get("sqlStatements"));
        assertEquals(1L, ((java.util.Map<?, ?>) evidence.getResults().get("nPlusOneAfter")).get("sqlStatements"));
    }

    @Test
    void bulkUpdateAndDirtyCheckingPersistAndDeleteCorrectly() {
        assertEquals(3, books.archiveOlderThan(2010));
        assertEquals(3L, em.createQuery("select count(b) from Book b where b.archived = true", Long.class).getSingleResult());
        Book created = service.create(new Book(null, "Persistence Test", "Joshua Bloch", 2020, Genre.TECH, java.time.Instant.now()));
        long id = created.id();
        em.flush(); em.clear();
        Book loaded = service.requireById(id);
        assertEquals("Joshua Bloch", loaded.author());
        long version = loaded.getVersion();
        loaded.setYear(2021);
        em.flush(); em.clear();
        assertEquals(2021, service.requireById(id).year());
        assertEquals(version + 1, service.requireById(id).getVersion());
        service.delete(id); em.flush(); em.clear();
        assertTrue(books.findById(id).isEmpty());
    }
}
