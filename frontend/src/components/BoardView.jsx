import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useBoard } from '../hooks/useBoard';
import { createCard, moveCard, getHistory, undo, redo } from '../services/api';
import wsClient from '../services/ws';
import Column from './Column';
import CardModal from './CardModal';
import ActivityPanel from './ActivityPanel';

let sessionCounter = 0;

export default function BoardView() {
  const { code } = useParams();
  const navigate = useNavigate();
  const [view, setView] = useState('kanban');
  const { board, loading, error, reload, setBoard } = useBoard(code, view);
  const [sessionId] = useState(() => `session_${Date.now()}_${++sessionCounter}`);
  const [undoInfo, setUndoInfo] = useState({ undoCount: 0, redoCount: 0 });
  const [editCard, setEditCard] = useState(null);
  const [connected, setConnected] = useState(false);
  const [draggedCard, setDraggedCard] = useState(null);
  const [newCardTitles, setNewCardTitles] = useState({});
  const [showActivity, setShowActivity] = useState(false);
  const boardRef = useRef(board);
  boardRef.current = board;

  useEffect(() => {
    wsClient.connect(code, sessionId);

    const unsubConnected = wsClient.on('connected', () => setConnected(true));
    const unsubDisconnected = wsClient.on('disconnected', () => setConnected(false));

    const unsubCardAdded = wsClient.on('CARD_ADDED', (data) => {
      setBoard(prev => {
        if (!prev) return prev;
        const cols = prev.columns.map(col => {
          if (col.id !== data.card.columnId) return col;
          const exists = col.cards.find(c => c.id === data.card.id);
          if (exists) return col;
          return { ...col, cards: [...col.cards, data.card] };
        });
        return { ...prev, columns: cols };
      });
    });

    const unsubCardMoved = wsClient.on('CARD_MOVED', () => reload());
    const unsubCardEdited = wsClient.on('CARD_EDITED', () => reload());

    const unsubCardDeleted = wsClient.on('CARD_DELETED', (data) => {
      setBoard(prev => {
        if (!prev) return prev;
        const cols = prev.columns.map(col => ({
          ...col,
          cards: col.cards.filter(c => c.id !== data.cardId),
        }));
        return { ...prev, columns: cols };
      });
    });

    const unsubStateChanged = wsClient.on('STATE_CHANGED', () => reload());

    return () => {
      unsubConnected();
      unsubDisconnected();
      unsubCardAdded();
      unsubCardMoved();
      unsubCardEdited();
      unsubCardDeleted();
      unsubStateChanged();
      wsClient.disconnect();
    };
  }, [code, sessionId, reload, setBoard]);

  const updateUndoHistory = useCallback(() => {
    getHistory(code, sessionId)
      .then(setUndoInfo)
      .catch(() => {});
  }, [code, sessionId]);

  useEffect(() => {
    updateUndoHistory();
  }, [updateUndoHistory]);

  function handleUndo() {
    undo(code, sessionId)
      .then(res => {
        if (res.history) setUndoInfo(res.history);
        reload();
      })
      .catch(() => {});
  }

  function handleRedo() {
    redo(code, sessionId)
      .then(res => {
        if (res.history) setUndoInfo(res.history);
        reload();
      })
      .catch(() => {});
  }

  function handleDragStart(card) {
    setDraggedCard(card);
  }

  function handleDrop(targetColumnId) {
    if (!draggedCard) return;
    const sourceColId = draggedCard.columnId;
    const cardToMove = draggedCard;
    setDraggedCard(null);

    const currentBoard = boardRef.current;
    if (!currentBoard) return;

    const targetCol = currentBoard.columns.find(c => c.id === targetColumnId);
    const newPos = targetCol ? targetCol.cards.length : 0;

    setBoard(prev => {
      if (!prev) return prev;
      const updatedCols = prev.columns.map(col => {
        if (col.id === sourceColId) {
          return { ...col, cards: col.cards.filter(c => c.id !== cardToMove.id) };
        }
        if (col.id === targetColumnId) {
          const cardCopy = { ...cardToMove, columnId: targetColumnId, position: newPos };
          return { ...col, cards: [...col.cards, cardCopy] };
        }
        return col;
      });
      return { ...prev, columns: updatedCols };
    });

    moveCard(code, {
      cardId: cardToMove.id,
      toColumnId: targetColumnId,
      newPosition: newPos,
      sessionId,
    })
      .then(() => {
        updateUndoHistory();
        reload();
      })
      .catch(err => {
        // On a real conflict, reload to revert the optimistic move. On a 429 the
        // request was rejected pre-controller (and the interceptor already retried),
        // so cascading another reload just competes for a depleted bucket — skip it.
        if (err.response?.status !== 429) reload();
      });
  }

  function handleCreateCard(columnId) {
    const title = (newCardTitles[columnId] || '').trim();
    if (!title) return;
    createCard(code, columnId, title, sessionId)
      .then(card => {
        setBoard(prev => {
          if (!prev) return prev;
          const cols = prev.columns.map(col => {
            if (col.id !== columnId) return col;
            if (col.cards.find(c => c.id === card.id)) return col;
            return { ...col, cards: [...col.cards, card] };
          });
          return { ...prev, columns: cols };
        });
        setNewCardTitles(prev => ({ ...prev, [columnId]: '' }));
        updateUndoHistory();
      })
      .catch(() => {});
  }

  if (loading) return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center">
      <div className="flex items-center gap-3 text-slate-600 font-medium text-sm">
        <span className="w-5 h-5 border-2 border-indigo-600 border-t-transparent rounded-full animate-spin" />
        Loading workspace...
      </div>
    </div>
  );

  if (error) return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
      <div className="bg-white p-8 rounded-2xl border border-slate-200 shadow-md text-center max-w-sm">
        <p className="text-rose-600 font-semibold mb-4 text-sm">{error}</p>
        <button onClick={() => navigate('/')} className="px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-semibold">
          Back to Dashboard
        </button>
      </div>
    </div>
  );

  return (
    <div className="min-h-screen bg-slate-100 flex flex-col">
      {/* Header Bar */}
      <header className="sticky top-0 z-30 bg-white border-b border-slate-200 px-6 py-3.5 flex items-center justify-between shadow-xs">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate('/')}
            className="p-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-600 hover:text-slate-900 transition-colors"
            title="Back to Dashboard"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
          </button>

          <div className="flex items-center gap-3">
            <h1 className="font-heading font-extrabold text-lg text-slate-900 tracking-tight">
              {board.name}
            </h1>
            <span className="text-[11px] font-mono bg-indigo-50 border border-indigo-200 text-indigo-700 px-2.5 py-0.5 rounded-full font-semibold">
              {board.code}
            </span>
            <div className="flex items-center gap-1.5 bg-slate-100 border border-slate-200 px-2.5 py-1 rounded-full">
              <span className={`w-2 h-2 rounded-full ${connected ? 'bg-emerald-500' : 'bg-rose-500'}`} />
              <span className="text-[10px] uppercase font-bold tracking-wider text-slate-600">
                {connected ? 'Live' : 'Offline'}
              </span>
            </div>
          </div>
        </div>

        {/* View Switcher & Undo/Redo & Activity */}
        <div className="flex items-center gap-3">
          {/* Segmented View Switcher */}
          <div className="flex bg-slate-100 border border-slate-200 rounded-xl p-1">
            {['kanban', 'calendar', 'list'].map(v => (
              <button
                key={v}
                onClick={() => setView(v)}
                className={`px-3 py-1 rounded-lg text-xs font-semibold capitalize transition-all ${
                  view === v
                    ? 'bg-white text-indigo-700 shadow-sm border border-slate-200'
                    : 'text-slate-500 hover:text-slate-800'
                }`}
              >
                {v === 'kanban' ? 'Board' : v}
              </button>
            ))}
          </div>

          {/* Undo/Redo Buttons */}
          <div className="flex items-center gap-1 bg-slate-100 border border-slate-200 p-1 rounded-xl">
            <button
              onClick={handleUndo}
              disabled={undoInfo.undoCount === 0}
              className={`px-2.5 py-1 text-xs font-semibold rounded-lg transition-all flex items-center gap-1.5 ${
                undoInfo.undoCount > 0
                  ? 'bg-white text-slate-800 hover:text-indigo-600 shadow-xs border border-slate-200'
                  : 'text-slate-400 cursor-not-allowed opacity-50'
              }`}
            >
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 10h10a8 8 0 018 8v2M3 10l6 6m-6-6l6-6" />
              </svg>
              Undo <span className="font-mono text-[10px] bg-slate-100 text-slate-700 px-1.5 py-0.5 rounded border border-slate-200">{undoInfo.undoCount}</span>
            </button>

            <button
              onClick={handleRedo}
              disabled={undoInfo.redoCount === 0}
              className={`px-2.5 py-1 text-xs font-semibold rounded-lg transition-all flex items-center gap-1.5 ${
                undoInfo.redoCount > 0
                  ? 'bg-white text-slate-800 hover:text-indigo-600 shadow-xs border border-slate-200'
                  : 'text-slate-400 cursor-not-allowed opacity-50'
              }`}
            >
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 10H11a8 8 0 00-8 8v2m18-10l-6 6m6-6l-6-6" />
              </svg>
              Redo <span className="font-mono text-[10px] bg-slate-100 text-slate-700 px-1.5 py-0.5 rounded border border-slate-200">{undoInfo.redoCount}</span>
            </button>
          </div>

          {/* Activity Log Button */}
          <button
            onClick={() => setShowActivity(true)}
            className="px-3 py-1.5 text-xs font-semibold rounded-xl bg-white border border-slate-200 text-slate-700 hover:text-indigo-600 hover:border-indigo-300 shadow-xs transition-all flex items-center gap-1.5"
          >
            <svg className="w-4 h-4 text-slate-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            Activity
          </button>
        </div>
      </header>

      {/* Main Board Container */}
      <main className="flex-1 overflow-x-auto p-6">
        <div className="flex gap-6 h-full" style={{ minHeight: 0 }}>
          {board.columns.map(col => (
            <Column
              key={col.id}
              column={col}
              view={view}
              onDragStart={handleDragStart}
              onDrop={handleDrop}
              onCardClick={setEditCard}
              newCardTitle={newCardTitles[col.id] || ''}
              onNewCardTitleChange={val => setNewCardTitles(prev => ({ ...prev, [col.id]: val }))}
              onCreateCard={() => handleCreateCard(col.id)}
            />
          ))}
        </div>
      </main>

      {/* Card Edit Modal */}
      {editCard && (
        <CardModal
          card={editCard}
          boardCode={code}
          sessionId={sessionId}
          onClose={() => setEditCard(null)}
          onSaved={() => { setEditCard(null); reload(); updateUndoHistory(); }}
          onDeleted={() => { setEditCard(null); reload(); updateUndoHistory(); }}
        />
      )}

      {/* Activity History Panel */}
      <ActivityPanel
        boardCode={code}
        isOpen={showActivity}
        onClose={() => setShowActivity(false)}
      />
    </div>
  );
}
