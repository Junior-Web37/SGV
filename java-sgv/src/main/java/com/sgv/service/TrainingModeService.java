package com.sgv.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Serviço centralizado para bloquear operações de escrita quando o modo treino (demo) está activo.
 * Quando demoMode=true, qualquer tentativa de alterar dados reais deve ser bloqueada.
 */
@Service
public class TrainingModeService {

    private static final Logger log = LoggerFactory.getLogger(TrainingModeService.class);

    private final AppConfigService appConfigService;

    public TrainingModeService(AppConfigService appConfigService) {
        this.appConfigService = appConfigService;
    }

    public boolean isTrainingMode() {
        return appConfigService.isDemoMode();
    }

    /**
     * Verifica se a operação de escrita é permitida.
     * Retorna true se permitido (não está em modo treino).
     * Retorna false se bloqueado (está em modo treino).
     */
    public boolean checkAllowed(String operation) {
        if (!isTrainingMode()) return true;
        log.warn("Operação bloqueada pelo modo treino: {}", operation);
        return false;
    }

    /**
     * Verifica se a operação é permitida e lança exceção se não for.
     */
    public void requireAllowed(String operation) {
        if (!checkAllowed(operation)) {
            throw new TrainingModeBlockedException(
                "Operação bloqueada: modo treino está activo. " +
                "Desative o modo treino em Sistema > Modo Treinamento para efectuar esta acção."
            );
        }
    }

    /**
     * Exceção lançada quando uma operação de escrita é bloqueada pelo modo treino.
     */
    public static class TrainingModeBlockedException extends RuntimeException {
        public TrainingModeBlockedException(String message) {
            super(message);
        }
    }
}
