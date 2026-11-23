package com.mosek.classFlow.repository;

import com.mosek.classFlow.entity.TimetableSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TimetableSlotRepository extends JpaRepository<TimetableSlot, UUID> {
    List<TimetableSlot> findByAcademicClassIdOrderByDayOfWeekAscStartTimeAsc(UUID academicClassId);
}
