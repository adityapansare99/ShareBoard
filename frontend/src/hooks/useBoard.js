import { useState, useEffect, useCallback, useRef } from 'react';
import { fetchBoard } from '../services/api';

export function useBoard(boardCode, view = 'kanban') {
  const [board, setBoard] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const boardRef = useRef(board);
  boardRef.current = board;

  const loadBoard = useCallback((opts = {}) => {
    const isRetry = opts.isRetry === true;
    if (!isRetry) setLoading(true);
    setError(null);
    fetchBoard(boardCode, view)
      .then(data => {
        setBoard(data);
        setLoading(false);
      })
      .catch(err => {
        if (err.response?.status === 429) {
          // Transient throttle — never boot the user to the dashboard screen.
          // Keep the current board visible and retry once after the backoff.
          if (boardRef.current) setLoading(false);
          if (!isRetry) {
            const retryAfterMs = err.response?.data?.retryAfterMs || 1500;
            setTimeout(() => loadBoard({ isRetry: true }), retryAfterMs);
          } else if (!boardRef.current) {
            setError('Server is busy — please try again in a moment.');
          }
          return;
        }
        setError(err.response?.data?.message || err.message || 'Failed to load board');
        setLoading(false);
      });
  }, [boardCode, view]);

  useEffect(() => {
    loadBoard();
  }, [loadBoard]);

  return { board, loading, error, reload: loadBoard, setBoard };
}
