package com.dcs.controller;

import com.dcs.model.Complaint;
import com.dcs.service.ComplaintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashMap;

@Controller
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    private ComplaintService complaintService;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Object userObj = session.getAttribute("user");
        if (userObj == null || !(userObj instanceof com.dcs.model.User) || ((com.dcs.model.User) userObj).getRole() != com.dcs.model.User.Role.ADMIN) {
            return "redirect:/auth/login";
        }
        List<Complaint> all = complaintService.getAllComplaints();
        List<Complaint> pending = all.stream()
                .filter(c -> normalizeStatus(c.getStatus()).equals("pending"))
                .collect(Collectors.toList());
        model.addAttribute("complaints", all);
        model.addAttribute("pendingComplaints", pending);
        return "admin_dashboard";
    }

    // Admin can send/push email notification to department; this does not change status directly
    @PostMapping("/send/{id}")
    public String sendEmail(@PathVariable Long id, RedirectAttributes redirectAttributes, HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (userObj == null || !(userObj instanceof com.dcs.model.User) || ((com.dcs.model.User) userObj).getRole() != com.dcs.model.User.Role.ADMIN) {
            redirectAttributes.addFlashAttribute("error", "Not authenticated");
            return "redirect:/auth/login";
        }
        var opt = complaintService.getById(id);
        if(opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Complaint not found");
            return "redirect:/admin/dashboard";
        }
        Complaint c = opt.get();
        try {
            complaintService.sendEmailToDepartment(c);
            complaintService.upsertDepartmentStatus(c, "Pending", "Prioritized by Admin - notification sent to department");
            redirectAttributes.addFlashAttribute("success", "Notification sent to department.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Failed to send email: " + ex.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    // Admin cannot confirm department actions; only departments can manage status
    @GetMapping("/confirm/{id}")
    public String confirmDepartment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "Admins cannot confirm department actions. Please ask the department to manage status.");
        return "redirect:/admin/dashboard";
    }

    // Admin cannot modify complaint status via server-rendered endpoint
    @PostMapping("/update/{id}")
    public String updateStatus(@PathVariable Long id, @RequestParam String status, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "Admins cannot modify complaint status.");
        return "redirect:/admin/dashboard";
    }

    @GetMapping(path = "/api/complaints", produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> listComplaints(HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (userObj == null || !(userObj instanceof com.dcs.model.User) || ((com.dcs.model.User) userObj).getRole() != com.dcs.model.User.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }
        List<Complaint> complaints = complaintService.getAllComplaints();
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
            m.put("studentEmail", (c.getUser() != null && c.getUser().getEmail() != null) ? c.getUser().getEmail() : "");
            Map<String, String> info = complaintService.getDepartmentInfo(c);
            m.put("departmentStatus", info.get("departmentStatus"));
            m.put("departmentRemarks", info.get("departmentRemarks"));
            return m;
        }).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(Map.of("complaints", data));
    }

    // JSON API: Admin push/prioritize unattended complaints
    @PostMapping(path = "/api/push/{id}", produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> pushComplaint(@PathVariable Long id, HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (userObj == null || !(userObj instanceof com.dcs.model.User) || ((com.dcs.model.User) userObj).getRole() != com.dcs.model.User.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }
        var opt = complaintService.getById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Complaint not found"));
        }
        Complaint c = opt.get();
        try {
            complaintService.sendEmailToDepartment(c);
            complaintService.upsertDepartmentStatus(c, "Pending", "Prioritized by Admin - notification sent to department");
            return ResponseEntity.ok(Map.of("success", true, "id", c.getId()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping(path = "/api/update/{id}", consumes = MediaType.ALL_VALUE, produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> updateStatusApi(@PathVariable Long id,
                                             @RequestParam(required = false) String status,
                                             @RequestBody(required = false) Map<String, String> payload,
                                             HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (userObj == null || !(userObj instanceof com.dcs.model.User) || ((com.dcs.model.User) userObj).getRole() != com.dcs.model.User.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }
        // Admin cannot modify complaint status via JSON API
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Admins cannot modify complaint status"));
    }

    private String normalizeStatus(String status) {
        if (status == null) return "pending";
        switch (status) {
            case "Pending":
            case "pending": return "pending";
            case "In Progress":
            case "in-progress": return "in-progress";
            case "Sent to Department":
            case "sent-to-dept": return "sent-to-dept";
            case "Department Confirmed":
            case "dept-confirmed": return "dept-confirmed";
            case "Resolved":
            case "resolved": return "resolved";
            default: return "pending";
        }
    }

    private String defaultString(String s) {
        return s != null ? s : "";
    }

    // Map normalized token to DB-friendly title case status
    private String toDbStatus(String statusToken) {
        if (statusToken == null) return "Pending";
        switch (statusToken) {
            case "pending": return "Pending";
            case "sent-to-dept": return "Sent to Department";
            case "dept-confirmed": return "Department Confirmed";
            case "resolved": return "Resolved";
            default: return "Pending";
        }
    }
}
