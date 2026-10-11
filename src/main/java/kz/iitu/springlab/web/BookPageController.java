package kz.iitu.springlab.web;

import jakarta.validation.Valid;
import kz.iitu.springlab.catalog.Book;
import kz.iitu.springlab.catalog.BookService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/books")
public class BookPageController {

    private final BookService service;

    public BookPageController(BookService service) {
        this.service = service;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String author,
                       Model model) {
        model.addAttribute("books", service.findAll(author));
        model.addAttribute("author", author);
        model.addAttribute("title", "Catalogue");
        return "books/list";       // -> templates/books/list.html
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new BookForm());
        model.addAttribute("genres", Genre.values());
        return "books/form";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable long id, Model model) {
        model.addAttribute("book", service.requireById(id));
        return "books/detail";
    }

    // Replaces the old @RequestParam POST from lab05 (same path+method,
    // otherwise ambiguous mapping). Form fields incl. genre/date are
    // validated; only title/author/year are stored in Book.
    @PostMapping
    public String create(@Valid @ModelAttribute("form") BookForm form,
                         BindingResult binding,          // immediately after!
                         Model model,
                         RedirectAttributes redirect) {

        if (binding.hasErrors()) {
            model.addAttribute("genres", Genre.values());   // the model again
            return "books/form";
        }

        Book saved = service.create(new Book(null, form.getTitle(),
                form.getAuthor(), form.getYear()));

        redirect.addFlashAttribute("message",
                "Book «" + saved.title() + "» has been added");
        return "redirect:/books";
    }
}
