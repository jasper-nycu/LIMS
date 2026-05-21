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
}

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
}

export const CapacityAnalyticsView: React.FC<CapacityAnalyticsViewProps> = ({ language, machines = {} }) => {
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
      time_1w: 'Last 1 Week'
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
      time_1w: '近 1 週'
    }
  };

  const ui = i18n[language];

  // Controlled UI dropdown tracker for timeline bounds
  const [timeRange, setTimeRange] = useState<string>('1h');
  const [avgUtilization, setAvgUtilization] = useState<string>('0.0%');

  // DOM Canvas Hooks to guarantee isolated memory allocations
  const chartRef1 = useRef<HTMLCanvasElement | null>(null);
  const chartRef2 = useRef<HTMLCanvasElement | null>(null);
  const instanceRef1 = useRef<Chart | null>(null);
  const instanceRef2 = useRef<Chart | null>(null);

  // Helper date parsing mirroring standard index.html telemetry functions
  const formatTime = (d: Date) => `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`;
  const formatDate = (d: Date) => `${(d.getMonth() + 1).toString().padStart(2, '0')}/${d.getDate().toString().padStart(2, '0')}`;

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

    // Handle standard memory garbage collection on component unmounting steps
    return () => {
      if (instanceRef1.current) instanceRef1.current.destroy();
      if (instanceRef2.current) instanceRef2.current.destroy();
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
          <p className="text-[10px] text-slate-400 font-medium">{ui.calcRule}</p>
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
    </div>
  );
};