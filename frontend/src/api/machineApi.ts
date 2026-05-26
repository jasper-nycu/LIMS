// src/api/machineApi.ts
export type MachineDTO = {
  id: string;
  name: string;
  expKey: string;
  state: 'IDLE' | 'PROCESSING' | 'ALARM' | 'MAINTENANCE';
  cap: number;
  loadedCount: number;
  error: string | null;
  currentUtil: number;
  owners: string[];
};

const base = '/api/machines';

async function handleResponse(res: Response) {
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    const msg = body && (body.error || body.message) ? (body.error || body.message) : res.statusText;
    throw new Error(msg || 'API error');
  }
  return res.json().catch(() => null);
}

export async function getMachines(): Promise<MachineDTO[]> {
  const res = await fetch(base);
  return handleResponse(res);
}

export async function getMachine(id: string): Promise<MachineDTO> {
  const res = await fetch(`${base}/${encodeURIComponent(id)}`);
  return handleResponse(res);
}

export async function dispatchToMachine(id: string, waferIds: string[]): Promise<MachineDTO> {
  const res = await fetch(`${base}/${encodeURIComponent(id)}/dispatch`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ waferIds })
  });
  return handleResponse(res);
}

export async function toggleAlarm(id: string): Promise<MachineDTO> {
  const res = await fetch(`${base}/${encodeURIComponent(id)}/alarm`, { method: 'POST' });
  return handleResponse(res);
}

export async function toggleMaintenance(id: string): Promise<MachineDTO> {
  const res = await fetch(`${base}/${encodeURIComponent(id)}/maintenance`, { method: 'POST' });
  return handleResponse(res);
}

export async function getRecipes(id: string): Promise<string[]> {
  const res = await fetch(`${base}/${encodeURIComponent(id)}/recipes`);
  return handleResponse(res) || [];
}

export async function addRecipe(id: string, name: string): Promise<string[]> {
  const res = await fetch(`${base}/${encodeURIComponent(id)}/recipes`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name })
  });
  return handleResponse(res) || [];
}

export default { getMachines, getMachine, dispatchToMachine, toggleAlarm, toggleMaintenance, getRecipes, addRecipe };
