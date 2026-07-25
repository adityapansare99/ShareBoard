import React, { useState } from 'react';
import { login, register, sendOtp } from '../services/api';

export default function Login({ onAuthed }) {
  const [mode, setMode] = useState('login'); // 'login' | 'register'
  const [email, setEmail] = useState('');
  const [name, setName] = useState('');
  const [password, setPassword] = useState('');
  const [otp, setOtp] = useState('');
  const [otpSent, setOtpSent] = useState(false);
  const [otpInfo, setOtpInfo] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  function resetForm() {
    setError('');
    setOtpInfo('');
    setOtpSent(false);
    setOtp('');
  }

  async function handleSendOtp(e) {
    if (e) e.preventDefault();
    if (!email || !name || !password) {
      setError('Please enter Email, Full Name, and Password first.');
      return;
    }
    if (password.length < 8) {
      setError('Password must be at least 8 characters long.');
      return;
    }

    setError('');
    setBusy(true);
    try {
      const res = await sendOtp(email);
      setOtpSent(true);
      setOtpInfo(res.message || `Verification OTP sent to ${email}`);
    } catch (err) {
      const msg = err.response?.data?.message;
      setError(msg || 'Failed to send verification OTP.');
    } finally {
      setBusy(false);
    }
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');

    if (mode === 'register' && !otpSent) {
      await handleSendOtp();
      return;
    }

    setBusy(true);
    try {
      if (mode === 'register') {
        await register(email, name, password, otp);
      }
      await login(email, password);
      onAuthed();
    } catch (err) {
      const msg = err.response?.data?.message;
      setError(msg || (mode === 'register' ? 'Registration failed. Check OTP code.' : 'Invalid email or password'));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="relative min-h-screen flex items-center justify-center bg-slate-50 overflow-hidden px-4 py-12">
      {/* Light Mesh Backdrop */}
      <div className="absolute top-10 left-1/4 w-96 h-96 bg-indigo-100 rounded-full blur-3xl opacity-70 animate-float pointer-events-none" />
      <div className="absolute bottom-10 right-1/4 w-96 h-96 bg-blue-100 rounded-full blur-3xl opacity-60 animate-float pointer-events-none" style={{ animationDelay: '-2.5s' }} />

      {/* Main Auth Card */}
      <div className="relative z-10 w-full max-w-md animate-scale-up">
        <div className="bg-white rounded-2xl p-8 sm:p-10 shadow-xl border border-slate-200">

          {/* Brand Logo & Title */}
          <div className="text-center mb-8">
            <div className="inline-flex items-center justify-center w-12 h-12 rounded-xl bg-indigo-600 shadow-md shadow-indigo-200 mb-3 text-white font-black text-xl font-heading">
              P
            </div>
            <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight mb-1 font-heading">
              Planar
            </h1>
            <p className="text-xs text-slate-500 font-medium">
              {mode === 'login' ? 'Sign in to access your collaborative workspace' : 'Create an account with email verification'}
            </p>
          </div>

          {/* Mode Switcher Segmented Tabs */}
          <div className="flex bg-slate-100 p-1 rounded-xl mb-6 border border-slate-200/80">
            <button
              type="button"
              onClick={() => { setMode('login'); resetForm(); }}
              className={`flex-1 py-2 text-xs font-semibold rounded-lg transition-all ${
                mode === 'login'
                  ? 'bg-white text-slate-900 shadow-sm border border-slate-200'
                  : 'text-slate-500 hover:text-slate-800'
              }`}
            >
              Sign In
            </button>
            <button
              type="button"
              onClick={() => { setMode('register'); resetForm(); }}
              className={`flex-1 py-2 text-xs font-semibold rounded-lg transition-all ${
                mode === 'register'
                  ? 'bg-white text-slate-900 shadow-sm border border-slate-200'
                  : 'text-slate-500 hover:text-slate-800'
              }`}
            >
              Register
            </button>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-[11px] font-bold text-slate-600 mb-1 uppercase tracking-wider">Email Address</label>
              <div className="relative">
                <input
                  type="email"
                  placeholder="name@example.com"
                  value={email}
                  onChange={e => setEmail(e.target.value)}
                  disabled={busy || (mode === 'register' && otpSent)}
                  className="w-full bg-slate-50 border border-slate-300 rounded-xl pl-3.5 pr-4 py-2.5 text-sm text-slate-900 placeholder-slate-400 focus:outline-none focus:bg-white focus:border-indigo-600 focus:ring-2 focus:ring-indigo-500/10 transition-all disabled:opacity-60 font-medium"
                  autoComplete="email"
                  required
                />
              </div>
            </div>

            {mode === 'register' && (
              <div>
                <label className="block text-[11px] font-bold text-slate-600 mb-1 uppercase tracking-wider">Full Name</label>
                <input
                  type="text"
                  placeholder="Alex Rivers"
                  value={name}
                  onChange={e => setName(e.target.value)}
                  disabled={busy || otpSent}
                  className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2.5 text-sm text-slate-900 placeholder-slate-400 focus:outline-none focus:bg-white focus:border-indigo-600 focus:ring-2 focus:ring-indigo-500/10 transition-all disabled:opacity-60 font-medium"
                  required
                />
              </div>
            )}

            <div>
              <label className="block text-[11px] font-bold text-slate-600 mb-1 uppercase tracking-wider">Password</label>
              <input
                type="password"
                placeholder={mode === 'register' ? 'Min. 8 characters' : '••••••••'}
                value={password}
                onChange={e => setPassword(e.target.value)}
                disabled={busy || (mode === 'register' && otpSent)}
                className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2.5 text-sm text-slate-900 placeholder-slate-400 focus:outline-none focus:bg-white focus:border-indigo-600 focus:ring-2 focus:ring-indigo-500/10 transition-all disabled:opacity-60 font-medium"
                autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                required
              />
            </div>

            {/* OTP Input Field */}
            {mode === 'register' && otpSent && (
              <div className="animate-scale-up pt-1">
                <div className="flex items-center justify-between mb-1">
                  <label className="text-[11px] font-bold text-indigo-700 uppercase tracking-wider flex items-center gap-1">
                    <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 0121 9z" />
                    </svg>
                    Enter 6-Digit Email OTP
                  </label>
                  <button
                    type="button"
                    onClick={handleSendOtp}
                    disabled={busy}
                    className="text-[11px] text-indigo-600 hover:text-indigo-800 font-semibold"
                  >
                    Resend Code
                  </button>
                </div>
                <input
                  type="text"
                  placeholder="123456"
                  maxLength={6}
                  value={otp}
                  onChange={e => setOtp(e.target.value.replace(/\D/g, ''))}
                  className="w-full bg-indigo-50/50 border-2 border-indigo-300 rounded-xl px-4 py-2.5 text-center text-lg font-mono tracking-widest text-indigo-900 placeholder-slate-400 focus:outline-none focus:border-indigo-600 focus:ring-2 focus:ring-indigo-500/20 transition-all font-bold"
                  required
                />
              </div>
            )}

            {/* OTP Status Banner */}
            {otpInfo && (
              <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-xl text-emerald-800 text-xs font-medium animate-fade-in flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-emerald-500 shrink-0" />
                <span>{otpInfo}</span>
              </div>
            )}

            {/* Error Message */}
            {error && (
              <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-rose-700 text-xs font-medium animate-fade-in flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-rose-500 shrink-0" />
                {error}
              </div>
            )}

            {/* Action Button */}
            <button
              type="submit"
              disabled={busy}
              className="w-full mt-2 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-xl px-6 py-3 text-sm transition-all shadow-md shadow-indigo-200 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
            >
              {busy ? (
                <>
                  <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  Processing...
                </>
              ) : mode === 'login' ? (
                'Sign In to Dashboard'
              ) : !otpSent ? (
                'Send Verification OTP'
              ) : (
                'Verify OTP & Create Account'
              )}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
