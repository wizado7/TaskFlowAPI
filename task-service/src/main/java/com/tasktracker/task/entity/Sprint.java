package com.tasktracker.task.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sprints")
@Getter
@Setter
public class Sprint {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "board_id", nullable = false)
    private UUID boardId;

    @Column(nullable = false)
    private String name;

    private LocalDate startDate;

    private LocalDate endDate;

    @Column(nullable = false)
    private boolean active = false;

    private String goal;

    @Column(name = "capacity_points")
    private Integer capacityPoints;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SprintStatus status = SprintStatus.PLANNED;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
