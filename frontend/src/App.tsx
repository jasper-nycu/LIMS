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
import api from './api/axiosInstance';
import { getUtilizationHistory } from './api/machineApi';

interface Owner {
  initials: string;
  color: string;
}

interface MachineHistoryPoint {
  timestamp: number;
  util: number;
  state?: MachineState['state'];
}

interface MachineState {
  id: string;
  state: 'PROCESSING' | 'IDLE' | 'ALARM' | 'MAINTENANCE';
  loaded: string[];
  cap: number;
  expKey: string;
  name: string;
  error: string | null;
  currentUtil: number;
  owners: Owner[];
  loadedCount?: number;
  utilHistory?: MachineHistoryPoint[];
}

const App: React.FC = () => {
  const [isSidebarOpen, setIsSidebarOpen] = useState<boolean>(true);
  const [activeView, setActiveView] = useState<string>('view-factory-request');
  const [language, setLanguage] = useState<'en' | 'tw'>('en');
  
  // 1. Set notifications to an empty array for Stateless requirement
  const [notifications, setNotifications] = useState<NotificationData[]>([]);
  const deletedNotificationIds = useRef<Set<string>>(new Set());
  
  // 2. Set initial notification badge to false
  const [hasNew, setHasNew] = useState<boolean>(false); 

  // 3. JWT Auto-Restore (Stateless Session Recovery)
  // Initialize user synchronously from localStorage so the app does not flash the login screen on refresh.
  const [user, setUser] = useState<UserProfile | null>(() => {
    const token = localStorage.getItem('lims_jwt');
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        if (payload.exp * 1000 > Date.now()) {
          // avatarBase64 is intentionally empty here; the effect below will fetch it asynchronously.
          return { empId: payload.sub, name: payload.name, role: payload.role, avatarBase64: '' } as UserProfile;
        }
      } catch (e) {
        localStorage.removeItem('lims_jwt');
      }
    }
    return null;
  });

  // Fetch avatar after page refresh if the user was restored from JWT but has no avatar loaded yet.
  useEffect(() => {
    if (!user || user.avatarBase64) return;
    api.get('/users/me/avatar')
      .then(res => {
        if (res.data?.avatarBase64) {
          setUser(prev => prev ? { ...prev, avatarBase64: res.data.avatarBase64 } : prev);
        }
      })
      .catch(() => {
        // 404 means the user has no avatar yet, ignore silently.
      });
  }, [user?.empId]); // Re-run only when the logged-in user changes, not on every render.

  const [capacitySelectedMachineId, setCapacitySelectedMachineId] = useState<string>('');
  const [capacityTimeRange, setCapacityTimeRange] = useState<string>('1h');

  const now = Date.now();
  const makeHistory = (entries: Array<[number, number]>) =>
    entries.map(([minutesAgo, util]) => ({ timestamp: now - minutesAgo * 60_000, util }));

  const [machines, setMachines] = useState<Record<string, MachineState>>({
    'SEM-01': {
      id: 'SEM-01',
      state: 'PROCESSING',
      loaded: Array.from({ length: 18 }, (_, i) => `W-10${(i+1).toString().padStart(2, '0')}`),
      cap: 25,
      expKey: 'exp_sem',
      name: 'Surface Scan (SEM)',
      error: null,
      currentUtil: 72,
      owners: [{ initials: 'MW', color: 'bg-slate-600' }, { initials: 'JS', color: 'bg-blue-400' }],
      loadedCount: 18,
      utilHistory: makeHistory([
        [60, 65],
        [50, 55],
        [40, 82],
        [20, 0],
        [12, 28],
        [5, 60],
        [0, 72]
      ])
    },
    'BAKE-OVEN-01': {
      id: 'BAKE-OVEN-01',
      state: 'IDLE',
      loaded: [],
      cap: 50,
      expKey: 'exp_bake',
      name: 'High-Temp Bake',
      error: null,
      currentUtil: 0,
      owners: [{ initials: 'SC', color: 'bg-accent-sky' }],
      loadedCount: 0,
      utilHistory: makeHistory([
        [90, 20],
        [60, 40],
        [30, 50],
        [18, 0],
        [10, 0],
        [4, 0],
        [0, 0]
      ])
    },
    'TEM-01': {
      id: 'TEM-01',
      state: 'MAINTENANCE',
      loaded: [],
      cap: 10,
      expKey: 'exp_deep',
      name: 'Deep Analysis',
      error: null,
      currentUtil: 0,
      owners: [{ initials: 'RK', color: 'bg-red-500' }],
      loadedCount: 0,
      utilHistory: makeHistory([
        [0, 0]
      ])
    },
    'FIB-01': {
      id: 'FIB-01',
      state: 'IDLE',
      loaded: [],
      cap: 1,
      expKey: 'exp_fib',
      name: 'Focused Ion Beam',
      error: null,
      currentUtil: 0,
      owners: [{ initials: 'CH', color: 'bg-indigo-500' }],
      loadedCount: 0,
      utilHistory: makeHistory([
        [55, 0],
        [38, 0],
        [22, 20],
        [16, 0],
        [8, 0],
        [2, 0],
        [0, 0]
      ])
    },
    'E-TEST-02': {
      id: 'E-TEST-02',
      state: 'PROCESSING',
      loaded: Array.from({ length: 42 }, (_, i) => `W-20${(i+1).toString().padStart(2, '0')}`),
      cap: 50,
      expKey: 'exp_etest',
      name: 'Electrical Test',
      error: null,
      currentUtil: 84,
      owners: [{ initials: 'AS', color: 'bg-emerald-600' }],
      loadedCount: 42,
      utilHistory: makeHistory([
        [70, 92],
        [50, 88],
        [35, 80],
        [25, 78],
        [12, 84],
        [6, 84],
        [0, 84]
      ])
    },
    'XRD-01': {
      id: 'XRD-01',
      state: 'IDLE',
      loaded: [],
      cap: 25,
      expKey: 'exp_xrd',
      name: 'X-Ray Diffraction',
      error: null,
      currentUtil: 0,
      owners: [{ initials: 'TH', color: 'bg-amber-500' }],
      loadedCount: 0,
      utilHistory: makeHistory([
        [120, 0],
        [90, 10],
        [55, 18],
        [34, 12],
        [18, 0],
        [7, 0],
        [0, 0]
      ])
    }
  });

  // Update a machine partially and maintain utilHistory (newest-first)
  const updateMachine = (id: string, patch: Partial<MachineState>) => {
    setMachines(prev => {
      const m = prev[id] ?? { id, state: 'IDLE', loaded: [], cap: 1, expKey: '', name: id, error: null, currentUtil: 0, owners: [], loadedCount: 0, utilHistory: Array.from({ length: 7 }, () => ({ timestamp: Date.now(), util: 0 })) };
      const newUtil = patch.currentUtil ?? m.currentUtil;
      // Clone existing history entries to avoid accidental shared references between machines
      const oldHist = (m.utilHistory ?? Array.from({ length: 7 }, () => ({ timestamp: Date.now(), util: m.currentUtil ?? 0 }))).map(p => ({ ...p }));
      const shouldAppendHistory = patch.currentUtil !== undefined && patch.currentUtil !== null || patch.state !== undefined;
      const newHist = shouldAppendHistory
        ? [{ timestamp: Date.now(), util: newUtil, state: patch.state ?? m.state }, ...oldHist].slice(0, 100)
        : oldHist;
      return { ...prev, [id]: { ...m, ...patch, currentUtil: newUtil, utilHistory: newHist } };
    });
  };

  useEffect(() => {
    if (!user) return; // Don't fetch data if not authenticated

    const fetchMachines = async () => {
      try {
        const res = await api.get('/machines');
        const data = res.data;
        setMachines((prev) => {
          const next = { ...prev };
          const ownerColorPalette = ['bg-slate-600','bg-blue-400','bg-emerald-600','bg-red-500','bg-indigo-500','bg-amber-500','bg-accent-sky','bg-purple-500'];
          data.forEach((m: any) => {
            if (!next[m.id]) {
              // Machine missing from state (e.g. cleared on logout) — rebuild from backend
              next[m.id] = {
                id: m.id,
                name: m.name ?? m.id,
                state: (m.state as MachineState['state']) ?? 'IDLE',
                loaded: m.loadedWafers ?? [],
                cap: m.cap ?? 1,
                expKey: m.expKey ?? '',
                error: m.error ?? null,
                currentUtil: m.currentUtil ?? 0,
                owners: (m.owners ?? []).map((initials: string, idx: number) => ({
                  initials,
                  color: ownerColorPalette[idx % ownerColorPalette.length],
                })),
                loadedCount: m.loadedCount ?? 0,
                utilHistory: [{ timestamp: Date.now(), util: m.currentUtil ?? 0 }],
              };
            }
            if (next[m.id]) {
              // Preserve existing state but incorporate timestamped history coming from backend when present
              const existing = next[m.id];
              // Normalize incoming history if provided as objects with timestamps
              let incomingHistory: MachineHistoryPoint[] | undefined;
              const rawHist = m.utilHistory || m.history || m.util_history;
              if (Array.isArray(rawHist) && rawHist.length > 0) {
                if (typeof rawHist[0] === 'object' && rawHist[0] !== null && ('timestamp' in rawHist[0] || 'ts' in rawHist[0])) {
                  incomingHistory = rawHist.map((it: any) => ({ timestamp: Number(it.timestamp ?? it.ts), util: Number(it.util ?? it.v ?? it.value ?? 0) }));
                } else if (typeof rawHist[0] === 'number') {
                  // If backend provided numeric series without timestamps, space them ending at optional updatedAt or now
                  const endTs = Number(m.updatedAt ?? m.lastUpdated ?? m.lastChangeTs ?? Date.now());
                  const pts = rawHist.length;
                  incomingHistory = rawHist.map((val: number, idx: number) => ({ timestamp: endTs - Math.round(((pts - 1 - idx) / Math.max(1, pts - 1)) * (60 * 60 * 1000)), util: Number(val) }));
                }
              }

              // If backend supplies a last-change timestamp (e.g. unload time), and currentUtil changed, append that point
              const maybeTs = Number(m.lastUnloadAt ?? m.lastChangedAt ?? m.updatedAt ?? m.lastUpdated ?? m.lastChangeTs ?? 0) || undefined;
              const incomingCurrentUtil = m.currentUtil ?? existing.currentUtil;

              const mergedHistory = (() => {
                // Start from existing history (oldest-first)
                const base = (existing.utilHistory ?? []).slice();
                // If backend provided an explicit history, prefer it (merge recent points)
                if (incomingHistory && incomingHistory.length) {
                  // merge by timestamp, keeping unique timestamps and latest util
                  const byTs = new Map<number, number>();
                  [...base, ...incomingHistory].forEach(p => {
                    if (p && p.timestamp) byTs.set(p.timestamp, p.util);
                  });
                  const merged = Array.from(byTs.entries()).map(([timestamp, util]) => ({ timestamp, util }));
                  merged.sort((a, b) => a.timestamp - b.timestamp);
                  return merged.slice(-100);
                }

                // If backend reports a change timestamp, add that point
                if (maybeTs && maybeTs > 0) {
                  // Only append if it represents an actual change vs last recorded timestamp
                  const lastTs = base.length ? base[base.length - 1].timestamp : 0;
                  if (maybeTs !== lastTs) {
                    base.push({ timestamp: maybeTs, util: incomingCurrentUtil });
                  }
                } else if (incomingCurrentUtil !== existing.currentUtil) {
                  // No timestamp provided; append now
                  base.push({ timestamp: Date.now(), util: incomingCurrentUtil });
                }

                // Keep bounded history
                return base.slice(-100);
              })();

              next[m.id] = {
                ...next[m.id],
                state: (m.state as MachineState['state']) || next[m.id].state,
                cap: m.cap || next[m.id].cap,
                loaded: m.loadedWafers ?? [],
                currentUtil: incomingCurrentUtil,
                error: m.error ?? next[m.id].error,
                loadedCount: m.loadedCount ?? next[m.id].loadedCount,
                utilHistory: mergedHistory
              };
            }
          });
          return next;
        });
      } catch (error) {
        console.error('Could not load machines from backend', error);
      }
    };

    fetchMachines();
  }, [user]); // Trigger machine data fetch on user login/logout to ensure we have the latest data when authenticated

  // Fetch utilization history for all machines periodically to keep charts updated
  useEffect(() => {
    if (!user || Object.keys(machines).length === 0) return;

    const fetchAllHistories = async () => {
      try {
        // Fetch 7-day history (168 hours) for each machine
        const historyPromises = Object.keys(machines).map(machineId =>
          getUtilizationHistory(machineId, 168)
            .then(history => ({ machineId, history }))
            .catch(err => {
              console.warn(`Failed to fetch utilization history for ${machineId}:`, err);
              return { machineId, history: [] };
            })
        );

        const results = await Promise.all(historyPromises);

        // Update each machine with its history
        setMachines(prev => {
          const next = { ...prev };
          results.forEach(({ machineId, history }) => {
            if (history && history.length > 0 && next[machineId]) {
              const convertedHistory = history.map((point: any) => ({
                timestamp: Number(point.timestamp),
                util: Number(point.utilization ?? point.util ?? 0),
                state: point.state
              }));
              next[machineId].utilHistory = convertedHistory;
            }
          });
          return next;
        });
      } catch (error) {
        console.warn('Failed to fetch utilization histories:', error);
      }
    };

    // Fetch on initial load and then every 30 seconds
    fetchAllHistories();
    const intervalId = setInterval(fetchAllHistories, 30000);

    return () => clearInterval(intervalId);
  }, [user, Object.keys(machines).length]);

  useEffect(() => {
    const handleAuthExpired = () => {
      localStorage.removeItem('lims_jwt');
      setMachines({});
      setUser(null);
      setCapacitySelectedMachineId('');
    };

    window.addEventListener('auth-expired', handleAuthExpired);
    return () => window.removeEventListener('auth-expired', handleAuthExpired);
  }, []);

 const handleLogout = async () => {
    try {
      // Notify backend to set is_active to false
      await api.post('/users/me/logout');
      // Dispatch a log request to the backend database before removing authorization context
      await api.post('/users/me/notifications/logout-log');
    } catch (e) {
      console.warn('Backend logout sync failed', e);
    }
    localStorage.removeItem('lims_jwt'); // Clear token to prevent infinite login loop
    setMachines({});
    setUser(null);
    setNotifications([]);
    setHasNew(false);
    setActiveView('view-factory-request'); // Redirect on logout
  };
  
  const handleLoginSuccess = async (nextUser: UserProfile) => {
    setCapacitySelectedMachineId('');
    setCapacityTimeRange('1h');
    setUser(nextUser);

    if (nextUser.role === 'ROLE_FAB_USER') {
      setActiveView('view-factory-request');
    } else if (nextUser.role === 'ROLE_LAB_OPERATOR') {
      setActiveView('view-lab-operations');
    } else if (nextUser.role === 'ROLE_LAB_MANAGER') {
      setActiveView('view-manager-dashboard');
    } else if (nextUser.role === 'ROLE_MACHINE_OWNER') {
      setActiveView('view-capacity-analytics');
    } else { // ROLE_SYSADMIN and ROLE_PUBLIC
      setActiveView('view-my-profile');
    }

    try {
      const avatarRes = await api.get('/users/me/avatar');
      if (avatarRes.data?.avatarBase64) {
        setUser({ ...nextUser, avatarBase64: avatarRes.data.avatarBase64 });
      }
    } catch {
      // If 404 error, just mean user has no avatar, so we can ignore it.
    }
  };

  const toggleSidebar = () => setIsSidebarOpen(prev => !prev);
  const navigateToProfile = () => setActiveView('view-my-profile');
  
  const addNotification = (
    _titleKey: string | null,
    fallbackTitle: string,
    desc: string,
    type: 'info' | 'success' | 'error' | 'warning' = 'info'
  ) => {
    // 1. Show locally to the sender immediately
    const newNotif: NotificationData = {
      id: Date.now().toString(),
      title: fallbackTitle,
      desc,
      type,
      read: false
    };
    setNotifications(prev => [newNotif, ...prev]);
    setHasNew(true);

    // Asynchronously dispatch custom event to enforce zero-latency notification synchronization
    setTimeout(() => {
      window.dispatchEvent(new CustomEvent('sync-notifications'));
    }, 500);
  };

  const handleMarkAsRead = async () => {
    if (hasNew) {
      // Only dismiss the badge — notifications stay visible so the user can read them.
      // They will not reappear on next login because the backend marks them is_read = true.
      setHasNew(false);
      try {
        await api.put('/users/me/notifications/read');
      } catch (error) {
        console.warn('Failed to sync read status to backend', error);
      }
    }
  };

  const handleDeleteNotif = (id: string) => {
    // The notification is already is_read = true on the backend (set when the user opened the tray).
    // Just remove it from the local UI and track the ID so polling doesn't bring it back.
    deletedNotificationIds.current.add(id);
    const updated = notifications.filter(n => n.id !== id);
    setNotifications(updated);
    if (updated.length === 0) setHasNew(false);
  };

  // Destructive transaction to clear all personal alerts safely without altering concurrent modules
  const handleClearAllNotifs = async () => {
    try {
      // Direct call to the newly added DELETE endpoint on the backend resource
      await api.delete('/users/me/notifications');
      setNotifications([]);
      setHasNew(false);
    } catch (error) {
      console.warn('Network layer returned error while purging notifications. Executing defensive local state split.', error);
      // Defensive fallback: clear local states anyway to protect user experience from interceptor side effects
      setNotifications([]);
      setHasNew(false);
    }
  };

  // --- Start Real-time Notification Polling & Event-Driven Sync ---
  useEffect(() => {
    if (!user) return; // Do not poll if the user is not authenticated

    const fetchNotifications = async () => {
      try {
        // Fetch notifications from the backend NotificationController
        const res = await api.get('/users/me/notifications'); 
        
        if (Array.isArray(res.data)) {
          const incoming: NotificationData[] = res.data
            .map((n: any) => ({
              id: n.notifId?.toString(),
              title: n.title,
              desc: n.message,
              type: n.type || 'info',
              read: false
            }))
            .filter((n: NotificationData) => !deletedNotificationIds.current.has(n.id));

          // Polling must ONLY inject brand-new notifications into the existing list.
          // It must NOT overwrite the list — removal is exclusively the user's action
          // (single X or Clear All). This prevents the tray from self-clearing after
          // the user opens it and the backend stops returning is_read=true items.
          setNotifications(prev => {
            const existingIds = new Set(prev.map(n => n.id));
            const brandNew = incoming.filter(n => !existingIds.has(n.id));
            // Prepend new items; existing items stay until the user removes them
            return brandNew.length > 0 ? [...brandNew, ...prev] : prev;
          });

          // Light up the badge only for brand-new arrivals not previously seen
          setHasNew(prev => {
            if (prev) return true; // Badge already on, keep it
            const existingIds = new Set(notifications.map(n => n.id));
            return incoming.some(n => !existingIds.has(n.id));
          });
        }
      } catch (error) {
        console.warn('Failed to fetch notifications from backend', error);
      }
    };

    // Execute immediately upon component mount or user login
    fetchNotifications();

    // Setup short polling interval (fetch every 10 seconds)
    const intervalId = setInterval(fetchNotifications, 10000);

    // Event-driven immediate listener to intercept business state mutations
    const handleInstantSync = () => fetchNotifications();
    window.addEventListener('sync-notifications', handleInstantSync);

    // Cleanup the interval and listener when the component unmounts or user changes
    return () => {
      clearInterval(intervalId);
      window.removeEventListener('sync-notifications', handleInstantSync);
    };
  }, [user]);
  // --- End Real-time Notification Polling & Event-Driven Sync ---

  const renderContent = () => {
    // Standardized View Injection
    switch (activeView) {
      case 'view-factory-request':
        // Mapping simple notify to the standardized 4-param notify
        return <FabRequestView language={language} user={user} onNotify={(t, d, tp) => addNotification(null, t, d, tp)} />;
      case 'view-lab-operations':
        return <LabOperationsView language={language} user={user} onNotify={addNotification} machines={machines} updateMachine={updateMachine} />;
      case 'view-manager-dashboard':
        return <ManagerDashboardView language={language} user={user} onNotify={addNotification} />;
      case 'view-capacity-analytics':
        return (
          <CapacityAnalyticsView
            language={language}
            machines={machines}
            selectedMachineId={capacitySelectedMachineId}
            onSelectedMachineIdChange={setCapacitySelectedMachineId}
            timeRange={capacityTimeRange}
            onTimeRangeChange={setCapacityTimeRange}
          />
        );
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
        onLoginSuccess={handleLoginSuccess}
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
          user={user} 
        />

        <main className="flex-1 overflow-y-auto p-4 md:p-8 bg-slate-50 transition-all custom-scrollbar">
          {renderContent()}
        </main>
      </div>
    </div>
  );
};

export default App;
