package com.sgv.service;

import com.sgv.service.TrainingModeService.TrainingModeBlockedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TrainingModeServiceTest {

    private static class FakeAppConfigService extends AppConfigService {
        private final boolean demo;

        FakeAppConfigService(boolean demo) {
            super(null);
            this.demo = demo;
        }

        @Override
        public boolean isDemoMode() {
            return demo;
        }
    }

    private TrainingModeService service(boolean demo) {
        return new TrainingModeService(new FakeAppConfigService(demo));
    }

    @Test
    void isTrainingMode_reflectsConfig() {
        assertTrue(service(true).isTrainingMode());
        assertFalse(service(false).isTrainingMode());
    }

    @Test
    void checkAllowed_inTrainingMode_returnsFalse() {
        assertFalse(service(true).checkAllowed("SALVAR_PRODUTO"));
    }

    @Test
    void checkAllowed_normalMode_returnsTrue() {
        assertTrue(service(false).checkAllowed("SALVAR_PRODUTO"));
    }

    @Test
    void requireAllowed_inTrainingMode_throwsBlockedException() {
        TrainingModeBlockedException ex = assertThrows(TrainingModeBlockedException.class,
                () -> service(true).requireAllowed("SALVAR_PRODUTO"));
        assertTrue(ex.getMessage().contains("modo treino"));
    }

    @Test
    void requireAllowed_normalMode_doesNotThrow() {
        assertDoesNotThrow(() -> service(false).requireAllowed("SALVAR_PRODUTO"));
    }
}
