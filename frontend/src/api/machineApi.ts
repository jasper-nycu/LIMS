// src/api/machineApi.ts

export type MachineDTO = {
  id: string;
  name: string;
  expKey: string;
  state: 'IDLE' | 'PROCESSING' | 'ALARM' | 'MAINTENANCE';
  cap: number;
  loadedCount: number;
  loadedWafers: string[];
  error: string | null;
  currentUtil: number;
  owners: string[];
};

export type WipTaskDTO = {
  taskId: number;
  waferCode: string;
  expKey: string;
  priority: string;
  status: string;
};

export type LogEntry = {
  timestamp: string;
  level: string;
  message: string;
};

export type DispatchPayload = {
  waferCodes: string[];
  machineId: string;
  recipeId: string;
  expKey: string;
  requestId?: string;
};

const base = '/api/v1/machines';
const wipBase = '/api/v1/wip';

async function handleResponse<T>(res: Response): Promise<T> {
  const body = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new Error(body?.message || res.statusText || 'API error');
  }
  return body.data as T;
}

// ── Machines ──────────────────────────────────────────────────────────────

export async function getMachines(): Promise<MachineDTO[]> {
  return handleResponse<MachineDTO[]>(await fetch(base));
}

export async function getMachine(id: string): Promise<MachineDTO> {
  return handleResponse<MachineDTO>(await fetch(`${base}/${encodeURIComponent(id)}`));
}

// ── FSM Actions ───────────────────────────────────────────────────────────

export async function dispatch(id: string, payload: DispatchPayload): Promise<MachineDTO> {
  return handleResponse<MachineDTO>(await fetch(`${base}/${encodeURIComponent(id)}/dispatch`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ requestId: 'REQ-SYS-INIT', ...payload }),
  }));
}

export async function unload(id: string): Promise<MachineDTO> {
  return handleResponse<MachineDTO>(await fetch(`${base}/${encodeURIComponent(id)}/unload`, { method: 'POST' }));
}

export async function emgUnload(id: string, action: 'SCRAP' | 'REUSE'): Promise<MachineDTO> {
  return handleResponse<MachineDTO>(await fetch(`${base}/${encodeURIComponent(id)}/emg-unload`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ action }),
  }));
}

export async function simulateError(id: string): Promise<MachineDTO> {
  return handleResponse<MachineDTO>(await fetch(`${base}/${encodeURIComponent(id)}/simulate-error`, { method: 'POST' }));
}

export async function resolveAlarm(id: string): Promise<MachineDTO> {
  return handleResponse<MachineDTO>(await fetch(`${base}/${encodeURIComponent(id)}/resolve-alarm`, { method: 'POST' }));
}

export async function toMaintenance(id: string): Promise<MachineDTO> {
  return handleResponse<MachineDTO>(await fetch(`${base}/${encodeURIComponent(id)}/maintenance`, { method: 'POST' }));
}

export async function setOnline(id: string): Promise<MachineDTO> {
  return handleResponse<MachineDTO>(await fetch(`${base}/${encodeURIComponent(id)}/online`, { method: 'POST' }));
}

// ── Recipes ───────────────────────────────────────────────────────────────

export async function getRecipes(id: string): Promise<string[]> {
  return handleResponse<string[]>(await fetch(`${base}/${encodeURIComponent(id)}/recipes`)) ?? [];
}

export async function addRecipe(id: string, name: string): Promise<string[]> {
  return handleResponse<string[]>(await fetch(`${base}/${encodeURIComponent(id)}/recipes`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name }),
  })) ?? [];
}

export async function deleteRecipe(id: string, name: string): Promise<string[]> {
  return handleResponse<string[]>(await fetch(`${base}/${encodeURIComponent(id)}/recipes/${encodeURIComponent(name)}`, {
    method: 'DELETE',
  })) ?? [];
}

// ── Logs ──────────────────────────────────────────────────────────────────

export async function getMachineLogs(id: string): Promise<LogEntry[]> {
  return handleResponse<LogEntry[]>(await fetch(`${base}/${encodeURIComponent(id)}/logs`)) ?? [];
}

export function getLogsDownloadUrl(id: string): string {
  return `${base}/${encodeURIComponent(id)}/logs/download`;
}

// ── WIP ───────────────────────────────────────────────────────────────────

export async function getWipQueue(): Promise<WipTaskDTO[]> {
  return handleResponse<WipTaskDTO[]>(await fetch(wipBase)) ?? [];
}

export async function getWipPendingSorting(): Promise<WipTaskDTO[]> {
  return handleResponse<WipTaskDTO[]>(await fetch(`${wipBase}/pending-sorting`)) ?? [];
}
