package com.mvc.nms_be.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Bảng cấu hình hệ thống dạng key/value, ví dụ: SiteName = "FU News", PageSize = "10".
// Hibernate (ddl-auto=update) tự tạo bảng SystemSetting khi chạy ứng dụng.
@Entity
@Table(name = "SystemSetting", uniqueConstraints =
        @UniqueConstraint(name = "UQ_SystemSetting_Key", columnNames = "SettingKey"))
@Getter
@Setter
@NoArgsConstructor
public class SystemSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // ID do SQL Server tự tăng
    @Column(name = "SettingID")
    private Integer settingId;

    // Tên cấu hình, không được trùng (ràng buộc UNIQUE ở trên)
    @Column(name = "SettingKey", nullable = false, length = 100, columnDefinition = "nvarchar(100)")
    private String settingKey;

    // Giá trị lưu dạng chuỗi; nơi sử dụng tự đổi sang số/boolean nếu cần
    @Column(name = "SettingValue", nullable = false, length = 1000, columnDefinition = "nvarchar(1000)")
    private String settingValue;

    @Column(name = "Description", length = 500, columnDefinition = "nvarchar(500)")
    private String description;
}
