package it.gov.pagopa.payhub.ionotification.connector.organization.service;

import it.gov.pagopa.payhub.ionotification.connector.organization.OrganizationService;
import it.gov.pagopa.payhub.ionotification.connector.organization.OrganizationServiceImpl;
import it.gov.pagopa.payhub.ionotification.connector.organization.client.OrganizationClient;
import it.gov.pagopa.pu.organization.dto.generated.KeyTypeEnum;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationApiKeyType;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationApiKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationClient organizationClient;

    private OrganizationService organizationService;

    @BeforeEach
    void init(){
        organizationService = new OrganizationServiceImpl(
                organizationClient
        );
    }

    @AfterEach
    void verifyNoMoreInteractions(){
        Mockito.verifyNoMoreInteractions(
                organizationClient
        );
    }

    @Test
    void givenNotExistentFiscalCodeWhenGetOrganizationByFiscalCodeThenEmpty(){
        // Given
        Long orgId = 1L;
        String accessToken = "accessToken";
        OrganizationApiKeys expectedResult = new OrganizationApiKeys();
        expectedResult.setApiKey("apiKey");
        expectedResult.setKeyType(KeyTypeEnum.IO);

        Mockito.when(organizationClient.getOrganizationApiKey(accessToken, orgId, OrganizationApiKeyType.IO))
                .thenReturn(expectedResult);

        // When
        OrganizationApiKeys result = organizationService.getOrganizationApiKey(accessToken, orgId, OrganizationApiKeyType.IO);

        // Then
        Assertions.assertEquals("apiKey", result.getApiKey());
        Assertions.assertEquals(KeyTypeEnum.IO, result.getKeyType());
    }
}
