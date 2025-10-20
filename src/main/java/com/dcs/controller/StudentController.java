package com.dcs.controller;

import com.dcs.model.Complaint;
import com.dcs.model.User;
import com.dcs.service.ComplaintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashMap;

@Controller
@RequestMapping("/student")
public class StudentController {
    @Autowired
    private ComplaintService complaintService;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = (User)session.getAttribute("user");
        model.addAttribute("complaints", complaintService.getComplaintsByUser(user));
        return "student_dashboard";
    }

    @PostMapping("/complaints")
    public String submitComplaint(@RequestParam String title, @RequestParam String category,
                                  @RequestParam String description, HttpSession session) {
        User user = (User)session.getAttribute("user");
        Complaint complaint = new Complaint();
        complaint.setUser(user);
        complaint.setTitle(title);
        complaint.setCategory(category);
        complaint.setDescription(description);
        // Optionally map category to departmentEmail here
        complaint.setDepartmentEmail("department@example.com");
        complaintService.submitComplaint(complaint);
        complaintService.upsertDepartmentStatus(complaint, "Pending", "");
        return "redirect:/student/dashboard";
    }

    // JSON APIs for SPA
    @GetMapping(path = "/api/complaints", produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> listComplaints(HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (!(userObj instanceof User) || ((User) userObj).getRole() != User.Role.STUDENT) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }
        User user = (User) userObj;
        List<Complaint> complaints = complaintService.getComplaintsByUser(user);
        List<Map<String, Object>> data = complaints.stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("title", defaultString(c.getTitle()));
            m.put("category", defaultString(c.getCategory()));
            m.put("description", defaultString(c.getDescription()));
            m.put("status", normalizeStatus(c.getStatus()));
            m.put("submittedAt", c.getCreatedAt() != null ? c.getCreatedAt().toLocalDate().toString() : "");
            m.put("lastUpdated", c.getUpdatedAt() != null ? c.getUpdatedAt().toLocalDate().toString() : "");
            m.put("resolvedAt", c.getResolvedAt() != null ? c.getResolvedAt().toLocalDate().toString() : "");
            m.put("departmentEmail", defaultString(c.getDepartmentEmail()));
            return m;
        }).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(Map.of("complaints", data));
    }

    @PostMapping(path = "/api/complaints", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> submitComplaintJson(@RequestBody Map<String, String> payload, HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (!(userObj instanceof User) || ((User) userObj).getRole() != User.Role.STUDENT) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }
        User user = (User) userObj;
        String title = payload.getOrDefault("title", "").trim();
        String category = payload.getOrDefault("category", "other").trim();
        String description = payload.getOrDefault("description", "").trim();
        if (title.isEmpty() || description.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Title and description are required"));
        }
        Complaint complaint = new Complaint();
        complaint.setUser(user);
        complaint.setTitle(title);
        complaint.setCategory(category);
        complaint.setDescription(description);
        complaint.setDepartmentEmail(mapDepartmentEmail(category));
        complaintService.submitComplaint(complaint);
        complaintService.upsertDepartmentStatus(complaint, "Pending", "");
        return ResponseEntity.ok(Map.of("success", true, "id", complaint.getId()));
    }

    private String mapDepartmentEmail(String category) {
        switch (category) {
            case "facilities": return "facilities@university.edu";
            case "food-services": return "dining@university.edu";
            case "academic": return "academic@university.edu";
            case "technology": return "it@university.edu";
            case "transportation": return "transport@university.edu";
            default: return "general@university.edu";
        }
    }

    private String normalizeStatus(String status) {
        if (status == null) return "pending";
        switch (status) {
            case "Pending": return "pending";
            case "In Progress":
            case "in-progress": return "in-progress";
            case "Sent to Department": return "sent-to-dept";
            case "Department Confirmed": return "dept-confirmed";
            case "Resolved": return "resolved";
            default: return "pending";
        }
    }

    private String defaultString(String s) {
        return s != null ? s : "";
    }
}
