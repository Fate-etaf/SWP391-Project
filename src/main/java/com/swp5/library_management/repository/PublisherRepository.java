package com.swp5.library_management.repository;

import com.swp5.library_management.entity.Publisher;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;


public interface PublisherRepository extends JpaRepository<Publisher, Integer> {

    Optional<Publisher> findByPublisherNameIgnoreCase(String publisherName);
}
