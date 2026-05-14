// src/App.tsx
import React, { useState } from 'react';
import { Header, type UserProfile, type NotificationData } from './components/layout/Header';
import { Sidebar } from './components/layout/Sidebar';

const App: React.FC = () => {
  const [isSidebarOpen, setIsSidebarOpen] = useState<boolean>(true);
  const [activeView, setActiveView] = useState<string>('view-factory-request');
  const [language, setLanguage] = useState<'en' | 'tw'>('en');
  
  // 1. Set notifications to an empty array for Stateless requirement
  const [notifications, setNotifications] = useState<NotificationData[]>([]);
  
  // 2. Set initial notification badge to false
  const [hasNew, setHasNew] = useState<boolean>(false); 

  // User set to null (Public/Stateless mode)
  const [user] = useState<UserProfile | null>(null);

  const toggleSidebar = () => setIsSidebarOpen(prev => !prev);
  const navigateToProfile = () => setActiveView('view-my-profile');
  
  const handleMarkAsRead = () => {
    if (notifications.length > 0) {
      setHasNew(false);
    }
  };

  const handleDeleteNotif = (id: string) => {
    const updated = notifications.filter(n => n.id !== id);
    setNotifications(updated);
    if (updated.length === 0) setHasNew(false);
  };

  const renderContent = () => {
    const titles: Record<string, { en: string; tw: string }> = {
      'view-factory-request': { en: 'Fab Request', tw: '建立委託單' },
      'view-lab-operations': { en: 'Lab Operations', tw: '實驗室操作' },
      'view-manager-dashboard': { en: 'Manager Dashboard', tw: '簽核儀表板' },
      'view-capacity-analytics': { en: 'Capacity Analytics', tw: '產能分析' },
      'view-my-profile': { en: 'My Profile', tw: '個人設定' }
    };
    const title = titles[activeView] || titles['view-factory-request'];
    return (
      <h1 className="text-2xl md:text-3xl font-bold text-slate-200 uppercase tracking-[0.25em] text-center select-none">
        {language === 'en' ? title.en : title.tw}
      </h1>
    );
  };

  return (
    <div className="flex flex-col h-screen w-full overflow-hidden">
      <Header 
        onToggleMenu={toggleSidebar} 
        language={language}
        onLanguageChange={setLanguage}
        user={user}
        onProfileClick={navigateToProfile}
        notifications={notifications}
        onDeleteNotification={handleDeleteNotif}
        hasNew={hasNew}
        onMarkAsRead={handleMarkAsRead}
      />

      <div className="flex flex-1 overflow-hidden relative">
        <Sidebar 
          isOpen={isSidebarOpen} 
          onToggle={toggleSidebar} 
          activeView={activeView} 
          onViewChange={setActiveView} 
          language={language} 
        />
        <main className="flex-1 overflow-y-auto p-4 md:p-8 bg-slate-50 transition-all custom-scrollbar">
          <div className="max-w-7xl mx-auto bg-white p-6 md:p-10 rounded-2xl shadow-sm border border-slate-100 flex items-center justify-center min-h-[60vh]">
            {renderContent()}
          </div>
        </main>
      </div>
    </div>
  );
};

export default App;