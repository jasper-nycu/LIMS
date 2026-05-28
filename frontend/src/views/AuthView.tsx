// src/views/AuthView.tsx
import React, { useState, useEffect, useRef } from 'react';
import { type UserProfile } from '../components/layout/Header';
import { API_BASE_URL } from '../api';

interface AuthViewProps {
  language: 'en' | 'tw';
  onLanguageChange: (lang: 'en' | 'tw') => void;
  onLoginSuccess: (user: UserProfile) => void;
  onNotify: (titleKey: string | null, fallbackTitle: string, desc: string, type: 'info' | 'success' | 'error' | 'warning') => void;
}

export const AuthView: React.FC<AuthViewProps> = ({
  language,
  onLanguageChange,
  onLoginSuccess,
  onNotify
}) => {
  // i18n Translation Dictionary matching index.html auth targets exactly
  const i18n = {
    en: {
      auth_title_login: 'LIMS Portal',
      auth_title_reg: 'Create Account',
      auth_subtitle: 'Laboratory Information Management System',
      lbl_emp_id: 'Employee ID',
      lbl_pwd: 'Password',
      lbl_fname: 'First Name',
      lbl_lname: 'Last Name',
      lbl_email: 'E-mail',
      lbl_conf_pwd: 'Confirm Password',
      btn_signin: 'Sign In',
      btn_register: 'Register',
      link_new_here: 'New here? Create an account',
      link_back_login: 'Back to Sign In',
      totp_title: 'Email Verification',
      totp_desc: 'Please enter the 6-digit TOTP code sent to your email to complete registration.',
      btn_verify: 'Verify & Complete',
      btn_resend: 'Resend Code',
      validation_email: 'Please enter a valid email address.',
      validation_match: 'Passwords do not match.',
      validation_length: 'Password must be at least 8 characters long.',
      validation_totp: 'Please enter a valid 6-digit TOTP code.',
      notif_welcome: 'Welcome back to LIMS Portal.',
      notif_reg_success: 'Registration successful! Please sign in with your new account.'
    },
    tw: {
      auth_title_login: 'LIMS 系統入口',
      auth_title_reg: '建立帳號',
      auth_subtitle: '雲原生實驗室資訊管理系統',
      lbl_emp_id: '員工編號',
      lbl_pwd: '密碼',
      lbl_fname: '名字',
      lbl_lname: '姓氏',
      lbl_email: '電子郵件',
      lbl_conf_pwd: '確認密碼',
      btn_signin: '登入系統',
      btn_register: '註冊帳號',
      link_new_here: '第一次來嗎？建立帳號',
      link_back_login: '返回登入介面',
      totp_title: '電子郵件驗證',
      totp_desc: '請輸入發送至您信箱的 6 位數驗證碼以完成註冊。',
      btn_verify: '驗證並完成',
      btn_resend: '重新發送驗證碼',
      validation_email: '請輸入有效的電子郵件。',
      validation_match: '密碼不一致或為空！',
      validation_length: '密碼長度至少需 8 個字元。',
      validation_totp: '請輸入有效的 6 位數驗證碼。',
      notif_welcome: '歡迎回到 LIMS 系統。',
      notif_reg_success: '註冊成功！請使用新帳號登入系統。'
    }
  };

  const ui = i18n[language];

  // Screen view routing and layout visibility flags
  const [isRegisterMode, setIsRegisterMode] = useState<boolean>(false);
  const [showTotpModal, setShowTotpModal] = useState<boolean>(false);

  // Field visibility visibility toggle hooks
  const [showLoginPwd, setShowLoginPwd] = useState<boolean>(false);
  const [showRegPwd, setShowRegPwd] = useState<boolean>(false);
  const [showConfirmPwd, setShowConfirmPwd] = useState<boolean>(false);

  // Form input bindings
  const [loginForm, setLoginForm] = useState({ empId: '', password: '' });
  const [regForm, setRegForm] = useState({ firstName: '', lastName: '', email: '', password: '', confirmPassword: '' });
  const [totpCode, setTotpCode] = useState<string>('');

  // Custom alert popup modal state to prevent leaky pre-auth notifications
  const [alertModal, setAlertModal] = useState<{ show: boolean; title: string; message: string; type: 'error' | 'success' | 'info' }>({
    show: false,
    title: '',
    message: '',
    type: 'error'
  });

  // TOTP countdown timer controls
  const [countdown, setCountdown] = useState<number>(0);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // Clean up asynchronous interval pointers on lifecycle teardown
  useEffect(() => {
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, []);

  const startCountdown = () => {
    setCountdown(60);
    if (timerRef.current) clearInterval(timerRef.current);
    timerRef.current = setInterval(() => {
      setCountdown(prev => {
        if (prev <= 1) {
          if (timerRef.current) clearInterval(timerRef.current);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  };

  const readResponseBody = async (response: Response): Promise<any> => {
    const responseText = await response.text();
    if (!responseText) return {};
    try {
      return JSON.parse(responseText);
    } catch {
      return {};
    }
  };

  const handleLoginSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      const response = await fetch(`${API_BASE_URL}/api/v1/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(loginForm)
      });
      const data = await readResponseBody(response);

      if (!response.ok) {
        setAlertModal({
          show: true,
          title: language === 'en' ? 'Login Failed' : '登入失敗',
          message: data.message || `HTTP Error ${response.status}`,
          type: 'error'
        });
        return;
      }

      if (data.token) {
        localStorage.setItem('lims_jwt', data.token);
      }

      const signedInUser = {
        ...data.user,
        empId: data.user.empId ?? data.user.employeeId,
      } as UserProfile;

      if (!signedInUser.empId || !signedInUser.role) {
        setAlertModal({
          show: true,
          title: language === 'en' ? 'Login Failed' : '登入失敗',
          message: language === 'en' ? 'Login response is missing user profile data.' : '登入回應缺少使用者資料。',
          type: 'error'
        });
        return;
      }

      onNotify(null, 'Auth Success', ui.notif_welcome, 'success');
      onLoginSuccess(signedInUser);
    } catch {
      setAlertModal({
        show: true,
        title: language === 'en' ? 'Network Error' : '連線失敗',
        message: language === 'en' ? 'Cannot connect to backend server.' : '無法連線到後端伺服器。',
        type: 'error'
      });
    }
  };

  const handleRegisterClick = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!regForm.email.includes('@')) {
      setAlertModal({ show: true, title: language === 'en' ? 'Validation Error' : '驗證錯誤', message: ui.validation_email, type: 'error' });
      return;
    }
    if (!regForm.password || regForm.password !== regForm.confirmPassword) {
      setAlertModal({ show: true, title: language === 'en' ? 'Validation Error' : '驗證錯誤', message: ui.validation_match, type: 'error' });
      return;
    }
    if (regForm.password.length < 8) {
      setAlertModal({ show: true, title: language === 'en' ? 'Validation Error' : '驗證錯誤', message: ui.validation_length, type: 'error' });
      return;
    }

    try {
      const response = await fetch(`${API_BASE_URL}/api/v1/auth/register/initiate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(regForm)
      });
      const data = await readResponseBody(response);

      if (!response.ok) {
        setAlertModal({
          show: true,
          title: language === 'en' ? 'Registration Error' : '註冊失敗',
          message: data.message || `HTTP Error ${response.status}`,
          type: 'error'
        });
        return;
      }

      setTotpCode('');
      setShowTotpModal(true);
      startCountdown();
    } catch {
      setAlertModal({
        show: true,
        title: language === 'en' ? 'Network Error' : '連線失敗',
        message: language === 'en' ? 'Cannot connect to backend server.' : '無法連線到後端伺服器。',
        type: 'error'
      });
    }
  };

  const handleResendCode = () => {
    if (countdown > 0) return;
    setAlertModal({ 
      show: true, 
      title: language === 'en' ? 'Code Sent' : '驗證碼已發送', 
      message: language === 'en' ? 'A new verification code has been sent to your email.' : '新的驗證碼已發送至您的信箱。', 
      type: 'info' 
    });
    startCountdown();
  };

  const handleVerifyTOTP = async () => {
    if (totpCode.length !== 6 || isNaN(Number(totpCode))) {
      setAlertModal({ show: true, title: language === 'en' ? 'Verification Error' : '驗證錯誤', message: ui.validation_totp, type: 'error' });
      return;
    }

    try {
      const response = await fetch(`${API_BASE_URL}/api/v1/auth/register/verify`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: regForm.email, code: totpCode })
      });
      const data = await readResponseBody(response);

      if (!response.ok) {
        setAlertModal({
          show: true,
          title: language === 'en' ? 'Verification Error' : '驗證失敗',
          message: data.message || `HTTP Error ${response.status}`,
          type: 'error'
        });
        return;
      }

      if (timerRef.current) clearInterval(timerRef.current);
      setCountdown(0);
      setShowTotpModal(false);
      setIsRegisterMode(false); // Route back to sign in segment

      setAlertModal({ show: true, title: language === 'en' ? 'Registration Successful' : '註冊成功', message: data.message || ui.notif_reg_success, type: 'success' });
    } catch {
      setAlertModal({
        show: true,
        title: language === 'en' ? 'Network Error' : '連線失敗',
        message: language === 'en' ? 'Cannot connect to backend server.' : '無法連線到後端伺服器。',
        type: 'error'
      });
    }
  };

  return (
    <div className="fixed inset-0 z-[100] bg-slate-50 flex items-center justify-center p-4 transition-opacity duration-300 overflow-y-auto custom-scrollbar">
      {/* Top Right Floating Sliding Language Switcher Panel (Header Aligned) */}
      <div className="absolute top-6 right-6 flex bg-slate-100 p-0.5 rounded-lg text-[11px] md:text-xs font-bold shadow-inner border border-slate-200 h-8 md:h-9 z-10">
        <div className="absolute top-[2px] bottom-[2px] bg-white rounded shadow-sm transition-all duration-300" style={{ left: language === 'en' ? '2px' : '50%', width: 'calc(50% - 2px)' }} />
        <button onClick={() => onLanguageChange('en')} className={`relative z-10 px-3 md:px-4 py-1 transition-colors duration-300 cursor-pointer ${language === 'en' ? 'text-corporate-blue font-bold' : 'text-slate-500'}`}>EN</button>
        <button onClick={() => onLanguageChange('tw')} className={`relative z-10 px-3 md:px-4 py-1 transition-colors duration-300 cursor-pointer ${language === 'tw' ? 'text-corporate-blue font-bold' : 'text-slate-500'}`}>中文</button>
      </div>

      <div className="w-full max-w-[380px] space-y-5 my-auto">
        {/* Core Architecture Identity Branding */}
        <div className="text-center">
          <div className="mx-auto w-12 h-14 bg-corporate-blue text-white rounded-2xl flex items-center justify-center shadow-lg mb-2">
            <span className="material-symbols-outlined text-3xl">science</span>
          </div>
          <h2 className="text-xl font-bold tracking-tight text-slate-900">
            {isRegisterMode ? ui.auth_title_reg : ui.auth_title_login}
          </h2>
          <p className="text-[10px] text-slate-400 mt-0.5 font-medium uppercase tracking-wider">
            {ui.auth_subtitle}
          </p>
        </div>

        {/* Form Container Module */}
        {!isRegisterMode ? (
          /* Sign In UI Module */
          <form 
            onSubmit={handleLoginSubmit} 
            className="bg-white p-6 rounded-3xl shadow-xl border border-slate-100 space-y-4 animate-[fadeIn_0.3s_ease-out]"
          >
            <div className="space-y-1">
              <label className="text-[10px] font-bold text-slate-400 uppercase ml-1">{ui.lbl_emp_id}</label>
              <input 
                type="text" 
                required
                value={loginForm.empId}
                placeholder="Employee ID"
                onChange={(e) => setLoginForm({ ...loginForm, empId: e.target.value.toUpperCase() })}
                className="w-full rounded-xl border-slate-200 focus:ring-corporate-blue focus:border-corporate-blue text-sm h-11 px-4"
              />
            </div>
            <div className="space-y-1">
              <label className="text-[10px] font-bold text-slate-400 uppercase ml-1">{ui.lbl_pwd}</label>
              <div className="relative">
                <input 
                  type={showLoginPwd ? 'text' : 'password'} 
                  required
                  value={loginForm.password}
                  placeholder="Password"
                  onChange={(e) => setLoginForm({ ...loginForm, password: e.target.value })}
                  className="w-full rounded-xl border-slate-200 focus:ring-corporate-blue focus:border-corporate-blue text-sm pr-10 h-11 px-4"
                />
                <button 
                  type="button" 
                  onClick={() => setShowLoginPwd(!showLoginPwd)}
                  className={`absolute inset-y-0 right-0 px-3 flex items-center transition-colors focus:outline-none cursor-pointer ${showLoginPwd ? 'text-corporate-blue' : 'text-slate-400 hover:text-corporate-blue'}`}
                >
                  <span className="material-symbols-outlined text-[18px]">
                    {showLoginPwd ? 'visibility_off' : 'visibility'}
                  </span>
                </button>
              </div>
            </div>
            <button 
              type="submit" 
              className="w-full bg-corporate-blue text-white py-3 rounded-xl font-bold hover:bg-blue-700 transition-all shadow-lg shadow-blue-100 text-sm cursor-pointer active:scale-[0.98]"
            >
              {ui.btn_signin}
            </button>
            <div className="text-center pt-1">
              <button 
                type="button"
                onClick={() => setIsRegisterMode(true)}
                className="text-[11px] font-semibold text-corporate-blue hover:underline cursor-pointer bg-transparent border-0"
              >
                {ui.link_new_here}
              </button>
            </div>
          </form>
        ) : (
          /* Registration UI Module */
          <form 
            onSubmit={handleRegisterClick} 
            className="bg-white p-5 rounded-3xl shadow-xl border border-slate-100 space-y-3 animate-[fadeIn_0.3s_ease-out]"
          >
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <label className="text-[10px] font-bold text-slate-400 uppercase ml-1">{ui.lbl_fname}</label>
                <input 
                  type="text" required value={regForm.firstName} placeholder="First Name"
                  onChange={(e) => setRegForm({ ...regForm, firstName: e.target.value })}
                  className="w-full rounded-xl border-slate-200 text-xs h-10 px-3 focus:ring-corporate-blue focus:border-corporate-blue"
                />
              </div>
              <div className="space-y-1">
                <label className="text-[10px] font-bold text-slate-400 uppercase ml-1">{ui.lbl_lname}</label>
                <input 
                  type="text" required value={regForm.lastName} placeholder="Last Name"
                  onChange={(e) => setRegForm({ ...regForm, lastName: e.target.value })}
                  className="w-full rounded-xl border-slate-200 text-xs h-10 px-3 focus:ring-corporate-blue focus:border-corporate-blue"
                />
              </div>
            </div>
            <div className="space-y-1">
              <label className="text-[10px] font-bold text-slate-400 uppercase ml-1">{ui.lbl_email}</label>
              <input 
                type="email" required value={regForm.email} placeholder="example@tsmc.com"
                onChange={(e) => setRegForm({ ...regForm, email: e.target.value })}
                className="w-full rounded-xl border-slate-200 text-xs h-10 px-3 focus:ring-corporate-blue focus:border-corporate-blue"
              />
            </div>
            <div className="space-y-1">
              <label className="text-[10px] font-bold text-slate-400 uppercase ml-1">{ui.lbl_pwd}</label>
              <div className="relative">
                <input 
                  type={showRegPwd ? 'text' : 'password'} required value={regForm.password} placeholder="Create password"
                  onChange={(e) => setRegForm({ ...regForm, password: e.target.value })}
                  className="w-full rounded-xl border-slate-200 text-xs pr-10 h-10 px-3 focus:ring-corporate-blue focus:border-corporate-blue"
                />
                <button 
                  type="button" onClick={() => setShowRegPwd(!showRegPwd)}
                  className={`absolute inset-y-0 right-0 px-3 flex items-center focus:outline-none cursor-pointer ${showRegPwd ? 'text-corporate-blue' : 'text-slate-400'}`}
                >
                  <span className="material-symbols-outlined text-[16px]">{showRegPwd ? 'visibility_off' : 'visibility'}</span>
                </button>
              </div>
            </div>
            <div className="space-y-1">
              <label className="text-[10px] font-bold text-slate-400 uppercase ml-1">{ui.lbl_conf_pwd}</label>
              <div className="relative">
                <input 
                  type={showConfirmPwd ? 'text' : 'password'} required value={regForm.confirmPassword} placeholder="Confirm password"
                  onChange={(e) => setRegForm({ ...regForm, confirmPassword: e.target.value })}
                  className="w-full rounded-xl border-slate-200 text-xs pr-10 h-10 px-3 focus:ring-corporate-blue focus:border-corporate-blue"
                />
                <button 
                  type="button" onClick={() => setShowConfirmPwd(!showConfirmPwd)}
                  className={`absolute inset-y-0 right-0 px-3 flex items-center focus:outline-none cursor-pointer ${showConfirmPwd ? 'text-corporate-blue' : 'text-slate-400'}`}
                >
                  <span className="material-symbols-outlined text-[16px]">{showConfirmPwd ? 'visibility_off' : 'visibility'}</span>
                </button>
              </div>
            </div>
            <button 
              type="submit" 
              className="w-full bg-corporate-blue text-white py-3 rounded-xl font-bold hover:bg-blue-700 transition-all text-sm mt-2 cursor-pointer active:scale-[0.98]"
            >
              {ui.btn_register}
            </button>
            <div className="text-center pt-1">
              <button 
                type="button" onClick={() => setIsRegisterMode(false)}
                className="text-[11px] font-semibold text-corporate-blue hover:underline cursor-pointer bg-transparent border-0"
              >
                {ui.link_back_login}
              </button>
            </div>
          </form>
        )}
      </div>

      {/* TOTP Security Modal Layer */}
      {showTotpModal && (
        <div className="fixed inset-0 bg-slate-900/50 z-[120] flex items-center justify-center p-4 backdrop-blur-sm animate-[fadeIn_0.2s_ease-out]">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-sm overflow-hidden animate-[zoomIn_0.2s_ease-out]">
            <div className="p-6 border-b border-slate-100 flex justify-between items-center bg-slate-50/50">
              <h3 className="font-bold text-base text-slate-900 flex items-center gap-2">
                <span className="material-symbols-outlined text-corporate-blue">mail_lock</span>
                {ui.totp_title}
              </h3>
              <button 
                onClick={() => { setShowTotpModal(false); setCountdown(0); if (timerRef.current) clearInterval(timerRef.current); }} 
                className="text-slate-400 hover:text-slate-600 focus:outline-none cursor-pointer"
              >
                <span className="material-symbols-outlined text-xl">close</span>
              </button>
            </div>
            <div className="p-6 space-y-6 text-center">
              <p className="text-xs text-slate-500 leading-relaxed px-2">{ui.totp_desc}</p>
              <input 
                type="text" 
                maxLength={6}
                value={totpCode}
                placeholder="000000"
                onChange={(e) => setTotpCode(e.target.value.replace(/[^\d]/g, ''))}
                className="w-3/4 mx-auto text-center tracking-[0.4em] font-mono text-xl font-bold rounded-xl border-slate-200 bg-slate-50 h-12 focus:ring-corporate-blue focus:border-corporate-blue"
              />
            </div>
            <div className="p-6 pt-0 flex flex-col gap-2">
              <button 
                onClick={handleVerifyTOTP}
                className="w-full px-4 py-2.5 bg-corporate-blue text-white text-sm font-bold rounded-xl hover:bg-blue-700 shadow-md transition-colors cursor-pointer active:scale-95"
              >
                {ui.btn_verify}
              </button>
              <button 
                type="button"
                disabled={countdown > 0}
                onClick={handleResendCode}
                className="text-[10px] font-bold text-slate-400 hover:text-slate-600 disabled:opacity-50 disabled:cursor-not-allowed transition-all py-1 cursor-pointer"
              >
                {countdown > 0 ? `${ui.btn_resend} (${countdown}s)` : ui.btn_resend}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Industrial Custom Alert Popup Modal Window (Prototype styled warning-modal alignment) */}
      {alertModal.show && (
        <div className="fixed inset-0 bg-slate-900/50 z-[200] flex items-center justify-center p-4 backdrop-blur-sm animate-[fadeIn_0.2s_ease-out]">
          <div className="bg-white rounded-3xl shadow-2xl w-full max-w-sm p-6 text-center space-y-4 animate-[zoomIn_0.2s_ease-out]">
            <div className={`mx-auto flex items-center justify-center h-16 w-16 rounded-full mb-2 border-8 ${alertModal.type === 'error' ? 'bg-red-50 border-red-50/50 text-red-500' : alertModal.type === 'success' ? 'bg-emerald-50 border-emerald-50/50 text-emerald-500' : 'bg-blue-50 border-blue-50/50 text-corporate-blue'}`}>
              <span className="material-symbols-outlined text-4xl">
                {alertModal.type === 'error' ? 'error' : alertModal.type === 'success' ? 'check_circle' : 'info'}
              </span>
            </div>
            <h3 className="text-xl font-bold text-slate-900">{alertModal.title}</h3>
            <p className="text-sm text-slate-500 font-medium leading-relaxed">{alertModal.message}</p>
            <button 
              onClick={() => setAlertModal({ ...alertModal, show: false })}
              className="w-full mt-4 px-4 py-3 bg-slate-900 text-white text-sm font-bold rounded-xl hover:bg-slate-800 transition-all shadow-md active:scale-95 cursor-pointer"
            >
              {language === 'en' ? 'Understood' : '確認'}
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
