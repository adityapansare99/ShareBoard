package com.cb.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "board_members",
       uniqueConstraints = @UniqueConstraint(columnNames = {"board_id", "user_email"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BoardMember { // Tracks user access to boards
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @Column(name = "user_email", nullable = false, length = 254)
    private String userEmail;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
    }
}
