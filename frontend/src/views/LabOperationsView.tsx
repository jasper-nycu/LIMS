// src/views/LabOperationsView.tsx
import React, { useEffect, useState } from 'react';
import * as machineApi from '../api/machineApi';
import type { LogEntry, MachineDTO } from '../api/machineApi';

// --- Interfaces ---
interface MachineState {
  id: string;
  state: 'IDLE' | 'PROCESSING' | 'ALARM' | 'MAINTENANCE';
  loaded: string[];
  cap: number;
  expKey: string;
  name: string;
  error: string | null;
  currentUtil: number;
  owners: { initials: string; color: string }[];
}

export interface WipWafer {
  id: string;
  expKey: string;
  priority: 'NORMAL' | 'URGENT' | 'CRITICAL';
}

interface LabOperationsViewProps {
  language: 'en' | 'tw';
  onNotify: (titleKey: string | null, fallbackTitle: string, desc: string, type: 'info' | 'success' | 'error' | 'warning') => void;
  machines?: Record<string, MachineState>;
  updateMachine?: (id: string, patch: Partial<MachineState>) => void;
  initialWips?: WipWafer[];
  user?: { empId: string; name: string; role: string; avatarBase64?: string; } | null;
}

// Deterministic color map for owner initials
const OWNER_COLORS: Record<string, string> = {
  MW: 'bg-slate-600', JS: 'bg-blue-400', SC: 'bg-sky-400',
  RK: 'bg-red-500',  CH: 'bg-indigo-500', AS: 'bg-emerald-600', TH: 'bg-amber-500',
};
const ownerColor = (init: string) => OWNER_COLORS[init] ?? 'bg-slate-500';

function toMachineState(dto: MachineDTO): MachineState {
  return {
    id: dto.id,
    state: dto.state,
    loaded: dto.loadedWafers ?? [],
    cap: dto.cap,
    expKey: dto.expKey,
    name: dto.name,
    error: dto.error,
    currentUtil: dto.currentUtil,
    owners: (dto.owners ?? []).map(init => ({ initials: init, color: ownerColor(init) })),
  };
}

const defaultMachineState: Record<string, MachineState> = {
  'SEM-01': { id: 'SEM-01', state: 'PROCESSING', loaded: Array.from({length: 18}, (_, i) => `W-10${(i+1).toString().padStart(2, '0')}`), cap: 25, expKey: 'exp_sem', name: 'Surface Scan (SEM)', error: null, currentUtil: 72, owners: [{ initials: 'MW', color: 'bg-slate-600' }, { initials: 'JS', color: 'bg-blue-400' }] },
  'BAKE-OVEN-01': { id: 'BAKE-OVEN-01', state: 'IDLE', loaded: [], cap: 50, expKey: 'exp_bake', name: 'High-Temp Bake', error: null, currentUtil: 0, owners: [{ initials: 'SC', color: 'bg-accent-sky' }] },
  'TEM-01': { id: 'TEM-01', state: 'IDLE', loaded: [], cap: 10, expKey: 'exp_deep', name: 'Deep Analysis', error: null, currentUtil: 0, owners: [{ initials: 'RK', color: 'bg-red-500' }] },
  'FIB-01': { id: 'FIB-01', state: 'IDLE', loaded: [], cap: 1, expKey: 'exp_fib', name: 'Focused Ion Beam', error: null, currentUtil: 0, owners: [{ initials: 'CH', color: 'bg-indigo-500' }] },
  'E-TEST-02': { id: 'E-TEST-02', state: 'PROCESSING', loaded: Array.from({length: 42}, (_, i) => `W-20${(i+1).toString().padStart(2, '0')}`), cap: 50, expKey: 'exp_etest', name: 'Electrical Test', error: null, currentUtil: 84, owners: [{ initials: 'AS', color: 'bg-emerald-600' }] },
  'XRD-01': { id: 'XRD-01', state: 'IDLE', loaded: [], cap: 25, expKey: 'exp_xrd', name: 'X-Ray Diffraction', error: null, currentUtil: 0, owners: [{ initials: 'TH', color: 'bg-amber-500' }] }
};

