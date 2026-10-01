package com.nexatech.nexacare.repository;

import com.nexatech.nexacare.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<Usuario> findAllByOrderByNomeAsc();

    Optional<Usuario> findByProfissionalId(Long profissionalId);
}
