package com.cb.controller;

import com.cb.dto.BoardDTO;
import com.cb.dto.BoardSnapshot;
import com.cb.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @GetMapping
    public ResponseEntity<List<BoardDTO>> listBoards() {
        return ResponseEntity.ok(boardService.listBoards());
    }

    @PostMapping
    public ResponseEntity<BoardDTO> createBoard(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String description = body.getOrDefault("description", "");
        var board = boardService.createBoard(name, description);
        return ResponseEntity.ok(new BoardDTO(
            board.getId(), board.getName(), board.getCode(),
            board.getDescription(), board.getCreatedAt(), board.getUpdatedAt()
        ));
    }

    @PostMapping("/join")
    public ResponseEntity<BoardDTO> joinBoard(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        var board = boardService.joinBoard(code);
        return ResponseEntity.ok(new BoardDTO(
            board.getId(), board.getName(), board.getCode(),
            board.getDescription(), board.getCreatedAt(), board.getUpdatedAt()
        ));
    }

    @GetMapping("/{code}")
    public ResponseEntity<BoardSnapshot> getBoard(
            @PathVariable String code,
            @RequestParam(required = false, defaultValue = "kanban") String view) {
        return ResponseEntity.ok(boardService.getBoardSnapshot(code, view));
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> deleteBoard(@PathVariable String code) {
        boardService.deleteBoard(code);
        return ResponseEntity.noContent().build();
    }
}

