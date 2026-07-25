import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchBoards, createBoard, deleteBoard, joinBoard, logout } from '../services/api';

export default function Dashboard({ user, setUser }) {
  const [boards, setBoards] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [newName, setNewName] = useState('');
  const [newDesc, setNewDesc] = useState('');
  const [joinCode, setJoinCode] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    fetchBoards()
      .then(setBoards)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  function handleCreate(e) {
    e.preventDefault();
    if (!newName.trim()) return;
    createBoard(newName, newDesc)
      .then(board => {
        setBoards(prev => [board, ...prev]);
        setShowCreate(false);
        setNewName('');
        setNewDesc('');
        navigate(`/board/${board.code}`);
      })
      .catch(() => {});
  }

  function handleJoin(e) {
    e.preventDefault();
    const code = joinCode.trim().toUpperCase();
    if (!code) return;
    joinBoard(code)
      .then(board => {
        setBoards(prev => {
          if (prev.find(b => b.code === board.code)) return prev;
          return [board, ...prev];
        });
        setJoinCode('');
        navigate(`/board/${board.code}`);
      })
      .catch(() => {
        alert('Board not found. Check the code and try again.');
      });
  }

  function handleDelete(code, e) {
    e.stopPropagation();
    if (!window.confirm('Delete this board?')) return;
    deleteBoard(code)
      .then(() => setBoards(prev => prev.filter(b => b.code !== code)))
      .catch(() => {});
  }

  function handleLogout() {
    logout().then(() => setUser(null)).catch(() => setUser(null));
  }

  return (
    <div className="min-h-screen bg-slate-50 text-slate-800 flex flex-col">
      {/* Top Navbar */}
      <header className="sticky top-0 z-30 bg-white border-b border-slate-200 shadow-sm">
        <div className="max-w-7xl mx-auto px-6 py-3.5 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-indigo-600 shadow-md shadow-indigo-200 flex items-center justify-center text-white font-black text-lg font-heading">
              P
            </div>
            <div>
              <span className="font-heading font-extrabold text-slate-900 text-lg tracking-tight block leading-none">
                Planar Workspace
              </span>
              <span className="text-[11px] text-slate-500 font-medium">Real-time Collaborative Kanban</span>
            </div>
          </div>

          <div className="flex items-center gap-4">
            {user && (
              <div className="flex items-center gap-2 px-3 py-1.5 rounded-full bg-slate-100 border border-slate-200 text-xs font-semibold text-slate-700">
                <span className="w-2 h-2 rounded-full bg-emerald-500" />
                <span>{user.name || user.email}</span>
              </div>
            )}
            <button
              onClick={handleLogout}
              className="px-3.5 py-1.5 rounded-lg text-xs font-semibold text-slate-600 hover:text-slate-900 hover:bg-slate-100 transition-all border border-slate-200"
            >
              Sign Out
            </button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-6 py-8">

        {/* Hero Banner */}
        <div className="relative rounded-2xl p-8 mb-8 bg-gradient-to-r from-indigo-900 via-indigo-800 to-slate-900 text-white shadow-xl overflow-hidden">
          <div className="relative z-10 max-w-2xl">
            <h1 className="font-heading text-3xl font-extrabold tracking-tight mb-2">
              Welcome back, {user?.name || 'User'}
            </h1>
            <p className="text-sm text-indigo-100/90 leading-relaxed mb-6 font-normal">
              Manage your team projects, track task lifecycles, and collaborate in real-time across your workspace.
            </p>

            <div className="flex flex-wrap items-center gap-3">
              <button
                onClick={() => setShowCreate(true)}
                className="px-5 py-2.5 rounded-xl bg-white text-indigo-900 hover:bg-indigo-50 font-bold text-xs shadow-md transition-all flex items-center gap-2"
              >
                <svg className="w-4 h-4 text-indigo-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M12 4v16m8-8H4" />
                </svg>
                Create New Board
              </button>

              <form onSubmit={handleJoin} className="flex items-center gap-2 bg-white/10 backdrop-blur-md p-1 rounded-xl border border-white/20">
                <input
                  type="text"
                  placeholder="Enter 6-char code"
                  value={joinCode}
                  onChange={e => setJoinCode(e.target.value)}
                  maxLength={6}
                  className="bg-transparent px-3 py-1.5 text-xs text-white placeholder-indigo-200/60 uppercase font-mono focus:outline-none w-36"
                />
                <button
                  type="submit"
                  className="px-3.5 py-1.5 rounded-lg bg-white/20 hover:bg-white/30 text-white text-xs font-semibold transition-all"
                >
                  Join Board
                </button>
              </form>
            </div>
          </div>
        </div>

        {/* Board Section Header */}
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-2">
            <h2 className="font-heading text-xl font-bold text-slate-900 tracking-tight">Your Boards</h2>
            <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-slate-200 text-slate-700 font-mono">
              {boards.length}
            </span>
          </div>
        </div>

        {/* Board Cards Grid */}
        {loading ? (
          <div className="flex items-center justify-center py-16">
            <span className="w-6 h-6 border-2 border-indigo-600 border-t-transparent rounded-full animate-spin" />
            <span className="text-slate-500 text-sm font-medium ml-3">Loading boards...</span>
          </div>
        ) : boards.length === 0 ? (
          <div className="text-center py-16 bg-white rounded-2xl border border-slate-200 shadow-sm p-8 max-w-md mx-auto">
            <div className="w-12 h-12 rounded-xl bg-slate-100 border border-slate-200 flex items-center justify-center text-slate-400 mx-auto mb-3">
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
              </svg>
            </div>
            <h3 className="font-bold text-slate-900 text-base mb-1">No active boards found</h3>
            <p className="text-xs text-slate-500 mb-5">Create a board or enter a 6-character code to join a workspace.</p>
            <button
              onClick={() => setShowCreate(true)}
              className="px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-xs transition-all shadow-sm"
            >
              Create First Board
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {boards.map(b => (
              <div
                key={b.code}
                onClick={() => navigate(`/board/${b.code}`)}
                className="light-card rounded-2xl p-6 cursor-pointer group flex flex-col justify-between"
              >
                <div>
                  <div className="flex items-start justify-between mb-3">
                    <h3 className="font-heading text-lg font-bold text-slate-900 group-hover:text-indigo-600 transition-colors line-clamp-1">
                      {b.name}
                    </h3>
                    <div className="flex items-center gap-1.5">
                      <span className="font-mono text-[11px] font-semibold bg-slate-100 text-slate-700 px-2 py-0.5 rounded border border-slate-200">
                        {b.code}
                      </span>
                      {user && b.ownerEmail === user.email && (
                        <button
                          onClick={e => handleDelete(b.code, e)}
                          className="p-1 rounded text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition-colors"
                          title="Delete Board"
                        >
                          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                          </svg>
                        </button>
                      )}
                    </div>
                  </div>

                  <p className="text-xs text-slate-500 line-clamp-2 mb-4 font-normal">
                    {b.description || 'No description provided for this board workspace.'}
                  </p>
                </div>

                <div className="pt-4 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-500">
                  <div className="flex items-center gap-1">
                    <svg className="w-3.5 h-3.5 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                    </svg>
                    <span>{b.ownerEmail}</span>
                  </div>
                  <span className="font-semibold text-indigo-600 group-hover:translate-x-0.5 transition-transform inline-flex items-center gap-1">
                    Open Board &rarr;
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>

      {/* Create Board Modal */}
      {showCreate && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-fade-in">
          <div className="bg-white rounded-2xl p-6 sm:p-8 w-full max-w-md shadow-2xl border border-slate-200 animate-scale-up">
            <h2 className="font-heading text-xl font-bold text-slate-900 mb-1">Create Board</h2>
            <p className="text-xs text-slate-500 mb-6">Setup a new Kanban workspace for your team.</p>

            <form onSubmit={handleCreate} className="space-y-4">
              <div>
                <label className="block text-[11px] font-bold text-slate-600 mb-1 uppercase tracking-wider">Board Name</label>
                <input
                  type="text"
                  placeholder="e.g. Q3 Roadmap & Features"
                  value={newName}
                  onChange={e => setNewName(e.target.value)}
                  className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2.5 text-sm text-slate-900 focus:outline-none focus:bg-white focus:border-indigo-600 focus:ring-2 focus:ring-indigo-500/10"
                  required
                />
              </div>

              <div>
                <label className="block text-[11px] font-bold text-slate-600 mb-1 uppercase tracking-wider">Description (Optional)</label>
                <textarea
                  placeholder="Brief summary of board goals..."
                  value={newDesc}
                  onChange={e => setNewDesc(e.target.value)}
                  rows={3}
                  className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2.5 text-sm text-slate-900 focus:outline-none focus:bg-white focus:border-indigo-600 focus:ring-2 focus:ring-indigo-500/10 resize-none"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setShowCreate(false)}
                  className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100 transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-xs transition-all shadow-sm"
                >
                  Create Board
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
