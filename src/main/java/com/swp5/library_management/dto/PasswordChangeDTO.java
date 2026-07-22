package com.swp5.library_management.dto;

public class PasswordChangeDTO {
  private String oldPassword;
    private String newPassword;
    private String confirmPassword;

    // --- BẮT BUỘC PHẢI CÓ GETTER VÀ SETTER ---
    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
