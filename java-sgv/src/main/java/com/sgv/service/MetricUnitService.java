package com.sgv.service;

import com.sgv.entity.MetricUnit;
import com.sgv.repository.MetricUnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class MetricUnitService {

    private final MetricUnitRepository metricUnitRepository;

    public MetricUnitService(MetricUnitRepository metricUnitRepository) {
        this.metricUnitRepository = metricUnitRepository;
    }

    public List<MetricUnit> getAllUnits() {
        return metricUnitRepository.findAll();
    }

    public Optional<MetricUnit> getUnitById(Long id) {
        return metricUnitRepository.findById(id);
    }

    public Optional<MetricUnit> getUnitByAbbreviation(String abbreviation) {
        return metricUnitRepository.findByAbbreviation(abbreviation);
    }

    public boolean existsByAbbreviation(String abbreviation, Long excludeId) {
        if (abbreviation == null || abbreviation.isBlank()) return false;
        String trimmed = abbreviation.trim();
        return metricUnitRepository.findByAbbreviation(trimmed)
                .filter(u -> excludeId == null || !excludeId.equals(u.getId()))
                .isPresent();
    }

    @Transactional
    public MetricUnit createUnit(String abbreviation, String description) {
        if (metricUnitRepository.existsByAbbreviation(abbreviation)) {
            throw new RuntimeException("Já existe uma unidade com a abreviatura: " + abbreviation);
        }

        MetricUnit unit = new MetricUnit();
        unit.setAbbreviation(abbreviation.toUpperCase());
        unit.setDescription(description);

        return metricUnitRepository.save(unit);
    }

    @Transactional
    public MetricUnit saveMetricUnit(MetricUnit unit) {
        if (unit == null) throw new IllegalArgumentException("MetricUnit is null");
        String trimmedAbbr = unit.getAbbreviation() == null ? "" : unit.getAbbreviation().trim();
        if (trimmedAbbr.isBlank()) throw new IllegalArgumentException("Abreviatura é obrigatória.");
        if (existsByAbbreviation(trimmedAbbr, unit.getId())) {
            throw new IllegalArgumentException("Abreviatura já existe.");
        }
        unit.setAbbreviation(trimmedAbbr.toUpperCase());
        return metricUnitRepository.save(unit);
    }

    @Transactional
    public MetricUnit updateUnit(Long id, String abbreviation, String description) {
        MetricUnit unit = metricUnitRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Unidade não encontrada: " + id));

        if (!unit.getAbbreviation().equalsIgnoreCase(abbreviation)
            && metricUnitRepository.existsByAbbreviation(abbreviation)) {
            throw new RuntimeException("Já existe uma unidade com a abreviatura: " + abbreviation);
        }

        unit.setAbbreviation(abbreviation.toUpperCase());
        unit.setDescription(description);

        return metricUnitRepository.save(unit);
    }

    @Transactional
    public void deleteUnit(Long id) {
        if (!metricUnitRepository.existsById(id)) {
            throw new RuntimeException("Unidade não encontrada: " + id);
        }
        metricUnitRepository.deleteById(id);
    }

    @Transactional
    public void seedDefaultUnits() {
        if (metricUnitRepository.count() == 0) {
            createUnit("UN", "Unidade");
            createUnit("KG", "Quilograma");
            createUnit("G", "Grama");
            createUnit("LT", "Litro");
            createUnit("ML", "Mililitro");
            createUnit("M", "Metro");
            createUnit("CM", "Centímetro");
            createUnit("PC", "Peça");
            createUnit("CX", "Caixa");
            createUnit("SAC", "Saco");
            createUnit("BAR", "Barra");
            createUnit("LTR", "Lote");
            createUnit("H", "Hora");
            createUnit("D", "Dia");
            createUnit("M2", "Metro Quadrado");
            createUnit("M3", "Metro Cúbico");
        }
    }
}
