package it.gov.pagopa.payhub.ionotification.connector.organization.client;

import it.gov.pagopa.payhub.ionotification.connector.organization.config.OrganizationApisHolder;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationApiKeyType;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationApiKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Lazy
@Service
@Slf4j
public class OrganizationClient {

    private final OrganizationApisHolder organizationApisHolder;

    public OrganizationClient(OrganizationApisHolder organizationApisHolder) {
        this.organizationApisHolder = organizationApisHolder;
    }

    public OrganizationApiKeys getOrganizationApiKey(String accessToken, Long organizationId, OrganizationApiKeyType keyType) {
        return organizationApisHolder.getOrganizationApi(accessToken).getOrganizationApiKey(organizationId, keyType, null);
    }
}
