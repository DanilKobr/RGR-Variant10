package com.example.lab3_clean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@Controller
public class SqlController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/")
    public String home(Model model) {
        return "main";
    }

    @PostMapping("/execute")
    // Додали Authentication authentication, щоб знати, хто стукає
    public String executeCommand(@RequestParam(value = "command", required = false) String command,
                                 Model model,
                                 Authentication authentication) {

        if (command == null || command.trim().isEmpty()) {
            model.addAttribute("error", "Команда не може бути порожньою");
            return "main";
        }

        String sql = command.trim();
        String upperSql = sql.toUpperCase();

        // --- БЛОК БЕЗПЕКИ ---

        // Перевіряємо, чи це команда на зміну даних (не SELECT)
        boolean isModification = !upperSql.startsWith("SELECT");

        // Перевіряємо, чи користувач АДМІН
        boolean isAdmin = authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));

        // Якщо хтось хоче змінити дані, але він не адмін -> БЛОКУЄМО
        if (isModification && !isAdmin) {
            model.addAttribute("error", "ПОМИЛКА ДОСТУПУ: Тільки адміністратор може змінювати дані!");
            return "main";
        }

        // ---------------------

        try {
            if (upperSql.startsWith("SELECT")) {
                List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
                model.addAttribute("result", result);
                model.addAttribute("message", "Успішно виконано! Знайдено рядків: " + result.size());
            } else {
                int rowsAffected = jdbcTemplate.update(sql);
                model.addAttribute("message", "Успішно! Змінено рядків: " + rowsAffected);
            }
        } catch (Exception e) {
            model.addAttribute("error", "Помилка бази даних: " + e.getMessage());
        }

        return "main";
    }
}