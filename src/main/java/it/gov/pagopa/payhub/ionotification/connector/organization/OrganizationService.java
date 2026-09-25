package it.gov.pagopa.payhub.ionotification.connector.organization;

import it.gov.pagopa.pu.organization.dto.generated.OrganizationApiKeyType;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationApiKeys;

public interface OrganizationService {

  OrganizationApiKeys getOrganizationApiKey(String accessToken, Long organizationId, OrganizationApiKeyType keyType);
}
