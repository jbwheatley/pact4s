package pact4s

import au.com.dius.pact.provider.{IConsumerInfo, ProviderVerifier}

trait PactVerifyResources {
  def provider: ProviderInfoBuilder

  private[pact4s] def providerInfo = provider.toProviderInfo

  private[pact4s] val verifier = new ProviderVerifier()

  private[pact4s] def verifySingleConsumer(consumer: IConsumerInfo): Unit

  def verifyPacts(): Unit = {
    verifier.initialiseReporters(providerInfo)
    providerInfo.getConsumers.forEach(verifySingleConsumer(_))
  }
}
