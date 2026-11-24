package com.example.demo;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class UsuarioController {

    private static final int TAMANHO_MINIMO_SENHA = 6;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UsuarioController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/cadastrar")
    public ResponseEntity<String> cadastrar(@RequestBody Usuario novoUsuario) {
        String email = normalizar(novoUsuario.getEmail());
        String senha = novoUsuario.getSenha();

        if (email.isEmpty() || !email.contains("@")) {
            return ResponseEntity.badRequest().body("Informe um e-mail válido.");
        }
        if (senha == null || senha.length() < TAMANHO_MINIMO_SENHA) {
            return ResponseEntity.badRequest()
                    .body("A senha deve ter pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres.");
        }
        if (usuarioRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.status(409).body("Este e-mail já está cadastrado!");
        }

        novoUsuario.setId(null); // impede que o cliente sobrescreva um usuário existente enviando um id
        novoUsuario.setEmail(email);
        novoUsuario.setSenha(passwordEncoder.encode(senha));

        try {
            usuarioRepository.save(novoUsuario);
        } catch (DataIntegrityViolationException e) {
            // dois cadastros do mesmo e-mail ao mesmo tempo
            return ResponseEntity.status(409).body("Este e-mail já está cadastrado!");
        }
        return ResponseEntity.ok("Usuário cadastrado com sucesso!");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Usuario dadosLogin) {
        String email = normalizar(dadosLogin.getEmail());
        String senha = dadosLogin.getSenha();

        Optional<Usuario> usuario = usuarioRepository.findByEmail(email);

        if (usuario.isPresent() && senha != null
                && passwordEncoder.matches(senha, usuario.get().getSenha())) {
            return ResponseEntity.ok("Login efetuado com sucesso!");
        }
        return ResponseEntity.status(401).body("E-mail ou senha incorretos!");
    }

    private String normalizar(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
