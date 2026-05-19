// src/components/layout/Header.tsx
import React, { useState } from 'react';

export interface UserProfile {
  employeeId?: string;
  name: string;
  role: string;
}

export interface NotificationData {
  id: string;
  title: string;
  desc: string;
  type: 'info' | 'success' | 'error' | 'warning';
  read?: boolean;
}

interface HeaderProps {
  onToggleMenu: () => void;
  language: 'en' | 'tw';
  onLanguageChange: (lang: 'en' | 'tw') => void;
  user: UserProfile | null;
  onProfileClick: () => void;
  notifications: NotificationData[];
  onDeleteNotification: (id: string) => void;
  onClearAllNotifications: () => void;
  hasNew: boolean;
  onMarkAsRead: () => void;
}

export const Header: React.FC<HeaderProps> = ({ 
  onToggleMenu, 
  language, 
  onLanguageChange, 
  user,
  onProfileClick,
  notifications,
  onDeleteNotification,
  onClearAllNotifications,
  hasNew,
  onMarkAsRead
}) => {
  const [showNotifs, setShowNotifs] = useState(false);
  const [isClearing, setIsClearing] = useState(false);

  const handleClearAll = () => {
    setIsClearing(true);
    // Wait for the fade-out animation (300ms) to complete before actual deletion
    setTimeout(() => {
      onClearAllNotifications();
      setIsClearing(false);
    }, 300);
  };

  // Localization Dictionary
  const i18n = {
    en: {
      title: 'System Notifications',
      empty: 'No new notifications',
      new: 'New',
      clear: 'Clear All'
    },
    tw: {
      title: '系統通知',
      empty: '目前沒有新通知',
      new: '則新訊息',
      clear: '一鍵刪除'
    }
  };

  const ui = i18n[language];

  const handleToggleNotifs = (e: React.MouseEvent) => {
    e.stopPropagation();
    const nextState = !showNotifs;
    setShowNotifs(nextState);
    if (nextState) onMarkAsRead();
  };

  return (
    <header className="sticky top-0 z-50 bg-white border-b border-slate-200 px-4 md:px-6 py-3 flex items-center justify-between shadow-sm relative">
      <div className="flex items-center gap-2 md:gap-3">
        <button onClick={onToggleMenu} className="p-1.5 -ml-1 text-slate-500 hover:bg-slate-100 rounded-lg transition-colors focus:outline-none cursor-pointer">
          <span className="material-symbols-outlined text-[24px]">menu</span>
        </button>
        <div className="bg-corporate-blue p-1.5 md:p-2 rounded-lg text-white flex items-center justify-center">
          <span className="material-symbols-outlined text-[20px] md:text-[24px]">science</span>
        </div>
        <span className="text-lg md:text-xl font-bold tracking-tight hidden sm:block">
          LIMS <span className="text-corporate-blue font-normal">Cloud-Native</span>
        </span>
      </div>

      <div className="flex items-center gap-3 md:gap-6">
        {/* Sliding Language Switcher */}
        <div className="relative flex bg-slate-100 p-0.5 rounded-lg text-[11px] md:text-xs font-bold shadow-inner border border-slate-200 h-8 md:h-9">
          <div className="lang-pill" style={{ left: language === 'en' ? '2px' : '50%', width: 'calc(50% - 2px)' }} />
          <button onClick={() => onLanguageChange('en')} className={`relative z-10 px-3 md:px-4 py-1 transition-colors duration-300 cursor-pointer ${language === 'en' ? 'text-corporate-blue' : 'text-slate-500'}`}>EN</button>
          <button onClick={() => onLanguageChange('tw')} className={`relative z-10 px-3 md:px-4 py-1 transition-colors duration-300 cursor-pointer ${language === 'tw' ? 'text-corporate-blue' : 'text-slate-500'}`}>中文</button>
        </div>

        <div className="flex items-center gap-2 md:gap-4">
          <div className="relative">
            <button onClick={handleToggleNotifs} className="relative p-1.5 md:p-2 text-slate-500 hover:bg-slate-100 rounded-full transition-colors focus:outline-none cursor-pointer">
              <span className="material-symbols-outlined text-[22px] md:text-[24px]">notifications</span>
              {/* Red dot appears only if hasNew is true and there are messages */}
              {hasNew && notifications.length > 0 && (
                <span className="absolute top-2 right-2 flex h-2 w-2">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-red-400 opacity-75"></span>
                  <span className="relative inline-flex rounded-full h-2 w-2 bg-red-500 border border-white"></span>
                </span>
              )}
            </button>
            
            {showNotifs && (
              <div className="absolute right-0 mt-3 w-72 md:w-80 glass-panel rounded-2xl p-4 z-50 animate-[fadeIn_0.2s_ease-out] shadow-xl border border-slate-200/60" onClick={(e) => e.stopPropagation()}>
                <div className="flex justify-between items-center mb-4 px-1">
                  <div className="flex items-center gap-2">
                    <h3 className="text-sm font-bold text-slate-800">{ui.title}</h3>
                    {notifications.length > 0 && (
                      <span className="text-[10px] font-bold text-white bg-red-500 px-2 py-0.5 rounded-full shadow-sm">
                        {notifications.length} {ui.new}
                      </span>
                    )}
                  </div>
                  {notifications.length > 0 && (
                    <button 
                      onClick={(e) => { e.stopPropagation(); handleClearAll(); }}
                      className="text-[10px] font-bold text-corporate-blue hover:text-blue-700 cursor-pointer transition-colors"
                    >
                      {ui.clear}
                    </button>
                  )}
                </div>

                <div className={`space-y-2 max-h-72 overflow-y-auto custom-scrollbar pr-1 transition-all duration-300 transform ${isClearing ? 'opacity-0 translate-x-8 blur-sm' : 'opacity-100 translate-x-0'}`}>
                  {notifications.length > 0 ? (
                    notifications.map((notif) => (
                      <div key={notif.id} className="group relative flex justify-between items-start p-3 bg-white/50 rounded-xl border border-slate-100 transition-all">
                        <div className="flex gap-3">
                          <span className={`material-symbols-outlined text-sm mt-0.5 ${notif.type === 'error' ? 'text-red-500' : 'text-corporate-blue'}`}>info</span>
                          <div>
                            <p className="text-xs font-bold text-slate-800">{notif.title}</p>
                            <p className="text-[10px] text-slate-500">{notif.desc}</p>
                          </div>
                        </div>
                        <button onClick={() => onDeleteNotification(notif.id)} className="opacity-0 md:group-hover:opacity-100 text-slate-400 hover:text-red-500 cursor-pointer"><span className="material-symbols-outlined text-[16px]">close</span></button>
                      </div>
                    ))
                  ) : (
                    <div className="py-10 text-center">
                      <span className="material-symbols-outlined text-slate-200 text-[40px] mb-2 block">notifications_off</span>
                      <p className="text-slate-400 text-[11px] italic">{ui.empty}</p>
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>

          <div className="h-6 md:h-8 w-px bg-slate-200"></div>
          
          <div onClick={onProfileClick} className="flex items-center gap-2 md:gap-3 cursor-pointer group pr-1">
            <div className="text-right hidden sm:block">
              <p className="text-xs md:text-sm font-bold text-slate-900 group-hover:text-corporate-blue transition-colors leading-tight">{user?.name || "Username"}</p>
              <p className="text-[9px] md:text-[10px] text-slate-400 font-bold uppercase tracking-widest mt-1">{user?.role || "Public"}</p>
            </div>
            <div className="w-8 h-8 md:w-10 md:h-10 rounded-full bg-slate-100 border border-slate-300 flex items-center justify-center text-slate-400 shadow-inner group-hover:border-corporate-blue/30 transition-all overflow-hidden">
              <span className="material-symbols-outlined text-[24px] md:text-[28px] translate-y-0.5 select-none">person</span>
            </div>
          </div>
        </div>
      </div>
    </header>
  );
};
