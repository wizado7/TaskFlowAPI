package com.tasktracker.task.repository;

import com.tasktracker.task.entity.BoardMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BoardMemberRepository extends JpaRepository<BoardMember, UUID> {
    Optional<BoardMember> findByBoardIdAndUserEmail(UUID boardId, String userEmail);
    List<BoardMember> findByBoardId(UUID boardId);
    List<BoardMember> findByUserEmail(String userEmail);
    void deleteByBoardIdIn(List<UUID> boardIds);
}
