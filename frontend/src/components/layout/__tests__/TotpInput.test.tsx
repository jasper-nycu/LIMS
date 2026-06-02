// src/components/layout/__tests__/TotpInput.test.tsx
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { TotpInput } from '../TotpInput';

const defaultProps = {
  title: 'Email Verification',
  description: 'Enter the 6-digit code sent to your email.',
  value: '',
  onChange: vi.fn(),
  countdown: 0,
  resendLabel: 'Resend Code',
  onResend: vi.fn(),
};

describe('TotpInput', () => {
  // ── Rendering ──────────────────────────────────────────────────────────────

  it('renders title and description', () => {
    render(<TotpInput {...defaultProps} />);

    expect(screen.getByText('Email Verification')).toBeInTheDocument();
    expect(screen.getByText('Enter the 6-digit code sent to your email.')).toBeInTheDocument();
  });

  it('renders input with placeholder 000000 and maxLength 6', () => {
    render(<TotpInput {...defaultProps} />);

    const input = screen.getByPlaceholderText('000000');
    expect(input).toBeInTheDocument();
    expect(input).toHaveAttribute('maxLength', '6');
    expect(input).toHaveAttribute('type', 'text');
  });

  it('renders the resend button with the provided label', () => {
    render(<TotpInput {...defaultProps} />);

    expect(screen.getByRole('button', { name: /Resend Code/i })).toBeInTheDocument();
  });

  // ── Error display ──────────────────────────────────────────────────────────

  it('shows error message when error prop is provided', () => {
    render(<TotpInput {...defaultProps} error="Invalid or expired code." />);

    expect(screen.getByText('Invalid or expired code.')).toBeInTheDocument();
  });

  it('does not show error element when error prop is absent', () => {
    render(<TotpInput {...defaultProps} />);

    expect(screen.queryByText(/invalid/i)).not.toBeInTheDocument();
  });

  it('does not show error element when error is empty string', () => {
    render(<TotpInput {...defaultProps} error="" />);

    expect(screen.queryByText(/error/i)).not.toBeInTheDocument();
  });

  // ── Input behaviour ────────────────────────────────────────────────────────

  it('calls onChange with only digits stripped of non-digit characters', () => {
    const onChange = vi.fn();
    render(<TotpInput {...defaultProps} onChange={onChange} />);

    fireEvent.change(screen.getByPlaceholderText('000000'), { target: { value: '12a34b' } });

    expect(onChange).toHaveBeenCalledWith('1234');
  });

  it('reflects controlled value in the input', () => {
    render(<TotpInput {...defaultProps} value="123456" />);

    expect(screen.getByPlaceholderText('000000')).toHaveValue('123456');
  });

  // ── Resend button state ────────────────────────────────────────────────────

  it('disables resend button when countdown is greater than 0', () => {
    render(<TotpInput {...defaultProps} countdown={30} />);

    expect(screen.getByRole('button')).toBeDisabled();
  });

  it('enables resend button when countdown is 0', () => {
    render(<TotpInput {...defaultProps} countdown={0} />);

    expect(screen.getByRole('button')).not.toBeDisabled();
  });

  it('appends countdown seconds to label when countdown > 0', () => {
    render(<TotpInput {...defaultProps} countdown={42} resendLabel="Resend Code" />);

    expect(screen.getByRole('button')).toHaveTextContent('Resend Code (42s)');
  });

  it('shows only the base label when countdown is 0', () => {
    render(<TotpInput {...defaultProps} countdown={0} resendLabel="Resend Code" />);

    expect(screen.getByRole('button')).toHaveTextContent('Resend Code');
    expect(screen.getByRole('button').textContent).not.toContain('(');
  });

  it('calls onResend when button is clicked and countdown is 0', () => {
    const onResend = vi.fn();
    render(<TotpInput {...defaultProps} countdown={0} onResend={onResend} />);

    fireEvent.click(screen.getByRole('button'));

    expect(onResend).toHaveBeenCalledOnce();
  });

  it('does not call onResend when button is disabled (countdown > 0)', () => {
    const onResend = vi.fn();
    render(<TotpInput {...defaultProps} countdown={15} onResend={onResend} />);

    fireEvent.click(screen.getByRole('button'));

    expect(onResend).not.toHaveBeenCalled();
  });
});
