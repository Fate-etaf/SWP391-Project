package com.swp5.library_management.repository;

import com.swp5.library_management.entity.StudyRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyRoomRepository extends JpaRepository<StudyRoom, Integer> {
    List<StudyRoom> findByCampus_CampusIdAndStatus(Integer campusId, String status);
    List<StudyRoom> findByCampus_CampusId(Integer campusId);
}
