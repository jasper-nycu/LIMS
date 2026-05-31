// src/views/CapacityAnalyticsView.tsx
import React, { useState, useEffect, useRef } from 'react';
import { Chart, registerables } from 'chart.js';

// Register Chart.js modules safely for React lifecycle handles
Chart.register(...registerables);

interface Owner {
  initials: string;
  color: string;
}

interface MachineHistoryPoint {
  timestamp: number;
  util: number;
  state?: MachineState['state'];
}

type OperationStatus = 'PROCESSING' | 'IDLE' | 'MAINTENANCE' | 'ALARM';

interface MachineState {
  id: string;
  state: 'PROCESSING' | 'IDLE' | 'ALARM' | 'MAINTENANCE';
  loadedCount?: number;
  cap: number;
  expKey: string;
  name: string;
  error: string | null;
  currentUtil: number;
  owners: Owner[];
  utilHistory?: MachineHistoryPoint[];
}

interface CapacityAnalyticsViewProps {
  language: 'en' | 'tw';
  machines?: Record<string, MachineState>;
  selectedMachineId?: string;
  onSelectedMachineIdChange?: (machineId: string) => void;
  timeRange?: string;
  onTimeRangeChange?: (timeRange: string) => void;
}

export const CapacityAnalyticsView: React.FC<CapacityAnalyticsViewProps> = ({
  language,
  machines = {},
  selectedMachineId: controlledSelectedMachineId,
  onSelectedMachineIdChange,
  timeRange: controlledTimeRange,
  onTimeRangeChange
}) => {
  // i18n Translation Dictionary matching index.html structural targets
  const i18n = {
    en: {
      title: 'Machine Utilization Time-Series',
      calcRule: '* Utilization is tracked dynamically.',
      avgUtil: 'Avg Utilization',
      analysisTitle: 'Analysis Equipment',
      processTitle: 'Process & Test Equipment',
      yAxisLabel: 'Utilization (%)',
      nowLabel: 'Now',
      time_5m: 'Last 5 Minutes',
      time_1h: 'Last 1 Hour',
      time_3h: 'Last 3 Hours',
      time_12h: 'Last 12 Hours',
      time_1d: 'Last 1 Day',
      time_3d: 'Last 3 Days',
      time_1w: 'Last 1 Week',
      operationTitle: 'Machine Operation Status',
      operationDesc: 'Chart shows machineoperation status across the selected interval.',
      activeRuntime: 'Active Runtime',
      statusProcessing: 'PROCESSING',
      statusIdle: 'IDLE',
      statusMaintenance: 'MAINTENANCE',
      statusAlarm: 'ALARM',
      pieAllMachines: 'All machines (machine-hours)',
      machineLabel: 'Machine'
    },
    tw: {
      title: '機台利用率時序圖',
      calcRule: '* 利用率 (%) 計算邏輯為：執行派發至下貨之間的時間差。',
      avgUtil: '平均稼動率',
      analysisTitle: '分析檢測機台',
      processTitle: '製程與測試機台',
      yAxisLabel: '利用率 (%)',
      nowLabel: '現在',
      time_5m: '近 5 分鐘',
      time_1h: '近 1 小時',
      time_3h: '近 3 小時',
      time_12h: '近 12 小時',
      time_1d: '近 1 天',
      time_3d: '近 3 天',
      time_1w: '近 1 週',
      operationTitle: '機台運作狀態',
      operationDesc: '圖表顯示所選時間段內的機台運作狀態。',
      activeRuntime: '運轉時間',
      statusProcessing: '執行中',
      statusIdle: '閒置中',
      statusMaintenance: '維護中',
      statusAlarm: '異常',
      pieAllMachines: '所有機台（機台工時）',
      machineLabel: '機台'
    }
  };

  const ui = i18n[language];

  // Controlled UI dropdown tracker for timeline bounds
  const [internalTimeRange, setInternalTimeRange] = useState<string>('1h');
  const timeRange = controlledTimeRange ?? internalTimeRange;
  const setTimeRange = onTimeRangeChange ?? setInternalTimeRange;
  const [avgUtilization, setAvgUtilization] = useState<string>('0.0%');
  const [internalSelectedMachineId, setInternalSelectedMachineId] = useState<string>('');
  const selectedMachineId = controlledSelectedMachineId ?? internalSelectedMachineId;
  const setSelectedMachineId = onSelectedMachineIdChange ?? setInternalSelectedMachineId;
  const machineIds = Object.keys(machines);
  const machineIdKey = machineIds.join('|');

  // DOM Canvas Hooks to guarantee isolated memory allocations
  const chartRef1 = useRef<HTMLCanvasElement | null>(null);
  const chartRef2 = useRef<HTMLCanvasElement | null>(null);
  const statusPieRef = useRef<HTMLCanvasElement | null>(null);
  const instanceRef1 = useRef<Chart | null>(null);
  const instanceRef2 = useRef<Chart | null>(null);
  const statusPieInstance = useRef<Chart | null>(null);

  // Helper date parsing mirroring standard index.html telemetry functions
  const formatTime = (d: Date) => `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`;
  const formatDate = (d: Date) => `${(d.getMonth() + 1).toString().padStart(2, '0')}/${d.getDate().toString().padStart(2, '0')}`;

  // Default to the first machine for a new login session, then keep the user's choice while App stays logged in.
  React.useEffect(() => {
    const first = machineIds[0];
    if (!first) {
      if (selectedMachineId) setSelectedMachineId('');
      return;
    }
    if (!selectedMachineId || !machines[selectedMachineId]) {
      setSelectedMachineId(first);
    }
  }, [machineIdKey, selectedMachineId, setSelectedMachineId]);

  const rangeMsMap: Record<string, number> = {
    '5m': 5 * 60 * 1000,
    '1h': 60 * 60 * 1000,
    '3h': 3 * 60 * 60 * 1000,
    '12h': 12 * 60 * 60 * 1000,
    '1d': 24 * 60 * 60 * 1000,
    '3d': 3 * 24 * 60 * 60 * 1000,
    '1w': 7 * 24 * 60 * 60 * 1000,
  };

  const formatAxisLabel = (timestamp: number) => {
    const d = new Date(timestamp);
    if (timeRange === '3d' || timeRange === '1w') {
      return `${formatDate(d)} ${formatTime(d)}`;
    }
    return formatTime(d);
  };

  const formatRuntime = (ms: number) => {
    const totalMinutes = Math.round(ms / 60000);
    const hours = Math.floor(totalMinutes / 60);
    const minutes = totalMinutes % 60;
    return hours > 0 ? `${hours}h ${minutes}m` : `${minutes}m`;
  };

  const getOperationStatus = (machine: MachineState, rangeMs: number) => {
    const now = Date.now();
    const startTs = now - rangeMs;
    // Deep-clone history entries to avoid accidental shared object mutation
    const rawHistory = Array.isArray(machine.utilHistory)
      ? machine.utilHistory.map(p => ({ timestamp: p.timestamp, util: p.util, state: p.state }))
      : [];
    rawHistory.sort((a, b) => a.timestamp - b.timestamp);

    const beforeStart = rawHistory.filter(point => point.timestamp < startTs).pop();
    const visible = rawHistory.filter(point => point.timestamp >= startTs && point.timestamp <= now);
    const startValue = beforeStart ? beforeStart.util : visible.length > 0 ? visible[0].util : machine.currentUtil;
    const startState = beforeStart?.state ?? visible[0]?.state;

    const points: Array<MachineHistoryPoint> = [{ timestamp: startTs, util: startValue, state: startState }, ...visible];
    if (points[points.length - 1].timestamp < now) {
      points.push({ timestamp: now, util: machine.currentUtil, state: machine.state });
    }

    const segments: Array<{ status: OperationStatus; start: number; end: number; duration: number }> = [];
    for (let i = 0; i < points.length - 1; i += 1) {
      const current = points[i];
      const next = points[i + 1];
      const duration = next.timestamp - current.timestamp;
      if (duration <= 0) continue;
      const isLastSegment = i === points.length - 2;
      const computedStatus: OperationStatus = current.state
        ? current.state
        : current.util > 0
          ? 'PROCESSING'
          : isLastSegment
            ? machine.state
            : 'IDLE';
      segments.push({ status: computedStatus, start: current.timestamp, end: next.timestamp, duration });
    }

    if (segments.length === 0) {
      segments.push({ status: machine.state === 'MAINTENANCE' ? 'MAINTENANCE' : machine.currentUtil > 0 ? 'PROCESSING' : 'IDLE', start: startTs, end: now, duration: rangeMs });
    }

    const activeRuntime = segments.filter(seg => seg.status === 'PROCESSING').reduce((sum, seg) => sum + seg.duration, 0);
    const activePercent = rangeMs > 0 ? Math.round((activeRuntime / rangeMs) * 1000) / 10 : 0;

    return { segments, activeRuntime, activePercent };
  };

  const getStatusColor = (status: OperationStatus) => {
    switch (status) {
      case 'PROCESSING': return '#10b981';
      case 'MAINTENANCE': return '#3b82f6';
      case 'ALARM': return '#ef4444';
      default: return '#94a3b8';
    }
  };

  const getStatusLabel = (status: OperationStatus) => {
    switch (status) {
      case 'PROCESSING': return ui.statusProcessing;
      case 'MAINTENANCE': return ui.statusMaintenance;
      case 'ALARM': return ui.statusAlarm;
      default: return ui.statusIdle;
    }
  };

  useEffect(() => {
    if (!chartRef1.current || !chartRef2.current) return;

    const now = new Date();
    const labels: string[] = [];
    const points = 7;
    // Build labels (oldest -> now)
    for (let i = points - 1; i >= 0; i--) {
      const t = new Date(now.getTime());
      if (timeRange === '5m') t.setMinutes(t.getMinutes() - i);
      else if (timeRange === '1h') t.setMinutes(t.getMinutes() - i * 10);
      else if (timeRange === '3h') t.setMinutes(t.getMinutes() - i * 30);
      else if (timeRange === '12h') t.setHours(t.getHours() - i * 2);
      else if (timeRange === '1d') t.setHours(t.getHours() - i * 4);
      else if (timeRange === '3d') t.setHours(t.getHours() - i * 12);
      else if (timeRange === '1w') t.setDate(t.getDate() - i);

      if (i === 0) labels.push(ui.nowLabel);
      else labels.push(timeRange === '3d' || timeRange === '1w' ? `${formatDate(t)} ${formatTime(t)}` : formatTime(t));
    }

    // Helper: produce an oldest->now ordered history array for a machine
    const getHistoryFor = (id: string) => {
      const m = machines[id];
      const pts = points;
      const rangeMs = {
        '5m': 5 * 60 * 1000,
        '1h': 60 * 60 * 1000,
        '3h': 3 * 60 * 60 * 1000,
        '12h': 12 * 60 * 60 * 1000,
        '1d': 24 * 60 * 60 * 1000,
        '3d': 3 * 24 * 60 * 60 * 1000,
        '1w': 7 * 24 * 60 * 60 * 1000,
      }[timeRange] ?? 60 * 60 * 1000;
      const nowTs = Date.now();

      const currentValue = m ? (m.state === 'ALARM' || m.state === 'MAINTENANCE' ? 0 : m.currentUtil) : 0;
      const history = Array.isArray(m?.utilHistory) ? m!.utilHistory! : [];
      const sorted = [...history].sort((a, b) => a.timestamp - b.timestamp);

      const values: number[] = [];
      for (let i = 0; i < pts; i++) {
        const targetTs = nowTs - Math.round(rangeMs * ((pts - 1 - i) / Math.max(1, pts - 1)));
        const candidate = sorted.filter(point => point.timestamp <= targetTs).pop();
        if (candidate) {
          values.push(candidate.util);
        } else if (sorted.length > 0) {
          values.push(sorted[0].util);
        } else {
          values.push(currentValue);
        }
      }

      if (values[values.length - 1] !== currentValue) {
        values[values.length - 1] = currentValue;
      }

      return values;
    };

    const dataLineSEM: number[] = getHistoryFor('SEM-01');
    const dataLineTEM: number[] = getHistoryFor('TEM-01');
    const dataLineFIB: number[] = getHistoryFor('FIB-01');
    const dataLineBAKE: number[] = getHistoryFor('BAKE-OVEN-01');
    const dataLineETEST: number[] = getHistoryFor('E-TEST-02');
    const dataLineXRD: number[] = getHistoryFor('XRD-01');

    // compute average utilization across all machines and points
    const totalSum = [dataLineSEM, dataLineTEM, dataLineFIB, dataLineBAKE, dataLineETEST, dataLineXRD].reduce((acc, arr) => acc + arr.reduce((s, v) => s + v, 0), 0);
    const avg = totalSum / (points * 6);
    setAvgUtilization(avg.toFixed(1) + '%');

    // Dynamic color helpers managing failure isolation mapping
    const getBorderColor = (machId: string, defaultColor: string) => {
      const m = machines[machId];
      return m && m.state === 'ALARM' ? '#ef4444' : defaultColor;
    };
    const getBgColor = (machId: string, defaultBg: string) => {
      const m = machines[machId];
      return m && m.state === 'ALARM' ? 'rgba(239, 68, 68, 0.1)' : defaultBg;
    };

    // Chart Options Shared Model Configuration
    const commonOptions = {
      responsive: true,
      maintainAspectRatio: false,
      interaction: { mode: 'index' as const, intersect: false },
      plugins: {
        legend: { position: 'top' as const, labels: { font: { family: "'Public Sans', sans-serif" } } }
      },
      scales: {
        y: { beginAtZero: true, max: 100, title: { display: true, text: ui.yAxisLabel, font: { size: 10 } } },
        x: { title: { display: false } }
      }
    };

    // Clean up old canvas instances before generating replacements
    if (instanceRef1.current) instanceRef1.current.destroy();
    if (instanceRef2.current) instanceRef2.current.destroy();
    if (statusPieInstance.current) statusPieInstance.current.destroy();

    // Chart 1: Analysis Equipment Node Generation
    instanceRef1.current = new Chart(chartRef1.current, {
      type: 'line',
      data: {
        labels,
        datasets: [
          { label: 'SEM-01', data: dataLineSEM, borderColor: getBorderColor('SEM-01', '#10b981'), backgroundColor: getBgColor('SEM-01', 'rgba(16, 185, 129, 0.1)'), borderWidth: 2, pointBackgroundColor: getBorderColor('SEM-01', '#10b981'), tension: 0.4, fill: true },
          { label: 'TEM-01', data: dataLineTEM, borderColor: getBorderColor('TEM-01', '#6366f1'), backgroundColor: getBgColor('TEM-01', 'rgba(99, 102, 241, 0.1)'), borderWidth: 2, pointBackgroundColor: getBorderColor('TEM-01', '#6366f1'), tension: 0.4, fill: true },
          { label: 'FIB-01', data: dataLineFIB, borderColor: getBorderColor('FIB-01', '#ec4899'), backgroundColor: getBgColor('FIB-01', 'rgba(236, 72, 153, 0.1)'), borderWidth: 2, pointBackgroundColor: getBorderColor('FIB-01', '#ec4899'), tension: 0.4, fill: true }
        ]
      },
      options: { ...commonOptions, plugins: { ...commonOptions.plugins, title: { display: true, text: ui.analysisTitle } } }
    });

    // Chart 2: Process & Test Equipment Node Generation
    instanceRef2.current = new Chart(chartRef2.current, {
      type: 'line',
      data: {
        labels,
        datasets: [
          { label: 'BAKE-OVEN-01', data: dataLineBAKE, borderColor: getBorderColor('BAKE-OVEN-01', '#0ea5e9'), backgroundColor: getBgColor('BAKE-OVEN-01', 'rgba(14, 165, 233, 0.1)'), borderWidth: 2, pointBackgroundColor: getBorderColor('BAKE-OVEN-01', '#0ea5e9'), tension: 0.4, fill: true },
          { label: 'E-TEST-02', data: dataLineETEST, borderColor: getBorderColor('E-TEST-02', '#f59e0b'), backgroundColor: getBgColor('E-TEST-02', 'rgba(245, 158, 11, 0.1)'), borderWidth: 2, pointBackgroundColor: getBorderColor('E-TEST-02', '#f59e0b'), tension: 0.4, fill: true },
          { label: 'XRD-01', data: dataLineXRD, borderColor: getBorderColor('XRD-01', '#8b5cf6'), backgroundColor: getBgColor('XRD-01', 'rgba(139, 92, 246, 0.1)'), borderWidth: 2, pointBackgroundColor: getBorderColor('XRD-01', '#8b5cf6'), tension: 0.4, fill: true }
        ]
      },
      options: { ...commonOptions, plugins: { ...commonOptions.plugins, title: { display: true, text: ui.processTitle } } }
    });

    const statusTotals = Object.values(machines).reduce(
      (acc, machine) => {
        const { segments } = getOperationStatus(machine, rangeMsMap[timeRange] ?? rangeMsMap['1h']);
        segments.forEach((segment) => {
          acc[segment.status] += segment.duration;
        });
        return acc;
      },
      { PROCESSING: 0, IDLE: 0, MAINTENANCE: 0, ALARM: 0 }
    );

    const statusData = [
      statusTotals.PROCESSING,
      statusTotals.IDLE,
      statusTotals.MAINTENANCE,
      statusTotals.ALARM
    ];
    const totalStatus = statusData.reduce((sum, value) => sum + value, 0) || 1;

    if (statusPieRef.current) {
      statusPieInstance.current = new Chart(statusPieRef.current, {
        type: 'pie',
        data: {
          labels: [ui.statusProcessing, ui.statusIdle, ui.statusMaintenance, ui.statusAlarm],
          datasets: [{
            data: statusData,
            backgroundColor: ['#10b981', '#94a3b8', '#3b82f6', '#ef4444'],
            borderColor: ['#ffffff', '#ffffff', '#ffffff'],
            borderWidth: 2
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          layout: {
            padding: {
              bottom: 20 
            }
          },
          plugins: {
            legend: { position: 'bottom' as const, labels: { font: { family: "'Public Sans', sans-serif" }, boxWidth: 12} },
            tooltip: {
              callbacks: {
                label: (context: any) => {
                  const label = context.label || '';
                  const value = context.parsed || 0;
                  const pct = totalStatus > 0 ? Math.round((value / totalStatus) * 1000) / 10 : 0;
                  return `${label}: ${formatRuntime(value)} (${pct}%)`;
                }
              }
            }
          }
        }
      });
    }

    // Handle standard memory garbage collection on component unmounting steps
    return () => {
      if (instanceRef1.current) instanceRef1.current.destroy();
      if (instanceRef2.current) instanceRef2.current.destroy();
      if (statusPieInstance.current) statusPieInstance.current.destroy();
    };
  }, [timeRange, language, machines]);

  return (
    <div className="max-w-7xl mx-auto bg-white p-4 md:p-6 rounded-2xl shadow-sm border border-slate-100 flex flex-col overflow-hidden animate-[fadeIn_0.3s_ease-out]">
      {/* Top Controls Header Panel */}
      <div className="flex justify-between items-start mb-6">
        <div className="space-y-1.5">
          <div className="flex flex-col sm:flex-row sm:items-center gap-3">
            <h2 className="font-bold text-base md:text-lg text-slate-900">{ui.title}</h2>
            <select 
              id="chartTimeRange" 
              value={timeRange}
              onChange={(e) => setTimeRange(e.target.value)}
              className="text-xs font-bold rounded-lg border-slate-200 text-slate-600 focus:ring-corporate-blue focus:border-corporate-blue bg-slate-50 cursor-pointer shadow-sm py-1 pl-2 pr-8"
            >
              <option value="5m">{ui.time_5m}</option>
              <option value="1h">{ui.time_1h}</option>
              <option value="3h">{ui.time_3h}</option>
              <option value="12h">{ui.time_12h}</option>
              <option value="1d">{ui.time_1d}</option>
              <option value="3d">{ui.time_3d}</option>
              <option value="1w">{ui.time_1w}</option>
            </select>
          </div>
        </div>
        
        {/* Aggregated Performance Metric Block */}
        <div className="text-right shrink-0 ml-4">
          <p className="text-[10px] md:text-[11px] font-bold text-slate-400 uppercase tracking-wide">{ui.avgUtil}</p>
          <p className="text-2xl md:text-3xl font-bold text-emerald-500 tracking-tight leading-none mt-1 md:mt-1.5">
            {avgUtilization}
          </p>
        </div>
      </div>

      {/* Double Column Chart Graph Layout Matrix */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mt-2">
        <div className="h-64 md:h-80 relative bg-slate-50/50 rounded-xl p-2 border border-slate-100">
          <canvas ref={chartRef1}></canvas>
        </div>
        <div className="h-64 md:h-80 relative bg-slate-50/50 rounded-xl p-2 border border-slate-100">
          <canvas ref={chartRef2}></canvas>
        </div>
      </div>

      <div className="mt-8 bg-slate-50/80 rounded-2xl border border-slate-100 p-4">
        <div className="flex flex-col lg:flex-row lg:items-end lg:justify-between gap-4 mb-4">
          <div>
            <h3 className="text-base font-bold text-slate-900">{ui.operationTitle}</h3>
            <p className="text-xs text-slate-500 mt-1">{ui.operationDesc}</p>
          </div>
          <div className="flex flex-col sm:flex-row sm:items-center gap-3 text-xs text-slate-500">
            <div className="inline-flex items-center gap-2"><span className="w-3 h-3 rounded-sm bg-emerald-500"></span>{ui.statusProcessing}</div>
            <div className="inline-flex items-center gap-2"><span className="w-3 h-3 rounded-sm bg-slate-400"></span>{ui.statusIdle}</div>
            <div className="inline-flex items-center gap-2"><span className="w-3 h-3 rounded-sm bg-blue-500"></span>{ui.statusMaintenance}</div>
            <div className="inline-flex items-center gap-2"><span className="w-3 h-3 rounded-sm bg-red-500"></span>{ui.statusAlarm}</div>
            <div className="inline-flex items-center gap-2">
              <label htmlFor="statusMachineSelect" className="font-medium text-slate-700">{ui.machineLabel}</label>
              <select
                id="statusMachineSelect"
                value={selectedMachineId}
                onChange={(e) => setSelectedMachineId(e.target.value)}
                className="text-xs font-bold rounded-lg border-slate-200 text-slate-600 bg-white py-1 px-2 shadow-sm"
              >
                {Object.values(machines).map((machine) => (
                  <option key={machine.id} value={machine.id}>{machine.id}</option>
                ))}
              </select>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-[300px_1fr] gap-6">
          <div className="h-64 rounded-2xl bg-white p-3 border border-slate-200 shadow-sm">
            <div className="text-sm font-semibold text-slate-800 mb-3"> {ui.pieAllMachines}</div>
            <canvas ref={statusPieRef}></canvas>
          </div>

          <div>
            {machines[selectedMachineId] ? (() => {
              const machine = machines[selectedMachineId];
              const rangeMs = rangeMsMap[timeRange] ?? rangeMsMap['1h'];
              const { segments, activeRuntime, activePercent } = getOperationStatus(machine, rangeMs);
              const startTs = Date.now() - rangeMs;
              const axisSteps = [0, 0.25, 0.5, 0.75, 1].map((fraction) => formatAxisLabel(startTs + Math.round(rangeMs * fraction)));

              return (
                <div className="space-y-4 bg-white/80 rounded-2xl border border-slate-200 p-3 shadow-sm">
                  <div className="grid grid-cols-[1fr_auto] items-center gap-4">
                    <div className="text-sm font-semibold text-slate-800">{machine.id}</div>
                    <div className="text-right text-xs text-slate-600">
                      <div className="font-medium text-slate-700">{ui.activeRuntime}:</div>
                      <div className="mt-1 text-sm font-bold text-emerald-600">{formatRuntime(activeRuntime)} / {formatRuntime(rangeMs)} ({activePercent}%)</div>
                    </div>
                  </div>
                  <div className="h-10 rounded-full bg-slate-100 overflow-hidden border border-slate-200 flex">
                    {segments.map((segment, index) => (
                      <div
                        key={`${machine.id}-${index}`}
                        className="flex items-center justify-center text-[10px] text-white leading-none"
                        style={{
                          width: `${Math.max((segment.duration / rangeMs) * 100, 1)}%`,
                          backgroundColor: getStatusColor(segment.status)
                        }}
                        title={`${getStatusLabel(segment.status)} · ${formatRuntime(segment.duration)}\n${formatTime(new Date(segment.start))} - ${formatTime(new Date(segment.end))}`}
                      >
                        {segment.duration / rangeMs > 0.12 ? getStatusLabel(segment.status) : ''}
                      </div>
                    ))}
                  </div>
                  <div className="grid grid-cols-5 text-[11px] text-slate-500">
                    {axisSteps.map((label, idx) => (
                      <div key={`${machine.id}-axis-${idx}`} className="text-left first:pl-1 last:text-right last:pr-1">
                        {label}
                      </div>
                    ))}
                  </div>
                </div>
              );
            })() : (
              <div className="rounded-2xl border border-slate-200 bg-white/80 p-6 text-sm text-slate-600">
                Select a machine to show its operation timeline.
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
