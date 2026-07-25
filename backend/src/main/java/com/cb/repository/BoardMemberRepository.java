package com.cb.repository;

import com.cb.model.BoardMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BoardMemberRepository extends JpaRepository<BoardMember, Long> {

    boolean existsByBoardIdAndUserEmail(Long boardId, String userEmail);

    Optional<BoardMember> findByBoardIdAndUserEmail(Long boardId, String userEmail);
}
