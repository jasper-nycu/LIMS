// src/views/__tests__/CapacityAnalyticsView.test.tsx
import { render, screen, fireEvent, cleanup } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { CapacityAnalyticsView } from '../CapacityAnalyticsView';

// Secure hoisted variables for mock isolation within the Vitest compiler lifecycle
const { mockDestroy, mockChartConstructor } = vi.hoisted(() => {
  const destroyFn = vi.fn();
  
  // Use a traditional constructible function instead of an arrow function
  const constructorFn = vi.fn().mockImplementation(function() {
    return {
      destroy: destroyFn,
      update: vi.fn(),
    };
  });
  
  // Attach the static register method to prevent runtime evaluation crashes
  (constructorFn as any).register = vi.fn();

  return {
    mockDestroy: destroyFn,
    mockChartConstructor: constructorFn,
  };
});

vi.mock('chart.js', () => {
  return {
    Chart: mockChartConstructor,
    registerables: [],
  };
});

describe('CapacityAnalyticsView - Enterprise Telemetry Testing Suite', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  describe('1. Internationalization (i18n) Layout Matrix Verification', () => {
    it('should correctly mount with standard English strings and layout structures', () => {
      render(<CapacityAnalyticsView language="en" />);

      // Verify header parameters
      expect(screen.getByText('Machine Utilization Time-Series')).toBeInTheDocument();
      expect(screen.getByText('Avg Utilization')).toBeInTheDocument();

      // Verify that Chart.js was instantiated for both equipment categories and status summary
      expect(mockChartConstructor).toHaveBeenCalledTimes(3);

      // Inspect specific instantiation arguments sent to the first chart (Analysis Equipment)
      const firstChartArgs = mockChartConstructor.mock.calls[0][1];
      expect(firstChartArgs.options.plugins.title.text).toBe('Analysis Equipment');
      expect(firstChartArgs.options.scales.y.title.text).toBe('Utilization (%)');
    });

    it('should hot-swap dynamically and map exact Traditional Chinese vocabulary tokens', () => {
      render(<CapacityAnalyticsView language="tw" />);

      // Verify localized header markers
      expect(screen.getByText('機台利用率時序圖')).toBeInTheDocument();
      expect(screen.getByText('平均稼動率')).toBeInTheDocument();

      // Inspect specific instantiation arguments sent to the second chart (Process Equipment)
      const secondChartArgs = mockChartConstructor.mock.calls[1][1];
      expect(secondChartArgs.options.plugins.title.text).toBe('製程與測試機台');
      expect(secondChartArgs.options.scales.y.title.text).toBe('利用率 (%)');
    });
  });

  describe('2. Timeline Horizon & Dropdown Control Flow Interactivity', () => {
    it('should refresh chart models and recalculate average bounds when time horizon shifts', () => {
      const { container } = render(<CapacityAnalyticsView language="en" />);
      
      // Clear original mounting calls to cleanly trace interaction impacts
      mockChartConstructor.mockClear();
      mockDestroy.mockClear();

      // Locate the time-series range dropdown element
      const selectElement = container.querySelector('#chartTimeRange') as HTMLSelectElement;
      expect(selectElement).toBeInTheDocument();

      // Simulate operator swapping from Last 1 Hour ('1h') to Last 1 Week ('1w')
      fireEvent.change(selectElement, { target: { value: '1w' } });

      // Assert previous charts were safely discarded (triggered via useEffect cleanup and inline re-init checks)
      expect(mockDestroy).toHaveBeenCalledTimes(6);

      // Assert that new chart engines were re-provisioned with the newly calculated intervals
      expect(mockChartConstructor).toHaveBeenCalledTimes(3);

      // Verify that the newest chart configuration includes the 'Now' token on its final coordinate node
      const currentChartConfig = mockChartConstructor.mock.calls[0][1];
      const timeLabels = currentChartConfig.data.labels;
      expect(timeLabels[timeLabels.length - 1]).toBe('Now');
    });
  });

  describe('3. Resource Management & Garbage Collection Guardrails', () => {
    it('should invoke destroy handlers upon unmounting to eliminate detached DOM memory leaks', () => {
      const { unmount } = render(<CapacityAnalyticsView language="en" />);
      
      expect(mockDestroy).not.toHaveBeenCalled();

      // Trigger standard React view unmounting process
      unmount();

      // Strict memory guardrail check: Chart memory pointers must be completely detached
      expect(mockDestroy).toHaveBeenCalledTimes(3);
    });
  });
});
