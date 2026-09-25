package it.gov.pagopa.payhub.ionotification.connector.organization;

import it.gov.pagopa.payhub.ionotification.connector.organization.client.OrganizationClient;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationApiKeyType;
import it.gov.pagopa.pu.organization.dto.generated.OrganizationApiKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@CacheConfig(cacheNames = it.gov.pagopa.payhub.ionotification.config.CacheConfig.Fields.organizationApiKey)
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationClient organizationClient;

    public OrganizationServiceImpl(OrganizationClient organizationClient) {
        this.organizationClient = organizationClient;
    }

    @Override
    @Cacheable(key = "#organizationId + '-' + #keyType", unless = "#result == null")
    public OrganizationApiKeys getOrganizationApiKey(String accessToken, Long organizationId, OrganizationApiKeyType keyType) {
        log.debug("Fetching API key for organizationId: {} and keyType: {}", organizationId, keyType);
        try {
            return organizationClient.getOrganizationApiKey(accessToken, organizationId, keyType);
        } catch (Exception e) {
            log.error("Failed to retrieve API key for organizationId: {} and keyType: {}", organizationId, keyType, e);
            return null;
        }
    }
}
