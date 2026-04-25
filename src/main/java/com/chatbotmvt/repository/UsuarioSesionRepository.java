package com.chatbotmvt.repository;

import com.chatbotmvt.entity.UsuarioSesion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioSesionRepository extends JpaRepository<UsuarioSesion, Long> {
    Optional<UsuarioSesion> findByPhone(String phone);
}
