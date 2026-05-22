import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { CapacityAnalyticsView } from '../CapacityAnalyticsView';

const { mockChartConstructor } = vi.hoisted(() => {
  const constructorFn = vi.fn().mockImplementation(function() {
    return {
      destroy: vi.fn(),
      update: vi.fn(),
    };
  });

  (constructorFn as any).register = vi.fn();

  return {
    mockChartConstructor: constructorFn,
  };
});

vi.mock('chart.js', () => ({
  Chart: mockChartConstructor,
  registerables: [],
}));

describe('CapacityAnalyticsView machine operation timeline', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('keeps BAKE-OVEN-01 historical processing segments when the current state becomes ALARM', () => {
    const now = Date.now();

    const { container } = render(
      <CapacityAnalyticsView
        language="en"
        machines={{
          'BAKE-OVEN-01': {
            id: 'BAKE-OVEN-01',
            state: 'ALARM',
            loadedCount: 0,
            cap: 50,
            expKey: 'exp_bake',
            name: 'High-Temp Bake',
            error: 'ERR_SIMULATED_FAULT',
            currentUtil: 0,
            owners: [],
            utilHistory: [
              { timestamp: now - 45 * 60_000, util: 50 },
              { timestamp: now - 20 * 60_000, util: 50 },
              { timestamp: now - 1 * 60_000, util: 0, state: 'ALARM' },
            ],
          },
        }}
      />
    );

    expect(screen.getAllByText('BAKE-OVEN-01').length).toBeGreaterThan(0);
    expect(container.querySelector('[title^="PROCESSING"]')).toBeInTheDocument();
    expect(container.querySelector('[title^="ALARM"]')).toBeInTheDocument();
  });
});
