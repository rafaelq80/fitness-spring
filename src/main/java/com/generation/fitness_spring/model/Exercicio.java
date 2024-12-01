package com.generation.fitness_spring.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "tb_exercicios")
public class Exercicio {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "O local de partida é obrigatório")
	private String nome;

	private Double tempo;

	private Integer serie;

	private Integer repeticao;
	
	private Double peso;

	private Double descanso;
	
	@Size(max = 5000, message = "O link da foto deve ter no máximo 5000 caracteres")
	private String foto;

	@ManyToOne
	@JsonIgnoreProperties("exercicio")
	private Categoria categoria;

	public Exercicio() { }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public Double getTempo() {
		return tempo;
	}

	public void setTempo(Double tempo) {
		this.tempo = tempo;
	}

	public Integer getSerie() {
		return serie;
	}

	public void setSerie(Integer serie) {
		this.serie = serie;
	}

	public Integer getRepeticao() {
		return repeticao;
	}

	public void setRepeticao(Integer repeticao) {
		this.repeticao = repeticao;
	}

	public Double getPeso() {
		return peso;
	}

	public void setPeso(Double peso) {
		this.peso = peso;
	}

	public Double getDescanso() {
		return descanso;
	}

	public void setDescanso(Double descanso) {
		this.descanso = descanso;
	}

	public String getFoto() {
		return foto;
	}

	public void setFoto(String foto) {
		this.foto = foto;
	}

	public Categoria getCategoria() {
		return categoria;
	}

	public void setCategoria(Categoria categoria) {
		this.categoria = categoria;
	}

}