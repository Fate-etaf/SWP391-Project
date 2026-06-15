package com.swp5.library_management.repository;

import com.swp5.library_management.entity.TransferDetail;
import com.swp5.library_management.entity.TransferDetailId;
import org.springframework.data.jpa.repository.JpaRepository;



public interface TransferDetailRepository extends JpaRepository<TransferDetail, TransferDetailId> {
}
