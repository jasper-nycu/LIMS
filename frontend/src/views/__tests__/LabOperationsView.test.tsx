// src/views/__tests__/LabOperationsView.test.tsx
import { render, screen, fireEvent, within, cleanup } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { LabOperationsView, type WipWafer } from '../LabOperationsView';

// Mock API so FSM state changes work without a real backend
vi.mock('../../api/axiosInstance', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ data: [] })),
    post: vi.fn((url: string, body?: any) => {
      const machineData = (state: string, loaded: string[] = [], util = 0, error: string | null = null) => ({
        data: { data: { state, loadedWafers: loaded, currentUtil: util, error } },
      });
      if (url.includes('/dispatch')) {
        const codes: string[] = (body as any)?.waferCodes ?? [];
        return Promise.resolve(machineData('PROCESSING', codes, Math.round(codes.length / 50 * 100)));
      }
      if (url.includes('/unload'))          return Promise.resolve(machineData('IDLE'));
      if (url.includes('/emg-unload'))      return Promise.resolve(machineData('IDLE'));
      if (url.includes('/simulate-error'))  return Promise.resolve(machineData('ALARM', [], 0, 'ERR_SIMULATED_FAULT'));
      if (url.includes('/resolve-alarm'))   return Promise.resolve(machineData('PROCESSING'));
      if (url.includes('/maintenance'))     return Promise.resolve(machineData('MAINTENANCE'));
      if (url.includes('/online'))          return Promise.resolve(machineData('PROCESSING'));
      if (url.includes('/experiment-failed')) return Promise.resolve({ data: {} });
      if (url.includes('/recipes'))         return Promise.resolve({ data: { data: ['DYN-REC-2026'] } });
      return Promise.resolve({ data: {} });
    }),
    delete: vi.fn(() => Promise.resolve({ data: { data: [] } })),
  },
}));

