package com.chatbotmvt.handlers;

import com.chatbotmvt.dto.SessionData;
import com.chatbotmvt.entity.BotFlowRule;
import com.chatbotmvt.entity.BotOpcion;
import com.chatbotmvt.entity.BotState;
import com.chatbotmvt.entity.Sector;
import com.chatbotmvt.entity.UsuarioSesion;
import com.chatbotmvt.repository.BotStateRepository;
import com.chatbotmvt.services.SectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ConfirmSectorActionHandler implements BotActionHandler {

    private final SectorService sectorService;
    private final BotStateRepository botStateRepository;

    @Override
    public String getActionType() { return "CONFIRM_SECTOR"; }

    @Override
    public String execute(UsuarioSesion sesion, BotFlowRule rule, String input) {
        return procesarConfirmacionYSalto(sesion);
    }

    @Override
    public String executeFromOption(UsuarioSesion sesion, BotOpcion opcion, String input) {
        return procesarConfirmacionYSalto(sesion);
    }

    private String procesarConfirmacionYSalto(UsuarioSesion sesion) {
        SessionData data = sesion.getTempData();

        if (data.getPendingSectorId() != null) {
            Sector sector = sectorService.findById(data.getPendingSectorId());
            sesion.setSector(sector);
            data.setPendingSectorId(null);
        }

        String tipo = data.getTipoReclamo();
        if (tipo != null) {
            Map<String, Long> mapaDestinos = Map.of(
                    "DESMALEZADO", 4L,
                    "BARRIDO", 5L,
                    "RIEGO", 30L,
                    "ESCOMBROS", 31L,
                    "BOLSONES", 21L,
                    "DESPERDICIOS", 21L
            );

            Long nextStateId = mapaDestinos.get(tipo.toUpperCase());

            if (nextStateId != null) {
                BotState nextState = botStateRepository.findById(nextStateId).orElse(null);
                if (nextState != null) {
                    sesion.setCurrentState(nextState);
                }
            }
        }

        return null;
    }
}