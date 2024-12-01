package com.generation.fitness_spring.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import com.generation.fitness_spring.model.Exercicio;

public interface ExercicioRepository extends JpaRepository<Exercicio, Long> {

	List<Exercicio> findAllByNomeContainingIgnoreCase(@Param("nome") String nome);

}