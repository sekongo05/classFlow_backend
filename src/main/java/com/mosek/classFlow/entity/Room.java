package com.mosek.classFlow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Room extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "wifi_ssid", nullable = false, length = 120)
    private String wifiSsid;

    @Column(name = "network_secret", nullable = false, length = 120)
    private String networkSecret;

    @Column(name = "qr_code_token", nullable = false, unique = true, length = 120)
    private String qrCodeToken;

    @Column(nullable = false)
    @Builder.Default
    private boolean operational = true;
}
