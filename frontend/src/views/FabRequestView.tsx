// src/views/FabRequestView.tsx
import React, { useState } from 'react';

// --- Custom Modal Component ---
interface CustomModalProps {
  isOpen: boolean;
  title: string;
  message: string;
  type: 'success' | 'error' | 'warning';
  onClose: () => void;
}

const InfoModal: React.FC<CustomModalProps> = ({ isOpen, title, message, type, onClose }) => {
  if (!isOpen) return null;
  const iconMap = {
    success: { icon: 'check_circle', color: 'text-emerald-500', bg: 'bg-emerald-50' },
    error: { icon: 'error', color: 'text-red-500', bg: 'bg-red-50' },
    warning: { icon: 'warning', color: 'text-amber-500', bg: 'bg-amber-50' },
  };
  const theme = iconMap[type];

  return (
    <div className="fixed inset-0 w-screen h-screen z-[9999] flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-[fadeIn_0.2s_ease-out]">
      <div className="bg-white rounded-3xl shadow-2xl w-full max-w-sm p-8 text-center space-y-4 scale-100 animate-[zoomIn_0.2s_ease-out]">
        <div className={`mx-auto flex items-center justify-center h-16 w-16 rounded-full ${theme.bg} mb-2 border-8 border-white shadow-sm`}>
          <span className={`material-symbols-outlined text-4xl ${theme.color}`}>{theme.icon}</span>
        </div>
        <h3 className="text-xl font-bold text-slate-900">{title}</h3>
        <p className="text-sm text-slate-500 font-medium leading-relaxed">{message}</p>
        <button 
          onClick={onClose}
          className="w-full mt-4 px-4 py-3 bg-slate-900 text-white text-sm font-bold rounded-xl hover:bg-slate-800 transition-all shadow-md active:scale-95 cursor-pointer"
        >
          Understood
        </button>
      </div>
    </div>
  );
};

interface RequestItem {
  id: string;
  experiments: string[];
  waferCount: number;
  status: string;
  priority: string;
}

