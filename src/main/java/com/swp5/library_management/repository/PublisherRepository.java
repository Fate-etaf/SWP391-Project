package com.swp5.library_management.repository;

import com.swp5.library_management.entity.Publisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PublisherRepository extends JpaRepository<Publisher, Integer> {

    Optional<Publisher> findByPublisherNameIgnoreCase(String publisherName);
}
