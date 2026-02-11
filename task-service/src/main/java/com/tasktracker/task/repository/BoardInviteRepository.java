package com.tasktracker.task.repository;

import com.tasktracker.task.entity.BoardInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BoardInviteRepository extends JpaRepository<BoardInvite, UUID> {
    Optional<BoardInvite> findByToken(String token);
    void deleteByBoardIdIn(Iterable<UUID> boardIds);
}
