package com.dcs.config;

import com.dcs.model.User;
import com.dcs.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initUsers(UserRepository userRepository) {
        return args -> {
            userRepository.findByUsername("admin").ifPresentOrElse(
                existing -> {},
                () -> {
                    User admin = new User();
                    admin.setUsername("admin");
                    admin.setPassword("admin123"); // For development only; consider hashing in production
                    admin.setEmail("admin@example.com");
                    admin.setRole(User.Role.ADMIN);
                    userRepository.save(admin);
                    System.out.println("Seeded default ADMIN user: username='admin', password='admin123'");
                }
            );

            userRepository.findByUsername("student").ifPresentOrElse(
                existing -> {},
                () -> {
                    User student = new User();
                    student.setUsername("student");
                    student.setPassword("student123");
                    student.setEmail("student@example.com");
                    student.setRole(User.Role.STUDENT);
                    userRepository.save(student);
                    System.out.println("Seeded default STUDENT user: username='student', password='student123'");
                }
            );

            userRepository.findByUsername("tech").ifPresentOrElse(
                existing -> {
                    boolean changed = false;
                    if (existing.getRole() != User.Role.DEPARTMENT) {
                        existing.setRole(User.Role.DEPARTMENT);
                        changed = true;
                    }
                    if (existing.getDepartment() == null || existing.getDepartment().isBlank()) {
                        existing.setDepartment("technology");
                        changed = true;
                    }
                    // Ensure email is set for existing tech user so email login works
                    if (existing.getEmail() == null || existing.getEmail().isBlank()) {
                        existing.setEmail("it@university.edu");
                        changed = true;
                    }
                    // Ensure password is set if missing (do not override if already set)
                    if (existing.getPassword() == null || existing.getPassword().isBlank()) {
                        existing.setPassword("tech123");
                        changed = true;
                    }
                    if (changed) {
                        userRepository.save(existing);
                        System.out.println("Updated existing DEPARTMENT user: username='tech', role='DEPARTMENT', department='technology', email='it@university.edu'");
                    }
                },
                () -> {
                    User dept = new User();
                    dept.setUsername("tech");
                    dept.setPassword("tech123");
                    dept.setEmail("it@university.edu");
                    dept.setRole(User.Role.DEPARTMENT);
                    dept.setDepartment("technology");
                    userRepository.save(dept);
                    System.out.println("Seeded default DEPARTMENT user: username='tech', password='tech123', department='technology'");
                }
            );

            // Seed Facilities department account
            userRepository.findByUsername("facilities").ifPresentOrElse(
                existing -> {
                    boolean changed = false;
                    if (existing.getRole() != User.Role.DEPARTMENT) {
                        existing.setRole(User.Role.DEPARTMENT);
                        changed = true;
                    }
                    if (existing.getDepartment() == null || existing.getDepartment().isBlank()) {
                        existing.setDepartment("facilities");
                        changed = true;
                    }
                    if (existing.getEmail() == null || existing.getEmail().isBlank()) {
                        existing.setEmail("facilities@university.edu");
                        changed = true;
                    }
                    if (existing.getPassword() == null || existing.getPassword().isBlank()) {
                        existing.setPassword("facilities123");
                        changed = true;
                    }
                    if (changed) {
                        userRepository.save(existing);
                        System.out.println("Updated existing DEPARTMENT user: username='facilities', role='DEPARTMENT', department='facilities', email='facilities@university.edu'");
                    }
                },
                () -> {
                    User facilities = new User();
                    facilities.setUsername("facilities");
                    facilities.setPassword("facilities123");
                    facilities.setEmail("facilities@university.edu");
                    facilities.setRole(User.Role.DEPARTMENT);
                    facilities.setDepartment("facilities");
                    userRepository.save(facilities);
                    System.out.println("Seeded default DEPARTMENT user: username='facilities', password='facilities123', department='facilities'");
                }
            );
        };
    }
}