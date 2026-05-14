// src/components/layout/__tests__/Header.test.tsx
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { Header } from '../Header';

describe('Header Component', () => {
  const defaultProps = {
    onToggleMenu: vi.fn(),
    language: 'en' as const,
    onLanguageChange: vi.fn(),
    user: null, // Stateless mode
    onProfileClick: vi.fn(),
    notifications: [],
    onDeleteNotification: vi.fn(),
    hasNew: false,
    onMarkAsRead: vi.fn(),
  };

  it('renders application title and logo', () => {
    render(<Header {...defaultProps} />);
    expect(screen.getByText(/LIMS/i)).toBeInTheDocument();
    expect(screen.getByText(/Cloud-Native/i)).toBeInTheDocument();
  });

  it('displays "Username" and "Public" in stateless mode', () => {
  render(<Header {...defaultProps} />);
  
  // Using regex /text/i makes the search case-insensitive
  // This is better because CSS 'uppercase' doesn't change the underlying DOM text
  expect(screen.getByText(/Username/i)).toBeInTheDocument();
  expect(screen.getByText(/Public/i)).toBeInTheDocument();
});

  it('calls onLanguageChange when language buttons are clicked', () => {
    render(<Header {...defaultProps} />);
    const chineseBtn = screen.getByText('中文');
    fireEvent.click(chineseBtn);
    expect(defaultProps.onLanguageChange).toHaveBeenCalledWith('tw');
  });

  it('shows notification badge when hasNew is true and notifications exist', () => {
    const propsWithNotifs = {
      ...defaultProps,
      hasNew: true,
      notifications: [{ id: '1', title: 'Test', desc: 'Test', type: 'info' as const }],
    };
    const { container } = render(<Header {...propsWithNotifs} />);
    // Check for the presence of the red dot (span with bg-red-500)
    const badge = container.querySelector('.bg-red-500');
    expect(badge).toBeInTheDocument();
  });

  it('triggers onMarkAsRead when notification panel is opened', () => {
    render(<Header {...defaultProps} />);
    const bellBtn = screen.getByText('notifications');
    fireEvent.click(bellBtn);
    expect(defaultProps.onMarkAsRead).toHaveBeenCalled();
  });
});