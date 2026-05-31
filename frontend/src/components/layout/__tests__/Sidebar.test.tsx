// src/components/layout/__tests__/Sidebar.test.tsx
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { Sidebar } from '../Sidebar';

describe('Sidebar - Enterprise Layout Router Testing Suite', () => {
  const defaultProps = {
    isOpen: true,
    onToggle: vi.fn(),
    activeView: 'view-factory-request',
    onViewChange: vi.fn(),
    language: 'en' as const,
    user: {
      empId: 'TS-TEST',
      name: 'Test User',
      role: 'ROLE_SYSADMIN'
    }
  };

  it('renders correct labels in English', () => {
    render(<Sidebar {...defaultProps} />);
    expect(screen.getByText('Fab Request')).toBeInTheDocument();
    expect(screen.getByText('Lab Operations')).toBeInTheDocument();
  });

  it('renders correct labels in Chinese', () => {
    render(<Sidebar {...defaultProps} language="tw" />);
    expect(screen.getByText('建立委託單')).toBeInTheDocument();
    expect(screen.getByText('實驗室操作')).toBeInTheDocument();
  });

  it('triggers onViewChange with correct ID when an item is clicked', () => {
    render(<Sidebar {...defaultProps} />);
    const dashboardBtn = screen.getByText('Manager Dashboard');
    fireEvent.click(dashboardBtn);
    expect(defaultProps.onViewChange).toHaveBeenCalledWith('view-manager-dashboard');
  });

  it('applies hidden classes when isOpen is false', () => {
    const { container } = render(<Sidebar {...defaultProps} isOpen={false} />);
    const aside = container.querySelector('aside');
    // Check for the translation class that hides the sidebar
    expect(aside).toHaveClass('-translate-x-full');
  });

  it('highlights the active navigation item', () => {
    render(<Sidebar {...defaultProps} activeView="view-lab-operations" />);
    const activeBtn = screen.getByText('Lab Operations').closest('button');
    // Check for the corporate-blue background which indicates active state
    expect(activeBtn).toHaveClass('bg-corporate-blue');
  });
});