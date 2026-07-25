package com.cb.repository;

import com.cb.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByColumnIdOrderByPosition(Long columnId);

    @Query("SELECT c FROM Card c LEFT JOIN FETCH c.dependsOn WHERE c.column.board.code = :boardCode")
    List<Card> findByBoardCodeWithDependencies(String boardCode);
}
