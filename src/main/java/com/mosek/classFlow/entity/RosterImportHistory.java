package com.mosek.classFlow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "roster_import_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RosterImportHistory extends BaseEntity {

    @Column(name = "academic_class_id", nullable = false)
    private UUID academicClassId;

    @Column(name = "imported_by", nullable = false)
    private UUID importedBy;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "imported_count", nullable = false)
    private int importedCount;

    @Column(name = "updated_count", nullable = false)
    private int updatedCount;
}
