package com.generation.fitness_spring.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.generation.fitness_spring.exception.GeminiApiException;
import com.generation.fitness_spring.model.Usuario;
import com.generation.fitness_spring.repository.UsuarioRepository;

import io.github.cdimascio.dotenv.Dotenv;

@Service
public class DietaGeminiService {
	
    private static final Logger logger = LoggerFactory.getLogger(DietaGeminiService.class);
    
    private static final Dotenv dotenv = Dotenv.load();
    
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?";
    private static final String API_KEY = dotenv.get("API_KEY");
    
    //private static final String API_KEY = "AIzaSyC4sLt3r-1Zss9B7mh6gJGPx7RK33Z6wQk"; 
    
    private final RestTemplate restTemplate;
    private final UsuarioRepository usuarioRepository;

    public DietaGeminiService(RestTemplate restTemplate, UsuarioRepository usuarioRepository) {
        this.restTemplate = restTemplate;
        this.usuarioRepository = usuarioRepository;
        
    }

    public Map<String, Object> gerarDieta(long id) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        String prompt = criarPromptDieta(usuario);
        String resposta = chamarGeminiAPI(prompt);

        return processarRespostaDieta(resposta);
    }

    private String criarPromptDieta(Usuario usuario) {
    	
        int idade = calcularIdade(usuario.getDataNascimento());
        double peso = usuario.getPeso();
        double altura = usuario.getAltura();
        String objetivo;
        
        if (usuario.getImc() < 18.50)
        	objetivo = "Ganho de peso saudável";
        else if (usuario.getImc() < 24.90)
        	objetivo = "Manter o peso saudável";
        else if (usuario.getImc() < 29.90)
        	objetivo = "Reduzir a gordura corporal";
        else
        	objetivo = "Perda de peso significativa";

        return String.format(
            "Crie um plano alimentar detalhado para uma pessoa com as seguintes características: " +
            "Idade: %d anos, Peso: %.2f kg, Altura: %.2f m, Objetivo: %s. " +
            "O plano deve incluir obrigatoriamente 6 refeições (café da manhã, lanche matinal, almoço, lanche da tarde, jantar, ceia). " +
            "Para cada refeição, indique: nome do prato, ingredientes, modo de preparo e total de calorias. " +
            "Use linguagem clara e direta. Formate a resposta em JSON.", 
            idade, peso, altura, objetivo
        );
    }

    private String chamarGeminiAPI(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(
            Map.of("parts", List.of(
                Map.of("text", prompt)
            ))
        ));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            String response = restTemplate.postForObject(
                GEMINI_API_URL + "key=" + API_KEY, 
                request, 
                String.class
            );
            
            logger.info(response);
            
            // Validação da resposta
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            
            // Verificação de conteúdo válido
            if (!root.has("candidates") || root.path("candidates").isEmpty()) {
                logger.warn("Nenhuma resposta válida recebida da API Gemini");
                throw new GeminiApiException("Sem respostas válidas", HttpStatus.NO_CONTENT);
            }
            
            // Extração do texto da resposta
            return root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
            
        } catch (HttpClientErrorException e) {
            // Erros de cliente (4xx)
            logger.error("Erro de cliente na chamada da Gemini API", e);
            throw new GeminiApiException("Erro de cliente: " + e.getStatusText(), e.getStatusCode());

        } catch (HttpServerErrorException e) {
            // Erros de servidor (5xx)
            logger.error("Erro de servidor na chamada da Gemini API", e);
            throw new GeminiApiException("Erro interno do servidor", HttpStatus.INTERNAL_SERVER_ERROR);

        } catch (JsonProcessingException e) {
            // Erro de parsing do JSON
            logger.error("Erro ao processar resposta JSON", e);
            throw new GeminiApiException("Erro no processamento da resposta", HttpStatus.UNPROCESSABLE_ENTITY);

        } catch (RestClientException e) {
            // Outros erros de comunicação REST
            logger.error("Erro de comunicação com a API Gemini", e);
            throw new GeminiApiException("Falha na comunicação", HttpStatus.SERVICE_UNAVAILABLE);

        } catch (Exception e) {
            // Tratamento de exceções inesperadas
            logger.error("Erro inesperado ao processar resposta do Gemini", e);
            throw new GeminiApiException("Erro inesperado", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Map<String, Object> processarRespostaDieta(String resposta) {
        try {
            // Extrair o JSON do plano alimentar
            int inicioJson = resposta.indexOf("{");
            int fimJson = resposta.lastIndexOf("}") + 1;
            String jsonPlano = resposta.substring(inicioJson, fimJson);

            // Parsear o JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode planoAlimentar = mapper.readTree(jsonPlano)
                .path("planoAlimentar")
                .path("refeicoes");

            // Lista para armazenar as refeições
            List<Map<String, Object>> refeicoes = new ArrayList<>();
            double totalCalories = 0;

            // Refeições específicas para processamento
            String[] refeicoesEsperadas = {"Café da Manhã", "Lanche Matinal", "Almoço", "Lanche da Tarde", "Jantar", "Ceia"};

            // Criar um mapa temporário para armazenar as refeições encontradas
            Map<String, Map<String, Object>> refeicoesMap = new LinkedHashMap<>();

            for (String refeicaoEsperada : refeicoesEsperadas) {
                boolean refeicaoEncontrada = false;
                for (JsonNode item : planoAlimentar) {
                    if (item.path("nome").asText().equals(refeicaoEsperada)) {
                        // Processar detalhes da refeição
                        Map<String, Object> mealDetails = new HashMap<>();

                        // Adicionar atributos individuais
                        mealDetails.put("nome", item.path("nome").asText());
                        mealDetails.put("prato", item.path("prato").asText());

                        // Processar ingredientes como lista
                        List<String> ingredientes = new ArrayList<>();
                        JsonNode ingredientesNode = item.path("ingredientes");
                        if (ingredientesNode.isArray()) {
                            for (JsonNode ingrediente : ingredientesNode) {
                                ingredientes.add(ingrediente.asText());
                            }
                        }
                        mealDetails.put("ingredientes", ingredientes);

                        // Modo de preparo
                        mealDetails.put("modoPreparo", item.path("modoPreparo").asText());

                        // Processar calorias
                        String caloriasTexto = item.path("calorias").asText()
                            .replace("Aproximadamente ", "")
                            .replace(" kcal", "");
                        double calorias = Double.parseDouble(caloriasTexto);
                        mealDetails.put("calorias", calorias);

                        // Adicionar a refeição ao mapa temporário
                        refeicoesMap.put(refeicaoEsperada, mealDetails);

                        // Atualizar o total de calorias
                        totalCalories += calorias;
                        refeicaoEncontrada = true;
                        break;
                    }
                }

                // Se a refeição não for encontrada, adiciona uma refeição vazia
                if (!refeicaoEncontrada) {
                    Map<String, Object> refeicaoVazia = new HashMap<>();
                    refeicaoVazia.put("nome", refeicaoEsperada);
                    refeicaoVazia.put("prato", "Não informado");
                    List<String> ingredientesVazios = new ArrayList<>();
                    ingredientesVazios.add("Não informado");
                    refeicaoVazia.put("ingredientes", ingredientesVazios);
                    refeicaoVazia.put("modoPreparo", "Não informado");
                    refeicaoVazia.put("calorias", 0.0);
                    refeicoesMap.put(refeicaoEsperada, refeicaoVazia);
                }
            }

            // Adicionar as refeições na ordem esperada à lista 'refeicoes'
            for (String refeicaoEsperada : refeicoesEsperadas) {
                Map<String, Object> refeicao = refeicoesMap.get(refeicaoEsperada);
                if (refeicao != null) {
                    refeicoes.add(refeicao);
                }
            }

            // Criar o mapa final com as refeições e o total de calorias
            Map<String, Object> resultado = new LinkedHashMap<>();
            resultado.put("refeicoes", refeicoes);
            resultado.put("totalCalorias", totalCalories);

            return resultado;
        } catch (Exception e) {
            logger.error("Erro ao processar resposta da dieta", e);
            throw new RuntimeException("Falha ao processar resposta da dieta", e);
        }
    }


    private int calcularIdade(LocalDate dataNascimento) {
        LocalDate dataAtual = LocalDate.now();
        Period idade = Period.between(dataNascimento, dataAtual);
        return idade.getYears();
    }
}