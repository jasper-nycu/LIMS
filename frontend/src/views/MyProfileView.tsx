// src/views/MyProfileView.tsx
import React, { useRef, useState } from 'react';
import { type UserProfile } from '../components/layout/Header';

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
  const ui = {
    title: language === 'en' ? 'User Profile' : '使用者檔案',
    desc: language === 'en' ? 'Manage your personal information and security settings.' : '管理您的個人資訊與系統安全設定。',
    btnLogout: language === 'en' ? 'Sign Out' : '登出系統',
    btnSave: language === 'en' ? 'Save Changes' : '儲存基本資料',
    status: language === 'en' ? 'Status' : '狀態',
    active: language === 'en' ? 'Active' : '使用中',
    privileges: language === 'en' ? 'System Privileges' : '系統權限',
    personalInfo: language === 'en' ? 'Personal Information' : '個人資訊',
    titleLabel: language === 'en' ? 'Title' : '稱謂',
    firstName: language === 'en' ? 'First Name' : '名字',
    lastName: language === 'en' ? 'Last Name' : '姓氏',
    email: language === 'en' ? 'E-Mail Address' : '電子郵件',
    department: language === 'en' ? 'Department' : '所屬部門',
    telephone: language === 'en' ? 'Telephone' : '公司電話',
    extension: language === 'en' ? 'Extension' : '分機',
    mobile: language === 'en' ? 'Mobile Phone' : '行動電話',
    role: language === 'en' ? 'Primary Role' : '主要職位',
    security: language === 'en' ? 'Security Settings' : '安全性設定',
    password: language === 'en' ? 'Account Password' : '帳號密碼',
    passwordDesc: language === 'en' ? 'Last changed: 3 months ago. We recommend updating regularly.' : '上次變更：3 個月前。我們建議您定期更新密碼。',
    changePassword: language === 'en' ? 'Change Password' : '變更密碼',
    twoFactor: language === 'en' ? 'Two-Factor Authentication (2FA)' : '雙重驗證 (2FA)',
    twoFactorDesc: language === 'en' ? '2FA adds an extra layer of security.' : '2FA 為您的帳號增加額外的安全防護。',
    enabled: language === 'en' ? 'Enabled' : '已啟用',
    disabled: language === 'en' ? 'Disabled' : '已停用',
    employeeId: language === 'en' ? 'Employee ID' : '員工編號',
    username: language === 'en' ? 'Username' : '未命名使用者',
    uploadAvatar: language === 'en' ? 'Upload avatar image' : '上傳頭像圖片',
    mr: language === 'en' ? 'Mr.' : '先生',
    ms: language === 'en' ? 'Ms.' : '女士',
    dr: language === 'en' ? 'Dr.' : '博士',
    modalTitle: language === 'en' ? 'Change Password' : '變更密碼',
    oldPassword: language === 'en' ? 'Current Password' : '目前密碼',
    newPassword: language === 'en' ? 'New Password' : '新密碼',
    confirmPassword: language === 'en' ? 'Confirm New Password' : '確認新密碼',
    cancel: language === 'en' ? 'Cancel' : '取消',
    confirm: language === 'en' ? 'Update Password' : '確認變更'
  };

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
  const [showSuccess, setShowSuccess] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const currentFullName = `${formData.firstName} ${formData.lastName}`.trim();
  const displayRole = user?.role || 'ROLE_PUBLIC';
  const displayRoleLabel = roleLabels[displayRole] || roleLabels.ROLE_PUBLIC;

  const regexName = /^[\u4e00-\u9fa5a-zA-Z\s]+$/;
  const regexDept = /^[\u4e00-\u9fa5a-zA-Z0-9\s]+$/;
  const regexEmail = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  const regexTel = /^(\+886-3|03)-\d{7}$/;
  const regexExt = /^\d{7}$/;
  const regexMobile = /^09\d{2}-\d{3}-\d{3}$/;

  const getInitials = (name: string) => {
    if (!name) return '';
    if (/[\u4e00-\u9fa5]/.test(name)) return name.charAt(0);
    const parts = name.trim().split(/\s+/);
    if (parts.length >= 2) return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    return parts[0].substring(0, 2).toUpperCase();
  };

  const handleAvatarChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;
    if (!file.type.startsWith('image/')) {
      onNotify(null, 'Validation Error', 'Avatar must be an image file.', 'error');
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      const nextAvatar = String(reader.result || '');
      setAvatarBase64(nextAvatar);
      onUpdateUser({
        name: currentFullName || user?.name || ui.username,
        role: displayRole,
        avatarBase64: nextAvatar
      });
      onNotify(null, 'Avatar Updated', 'Profile image has been stored as Base64.', 'success');
    };
    reader.onerror = () => onNotify(null, 'Upload Error', 'Could not read the selected image.', 'error');
    reader.readAsDataURL(file);
  };

  const handleToggle2FA = () => {
    const nextState = !formData.is2FA;
    setFormData({ ...formData, is2FA: nextState });
    onNotify(
      null,
      'Security Settings Updated',
      `Two-Factor Authentication (2FA) has been ${nextState ? 'enabled' : 'disabled'}.`,
      nextState ? 'success' : 'warning'
    );
  };

  const handlePasswordUpdate = () => {
    if (!pwdForm.oldPassword || !pwdForm.newPassword || !pwdForm.confirmPassword) {
      onNotify(null, 'Validation Error', 'All password fields are required.', 'error');
      return;
    }
    if (pwdForm.newPassword !== pwdForm.confirmPassword) {
      onNotify(null, 'Validation Error', 'New passwords do not match.', 'error');
      return;
    }

    setShowPwdModal(false);
    setPwdForm({ oldPassword: '', newPassword: '', confirmPassword: '' });
    onNotify(null, 'Password Changed', 'Your account password has been updated successfully.', 'success');
  };

  const handleSave = () => {
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
      onNotify(null, 'Validation Error', 'Mobile Phone must match 0912-345-678.', 'error');
      return;
    }

    onUpdateUser({
      name: currentFullName || ui.username,
      role: displayRole,
      avatarBase64
    });

    onNotify(null, 'Profile Synchronized', 'User settings successfully pushed to global layout.', 'success');
    setShowSuccess(true);
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
              <div className={`h-full w-full overflow-hidden rounded-2xl border-4 border-slate-100 flex items-center justify-center shadow-inner transition-all ${currentFullName && !avatarBase64 ? 'bg-corporate-blue text-white font-bold text-4xl' : 'bg-slate-100 text-slate-400'}`}>
                {avatarBase64 ? (
                  <img src={avatarBase64} alt="" className="h-full w-full object-cover" />
                ) : currentFullName ? (
                  getInitials(currentFullName)
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
                <span className="text-slate-900 font-mono">#TS-0000</span>
              </div>
              <div className="flex justify-between font-medium">
                <span className="text-slate-500">{ui.status}</span>
                <span className="inline-flex items-center gap-1.5 text-emerald-600">
                  <span className="h-1.5 w-1.5 rounded-full bg-emerald-600 animate-pulse"></span>
                  {ui.active}
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
                  <p className="text-xs text-slate-500">{ui.passwordDesc}</p>
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
                <div className="border-b border-slate-50 bg-slate-50/50 px-8 py-4">
                  <h3 className="font-bold text-slate-800">{ui.modalTitle}</h3>
                </div>
                <div className="p-8 space-y-4">
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
                    <button onClick={() => { setShowPwdModal(false); setPwdForm({ oldPassword: '', newPassword: '', confirmPassword: '' }); }} className="px-4 py-2 bg-slate-100 text-slate-600 text-xs font-bold rounded-lg hover:bg-slate-200 cursor-pointer">
                      {ui.cancel}
                    </button>
                    <button onClick={handlePasswordUpdate} className="px-4 py-2 bg-corporate-blue text-white text-xs font-bold rounded-lg hover:bg-blue-700 shadow-md cursor-pointer">
                      {ui.confirm}
                    </button>
                  </div>
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
                <h3 className="text-xl font-bold text-slate-900">{language === 'en' ? 'Profile Saved' : '檔案更新成功'}</h3>
                <p className="text-sm text-slate-500 font-medium leading-relaxed">
                  {language === 'en' ? 'Your personal information and settings have been synchronized.' : '您的個人資訊與設定已同步完成。'}
                </p>
                <button onClick={() => setShowSuccess(false)} className="w-full mt-4 px-4 py-3 bg-slate-900 text-white text-sm font-bold rounded-xl hover:bg-slate-800 transition-all shadow-md active:scale-95 cursor-pointer">
                  {language === 'en' ? 'Understood' : '確認'}
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
