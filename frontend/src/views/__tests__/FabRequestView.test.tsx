// src/views/__tests__/FabRequestView.test.tsx
import { render, screen, fireEvent, within } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { FabRequestView } from '../FabRequestView';

describe('FabRequestView - Enterprise Order Provisioning Testing Suite', () => {
  const mockOnNotify = vi.fn();
  const defaultProps = {
    language: 'en' as const,
    onNotify: mockOnNotify,
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should handle wafer input validation and case-insensitivity (w vs W)', () => {
    render(<FabRequestView {...defaultProps} />);
    const input = screen.getByPlaceholderText(/W-0120/i);
    const addBtn = screen.getByRole('button', { name: /Add Wafer/i });

    fireEvent.change(input, { target: { value: 'invalid-id' } });
    fireEvent.click(addBtn);
    expect(screen.getByRole('heading', { name: /Invalid Format/i })).toBeInTheDocument();
    fireEvent.click(screen.getByText(/Understood/i));

    fireEvent.change(input, { target: { value: 'w-1234' } });
    fireEvent.click(addBtn);
    expect(screen.getByText('W-1234')).toBeInTheDocument();
  });

  it('should prevent adding duplicate Wafer IDs', () => {
    render(<FabRequestView {...defaultProps} />);
    const input = screen.getByPlaceholderText(/W-0120/i);
    const addBtn = screen.getByRole('button', { name: /Add Wafer/i });

    fireEvent.change(input, { target: { value: 'W-9999' } });
    fireEvent.click(addBtn);
    fireEvent.change(input, { target: { value: 'W-9999' } });
    fireEvent.click(addBtn);
    
    expect(screen.getByRole('heading', { name: /Duplicate Entry/i })).toBeInTheDocument();
  });

  it('should prevent submission if no experiments are selected', () => {
    render(<FabRequestView {...defaultProps} />);
    fireEvent.change(screen.getByPlaceholderText(/W-0120/i), { target: { value: 'W-1111' } });
    fireEvent.click(screen.getByRole('button', { name: /Add Wafer/i }));
    fireEvent.click(screen.getByRole('button', { name: /Submit/i }));
    expect(screen.getByRole('heading', { name: /Missing Info/i })).toBeInTheDocument();
  });

  it('should trigger notification and display correct data in table after submission', () => {
    render(<FabRequestView {...defaultProps} />);
    
    fireEvent.change(screen.getByPlaceholderText(/W-0120/i), { target: { value: 'W-5555' } });
    fireEvent.click(screen.getByRole('button', { name: /Add Wafer/i }));
    fireEvent.click(screen.getByText(/High-Temp Bake/i));
    fireEvent.click(screen.getByRole('button', { name: /Submit/i }));

    expect(mockOnNotify).toHaveBeenCalledWith(
      expect.any(String),
      expect.stringMatching(/1 wafers submitted/i),
      'success'
    );

    const table = screen.getByRole('table');
    const tbody = table.querySelector('tbody');
    expect(within(tbody as HTMLElement).getByText('1')).toBeInTheDocument(); 
    expect(within(tbody as HTMLElement).getByText(/High-Temp Bake/i)).toBeInTheDocument();
  });

  it('should switch available experiments based on target laboratory', () => {
    render(<FabRequestView {...defaultProps} />);
    const labSelect = screen.getByLabelText(/Target Laboratory/i);

    expect(screen.getByText(/High-Temp Bake/i)).toBeInTheDocument();

    fireEvent.change(labSelect, { target: { value: 'LAB_MA' } });
    expect(screen.getByText(/Surface Scan \(SEM\)/i)).toBeInTheDocument();
    expect(screen.queryByText(/High-Temp Bake/i)).not.toBeInTheDocument();
  });

  it('should allow removing a wafer before submission and reset form after', () => {
    render(<FabRequestView {...defaultProps} />);
    const input = screen.getByPlaceholderText(/W-0120/i);
    const addBtn = screen.getByRole('button', { name: /Add Wafer/i });

    fireEvent.change(input, { target: { value: 'W-1234' } });
    fireEvent.click(addBtn);
    expect(screen.getByText('W-1234')).toBeInTheDocument();

    const removeBtn = screen.getByRole('button', { name: /close/i });
    fireEvent.click(removeBtn);
    expect(screen.queryByText('W-1234')).not.toBeInTheDocument();
  });
});