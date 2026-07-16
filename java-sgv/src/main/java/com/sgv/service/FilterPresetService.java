package com.sgv.service;

import com.sgv.entity.FilterPreset;
import com.sgv.repository.FilterPresetRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FilterPresetService {

    private final FilterPresetRepository repo;

    public FilterPresetService(FilterPresetRepository repo) {
        this.repo = repo;
    }

    public FilterPreset savePreset(FilterPreset preset) {
        return repo.findByUserIdAndTypeAndName(preset.getUserId(), preset.getType(), preset.getName())
                .map(existing -> {
                    existing.setData(preset.getData());
                    return repo.save(existing);
                })
                .orElseGet(() -> repo.save(preset));
    }

    public List<FilterPreset> listPresets(Long userId, String type) {
        return repo.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type);
    }

    public Optional<FilterPreset> findPreset(Long userId, String type, String name) {
        return repo.findByUserIdAndTypeAndName(userId, type, name);
    }

    public void deletePresetByName(Long userId, String type, String name) {
        findPreset(userId, type, name).ifPresent(p -> repo.deleteById(p.getId()));
    }
}