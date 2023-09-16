package extensions.authorization

import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(AuthorizedRouteExtension::class)
annotation class Authorized