export const FabRequestView: React.FC<{ 
  language: 'en' | 'tw'; 
  onNotify: (title: string, desc: string, type: 'info' | 'success' | 'error' | 'warning') => void; 
}> = ({ language, onNotify }) => {
  const labs = {
    LAB_RA: { en: 'Reliability Lab (RA)', tw: '可靠度實驗室 (RA)' },
    LAB_MA: { en: 'Material Analysis Lab (MA)', tw: '材料分析實驗室 (MA)' },
    LAB_FA: { en: 'Failure Analysis Lab (FA)', tw: '故障分析實驗室 (FA)' },
  };

  const experimentsMap: Record<string, string[]> = {
    LAB_RA: ['exp_bake', 'exp_etest'],
    LAB_MA: ['exp_sem', 'exp_deep', 'exp_xrd'],
    LAB_FA: ['exp_fib'],
  };

  const i18n = {
    en: {
      title: 'Create New Request', desc: 'Fine-grained experiment assignment per Wafer ID.',
      empId: 'Employee ID', targetLab: 'Target Laboratory', selectExp: 'Select Experiment Items',
      waferInput: 'Iterative Wafer ID Input', addBtn: 'Add Wafer', waferHint: 'W-0120',
      priority: 'Priority', priNormal: 'Normal', priUrgent: 'Urgent', priCritical: 'Critical',
      remarks: 'Order Remarks', remarksPlaceholder: 'Enter any special instructions...',
      submitBtn: 'Submit for Approval', tableTitle: 'My Requests Progress', empty: 'Queue is empty.',
      exp_bake: 'High-Temp Bake', exp_etest: 'Electrical Test', exp_sem: 'Surface Scan (SEM)',
      exp_deep: 'Deep Layer Analysis', exp_xrd: 'X-Ray Diffraction (XRD)', exp_fib: 'Focused Ion Beam (FIB)',
      colId: 'ID', colExp: 'Experiments', colCount: 'Wafers', colStatus: 'Status',
      err_format: 'Invalid Format: Must be W-XXXX', err_duplicate: 'Wafer ID already exists.',
      err_empty: 'Please add wafers and select experiments.',
      success_title: 'Request Submitted', success_msg: 'Your request has been sent for approval.',
    },
    tw: {
      title: '建立新委託單', desc: '針對各晶圓 (Wafer) 進行精細的實驗項目分配。',
      empId: '員工編號', targetLab: '目標實驗室', selectExp: '選擇實驗項目',
      waferInput: '輸入晶圓編號 (Wafer ID)', addBtn: '新增晶圓', waferHint: 'W-0120',
      priority: '優先權', priNormal: '一般', priUrgent: '急件', priCritical: '特急件',
      remarks: '委託單備註', remarksPlaceholder: '請輸入特殊需求說明...',
      submitBtn: '送出簽核', tableTitle: '我的委託單進度', empty: '目前無待處理項目！',
      exp_bake: '高溫烘烤', exp_etest: '電性測試', exp_sem: '表面掃描 (SEM)',
      exp_deep: '深層結構分析', exp_xrd: 'X光繞射 (XRD)', exp_fib: '聚焦離子束 (FIB)',
      colId: '委託單號', colExp: '實驗項目', colCount: '晶圓數量', colStatus: '狀態',
      err_format: '格式錯誤：需為 W-XXXX', err_duplicate: '該晶圓編號已存在。',
      err_empty: '請新增晶圓並選擇至少一項實驗。',
      success_title: '委託單已送出', success_msg: '您的申請已提交，請等待主管簽核。',
    }
  };

  const ui = i18n[language];

  // States
  const [selectedLab, setSelectedLab] = useState<keyof typeof labs>('LAB_RA');
  const [selectedExps, setSelectedExps] = useState<string[]>([]);
  const [waferInput, setWaferInput] = useState('');
  const [wafers, setWafers] = useState<string[]>([]);
  const [priority, setPriority] = useState('NORMAL');
  const [remarks, setRemarks] = useState('');
  const [requests, setRequests] = useState<RequestItem[]>([]);
  const [modal, setModal] = useState({ isOpen: false, title: '', message: '', type: 'info' as any });

  const handleAddWafer = () => {
    const val = waferInput.trim().toUpperCase();
    if (!/^W-[0-9]{4}$/.test(val)) {
      setModal({ isOpen: true, title: 'Invalid Format', message: ui.err_format, type: 'error' });
      return;
    }
    if (wafers.includes(val)) {
      setModal({ isOpen: true, title: 'Duplicate Entry', message: ui.err_duplicate, type: 'warning' });
      return;
    }
    setWafers([...wafers, val]);
    setWaferInput('');
  };

  const toggleExp = (key: string) => {
    setSelectedExps(prev => prev.includes(key) ? prev.filter(k => k !== key) : [...prev, key]);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (wafers.length === 0 || selectedExps.length === 0) {
      setModal({ isOpen: true, title: 'Missing Info', message: ui.err_empty, type: 'error' });
      return;
    }

    const newReq: RequestItem = {
      id: `REQ-${Math.floor(Math.random() * 9000 + 1000)}`,
      experiments: selectedExps.map(k => (ui as any)[k]),
      waferCount: wafers.length,
      status: language === 'en' ? 'PENDING' : '待處理',
      priority
    };

    setRequests([newReq, ...requests]);

    onNotify(
      language === 'en' ? 'Request Created' : '委託單已建立',
      `${newReq.id}: ${wafers.length} wafers submitted.`,
      'success'
    );

    setModal({ isOpen: true, title: ui.success_title, message: `${ui.success_msg} (${newReq.id})`, type: 'success' });
    
    setWafers([]); setSelectedExps([]); setRemarks('');
  };

  const getPriorityStyle = () => {
    switch (priority) {
      case 'URGENT': return 'bg-amber-50 border-amber-200 text-amber-700';
      case 'CRITICAL': return 'bg-red-50 border-red-200 text-red-700';
      default: return 'bg-emerald-50 border-emerald-200 text-emerald-700';
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-8 animate-[fadeIn_0.3s_ease-in-out]">
      <InfoModal {...modal} onClose={() => setModal({ ...modal, isOpen: false })} />

      <div className="bg-white p-6 md:p-10 rounded-3xl shadow-sm border border-slate-100">
        <div className="mb-8 border-b border-slate-100 pb-5">
          <h1 className="text-2xl font-bold text-slate-900">{ui.title}</h1>
          <p className="text-sm text-slate-500 mt-1">{ui.desc}</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-7">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-2">
              <label htmlFor="emp-id" className="text-xs font-bold text-slate-700 uppercase tracking-wider">{ui.empId}</label>
              <input id="emp-id" className="w-full rounded-xl border-slate-200 bg-slate-50 text-slate-500 font-mono text-sm py-3 px-4" value="#TS-0001" readOnly />
            </div>
            <div className="space-y-2">
              <label htmlFor="lab-select" className="text-xs font-bold text-slate-700 uppercase tracking-wider">{ui.targetLab}</label>
              <select 
                id="lab-select"
                className="w-full rounded-xl border-slate-200 text-sm py-3 px-4 font-semibold text-slate-700 focus:ring-corporate-blue cursor-pointer"
                value={selectedLab}
                onChange={(e) => setSelectedLab(e.target.value as any)}
              >
                {Object.entries(labs).map(([key, label]) => (
                  <option key={key} value={key}>{language === 'en' ? label.en : label.tw}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="space-y-3">
            <p className="text-xs font-bold text-slate-700 uppercase tracking-wider">{ui.selectExp}</p>
            <div className="flex flex-wrap gap-3">
              {experimentsMap[selectedLab].map(expKey => (
                <button
                  key={expKey} type="button" onClick={() => toggleExp(expKey)}
                  className={`flex items-center gap-2 px-4 py-3 border rounded-xl transition-all duration-200 ${
                    selectedExps.includes(expKey) ? 'bg-corporate-blue/5 border-corporate-blue text-corporate-blue shadow-sm font-bold' : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50 font-medium'
                  }`}
                >
                  <span className="material-symbols-outlined text-sm">{selectedExps.includes(expKey) ? 'check_circle' : 'circle'}</span>
                  <span className="text-sm">{(ui as any)[expKey]}</span>
                </button>
              ))}
            </div>
          </div>

          <div className="space-y-3 p-5 bg-slate-50 rounded-2xl border border-slate-200">
            <label htmlFor="wafer-input" className="text-xs font-bold text-slate-700 uppercase tracking-wider">{ui.waferInput}</label>
            <div className="flex gap-3">
              <input 
                id="wafer-input"
                type="text" className="flex-1 rounded-xl border-slate-300 focus:ring-corporate-blue text-sm font-mono px-4"
                placeholder={ui.waferHint} value={waferInput} onChange={(e) => setWaferInput(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), handleAddWafer())}
              />
              <button type="button" onClick={handleAddWafer} className="px-6 py-3 bg-white border border-slate-300 rounded-xl font-bold text-xs hover:bg-slate-100 shadow-sm active:scale-95 cursor-pointer">{ui.addBtn}</button>
            </div>
            <div className="flex flex-wrap gap-2 mt-2 min-h-[32px]">
              {wafers.map(w => (
                <span key={w} className="bg-corporate-blue text-white px-3 py-1.5 rounded-lg text-xs font-bold flex items-center gap-2 animate-[zoomIn_0.2s_ease-out] shadow-sm">
                  {w}
                  <button type="button" onClick={() => setWafers(wafers.filter(x => x !== w))} className="hover:text-red-200 cursor-pointer"><span className="material-symbols-outlined text-[16px]">close</span></button>
                </span>
              ))}
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-2">
              <label htmlFor="priority-select" className="text-xs font-bold text-slate-700 uppercase tracking-wider">{ui.priority}</label>
              <select 
                id="priority-select"
                className={`w-full rounded-xl border font-bold text-sm py-3 px-4 transition-all duration-300 cursor-pointer ${getPriorityStyle()}`}
                value={priority} onChange={(e) => setPriority(e.target.value)}
              >
                <option value="NORMAL" className="bg-white text-emerald-700">{ui.priNormal}</option>
                <option value="URGENT" className="bg-white text-amber-700">{ui.priUrgent}</option>
                <option value="CRITICAL" className="bg-white text-red-700">{ui.priCritical}</option>
              </select>
            </div>
          </div>

          <div className="space-y-2">
            <label htmlFor="remarks-input" className="text-xs font-bold text-slate-700 uppercase tracking-wider">{ui.remarks}</label>
            <textarea 
              id="remarks-input"
              className="w-full rounded-xl border-slate-200 focus:ring-corporate-blue focus:border-corporate-blue text-sm py-3 px-4 min-h-[100px]"
              placeholder={ui.remarksPlaceholder}
              value={remarks}
              onChange={(e) => setRemarks(e.target.value)}
            />
          </div>

          <button type="submit" className="w-full bg-corporate-blue text-white py-4 rounded-2xl font-bold shadow-lg hover:bg-blue-700 transition-all flex justify-center items-center gap-3 active:scale-[0.98] cursor-pointer">
            <span className="material-symbols-outlined">send</span>{ui.submitBtn}
          </button>
        </form>
      </div>

      <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
        <div className="px-6 md:px-10 py-5 border-b border-slate-50 bg-slate-50/50">
          <h2 className="font-bold text-slate-800">{ui.tableTitle}</h2>
        </div>
        <div className="overflow-x-auto custom-scrollbar">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 text-slate-500 uppercase text-[10px] font-bold tracking-widest">
              <tr>
                <th className="px-6 md:px-10 py-4">{ui.colId}</th>
                <th className="px-6 md:px-10 py-4">{ui.colExp}</th>
                <th className="px-6 md:px-10 py-4">{ui.colCount}</th>
                <th className="px-6 md:px-10 py-4 text-right">{ui.colStatus}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50">
              {requests.length > 0 ? (
                requests.map(req => (
                  <tr key={req.id} className="hover:bg-slate-50 transition-colors animate-[fadeIn_0.3s_ease-out]">
                    <td className="px-6 md:px-10 py-5 font-mono font-bold text-slate-700">{req.id}</td>
                    <td className="px-6 md:px-10 py-5 text-xs text-slate-500">{req.experiments.join(', ')}</td>
                    <td className="px-6 md:px-10 py-5 text-slate-600 font-bold">{req.waferCount}</td>
                    <td className="px-6 md:px-10 py-5 text-right">
                      <span className="px-3 py-1.5 bg-amber-50 text-amber-600 rounded-lg text-[10px] font-bold border border-amber-100">{req.status}</span>
                    </td>
                  </tr>
                ))
              ) : (
                <tr><td colSpan={4} className="px-6 md:px-10 py-16 text-center text-slate-300"><p className="font-bold text-sm tracking-widest">{ui.empty}</p></td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};