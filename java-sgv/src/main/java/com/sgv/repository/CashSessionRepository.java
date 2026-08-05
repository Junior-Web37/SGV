package com.sgv.repository;

import com.sgv.entity.CashSession;
import com.sgv.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CashSessionRepository extends JpaRepository<CashSession, Long> {

    Optional<CashSession> findByUserAndState(User user, String state);

    boolean existsByUserAndState(User user, String state);

    List<CashSession> findByUserOrderByOpenedAtDesc(User user);

    /**
     * Devolve todas as sessões em aberto (state="OPEN") abertas ANTES de uma
     * data/hora — usado para detectar sessões "esquecidas" de dias anteriores.
     */
    List<CashSession> findByStateAndOpenedAtBefore(String state, LocalDateTime before);

    /**
     * Devolve a sessão em aberto (state="OPEN") do utilizador, se existir.
     */
    Optional<CashSession> findFirstByUserAndStateOrderByOpenedAtDesc(User user, String state);
}
