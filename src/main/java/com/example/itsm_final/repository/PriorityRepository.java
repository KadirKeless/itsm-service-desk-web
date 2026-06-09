package com.example.itsm_final.repository;

import com.example.itsm_final.model.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PriorityRepository extends JpaRepository<Priority, Integer> {

    List<Priority> findAllByOrderByLevelWeightAsc();

    Optional<Priority> findByPriorityName(String priorityName);
}
