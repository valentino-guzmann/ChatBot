package com.chatbotmvt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionData implements Serializable {

    private String tipoReclamo;
    private String direccion;
    private String referencia;
    private Long pendingSectorId;

    @Builder.Default
    private Map<String, String> extraInfo = new HashMap<>();

    public void addExtra(String key, String value) {
        if (this.extraInfo == null) this.extraInfo = new HashMap<>();
        this.extraInfo.put(key, value);
    }
}