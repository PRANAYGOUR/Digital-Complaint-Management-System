package com.dcs.controller;

import com.dcs.model.Complaint;
import com.dcs.model.User;
import com.dcs.service.ComplaintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import jakarta.servlet.http.HttpSession;
import java.util.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/dept")
public class DepartmentController {
    @Autowired
    private ComplaintService complaintService;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User dept = requireDepartment(session);
        if (dept == null) return "redirect:/index.html";
        List<Complaint> complaints = filterByDepartment(complaintService.getAllComplaints(), dept);
        model.addAttribute("complaints", complaints);
        model.addAttribute("department", dept.getDepartment());
        return "dept_dashboard";
    }

    @GetMapping(path = "/api/complaints", produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> list(HttpSession session) {
        User dept = requireDepartment(session);
        if (dept == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
        }
        List<Complaint> complaints = filterByDepartment(complaintService.getAllComplaints(), dept);
        List<Map<String, Object>> data = complaints.stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("title", defaultString(c.getTitle()));
            m.put("category", defaultString(c.getCategory()));
            m.put("description", defaultString(c.getDescription()));
            m.put("status", defaultString(c.getStatus()));
            m.put("submittedAt", c.getCreatedAt() != null ? c.getCreatedAt().toLocalDate().toString() : "");
            m.put("departmentEmail", defaultString(c.getDepartmentEmail()));
            return m;
        }).collect(java.util.stream.Collectors.toList());
        Map<String, Object> resp = new HashMap<>();
        resp.put("complaints", data);
        return ResponseEntity.ok(resp);
    }

    // Department manage endpoint: can update status, not allowed for admin
    @PostMapping(path = "/api/update/{id}", consumes = MediaType.ALL_VALUE, produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> update(HttpSession session, @PathVariable Long id,
                                    @RequestParam(required=false) String status,
                                    @RequestBody(required=false) Map<String, String> payload) {
        User dept = requireDepartment(session);
        if (dept == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
        }
        var opt = complaintService.getById(id);
        if (opt.isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Complaint not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
        }
        Complaint c = opt.get();
        if (!normalizeToken(dept.getDepartment()).equals(normalizeToken(c.getCategory()))) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Cannot manage other department complaints");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err);
        }
        String statusToken = status;
        if ((statusToken == null || statusToken.isBlank()) && payload != null) {
            statusToken = payload.getOrDefault("status", "");
        }
        if (statusToken == null || statusToken.isBlank()) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Missing status");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
        }
        c.setStatus(toDbStatus(statusToken));
        complaintService.updateStatus(c, c.getStatus());
        complaintService.upsertDepartmentStatus(c, complaintService.mapToDeptStatus(statusToken), "Updated by department");
        Map<String, Object> ok = new HashMap<>();
        ok.put("success", true);
        ok.put("id", c.getId());
        ok.put("status", c.getStatus());
        return ResponseEntity.ok(ok);
    }

    // New: Form submission endpoint returns redirect with flash messages
    @PostMapping(path = "/update/{id}")
    public String updateForm(HttpSession session, @PathVariable Long id,
                             @RequestParam String status,
                             RedirectAttributes redirect) {
        User dept = requireDepartment(session);
        if (dept == null) {
            redirect.addFlashAttribute("error", "Not authenticated");
            return "redirect:/auth/login";
        }
        var opt = complaintService.getById(id);
        if (opt.isEmpty()) {
            redirect.addFlashAttribute("error", "Complaint not found");
            return "redirect:/dept/dashboard";
        }
        Complaint c = opt.get();
        if (!normalizeToken(dept.getDepartment()).equals(normalizeToken(c.getCategory()))) {
            redirect.addFlashAttribute("error", "Cannot manage other department complaints");
            return "redirect:/dept/dashboard";
        }
        String statusToken = status;
        if (statusToken == null || statusToken.isBlank()) {
            redirect.addFlashAttribute("error", "Missing status");
            return "redirect:/dept/dashboard";
        }
        c.setStatus(toDbStatus(statusToken));
        complaintService.updateStatus(c, c.getStatus());
        complaintService.upsertDepartmentStatus(c, complaintService.mapToDeptStatus(statusToken), "Updated by department");
        redirect.addFlashAttribute("success", "Updated complaint #" + c.getId() + " to " + c.getStatus());
        return "redirect:/dept/dashboard";
    }

    private User requireDepartment(HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (userObj instanceof User) {
            User u = (User) userObj;
            if (u.getRole() == User.Role.DEPARTMENT && u.getDepartment() != null && !u.getDepartment().isBlank()) {
                return u;
            }
        }
        return null;
    }

    private List<Complaint> filterByDepartment(List<Complaint> complaints, User dept) {
        final String deptKey = normalizeToken(dept.getDepartment());
        return complaints.stream()
                .filter(c -> normalizeToken(c.getCategory()).equals(deptKey))
                .collect(java.util.stream.Collectors.toList());
    }

    private String defaultString(String s) { return s != null ? s : ""; }

    // Normalize token to DB status title-case
    private String toDbStatus(String statusToken) {
        if (statusToken == null) return "Pending";
        switch (statusToken) {
            case "pending": return "Pending";
            case "in-progress": return "In Progress";
            case "sent-to-dept": return "Sent to Department";
            case "dept-confirmed": return "Department Confirmed";
            case "resolved": return "Resolved";
            default: return "Pending";
        }
    }
    private String normalizeToken(String s) {
        return defaultString(s).toLowerCase().replaceAll("[\\s_-]+", "");
    }
}