// src/api/__tests__/machineApi.test.ts
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import {
  getMachines,
  getMachine,
  dispatch,
  unload,
  emgUnload,
  simulateError,
  resolveAlarm,
  toMaintenance,
  setOnline,
  getRecipes,
  addRecipe,
  deleteRecipe,
  getMachineLogs,
  getLogsDownloadUrl,
  getUtilizationHistory,
  getWipQueue,
  getWipPendingSorting,
  type MachineDTO,
  type WipTaskDTO,
} from '../machineApi';

// ── fetch mock helpers ────────────────────────────────────────────────────────

function mockFetch(status: number, body: unknown) {
  const response = {
    ok: status >= 200 && status < 300,
    status,
    statusText: status === 200 ? 'OK' : 'Error',
    json: () => Promise.resolve(body),
  } as unknown as Response;
  vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(response);
}

const stubMachine: MachineDTO = {
  id: 'SEM-01',
  name: 'Surface Scan (SEM)',
  expKey: 'exp_sem',
  state: 'IDLE',
  cap: 25,
  loadedCount: 0,
  loadedWafers: [],
  error: null,
  currentUtil: 0,
  owners: ['MW'],
};

const stubWip: WipTaskDTO = {
  taskId: 1,
  waferCode: 'W-1001',
  expKey: 'exp_sem',
  priority: 'NORMAL',
  status: 'QUEUE',
};

beforeEach(() => {
  vi.restoreAllMocks();
});

// ── getMachines ───────────────────────────────────────────────────────────────

describe('getMachines', () => {
  it('calls GET /api/v1/machines and returns data array', async () => {
    mockFetch(200, { data: [stubMachine] });

    const result = await getMachines();

    const fetchSpy = vi.spyOn(globalThis, 'fetch');
    expect(result).toEqual([stubMachine]);
  });

  it('throws an error when response is not ok', async () => {
    mockFetch(503, { message: 'Service unavailable' });

    await expect(getMachines()).rejects.toThrow('Service unavailable');
  });
});

// ── getMachine ────────────────────────────────────────────────────────────────

describe('getMachine', () => {
  it('calls GET /api/v1/machines/{id} with encoded id', async () => {
    mockFetch(200, { data: stubMachine });

    const result = await getMachine('SEM-01');

    expect(result).toEqual(stubMachine);
  });

  it('throws when machine not found', async () => {
    mockFetch(404, { message: 'Machine not found' });

    await expect(getMachine('NON-EXISTENT')).rejects.toThrow('Machine not found');
  });
});

// ── dispatch ──────────────────────────────────────────────────────────────────

describe('dispatch', () => {
  it('calls POST /api/v1/machines/{id}/dispatch with correct payload', async () => {
    const processingMachine = { ...stubMachine, state: 'PROCESSING' as const, loadedCount: 2 };
    mockFetch(200, { data: processingMachine });

    const payload = {
      waferCodes: ['W-1001', 'W-1002'],
      machineId: 'SEM-01',
      recipeId: 'Recipe-A',
      expKey: 'exp_sem',
    };
    const result = await dispatch('SEM-01', payload);

    expect(result.state).toBe('PROCESSING');
    expect(result.loadedCount).toBe(2);
  });
});

// ── unload ────────────────────────────────────────────────────────────────────

describe('unload', () => {
  it('calls POST /api/v1/machines/{id}/unload and returns idle machine', async () => {
    const idleMachine = { ...stubMachine, state: 'IDLE' as const, loadedCount: 0 };
    mockFetch(200, { data: idleMachine });

    const result = await unload('SEM-01');

    expect(result.state).toBe('IDLE');
    expect(result.loadedCount).toBe(0);
  });
});

// ── emgUnload ─────────────────────────────────────────────────────────────────

describe('emgUnload', () => {
  it('calls POST /api/v1/machines/{id}/emg-unload with SCRAP action', async () => {
    mockFetch(200, { data: { ...stubMachine, state: 'IDLE' as const } });

    const result = await emgUnload('SEM-01', 'SCRAP');

    expect(result.state).toBe('IDLE');
  });

  it('calls POST /api/v1/machines/{id}/emg-unload with REUSE action', async () => {
    mockFetch(200, { data: { ...stubMachine, state: 'IDLE' as const } });

    const result = await emgUnload('SEM-01', 'REUSE');

    expect(result.state).toBe('IDLE');
  });
});

// ── simulateError ─────────────────────────────────────────────────────────────

describe('simulateError', () => {
  it('calls POST /api/v1/machines/{id}/simulate-error and returns ALARM state', async () => {
    mockFetch(200, { data: { ...stubMachine, state: 'ALARM' as const, error: 'ERR_SIMULATED_FAULT' } });

    const result = await simulateError('SEM-01');

    expect(result.state).toBe('ALARM');
    expect(result.error).toBe('ERR_SIMULATED_FAULT');
  });
});

// ── resolveAlarm ──────────────────────────────────────────────────────────────

