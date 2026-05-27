// src/App.tsx
import React, { useEffect, useRef, useState } from 'react';
import { Header, type UserProfile, type NotificationData } from './components/layout/Header';
import { Sidebar } from './components/layout/Sidebar';
import { FabRequestView } from './views/FabRequestView';
import { LabOperationsView } from './views/LabOperationsView';
import { ManagerDashboardView } from './views/ManagerDashboardView';
import { CapacityAnalyticsView } from './views/CapacityAnalyticsView';
import { MyProfileView } from './views/MyProfileView';
import { AuthView } from './views/AuthView';
import { apiDelete, apiGet, apiPostVoid } from './api';

const resolveEmployeeId = (user: UserProfile | null): string | undefined => {
  if (!user) return undefined;
  if (user.employeeId) return user.employeeId;
  if (user.role === 'ROLE_SYSADMIN') return 'TS-0001';
  if (user.role === 'ROLE_FAB_USER') return 'TS-1001';
  if (user.role === 'ROLE_LAB_MANAGER') return 'TS-9001';
  return undefined;
};

const App: React.FC = () => {
  const [isSidebarOpen, setIsSidebarOpen] = useState<boolean>(true);
  const [activeView, setActiveView] = useState<string>('view-factory-request');
  const [language, setLanguage] = useState<'en' | 'tw'>('en');
  
  // 1. Set notifications to an empty array for Stateless requirement
  const [notifications, setNotifications] = useState<NotificationData[]>([]);
  const deletedNotificationIds = useRef<Set<string>>(new Set());
  
  // 2. Set initial notification badge to false
  const [hasNew, setHasNew] = useState<boolean>(false); 

  // User set to null (Public/Stateless mode)
  const [user, setUser] = useState<UserProfile | null>(null);

  const refreshNotifications = async () => {
    const employeeId = resolveEmployeeId(user);
    if (!employeeId) return;
    try {
      const serverNotifications = await apiGet<NotificationData[]>(`/api/v1/notifications?employeeId=${encodeURIComponent(employeeId)}`);
      const visibleNotifications = serverNotifications.filter(notif => !deletedNotificationIds.current.has(notif.id));
      setNotifications(visibleNotifications);
      setHasNew(visibleNotifications.some(notif => !notif.read));
    } catch {
      // Keep the existing in-session notifications when the backend is not running.
    }
  };

  useEffect(() => {
    refreshNotifications();
  }, [user?.role, user?.employeeId]);

  const apiUser = user ? { ...user, employeeId: resolveEmployeeId(user) } : null;

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
      setNotifications(prev => prev.map(notif => ({ ...notif, read: true })));
    }
    const employeeId = resolveEmployeeId(user);
    if (employeeId) {
      apiPostVoid(`/api/v1/notifications/read?employeeId=${encodeURIComponent(employeeId)}`).catch(() => undefined);
    }
  };

  const handleDeleteNotif = (id: string) => {
    deletedNotificationIds.current.add(id);
    const updated = notifications.filter(n => n.id !== id);
    setNotifications(updated);
    if (updated.length === 0) setHasNew(false);
    const employeeId = resolveEmployeeId(user);
    if (employeeId) {
      apiDelete(`/api/v1/notifications/${encodeURIComponent(id)}?employeeId=${encodeURIComponent(employeeId)}`).catch(() => undefined);
    }
  };

  const handleClearAllNotifs = () => {
    notifications.forEach(notif => deletedNotificationIds.current.add(notif.id));
    setNotifications([]);
    setHasNew(false);
    const employeeId = resolveEmployeeId(user);
    if (employeeId) {
      apiDelete(`/api/v1/notifications?employeeId=${encodeURIComponent(employeeId)}`).catch(() => undefined);
    }
  };

  const renderContent = () => {
    // Standardized View Injection
    switch (activeView) {
      case 'view-factory-request':
        // Mapping simple notify to the standardized 4-param notify
        return <FabRequestView language={language} user={apiUser} onNotify={(t, d, tp) => addNotification(null, t, d, tp)} onRefreshNotifications={refreshNotifications} />;
      case 'view-lab-operations':
        return <LabOperationsView language={language} onNotify={addNotification} />;
      case 'view-manager-dashboard':
        return <ManagerDashboardView language={language} user={apiUser} onNotify={addNotification} onRefreshNotifications={refreshNotifications} />;
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

  // Public Access Control Routing Layer
  if (!user) {
    return (
      <AuthView 
        language={language}
        onLanguageChange={setLanguage}
        onLoginSuccess={setUser}
        onNotify={addNotification}
      />
    );
  }

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
