// src/components/layout/__tests__/Header.test.tsx
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { Header } from '../Header';

describe('Header - Enterprise Global Navigation Testing Suite', () => {
  const defaultProps = {
    onToggleMenu: vi.fn(),
    language: 'en' as const,
    onLanguageChange: vi.fn(),
    user: null,
    onProfileClick: vi.fn(),
    notifications: [],
    onDeleteNotification: vi.fn(),
    onClearAllNotifications: vi.fn(),
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
    expect(screen.getByText(/Username/i)).toBeInTheDocument();
    expect(screen.getByText(/Public/i)).toBeInTheDocument();
  });

  it('calls onLanguageChange when language buttons are clicked', () => {
    render(<Header {...defaultProps} />);
    const twBtn = screen.getByText('中文');
    fireEvent.click(twBtn);
    expect(defaultProps.onLanguageChange).toHaveBeenCalledWith('tw');
  });

  it('shows notification badge when hasNew is true and notifications exist', () => {
    const propsWithNotifs = {
      ...defaultProps,
      hasNew: true,
      notifications: [{ id: '1', title: 'Test', desc: 'Test', type: 'info' as const }],
    };
    const { container } = render(<Header {...propsWithNotifs} />);
    expect(container.querySelector('.bg-red-500')).toBeInTheDocument();
  });

  it('triggers onMarkAsRead when notification panel is opened', () => {
    render(<Header {...defaultProps} />);
    const bellBtn = screen.getByText('notifications');
    fireEvent.click(bellBtn);
    expect(defaultProps.onMarkAsRead).toHaveBeenCalled();
  });

  it('renders the Base64 avatar when user profile has one', () => {
    const avatarBase64 = 'data:image/png;base64,aGVsbG8=';
    const { container } = render(
      <Header
        {...defaultProps}
        user={{ empId: 'TS-0001', name: 'John Doe', role: 'ROLE_PUBLIC', avatarBase64 }}
      />
    );

    expect(container.querySelector('img')).toHaveAttribute('src', avatarBase64);
  });
});
