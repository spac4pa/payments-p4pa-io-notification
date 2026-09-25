package it.gov.pagopa.payhub.ionotification.connector.organization.client;

import it.gov.pagopa.payhub.ionotification.connector.organization.config.OrganizationApisHolder;
import it.gov.pagopa.pu.organization.client.generated.OrganizationApi;
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
class OrganizationClientTest {

  @Mock
  private OrganizationApisHolder organizationApisHolderMock;
  @Mock
  private OrganizationApi organizationApi;

  private OrganizationClient organizationClient;

  @BeforeEach
  void setUp() {
    organizationClient = new OrganizationClient(organizationApisHolderMock);
  }

  @AfterEach
  void verifyNoMoreInteractions() {
    Mockito.verifyNoMoreInteractions(
      organizationApisHolderMock,
            organizationApi
    );
  }

  @Test
  void whenGetOrganizationApiKeyThenInvokeWithAccessToken() {
    // Given
    String accessToken = "ACCESSTOKEN";
    Long organizationId = 1L;

    OrganizationApiKeys expectedResult = new OrganizationApiKeys();
    expectedResult.setApiKey("apiKey");
    expectedResult.setKeyType(KeyTypeEnum.IO);

    Mockito.when(organizationApisHolderMock.getOrganizationApi(accessToken))
      .thenReturn(organizationApi);

    Mockito.when(organizationApi.getOrganizationApiKey(organizationId, OrganizationApiKeyType.IO, null))
            .thenReturn(expectedResult);

    // When
    OrganizationApiKeys result = organizationClient.getOrganizationApiKey(accessToken, organizationId, OrganizationApiKeyType.IO);

    // Then
    Assertions.assertSame(expectedResult, result);
    Mockito.verify(organizationApisHolderMock).getOrganizationApi(accessToken);
    Mockito.verify(organizationApi).getOrganizationApiKey(organizationId, OrganizationApiKeyType.IO, null);
  }
}
