package es.caib.concsv.back.helper;

import es.caib.concsv.logic.base.helper.AuthenticationHelper;
import es.caib.concsv.persist.entity.UsuariEntity;
import es.caib.concsv.persist.repository.UsuariRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
public class UsuarisRefreshHelper {

	private final UsuariRepository usuariRepository;
	private final AuthenticationHelper authenticationHelper;
	private final Map<String, LocalDateTime> usuariLastDBRefresh = new ConcurrentHashMap<>();

	private static final int REFRESH_THRESHOLD_MINUTES = 5;
	private static final int SESSION_THRESHOLD_MINUTES = 30;

	public void refreshCurrentUser() {
		String codi = authenticationHelper.getCurrentUserName();
		LocalDateTime now = LocalDateTime.now();

		AtomicBoolean shouldRun = new AtomicBoolean(false);

		usuariLastDBRefresh.compute(codi, (k, lastRefresh) -> {
			if (lastRefresh == null ||
				lastRefresh.plusMinutes(REFRESH_THRESHOLD_MINUTES).isBefore(now)) {
				shouldRun.set(true);
				return now;
			}
			return lastRefresh;
		});

		if (!shouldRun.get()) {
			return;
		}

		Optional<UsuariEntity> usuariEntityOptional = usuariRepository.findById(codi);
		if (usuariEntityOptional.isEmpty()) {
            return;
		}

        UsuariEntity usuariEntity = usuariEntityOptional.get();
        // Session logic
        if (usuariEntity.getDarreraActivitat() != null &&
            usuariEntity.getDarreraActivitat().plusMinutes(SESSION_THRESHOLD_MINUTES).isBefore(now)) {
            // Closed session
            usuariEntity.setDarrerPeriode(usuariEntity.getDarreraActivitat());
        }
		usuariEntity.setDarreraActivitat(now);
		usuariRepository.save(usuariEntity);
	}

}