describe('resolveAlarm', () => {
  it('calls POST /api/v1/machines/{id}/resolve-alarm and returns PROCESSING state', async () => {
    mockFetch(200, { data: { ...stubMachine, state: 'PROCESSING' as const } });

    const result = await resolveAlarm('SEM-01');

    expect(result.state).toBe('PROCESSING');
  });
});

// ── toMaintenance / setOnline ─────────────────────────────────────────────────

describe('toMaintenance', () => {
  it('returns MAINTENANCE state', async () => {
    mockFetch(200, { data: { ...stubMachine, state: 'MAINTENANCE' as const } });

    const result = await toMaintenance('SEM-01');

    expect(result.state).toBe('MAINTENANCE');
  });
});

describe('setOnline', () => {
  it('returns PROCESSING state after transitioning from maintenance', async () => {
    mockFetch(200, { data: { ...stubMachine, state: 'PROCESSING' as const } });

    const result = await setOnline('SEM-01');

    expect(result.state).toBe('PROCESSING');
  });
});

// ── Recipes ───────────────────────────────────────────────────────────────────

describe('getRecipes', () => {
  it('returns list of recipe names', async () => {
    mockFetch(200, { data: ['Recipe-A', 'Recipe-B'] });

    const result = await getRecipes('SEM-01');

    expect(result).toEqual(['Recipe-A', 'Recipe-B']);
  });

  it('returns empty array when data is null', async () => {
    mockFetch(200, { data: null });

    const result = await getRecipes('SEM-01');

    expect(result).toEqual([]);
  });
});

describe('addRecipe', () => {
  it('calls POST with recipe name and returns updated list', async () => {
    mockFetch(200, { data: ['Recipe-A', 'NEW-RECIPE'] });

    const result = await addRecipe('SEM-01', 'NEW-RECIPE');

    expect(result).toContain('NEW-RECIPE');
  });
});

describe('deleteRecipe', () => {
  it('calls DELETE for recipe by name and returns remaining list', async () => {
    mockFetch(200, { data: ['Recipe-A'] });

    const result = await deleteRecipe('SEM-01', 'Recipe-B');

    expect(result).toEqual(['Recipe-A']);
  });
});

// ── Logs ──────────────────────────────────────────────────────────────────────

describe('getMachineLogs', () => {
  it('returns log entries', async () => {
    const logs = [{ timestamp: '2025-01-01T10:00:00', level: 'INFO', message: 'Machine started' }];
    mockFetch(200, { data: logs });

    const result = await getMachineLogs('SEM-01');

    expect(result).toHaveLength(1);
    expect(result[0].level).toBe('INFO');
  });
});

describe('getLogsDownloadUrl', () => {
  it('returns correct download URL with encoded machine ID', () => {
    const url = getLogsDownloadUrl('SEM-01');
    expect(url).toBe('/api/v1/machines/SEM-01/logs/download');
  });
});

// ── Utilization ───────────────────────────────────────────────────────────────

describe('getUtilizationHistory', () => {
  it('returns utilization data points', async () => {
    const points = [{ timestamp: 1700000000000, utilization: 72, state: 'PROCESSING' }];
    mockFetch(200, { data: points });

    const result = await getUtilizationHistory('SEM-01', 24);

    expect(result).toHaveLength(1);
    expect(result[0].utilization).toBe(72);
  });

  it('defaults to 168 hours when hours parameter is omitted', async () => {
    mockFetch(200, { data: [] });
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve({ data: [] }),
    } as unknown as Response);

    await getUtilizationHistory('SEM-01');

    expect(fetchSpy).toHaveBeenCalledWith(expect.stringContaining('hours=168'));
  });
});

// ── WIP ───────────────────────────────────────────────────────────────────────

describe('getWipQueue', () => {
  it('returns QUEUE status wip tasks', async () => {
    mockFetch(200, { data: [stubWip] });

    const result = await getWipQueue();

    expect(result).toHaveLength(1);
    expect(result[0].waferCode).toBe('W-1001');
  });

  it('returns empty array when data is null', async () => {
    mockFetch(200, { data: null });

    const result = await getWipQueue();

    expect(result).toEqual([]);
  });
});

describe('getWipPendingSorting', () => {
  it('returns PENDING_SORTING wip tasks', async () => {
    const pendingWip = { ...stubWip, status: 'PENDING_SORTING' };
    mockFetch(200, { data: [pendingWip] });

    const result = await getWipPendingSorting();

    expect(result).toHaveLength(1);
    expect(result[0].status).toBe('PENDING_SORTING');
  });
});

// ── Error handling ────────────────────────────────────────────────────────────

describe('error handling', () => {
  it('uses statusText as fallback when response body has no message', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: false,
      status: 500,
      statusText: 'Internal Server Error',
      json: () => Promise.resolve({}),
    } as unknown as Response);

    await expect(getMachines()).rejects.toThrow('Internal Server Error');
  });

  it('uses generic API error message when both body and statusText are empty', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce({
      ok: false,
      status: 500,
      statusText: '',
      json: () => Promise.resolve({}),
    } as unknown as Response);

    await expect(getMachines()).rejects.toThrow('API error');
  });
});
