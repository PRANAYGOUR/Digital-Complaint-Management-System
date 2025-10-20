package com.dcs.service;

import com.dcs.model.Complaint;
import com.dcs.model.User;
import com.dcs.repository.ComplaintRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.sql.*;

import com.dcs.config.JdbcConnector;

@Service
public class ComplaintService {
    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired(required=false)
    private JavaMailSender mailSender; // Optional, can be null if not configured

    @Autowired
    private JdbcConnector jdbcConnector;

    @Transactional
    public Complaint submitComplaint(Complaint complaint) {
        return complaintRepository.save(complaint);
    }

    public List<Complaint> getComplaintsByUser(User user) {
        return complaintRepository.findByUser(user);
    }

    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAll();
    }

    public Optional<Complaint> getById(Long id) {
        return complaintRepository.findById(id);
    }

    @Transactional
    public Complaint updateStatus(Complaint complaint, String status) {
        complaint.setStatus(status);
        complaint.setUpdatedAt(java.time.LocalDateTime.now());
        if (status != null && ("Resolved".equalsIgnoreCase(status) || "resolved".equalsIgnoreCase(status))) {
            complaint.setResolvedAt(java.time.LocalDateTime.now());
        }
        return complaintRepository.save(complaint);
    }

    public void sendEmailToDepartment(Complaint complaint) {
        // In development, if mailSender is not configured, skip sending email gracefully
        if (mailSender == null) {
            return;
        }
        if (complaint.getDepartmentEmail() == null || complaint.getDepartmentEmail().isBlank()) {
            // If department email is missing, skip sending
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(complaint.getDepartmentEmail());
        message.setSubject("New Complaint: " + complaint.getTitle());
        message.setText("Complaint Details:\nTitle: " + complaint.getTitle() +
                "\nDescription: " + complaint.getDescription() +
                "\nConfirm at: http://localhost:8092/admin/confirm/" + complaint.getId());
        try {
            mailSender.send(message);
        } catch (Exception ex) {
            // In development, ignore mail sending failures to avoid breaking admin push
        }
    }

    // ===== Department tables integration (facilities_dept, food_services_dept, etc.) =====

    private String tableForCategory(String category) {
        String cat = (category == null || category.isBlank()) ? "other" : category.trim().toLowerCase();
        switch (cat) {
            case "facilities": return "facilities_dept";
            case "food-services": return "food_services_dept";
            case "academic": return "academic_dept";
            case "technology": return "technology_dept";
            case "transportation": return "transportation_dept";
            default: return "other_dept";
        }
    }

    public String mapToDeptStatus(String statusToken) {
        if (statusToken == null) return "Pending";
        switch (statusToken) {
            case "pending": return "Pending";
            case "in-progress": return "In Progress";
            case "sent-to-dept": return "Pending";
            case "dept-confirmed": return "Confirmed";
            case "resolved": return "Resolved";
            default: return "Pending";
        }
    }

    /**
     * Upsert department status/remarks for a complaint into the appropriate department table.
     */
    public void upsertDepartmentStatus(Complaint complaint, String deptStatus, String remarks) {
        String table = tableForCategory(complaint.getCategory());
        String status = (deptStatus == null || deptStatus.isBlank()) ? "Pending" : deptStatus;
        String note = (remarks == null) ? "" : remarks;
        try (Connection conn = jdbcConnector.getConnection()) {
            boolean exists = false;
            try (PreparedStatement check = conn.prepareStatement("SELECT id FROM " + table + " WHERE complaint_id = ?")) {
                check.setLong(1, complaint.getId());
                try (ResultSet rs = check.executeQuery()) {
                    exists = rs.next();
                }
            }
            if (exists) {
                try (PreparedStatement upd = conn.prepareStatement(
                        "UPDATE " + table + " SET department_status = ?, remarks = ?, updated_at = CURRENT_TIMESTAMP WHERE complaint_id = ?")) {
                    upd.setString(1, status);
                    upd.setString(2, note);
                    upd.setLong(3, complaint.getId());
                    upd.executeUpdate();
                }
            } else {
                try (PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO " + table + " (complaint_id, department_status, remarks) VALUES (?, ?, ?)")) {
                    ins.setLong(1, complaint.getId());
                    ins.setString(2, status);
                    ins.setString(3, note);
                    ins.executeUpdate();
                }
            }
        } catch (SQLException e) {
            // For this application, swallow exception and continue (could add logging later)
        }
    }

    /**
     * Fetch department status/remarks for a complaint.
     */
    public Map<String, String> getDepartmentInfo(Complaint complaint) {
        String table = tableForCategory(complaint.getCategory());
        Map<String, String> info = new HashMap<>();
        info.put("departmentStatus", "Pending");
        info.put("departmentRemarks", "");
        try (Connection conn = jdbcConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT department_status, remarks FROM " + table + " WHERE complaint_id = ?")) {
            ps.setLong(1, complaint.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    info.put("departmentStatus", rs.getString(1) != null ? rs.getString(1) : "Pending");
                    info.put("departmentRemarks", rs.getString(2) != null ? rs.getString(2) : "");
                }
            }
        } catch (SQLException e) {
            // default values already set
        }
        return info;
    }
}
