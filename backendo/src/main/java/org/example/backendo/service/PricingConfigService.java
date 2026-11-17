package org.example.backendo.service;

import org.example.backendo.dto.PricingConfigRequest;
import org.example.backendo.model.PricingConfig;
import org.example.backendo.repository.PricingConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PricingConfigService {

    private final PricingConfigRepository pricingConfigRepository;

    public PricingConfigService(PricingConfigRepository pricingConfigRepository) {
        this.pricingConfigRepository = pricingConfigRepository;
    }

    public PricingConfig getConfig() {
        return pricingConfigRepository.findById(1L).orElseGet(() -> {
            PricingConfig defaultConfig = PricingConfig.builder()
                    .supplementoOspiteExtra(20.0)
                    .moltiplicatoreGiugnoSettembre(1.15)
                    .moltiplicatoreLuglio(1.30)
                    .moltiplicatoreAgosto(1.50)
                    .build();
            return pricingConfigRepository.save(defaultConfig);
        });
    }

    @Transactional
    public PricingConfig updateConfig(PricingConfigRequest request) {
        PricingConfig config = getConfig();

        config.setSupplementoOspiteExtra(request.getSupplementoOspiteExtra());
        if (request.getMoltiplicatoreGiugnoSettembre() != null) {
            config.setMoltiplicatoreGiugnoSettembre(request.getMoltiplicatoreGiugnoSettembre());
        }
        if (request.getMoltiplicatoreLuglio() != null) {
            config.setMoltiplicatoreLuglio(request.getMoltiplicatoreLuglio());
        }
        if (request.getMoltiplicatoreAgosto() != null) {
            config.setMoltiplicatoreAgosto(request.getMoltiplicatoreAgosto());
        }

        return pricingConfigRepository.save(config);
    }
}
