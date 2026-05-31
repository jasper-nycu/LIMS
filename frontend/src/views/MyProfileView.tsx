// src/views/MyProfileView.tsx
import React, { useRef, useState, useEffect } from 'react';
import { type UserProfile } from '../components/layout/Header';
import { TotpInput } from '../components/layout/TotpInput';
import api from '../api/axiosInstance';

interface MyProfileViewProps {
  language: 'en' | 'tw';
  user: UserProfile | null;
  onUpdateUser: (updatedUser: UserProfile) => void;
  onLogout: () => void;
  onNotify: (titleKey: string | null, fallbackTitle: string, desc: string, type: 'info' | 'success' | 'error' | 'warning') => void;
}

export const MyProfileView: React.FC<MyProfileViewProps> = ({
  language,
  user,
  onUpdateUser,
  onLogout,
  onNotify
}) => {
  const i18n = {
    en: {
      title: 'User Profile',
      desc: 'Manage your personal information and security settings.',
      passwordAgeToday: 'Last changed: Today',
      passwordAgeUnknown: 'Last changed: Unknown',
      passwordAgeDays: (days: number) => `Last changed: ${days} days ago`,
      passwordAgeMonths: (months: number) => `Last changed: ${months} ${months === 1 ? 'month' : 'months'} ago. We recommend updating regularly.`,
      offline: 'Offline',
      pwdFieldsRequired: 'All password fields are required.',
      pwdMismatch: 'New passwords do not match.',
      pwdIncorrectOrFailed: 'Current password is incorrect or failed to send code.',
      pwdLabelBack: 'Back',
      pwdLabelNext: 'Next',
      pwdStepOf: (step: number) => `Step ${step} of 2`,
      btnLogout: 'Sign Out',
      btnSave: 'Save Changes',
      status: 'Status',
      active: 'Active',
      privileges: 'System Privileges',
      personalInfo: 'Personal Information',
      titleLabel: 'Title',
      firstName: 'First Name',
      lastName: 'Last Name',
      email: 'E-Mail Address',
      department: 'Department',
      telephone: 'Telephone',
      extension: 'Extension',
      mobile: 'Mobile Phone',
      role: 'Primary Role',
      security: 'Security Settings',
      password: 'Account Password',
      changePassword: 'Change Password',
      twoFactor: 'Two-Factor Authentication (2FA)',
      twoFactorDesc: '2FA adds an extra layer of security.',
      enabled: 'Enabled',
      disabled: 'Disabled',
      employeeId: 'Employee ID',
      username: 'Unnamed User',
      uploadAvatar: 'Upload avatar image',
      mr: 'Mr.', ms: 'Ms.', dr: 'Dr.',
      modalTitle: 'Change Password',
      oldPassword: 'Current Password',
      newPassword: 'New Password',
      confirmPassword: 'Confirm New Password',
      cancel: 'Cancel',
      confirm: 'Update Password',
      confirmEmailChange: 'Update Email',
      twoFaTitle: 'Email Verification',
      twoFaDesc: 'Enter the 6-digit code sent to your registered email to confirm the password change.',
      resendCode: 'Resend Code',
      profileSaved: 'Profile Saved',
      profileSavedDesc: 'Your personal information and settings have been synchronized.',
      understood: 'Understood',
      emailVerifyTitle: 'Verify New Email',
      emailVerifyDesc: 'Please enter the 6-digit code sent to your new email address.',
      mobileFormatErr: 'Mobile Phone must match 0912-345-678 or 0912345678.',
      emailVerifyErr: 'Please enter the email verification code.',
      telFormatErr: 'Telephone must match +886-3-XXXXXXX or 03-XXXXXXX.'
    },
    tw: {
      title: '使用者檔案',
      desc: '管理您的個人資訊與系統安全設定。',
      passwordAgeToday: '上次變更：今天',
      passwordAgeUnknown: '上次變更：未知時間',
      passwordAgeDays: (days: number) => `上次變更：${days} 天前`,
      passwordAgeMonths: (months: number) => `上次變更：${months} 個月前。建議您定期更新密碼以維護帳戶資安。`,
      offline: '離線',
      pwdFieldsRequired: '請填寫所有密碼欄位。',
      pwdMismatch: '新密碼與確認密碼不相符。',
      pwdIncorrectOrFailed: '密碼錯誤或發送驗證碼失敗。',
      pwdLabelBack: '返回',
      pwdLabelNext: '下一步',
      pwdStepOf: (step: number) => `第 ${step} / 2 步`,
      btnLogout: '登出系統',
      btnSave: '儲存基本資料',
      status: '狀態',
      active: '使用中',
      privileges: '系統權限',
      personalInfo: '個人資訊',
      titleLabel: '稱謂',
      firstName: '名字',
      lastName: '姓氏',
      email: '電子郵件',
      department: '所屬部門',
      telephone: '公司電話',
      extension: '分機',
      mobile: '行動電話',
      role: '主要職位',
      security: '安全性設定',
      password: '帳號密碼',
      changePassword: '變更密碼',
      twoFactor: '雙重驗證 (2FA)',
      twoFactorDesc: '2FA 為您的帳號增加額外的安全防護。',
      enabled: '已啟用',
      disabled: '已停用',
      employeeId: '員工編號',
      username: '未命名使用者',
      uploadAvatar: '上傳頭像圖片',
      mr: '先生', ms: '女士', dr: '博士',
      modalTitle: '變更密碼',
      oldPassword: '目前密碼',
      newPassword: '新密碼',
      confirmPassword: '確認新密碼',
      cancel: '取消',
      confirm: '確認變更',
      confirmEmailChange: '確認變更',
      twoFaTitle: '電子郵件驗證',
      twoFaDesc: '請輸入已發送至您電子郵件的 6 位數驗證碼以確認密碼變更。',
      resendCode: '重新發送',
      profileSaved: '檔案更新成功',
      profileSavedDesc: '您的個人資訊與設定已同步完成。',
      understood: '確認',
      emailVerifyTitle: '驗證新電子郵件',
      emailVerifyDesc: '請輸入已發送至您新信箱的 6 位數驗證碼。',
      mobileFormatErr: '手機格式需為 0912-345-678 或 0912345678。',
      emailVerifyErr: '請輸入信箱驗證碼。',
      telFormatErr: '市話格式需為 +886-3-XXXXXXX 或 03-XXXXXXX。'
    }
  };
  const ui = i18n[language];

  const roleLabels: Record<string, string> = {
    ROLE_SYSADMIN: language === 'en' ? 'System Admin' : '系統管理員',
    ROLE_LAB_MANAGER: language === 'en' ? 'Lab Supervisor' : '實驗室主管',
    ROLE_LAB_OPERATOR: language === 'en' ? 'Lab Operator' : '實驗室人員',
    ROLE_MACHINE_OWNER: language === 'en' ? 'Machine Owner' : '機台負責人',
    ROLE_FAB_USER: language === 'en' ? 'Fab User' : '廠區使用者',
    ROLE_PUBLIC: language === 'en' ? 'Public' : '一般大眾'
  };

  const splitName = (fullName: string | undefined) => {
    if (!fullName) return { first: '', last: '' };
    const parts = fullName.trim().split(/\s+/);
    if (parts.length >= 2) return { first: parts[0], last: parts[parts.length - 1] };
    return { first: parts[0] || '', last: '' };
  };

  const [formData, setFormData] = useState({
    title: 'Mr.',
    firstName: splitName(user?.name).first,
    lastName: splitName(user?.name).last,
    dept: '',
    email: '',
    tel: '',
    ext: '',
    mobile: '',
    is2FA: true
  });
  const [avatarBase64, setAvatarBase64] = useState(user?.avatarBase64 || '');
  const [showPwdModal, setShowPwdModal] = useState(false);
  const [pwdForm, setPwdForm] = useState({ oldPassword: '', newPassword: '', confirmPassword: '' });
  const [pwdError, setPwdError] = useState('');
  const [showSuccess, setShowSuccess] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // 2FA password change flow state
  // step 1: fill in old/new password; step 2: enter TOTP code sent to email
  const [pwdStep, setPwdStep] = useState<1 | 2>(1);
  const [pwdTotpCode, setPwdTotpCode] = useState('');
  const [pwdTotpCountdown, setPwdTotpCountdown] = useState(0);
  const pwdTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  
  // Real DB state trackers
  const [isActive, setIsActive] = useState(true);
  const [empId, setEmpId] = useState(user?.empId || '#TS-0000');
  const [originalEmail, setOriginalEmail] = useState('');
  const [passwordModifiedAt, setPasswordModifiedAt] = useState<string | null>(null);

  // Email verification modal state
  const [showEmailModal, setShowEmailModal] = useState(false);
  const [emailTotpCode, setEmailTotpCode] = useState('');
  const [emailTotpCountdown, setEmailTotpCountdown] = useState(0);
  const [emailError, setEmailError] = useState('');
  const emailTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const startEmailTotpCountdown = () => {
    setEmailTotpCountdown(60);
    if (emailTimerRef.current) clearInterval(emailTimerRef.current);
    emailTimerRef.current = setInterval(() => {
      setEmailTotpCountdown(prev => {
        if (prev <= 1) { clearInterval(emailTimerRef.current!); return 0; }
        return prev - 1;
      });
    }, 1000);
  };

  // Compute a human-readable relative time string for the password age indicator
  const formatPasswordAge = (dateStr: string | null): string => {
    if (!dateStr) return ui.passwordAgeUnknown;
    const past = new Date(dateStr);
    const now = new Date();
    const diffDays = Math.floor(Math.abs(now.getTime() - past.getTime()) / (1000 * 60 * 60 * 24));
    if (diffDays === 0) return ui.passwordAgeToday;
    if (diffDays < 30) return ui.passwordAgeDays(diffDays);
    return ui.passwordAgeMonths(Math.floor(diffDays / 30));
  };

  // Synchronize profile and avatar from backend database on mount
  useEffect(() => {
    const fetchUserData = async () => {
      try {
        // 1. Get user profile data (FirstName, LastName, Email)
        const profileRes = await api.get('/users/me/profile');
        if (profileRes.data) {
          setFormData(prev => ({
            ...prev,
            title: profileRes.data.title || prev.title,
            firstName: profileRes.data.first_name || prev.firstName,
            lastName: profileRes.data.last_name || prev.lastName,
            dept: profileRes.data.department || prev.dept,
            email: profileRes.data.email || prev.email,
            tel: profileRes.data.telephone || prev.tel,
            ext: profileRes.data.extension || prev.ext,
            mobile: profileRes.data.mobile_phone || prev.mobile,
            is2FA: profileRes.data.two_factor_enabled ?? true
          }));
          setIsActive(profileRes.data.is_active ?? true);
          setEmpId(profileRes.data.employee_id);
          setPasswordModifiedAt(profileRes.data.password_modified_at || null);
          setOriginalEmail(profileRes.data.email || ''); // Save original email to detect changes
        }
      } catch (err) {
        console.warn('Could not fetch user profile data');
      }

      try {
        // 2. Get avatar image
        const avatarRes = await api.get('/users/me/avatar');
        if (avatarRes.data && avatarRes.data.avatarBase64) {
          setAvatarBase64(avatarRes.data.avatarBase64);
          if (user) {
            onUpdateUser({ ...user, avatarBase64: avatarRes.data.avatarBase64 });
          }
        }
      } catch (err: any) {
        // If status is 500, print detailed SQL error for debugging
        if (err.response?.status === 500) {
           console.error('Avatar DB Error:', err.response.data.error);
        }
      }
    };
    fetchUserData();
  }, []);

  const currentFullName = `${formData.firstName} ${formData.lastName}`.trim();
  const displayRole = user?.role || 'ROLE_PUBLIC';
  const displayRoleLabel = roleLabels[displayRole] || roleLabels.ROLE_PUBLIC;

  const regexName = /^[\u4e00-\u9fa5a-zA-Z\s]+$/;
  const regexDept = /^[\u4e00-\u9fa5a-zA-Z0-9\s]+$/;
  const regexEmail = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  const regexTel = /^(\+886-|0)\d-(\d{7}|\d{8})$/;
  const regexExt = /^\d{7}$/;
  const regexMobile = /^(09\d{8}|09\d{2}-\d{3}-\d{3})$/; // Support both 0912345678 and 0912-345-678

  const handleAvatarChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;
    if (!file.type.startsWith('image/')) {
      onNotify(null, 'Validation Error', 'Avatar must be an image file.', 'error');
      return;
    }

    const reader = new FileReader();
    reader.onload = async () => {
      const nextAvatar = String(reader.result || '');
      
      try {
        // Call the /me backend API; identity is securely verified via the intercepted JWT
        await api.put('/users/me/avatar', { avatarBase64: nextAvatar });
        
        setAvatarBase64(nextAvatar);
        onUpdateUser({
          empId: user?.empId || '',
          name: currentFullName || user?.name || ui.username,
          role: displayRole,
          avatarBase64: nextAvatar
        });
        onNotify(null, 'Avatar Updated', 'Profile image has been stored to Database.', 'success');
      } catch (error) {
        onNotify(null, 'Upload Error', 'Failed to save avatar to the server.', 'error');
      }
    };
    reader.onerror = () => onNotify(null, 'Upload Error', 'Could not read the selected image.', 'error');
    reader.readAsDataURL(file);
  };

  const handleToggle2FA = async () => {
    const nextState = !formData.is2FA;
    setFormData({ ...formData, is2FA: nextState });
    try {
      await api.put('/users/me/profile', {
        title: formData.title,
        firstName: formData.firstName,
        lastName: formData.lastName,
        department: formData.dept,
        email: formData.email,
        telephone: formData.tel,
        extension: formData.ext,
        mobilePhone: formData.mobile,
        twoFactorEnabled: nextState
      });
      onNotify(
        null,
        'Security Settings Updated',
        `Two-Factor Authentication (2FA) has been ${nextState ? 'enabled' : 'disabled'}.`,
        nextState ? 'success' : 'warning'
      );
    } catch {
      // Roll back the UI toggle if the API call fails
      setFormData(prev => ({ ...prev, is2FA: !nextState }));
      onNotify(null, 'Error', 'Failed to update 2FA setting.', 'error');
    }
  };

  // Start a 60-second countdown to prevent spamming the resend button
  const startPwdTotpCountdown = () => {
    setPwdTotpCountdown(60);
    if (pwdTimerRef.current) clearInterval(pwdTimerRef.current);
    pwdTimerRef.current = setInterval(() => {
      setPwdTotpCountdown(prev => {
        if (prev <= 1) { clearInterval(pwdTimerRef.current!); return 0; }
        return prev - 1;
      });
    }, 1000);
  };

  // Validate step 1 fields via backend, then advance to step 2
  const handleNextStep = async () => {
    setPwdError('');
    if (!pwdForm.oldPassword || !pwdForm.newPassword || !pwdForm.confirmPassword) {
      setPwdError(ui.pwdFieldsRequired);
      return;
    }
    if (pwdForm.newPassword !== pwdForm.confirmPassword) {
      setPwdError(ui.pwdMismatch);
      return;
    }
    
    try {
      // Verify old password before proceeding
      await api.post('/users/me/password/verify', { oldPassword: pwdForm.oldPassword, newPassword: '', totpCode: '' });
      
      // If 2FA is enabled, trigger the email sending API
      if (formData.is2FA) {
        await api.post('/users/me/password/send-code');
        startPwdTotpCountdown();
      }
      setPwdStep(2);
    } catch (err: any) {
      setPwdError(err.response?.data?.message || ui.pwdIncorrectOrFailed);
    }
  };

  const handleResendPwdTotpCode = async () => {
    try {
      await api.post('/users/me/password/send-code');
      startPwdTotpCountdown();
      onNotify(null, 'Code Resent', 'A new verification code has been sent to your email.', 'info');
    } catch {
      onNotify(null, 'Error', 'Failed to resend verification code.', 'error');
    }
  };

  const handlePasswordUpdate = async () => {
    // Guard: TOTP code is required on step 2 when 2FA is enabled
    if (formData.is2FA && !pwdTotpCode) {
      setPwdError(language === 'en' ? 'Please enter the 2FA verification code.' : '請輸入雙重驗證碼。');
      return;
    }
    try {
      await api.put('/users/me/password', {
        oldPassword: pwdForm.oldPassword,
        newPassword: pwdForm.newPassword,
        ...(formData.is2FA && { totpCode: pwdTotpCode })
      });
      // Reset all modal state on success
      setShowPwdModal(false);
      setPwdStep(1);
      setPwdForm({ oldPassword: '', newPassword: '', confirmPassword: '' });
      setPwdTotpCode('');
      setPwdTotpCountdown(0);
      if (pwdTimerRef.current) clearInterval(pwdTimerRef.current);
      onNotify(null, 'Password Changed', 'Your account password has been updated successfully.', 'success');
    } catch (err: any) {
      setPwdError(err.response?.data?.message || ui.pwdIncorrectOrFailed);
    }
  };

  const handleSave = async () => {
    if (formData.firstName && !regexName.test(formData.firstName)) {
      onNotify(null, 'Validation Error', 'First Name contains invalid characters.', 'error');
      return;
    }
    if (formData.lastName && !regexName.test(formData.lastName)) {
      onNotify(null, 'Validation Error', 'Last Name contains invalid characters.', 'error');
      return;
    }
    if (formData.dept && !regexDept.test(formData.dept)) {
      onNotify(null, 'Validation Error', 'Department contains invalid characters.', 'error');
      return;
    }
    if (formData.email && !regexEmail.test(formData.email)) {
      onNotify(null, 'Validation Error', 'Invalid E-Mail Address format.', 'error');
      return;
    }
    if (formData.tel && !regexTel.test(formData.tel)) {
      onNotify(null, 'Validation Error', 'Telephone must match +886-3-5636688 or 03-5636688.', 'error');
      return;
    }
    if (formData.ext && !regexExt.test(formData.ext)) {
      onNotify(null, 'Validation Error', 'Extension must be a 7-digit number (e.g., 7123456).', 'error');
      return;
    }
    if (formData.mobile && !regexMobile.test(formData.mobile)) {
      onNotify(null, 'Validation Error', ui.mobileFormatErr, 'error');
      return;
    }

    // Standardize mobile formatting automatically into the hyphen variant (09XX-XXX-XXX)
    let normalizedMobile = formData.mobile;
    if (formData.mobile && formData.mobile.length === 10 && !formData.mobile.includes('-')) {
      normalizedMobile = `${formData.mobile.slice(0, 4)}-${formData.mobile.slice(4, 7)}-${formData.mobile.slice(7)}`;
    }

    // If email is changed, trigger the verification modal instead of saving directly
    if (formData.email !== originalEmail) {
      try {
        await api.post('/users/me/profile/email/send-code', { newEmail: formData.email });
        setEmailError('');
        setEmailTotpCode('');
        setShowEmailModal(true);
        startEmailTotpCountdown();
      } catch (err: any) {
        onNotify(null, 'Error', err.response?.data?.message || 'Failed to send verification code.', 'error');
      }
      return;
    }

    // Proceed directly if email is not changed
    await executeProfileUpdate(normalizedMobile);
  };

  const handleResendEmailCode = async () => {
    try {
      await api.post('/users/me/profile/email/send-code', { newEmail: formData.email });
      startEmailTotpCountdown();
      onNotify(null, 'Code Resent', 'A new verification code has been sent to your new email.', 'info');
    } catch {
      onNotify(null, 'Error', 'Failed to resend verification code.', 'error');
    }
  };

  const executeProfileUpdate = async (targetMobile: string, totpCode?: string) => {
    if (totpCode === '' && formData.email !== originalEmail) {
      setEmailError(ui.emailVerifyErr);
      return;
    }
    
    try {
      await api.put('/users/me/profile', {
        title: formData.title,
        firstName: formData.firstName,
        lastName: formData.lastName,
        department: formData.dept,
        email: formData.email,
        telephone: formData.tel,
        extension: formData.ext,
        mobilePhone: targetMobile,
        twoFactorEnabled: formData.is2FA,
        ...(totpCode && { emailTotpCode: totpCode })
      });
      
      setOriginalEmail(formData.email);
      setFormData(prev => ({ ...prev, mobile: targetMobile }));
      setShowEmailModal(false);

      onUpdateUser({
        empId: empId,
        name: currentFullName || ui.username,
        role: displayRole,
        avatarBase64
      });

      setPasswordModifiedAt(new Date().toISOString());
      onNotify(null, 'Profile Synchronized', 'User settings successfully stored in database.', 'success');
      setShowSuccess(true);
    } catch (err: any) {
      if (totpCode) {
         setEmailError(err.response?.data?.message || 'Verification failed.');
      } else {
         onNotify(null, 'Save Error', 'Failed to synchronize with database.', 'error');
      }
    }
  };

  const getPrivileges = (role: string) => {
    const map: Record<string, string[]> = {
      ROLE_SYSADMIN: ['Full System Access', 'Security Settings Control', 'Audit Logs Access'],
      ROLE_LAB_MANAGER: ['Laboratory Analytics', 'Audit Logs Access'],
      ROLE_LAB_OPERATOR: ['Laboratory Analytics'],
      ROLE_MACHINE_OWNER: ['Maintenance Control', 'Alarm Clearance'],
      ROLE_FAB_USER: ['Request Creation'],
      ROLE_PUBLIC: ['View Own Profile']
    };
    if (language === 'tw') {
      return {
        ROLE_SYSADMIN: ['完整系統存取權', '安全性設定控制', '稽核日誌存取'],
        ROLE_LAB_MANAGER: ['實驗室產能分析', '稽核日誌存取'],
        ROLE_LAB_OPERATOR: ['實驗室產能分析'],
        ROLE_MACHINE_OWNER: ['維護排程控制', '警報排除'],
        ROLE_FAB_USER: ['建立委託單'],
        ROLE_PUBLIC: ['檢視個人檔案']
      }[role] || ['檢視個人檔案'];
    }
    return map[role] || map.ROLE_PUBLIC;
  };

  return (
    <div className="max-w-6xl mx-auto space-y-8 animate-[fadeIn_0.3s_ease-out]">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl md:text-3xl font-bold tracking-tight text-slate-900">{ui.title}</h2>
          <p className="text-sm text-slate-500 mt-1">{ui.desc}</p>
        </div>
        <button onClick={onLogout} className="rounded-xl border border-red-100 bg-red-50 px-4 py-2.5 text-sm font-bold text-red-500 hover:bg-red-100 transition-all shadow-sm flex items-center gap-2 cursor-pointer active:scale-95">
          <span className="material-symbols-outlined text-[20px]">logout</span>
          {ui.btnLogout}
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-1 space-y-6">
          <div className="rounded-3xl border border-slate-200 bg-white p-8 text-center shadow-sm">
            <div className="relative mx-auto mb-6 h-32 w-32 group">
              <div className="h-full w-full overflow-hidden rounded-2xl border-4 border-slate-100 flex items-center justify-center shadow-inner transition-all bg-slate-100 text-slate-400">
                {avatarBase64 ? (
                  <img src={avatarBase64} alt="Avatar" className="h-full w-full object-cover" />
                ) : (
                  <span className="material-symbols-outlined select-none text-slate-300" style={{ fontSize: '90px', lineHeight: '1', display: 'block' }}>person</span>
                )}
              </div>
              <input ref={fileInputRef} type="file" accept="image/*" className="hidden" aria-label={ui.uploadAvatar} onChange={handleAvatarChange} />
              <button type="button" aria-label={ui.uploadAvatar} onClick={() => fileInputRef.current?.click()} className="absolute -bottom-2 -right-2 rounded-full bg-accent-sky p-2 text-white shadow-lg hover:scale-110 transition-transform cursor-pointer flex items-center justify-center">
                <span className="material-symbols-outlined text-lg">photo_camera</span>
              </button>
            </div>
            <h3 className="text-xl font-bold text-slate-900">{currentFullName || ui.username}</h3>
            <p className="text-accent-sky font-bold text-xs uppercase tracking-widest mt-1">{displayRoleLabel}</p>

            <div className="mt-6 flex flex-col gap-3 border-t border-slate-50 pt-6 text-sm">
              <div className="flex justify-between font-medium">
                <span className="text-slate-500">{ui.employeeId}</span>
                <span className="text-slate-900 font-mono">{empId}</span>
              </div>
              <div className="flex justify-between font-medium">
                <span className="text-slate-500">{ui.status}</span>
                <span className={`inline-flex items-center gap-1.5 ${isActive ? 'text-emerald-600' : 'text-slate-500'}`}>
                  <span className={`h-1.5 w-1.5 rounded-full ${isActive ? 'bg-emerald-600 animate-pulse' : 'bg-slate-400'}`}></span>
                  {isActive ? ui.active : ui.offline}
                </span>
              </div>
            </div>
          </div>

          <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
            <h4 className="font-bold mb-4 flex items-center gap-2 text-slate-800 text-sm">
              <span className="material-symbols-outlined text-accent-sky">verified_user</span>
              {ui.privileges}
            </h4>
            <ul className="space-y-3">
              {getPrivileges(displayRole).map((priv, i) => (
                <li key={i} className="flex items-center gap-3 text-xs text-slate-600 font-medium">
                  <span className="material-symbols-outlined text-emerald-500 text-sm">check_circle</span>
                  {priv}
                </li>
              ))}
            </ul>
          </div>
        </div>

        <div className="lg:col-span-2 space-y-6">
          <section className="rounded-3xl border border-slate-200 bg-white shadow-sm overflow-hidden">
            <div className="border-b border-slate-50 bg-slate-50/50 px-8 py-4">
              <h3 className="font-bold text-slate-800">{ui.personalInfo}</h3>
            </div>
            <div className="p-8">
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-6 mb-8">
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-500">{ui.titleLabel}</label>
                  <select value={formData.title} onChange={(e) => setFormData({ ...formData, title: e.target.value })} className="w-full rounded-lg border-slate-200 bg-white py-2.5 px-4 text-sm focus:ring-corporate-blue focus:border-corporate-blue cursor-pointer">
                    <option value="Mr.">{ui.mr}</option>
                    <option value="Ms.">{ui.ms}</option>
                    <option value="Dr.">{ui.dr}</option>
                  </select>
                </div>
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-500">{ui.lastName}</label>
                  <input type="text" value={formData.lastName} onChange={(e) => setFormData({ ...formData, lastName: e.target.value })} className="w-full rounded-lg border-slate-200 bg-white py-2.5 px-4 text-sm focus:ring-corporate-blue focus:border-corporate-blue" />
                </div>
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-500">{ui.firstName}</label>
                  <input type="text" value={formData.firstName} onChange={(e) => setFormData({ ...formData, firstName: e.target.value })} className="w-full rounded-lg border-slate-200 bg-white py-2.5 px-4 text-sm focus:ring-corporate-blue focus:border-corporate-blue" />
                </div>
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-500">{ui.department}</label>
                  <input type="text" value={formData.dept} onChange={(e) => setFormData({ ...formData, dept: e.target.value })} className="w-full rounded-lg border-slate-200 bg-white py-2.5 px-4 text-sm focus:ring-corporate-blue" />
                </div>
                <div className="space-y-1.5 sm:col-span-2">
                  <label className="text-xs font-semibold text-slate-500">{ui.email}</label>
                  <input type="email" value={formData.email} placeholder="example@tsmc.com" onChange={(e) => setFormData({ ...formData, email: e.target.value })} className="w-full rounded-lg border-slate-200 bg-white py-2.5 px-4 text-sm focus:ring-corporate-blue" />
                </div>
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-500">{ui.telephone}</label>
                  <input type="tel" value={formData.tel} placeholder="+886-3-5636688" onChange={(e) => setFormData({ ...formData, tel: e.target.value })} className="w-full rounded-lg border-slate-200 bg-white py-2.5 px-4 text-sm focus:ring-corporate-blue focus:border-corporate-blue" />
                </div>
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-500">{ui.extension}</label>
                  <input type="text" inputMode="numeric" pattern="[0-9]*" value={formData.ext} placeholder="7123456" onChange={(e) => setFormData({ ...formData, ext: e.target.value })} className="w-full rounded-lg border-slate-200 bg-white py-2.5 px-4 text-sm focus:ring-corporate-blue focus:border-corporate-blue" />
                </div>
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-500">{ui.mobile}</label>
                  <input type="tel" value={formData.mobile} placeholder="0912-345-678" onChange={(e) => setFormData({ ...formData, mobile: e.target.value })} className="w-full rounded-lg border-slate-200 bg-white py-2.5 px-4 text-sm focus:ring-corporate-blue focus:border-corporate-blue" />
                </div>
              </div>

              <div className="border-t border-slate-50 pt-6 mt-6 flex flex-col sm:flex-row items-end gap-6">
                <div className="space-y-1.5 flex-1 w-full">
                  <label className="text-xs font-semibold text-slate-500">{ui.role}</label>
                  <input type="text" value={displayRoleLabel} readOnly className="w-full rounded-lg border-slate-200 bg-slate-50 py-2.5 px-4 text-sm font-bold text-slate-500 cursor-not-allowed" />
                </div>
                <button onClick={handleSave} className="w-full sm:w-auto shrink-0 rounded-xl bg-corporate-blue px-8 py-3 text-sm font-bold text-white shadow-lg hover:bg-blue-700 transition-all flex items-center justify-center gap-2 cursor-pointer active:scale-95">
                  <span className="material-symbols-outlined text-[18px]">save</span>
                  {ui.btnSave}
                </button>
              </div>
            </div>
          </section>

          <section className="rounded-3xl border border-slate-200 bg-white shadow-sm overflow-hidden">
            <div className="border-b border-slate-50 bg-slate-50/50 px-8 py-4">
              <h3 className="font-bold text-slate-800">{ui.security}</h3>
            </div>
            <div className="p-8 space-y-6">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div>
                  <h4 className="font-bold text-sm text-slate-900">{ui.password}</h4>
                  <p className="text-xs text-slate-500">{formatPasswordAge(passwordModifiedAt)}</p>
                </div>
                <button onClick={() => setShowPwdModal(true)} className="px-4 py-2 bg-slate-100 text-slate-600 text-xs font-bold rounded-lg hover:bg-slate-200 cursor-pointer">
                  {ui.changePassword}
                </button>
              </div>
              <div className="h-px bg-slate-50"></div>
              <div className="flex items-start justify-between gap-4">
                <div className="flex gap-4">
                  <div className="mt-1 flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600 border border-emerald-100">
                    <span className="material-symbols-outlined">security</span>
                  </div>
                  <div>
                    <h4 className="font-bold text-sm text-slate-900 flex items-center gap-2">
                      {ui.twoFactor}
                      <span className={`text-[9px] px-1.5 py-0.5 rounded font-bold uppercase ${formData.is2FA ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-500'}`}>
                        {formData.is2FA ? ui.enabled : ui.disabled}
                      </span>
                    </h4>
                    <p className="text-xs text-slate-500 max-w-sm mt-1">{ui.twoFactorDesc}</p>
                  </div>
                </div>
                <button onClick={handleToggle2FA} className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ${formData.is2FA ? 'bg-emerald-500' : 'bg-slate-200'}`}>
                  <span className={`inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ${formData.is2FA ? 'translate-x-5' : 'translate-x-0'}`} />
                </button>
              </div>
            </div>
          </section>

          {showPwdModal && (
            <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-[fadeIn_0.2s_ease-out]">
              <div className="bg-white rounded-3xl shadow-2xl w-full max-w-md overflow-hidden animate-[zoomIn_0.2s_ease-out]">
                <div className="border-b border-slate-50 bg-slate-50/50 px-8 py-4 flex items-center justify-between">
                  <h3 className="font-bold text-slate-800">
                    {ui.modalTitle}
                    {/* Show step indicator only when 2FA is enabled */}
                    {formData.is2FA && (
                      <span className="ml-2 text-[10px] font-bold text-slate-400 uppercase tracking-widest">
                        {ui.pwdStepOf(pwdStep)}
                      </span>
                    )}
                  </h3>
                  <button
                    onClick={() => { setShowPwdModal(false); setPwdStep(1); setPwdError(''); setPwdForm({ oldPassword: '', newPassword: '', confirmPassword: '' }); setPwdTotpCode(''); setPwdTotpCountdown(0); if (pwdTimerRef.current) clearInterval(pwdTimerRef.current); }}
                    className="text-slate-400 hover:text-slate-600 focus:outline-none cursor-pointer"
                  >
                    <span className="material-symbols-outlined text-xl">close</span>
                  </button>
                </div>
                <div className="p-8 space-y-4">
                  {pwdError && pwdStep === 1 && (
                    <div className="p-3 bg-red-50 border border-red-100 rounded-lg text-red-600 text-xs font-bold flex items-center gap-2 animate-[fadeIn_0.2s_ease-out]">
                      <span className="material-symbols-outlined text-[16px]">error</span>
                      {pwdError}
                    </div>
                  )}
                  {pwdStep === 1 ? (
                    // Step 1: Enter current and new passwords
                    <>
                      <div className="space-y-1.5">
                        <label className="text-xs font-semibold text-slate-500">{ui.oldPassword}</label>
                        <input type="password" value={pwdForm.oldPassword} onChange={(e) => setPwdForm({ ...pwdForm, oldPassword: e.target.value })} className="w-full rounded-lg border-slate-200 bg-white py-2.5 px-4 text-sm focus:ring-corporate-blue" />
                      </div>
                      <div className="space-y-1.5">
                        <label className="text-xs font-semibold text-slate-500">{ui.newPassword}</label>
                        <input type="password" value={pwdForm.newPassword} onChange={(e) => setPwdForm({ ...pwdForm, newPassword: e.target.value })} className="w-full rounded-lg border-slate-200 bg-white py-2.5 px-4 text-sm focus:ring-corporate-blue" />
                      </div>
                      <div className="space-y-1.5">
                        <label className="text-xs font-semibold text-slate-500">{ui.confirmPassword}</label>
                        <input type="password" value={pwdForm.confirmPassword} onChange={(e) => setPwdForm({ ...pwdForm, confirmPassword: e.target.value })} className="w-full rounded-lg border-slate-200 bg-white py-2.5 px-4 text-sm focus:ring-corporate-blue" />
                      </div>
                      <div className="flex justify-end gap-3 pt-4 border-t border-slate-50 mt-6">
                        <button
                          onClick={() => { setShowPwdModal(false); setPwdError(''); setPwdForm({ oldPassword: '', newPassword: '', confirmPassword: '' }); }}
                          className="px-4 py-2 bg-slate-100 text-slate-600 text-xs font-bold rounded-lg hover:bg-slate-200 cursor-pointer"
                        >
                          {ui.cancel}
                        </button>
                        {/* If 2FA is enabled, next step sends the code; otherwise submit directly */}
                        {formData.is2FA ? (
                          <button
                            onClick={handleNextStep}
                            className="px-4 py-2 bg-corporate-blue text-white text-xs font-bold rounded-lg hover:bg-blue-700 shadow-md cursor-pointer flex items-center gap-1.5"
                          >
                            {ui.pwdLabelNext}
                            <span className="material-symbols-outlined text-[14px]">arrow_forward</span>
                          </button>
                        ) : (
                          <button
                            onClick={handlePasswordUpdate}
                            className="px-4 py-2 bg-corporate-blue text-white text-xs font-bold rounded-lg hover:bg-blue-700 shadow-md cursor-pointer"
                          >
                            {ui.confirm}
                          </button>
                        )}
                      </div>
                    </>
                  ) : (
                    // Step 2: Enter the TOTP code sent to the user's email (2FA only)
                    <>
                      <div className="py-2">
                        <TotpInput
                          title={ui.twoFaTitle}
                          description={ui.twoFaDesc}
                          value={pwdTotpCode}
                          onChange={(val) => { setPwdTotpCode(val); setPwdError(''); }}
                          countdown={pwdTotpCountdown}
                          resendLabel={ui.resendCode}
                          onResend={handleResendPwdTotpCode}
                          error={pwdError}
                        />
                      </div>
                      <div className="flex justify-end gap-3 pt-4 border-t border-slate-50 mt-2">
                        {/* Back button returns to step 1 without closing the modal */}
                        <button
                          onClick={() => { setPwdStep(1); setPwdTotpCode(''); setPwdError(''); }}
                          className="px-4 py-2 bg-slate-100 text-slate-600 text-xs font-bold rounded-lg hover:bg-slate-200 cursor-pointer flex items-center gap-1.5"
                        >
                          <span className="material-symbols-outlined text-[14px]">arrow_back</span>
                          {ui.pwdLabelBack}
                        </button>
                        <button
                          onClick={handlePasswordUpdate}
                          disabled={formData.is2FA && pwdTotpCode.length !== 6}
                          className="px-4 py-2 bg-corporate-blue text-white text-xs font-bold rounded-lg hover:bg-blue-700 shadow-md cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed transition-all"
                        >
                          {ui.confirm}
                        </button>
                      </div>
                    </>
                  )}
                </div>
              </div>
            </div>
          )}

          {showSuccess && (
            <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-[fadeIn_0.2s_ease-out]">
              <div className="bg-white rounded-3xl shadow-2xl w-full max-w-sm p-8 text-center space-y-4 animate-[zoomIn_0.2s_ease-out]">
                <div className="mx-auto flex items-center justify-center h-16 w-16 rounded-full bg-emerald-50 mb-2 border-8 border-white shadow-sm">
                  <span className="material-symbols-outlined text-4xl text-emerald-500">check_circle</span>
                </div>
                <h3 className="text-xl font-bold text-slate-900">{ui.profileSaved}</h3>
                <p className="text-sm text-slate-500 font-medium leading-relaxed">{ui.profileSavedDesc}</p>
                <button onClick={() => setShowSuccess(false)} className="w-full mt-4 px-4 py-3 bg-slate-900 text-white text-sm font-bold rounded-xl hover:bg-slate-800 transition-all shadow-md active:scale-95 cursor-pointer">
                  {ui.understood}
                </button>
              </div>
            </div>
          )}

          {showEmailModal && (
            <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-[fadeIn_0.2s_ease-out]">
              <div className="bg-white rounded-3xl shadow-2xl w-full max-w-md overflow-hidden animate-[zoomIn_0.2s_ease-out]">
                <div className="border-b border-slate-50 bg-slate-50/50 px-8 py-4 flex items-center justify-between">
                  <h3 className="font-bold text-slate-800">{ui.emailVerifyTitle}</h3>
                  <button
                    onClick={() => { setShowEmailModal(false); if (emailTimerRef.current) clearInterval(emailTimerRef.current); }}
                    className="text-slate-400 hover:text-slate-600 focus:outline-none cursor-pointer"
                  >
                    <span className="material-symbols-outlined text-xl">close</span>
                  </button>
                </div>
                <div className="p-8 space-y-4">
                  <div className="py-2">
                    <TotpInput
                      title={ui.emailVerifyTitle}
                      description={ui.emailVerifyDesc}
                      value={emailTotpCode}
                      onChange={(val) => { setEmailTotpCode(val); setEmailError(''); }}
                      countdown={emailTotpCountdown}
                      resendLabel={ui.resendCode}
                      onResend={handleResendEmailCode}
                      error={emailError}
                    />
                  </div>
                  <div className="flex justify-end gap-3 pt-4 border-t border-slate-50 mt-2">
                    <button
                      onClick={() => { setShowEmailModal(false); }}
                      className="px-4 py-2 bg-slate-100 text-slate-600 text-xs font-bold rounded-lg hover:bg-slate-200 cursor-pointer"
                    >
                      {ui.cancel}
                    </button>
                    <button
                      onClick={() => {
                        let normalizedMobile = formData.mobile;
                        if (formData.mobile && formData.mobile.length === 10 && !formData.mobile.includes('-')) {
                          normalizedMobile = `${formData.mobile.slice(0, 4)}-${formData.mobile.slice(4, 7)}-${formData.mobile.slice(7)}`;
                        }
                        executeProfileUpdate(normalizedMobile, emailTotpCode);
                      }}
                      disabled={emailTotpCode.length !== 6}
                      className="px-4 py-2 bg-corporate-blue text-white text-xs font-bold rounded-lg hover:bg-blue-700 shadow-md cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed transition-all"
                    >
                      {ui.confirmEmailChange}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};