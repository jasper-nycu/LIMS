// src/components/layout/Sidebar.tsx
import React from 'react';
import { type UserProfile } from './Header';

interface SidebarProps {
  isOpen: boolean;
  onToggle: () => void;
  activeView: string;
  onViewChange: (view: string) => void;
  language: 'en' | 'tw';
  user: UserProfile;
}

// Translations dictionary for sidebar labels
const i18nLabels = {
  en: {
    'view-factory-request': 'Fab Request',
    'view-lab-operations': 'Lab Operations',
    'view-manager-dashboard': 'Manager Dashboard',
    'view-capacity-analytics': 'Capacity Analytics',
    'view-my-profile': 'My Profile'
  },
  tw: {
    'view-factory-request': '建立委託單',
    'view-lab-operations': '實驗室操作',
    'view-manager-dashboard': '簽核儀表板',
    'view-capacity-analytics': '產能分析儀表板',
    'view-my-profile': '個人設定'
  }
};

export const Sidebar: React.FC<SidebarProps> = ({ 
  isOpen, 
  onToggle, 
  activeView, 
  onViewChange, 
  language,
  user
}) => {
  const navItems = [
    { id: 'view-factory-request', icon: 'description', roles: ['ROLE_SYSADMIN', 'ROLE_FAB_USER'] },
    { id: 'view-lab-operations', icon: 'biotech', roles: ['ROLE_SYSADMIN', 'ROLE_LAB_MANAGER', 'ROLE_LAB_OPERATOR', 'ROLE_MACHINE_OWNER'] },
    { id: 'view-manager-dashboard', icon: 'dashboard', roles: ['ROLE_SYSADMIN', 'ROLE_LAB_MANAGER'] },
    { id: 'view-capacity-analytics', icon: 'insights', roles: ['ROLE_SYSADMIN', 'ROLE_LAB_MANAGER', 'ROLE_MACHINE_OWNER'] },
    { id: 'view-my-profile', icon: 'account_circle' }
  ].filter(item => !item.roles || item.roles.includes(user.role));

  return (
    <>
      {/* Mobile Sidebar Overlay */}
      {isOpen && (
        <div 
          className="fixed inset-0 bg-slate-900/50 z-30 md:hidden transition-opacity"
          onClick={onToggle}
        ></div>
      )}

      <aside className={`
        absolute inset-y-0 left-0 z-40 w-64 transform transition-all duration-300 ease-in-out bg-white border-r border-slate-200 flex flex-col p-4 space-y-2 h-full
        ${isOpen ? 'translate-x-0 relative' : '-translate-x-full md:-ml-64'}
      `}>
        {/* Mobile Close Button */}
        <div className="md:hidden flex justify-end mb-2">
          <button onClick={onToggle} className="p-1 text-slate-400 hover:text-slate-600 focus:outline-none cursor-pointer">
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>

        <nav className="space-y-1">
          {navItems.map((item) => {
            const isActive = activeView === item.id;
            const label = i18nLabels[language][item.id as keyof typeof i18nLabels['en']];
            
            return (
              <button
                key={item.id}
                onClick={() => {
                  onViewChange(item.id);
                  if (window.innerWidth < 768) onToggle();
                }}
                className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-all cursor-pointer ${
                  isActive 
                    ? 'bg-corporate-blue text-white shadow-sm' 
                    : 'text-slate-600 hover:bg-slate-50 hover:text-corporate-blue'
                }`}
              >
                <span className="material-symbols-outlined text-[20px]">{item.icon}</span>
                <span className="whitespace-nowrap">{label}</span>
              </button>
            );
          })}
        </nav>
      </aside>
    </>
  );
};