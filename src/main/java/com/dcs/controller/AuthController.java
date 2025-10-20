package com.dcs.controller;

import com.dcs.model.User;
import com.dcs.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.http.MediaType;
import java.util.HashMap;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password,
                        @RequestParam String role, HttpSession session, RedirectAttributes redirectAttributes) {
        var userOpt = userService.login(username, password);
        if(userOpt.isPresent() && userOpt.get().getRole().name().equalsIgnoreCase(role)) {
            session.setAttribute("user", userOpt.get());
            if(role.equalsIgnoreCase("STUDENT")) return "redirect:/student/dashboard";
            else if(role.equalsIgnoreCase("DEPARTMENT")) return "redirect:/dept/dashboard";
            else return "redirect:/admin/dashboard";
        }
        redirectAttributes.addFlashAttribute("error", "Invalid credentials");
        return "redirect:/auth/login";
    }

    // JSON API for SPA login
    @PostMapping(path = "/api/login", consumes = MediaType.ALL_VALUE, produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> loginJson(HttpServletRequest request, @RequestBody(required = false) Map<String, String> payload, HttpSession session) {
        try {
            if (payload == null || payload.isEmpty()) {
                String raw = request.getReader().lines().collect(Collectors.joining("\n"));
                if (raw != null && !raw.isBlank()) {
                    ObjectMapper mapper = new ObjectMapper();
                    payload = mapper.readValue(raw, new TypeReference<HashMap<String, String>>() {});
                }
            }
        } catch (Exception e) {
            // If parsing fails, return a clear error
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "error", "Invalid JSON payload"
            ));
        }

        if (payload == null) payload = new HashMap<>();
        String usernameOrEmail = payload.getOrDefault("username", payload.getOrDefault("email", "")).trim();
        String password = payload.getOrDefault("password", ""); // do NOT trim password
        String role = payload.getOrDefault("role", "STUDENT").trim();

        if (usernameOrEmail.isEmpty() || password.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "error", "Missing username/email or password"
            ));
        }

        var userOpt = userService.login(usernameOrEmail, password);
        if (userOpt.isPresent()) {
            User u = userOpt.get();
            session.setAttribute("user", u);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "user", Map.of(
                            "id", u.getId(),
                            "username", u.getUsername(),
                            "email", u.getEmail(),
                            "role", u.getRole().name(),
                            "department", u.getDepartment() == null ? "" : u.getDepartment()
                    )
            ));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "success", false,
                "error", "Invalid credentials"
        ));
    }

    @GetMapping(path = "/api/me", produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> me(HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (userObj instanceof User) {
            User u = (User) userObj;
            return ResponseEntity.ok(Map.of(
                    "id", u.getId(),
                    "username", u.getUsername(),
                    "email", u.getEmail(),
                    "role", u.getRole().name(),
                    "department", u.getDepartment() == null ? "" : u.getDepartment()
            ));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
    }

    @GetMapping("/register")
    public String registerPage() { return "register"; }

    @PostMapping("/register")
    public String register(@RequestParam String username, @RequestParam String email,
                           @RequestParam String password, RedirectAttributes redirectAttributes) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole(User.Role.STUDENT);
        userService.register(user);
        redirectAttributes.addFlashAttribute("success", "Registration successful. Please log in.");
        return "redirect:/auth/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, jakarta.servlet.http.HttpServletRequest request) {
        session.invalidate();
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            return "redirect:" + referer;
        }
        return "redirect:/index.html";
    }
}
