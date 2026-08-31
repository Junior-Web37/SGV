package com.billywater;

import javafx.application.Application;

/**
 * Launcher do fat-jar.
 *
 * <p>Não estende {@code javafx.application.Application} (para o JDK não bloquear com
 * "JavaFX runtime components are missing") — apenas delega para {@link BillyWaterApp}.
 * Permite correr o jar directamente: {@code java -jar billywater-5.0.0.jar}.</p>
 */
public final class Launcher {

    private Launcher() {}

    public static void main(String[] args) {
        Application.launch(BillyWaterApp.class, args);
    }
}
