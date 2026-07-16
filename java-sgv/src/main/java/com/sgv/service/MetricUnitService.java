package com.sgv.service;

import com.sgv.entity.MetricUnit;
import com.sgv.repository.MetricUnitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class MetricUnitService {

    @Autowired
    private MetricUnitRepository metricUnitRepository;

    public List<MetricUnit> getAllUnits() {
        return metricUnitRepository.findAll();
    }

    public Optional<MetricUnit> getUnitById(Long id) {
        return metricUnitRepository.findById(id);
    }

    public Optional<MetricUnit> getUnitByAbbreviation(String abbreviation) {
        return metricUnitRepository.findByAbbreviation(abbreviation);
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
    public MetricUnit updateUnit(Long id, String abbreviation, String description) {
        MetricUnit unit = metricUnitRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Unidade não encontrada: " + id));
        
        // Verificar se a nova abreviatura já existe (se for diferente da atual)
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

    /**
     * Seed de unidades padrão para novos installs
     */
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
