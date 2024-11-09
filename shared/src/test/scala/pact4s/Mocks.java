/*
 * Copyright 2021 io.github.jbwheatley
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
