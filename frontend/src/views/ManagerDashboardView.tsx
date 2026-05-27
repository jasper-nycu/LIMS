// src/views/ManagerDashboardView.tsx
import React, { useEffect, useState } from 'react';
import { apiGet, apiPost } from '../api';
import { type UserProfile } from '../components/layout/Header';

// --- Interfaces ---
export interface ManagerRequest {
  id: string;
  requester: string;
  role: string;
  waferIds: string[];
  experiments: string[];
  remarks: string;
  priority: 'NORMAL' | 'URGENT' | 'CRITICAL';
  submitTime: string;
}

const priorityRank: Record<ManagerRequest['priority'], number> = {
  CRITICAL: 0,
  URGENT: 1,
  NORMAL: 2,
};

const sortManagerQueue = (requests: ManagerRequest[]) =>
  requests
    .map((request, index) => ({ request, index }))
    .sort((a, b) =>
      priorityRank[a.request.priority] - priorityRank[b.request.priority]
      || a.request.submitTime.localeCompare(b.request.submitTime)
      || a.index - b.index
    )
    .map(({ request }) => request);

interface ManagerDashboardProps {
  language: 'en' | 'tw';
  onNotify: (titleKey: string | null, fallbackTitle: string, desc: string, type: 'info' | 'success' | 'error' | 'warning') => void;
  initialRequests?: ManagerRequest[]; // Support dynamic data injection
  user?: UserProfile | null;
  onRefreshNotifications?: () => void;
}

