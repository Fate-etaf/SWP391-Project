package com.swp5.library_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swp5.library_management.entity.Subject;/**
 * Repository cho bảng Subjects.
 * Dùng để populate dropdown chọn mã môn học (UCG01 – A1: Advanced search by Subject Code).
 */
@Repository
public interface SubjectRepository extends JpaRepository<Subject, String> {
    // PK is SubjectCode (String) — findById(subjectCode) is available from JpaRepository

}
