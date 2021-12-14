package pact4s.provider

sealed trait StateManagement {
  private[pact4s] def url: String
}

object StateManagement {
  final case class ProviderUrl(private[pact4s] val url: String) extends StateManagement

  sealed abstract case class StateManagementFunction(
      stateChangeFunc: PartialFunction[ProviderState, Unit],
      host: String,
      port: Int,
      endpoint: String
  ) extends StateManagement {
    def withOverrides(
        hostOverride: String = host,
        portOverride: Int = port,
        endpointOverride: String = endpoint
    ): StateManagementFunction =
      new StateManagementFunction(
        stateChangeFunc,
        host = hostOverride,
        port = portOverride,
        endpoint = endpointOverride
      ) {}

    private val slashedEndpoint     = if (!endpoint.startsWith("/")) "/" + endpoint else endpoint
    private[pact4s] val url: String = s"http://$host:$port$slashedEndpoint"
  }

  object StateManagementFunction {
    def apply(stateChangeFunc: PartialFunction[ProviderState, Unit]): StateManagementFunction =
      new StateManagementFunction(stateChangeFunc, "localhost", 64646, "/pact4s-state-change") {}
  }
}
