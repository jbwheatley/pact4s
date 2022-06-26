package pact4s;

import au.com.dius.pact.provider.IConsumerInfo;
import au.com.dius.pact.provider.ProviderInfo;
import au.com.dius.pact.provider.ProviderVerifier;
import pact4s.provider.PactSource;

import static org.mockito.Mockito.mock;

public class Mocks {
    PactSource pactSource = mock(PactSource.class);
    ProviderInfo providerInfo = mock(ProviderInfo.class);
    IConsumerInfo consumerInfo = mock(IConsumerInfo.class);
    ProviderVerifier providerVerifier = mock(ProviderVerifier.class);
}
