package pact4s;

import au.com.dius.pact.provider.IConsumerInfo;
import au.com.dius.pact.provider.ProviderInfo;
import au.com.dius.pact.provider.ProviderVerifier;
import pact4s.provider.PactSource;
import pact4s.provider.ProviderInfoBuilder;

import static org.mockito.Mockito.mock;

public class Mocks {
    PactSource pactSource = mock(PactSource.class);
    ProviderInfo providerInfo = mock(ProviderInfo.class);
    IConsumerInfo consumerInfo = mock(IConsumerInfo.class);
    ProviderInfoBuilder providerInfoBuilder = mock(ProviderInfoBuilder.class);
}
