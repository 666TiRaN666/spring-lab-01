package kz.iitu.springlab.web;

import kz.iitu.springlab.catalog.BookNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(annotations = Controller.class)
public class PageExceptionHandler {

    @ExceptionHandler(BookNotFoundException.class)
    public String handleNotFound(BookNotFoundException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error/not-found";          // your own template
    }
}
