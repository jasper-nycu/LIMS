// src/App.tsx
import React, { useState } from 'react';
import { Header, type UserProfile, type NotificationData } from './components/layout/Header';
import { Sidebar } from './components/layout/Sidebar';
import { FabRequestView } from './views/FabRequestView';
import { LabOperationsView } from './views/LabOperationsView';
import { ManagerDashboardView } from './views/ManagerDashboardView';
import { CapacityAnalyticsView } from './views/CapacityAnalyticsView';
import { MyProfileView } from './views/MyProfileView';

const App: React.FC = () => {
  const [isSidebarOpen, setIsSidebarOpen] = useState<boolean>(true);
  const [activeView, setActiveView] = useState<string>('view-factory-request');
  const [language, setLanguage] = useState<'en' | 'tw'>('en');
  
  // 1. Set notifications to an empty array for Stateless requirement
  const [notifications, setNotifications] = useState<NotificationData[]>([]);
  
  // 2. Set initial notification badge to false
  const [hasNew, setHasNew] = useState<boolean>(false); 

  // User set to null (Public/Stateless mode)
  const [user, setUser] = useState<UserProfile | null>(null);

  const handleLogout = () => {
    setUser(null);
    setActiveView('view-factory-request'); // Redirect on logout
  };

  const toggleSidebar = () => setIsSidebarOpen(prev => !prev);
  const navigateToProfile = () => setActiveView('view-my-profile');
  
  // Function to bridge views with the header notifications
  const addNotification = (_titleKey: string | null, fallbackTitle: string, desc: string, type: 'info' | 'success' | 'error' | 'warning' = 'info') => {
    const newNotif: NotificationData = {
      id: Date.now().toString(),
      title: fallbackTitle, // In a real app, titleKey would be used with i18n lookup
      desc,
      type
    };
    setNotifications(prev => [newNotif, ...prev]);
    setHasNew(true);
  };

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

  const handleClearAllNotifs = () => {
    setNotifications([]);
  };

  const renderContent = () => {
    // Standardized View Injection
    switch (activeView) {
      case 'view-factory-request':
        // Mapping simple notify to the standardized 4-param notify
        return <FabRequestView language={language} onNotify={(t, d, tp) => addNotification(null, t, d, tp)} />;
      case 'view-lab-operations':
        return <LabOperationsView language={language} onNotify={addNotification} />;
      case 'view-manager-dashboard':
        return <ManagerDashboardView language={language} onNotify={addNotification} />;
      case 'view-capacity-analytics':
        return <CapacityAnalyticsView language={language} />;
      case 'view-my-profile':
        return <MyProfileView 
          language={language} 
          user={user} 
          onNotify={addNotification}
          onUpdateUser={setUser} 
          onLogout={handleLogout} 
        />;
      default:
        const titles: Record<string, { en: string; tw: string }> = {
          'view-manager-dashboard': { en: 'Manager Dashboard', tw: '簽核儀表板' },
          'view-capacity-analytics': { en: 'Capacity Analytics', tw: '產能分析' },
          'view-my-profile': { en: 'My Profile', tw: '個人設定' }
        };
        const title = titles[activeView] || { en: 'Under Development', tw: '功能開發中' };
        return (
          <div className="max-w-7xl mx-auto bg-white p-6 md:p-10 rounded-2xl shadow-sm border border-slate-100 flex items-center justify-center min-h-[60vh]">
            <h1 className="text-2xl md:text-3xl font-bold text-slate-200 uppercase tracking-[0.25em] text-center select-none">
              {language === 'en' ? title.en : title.tw}
            </h1>
          </div>
        );
    }
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
        onClearAllNotifications={handleClearAllNotifs}
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
          {renderContent()}
        </main>
      </div>
    </div>
  );
};

export default App;