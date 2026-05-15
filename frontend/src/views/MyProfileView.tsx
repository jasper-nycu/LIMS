// src/views/MyProfileView.tsx
import React, { useState, useEffect } from 'react';
import { type UserProfile } from '../components/layout/Header';

interface MyProfileViewProps {
  language: 'en' | 'tw';
  user: UserProfile | null;
  onUpdateUser: (updatedUser: UserProfile) => void;
  onLogout: () => void;
}

export const MyProfileView: React.FC<MyProfileViewProps> = ({ 
  language, 
  user, 
  onUpdateUser, 
  onLogout 
}) => {
  const i18n = {
    en: {
      title: 'User Profile', desc: 'Manage your personal information and security settings.',
      btn_logout: 'Sign Out', btn_save: 'Save Changes', status: 'Status', stat_active: 'Active',
      sys_priv: 'System Privileges', pers_info: 'Personal Information',
      lbl_title: 'Title', lbl_fname: 'First Name', lbl_lname: 'Last Name',
      lbl_email: 'E-Mail Address', lbl_dept: 'Department', lbl_tel: 'Telephone',
      lbl_ext: 'Extension', lbl_mobile: 'Mobile Phone', lbl_role: 'Primary Role',
      sec_settings: 'Security Settings', acc_pwd: 'Account Password',
      pwd_desc: 'Last changed: 3 months ago. We recommend updating regularly.', btn_chg_pwd: 'Change Password',
      two_fac: 'Two-Factor Authentication (2FA)', two_fac_desc: '2FA adds an extra layer of security.',
      stat_enabled: 'Enabled', stat_disabled: 'Disabled', emp_id: 'Employee ID',
      role_sysadmin: 'System Admin', role_lab_mgr: 'Lab Supervisor', role_lab_op: 'Lab Operator',
      role_fab_user: 'Fab User', role_public: 'Public',
      priv_all: 'Full System Access', priv_sec: 'Security Settings Control', priv_ana: 'Laboratory Analytics',
      priv_audit: 'Audit Logs Access', priv_req: 'Request Creation', priv_view: 'View Own Profile',
      placeholder_user: 'Username'
    },
    tw: {
      title: '使用者檔案', desc: '管理您的個人資訊與系統安全設定。',
      btn_logout: '登出系統', btn_save: '儲存基本資料', status: '狀態', stat_active: '使用中',
      sys_priv: '系統權限', pers_info: '個人資訊',
      lbl_title: '稱謂', lbl_fname: '名字', lbl_lname: '姓氏',
      lbl_email: '電子郵件', lbl_dept: '所屬部門', lbl_tel: '公司電話',
      lbl_ext: '分機', lbl_mobile: '行動電話', lbl_role: '主要職位',
      sec_settings: '安全性設定', acc_pwd: '帳號密碼',
      pwd_desc: '上次變更：3 個月前。我們建議您定期更新密碼。', btn_chg_pwd: '變更密碼',
      two_fac: '雙重驗證 (2FA)', two_fac_desc: '2FA 為您的帳號增加額外的安全防護。',
      stat_enabled: '已啟用', stat_disabled: '已停用', emp_id: '員工編號',
      role_sysadmin: '系統管理員', role_lab_mgr: '實驗室主管', role_lab_op: '實驗室人員',
      role_fab_user: '廠區使用者', role_public: '一般大眾',
      priv_all: '完整系統存取權', priv_sec: '安全性設定控制', priv_ana: '實驗室產能分析',
      priv_audit: '稽核日誌存取', priv_req: '建立委託單', priv_view: '檢視個人檔案',
      placeholder_user: '未命名使用者'
    }
  };

  const ui = i18n[language];

  // Helper to split name for Stateless initialization
  const splitName = (fullName: string | undefined) => {
    if (!fullName) return { first: '', last: '' };
    const parts = fullName.trim().split(/\s+/);
    if (parts.length >= 2) return { first: parts[0], last: parts[parts.length - 1] };
    return { first: parts[0] || '', last: '' };
  };

  // State management aligned with Prototype fields
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

  // Initials logic matching index.html
  const getInitials = (name: string) => {
    if (!name) return '';
    if (/[\u4e00-\u9fa5]/.test(name)) return name.charAt(0);
    const parts = name.trim().split(/\s+/);
    if (parts.length >= 2) return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    return parts[0].substring(0, 2).toUpperCase();
  };

  const currentFullName = `${formData.firstName} ${formData.lastName}`.trim();
  const displayRole = user?.role || 'ROLE_PUBLIC';
  const [showSuccess, setShowSuccess] = useState(false);

  // Privileges Mapping logic from Prototype RBAC
  const getPrivileges = (role: string) => {
    const map: Record<string, string[]> = {
      'ROLE_SYSADMIN': [ui.priv_all, ui.priv_sec, ui.priv_audit],
      'ROLE_LAB_MANAGER': [ui.priv_ana, ui.priv_audit],
      'ROLE_LAB_OPERATOR': [ui.priv_ana],
      'ROLE_FAB_USER': [ui.priv_req],
      'ROLE_PUBLIC': [ui.priv_view]
    };
    return map[role] || map['ROLE_PUBLIC'];
  };

  const handleSave = () => {
    onUpdateUser({
      name: currentFullName || ui.placeholder_user,
      role: displayRole // Role remains fixed as per production requirement
    });
    setShowSuccess(true);
  };

  return (
    <div className="max-w-6xl mx-auto space-y-8 animate-[fadeIn_0.3s_ease-out]">
      {/* Header Section */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl md:text-3xl font-bold tracking-tight text-slate-900">{ui.title}</h2>
          <p className="text-sm text-slate-500 mt-1">{ui.desc}</p>
        </div>
        <button 
          onClick={onLogout}
          className="rounded-xl border border-red-100 bg-red-50 px-4 py-2.5 text-sm font-bold text-red-500 hover:bg-red-100 transition-all shadow-sm flex items-center gap-2 cursor-pointer active:scale-95"
        >
          <span className="material-symbols-outlined text-[20px]">logout</span>
          {ui.btn_logout}
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left Column: Avatar & Privileges (Synced with Header) */}
        <div className="lg:col-span-1 space-y-6">
          <div className="rounded-3xl border border-slate-200 bg-white p-8 text-center shadow-sm">
            <div className="relative mx-auto mb-6 h-32 w-32 group">
              <div className={`h-full w-full overflow-hidden rounded-2xl border-4 border-slate-100 flex items-center justify-center shadow-inner transition-all ${currentFullName ? 'bg-corporate-blue text-white font-bold text-4xl' : 'bg-slate-100 text-slate-400'}`}>
                {currentFullName ? (
                  getInitials(currentFullName)
                ) : (
                  <span 
                    className="material-symbols-outlined select-none text-slate-300"
                    style={{ fontSize: '90px', lineHeight: '1', display: 'block' }}
                  >
                    person
                  </span>
                )}
              </div>
              <button className="absolute -bottom-2 -right-2 rounded-full bg-accent-sky p-2 text-white shadow-lg hover:scale-110 transition-transform cursor-pointer flex items-center justify-center">
                <span className="material-symbols-outlined text-lg">photo_camera</span>
              </button>
            </div>
            <h3 className="text-xl font-bold text-slate-900">{currentFullName || ui.placeholder_user}</h3>
            <p className="text-accent-sky font-bold text-xs uppercase tracking-widest mt-1">
              {(ui as any)[`role_${displayRole.toLowerCase().replace('role_', '')}`]}
            </p>
            
            <div className="mt-6 flex flex-col gap-3 border-t border-slate-50 pt-6 text-sm">
              <div className="flex justify-between font-medium">
                <span className="text-slate-500">{ui.emp_id}</span>
                <span className="text-slate-900 font-mono">#TS-0000</span>
              </div>
              <div className="flex justify-between font-medium">
                <span className="text-slate-500">{ui.status}</span>
                <span className="inline-flex items-center gap-1.5 text-emerald-600">
                  <span className="h-1.5 w-1.5 rounded-full bg-emerald-600 animate-pulse"></span>
                  {ui.stat_active}
                </span>
              </div>
            </div>
          </div>

          <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
            <h4 className="font-bold mb-4 flex items-center gap-2 text-slate-800 text-sm">
              <span className="material-symbols-outlined text-accent-sky">verified_user</span>
              {ui.sys_priv}
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

        {/* Right Column: Information Forms (Production Styling) */}
        <div className="lg:col-span-2 space-y-6">
          <section className="rounded-3xl border border-slate-200 bg-white shadow-sm overflow-hidden">
            <div className="border-b border-slate-50 bg-slate-50/50 px-8 py-4">
              <h3 className="font-bold text-slate-800">{ui.pers_info}</h3>
            </div>
            <div className="p-8">
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-6 mb-8">
                {/* Row 1: Title, Last, First */}
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-500">{ui.lbl_title}</label>
                  <select 
                    value={formData.title}
                    onChange={(e) => setFormData({...formData, title: e.target.value})}
                    className="w-full rounded-lg border-slate-200 bg-white py-2.5 px-4 text-sm focus:ring-corporate-blue focus:border-corporate-blue cursor-pointer"
                  >
                    <option value="Mr.">Mr.</option><option value="Ms.">Ms.</option><option value="Dr.">Dr.</option>
                  </select>
                </div>
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-500">{ui.lbl_lname}</label>
                  <input 
                    type="text" value={formData.lastName}
                    onChange={(e) => setFormData({...formData, lastName: e.target.value})}
                    className="w-full rounded-lg border-slate-200 bg-white py-2.5 px-4 text-sm focus:ring-corporate-blue focus:border-corporate-blue"
                  />
                </div>
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-500">{ui.lbl_fname}</label>
                  <input 
                    type="text" value={formData.firstName}
                    onChange={(e) => setFormData({...formData, firstName: e.target.value})}
                    className="w-full rounded-lg border-slate-200 bg-white py-2.5 px-4 text-sm focus:ring-corporate-blue focus:border-corporate-blue"
                  />
                </div>

                {/* Row 2: Dept, Email */}
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-500">{ui.lbl_dept}</label>
                  <input 
                    type="text" value={formData.dept}
                    onChange={(e) => setFormData({...formData, dept: e.target.value})}
                    className="w-full rounded-lg border-slate-200 bg-white py-2.5 px-4 text-sm focus:ring-corporate-blue"
                  />
                </div>
                <div className="space-y-1.5 sm:col-span-2">
                  <label className="text-xs font-semibold text-slate-500">{ui.lbl_email}</label>
                  <input 
                    type="email" value={formData.email}
                    onChange={(e) => setFormData({...formData, email: e.target.value})}
                    className="w-full rounded-lg border-slate-200 bg-white py-2.5 px-4 text-sm focus:ring-corporate-blue"
                  />
                </div>

                {/* Row 3: Tel, Ext, Mobile */}
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-500">{ui.lbl_tel}</label>
                  <input 
                    type="text" value={formData.tel}
                    onChange={(e) => setFormData({...formData, tel: e.target.value})}
                    className="w-full rounded-lg border-slate-200 bg-white py-2.5 px-4 text-sm focus:ring-corporate-blue"
                  />
                </div>
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-500">{ui.lbl_ext}</label>
                  <input 
                    type="text" value={formData.ext}
                    onChange={(e) => setFormData({...formData, ext: e.target.value})}
                    className="w-full rounded-lg border-slate-200 bg-white py-2.5 px-4 text-sm focus:ring-corporate-blue"
                  />
                </div>
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-500">{ui.lbl_mobile}</label>
                  <input 
                    type="text" value={formData.mobile}
                    onChange={(e) => setFormData({...formData, mobile: e.target.value})}
                    className="w-full rounded-lg border-slate-200 bg-white py-2.5 px-4 text-sm focus:ring-corporate-blue"
                  />
                </div>
              </div>

              {/* Primary Role (Read-only as per instructions) */}
              <div className="border-t border-slate-50 pt-6 mt-6 flex flex-col sm:flex-row items-end gap-6">
                <div className="space-y-1.5 flex-1 w-full">
                  <label className="text-xs font-semibold text-slate-500">{ui.lbl_role}</label>
                  <input 
                    type="text" 
                    value={(ui as any)[`role_${displayRole.toLowerCase().replace('role_', '')}`]}
                    readOnly
                    className="w-full rounded-lg border-slate-200 bg-slate-50 py-2.5 px-4 text-sm font-bold text-slate-500 cursor-not-allowed"
                  />
                </div>
                <button 
                  onClick={handleSave}
                  className="w-full sm:w-auto shrink-0 rounded-xl bg-corporate-blue px-8 py-3 text-sm font-bold text-white shadow-lg hover:bg-blue-700 transition-all flex items-center justify-center gap-2 cursor-pointer active:scale-95"
                >
                  <span className="material-symbols-outlined text-[18px]">save</span>
                  {ui.btn_save}
                </button>
              </div>
            </div>
          </section>

          {/* Security Settings Section */}
          <section className="rounded-3xl border border-slate-200 bg-white shadow-sm overflow-hidden">
            <div className="border-b border-slate-50 bg-slate-50/50 px-8 py-4">
              <h3 className="font-bold text-slate-800">{ui.sec_settings}</h3>
            </div>
            <div className="p-8 space-y-6">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div>
                  <h4 className="font-bold text-sm text-slate-900">{ui.acc_pwd}</h4>
                  <p className="text-xs text-slate-500">{ui.pwd_desc}</p>
                </div>
                <button className="px-4 py-2 bg-slate-100 text-slate-600 text-xs font-bold rounded-lg hover:bg-slate-200 cursor-pointer">
                  {ui.btn_chg_pwd}
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
                      {ui.two_fac}
                      <span className={`text-[9px] px-1.5 py-0.5 rounded font-bold uppercase ${formData.is2FA ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-500'}`}>
                        {formData.is2FA ? ui.stat_enabled : ui.stat_disabled}
                      </span>
                    </h4>
                    <p className="text-xs text-slate-500 max-w-sm mt-1">{ui.two_fac_desc}</p>
                  </div>
                </div>
                <button 
                  onClick={() => setFormData({...formData, is2FA: !formData.is2FA})}
                  className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ${formData.is2FA ? 'bg-emerald-500' : 'bg-slate-200'}`}
                >
                  <span className={`inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ${formData.is2FA ? 'translate-x-5' : 'translate-x-0'}`} />
                </button>
              </div>
            </div>
          </section>
            {/* Success Modal */}
            {showSuccess && (
                <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-[fadeIn_0.2s_ease-out]">
                    <div className="bg-white rounded-3xl shadow-2xl w-full max-w-sm p-8 text-center space-y-4 animate-[zoomIn_0.2s_ease-out]">
                        <div className="mx-auto flex items-center justify-center h-16 w-16 rounded-full bg-emerald-50 mb-2 border-8 border-white shadow-sm">
                        <span className="material-symbols-outlined text-4xl text-emerald-500">check_circle</span>
                        </div>
                        <h3 className="text-xl font-bold text-slate-900">{language === 'en' ? 'Profile Saved' : '檔案更新成功'}</h3>
                        <p className="text-sm text-slate-500 font-medium leading-relaxed">
                        {language === 'en' 
                            ? 'Your personal information and settings have been synchronized.' 
                            : '您的個人資訊與設定已同步完成。'}
                        </p>
                        <button 
                        onClick={() => setShowSuccess(false)}
                        className="w-full mt-4 px-4 py-3 bg-slate-900 text-white text-sm font-bold rounded-xl hover:bg-slate-800 transition-all shadow-md active:scale-95 cursor-pointer"
                        >
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