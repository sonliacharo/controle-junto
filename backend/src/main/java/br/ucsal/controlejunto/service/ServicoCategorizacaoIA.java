package br.ucsal.controlejunto.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Funcionalidade com IA (RF-010): sugere a categoria de um gasto a partir da descrição.
 *
 * 1. Se houver chave da API configurada (GEMINI_API_KEY), pergunta ao modelo de linguagem.
 * 2. Se não houver chave, se a API falhar ou demorar mais de 3 segundos (RNF-015),
 *    usa uma classificação local por palavras-chave.
 *
 * Para atender à LGPD, apenas o texto da descrição é enviado; nenhum dado pessoal.
 */
@Service
public class ServicoCategorizacaoIA {

    public static final String OUTROS = "Outros";

    private static final Logger log = LoggerFactory.getLogger(ServicoCategorizacaoIA.class);

    /** Palavras-chave usadas quando a IA não está disponível. */
    private static final Map<String, List<String>> PALAVRAS_CHAVE = new LinkedHashMap<>();

    static {
        PALAVRAS_CHAVE.put("Transporte", List.of("uber", "99", "taxi", "onibus", "metro", "gasolina",
                "combustivel", "estacionamento", "pedagio", "passagem", "brt", "corrida", "posto"));
        PALAVRAS_CHAVE.put("Alimentação", List.of("mercado", "supermercado", "atacadao", "padaria", "restaurante",
                "lanche", "pizza", "ifood", "almoco", "jantar", "cafe", "acai", "feira", "hamburguer", "comida"));
        PALAVRAS_CHAVE.put("Moradia", List.of("aluguel", "condominio", "iptu", "reforma", "moveis", "casa"));
        PALAVRAS_CHAVE.put("Contas e Serviços", List.of("luz", "energia", "coelba", "agua", "embasa", "internet",
                "telefone", "celular", "gas", "netflix", "spotify", "assinatura", "conta"));
        PALAVRAS_CHAVE.put("Saúde", List.of("farmacia", "remedio", "medico", "consulta", "exame", "hospital",
                "dentista", "plano de saude", "academia"));
        PALAVRAS_CHAVE.put("Educação", List.of("faculdade", "mensalidade", "curso", "livro", "material escolar",
                "apostila", "escola", "ucsal"));
        PALAVRAS_CHAVE.put("Lazer", List.of("cinema", "show", "ingresso", "bar", "festa", "praia", "jogo", "teatro",
                "balada", "parque"));
        PALAVRAS_CHAVE.put("Viagem", List.of("pousada", "hotel", "airbnb", "hospedagem", "voo", "aereo", "viagem",
                "chapada", "rodoviaria", "diaria"));
        PALAVRAS_CHAVE.put("Compras", List.of("roupa", "sapato", "tenis", "shopping", "loja", "presente",
                "eletronico", "shopee", "amazon", "mercado livre"));
    }

    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http;
    private final String chave;
    private final String modelo;
    private final Duration timeout;

    public ServicoCategorizacaoIA(@Value("${app.ia.chave:}") String chave,
                                  @Value("${app.ia.modelo:gemini-3.8-flash}") String modelo,
                                  @Value("${app.ia.timeout-segundos:3}") long timeoutSegundos) {
        this.chave = chave;
        this.modelo = modelo;
        this.timeout = Duration.ofSeconds(timeoutSegundos);
        this.http = HttpClient.newBuilder().connectTimeout(this.timeout).build();
        if (iaConfigurada()) {
            log.info("Categorização por IA ativa (Google Gemini, modelo {}).", modelo);
        } else {
            log.warn("GEMINI_API_KEY não configurada: a categoria será sugerida pela classificação local "
                    + "(palavras-chave). Veja o arquivo .env.exemplo.");
        }
    }

    private boolean iaConfigurada() {
        return chave != null && !chave.isBlank();
    }

    /** Resultado da sugestão: o nome da categoria e de onde ela veio. */
    public record Sugestao(String categoria, String origem) {
    }

    public Sugestao sugerir(String descricao, List<String> categorias) {
        if (iaConfigurada()) {
            Optional<String> daIA = perguntarAoModelo(descricao, categorias);
            if (daIA.isPresent()) {
                return new Sugestao(daIA.get(), "IA");
            }
        }
        return new Sugestao(classificarPorPalavrasChave(descricao), "REGRAS");
    }

    /** Chama a API do Google Gemini. Devolve vazio em caso de erro ou resposta fora da lista. */
    Optional<String> perguntarAoModelo(String descricao, List<String> categorias) {
        String prompt = "Classifique o gasto a seguir em uma destas categorias: "
                + String.join(", ", categorias)
                + ". Responda somente com o nome da categoria, exatamente como está na lista. Gasto: "
                + descricao;
        try {
            Map<String, Object> config = new LinkedHashMap<>();
            config.put("temperature", 0);
            // O limite inclui os tokens de "raciocínio" do modelo; a resposta em si é só o nome da categoria
            config.put("maxOutputTokens", 1024);
            if (modelo.startsWith("gemini-2.5")) {
                // Gemini 2.5: desliga o "raciocínio" para responder dentro do limite de tempo (RNF-015)
                config.put("thinkingConfig", Map.of("thinkingBudget", 0));
            } else if (modelo.startsWith("gemini-3")) {
                // Gemini 3.x: não permite desligar o raciocínio; o nível mais baixo é "low"
                config.put("thinkingConfig", Map.of("thinkingLevel", "low"));
            }
            Map<String, Object> corpo = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                    "generationConfig", config);
            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + URLEncoder.encode(modelo, StandardCharsets.UTF_8) + ":generateContent";
            HttpRequest requisicao = HttpRequest.newBuilder(URI.create(url))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", chave)
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(corpo)))
                    .build();
            HttpResponse<String> resposta = http.send(requisicao, HttpResponse.BodyHandlers.ofString());
            if (resposta.statusCode() != 200) {
                log.warn("API de IA respondeu {}: {}. Usando classificação local.", resposta.statusCode(),
                        resposta.body().length() > 300 ? resposta.body().substring(0, 300) : resposta.body());
                return Optional.empty();
            }
            JsonNode texto = json.readTree(resposta.body())
                    .path("candidates").path(0).path("content").path("parts").path(0).path("text");
            String respostaModelo = texto.asText("").trim();
            return categorias.stream()
                    .filter(c -> normalizar(respostaModelo).contains(normalizar(c)))
                    .findFirst();
        } catch (Exception e) {
            log.warn("Falha ao consultar a API de IA ({}). Usando classificação local.", e.getMessage());
            return Optional.empty();
        }
    }

    /** Classificação local, usada como reserva. */
    static String classificarPorPalavrasChave(String descricao) {
        String texto = " " + normalizar(descricao) + " ";
        for (Map.Entry<String, List<String>> entrada : PALAVRAS_CHAVE.entrySet()) {
            for (String palavra : entrada.getValue()) {
                if (texto.contains(" " + palavra + " ") || texto.contains(" " + palavra)) {
                    return entrada.getKey();
                }
            }
        }
        return OUTROS;
    }

    /** Minúsculas e sem acentos, para comparar textos. */
    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }
}