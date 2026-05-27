// src/views/__tests__/ManagerDashboardView.test.tsx
import { render, screen, fireEvent, act, cleanup } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { ManagerDashboardView, type ManagerRequest } from '../ManagerDashboardView';

describe('ManagerDashboardView - Enterprise Supervisor Approval Testing Suite', () => {
  const mockOnNotify = vi.fn();

  // Helper: Dynamically generate unique requests [Requirement: Stateless]
  const generateDynamicRequests = (count: number): ManagerRequest[] => {
    return Array.from({ length: count }, (_, i) => ({
      id: `REQ-${(1000 + i).toString()}`,
      requester: `Tester ${i}`,
      role: 'Fab User',
      waferIds: [`W-TEST-${i}`],
      experiments: ['exp_sem'],
      remarks: `Dynamic remark for test case ${i}`,
      priority: i % 2 === 0 ? 'URGENT' : 'NORMAL',
      submitTime: '2026-05-15 14:53',
    }));
  };

  const defaultProps = (requests: ManagerRequest[] = []) => ({
    language: 'en' as const,
    onNotify: mockOnNotify,
    initialRequests: requests,
  });

  beforeEach(() => {
    vi.clearAllMocks();
    vi.useFakeTimers(); // Enable fake timers for 400ms animation logic
  });

  afterEach(() => {
    vi.runOnlyPendingTimers();
    vi.useRealTimers();
    cleanup();
  });

  describe('Stateless Approval Flow', () => {
    it('should generate a dynamic request and remove it with animation upon approval', async () => {
      const singleRequest = generateDynamicRequests(1);
      render(<ManagerDashboardView {...defaultProps(singleRequest)} />);

      // Use Regex to handle text split by bullet points/timestamps in the same element
      expect(screen.getByText(new RegExp(singleRequest[0].id))).toBeInTheDocument();
      
      // Select the Approve button
      const approveBtn = screen.getByRole('button', { name: /Approve/i });
      fireEvent.click(approveBtn);

      // Transition check: Item should still be in DOM immediately after click (fade-out phase)
      expect(screen.getByText(new RegExp(singleRequest[0].id))).toBeInTheDocument();

      // IMPORTANT: Manually advance timers by 400ms inside act to trigger state update
      await act(async () => {
        vi.advanceTimersByTime(400);
      });

      // Verification: Now the item should be removed from DOM
      expect(screen.queryByText(new RegExp(singleRequest[0].id))).not.toBeInTheDocument();
      
      // Use case-insensitive regex for PENDING text to handle whitespace/formatting
      expect(screen.getByText(/0\s+PENDING/i)).toBeInTheDocument();
    });

    it('should properly clear pulsing animation when 0 PENDING', async () => {
      const singleRequest = generateDynamicRequests(1);
      render(<ManagerDashboardView {...defaultProps(singleRequest)} />);

      // Check if animate-ping class exists (pulsing indicator)
      expect(document.querySelector('.animate-ping')).toBeInTheDocument();

      fireEvent.click(screen.getByRole('button', { name: /Approve/i }));
      
      await act(async () => {
        vi.advanceTimersByTime(400);
      });

      // Pulsing indicator must be removed when count reaches 0
      expect(document.querySelector('.animate-ping')).not.toBeInTheDocument();
    });
  });

  describe('Stateless Rejection Flow', () => {
    it('should purge rejected request from list after modal confirmation', async () => {
      const dynamicList = generateDynamicRequests(1);
      render(<ManagerDashboardView {...defaultProps(dynamicList)} />);

      // Open Reject Modal
      fireEvent.click(screen.getByRole('button', { name: /Reject/i }));
      expect(screen.getByText(/Reject Request/i)).toBeInTheDocument();

      // Enter Reason
      const textarea = screen.getByPlaceholderText(/Enter reason here.../i);
      fireEvent.change(textarea, { target: { value: 'Incomplete documentation' } });
      
      const confirmBtn = screen.getByRole('button', { name: /Confirm Reject/i });
      fireEvent.click(confirmBtn);

      // Advance clock 400ms to trigger handleReject setTimeout
      await act(async () => {
        vi.advanceTimersByTime(400);
      });

      expect(screen.queryByText(new RegExp(dynamicList[0].id))).not.toBeInTheDocument();
      expect(mockOnNotify).toHaveBeenCalledWith(null, 'Request Rejected', expect.stringContaining('Incomplete documentation'), 'error');
    });
  });

  describe('Data Isolation Check', () => {
    it('should only render data from its current session (Isolation Check)', () => {
      const sessionData = generateDynamicRequests(2);
      render(<ManagerDashboardView {...defaultProps(sessionData)} />);

      expect(screen.getByText(new RegExp(sessionData[0].id))).toBeInTheDocument();
      expect(screen.getByText(new RegExp(sessionData[1].id))).toBeInTheDocument();
      
      // Ensure specific hardcoded mock IDs (like REQ-8842) are not present [Requirement: Stateless]
      expect(screen.queryByText('REQ-8842')).not.toBeInTheDocument();
    });
  });

  describe('Queue Sorting', () => {
    it('orders requests by priority and then submit time within the same priority', () => {
      const requests: ManagerRequest[] = [
        { ...generateDynamicRequests(1)[0], id: 'REQ-NORMAL', priority: 'NORMAL', submitTime: '2026-05-15 09:00' },
        { ...generateDynamicRequests(1)[0], id: 'REQ-CRIT-OLD', priority: 'CRITICAL', submitTime: '2026-05-15 08:00' },
        { ...generateDynamicRequests(1)[0], id: 'REQ-URGENT', priority: 'URGENT', submitTime: '2026-05-15 07:00' },
        { ...generateDynamicRequests(1)[0], id: 'REQ-CRIT-NEW', priority: 'CRITICAL', submitTime: '2026-05-15 10:00' },
      ];

      render(<ManagerDashboardView {...defaultProps(requests)} />);

      const bodyRows = screen.getAllByRole('row').slice(1);
      expect(bodyRows.map(row => row.textContent)).toEqual([
        expect.stringContaining('REQ-CRIT-OLD'),
        expect.stringContaining('REQ-CRIT-NEW'),
        expect.stringContaining('REQ-URGENT'),
        expect.stringContaining('REQ-NORMAL'),
      ]);
    });
  });
});
