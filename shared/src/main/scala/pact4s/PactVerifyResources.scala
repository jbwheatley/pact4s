package pact4s

import au.com.dius.pact.provider.{IConsumerInfo, ProviderInfo, ProviderVerifier}

trait PactVerifyResources {
  def provider: ProviderInfo

  private[pact4s] val verifier = new ProviderVerifier()

  private[pact4s] def verifySingleConsumer(consumer: IConsumerInfo): Unit

  def verifyPacts(): Unit = {
    verifier.initialiseReporters(provider)
    provider.getConsumers.forEach(verifySingleConsumer(_))
  }
}
