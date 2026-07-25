import React, { useState, useEffect } from 'react';
import { fetchBoardActivity } from '../services/api';

const ACTION_META = {
  CREATE_CARD:  { label: 'Created Card',    color: 'text-emerald-700', bg: 'bg-emerald-50 border-emerald-200' },
  MOVE_CARD:    { label: 'Moved Card',      color: 'text-indigo-700',  bg: 'bg-indigo-50 border-indigo-200' },
  EDIT_CARD:    { label: 'Edited Card',     color: 'text-amber-800',   bg: 'bg-amber-50 border-amber-200' },
  DELETE_CARD:  { label: 'Deleted Card',    color: 'text-rose-700',    bg: 'bg-rose-50 border-rose-200' },
  CHANGE_STATE: { label: 'Changed Status',  color: 'text-purple-700',  bg: 'bg-purple-50 border-purple-200' },
};

function timeAgo(dateStr) {
  const now = new Date();
  const then = new Date(dateStr);
  const diffMs = now - then;
  const diffSec = Math.floor(diffMs / 1000);
  if (diffSec < 60) return 'just now';
  const diffMin = Math.floor(diffSec / 60);
  if (diffMin < 60) return `${diffMin}m ago`;
  const diffHr = Math.floor(diffMin / 60);
  if (diffHr < 24) return `${diffHr}h ago`;
  const diffDay = Math.floor(diffHr / 24);
  return `${diffDay}d ago`;
}

