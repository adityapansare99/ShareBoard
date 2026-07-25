import React, { useState, useEffect } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { fetchAuthStatus } from './services/api';
import Login from './components/Login';
import Dashboard from './components/Dashboard';
import BoardView from './components/BoardView';

export default function App() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchAuthStatus()
      .then(data => { if (data.authenticated) setUser(data); })
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-gray-500 text-lg">Loading Planar...</div>
      </div>
    );
  }

  if (!user) {
    return <Login onAuthed={() => fetchAuthStatus().then(setUser)} />;
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Routes>
        <Route path="/" element={<Dashboard user={user} setUser={setUser} />} />
        <Route path="/board/:code" element={<BoardView />} />
        <Route path="*" element={<Navigate to="/" />} />
      </Routes>
    </div>
  );
}
