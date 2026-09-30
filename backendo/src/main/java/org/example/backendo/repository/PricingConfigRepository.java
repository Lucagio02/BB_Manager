package org.example.backendo.repository;


import org.example.backendo.model.PricingConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PricingConfigRepository extends JpaRepository<PricingConfig, Long> {
}
