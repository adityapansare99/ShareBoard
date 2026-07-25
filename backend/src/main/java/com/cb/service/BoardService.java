package com.cb.service;

import com.cb.config.UserContext;
import com.cb.dto.BoardDTO;
import com.cb.dto.BoardSnapshot;
import com.cb.model.Board;
import com.cb.model.BoardColumn;
import com.cb.model.BoardMember;
import com.cb.patterns.strategy.BoardViewStrategy;
import com.cb.repository.BoardMemberRepository;
import com.cb.repository.BoardRepository;
import com.cb.repository.ColumnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final ColumnRepository columnRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final Map<String, BoardViewStrategy> viewStrategies;
    private final UserContext userContext;

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public Board createBoard(String name, String description) {
        String email = userContext.getCurrentUserEmail();
        Board board = Board.builder()
            .name(name)
            .code(generateCode())
            .description(description)
            .ownerEmail(email)
            .build();
        board = boardRepository.save(board);

        // Create default columns
        String[] defaultColumns = {"To Do", "In Progress", "Review", "Done"};
        for (int i = 0; i < defaultColumns.length; i++) {
            BoardColumn col = BoardColumn.builder()
                .name(defaultColumns[i])
                .position(i)
                .board(board)
                .build();
            columnRepository.save(col);
        }

        // Auto-add creator as a board member
        BoardMember member = BoardMember.builder()
            .board(board)
            .userEmail(email)
            .build();
        boardMemberRepository.save(member);

        return boardRepository.findByCodeWithColumns(board.getCode())
            .orElse(board);
    }

    @Transactional(readOnly = true)
    public Board getBoard(String code) {
        return boardRepository.findByCodeWithColumns(code)
            .orElseThrow(() -> new RuntimeException("Board not found: " + code));
    }

    @Transactional(readOnly = true)
    public BoardSnapshot getBoardSnapshot(String code, String view) {
        Board board = getBoard(code);

        if (view == null || view.equals("kanban")) {
            return BoardSnapshot.from(board);
        }

        BoardViewStrategy strategy = viewStrategies.get(view + "ViewStrategy");
        if (strategy != null) {
            return strategy.render(board);
        }

        return BoardSnapshot.from(board);
    }

    @Transactional(readOnly = true)
    public List<BoardDTO> listBoards() {
        String email = userContext.getCurrentUserEmail();
        return boardRepository.findAccessibleBoards(email).stream()
            .map(b -> new BoardDTO(b.getId(), b.getName(), b.getCode(),
                b.getDescription(), b.getCreatedAt(), b.getUpdatedAt()))
            .collect(Collectors.toList());
    }

    /**
     * Joins the current user to a board by its share code.
     * If they are already a member, this is a no-op and simply returns the board.
     */
    @Transactional
    public Board joinBoard(String code) {
        Board board = getBoard(code);
        String email = userContext.getCurrentUserEmail();

        if (!boardMemberRepository.existsByBoardIdAndUserEmail(board.getId(), email)) {
            BoardMember member = BoardMember.builder()
                .board(board)
                .userEmail(email)
                .build();
            boardMemberRepository.save(member);
        }

        return board;
    }

    @Transactional
    public void deleteBoard(String code) {
        Board board = getBoard(code);
        boardRepository.delete(board);
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            code.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        // Ensure uniqueness
        if (boardRepository.findByCode(code.toString()).isPresent()) {
            return generateCode();
        }
        return code.toString();
    }
}

