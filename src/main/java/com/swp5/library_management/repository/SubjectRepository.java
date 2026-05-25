package com.swp5.library_management.repository;

import com.swp5.library_management.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, String> {
    // PK is SubjectCode (String) — findById(subjectCode) is available from JpaRepository
}