export const LabOperationsView: React.FC<LabOperationsViewProps> = ({ language, onNotify, machines = defaultMachineState, updateMachine, initialWips = [], user }) => {
  const [localMachines, setLocalMachines] = useState<Record<string, MachineState>>(machines);
  const sharedMachines = updateMachine ? machines : localMachines;
  const updateMachineLocal = (id: string, patch: Partial<MachineState>) => {
    setLocalMachines(prev => {
      const m = prev[id] ?? defaultMachineState[id] ?? { id, state: 'IDLE', loaded: [], cap: 1, expKey: '', name: id, error: null, currentUtil: 0, owners: [], loadedCount: 0, utilHistory: Array(7).fill(0) } as MachineState;
      const newUtil = patch.currentUtil ?? m.currentUtil;
      const oldHist = (m as any).utilHistory ?? Array(7).fill(m.currentUtil ?? 0);
      const newHist = (patch.currentUtil !== undefined && patch.currentUtil !== null) ? [newUtil, ...oldHist].slice(0,7) : oldHist;
      return { ...prev, [id]: { ...m, ...patch, currentUtil: newUtil, utilHistory: newHist } };
    });
  };
  const applyUpdate = updateMachine ?? updateMachineLocal;
  const i18n = {
    en: {
      wip: 'Pending Wafers for Sorting (WIP)', dispatch: 'Dispatch to Machine',
      target: 'Target Machine', recipe: 'Recipe', execute: 'Execute Dispatch',
      fsm: 'Machine Management', unload: 'Unload', simulate: 'Simulate Error',
      resolve: 'Resolve Alarm', idle: 'IDLE', processing: 'PROCESSING', alarm: 'ALARM', maintenance: 'MAINTENANCE',
      exp_bake: 'High-Temp Bake', exp_etest: 'Electrical Test', exp_sem: 'Surface Scan (SEM)',
      exp_deep: 'Deep Layer Analysis', exp_fib: 'Focused Ion Beam (FIB)', exp_xrd: 'X-Ray Diffraction (XRD)',
      sel_count: 'Wafers Selected', idle_mode: 'Status: Ready for dispatch', lbl_cap: 'Capacity',
      btn_manage_mach: 'Manage Machines', btn_manage_recipe: 'Manage Recipes',
      lbl_loaded: 'Loaded:', lbl_error: 'Error:', btn_view_logs: 'View Logs',
      btn_online: 'Set Online', cap_err_title: 'Capacity Error',
      cap_err_msg: 'Total wafers exceed machine capacity.',
      queue_empty: 'Queue is empty. All caught up!', emg_unload: 'EMG Unload',
      modal_mach_title: 'Manage Machines', modal_rec_title: 'Manage Recipes',
      emg_title: 'Emergency Unload', emg_desc: 'is currently processing. Choose how to handle wafers:',
      btn_scrap: 'Scrap Wafers (Discard)', btn_reuse: 'Reuse Wafers (Return to WIP)',
      warn_title: 'Process Mismatch', warn_desc: 'Machine exclusively supports: ',
      warn_select: 'Please select at least one wafer.', err_maint: 'Cannot maintain while processing.',
      tbl_wafer_id: 'Wafer ID', tbl_req_exp: 'Required Exp.', tbl_priority: 'Priority',
      fsm_owners: 'Owners', sel_count_msg: 'Wafers Selected',
      log_title: 'System Event Logs', log_download: 'Download Logs',
      notif_rec_added: 'Recipe successfully added to machine.'
    },
    tw: {
      wip: '待分貨晶圓 (WIP)', dispatch: '派發至機台',
      target: '目標機台', recipe: '實驗參數 (Recipe)', execute: '執行派發',
      fsm: '機台管理與狀態', unload: '下貨', simulate: '模擬報錯',
      resolve: '解除警報', idle: '閒置', processing: '執行中', alarm: '異常', maintenance: '維修中',
      exp_bake: '高溫烘烤', exp_etest: '電性測試', exp_sem: '表面掃描',
      exp_deep: '深層結構分析', exp_fib: '聚焦離子束', exp_xrd: 'X光繞射',
      sel_count: '片晶圓已選擇', idle_mode: '狀態: 待機中', lbl_cap: '剩餘容量',
      btn_manage_mach: '機台管理', btn_manage_recipe: 'Recipe 管理',
      lbl_loaded: '當前載入:', lbl_error: '錯誤碼:', btn_online: '恢復上線',
      cap_err_title: '超出容量限制', cap_err_msg: '晶圓數量超出機台容量上限',
      queue_empty: '目前無待處理項目！', emg_unload: '緊急下貨',
      modal_mach_title: '機台狀態監控', modal_rec_title: '參數管理',
      emg_title: '緊急下貨處理', emg_desc: '正在運作中。請選擇晶圓處置方式：',
      btn_scrap: '報廢晶圓 (Discard)', btn_reuse: '重用晶圓 (回到 WIP)',
      warn_title: '製程不匹配', warn_desc: '此機台僅支援執行：',
      warn_select: '請至少選擇一片晶圓。', err_maint: '機台執行中，無法變更維護狀態。',
      tbl_wafer_id: '晶圓編號', tbl_req_exp: '需求實驗', tbl_priority: '優先權',
      btn_view_logs: '歷史紀錄', fsm_owners: '負責人', sel_count_msg: '已選擇',
      log_title: '機台事件日誌', log_download: '下載日誌內容',
      notif_rec_added: '實驗參數已成功新增至機台。'
    }
  };

  const ui = i18n[language];

  // --- States ---
  const [wips, setWips] = useState<WipWafer[]>(initialWips);
  const [selectedWipIds, setSelectedWips] = useState<Set<string>>(new Set());
  const [showMachModal, setShowMachModal] = useState(false);
  const [showRecModal, setShowRecModal] = useState(false);
  const [warning, setWarning] = useState<{ isOpen: boolean; title: string; msg: string }>({ isOpen: false, title: '', msg: '' });
  const [emgModal, setEmgModal] = useState<{ isOpen: boolean; machId: string | null }>({ isOpen: false, machId: null });
  const [logModal, setLogModal] = useState<{ isOpen: boolean; machId: string | null; entries: LogEntry[] }>({ isOpen: false, machId: null, entries: [] });
  const [newRecipeInput, setNewRecipeInput] = useState('');
  const [machines, setMachines] = useState<Record<string, MachineState>>({});
  const [recipes, setRecipes] = useState<Record<string, string[]>>({});
  const [targetMachId, setTargetMachId] = useState('');
  const [selectedRecipeId, setSelectedRecipeId] = useState('');

  // --- Data loading ---
  const loadMachines = async () => {
    try {
      const dtos = await machineApi.getMachines();
      const machMap: Record<string, MachineState> = {};
      const recipeMap: Record<string, string[]> = {};
      await Promise.all(dtos.map(async dto => {
        machMap[dto.id] = toMachineState(dto);
        recipeMap[dto.id] = await machineApi.getRecipes(dto.id);
      }));
      setMachines(machMap);
      setRecipes(recipeMap);
      if (!targetMachId && dtos.length > 0) {
        setTargetMachId(dtos[0].id);
      }
    } catch (e) {
      console.error('Failed to load machines', e);
    }
  };

  const loadWips = async () => {
    if (initialWips.length > 0) return;
    try {
      const [queue, pending] = await Promise.all([
        machineApi.getWipQueue(),
        machineApi.getWipPendingSorting(),
      ]);
      setWips([...queue, ...pending].map(t => ({
        id: t.waferCode,
        expKey: t.expKey,
        priority: (t.priority as 'NORMAL' | 'URGENT' | 'CRITICAL') ?? 'NORMAL',
      })));
    } catch (e) {
      console.error('Failed to load WIP', e);
    }
  };

  const refreshMachine = async (id: string) => {
    try {
      const dto = await machineApi.getMachine(id);
      setMachines(prev => ({ ...prev, [id]: toMachineState(dto) }));
    } catch (e) {
      console.error(`Failed to refresh machine ${id}`, e);
    }
  };

  useEffect(() => {
    loadMachines();
    loadWips();
  }, []);

  useEffect(() => {
    const list = recipes[targetMachId];
    setSelectedRecipeId(list?.[0] ?? '');
  }, [targetMachId, recipes]);

  const toggleAllWips = () => {
    if (selectedWipIds.size === wips.length && wips.length > 0) {
      setSelectedWips(new Set());
    } else {
      setSelectedWips(new Set(wips.map(w => w.id)));
    }
  };

<<<<<<< HEAD
=======

  const [recipes, setRecipes] = useState<Record<string, string[]>>({
    'SEM-01': ['SEM-Surface-Std', 'SEM-High-Res'], 'BAKE-OVEN-01': ['Bake-150C-4H', 'Bake-250C-2H'],
    'TEM-01': ['TEM-Lattice-View'], 'FIB-01': ['FIB-Cross-Section', 'FIB-Circuit-Edit'],
    'E-TEST-02': ['E-TEST-Parametric', 'E-TEST-Yield'], 'XRD-01': ['XRD-Crystal-Scan']
  });

  const [targetMachId, setTargetMachId] = useState('BAKE-OVEN-01');

>>>>>>> origin/main
  // --- Handlers ---
  const handleDispatch = async () => {
    if (selectedWipIds.size === 0) return setWarning({ isOpen: true, title: 'Selection Error', msg: ui.warn_select });
<<<<<<< HEAD
    const mach = machines[targetMachId];
    if (!mach) return;
=======
    const mach = sharedMachines[targetMachId];
>>>>>>> origin/main
    const selectedList = wips.filter(w => selectedWipIds.has(w.id));

    if (selectedList.some(w => w.expKey !== mach.expKey)) {
      return setWarning({ isOpen: true, title: ui.warn_title, msg: ui.warn_desc + ui[mach.expKey as keyof typeof ui] });
    }
    if (mach.loaded.length + selectedWipIds.size > mach.cap) {
      return setWarning({ isOpen: true, title: ui.cap_err_title, msg: ui.cap_err_msg });
    }
    if (mach.state === 'PROCESSING') return setWarning({ isOpen: true, title: 'Machine Busy', msg: 'Machine is currently processing.' });

<<<<<<< HEAD
    try {
      const dto = await machineApi.dispatch(targetMachId, {
        waferCodes: Array.from(selectedWipIds),
        machineId: targetMachId,
        recipeId: selectedRecipeId || (recipes[targetMachId]?.[0] ?? 'Default'),
        expKey: mach.expKey,
      });
      setMachines(prev => ({ ...prev, [targetMachId]: toMachineState(dto) }));
      setWips(wips.filter(w => !selectedWipIds.has(w.id)));
      setSelectedWips(new Set());
      onNotify(null, 'Dispatch Success', `${selectedWipIds.size} wafers sent to ${targetMachId}`, 'success');
    } catch (e) {
      setWarning({ isOpen: true, title: 'Dispatch Error', msg: (e as Error).message });
    }
=======
    if (mach.loaded.length + selectedWipIds.size > mach.cap) return setWarning({ isOpen: true, title: 'Capacity Error', msg: 'Exceeded machine capacity.' });
    
    applyUpdate(targetMachId, { state: 'PROCESSING', loaded: [...mach.loaded, ...Array.from(selectedWipIds)], currentUtil: Math.round(((mach.loaded.length + selectedWipIds.size) / mach.cap) * 100) });
    setWips(wips.filter(w => !selectedWipIds.has(w.id)));
    setSelectedWips(new Set());
    onNotify(null, 'Dispatch Success', `${selectedWipIds.size} wafers sent to ${targetMachId}`, 'success');
>>>>>>> origin/main
  };

  const handleEMGAction = async (action: 'SCRAP' | 'REUSE') => {
    const id = emgModal.machId; if (!id) return;
    const mach = sharedMachines[id];

<<<<<<< HEAD
    try {
      const dto = await machineApi.emgUnload(id, action);
      setMachines(prev => ({ ...prev, [id]: toMachineState(dto) }));

      if (action === 'REUSE') {
        const returned: WipWafer[] = mach.loaded.map(wId => ({ id: wId, expKey: mach.expKey, priority: 'NORMAL' }));
        setWips(prev => [...returned, ...prev]);
        onNotify(null, language === 'en' ? 'Wafers Reused' : '晶圓重用', `${mach.loaded.length} ${language === 'en' ? 'wafers returned to WIP.' : '片晶圓已回到 WIP。'}`, 'info');
      } else {
=======
    if (action === 'REUSE') {
        // Correct reuse logic: Re-insert into WIP with the original machine expKey
        const returned: WipWafer[] = mach.loaded.map(wId => ({ 
            id: wId, 
            expKey: mach.expKey, 
            priority: 'NORMAL' 
        }));
        setWips([...returned, ...wips]);
        applyUpdate(id, { state: 'IDLE', loaded: [], currentUtil: 0 });
        onNotify(null, language === 'en' ? 'Wafers Reused' : '晶圓重用', `${mach.loaded.length} ${language === 'en' ? 'wafers returned to WIP.' : '片晶圓已回到 WIP。'}`, 'info');
    } else {
        // Correct Scrap Logic: Return to IDLE directly without fault code per instructions
        applyUpdate(id, { state: 'IDLE', loaded: [], error: null, currentUtil: 0 });
>>>>>>> origin/main
        onNotify(null, language === 'en' ? 'Wafers Scrapped' : '晶圓作廢', `${mach.loaded.length} ${language === 'en' ? 'wafers discarded.' : '片晶圓已根據政策作廢。'}`, 'warning');
      }
    } catch (e) {
      setWarning({ isOpen: true, title: 'EMG Unload Error', msg: (e as Error).message });
    }
    setEmgModal({ isOpen: false, machId: null });
  };

<<<<<<< HEAD
  const toggleAlarm = async (id: string) => {
    const m = machines[id];
    try {
      if (m.state === 'ALARM') {
        const dto = await machineApi.resolveAlarm(id);
        setMachines(prev => ({ ...prev, [id]: toMachineState(dto) }));
        onNotify(null, 'Alarm Resolved', `${id} is back online.`, 'success', 'owners');
      } else {
        const dto = await machineApi.simulateError(id);
        setMachines(prev => ({ ...prev, [id]: toMachineState(dto) }));
        onNotify(null, language === 'en' ? '🚨 System Alert' : '🚨 系統警報', `${id} ${language === 'en' ? 'reported a simulated fault.' : '觸發模擬故障。'}`, 'error', 'owners');
      }
    } catch (e) {
      setWarning({ isOpen: true, title: 'Error', msg: (e as Error).message });
=======
  const toggleAlarm = (id: string) => {
    const m = sharedMachines[id];
    if (m.state === 'ALARM') {
        const next = m.loaded.length > 0 ? 'PROCESSING' : 'IDLE';
        applyUpdate(id, { state: next, error: null, currentUtil: m.loaded.length > 0 ? Math.round((m.loaded.length / m.cap) * 100) : 0 });
        onNotify(null, 'Alarm Resolved', `${id} is back online.`, 'success');
    } else {
        // Requirement: Keep loaded wafers during alarm
        applyUpdate(id, { state: 'ALARM', loaded: [...m.loaded], error: 'ERR_SIMULATED_FAULT', currentUtil: 0 });
        onNotify(null, language === 'en' ? '🚨 System Alert' : '🚨 系統警報', `${id} ${language === 'en' ? 'reported a simulated fault.' : '觸發模擬故障。'}`, 'error');
>>>>>>> origin/main
    }
  };

  const getBadgeStyle = (state: string) => {
    switch (state) {
      case 'PROCESSING': return 'bg-emerald-100 text-emerald-700';
      case 'ALARM': return 'bg-red-100 text-red-700';
      case 'MAINTENANCE': return 'bg-slate-200 text-slate-600';
      default: return 'bg-slate-100 text-slate-600';
    }
  };

<<<<<<< HEAD
  const toggleMaint = async (id: string) => {
    const m = machines[id];
=======
  const toggleMaint = (id: string) => {
    const m = sharedMachines[id];
    // Strict FSM: Processing machines cannot enter maintenance
>>>>>>> origin/main
    if (m.state === 'PROCESSING') {
      setWarning({ isOpen: true, title: 'Action Denied', msg: ui.err_maint });
      return;
    }
    try {
      const isMaint = m.state === 'MAINTENANCE';
      const dto = isMaint ? await machineApi.setOnline(id) : await machineApi.toMaintenance(id);
      setMachines(prev => ({ ...prev, [id]: toMachineState(dto) }));
      onNotify(null, isMaint ? 'Machine Online' : 'Machine Offline', `${id} is now ${isMaint ? 'Online' : 'under Maintenance'}.`, isMaint ? 'success' : 'info');
    } catch (e) {
      setWarning({ isOpen: true, title: 'Error', msg: (e as Error).message });
    }
  };

<<<<<<< HEAD
  const openLogModal = async (machId: string) => {
    setLogModal({ isOpen: true, machId, entries: [] });
    try {
      const entries = await machineApi.getMachineLogs(machId);
      setLogModal(prev => ({ ...prev, entries }));
    } catch {
      // show empty log
    }
=======
    const isMaint = m.state === 'MAINTENANCE';
    const nextState = isMaint ? (m.loaded.length > 0 ? 'PROCESSING' : 'IDLE') : 'MAINTENANCE';
    
    applyUpdate(id, { state: nextState, error: null, currentUtil: nextState === 'MAINTENANCE' ? 0 : m.currentUtil });

    onNotify(
        null, 
        isMaint ? 'Machine Online' : 'Machine Offline', 
        `${id} is now ${isMaint ? 'Online' : 'under Maintenance'}.`, 
        isMaint ? 'success' : 'info'
    );
>>>>>>> origin/main
  };

  return (
    <>
      <div className="space-y-6 md:space-y-8 animate-[fadeIn_0.3s_ease-out]">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 md:gap-8">
        {/* WIP Table */}
        <div className="lg:col-span-2 bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden flex flex-col">
          <div className="px-6 py-4 border-b border-slate-50 font-bold text-slate-800 tracking-tight">{ui.wip}</div>
          <div className="overflow-auto max-h-[300px] flex-1 custom-scrollbar">
            <table className="w-full text-left text-sm min-w-[500px]">
              <thead className="bg-slate-50 text-slate-500 uppercase text-[10px] sticky top-0 z-10 shadow-sm">
                <tr>
                  <th className="px-4 md:px-6 py-3 w-10">
                    <input
                      type="checkbox"
                      className="rounded border-slate-300 text-corporate-blue focus:ring-corporate-blue h-4 w-4 cursor-pointer"
                      checked={wips.length > 0 && selectedWipIds.size === wips.length}
                      onChange={toggleAllWips}
                    />
                  </th>
                  <th className="px-4 md:px-6 py-3">{ui.tbl_wafer_id}</th>
                  <th className="px-4 md:px-6 py-3">{ui.tbl_req_exp}</th>
                  <th className="px-4 md:px-6 py-3">{ui.tbl_priority}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50">
                {wips.length > 0 ? wips.map(w => (
                  <tr key={w.id} className={`hover:bg-slate-50 transition-colors ${selectedWipIds.has(w.id) ? 'bg-blue-50/40' : ''}`}>
                    <td className="px-6 py-4">
                        <input
                            type="checkbox"
                            className="rounded border-slate-300 text-corporate-blue focus:ring-corporate-blue h-4 w-4 cursor-pointer"
                            checked={selectedWipIds.has(w.id)}
                            onChange={() => {
                                const n = new Set(selectedWipIds);
                                n.has(w.id) ? n.delete(w.id) : n.add(w.id);
                                setSelectedWips(n);
                            }}
                        />
                    </td>
                    <td className="px-6 py-4 font-mono font-bold text-slate-700">{w.id}</td>
                    <td className="px-6 py-4 text-xs text-slate-500">{ui[w.expKey as keyof typeof ui] ?? w.expKey ?? '—'}</td>
                    <td className="px-6 py-4">
                      <span className={`px-2 py-1 rounded text-[10px] font-bold ${
                        w.priority === 'CRITICAL' ? 'bg-red-50 text-red-600' :
                        w.priority === 'URGENT' ? 'bg-amber-50 text-amber-600' :
                        'bg-slate-100 text-slate-600'
                      }`}>
                        {w.priority}
                      </span>
                    </td>
                  </tr>
                )): (
                  <tr className="empty-row">
                    <td colSpan={4} className="px-4 md:px-6 py-12 text-center text-slate-400">
                        <span className="material-symbols-outlined text-4xl mb-2 opacity-50">inbox</span>
                        <p className="font-bold text-sm">{ui.queue_empty}</p>
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Dispatch Control */}
        <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-100 flex flex-col justify-between">
          <h2 className="font-bold text-slate-800 border-b border-slate-50 pb-4">{ui.dispatch}</h2>
          <div className="space-y-4 py-4 flex-1">
            <div className="space-y-1">
              <label className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">{ui.sel_count}</label>
              <div className="p-3 border border-slate-200 rounded-xl bg-slate-50 text-sm font-mono text-corporate-blue font-bold tracking-wider">
                {language === 'en'
                  ? `${selectedWipIds.size} Wafers Selected`
                  : `已選擇 ${selectedWipIds.size} 片晶圓`}
              </div>
            </div>
              <div className="space-y-1"><label htmlFor="mach-op-sel" className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">{ui.target}</label>
              <select id="mach-op-sel" className="w-full rounded-xl border-slate-200 text-sm font-semibold text-slate-700 focus:ring-corporate-blue cursor-pointer" value={targetMachId} onChange={e => setTargetMachId(e.target.value)}>
                {Object.values(sharedMachines).map(m => <option key={m.id} value={m.id} disabled={m.state !== 'IDLE'}>{m.id} ({m.state === 'IDLE' ? `Idle - ${m.cap - m.loaded.length} slots` : m.state})</option>)}
              </select>
            </div>
            <div className="space-y-1"><label htmlFor="rec-op-sel" className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">{ui.recipe}</label>
              <select id="rec-op-sel" className="w-full rounded-xl border-slate-200 text-sm text-slate-700 cursor-pointer" value={selectedRecipeId} onChange={e => setSelectedRecipeId(e.target.value)}>
                {(recipes[targetMachId] || ['Default']).map(r => <option key={r} value={r}>{r}</option>)}
              </select>
            </div>
          </div>
<<<<<<< HEAD
          <button
            onClick={handleDispatch}
            className="w-full bg-corporate-blue text-white py-4 rounded-xl font-bold hover:bg-blue-700 shadow-lg shadow-blue-100 flex justify-center items-center gap-2 active:scale-95 transition-all cursor-pointer"
          >
            <span className="material-symbols-outlined">bolt</span> {ui.execute}
          </button>
=======
          {/* RBAC Compliance: Enforce distinct split between administrative supervision and shop-floor execution */}
          {user?.role !== 'ROLE_LAB_MANAGER' && (
            <button 
              onClick={handleDispatch} 
              className="w-full bg-corporate-blue text-white py-4 rounded-xl font-bold hover:bg-blue-700 shadow-lg shadow-blue-100 flex justify-center items-center gap-2 active:scale-95 transition-all cursor-pointer"
            >
              <span className="material-symbols-outlined">bolt</span> {ui.execute}
            </button>
          )}
>>>>>>> origin/main
        </div>
      </div>

      {/* FSM Cards */}
      <div>
        <div className="flex justify-between items-center mb-6 mt-6"><h2 className="font-bold text-lg text-slate-800">{ui.fsm}</h2>
          <div className="flex gap-2">
            <button onClick={() => setShowMachModal(true)} className="px-3 py-1.5 bg-white border border-slate-200 text-slate-600 text-xs font-bold rounded-lg hover:bg-slate-50 flex items-center gap-1 shadow-sm cursor-pointer transition-colors">
              <span className="material-symbols-outlined text-[16px]">settings</span> {ui.btn_manage_mach}
            </button>
            <button onClick={() => setShowRecModal(true)} className="px-3 py-1.5 bg-white border border-slate-200 text-slate-600 text-xs font-bold rounded-lg hover:bg-slate-50 flex items-center gap-1 shadow-sm cursor-pointer transition-colors">
              <span className="material-symbols-outlined text-[16px]">list_alt</span> {ui.btn_manage_recipe}
            </button>
          </div></div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {Object.values(sharedMachines).map(m => (
            <div key={m.id} className={`bg-white rounded-2xl p-5 border-2 relative overflow-hidden flex flex-col h-[380px] transition-all ${
                m.state === 'PROCESSING' ? 'border-emerald-400 shadow-[0_0_15px_rgba(16,185,129,0.15)]' :
                m.state === 'ALARM' ? 'border-red-300 bg-red-50/20' :
                m.state === 'MAINTENANCE' ? 'border-slate-300 bg-slate-100' : 'border-slate-100 shadow-sm'
            }`}>
              {m.state === 'PROCESSING' && <div className="absolute top-0 left-0 w-full h-1 bg-emerald-500 animate-pulse"></div>}
              <div className="flex justify-between items-start mb-3"><div><h3 className="font-bold text-lg text-slate-800">{m.id}</h3><p className="text-xs text-slate-500 font-mono">{ui[m.expKey as keyof typeof ui]}</p></div>
                <span className={`px-2 py-1 rounded text-[10px] font-bold shadow-sm ${getBadgeStyle(m.state)}`}>
                  {ui[m.state.toLowerCase() as keyof typeof ui] || m.state}
                </span></div>

              <div className="flex-1 overflow-y-auto custom-scrollbar mb-4 pr-1">
                {m.state === 'IDLE' ? (
                  <div className="bg-slate-50 rounded p-2 border border-slate-100 border-dashed flex items-center justify-center h-16"><p className="text-xs font-mono text-slate-400">{ui.idle_mode}</p></div>
                ) : m.state === 'ALARM' ? (
                  <div className="bg-red-100/50 rounded p-2 border border-red-200"><p className="text-[10px] font-bold text-red-700 uppercase">{ui.lbl_error}</p><p className="text-xs font-mono text-red-600 mt-0.5">{m.error}</p></div>
                ) : m.state === 'MAINTENANCE' ? (
                  <div className="bg-slate-200/50 rounded p-2 border border-slate-200 border-dashed flex items-center justify-center h-16">
                    <p className="text-xs font-bold text-slate-500 uppercase tracking-widest">{ui.maintenance}</p>
                  </div>
                ) : (
                  <div className="bg-emerald-50 rounded p-2 border border-emerald-100/50"><p className="text-[10px] font-bold text-emerald-700 uppercase">{ui.lbl_loaded}</p><p className="text-xs font-mono text-emerald-600 mt-0.5 break-all">{m.loaded.join(', ')}</p></div>
                )}
              </div>

              <div className="mt-auto space-y-4">
                <div className="space-y-1.5"><div className="flex justify-between items-center"><span className="text-[10px] font-bold text-slate-500 uppercase">{ui.lbl_cap}</span><span className="text-[10px] font-mono font-bold text-slate-800">{m.loaded.length} / {m.cap}</span></div><div className="w-full bg-slate-100 rounded-full h-1.5 overflow-hidden shadow-inner"><div className={`${m.state === 'ALARM' ? 'bg-red-500' : 'bg-corporate-blue'} h-full transition-all duration-700`} style={{ width: `${(m.loaded.length / m.cap) * 100}%` }}></div></div></div>
                <div className="flex flex-col gap-2">
                    {m.state === 'MAINTENANCE' ? (
                      <div className="flex gap-2 w-full">
                        <button
                          onClick={() => openLogModal(m.id)}
                          className="py-2 text-[10px] font-bold rounded-lg border bg-slate-50 text-slate-600 flex-1 hover:bg-slate-100 cursor-pointer transition-colors"
                        >
                          {ui.btn_view_logs}
                        </button>
                        <button
                          onClick={() => toggleMaint(m.id)}
                          className="py-2 text-[10px] font-bold rounded-lg border bg-slate-300 text-slate-700 border-transparent hover:bg-slate-400 flex-1 cursor-pointer transition-colors shadow-sm"
                        >
                          {ui.btn_online}
                        </button>
                      </div>
                    ) : (
                      <>
                        <div className="flex gap-2">
                          <button
                            onClick={() => openLogModal(m.id)}
                            className="py-2 text-[10px] font-bold rounded-lg border bg-slate-50 text-slate-600 flex-1 hover:bg-slate-100 cursor-pointer transition-colors"
                            >
                            {ui.btn_view_logs}
                            </button>
                          {m.state === 'PROCESSING' && (
<<<<<<< HEAD
                            <button
                              onClick={async () => {
                                try {
                                  const dto = await machineApi.unload(m.id);
                                  setMachines(prev => ({ ...prev, [m.id]: toMachineState(dto) }));
                                  onNotify(null, language === 'en' ? 'Unload Success' : '下貨成功', `${m.id} ${language === 'en' ? 'wafers collected.' : '晶圓已卸載。'}`, 'success');
                                } catch (e) {
                                  setWarning({ isOpen: true, title: 'Unload Error', msg: (e as Error).message });
                                }
                              }}
=======
                            <button 
                              onClick={() => {
                                applyUpdate(m.id, { state: 'IDLE', loaded: [], currentUtil: 0, error: null });
                                onNotify(null, language === 'en' ? 'Unload Success' : '下貨成功', `${m.id} ${language === 'en' ? 'wafers collected.' : '晶圓已卸載。'}`, 'success');
                              }} 
>>>>>>> origin/main
                              className="py-2 text-[10px] font-bold rounded-lg border bg-emerald-50 text-emerald-600 border-emerald-200 flex-1 cursor-pointer hover:bg-emerald-100 transition-colors"
                            >
                              {ui.unload}
                            </button>
                          )}
                        </div>
                        <div className="flex gap-2">
                          {m.state === 'PROCESSING' && (
                            <button
                              onClick={() => setEmgModal({ isOpen: true, machId: m.id })}
                              className="py-2 text-[10px] font-bold rounded-lg border bg-amber-50 text-amber-600 border-amber-100 flex-1 cursor-pointer hover:bg-amber-100 transition-colors"
                            >
                              {ui.emg_unload}
                            </button>
                          )}
                          <button
                            onClick={() => toggleAlarm(m.id)}
                            className={`py-2 text-[10px] font-bold rounded-lg border shadow-sm flex-1 cursor-pointer transition-all ${
                              m.state === 'ALARM' ? 'bg-red-500 text-white border-transparent' : 'bg-red-50 text-red-600 border-red-100 hover:bg-red-100'
                            }`}
                          >
                            {m.state === 'ALARM' ? ui.resolve : ui.simulate}
                          </button>
                        </div>
                      </>
                    )}
                </div>
                <div className="flex justify-between items-center pt-3 border-t border-slate-100">
                  <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">{ui.fsm_owners}</span>
                  <div className="flex -space-x-2">{m.owners.map((o, i) => <div key={i} className={`w-6 h-6 rounded-full ${o.color} text-white flex items-center justify-center text-[10px] font-bold border-2 border-white shadow-sm`}>{o.initials}</div>)}</div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>

      {/* --- Modals --- */}
      {/* Log Viewer Modal */}
      {logModal.isOpen && (
        <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-[fadeIn_0.2s_ease-out]">
          <div className="bg-white rounded-3xl shadow-2xl w-full max-w-2xl overflow-hidden animate-[zoomIn_0.2s_ease-out]">
            <div className="p-6 border-b border-slate-100 flex justify-between items-center bg-slate-50/50">
              <div className="flex items-center gap-3">
                <span className="material-symbols-outlined text-corporate-blue">terminal</span>
                <div>
                  <h3 className="font-bold text-lg text-slate-900">{ui.log_title}</h3>
                  <p className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Machine: {logModal.machId}</p>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <a
                  href={logModal.machId ? machineApi.getLogsDownloadUrl(logModal.machId) : '#'}
                  download
                  className="p-2 hover:bg-white rounded-full text-slate-400 hover:text-corporate-blue transition-all cursor-pointer"
                  title={ui.log_download}
                >
                  <span className="material-symbols-outlined">download</span>
                </a>
                <button onClick={() => setLogModal({ isOpen: false, machId: null, entries: [] })} className="p-2 hover:bg-white rounded-full text-slate-400 hover:text-red-500 transition-all cursor-pointer">
                  <span className="material-symbols-outlined">close</span>
                </button>
              </div>
            </div>
            <div className="p-6 bg-slate-900 overflow-y-auto max-h-[400px] custom-scrollbar font-mono text-xs leading-relaxed">
              <div className="space-y-1 text-emerald-400/90">
                {logModal.entries.length > 0 ? logModal.entries.map((entry, i) => (
                  <p key={i}>
                    <span className="text-slate-500">[{entry.timestamp.replace('T', ' ').substring(0, 19)}]</span>
                    {' '}<span className={
                      entry.level === 'ALARM' ? 'text-red-400' :
                      entry.level === 'MAINT' ? 'text-amber-400' :
                      entry.level === 'CFG'   ? 'text-blue-400' :
                      'text-emerald-400'
                    }>{entry.level}:</span>
                    {' '}{entry.message}
                  </p>
                )) : (
                  <p className="text-slate-500 italic">No log entries yet.</p>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {warning.isOpen && (
        <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-[fadeIn_0.2s_ease-out]">
          <div className="bg-white rounded-3xl shadow-2xl w-full max-w-sm p-8 text-center space-y-4 scale-100 animate-[zoomIn_0.2s_ease-out]">
            <div className="mx-auto flex items-center justify-center h-16 w-16 rounded-full bg-red-50 mb-2 border-8 border-white shadow-sm">
              <span className="material-symbols-outlined text-4xl text-red-500">error</span>
            </div>
            <h3 className="text-xl font-bold text-slate-900">{warning.title}</h3>
            <p className="text-sm text-slate-500 font-medium leading-relaxed">{warning.msg}</p>
            <button
              onClick={() => setWarning({...warning, isOpen: false})}
              className="w-full mt-4 px-4 py-3 bg-slate-900 text-white text-sm font-bold rounded-xl hover:bg-slate-800 transition-all shadow-md active:scale-95 cursor-pointer"
            >
              Understood
            </button>
          </div>
        </div>
      )}

      {emgModal.isOpen && (
        <div className="fixed inset-0 z-[9998] flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm">
          <div className="bg-white rounded-3xl shadow-2xl w-full max-w-md p-8 text-center space-y-6">
            <div className="mx-auto flex items-center justify-center h-16 w-16 rounded-full bg-amber-50 border-8 border-white shadow-sm"><span className="material-symbols-outlined text-4xl text-amber-500">warning</span></div>
            <h3 className="text-xl font-bold text-slate-900">{ui.emg_title}</h3><p className="text-sm text-slate-500 font-medium">Machine <b>{emgModal.machId}</b> {ui.emg_desc}</p>
            <div className="grid gap-3">
                <button onClick={() => handleEMGAction('SCRAP')} className="w-full p-4 border-2 border-slate-100 rounded-2xl hover:border-red-500 hover:bg-red-50 transition-all flex items-center gap-4 group cursor-pointer">
                    <span className="material-symbols-outlined text-red-500 group-hover:scale-110 transition-transform">delete_forever</span>
                    <div className="text-left font-bold text-slate-800 text-sm">{ui.btn_scrap}</div>
                </button>
                <button onClick={() => handleEMGAction('REUSE')} className="w-full p-4 border-2 border-slate-100 rounded-2xl hover:border-corporate-blue hover:bg-blue-50 transition-all flex items-center gap-4 group cursor-pointer">
                    <span className="material-symbols-outlined text-corporate-blue group-hover:scale-110 transition-transform">assignment_return</span>
                    <div className="text-left font-bold text-slate-800 text-sm">{ui.btn_reuse}</div>
                </button>
            </div>
            <button onClick={() => setEmgModal({ isOpen: false, machId: null })} className="text-xs font-bold text-slate-400 hover:text-slate-600 uppercase tracking-widest mt-4 cursor-pointer">Cancel</button>
          </div>
        </div>
      )}

      {showMachModal && (
        <div className="fixed inset-0 z-[9997] flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm">
           <div className="bg-white rounded-2xl shadow-xl w-full max-w-lg overflow-hidden">
            <div className="p-6 border-b border-slate-100 flex justify-between items-center"><h3 className="font-bold text-lg flex items-center gap-2"><span className="material-symbols-outlined">settings</span> {ui.modal_mach_title}</h3><button onClick={() => setShowMachModal(false)} className="text-slate-400 cursor-pointer"><span className="material-symbols-outlined">close</span></button></div>
            <div className="p-6 space-y-3 max-h-96 overflow-y-auto custom-scrollbar">
              {Object.values(sharedMachines).map(m => (
                <div key={m.id} className="p-4 bg-slate-50 border border-slate-200 rounded-xl flex justify-between items-center">
                  <div><h4 className="font-bold text-sm text-slate-800">{m.id}</h4><span className={`px-2 py-0.5 rounded text-[9px] font-bold uppercase shadow-sm ${getBadgeStyle(m.state)}`}>{m.state}</span></div>
                  <button onClick={() => toggleMaint(m.id)} className="w-9 h-9 flex items-center justify-center bg-white border border-slate-200 rounded-lg text-slate-500 hover:bg-slate-100 hover:text-corporate-blue shadow-sm transition-all cursor-pointer group" title="Toggle Maintenance">
                    <span className="material-symbols-outlined text-[18px] group-hover:scale-110 transition-transform">{m.state === 'MAINTENANCE' ? 'play_arrow' : 'build'}</span>
                  </button>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {showRecModal && (
        <div className="fixed inset-0 z-[9996] flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-lg overflow-hidden">
            <div className="p-6 border-b border-slate-100 flex justify-between items-center"><h3 className="font-bold text-lg flex items-center gap-2"><span className="material-symbols-outlined">list_alt</span> {ui.modal_rec_title}</h3><button onClick={() => setShowRecModal(false)} className="text-slate-400 cursor-pointer"><span className="material-symbols-outlined">close</span></button></div>
            <div className="p-6 space-y-4"><select className="w-full rounded-xl border-slate-200 text-sm cursor-pointer" onChange={(e) => setTargetMachId(e.target.value)} value={targetMachId}>{Object.keys(recipes).map(id => <option key={id} value={id}>{id}</option>)}</select>
              <div className="space-y-2 bg-slate-50 p-4 rounded-xl border border-slate-100 max-h-48 overflow-y-auto custom-scrollbar">
                {recipes[targetMachId]?.map(r => (
                  <div key={r} className="flex justify-between items-center bg-white p-2 rounded-lg border border-slate-100">
                    <span className="text-sm font-mono text-slate-700">{r}</span>
                    <button onClick={async () => {
                      try {
                        const updated = await machineApi.deleteRecipe(targetMachId, r);
                        setRecipes(prev => ({ ...prev, [targetMachId]: updated }));
                      } catch (e) {
                        setWarning({ isOpen: true, title: 'Recipe Error', msg: (e as Error).message });
                      }
                    }} className="text-slate-400 hover:text-red-500 cursor-pointer">
                      <span className="material-symbols-outlined text-[16px]">delete</span>
                    </button>
                  </div>
                ))}
              </div>
              <div className="flex gap-2">
                <input type="text" className="flex-1 rounded-xl border-slate-200 text-sm" placeholder="New recipe name..." value={newRecipeInput} onChange={e => setNewRecipeInput(e.target.value)} />
                <button
                  onClick={async () => {
                    if (!newRecipeInput.trim()) return;
                    try {
                      const updated = await machineApi.addRecipe(targetMachId, newRecipeInput.trim());
                      setRecipes(prev => ({ ...prev, [targetMachId]: updated }));
                      setNewRecipeInput('');
                      onNotify(null, language === 'en' ? 'Recipe Added' : '參數新增成功', ui.notif_rec_added, 'success');
                    } catch (e) {
                      setWarning({ isOpen: true, title: 'Recipe Error', msg: (e as Error).message });
                    }
                  }}
                  className="px-4 py-2 bg-corporate-blue text-white text-sm font-bold rounded-xl hover:bg-blue-700 cursor-pointer"
                >
                  Add
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
};