describe('LabOperationsView - Enterprise Finite State Machine Testing Suite', () => {
  const mockOnNotify = vi.fn();

  // Helper: Dynamically generate compliant IDs (W-XXXX) for isolation [Requirement 3]
  const generateDynamicWafers = (count: number, expKey = 'exp_bake'): WipWafer[] => {
    return Array.from({ length: count }, (_, i) => {
      const code = `W-${(8000 + i).toString().padStart(4, '0')}`;
      return { id: code, waferCode: code, expKey, priority: 'NORMAL' as const };
    });
  };

  const defaultProps = (initialWips: WipWafer[] = []) => ({
    language: 'en' as const,
    onNotify: mockOnNotify,
    initialWips,
  });

  beforeEach(() => {
    vi.clearAllMocks();
    // Math.random < 0.9 → always success, keeping tests deterministic
    vi.spyOn(Math, 'random').mockReturnValue(0.5);
    cleanup();
  });

  // Helper: Find a specific machine card by its ID text
  const getMachineCard = (id: string) => {
    const card = screen.getByText(id).closest('div.bg-white') as HTMLElement;
    if (!card) throw new Error(`Machine card ${id} not found`);
    return card;
  };

  // Helper: Find the checkbox for a specific wafer ID in the WIP table
  const getWaferCheckbox = (waferId: string) => {
    const row = screen.getByText(waferId).closest('tr');
    return within(row!).getByRole('checkbox');
  };

  // Helper: Locate the WIP Table container for scoped queries
  const getWipTable = () => screen.getByText(/Pending Wafers/i).closest('div.bg-white') as HTMLElement;

  // --- Machine Management (FSM) ---
  describe('Machine Management (FSM)', () => {
    it('1 & 2. should follow full Machine FSM path', () => {
      const wips = generateDynamicWafers(1, 'exp_bake');
      render(<LabOperationsView {...defaultProps(wips)} />);

      const machId = 'BAKE-OVEN-01';
      const machCard = getMachineCard(machId);

      // [IDLE -> PROCESSING] Dispatch
      fireEvent.click(getWaferCheckbox(wips[0].id));
      fireEvent.change(screen.getByLabelText(/Target Machine/i), { target: { value: machId } });
      fireEvent.click(screen.getByRole('button', { name: /bolt Execute Dispatch/i }));
      expect(within(machCard).getByText('PROCESSING')).toBeInTheDocument();

      // [PROCESSING -> ALARM] Simulate Error
      fireEvent.click(within(machCard).getByRole('button', { name: /Simulate Error/i }));
      expect(within(machCard).getByText('ALARM')).toBeInTheDocument();

      // [ALARM -> MAINTENANCE] Open Modal and Toggle
      fireEvent.click(screen.getByRole('button', { name: /settings Manage Machines/i }));
      const modal = screen.getByRole('heading', { name: /Manage Machines/i }).closest('div.bg-white') as HTMLElement;
      const maintBtn = within(modal).getAllByTitle(/Toggle Maintenance/i).find(btn => within(btn.closest('div')!).queryByText(machId));
      fireEvent.click(maintBtn!);
      expect(within(machCard).getAllByText(/MAINTENANCE/i).length).toBeGreaterThan(0);

      // [MAINTENANCE -> PROCESSING] Set Online
      fireEvent.click(maintBtn!);
      expect(within(machCard).getByText('PROCESSING')).toBeInTheDocument();
      fireEvent.click(within(modal).getByText('close'));

      // [PROCESSING -> IDLE] Safe Unload — shows experiment result modal on success
      fireEvent.click(within(machCard).getByRole('button', { name: /^Unload$/ }));
      expect(within(machCard).getByText('IDLE')).toBeInTheDocument();
      // Verify experiment result modal appears and shows success
      expect(screen.getByText(/Experiment Successful!/i)).toBeInTheDocument();
    });

    it('3. should increase machine capacity count after dispatch', () => {
      const wips = generateDynamicWafers(3, 'exp_bake');
      render(<LabOperationsView {...defaultProps(wips)} />);
      const machCard = getMachineCard('BAKE-OVEN-01');

      fireEvent.click(getWaferCheckbox(wips[0].id));
      fireEvent.click(getWaferCheckbox(wips[1].id));
      fireEvent.click(screen.getByRole('button', { name: /bolt Execute Dispatch/i }));

      expect(within(machCard).getByText('2 / 50')).toBeInTheDocument();
    });

    it('4. should reject dispatch if wafers exceed machine capacity', () => {
      const wips = generateDynamicWafers(11, 'exp_deep'); // TEM-01 cap is 10
      render(<LabOperationsView {...defaultProps(wips)} />);

      fireEvent.change(screen.getByLabelText(/Target Machine/i), { target: { value: 'TEM-01' } });
      const checkboxes = within(getWipTable()).getAllByRole('checkbox').slice(1); // Select all 11
      checkboxes.forEach(cb => fireEvent.click(cb));

      fireEvent.click(screen.getByRole('button', { name: /bolt Execute Dispatch/i }));
      expect(screen.getByText(/Capacity Error/i)).toBeInTheDocument();
    });
  });

  // --- Wafers Process (FSM) ---
  describe('Wafers Process (FSM)', () => {
    it('1. Wafer Path: WIP -> PROCESSING -> Safe Unload', () => {
      const wips = generateDynamicWafers(1);
      render(<LabOperationsView {...defaultProps(wips)} />);
      const machCard = getMachineCard('BAKE-OVEN-01');

      fireEvent.click(getWaferCheckbox(wips[0].id));
      fireEvent.click(screen.getByRole('button', { name: /bolt Execute Dispatch/i }));
      expect(within(getWipTable()).queryByText(wips[0].id)).not.toBeInTheDocument();

      fireEvent.click(within(machCard).getByRole('button', { name: /^Unload$/ }));
      expect(within(machCard).getByText('IDLE')).toBeInTheDocument();
      // Experiment result modal should appear
      expect(screen.getByText(/Experiment Successful!/i)).toBeInTheDocument();
    });

    it('2. Wafer Path: WIP -> PROCESSING -> EMG Unload -> Reuse (Return to WIP)', () => {
      const wips = generateDynamicWafers(1);
      render(<LabOperationsView {...defaultProps(wips)} />);
      const machCard = getMachineCard('BAKE-OVEN-01');

      fireEvent.click(getWaferCheckbox(wips[0].id));
      fireEvent.click(screen.getByRole('button', { name: /bolt Execute Dispatch/i }));

      fireEvent.click(within(machCard).getByRole('button', { name: /EMG Unload/i }));
      fireEvent.click(screen.getByRole('button', { name: /assignment_return Reuse Wafers/i }));

      expect(within(getWipTable()).getByText(wips[0].id)).toBeInTheDocument();
    });

    it('3. Wafer Path: WIP -> PROCESSING -> EMG Unload -> Scrap (Discard)', () => {
      const wips = generateDynamicWafers(1);
      render(<LabOperationsView {...defaultProps(wips)} />);
      const machCard = getMachineCard('BAKE-OVEN-01');

      fireEvent.click(getWaferCheckbox(wips[0].id));
      fireEvent.click(screen.getByRole('button', { name: /bolt Execute Dispatch/i }));

      fireEvent.click(within(machCard).getByRole('button', { name: /EMG Unload/i }));
      fireEvent.click(screen.getByRole('button', { name: /delete_forever Scrap Wafers/i }));

      expect(within(getWipTable()).queryByText(wips[0].id)).not.toBeInTheDocument();
      expect(within(machCard).getByText('IDLE')).toBeInTheDocument();
    });

    it('4. Wafer Path: Recovery from Alarm', () => {
      const wips = generateDynamicWafers(1);
      render(<LabOperationsView {...defaultProps(wips)} />);
      const machCard = getMachineCard('BAKE-OVEN-01');

      fireEvent.click(getWaferCheckbox(wips[0].id));
      fireEvent.click(screen.getByRole('button', { name: /bolt Execute Dispatch/i }));
      fireEvent.click(within(machCard).getByRole('button', { name: /Simulate Error/i }));
      fireEvent.click(within(machCard).getByRole('button', { name: /Resolve Alarm/i }));

      expect(within(machCard).getByText('PROCESSING')).toBeInTheDocument();
    });
  });

  // --- Management Tools ---
  describe('Manage Tools Sync', () => {
    it('1. Manage Machines modal should sync status real-time', () => {
      render(<LabOperationsView {...defaultProps()} />);
      const machCard = getMachineCard('TEM-01');
      fireEvent.click(within(machCard).getByRole('button', { name: /Simulate Error/i }));

      fireEvent.click(screen.getByRole('button', { name: /settings Manage Machines/i }));
      const modal = screen.getByRole('heading', { name: /Manage Machines/i }).closest('div.bg-white') as HTMLElement;
      expect(within(modal).getByText('ALARM')).toBeInTheDocument();
    });

    it('2. Manage Recipes should update list after adding', () => {
      render(<LabOperationsView {...defaultProps()} />);
      fireEvent.click(screen.getByRole('button', { name: /list_alt Manage Recipes/i }));

      const input = screen.getByPlaceholderText(/New recipe name.../i);
      fireEvent.change(input, { target: { value: 'DYN-REC-2026' } });
      fireEvent.click(screen.getByRole('button', { name: 'Add' }));

      expect(screen.getAllByText('DYN-REC-2026').length).toBeGreaterThan(0);
    });
  });
});
