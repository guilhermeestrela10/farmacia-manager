package com.farmacia.farmacia.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.farmacia.farmacia.model.Perfil;
import com.farmacia.farmacia.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByLogin(String login);

    boolean existsByLogin(String login);

    long countByPerfilAndAtivoTrue(Perfil perfil);

    List<Usuario> findAllByOrderByNome();
}