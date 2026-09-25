package it.gov.pagopa.payhub.ionotification.service.notify;

import it.gov.pagopa.payhub.ionotification.connector.io.IORestConnector;
import it.gov.pagopa.payhub.ionotification.connector.organization.OrganizationService;
import it.gov.pagopa.payhub.ionotification.dto.*;
import it.gov.pagopa.payhub.ionotification.dto.generated.MessageResponseDTO;
import it.gov.pagopa.payhub.ionotification.dto.generated.NotificationRequestDTO;
import it.gov.pagopa.payhub.ionotification.dto.mapper.IONotificationMapper;
import it.gov.pagopa.payhub.ionotification.enums.NotificationStatus;
import it.gov.pagopa.payhub.ionotification.model.IONotification;
import it.gov.pagopa.payhub.ionotification.repository.IONotificationRepository;
import it.gov.pagopa.payhub.ionotification.service.UserIdObfuscatorService;
import it.gov.pagopa.payhub.ionotification.utils.Utilities;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationApiKeyType;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationApiKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static it.gov.pagopa.payhub.ionotification.enums.NotificationStatus.KO_SENDER_NOT_ALLOWED;
import static it.gov.pagopa.payhub.ionotification.enums.NotificationStatus.OK;

@Service
@Slf4j
public class IONotificationServiceImpl implements IONotificationService {

    private final IONotificationRepository ioNotificationRepository;
    private final IORestConnector connector;
    private final IONotificationMapper ioNotificationMapper;
    private final UserIdObfuscatorService obfuscatorService;
    private final OrganizationService organizationService;
    private final Long timeToLive;

    public IONotificationServiceImpl(IONotificationRepository ioNotificationRepository,
                                     IORestConnector connector,
                                     IONotificationMapper ioNotificationMapper,
                                     UserIdObfuscatorService obfuscatorService, OrganizationService organizationService,
                                     @Value("${rest.backend-io-manage.notification.ttl}") Long timeToLive) {
        this.ioNotificationRepository = ioNotificationRepository;
        this.connector = connector;
        this.ioNotificationMapper = ioNotificationMapper;
        this.obfuscatorService = obfuscatorService;
        this.organizationService = organizationService;
        this.timeToLive = timeToLive;
    }

    @Override
    public MessageResponseDTO sendMessage(String accessToken, NotificationRequestDTO notificationRequestDTO) {
        log.info("Sending notification to organizationId {} and debtPositionTypeOrgId {} related to nav {}",
                notificationRequestDTO.getOrgId(), notificationRequestDTO.getDebtPositionTypeOrgId(), notificationRequestDTO.getNav());
        OrganizationApiKeys apiKey = organizationService.getOrganizationApiKey(accessToken, notificationRequestDTO.getOrgId(), OrganizationApiKeyType.IO);
        if (apiKey != null && Boolean.TRUE.equals(apiKey.getServiceEnabled())) {
            String token = retrieveTokenIO(notificationRequestDTO.getServiceId(), apiKey.getApiKey());
            if (isSenderAllowed(notificationRequestDTO, token)) {
                String notificationId = sendNotification(notificationRequestDTO, token);
                return MessageResponseDTO.builder().notificationId(notificationId).build();
            }
        } else {
            log.info("No ApiKey configured for IO service on organization {}", notificationRequestDTO.getOrgId());
        }
        return null;
    }

    @Override
    public void deleteNotification(String notificationId) {
        Optional<IONotification> ioNotification = ioNotificationRepository.findByNotificationId(notificationId);

        if (ioNotification.isPresent()) {
            log.info("Deleting notification {}", notificationId);
            ioNotificationRepository.delete(ioNotification.get());
        }
    }

    private String retrieveTokenIO(String serviceId, String apiKey) {
        log.debug("Retrieve token from IO for service {}", serviceId);
        KeysDTO keys = connector.getServiceKeys(serviceId, apiKey);
        return keys.getPrimaryKey();
    }

    private boolean isSenderAllowed(NotificationRequestDTO notificationRequestDTO, String token) {
        FiscalCodeDTO fiscalCode = ioNotificationMapper.mapToGetProfile(notificationRequestDTO);

        if (!Utilities.checkFiscalCode(fiscalCode.getFiscalCode())) {
            return handleSenderNotAllowed(notificationRequestDTO);
        }

        log.debug("Verify if user is allowed to receive notification");
        ProfileResource profileResource = connector.getProfile(fiscalCode, token);
        if (profileResource == null || !profileResource.isSenderAllowed()) {
            return handleSenderNotAllowed(notificationRequestDTO);
        }
        return true;
    }

    private boolean handleSenderNotAllowed(NotificationRequestDTO notificationRequestDTO) {
        log.info("The user is not enabled to receive notifications");
        saveNotification(notificationRequestDTO, null, KO_SENDER_NOT_ALLOWED);
        return false;
    }

    private String sendNotification(NotificationRequestDTO notificationRequestDTO, String token) {

        NotificationDTO notificationDTO = ioNotificationMapper
                .map(timeToLive, notificationRequestDTO);

        log.debug("Sending notification to IO");
        NotificationResource notificationResource = connector.sendNotification(notificationDTO, token);
        saveNotification(notificationRequestDTO, notificationResource.getId(), OK);

        return notificationResource.getId();
    }

    private void saveNotification(NotificationRequestDTO notificationRequestDTO, String notificationId, NotificationStatus status) {
        IONotification ioNotification = ioNotificationMapper
                .mapToSaveNotification(notificationRequestDTO, status, encryptFiscalCode(notificationRequestDTO.getFiscalCode()));

        if (notificationId != null) {
            ioNotification.setNotificationId(notificationId);
        }

        log.debug("Saving notification with status {}", status);
        ioNotificationRepository.save(ioNotification);
    }

    private String encryptFiscalCode(String fiscalCode) {
        return obfuscatorService.obfuscate(fiscalCode);
    }
}
