package it.gov.pagopa.payhub.ionotification.service.notify;

import it.gov.pagopa.payhub.ionotification.connector.io.IORestConnector;
import it.gov.pagopa.payhub.ionotification.connector.organization.OrganizationService;
import it.gov.pagopa.payhub.ionotification.dto.FiscalCodeDTO;
import it.gov.pagopa.payhub.ionotification.dto.KeysDTO;
import it.gov.pagopa.payhub.ionotification.dto.NotificationResource;
import it.gov.pagopa.payhub.ionotification.dto.ProfileResource;
import it.gov.pagopa.payhub.ionotification.dto.generated.MessageResponseDTO;
import it.gov.pagopa.payhub.ionotification.dto.generated.NotificationRequestDTO;
import it.gov.pagopa.payhub.ionotification.dto.mapper.IONotificationMapper;
import it.gov.pagopa.payhub.ionotification.enums.NotificationStatus;
import it.gov.pagopa.payhub.ionotification.model.IONotification;
import it.gov.pagopa.payhub.ionotification.model.IOService;
import it.gov.pagopa.payhub.ionotification.repository.IONotificationRepository;
import it.gov.pagopa.payhub.ionotification.service.UserIdObfuscatorService;
import it.gov.pagopa.pu.organization.dto.generated.KeyTypeEnum;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationApiKeyType;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationApiKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static it.gov.pagopa.payhub.ionotification.enums.NotificationStatus.KO_SENDER_NOT_ALLOWED;
import static it.gov.pagopa.payhub.ionotification.enums.NotificationStatus.OK;
import static it.gov.pagopa.payhub.ionotification.utils.IOTestMapper.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IONotificationServiceTest {

    public static final long TIME_TO_LIVE = 3600L;
    public static final String API_KEY = "API_KEY";
    public static final String NOTIFICATION_ID = "NOTIFICATION_ID";
    private IONotificationService service;

    @Mock
    private IONotificationRepository ioNotificationRepositoryMock;
    @Mock
    private IORestConnector connectorMock;
    @Mock
    private IONotificationMapper ioNotificationMapperMock;
    @Mock
    private UserIdObfuscatorService obfuscatorServiceMock;
    @Mock
    private OrganizationService organizationServiceMock;

    private IOService ioService;
    private KeysDTO keysDTO;
    private NotificationRequestDTO notificationRequestDTO;
    private IONotification ioNotification;
    private FiscalCodeDTO fiscalCodeDTO;

    @BeforeEach
    void setup() {
        service = new IONotificationServiceImpl(
                ioNotificationRepositoryMock,
                connectorMock,
                ioNotificationMapperMock,
                obfuscatorServiceMock,
                organizationServiceMock,
                TIME_TO_LIVE);

        ioService = mapIoService(createServiceRequestDTO());
        keysDTO = getTokenIOResponse();
        notificationRequestDTO = buildNotificationRequestDTO();
        ioNotification = mapIONotification();
        fiscalCodeDTO = getUserProfileRequest();
    }

    @Test
    void givenSendNotificationThenSuccess() {
        mockServiceAndObtainIOToken();
        NotificationRequestDTO requestDTO = buildNotificationRequestDTO();
        MessageResponseDTO messageResponseDTO = buildMessageResponseDTO();
        String accessToken = "accessToken";

        OrganizationApiKeys organizationApiKeys = new OrganizationApiKeys();
        organizationApiKeys.setApiKey(API_KEY);
        organizationApiKeys.setKeyType(KeyTypeEnum.IO);
        organizationApiKeys.serviceEnabled(true);

        when(organizationServiceMock.getOrganizationApiKey(accessToken, ORG_ID, OrganizationApiKeyType.IO))
                .thenReturn(organizationApiKeys);

        when(connectorMock.getProfile(fiscalCodeDTO, keysDTO.getPrimaryKey()))
                .thenReturn(getUserProfileResponse());

        when(ioNotificationMapperMock.map(TIME_TO_LIVE, requestDTO))
                .thenReturn(sendNotificationRequest());

        when(connectorMock.sendNotification(sendNotificationRequest(), keysDTO.getPrimaryKey()))
                .thenReturn(new NotificationResource("ID"));

        MessageResponseDTO result = sendNotification(OK);

        assertEquals("ID", ioNotification.getNotificationId());
        assertEquals(messageResponseDTO, result);
    }

    @Test
    void givenSendNotificationWhenSenderIsLegalPersonWithInvalidCFThenSuccess() {
        NotificationRequestDTO requestDTO = buildNotificationRequestDTO();
        String accessToken = "accessToken";
        FiscalCodeDTO legalPersonCF = new FiscalCodeDTO("12345678901");

        OrganizationApiKeys organizationApiKeys = new OrganizationApiKeys();
        organizationApiKeys.setApiKey(API_KEY);
        organizationApiKeys.setKeyType(KeyTypeEnum.IO);
        organizationApiKeys.serviceEnabled(true);

        when(ioNotificationMapperMock.mapToGetProfile(requestDTO)).thenReturn(legalPersonCF);

        when(organizationServiceMock.getOrganizationApiKey(accessToken, ORG_ID, OrganizationApiKeyType.IO))
                .thenReturn(organizationApiKeys);
        when(connectorMock.getServiceKeys(SERVICE_ID, API_KEY)).thenReturn(keysDTO);

        when(obfuscatorServiceMock.obfuscate(FISCAL_CODE)).thenReturn(USER_ID);
        when(ioNotificationMapperMock.mapToSaveNotification(requestDTO, KO_SENDER_NOT_ALLOWED, USER_ID))
                .thenReturn(ioNotification);

        MessageResponseDTO result = service.sendMessage(accessToken, requestDTO);

        assertNull(result);
        verify(ioNotificationMapperMock).mapToSaveNotification(any(), any(), any());
    }

    @Test
    void givenSendNotificationWhenApiKeyNullThenReturnNull() {
        String accessToken = "accessToken";

        when(organizationServiceMock.getOrganizationApiKey(accessToken, ORG_ID, OrganizationApiKeyType.IO))
                .thenReturn(null);

        MessageResponseDTO result = service.sendMessage(accessToken, notificationRequestDTO);

        assertNull(ioNotification.getNotificationId());
        assertNull(result);
    }

    @Test
    void givenSendNotificationWhenSenderIsNotAllowedThenSaveKO() {
        mockServiceAndObtainIOToken();
        String accessToken = "accessToken";
        OrganizationApiKeys organizationApiKeys = new OrganizationApiKeys();
        organizationApiKeys.setApiKey(API_KEY);
        organizationApiKeys.setKeyType(KeyTypeEnum.IO);
        organizationApiKeys.serviceEnabled(true);

        when(organizationServiceMock.getOrganizationApiKey(accessToken, ORG_ID, OrganizationApiKeyType.IO))
                .thenReturn(organizationApiKeys);

        when(connectorMock.getProfile(fiscalCodeDTO, keysDTO.getPrimaryKey()))
                .thenReturn(new ProfileResource(false, new ArrayList<>()));

        MessageResponseDTO result = sendNotification(KO_SENDER_NOT_ALLOWED);

        assertNull(ioNotification.getNotificationId());
        assertNull(result);
    }

    @Test
    void givenSendNotificationWhenSenderNotAllowedExceptionThenSaveKO() {
        mockServiceAndObtainIOToken();
        String accessToken = "accessToken";
        OrganizationApiKeys organizationApiKeys = new OrganizationApiKeys();
        organizationApiKeys.setApiKey(API_KEY);
        organizationApiKeys.setKeyType(KeyTypeEnum.IO);
        organizationApiKeys.serviceEnabled(true);

        when(organizationServiceMock.getOrganizationApiKey(accessToken, ORG_ID, OrganizationApiKeyType.IO))
                .thenReturn(organizationApiKeys);

        doReturn(null).when(connectorMock).getProfile(fiscalCodeDTO, keysDTO.getPrimaryKey());

        MessageResponseDTO result = sendNotification(KO_SENDER_NOT_ALLOWED);

        assertNull(ioNotification.getNotificationId());
        assertNull(result);
    }

    @Test
    void givenDeleteNotificationThenSuccess() {
        when(ioNotificationRepositoryMock.findByNotificationId(NOTIFICATION_ID))
                .thenReturn(Optional.of(ioNotification));

        service.deleteNotification(NOTIFICATION_ID);

        verify(ioNotificationRepositoryMock, times(1)).delete(any(IONotification.class));
        verify(ioNotificationRepositoryMock, times(1)).delete(ioNotification);
    }

    @Test
    void givenDeleteNotificationWhenNotificationDoesNotExistThenDoNothing() {
        when(ioNotificationRepositoryMock.findByNotificationId(NOTIFICATION_ID))
                .thenReturn(Optional.empty());

        service.deleteNotification(NOTIFICATION_ID);

        verify(ioNotificationRepositoryMock, times(0)).delete(any(IONotification.class));
        verify(ioNotificationRepositoryMock, times(1)).findByNotificationId(NOTIFICATION_ID);
    }

    private MessageResponseDTO sendNotification(NotificationStatus status) {
        mockEncryptFiscalCode();
        String accessToken = "accessToken";

        when(ioNotificationMapperMock.mapToSaveNotification(notificationRequestDTO, status, USER_ID))
                .thenReturn(ioNotification);

        MessageResponseDTO messageResponseDTO = service.sendMessage(accessToken, notificationRequestDTO);

        verify(ioNotificationRepositoryMock, times(1)).save(ioNotification);

        return messageResponseDTO;
    }

    private void mockServiceAndObtainIOToken() {
        ioService.setServiceId(SERVICE_ID);

        when(connectorMock.getServiceKeys(SERVICE_ID, API_KEY)).thenReturn(keysDTO);

        when(ioNotificationMapperMock.mapToGetProfile(notificationRequestDTO)).thenReturn(fiscalCodeDTO);
    }

    private void mockEncryptFiscalCode() {
        when(obfuscatorServiceMock.obfuscate(FISCAL_CODE)).thenReturn(USER_ID);
    }

    private static MessageResponseDTO buildMessageResponseDTO() {
        return MessageResponseDTO.builder().notificationId("ID").build();
    }
}