export default function ActivityPanel({ boardCode, isOpen, onClose }) {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!isOpen) return;
    setLoading(true);
    fetchBoardActivity(boardCode)
      .then(setLogs)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [isOpen, boardCode]);

  if (!isOpen) return null;

  return (
    <>
      {/* Overlay */}
      <div
        className="fixed inset-0 bg-slate-900/30 backdrop-blur-xs z-40 animate-fade-in"
        onClick={onClose}
      />

      {/* Panel */}
      <div className="fixed top-0 right-0 bottom-0 w-full max-w-md z-50 flex flex-col bg-white border-l border-slate-200 shadow-2xl animate-scale-up"
           style={{ transformOrigin: 'right center' }}>

        {/* Header */}
        <div className="px-6 py-5 border-b border-slate-200 flex items-center justify-between shrink-0 bg-slate-50/50">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-indigo-50 border border-indigo-200 text-indigo-700 flex items-center justify-center">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <div>
              <h2 className="font-heading font-extrabold text-slate-900 text-lg leading-none">Activity Log</h2>
              <p className="text-xs text-slate-500 mt-0.5 font-medium">Board activity history</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-xl bg-slate-100 hover:bg-slate-200 border border-slate-200 text-slate-500 hover:text-slate-900 flex items-center justify-center transition-all"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto px-5 py-4">
          {loading ? (
            <div className="flex items-center justify-center py-16">
              <span className="w-5 h-5 border-2 border-indigo-600 border-t-transparent rounded-full animate-spin" />
              <span className="text-slate-500 text-sm ml-3 font-medium">Loading activity...</span>
            </div>
          ) : logs.length === 0 ? (
            <div className="text-center py-16">
              <div className="w-12 h-12 rounded-xl bg-slate-100 border border-slate-200 flex items-center justify-center text-slate-400 mx-auto mb-3">
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
                </svg>
              </div>
              <p className="text-slate-800 text-sm font-semibold">No activity recorded</p>
              <p className="text-slate-500 text-xs mt-1">Actions will appear here as team members make updates.</p>
            </div>
          ) : (
            <div className="relative">
              {/* Vertical Timeline Line */}
              <div className="absolute left-[18px] top-2 bottom-2 w-px bg-slate-200" />

              <div className="space-y-1">
                {logs.map((log, idx) => {
                  const meta = ACTION_META[log.action] || { label: log.action, color: 'text-slate-700', bg: 'bg-slate-100 border-slate-200' };
                  return (
                    <div key={log.id || idx} className="relative pl-10 py-3 group">
                      {/* Timeline Dot */}
                      <div className={`absolute left-2.5 top-[18px] w-3 h-3 rounded-full border-2 ${meta.bg} ring-2 ring-white z-10`} />

                      <div className="bg-white hover:bg-slate-50/80 border border-slate-200 rounded-xl p-3.5 transition-all shadow-xs">
                        {/* Top Row: Action Badge + Time */}
                        <div className="flex items-center justify-between mb-2">
                          <span className={`inline-flex items-center text-[10px] font-bold tracking-wider uppercase px-2 py-0.5 rounded-md border ${meta.bg} ${meta.color}`}>
                            {meta.label}
                          </span>
                          <span className="text-[10px] text-slate-400 font-mono font-medium">
                            {timeAgo(log.createdAt)}
                          </span>
                        </div>

                        {/* User Email */}
                        <div className="flex items-center gap-2 mb-1.5">
                          <span className="w-4 h-4 rounded-full bg-indigo-100 border border-indigo-200 flex items-center justify-center text-[9px] font-bold text-indigo-700 uppercase shrink-0">
                            {log.userEmail?.charAt(0) || '?'}
                          </span>
                          <span className="text-xs text-slate-800 font-semibold truncate">
                            {log.userEmail}
                          </span>
                        </div>

                        {/* Details Row */}
                        <div className="text-[11px] text-slate-600 space-y-1 mt-2 border-t border-slate-100 pt-2">
                          <div className="font-semibold text-slate-900 text-xs flex items-center gap-1.5">
                            <span className="text-slate-500 font-normal">Card:</span>
                            <span className="text-indigo-700 font-bold truncate">
                              {log.cardTitle || (log.action !== 'MOVE_CARD' && log.action !== 'CHANGE_STATE' ? log.newValue : null) || `Card #${log.cardId}`}
                            </span>
                          </div>

                          {log.action === 'MOVE_CARD' && (
                            <div className="flex items-center gap-1.5 text-slate-600 text-[11px] flex-wrap mt-1">
                              <span>Moved from</span>
                              <span className="font-mono bg-slate-100 border border-slate-200 text-slate-800 px-1.5 py-0.5 rounded text-[10px] font-medium">
                                {log.oldValue?.startsWith('from:') ? 'Source Column' : (log.oldValue || 'Column')}
                              </span>
                              <span className="text-indigo-600 font-bold">&rarr;</span>
                              <span className="font-mono bg-indigo-50 border border-indigo-200 text-indigo-700 px-1.5 py-0.5 rounded text-[10px] font-semibold">
                                {log.newValue?.startsWith('pos:') ? 'Target Column' : (log.newValue || 'Column')}
                              </span>
                            </div>
                          )}

                          {log.action === 'CHANGE_STATE' && (
                            <div className="flex items-center gap-1.5 text-slate-600 text-[11px] flex-wrap mt-1">
                              <span>Status:</span>
                              <span className="font-mono bg-slate-100 border border-slate-200 text-slate-700 px-1.5 py-0.5 rounded text-[10px]">
                                {log.oldValue}
                              </span>
                              <span className="text-purple-600 font-bold">&rarr;</span>
                              <span className="font-mono bg-purple-50 border border-purple-200 text-purple-700 px-1.5 py-0.5 rounded text-[10px] font-semibold">
                                {log.newValue}
                              </span>
                            </div>
                          )}

                          {(log.action === 'CREATE_CARD' || log.action === 'EDIT_CARD' || log.action === 'DELETE_CARD') && log.newValue && (
                            <div className="text-[10px] text-slate-500 flex items-center gap-1">
                              <span>Title:</span>
                              <span className="text-slate-800 font-medium italic">{log.newValue}</span>
                            </div>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="px-6 py-3 border-t border-slate-200 text-center bg-slate-50/50">
          <span className="text-[11px] text-slate-500 font-medium">
            {logs.length} event{logs.length !== 1 ? 's' : ''} recorded
          </span>
        </div>
      </div>
    </>
  );
}
