package kz.iitu.springlab.web;

import kz.iitu.springlab.security.AppUserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Administrator area: one page listing the users. Protected by the URL rule /admin/** → ADMIN. */
@Controller
@RequestMapping("/admin")
public class AdminController {
    private final AppUserRepository users;

    public AdminController(AppUserRepository users) { this.users = users; }

    @GetMapping
    public String users(Model model) {
        model.addAttribute("users", users.findAll(Sort.by("id")));
        return "admin/users";
    }
}
