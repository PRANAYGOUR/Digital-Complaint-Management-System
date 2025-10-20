package com.dcs.repository;

import com.dcs.model.Complaint;
import com.dcs.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByUser(User user);
    List<Complaint> findByCategoryIgnoreCase(String category);
}
