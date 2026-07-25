package com.cb.repository;

import com.cb.model.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
    Optional<Board> findByCode(String code);

    @Query("SELECT b FROM Board b LEFT JOIN FETCH b.columns WHERE b.code = :code")
    Optional<Board> findByCodeWithColumns(String code);

    List<Board> findByOwnerEmailOrderByCreatedAtDesc(String ownerEmail);

    /** Returns boards the user owns OR is a member of, ordered by most recent first. */
    @Query("SELECT DISTINCT b FROM Board b LEFT JOIN BoardMember m ON b.id = m.board.id " +
           "WHERE b.ownerEmail = :email OR m.userEmail = :email " +
           "ORDER BY b.createdAt DESC")
    List<Board> findAccessibleBoards(@Param("email") String email);
}

