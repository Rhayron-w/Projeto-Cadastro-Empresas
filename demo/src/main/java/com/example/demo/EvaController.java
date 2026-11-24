package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Ponte entre o chat do site e a IA em Python (Chat/api_server.py).
 * O navegador só fala com /api/eva/*; quem fala com o Python é o Spring.
 */
@RestController
@RequestMapping("/api/eva")
public class EvaController {

    private static final int TAMANHO_MAXIMO_MENSAGEM = 1000; // mesmo limite do Python

    // Usados se o Python não enviar a lista de atalhos
    private static final List<String> ATALHOS_PADRAO = List.of(
            "Conhecer a Eva", "Áreas de atuação", "Quero uma demonstração",
            "Abrir chamado", "Sou cliente", "Trabalhe conosco");

    private final RestClient clienteIa;
    private final String campoResposta;

    public record PerguntaEva(String mensagem, String sessionId) {}

    public EvaController(
            @Value("${eva.ia.url}") String urlIa,
            @Value("${eva.ia.campo-resposta:reply}") String campoResposta,
            @Value("${eva.ia.timeout-segundos:60}") long timeoutSegundos) {

        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(Duration.ofSeconds(timeoutSegundos));

        this.clienteIa = RestClient.builder()
                .baseUrl(urlIa)
                .requestFactory(fabrica)
                .build();
        this.campoResposta = campoResposta;
    }

    /** Saudação da EVA e botões de atalho (vêm do company.json do Python). */
    @GetMapping("/greeting")
    public ResponseEntity<Map<String, Object>> saudacao() {
        try {
            Map<?, ?> resposta = clienteIa.get().uri("/greeting").retrieve().body(Map.class);
            Object texto = resposta == null ? null : resposta.get(campoResposta);
            if (texto == null || texto.toString().isBlank()) {
                return ResponseEntity.status(502).build();
            }

            List<String> atalhos = ATALHOS_PADRAO;
            if (resposta.get("atalhos") instanceof List<?> lista && !lista.isEmpty()) {
                atalhos = lista.stream().map(String::valueOf).toList();
            }

            Map<String, Object> corpo = Map.of("mensagem", texto.toString(), "atalhos", atalhos);
            return ResponseEntity.ok(corpo);
        } catch (RestClientException e) {
            return ResponseEntity.status(502).build();
        }
    }

    @PostMapping("/chat")
    public ResponseEntity<String> chat(@RequestBody PerguntaEva pergunta) {
        String mensagem = pergunta.mensagem() == null ? "" : pergunta.mensagem().trim();

        if (mensagem.isEmpty()) {
            return ResponseEntity.badRequest().body("Digite uma mensagem.");
        }
        if (mensagem.length() > TAMANHO_MAXIMO_MENSAGEM) {
            return ResponseEntity.badRequest().body("Mensagem muito longa (máximo " + TAMANHO_MAXIMO_MENSAGEM + " caracteres).");
        }

        try {
            Map<?, ?> resposta = clienteIa.post()
                    .uri("/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", mensagem, "session_id", limparSessao(pergunta.sessionId())))
                    .retrieve()
                    .body(Map.class);
            return extrairTexto(resposta);

        } catch (RestClientException e) {
            // Python desligado, demorou demais ou respondeu com erro
            return ResponseEntity.status(502).body("A EVA está indisponível no momento.");
        }
    }

    private ResponseEntity<String> extrairTexto(Map<?, ?> resposta) {
        Object texto = resposta == null ? null : resposta.get(campoResposta);
        if (texto == null || texto.toString().isBlank()) {
            return ResponseEntity.status(502).body("A IA não devolveu uma resposta.");
        }
        return ResponseEntity.ok(texto.toString());
    }

    /** O Python usa o session_id para lembrar a conversa. Aceita só letras, números, _ e -. */
    private String limparSessao(String sessionId) {
        if (sessionId == null) return "anon";
        String limpo = sessionId.replaceAll("[^A-Za-z0-9_-]", "");
        if (limpo.isEmpty()) return "anon";
        return limpo.length() > 64 ? limpo.substring(0, 64) : limpo;
    }
}
