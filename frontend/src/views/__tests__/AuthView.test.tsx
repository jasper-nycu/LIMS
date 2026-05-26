// src/views/__tests__/AuthView.test.tsx
import { render, screen, fireEvent, cleanup, act, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { AuthView } from '../AuthView';

describe('AuthView - Enterprise Authentication & Verification Testing Suite', () => {
  const mockOnLanguageChange = vi.fn();
  const mockOnLoginSuccess = vi.fn();
  const mockOnNotify = vi.fn();

  const defaultProps = (lang: 'en' | 'tw' = 'en') => ({
    language: lang,
    onLanguageChange: mockOnLanguageChange,
    onLoginSuccess: mockOnLoginSuccess,
    onNotify: mockOnNotify,
  });

  beforeEach(() => {
    vi.clearAllMocks();
    vi.useFakeTimers(); // Intercept real-world clocks for deterministic TOTP countdown intervals

    // --- NEW: Mock global fetch API to simulate Backend Responses ---
    vi.stubGlobal('fetch', vi.fn().mockImplementation((url: string) => {
      if (url.includes('/login')) {
        const data = { token: 'mock-jwt', user: { name: 'Jasper Li', role: 'ROLE_SYSADMIN' } };
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(data),
          text: () => Promise.resolve(JSON.stringify(data))
        });
      }
      if (url.includes('/register/initiate')) {
        const data = { message: 'Sent' };
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(data),
          text: () => Promise.resolve(JSON.stringify(data))
        });
      }
      if (url.includes('/register/verify')) {
        const data = { message: 'Registration successful! Please sign in with your new account.' };
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(data),
          text: () => Promise.resolve(JSON.stringify(data))
        });
      }
      return Promise.resolve({ 
        ok: false, 
        json: () => Promise.resolve({}),
        text: () => Promise.resolve("{}")
      });
    }));
  });

  afterEach(() => {
    cleanup();
    vi.useRealTimers(); // Restore global runtime timers to preserve thread hygiene
  });

  describe('1. i18n Dictionary Localization & Mode Transitions', () => {
    it('should correctly render localized English tokens on initial mount', () => {
      render(<AuthView {...defaultProps('en')} />);

      expect(screen.getByText('LIMS Portal')).toBeInTheDocument();
      expect(screen.getByText('Laboratory Information Management System')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('TS-0001')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('Password')).toBeInTheDocument();
    });

    it('should invoke parent language callbacks when language switch selectors are toggled', () => {
      render(<AuthView {...defaultProps('en')} />);

      const twButton = screen.getByRole('button', { name: '中文' });
      fireEvent.click(twButton);
      expect(mockOnLanguageChange).toHaveBeenCalledWith('tw');
    });

    it('should smoothly toggle between Login and Registration forms via navigational anchors', () => {
      render(<AuthView {...defaultProps('en')} />);

      // Switch to Register Mode
      const createAccountLink = screen.getByText('New here? Create an account');
      fireEvent.click(createAccountLink);
      expect(screen.getByText('Create Account')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('First Name')).toBeInTheDocument();

      // Return back to Sign In Mode
      const backToLoginLink = screen.getByText('Back to Sign In');
      fireEvent.click(backToLoginLink);
      expect(screen.getByText('LIMS Portal')).toBeInTheDocument();
    });
  });

  describe('2. Authentication Flow & State Mapping Integrity', () => {
    it('should dispatch valid session properties up to root layout upon login form submit', async () => {
      render(<AuthView {...defaultProps('en')} />);

      const idInput = screen.getByPlaceholderText('TS-0001');
      const pwdInput = screen.getByPlaceholderText('Password');
      const submitBtn = screen.getByRole('button', { name: 'Sign In' });

      // Input admin test parameters
      fireEvent.change(idInput, { target: { value: 'TS-0001' } });
      fireEvent.change(pwdInput, { target: { value: 'securePass123' } });

      await act(async () => {
        fireEvent.submit(submitBtn);
      });

      expect(mockOnNotify).toHaveBeenCalledWith(null, 'Auth Success', 'Welcome back to LIMS Portal.', 'success');
      expect(mockOnLoginSuccess).toHaveBeenCalledWith(expect.objectContaining({
        name: 'Jasper Li'
      }));
    });
  });

  describe('3. Registration Guardrails & Pre-Auth Information Isolation', () => {
    beforeEach(() => {
      // Setup default active visibility into registration frame matrix
      render(<AuthView {...defaultProps('en')} />);
      fireEvent.click(screen.getByText('New here? Create an account'));
    });

    it('should block faulty email inputs using custom alert popups instead of leaking notifications to unauthenticated headers', () => {
      const emailInput = screen.getByPlaceholderText('example@tsmc.com');
      const submitBtn = screen.getByRole('button', { name: 'Register' });

      fireEvent.change(emailInput, { target: { value: 'faulty-email-format' } });
      fireEvent.submit(submitBtn);

      // Security check: Global onNotify must remain uncalled to safeguard pre-auth scopes
      expect(mockOnNotify).not.toHaveBeenCalled();

      // View compliance check: Secure internal warning popup modal must intercept execution bounds
      expect(screen.getByText('Validation Error')).toBeInTheDocument();
      expect(screen.getByText('Please enter a valid email address.')).toBeInTheDocument();

      // Resolve window instance boundaries
      fireEvent.click(screen.getByRole('button', { name: 'Understood' }));
      expect(screen.queryByText('Validation Error')).not.toBeInTheDocument();
    });

    it('should assert strict matching guardrails across password confirmation input pairs', () => {
      const emailInput = screen.getByPlaceholderText('example@tsmc.com');
      const pwdInput = screen.getByPlaceholderText('Create password');
      const confInput = screen.getByPlaceholderText('Confirm password');
      const submitBtn = screen.getByRole('button', { name: 'Register' });

      // Seed valid email format to bypass first guardrail check safely
      fireEvent.change(emailInput, { target: { value: 'jasper@tsmc.com' } });
      fireEvent.change(pwdInput, { target: { value: 'matrixPass1' } });
      fireEvent.change(confInput, { target: { value: 'matrixPass2' } });
      fireEvent.submit(submitBtn);

      expect(screen.getByText('Passwords do not match.')).toBeInTheDocument();
    });

    it('should enforce minimum length constraints to protect platform credential baselines', () => {
      const emailInput = screen.getByPlaceholderText('example@tsmc.com');
      const pwdInput = screen.getByPlaceholderText('Create password');
      const confInput = screen.getByPlaceholderText('Confirm password');
      const submitBtn = screen.getByRole('button', { name: 'Register' });

      fireEvent.change(emailInput, { target: { value: 'jasper@tsmc.com' } });
      fireEvent.change(pwdInput, { target: { value: 'short' } });
      fireEvent.change(confInput, { target: { value: 'short' } });
      fireEvent.submit(submitBtn);

      expect(screen.getByText('Password must be at least 8 characters long.')).toBeInTheDocument();
    });
  });

  describe('4. TOTP Verification Sequence & Interval Lifecycle Controls', () => {
    beforeEach(async () => {
      render(<AuthView {...defaultProps('en')} />);
      fireEvent.click(screen.getByText('New here? Create an account'));

      // Populate registration constraints safely to reach the authorization sub-modal layer
      fireEvent.change(screen.getByPlaceholderText('First Name'), { target: { value: 'Jasper' } });
      fireEvent.change(screen.getByPlaceholderText('Last Name'), { target: { value: 'Li' } });
      fireEvent.change(screen.getByPlaceholderText('example@tsmc.com'), { target: { value: 'jasper@tsmc.com' } });
      fireEvent.change(screen.getByPlaceholderText('Create password'), { target: { value: 'password123' } });
      fireEvent.change(screen.getByPlaceholderText('Confirm password'), { target: { value: 'password123' } });
      
      await act(async () => {
        fireEvent.submit(screen.getByRole('button', { name: 'Register' }));
      });

      expect(screen.getByText('Email Verification')).toBeInTheDocument();
    });

    it('should launch the TOTP layer modal and step-down countdown timers deterministically over virtual periods', () => {
      expect(screen.getByText('Email Verification')).toBeInTheDocument();
      
      const resendBtn = screen.getByRole('button', { name: /Resend Code/i });
      expect(resendBtn).toBeDisabled();
      expect(resendBtn).toHaveTextContent('Resend Code (60s)');

      // Advance virtual clock timeline by 30 periods wrapped inside act() to cleanly process component state ticks
      act(() => {
        vi.advanceTimersByTime(30000);
      });
      expect(resendBtn).toHaveTextContent('Resend Code (30s)');
      expect(resendBtn).toBeDisabled();

      // Complete full countdown scale parameters
      act(() => {
        vi.advanceTimersByTime(30000);
      });
      expect(resendBtn).not.toBeDisabled();
      expect(resendBtn).toHaveTextContent('Resend Code');
    });

    it('should throw validation blocks when non-conforming numeric length variants are targeted into the verification input', () => {
      const totpInput = screen.getByPlaceholderText('000000');
      const verifyBtn = screen.getByRole('button', { name: 'Verify & Complete' });

      // Feed short numerical combination variants
      fireEvent.change(totpInput, { target: { value: '123' } });
      fireEvent.click(verifyBtn);

      expect(screen.getByText('Please enter a valid 6-digit TOTP code.')).toBeInTheDocument();
    });

    it('should process valid 6-digit security strings, dissolve timers, and complete account provisioning phases', async () => {
      const totpInput = screen.getByPlaceholderText('000000');
      const verifyBtn = screen.getByRole('button', { name: 'Verify & Complete' });

      fireEvent.change(totpInput, { target: { value: '712345' } });
      
      await act(async () => {
        fireEvent.click(verifyBtn);
      });

      // Verify memory state handles and custom success popup modal layers are rendered instead of stale browser alerts
      expect(screen.queryByText('Email Verification')).not.toBeInTheDocument();
      expect(screen.getByText('Registration Successful')).toBeInTheDocument();
      expect(screen.getByText('Registration successful! Please sign in with your new account.')).toBeInTheDocument();
      
      // Resolve the custom alert popup to verify routing back to sign-in card view
      const understandBtn = screen.getByRole('button', { name: 'Understood' });
      fireEvent.click(understandBtn);

      // Verify redirect route back into initial user sign in module card layer
      expect(screen.getByText('LIMS Portal')).toBeInTheDocument();
    });
  });
});