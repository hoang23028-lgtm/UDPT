package com.quiz.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "app_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppSettings {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Column(name = "maintenance_mode", nullable = false)
    @Builder.Default
    private boolean maintenanceMode = false;

    @Column(name = "announcement_message", length = 2000)
    private String announcementMessage;
}