export const ManagerDashboardView: React.FC<ManagerDashboardProps> = ({ language, onNotify, initialRequests = [], user, onRefreshNotifications }) => {
  const i18n = {
    en: {
      title: 'Approval Queue',
      subtitle: 'Capacity Check & Resource Allocation Required',
      pending: 'PENDING',
      colRequester: 'Requester',
      colWafers: 'Wafer IDs',
      colExp: 'Experiments',
      colMsg: 'Message',
      colAction: 'Action',
      btnApprove: 'Approve',
      btnReject: 'Reject',
      empty: 'Queue is empty. All caught up!',
      modalMsgTitle: 'Requester Remarks',
      modalRejTitle: 'Reject Request',
      modalRejDesc: 'Please provide a reason for rejecting this request.',
      placeholderRej: 'Enter reason here...',
      btnCancel: 'Cancel',
      btnConfirmRej: 'Confirm Reject',
      btnVerify: 'Close',
      notifApprove: 'Request Approved',
      notifReject: 'Request Rejected'
    },
    tw: {
      title: '簽核佇列',
      subtitle: '需進行產能檢核與資源分配',
      pending: '待簽核',
      colRequester: '申請人',
      colWafers: '晶圓列表',
      colExp: '實驗項目',
      colMsg: '備註',
      colAction: '操作',
      btnApprove: '核准',
      btnReject: '退回',
      empty: '目前無待處理項目！',
      modalMsgTitle: '申請人備註',
      modalRejTitle: '退回委託單',
      modalRejDesc: '請填寫退回原因，系統將通知申請人。',
      placeholderRej: '請輸入原因...',
      btnCancel: '取消',
      btnConfirmRej: '確認退件',
      btnVerify: '關閉',
      notifApprove: '委託單已核准',
      notifReject: '委託單已退回'
    }
  };

  const ui = i18n[language];

  // --- States ---
  const [pendingRequests, setRequests] = useState<ManagerRequest[]>(sortManagerQueue(initialRequests));

  // --- UI States ---
  const [msgModal, setMsgModal] = useState<{ isOpen: boolean; content: string }>({ isOpen: false, content: '' });
  const [rejModal, setRejModal] = useState<{ isOpen: boolean; targetId: string | null }>({ isOpen: false, targetId: null });
  const [rejectReason, setRejReason] = useState('');
  const [removingId, setRemovingId] = useState<string | null>(null);

  useEffect(() => {
    if (!user?.employeeId && initialRequests.length === 0) return;
    if (initialRequests.length > 0) return;
    apiGet<ManagerRequest[]>('/api/v1/manager/requests/pending')
      .then(requests => setRequests(sortManagerQueue(requests)))
      .catch(() => undefined);
  }, [user?.employeeId, initialRequests.length]);

  // --- Handlers ---
  const handleApprove = (id: string) => {
    setRemovingId(id);
    // wait for 400ms to allow the fade-out animation before removing from state
    setTimeout(() => {
      const finish = () => {
        setRequests(prev => prev.filter(r => r.id !== id));
        setRemovingId(null);
        if (!user?.employeeId) {
          onNotify(null, ui.notifApprove, `${id} has been moved to Lab WIP queue.`, 'success');
        }
        onRefreshNotifications?.();
      };

      if (!user?.employeeId) {
        finish();
        return;
      }

      apiPost(`/api/v1/manager/requests/${id}/approve`, { approverId: user.employeeId })
        .then(finish)
        .catch(() => {
          setRemovingId(null);
          onNotify(null, 'Approval Failed', `${id} could not be approved.`, 'error');
        });
    }, 400);
  };

  const handleReject = () => {
    if (!rejectReason.trim()) return;
    const targetId = rejModal.targetId;
    setRemovingId(targetId);
    setRejModal({ isOpen: false, targetId: null });

    setTimeout(() => {
      const finish = () => {
        setRequests(prev => prev.filter(r => r.id !== targetId));
        setRemovingId(null);
        if (!user?.employeeId) {
          onNotify(null, ui.notifReject, `${targetId} rejected: ${rejectReason}`, 'error');
        }
        setRejReason('');
        onRefreshNotifications?.();
      };

      if (!user?.employeeId) {
        finish();
        return;
      }

      apiPost(`/api/v1/manager/requests/${targetId}/reject`, { approverId: user.employeeId, rejectReason })
        .then(finish)
        .catch(() => {
          setRemovingId(null);
          onNotify(null, 'Rejection Failed', `${targetId} could not be rejected.`, 'error');
        });
    }, 400);
  };

  const getPriorityStyle = (p: string) => {
    switch (p) {
      case 'URGENT': return 'bg-amber-50 text-amber-600 border-amber-100';
      case 'CRITICAL': return 'bg-red-50 text-red-600 border-red-100';
      default: return 'bg-slate-100 text-slate-600 border-slate-200';
    }
  };

  return (
    <div className="max-w-7xl mx-auto space-y-6 animate-[fadeIn_0.3s_ease-out]">
      {/* Approval Table Card */}
      <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden flex flex-col">
        <div className="px-6 md:px-10 py-6 border-b border-slate-50 flex justify-between items-center bg-slate-50/30">
          <div>
            <h2 className="font-bold text-lg text-slate-900">{ui.title}</h2>
            <p className="text-xs text-slate-500 mt-0.5">{ui.subtitle}</p>
          </div>
          <span className="px-4 py-1.5 bg-corporate-blue/10 text-corporate-blue text-xs font-bold rounded-full uppercase flex items-center gap-2">
            <span className="relative flex h-2 w-2">
              {pendingRequests.length > 0 && (
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-corporate-blue opacity-75"></span>
              )}
              <span className="relative inline-flex rounded-full h-2 w-2 bg-corporate-blue"></span>
            </span>
            {pendingRequests.length} {ui.pending}
          </span>
        </div>

        <div className="overflow-x-auto custom-scrollbar">
          <table className="w-full text-left text-sm min-w-[900px]">
            <thead className="bg-slate-50/50 text-slate-500 uppercase text-[10px] font-bold tracking-widest">
              <tr>
                <th className="px-8 py-4">{ui.colRequester}</th>
                <th className="px-6 py-4">{ui.colWafers}</th>
                <th className="px-6 py-4">{ui.colExp}</th>
                <th className="px-6 py-4 text-center">{ui.colMsg}</th>
                <th className="px-8 py-4 text-right">{ui.colAction}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50">
              {pendingRequests.length > 0 ? (
                pendingRequests.map(req => (
                  <tr 
                    key={req.id} 
                    className={`hover:bg-slate-50/80 transition-all duration-400 group ${
                      removingId === req.id 
                        ? 'opacity-0 translate-x-8 blur-sm pointer-events-none' 
                        : 'opacity-100 translate-x-0'
                    }`}
                  >
                    <td className="px-8 py-5">
                      <div className="flex items-center gap-4">
                        <div className="w-10 h-10 rounded-full bg-corporate-blue text-white flex items-center justify-center font-bold shadow-sm">
                          {req.requester.substring(0, 2).toUpperCase()}
                        </div>
                        <div>
                          <div className="font-bold text-slate-900 flex items-center gap-2">
                            {req.requester}
                            <span className={`px-2 py-0.5 rounded text-[9px] border ${getPriorityStyle(req.priority)}`}>
                              {req.priority}
                            </span>
                          </div>
                          <p className="text-[10px] text-slate-400 font-mono mt-0.5">{req.id} • {req.submitTime}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-5">
                      <div className="flex flex-wrap gap-1.5 max-w-[200px]">
                        {req.waferIds.map(w => (
                          <span key={w} className="px-2 py-1 bg-blue-50 text-corporate-blue text-[10px] font-bold rounded-md border border-blue-100 font-mono">
                            {w}
                          </span>
                        ))}
                      </div>
                    </td>
                    <td className="px-6 py-5">
                      <div className="flex flex-wrap gap-1.5 max-w-[250px]">
                        {req.experiments.map(ex => (
                          <span key={ex} className="px-2 py-1 bg-slate-100 text-slate-600 text-[10px] font-bold rounded-md border border-slate-200">
                            {ex}
                          </span>
                        ))}
                      </div>
                    </td>
                    <td className="px-6 py-5 text-center">
                      <button 
                        onClick={() => setMsgModal({ isOpen: true, content: req.remarks })}
                        className="p-2 text-corporate-blue hover:bg-blue-50 rounded-xl transition-all cursor-pointer inline-flex items-center justify-center"
                      >
                        <span className="material-symbols-outlined text-[20px]">chat</span>
                      </button>
                    </td>
                    <td className="px-8 py-5 text-right space-x-2">
                      <button 
                        onClick={() => handleApprove(req.id)}
                        className="px-4 py-2 bg-emerald-500 text-white text-xs font-bold rounded-xl hover:bg-emerald-600 shadow-sm active:scale-95 transition-all cursor-pointer"
                      >
                        {ui.btnApprove}
                      </button>
                      <button 
                        onClick={() => setRejModal({ isOpen: true, targetId: req.id })}
                        className="px-4 py-2 bg-slate-100 text-slate-600 text-xs font-bold rounded-xl hover:bg-slate-200 active:scale-95 transition-all cursor-pointer"
                      >
                        {ui.btnReject}
                      </button>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={5} className="py-20 text-center text-slate-300">
                    <span className="material-symbols-outlined text-5xl mb-3 opacity-30">task_alt</span>
                    <p className="font-bold text-sm tracking-widest">{ui.empty}</p>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* --- Modals --- */}
      
      {/* Message Modal */}
      {msgModal.isOpen && (
        <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-[fadeIn_0.2s_ease-out]">
          <div className="bg-white rounded-3xl shadow-2xl w-full max-w-md overflow-hidden animate-[zoomIn_0.2s_ease-out]">
            <div className="p-6 border-b border-slate-100 flex justify-between items-center">
              <h3 className="font-bold text-lg text-slate-900 flex items-center gap-2">
                <span className="material-symbols-outlined text-corporate-blue">chat</span> {ui.modalMsgTitle}
              </h3>
              <button onClick={() => setMsgModal({ ...msgModal, isOpen: false })} className="text-slate-400 hover:text-slate-600 cursor-pointer">
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>
            <div className="p-8">
              <p className="text-slate-600 text-sm leading-relaxed whitespace-pre-wrap italic bg-slate-50 p-4 rounded-2xl border border-slate-100">
                "{msgModal.content}"
              </p>
            </div>
            <div className="p-6 pt-0 text-right">
              <button 
                onClick={() => setMsgModal({ ...msgModal, isOpen: false })}
                className="px-6 py-2.5 bg-slate-900 text-white text-sm font-bold rounded-xl hover:bg-slate-800 transition-all cursor-pointer"
              >
                {ui.btnVerify}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Reject Modal */}
      {rejModal.isOpen && (
        <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-[fadeIn_0.2s_ease-out]">
          <div className="bg-white rounded-3xl shadow-2xl w-full max-w-md animate-[zoomIn_0.2s_ease-out]">
            <div className="p-8 text-center space-y-4">
              <div className="mx-auto flex items-center justify-center h-16 w-16 rounded-full bg-red-50 border-8 border-white shadow-sm">
                <span className="material-symbols-outlined text-3xl text-red-500">warning</span>
              </div>
              <h3 className="text-xl font-bold text-slate-900">{ui.modalRejTitle}</h3>
              <p className="text-sm text-slate-500 font-medium">{ui.modalRejDesc}</p>
              <textarea 
                value={rejectReason}
                onChange={(e) => setRejReason(e.target.value)}
                className="w-full mt-4 rounded-2xl border-slate-200 focus:ring-red-500 focus:border-red-500 text-sm p-4 min-h-[100px]"
                placeholder={ui.placeholderRej}
              />
              <div className="grid grid-cols-2 gap-3 pt-4">
                <button 
                  onClick={() => setRejModal({ isOpen: false, targetId: null })}
                  className="px-4 py-3 bg-slate-100 text-slate-600 text-sm font-bold rounded-xl hover:bg-slate-200 transition-all cursor-pointer"
                >
                  {ui.btnCancel}
                </button>
                <button 
                  onClick={handleReject}
                  className="px-4 py-3 bg-red-500 text-white text-sm font-bold rounded-xl hover:bg-red-600 shadow-md transition-all cursor-pointer"
                >
                  {ui.btnConfirmRej}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
