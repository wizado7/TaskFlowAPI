package com.tasktracker.task.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "board_columns")
@Getter
@Setter
public class BoardColumn {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "board_id", nullable = false)
    private UUID boardId;

    @Column(nullable = false)
    private String name;

    @Column(name = "position", nullable = false)
    private int position;
}
