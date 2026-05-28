// src/views/__tests__/MyProfileView.test.tsx
import { render, screen, fireEvent, cleanup, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

// Mock API requests to prevent pending promises and external network failures
vi.mock('../../api/axiosInstance', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ data: {} })),
    put: vi.fn(() => Promise.resolve({ data: {} })),
    post: vi.fn(() => Promise.resolve({ data: {} })),
    delete: vi.fn(() => Promise.resolve({ data: {} }))
  }
}));
import { MyProfileView } from '../MyProfileView';
import { type UserProfile } from '../../components/layout/Header';

describe('MyProfileView - Industrial Robustness & RBAC Compliance Tests', () => {
  const mockOnUpdateUser = vi.fn();
  const mockOnLogout = vi.fn();
  const mockOnNotify = vi.fn();

  const createMockUser = (role: string): UserProfile => ({
    empId: 'TS-0120',
    name: 'John Doe',
    role: role,
  });

  const defaultProps = (user: UserProfile | null = null) => ({
    language: 'en' as const,
    user: user,
    onUpdateUser: mockOnUpdateUser,
    onLogout: mockOnLogout,
    onNotify: mockOnNotify,
  });

  // Foolproof helper to find inputs dynamically by their sibling labels
  const getInputByLabel = (container: HTMLElement, labelText: string): HTMLInputElement => {
    const labels = Array.from(container.querySelectorAll('label, h4, span'));
    const targetLabel = labels.find(el => el.textContent?.trim() === labelText);
    if (!targetLabel) {
      throw new Error(`Could not find label with text: ${labelText}`);
    }
    return targetLabel.parentElement?.querySelector('input, select') as HTMLInputElement;
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  describe('1. Personal Information Validation (Regex Guardrails)', () => {
    it('should accept valid formatting and reject illegal strings for Names', () => {
      const { container } = render(<MyProfileView {...defaultProps(createMockUser('ROLE_PUBLIC'))} />);
      
      const firstNameInput = getInputByLabel(container, 'First Name');
      const lastNameInput = getInputByLabel(container, 'Last Name');
      const saveButton = screen.getByRole('button', { name: /Save Changes/i });

      // Test Case 1a: Invalid character in First Name (Numbers/Symbols)
      fireEvent.change(firstNameInput, { target: { value: 'John123!' } });
      fireEvent.click(saveButton);
      expect(mockOnNotify).toHaveBeenCalledWith(null, 'Validation Error', expect.stringContaining('First Name'), 'error');

      // Test Case 1b: Valid English and Chinese formatting
      vi.clearAllMocks();
      fireEvent.change(firstNameInput, { target: { value: 'John' } });
      fireEvent.change(lastNameInput, { target: { value: 'Doe' } });
      fireEvent.click(saveButton);
      expect(mockOnNotify).not.toHaveBeenCalledWith(null, 'Validation Error', expect.any(String), 'error');
    });

    it('should only permit alphanumeric strings and Chinese/English for Department', () => {
      const { container } = render(<MyProfileView {...defaultProps(createMockUser('ROLE_PUBLIC'))} />);
      const deptInput = getInputByLabel(container, 'Department');
      const saveButton = screen.getByRole('button', { name: /Save Changes/i });

      // Test Case 2a: Invalid characters (Special punctuation)
      fireEvent.change(deptInput, { target: { value: 'Fab12_@' } });
      fireEvent.click(saveButton);
      expect(mockOnNotify).toHaveBeenCalledWith(null, 'Validation Error', expect.stringContaining('Department'), 'error');

      // Test Case 2b: Valid alphanumeric and Chinese
      vi.clearAllMocks();
      fireEvent.change(deptInput, { target: { value: '黃光衍生課 12B' } });
      fireEvent.click(saveButton);
      expect(mockOnNotify).not.toHaveBeenCalledWith(null, 'Validation Error', expect.any(String), 'error');
    });

    it('should strictly validate the standard E-Mail Address configuration', () => {
      const { container } = render(<MyProfileView {...defaultProps(createMockUser('ROLE_PUBLIC'))} />);
      const emailInput = getInputByLabel(container, 'E-Mail Address');
      const saveButton = screen.getByRole('button', { name: /Save Changes/i });

      // Test Case 3a: Missing @ or domain extension
      fireEvent.change(emailInput, { target: { value: 'invalid-email.com' } });
      fireEvent.click(saveButton);
      expect(mockOnNotify).toHaveBeenCalledWith(null, 'Validation Error', expect.stringContaining('E-Mail Address'), 'error');
    });

    it('should validate the exact formats for corporate Telephone lines', () => {
      const { container } = render(<MyProfileView {...defaultProps(createMockUser('ROLE_PUBLIC'))} />);
      const telInput = getInputByLabel(container, 'Telephone');
      const saveButton = screen.getByRole('button', { name: /Save Changes/i });

      // Test Case 4a: Bad area code formatting (completely invalid string)
      fireEvent.change(telInput, { target: { value: '12345' } }); // Adjusted to definitively fail the updated regex guard
      fireEvent.click(saveButton);
      expect(mockOnNotify).toHaveBeenCalledWith(null, 'Validation Error', expect.stringContaining('Telephone'), 'error');

      // Test Case 4b: Correct TSMC style Hsinchu node code
      vi.clearAllMocks();
      fireEvent.change(telInput, { target: { value: '+886-3-5636688' } });
      fireEvent.click(saveButton);
      expect(mockOnNotify).not.toHaveBeenCalledWith(null, 'Validation Error', expect.any(String), 'error');
    });

    it('should assert the structural rule for TSMC Extension lines', () => {
      const { container } = render(<MyProfileView {...defaultProps(createMockUser('ROLE_PUBLIC'))} />);
      const extInput = getInputByLabel(container, 'Extension');
      const saveButton = screen.getByRole('button', { name: /Save Changes/i });

      // Test Case 5a: Out of length parameters
      fireEvent.change(extInput, { target: { value: '123' } });
      fireEvent.click(saveButton);
      expect(mockOnNotify).toHaveBeenCalledWith(null, 'Validation Error', expect.stringContaining('Extension'), 'error');
    });

    it('should ensure the Mobile Phone string conforms with the standard format', () => {
      const { container } = render(<MyProfileView {...defaultProps(createMockUser('ROLE_PUBLIC'))} />);
      const mobileInput = getInputByLabel(container, 'Mobile Phone');
      const saveButton = screen.getByRole('button', { name: /Save Changes/i });

      fireEvent.change(mobileInput, { target: { value: '0800123456' } }); // 0800 will trigger the validation error
      fireEvent.click(saveButton);
      expect(mockOnNotify).toHaveBeenCalledWith(null, 'Validation Error', expect.stringContaining('Mobile Phone'), 'error');
    });
  });

  describe('2. Security Settings & RBAC Privilege Transformations', () => {
    it('should verify that System Privileges description list is unique and updates per role', () => {
      const rolesToTest = [
        'ROLE_SYSADMIN',
        'ROLE_LAB_MANAGER',
        'ROLE_LAB_OPERATOR',
        'ROLE_MACHINE_OWNER',
        'ROLE_FAB_USER',
        'ROLE_PUBLIC'
      ];

      const collectedPrivileges: Record<string, string[]> = {};

      rolesToTest.forEach(role => {
        const { container, unmount } = render(<MyProfileView {...defaultProps(createMockUser(role))} />);
        
        const listItems = container.querySelectorAll('ul.space-y-3 li');
        const privileges = Array.from(listItems).map(li => li.textContent?.trim() || '');
        collectedPrivileges[role] = privileges;
        
        unmount();
      });

      // Verify variance between different logical privilege tiers (e.g., SysAdmin vs Public)
      expect(collectedPrivileges['ROLE_SYSADMIN']).not.toEqual(collectedPrivileges['ROLE_PUBLIC']);
      expect(collectedPrivileges['ROLE_LAB_MANAGER']).not.toEqual(collectedPrivileges['ROLE_FAB_USER']);
    });

    it('should broadcast signals over the onNotify grid for Save, 2FA, and Passwords', async () => {
      const { container } = render(<MyProfileView {...defaultProps(createMockUser('ROLE_PUBLIC'))} />);
      
      // A. Test Trigger for 2FA tracking network
      const toggle2FABtn = container.querySelector('button[class*="relative inline-flex"]') as HTMLButtonElement;
      fireEvent.click(toggle2FABtn);
      
      await waitFor(() => {
        expect(mockOnNotify).toHaveBeenCalledWith(null, 'Security Settings Updated', expect.any(String), expect.any(String));
      });

      // B. Test Trigger for Password Change sequence modal (2-Step Flow)
      const changePwdBtn = screen.getByRole('button', { name: /Change Password/i });
      fireEvent.click(changePwdBtn);
      
      const oldPwdInput = getInputByLabel(container, 'Current Password');
      const newPwdInput = getInputByLabel(container, 'New Password');
      const confirmPwdInput = getInputByLabel(container, 'Confirm New Password');
      
      fireEvent.change(oldPwdInput, { target: { value: 'oldSecret123' } });
      fireEvent.change(newPwdInput, { target: { value: 'newSecret456' } });
      fireEvent.change(confirmPwdInput, { target: { value: 'newSecret456' } });
      
      // Step 1: Click "Next" to trigger API verification and advance to Step 2
      const nextBtn = screen.getByRole('button', { name: /Next/i });
      fireEvent.click(nextBtn);
      
      // Wait for the step 2 (TOTP) UI to appear
      await waitFor(() => {
        expect(screen.getByText(/Email Verification/i)).toBeInTheDocument();
      });

      // Step 2: Enter the 6-digit TOTP code
      const totpInput = screen.getByPlaceholderText('000000');
      fireEvent.change(totpInput, { target: { value: '123456' } });
      
      // Click final Update Password button
      const updatePwdBtn = screen.getByRole('button', { name: /Update Password/i });
      fireEvent.click(updatePwdBtn);
      
      await waitFor(() => {
        expect(mockOnNotify).toHaveBeenCalledWith(null, 'Password Changed', expect.any(String), 'success');
      });

      // C. Test Trigger for Profile Save dispatch
      const saveButton = screen.getByRole('button', { name: /Save Changes/i });
      fireEvent.click(saveButton);
      
      await waitFor(() => {
        expect(mockOnNotify).toHaveBeenCalledWith(null, 'Profile Synchronized', expect.any(String), 'success');
      });
    });
  });

  describe('3. Component Architecture and Synchronous Flow Consistency', () => {
    it('should render an uploaded Base64 avatar from user profile state', () => {
      const userWithAvatar: UserProfile = {
        ...createMockUser('ROLE_PUBLIC'),
        avatarBase64: 'data:image/png;base64,aGVsbG8=',
      };
      const { container } = render(<MyProfileView {...defaultProps(userWithAvatar)} />);

      expect(container.querySelector('img')).toHaveAttribute('src', userWithAvatar.avatarBase64);
    });

    it('should assert strong synchronization across the avatar subtitle and read-only Primary Role inputbox', () => {
      const { container } = render(<MyProfileView {...defaultProps(createMockUser('ROLE_LAB_MANAGER'))} />);

      // Element A: Subtitle located below username avatar box
      const avatarCardRoleText = container.querySelector('p.text-accent-sky')?.textContent?.trim();

      // Element B: Value from the locked read-only field inside the card layout
      const readOnlyRoleInputBox = container.querySelector('input[readonly]') as HTMLInputElement;
      const readOnlyFieldText = readOnlyRoleInputBox.value.trim();

      // Core Consistency Assertion
      expect(avatarCardRoleText).toBeDefined();
      expect(readOnlyFieldText).toBeDefined();
      expect(avatarCardRoleText).toEqual(readOnlyFieldText);
    });
  });
});
